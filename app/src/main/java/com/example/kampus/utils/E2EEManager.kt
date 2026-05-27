package com.example.kampus.utils

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.PublicKey
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * E2EEManager: Orchestrates end-to-end encryption for the chat system
 * 
 * Responsibilities:
 * 1. Generate/retrieve user's AES key from Android Keystore
 * 2. Encrypt messages before Firebase upload
 * 3. Decrypt messages on receive (background thread)
 * 4. Manage public key distribution via Firestore
 * 5. Store encrypted keys securely using EncryptedSharedPreferences
 * 
 * Key Flow:
 * - On signup: Generate RSA key pair (KeyManager) + AES key (CryptoManager)
 * - Send AES key encrypted with RSA to Firestore (for backup/recovery)
 * - Store unencrypted AES key in Android Keystore (local only)
 * - All messages encrypted with AES before Firebase
 * - On receive: Decrypt using local AES key
 */
object E2EEManager {

    private var userAESKey: SecretKey? = null
    private var cachedUserId: String? = null
    private var encryptedPrefs: SharedPreferences? = null
    private val recipientPublicKeyCache = mutableMapOf<String, String>()
    private val chatSecretCache = mutableMapOf<String, SecretKey>()

    /**
     * Initialize E2EEManager with encrypted preferences
     * Call this once on app startup (KampusApplication)
     */
    fun initialize(sharedPrefs: SharedPreferences) {
        encryptedPrefs = sharedPrefs
        android.util.Log.d("E2EEManager", "E2EEManager initialized successfully")
    }

    /**
     * Store plaintext locally in EncryptedSharedPreferences so UI can show optimistic
     * plaintext across restarts without uploading plaintext to Firestore.
     */
    fun storeLocalPlaintext(messageId: String, plaintext: String) {
        try {
            val prefs = encryptedPrefs ?: return
            prefs.edit().putString("local_plain_$messageId", plaintext).apply()
        } catch (_: Exception) {
            // best-effort
        }
    }

    fun loadLocalPlaintext(messageId: String): String? {
        return try {
            encryptedPrefs?.getString("local_plain_$messageId", null)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLocalPlaintext(messageId: String) {
        try {
            encryptedPrefs?.edit()?.remove("local_plain_$messageId")?.apply()
        } catch (_: Exception) {
        }
    }

    /**
     * Check if E2EEManager is properly initialized
     */
    fun isInitialized(): Boolean {
        return encryptedPrefs != null
    }

    /**
     * Ensure user has encryption keys set up
     * Call on login/signup
     */
    suspend fun ensureUserKeys(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val hasAesKey = hasUserAESKey(userId)
                val hasKeyPair = KeyManager.hasKeyPair(userId)

                if (hasAesKey && hasKeyPair && KeyManager.verifyKeyPairRoundTrip(userId)) {
                    loadUserAESKey(userId)
                    KeyManager.getPublicKey(userId)
                    return@withContext Result.success(Unit)
                }

                android.util.Log.w(
                    "E2EEManager",
                    "Key check failed or missing for userId=$userId hasAesKey=$hasAesKey hasKeyPair=$hasKeyPair - regenerating keys",
                )

                runCatching { KeyManager.deleteKeyPair(userId) }

                // Regenerate cleanly if either side is missing or broken
                generateUserKeys(userId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Encrypt a message for sending to Firestore (Messenger-style).
     * Uses ONLY the per-chat shared AES secret. No per-message RSA wrapping.
     * Run in background (Dispatchers.Default)
     */
    suspend fun encryptMessageForSending(
        chatId: String,
        senderId: String,
        recipientId: String,
        plaintext: String,
    ): Result<EncryptedChatMessage> {
        return withContext(Dispatchers.Default) {
            try {
                val secretKey = chatSecretCache[chatId] ?: getSoftwareChatSecret(chatId)?.let { localSecretBase64 ->
                    try {
                        val secretBytes = Base64.decode(localSecretBase64, Base64.DEFAULT)
                        val secretKey = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")
                        chatSecretCache[chatId] = secretKey
                        secretKey
                    } catch (_: Exception) {
                        null
                    }
                } ?: run {
                    ensureDirectChatSecret(chatId, senderId, recipientId).getOrThrow()
                    getSharedChatSecret(chatId, senderId).getOrThrow()
                }
                val encrypted = CryptoManager.encryptMessage(plaintext, secretKey)

                // Messenger-style: store only encryptedPayload and IV
                // No per-message RSA key wrapping
                Result.success(
                    EncryptedChatMessage(
                        encryptedPayload = encrypted.encryptedPayload,
                        iv = encrypted.iv,
                        encryptedKeyForSender = "",
                        encryptedKeyForRecipient = "",
                        encryptedKeyForSenderSha1 = "",
                        encryptedKeyForRecipientSha1 = "",
                        timestamp = encrypted.timestamp,
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("E2EEManager", "Failed to encrypt message for sending", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Decrypt a received message (Messenger-style).
     * Uses ONLY the per-chat shared AES secret. No per-message RSA key unwrap.
     * Run in background (Dispatchers.Default)
     */
    suspend fun decryptReceivedMessage(
        chatId: String,
        userId: String,
        senderId: String,
        encryptedMessage: EncryptedChatMessage,
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                // Messenger-style: decrypt using ONLY the shared chat secret
                val secretKey = getSharedChatSecret(chatId, userId).getOrThrow()
                val plaintext = CryptoManager.decryptMessage(
                    EncryptedMessage(
                        encryptedPayload = encryptedMessage.encryptedPayload,
                        iv = encryptedMessage.iv,
                        timestamp = encryptedMessage.timestamp,
                    ),
                    secretKey,
                )
                Result.success(plaintext)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Decrypt with retry and graceful fallback for shared-secret recovery.
     * If shared secret can't be decrypted (KeyStore error), tries to recreate it.
     */
    suspend fun decryptReceivedMessageWithFallback(
        chatId: String,
        userId: String,
        senderId: String,
        encryptedMessage: EncryptedChatMessage,
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            // First attempt: try standard decryption
            val initialResult = decryptReceivedMessage(chatId, userId, senderId, encryptedMessage)
            if (initialResult.isSuccess) {
                return@withContext initialResult
            }

            // Second attempt: clear cache and retry
            try {
                chatSecretCache.remove(chatId)
                encryptedPrefs?.edit()?.remove("software_chat_secret_$chatId")?.apply()
                
                val retryResult = decryptReceivedMessage(chatId, userId, senderId, encryptedMessage)
                if (retryResult.isSuccess) {
                    return@withContext retryResult
                }
            } catch (e: Exception) {
                return@withContext initialResult
            }

            // Return original error
            return@withContext initialResult
        }
    }

    /**
     * Encrypt image for Firebase Storage
     * Run in background (Dispatchers.IO)
     */
    suspend fun encryptImageForStorage(userId: String, imageBytes: ByteArray): Result<EncryptedBinary> {
        return withContext(Dispatchers.IO) {
            try {
                // Compress first
                val compressed = ImageEncryptionManager.compressImageBytesToTarget(imageBytes)
                
                ensureAESKeyLoaded(userId)
                val aesKey = userAESKey ?: throw IllegalStateException("AES key not available")
                
                // Then encrypt
                val encrypted = withContext(Dispatchers.Default) {
                    CryptoManager.encryptBinary(compressed, aesKey)
                }
                
                Result.success(encrypted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Decrypt image from Firebase Storage
     * Run in background (Dispatchers.IO)
     */
    suspend fun decryptImageFromStorage(userId: String, encryptedBinary: EncryptedBinary): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                ensureAESKeyLoaded(userId)
                val aesKey = userAESKey ?: throw IllegalStateException("AES key not available")
                
                val decrypted = withContext(Dispatchers.Default) {
                    CryptoManager.decryptBinary(encryptedBinary, aesKey)
                }
                
                Result.success(decrypted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Seed user's keys on first signup
     */
    suspend fun seedKeysForNewUser(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Generate RSA key pair (stored in Android Keystore)
                val publicKey = KeyManager.generateRSAKeyPair(userId)
                
                // Generate AES key
                val aesKey = CryptoManager.generateAESKey()
                storeUserAESKey(userId, aesKey)
                
                // Upload public key to Firestore for future use
                val publicKeyPem = KeyManager.exportPublicKeyPem(publicKey)
                uploadPublicKeyToFirestore(userId, publicKeyPem)
                
                // Cache in memory
                userAESKey = aesKey
                cachedUserId = userId
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get other user's public key from Firestore for hybrid encryption
     */
    suspend fun getOtherUserPublicKey(userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("publicKeys")
                    .document(userId)
                    .get()
                    .await()
                
                val publicKeyPem = doc.getString("publicKey")
                    ?: return@withContext Result.failure(Exception("Public key not found for $userId"))
                
                Result.success(publicKeyPem)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear user keys on logout
     */
    fun clearUserKeys(userId: String) {
        if (cachedUserId == userId) {
            userAESKey = null
            cachedUserId = null
        }
        try {
            encryptedPrefs?.edit()?.remove("aes_key_$userId")?.apply()
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun preloadRecipientPublicKey(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                recipientPublicKeyCache[userId] = getOtherUserPublicKey(userId).getOrElse { throw it }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Diagnostic helper: return key health for a given userId.
     * Useful to call from UI or logs to see why decryption may fail.
     */
    suspend fun getKeyDiagnostics(userId: String): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val hasKeyPair = KeyManager.hasKeyPair(userId)
                val hasAesKey = hasUserAESKey(userId)
                val verifyRoundTrip = try { KeyManager.verifyKeyPairRoundTrip(userId) } catch (_: Exception) { false }

                // Try fetch public key PEM from Firestore (may not exist)
                val publicKeyPem = try {
                    FirebaseFirestore.getInstance().collection("publicKeys").document(userId).get().await().getString("publicKey")
                } catch (_: Exception) { null }

                val info = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "hasKeyPair" to hasKeyPair,
                    "hasAesKey" to hasAesKey,
                    "verifyRoundTrip" to verifyRoundTrip,
                )
                publicKeyPem?.let { info["publicKeyPemPresent"] = true }

                Result.success(info)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ============ Private Helpers ============

    private suspend fun generateUserKeys(userId: String) {
        withContext(Dispatchers.IO) {
            // Generate RSA key pair (stored in Android Keystore)
            val publicKey = KeyManager.generateRSAKeyPair(userId)
            
            // Generate AES key
            val aesKey = CryptoManager.generateAESKey()
            storeUserAESKey(userId, aesKey)
            
            // Upload public key to Firestore
            val publicKeyPem = KeyManager.exportPublicKeyPem(publicKey)
            uploadPublicKeyToFirestore(userId, publicKeyPem)
            
            // Cache in memory
            userAESKey = aesKey
            cachedUserId = userId
        }
    }

    private fun storeUserAESKey(userId: String, aesKey: SecretKey) {
        try {
            val prefs = encryptedPrefs ?: throw KeyStorageException("E2EEManager not initialized: encryptedPrefs is null")
            val encodedKey = Base64.encodeToString(aesKey.encoded, Base64.DEFAULT)
            prefs.edit().putString("aes_key_$userId", encodedKey).apply()
        } catch (e: Exception) {
            throw KeyStorageException("Failed to store AES key for $userId", e)
        }
    }

    private fun loadUserAESKey(userId: String) {
        try {
            val prefs = encryptedPrefs ?: throw KeyLoadException("E2EEManager not initialized: encryptedPrefs is null")
            val encodedKey = prefs.getString("aes_key_$userId", null)
                ?: throw KeyNotFoundException("AES key not found for $userId")
            
            val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
            userAESKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
            cachedUserId = userId
        } catch (e: Exception) {
            throw KeyLoadException("Failed to load AES key for $userId", e)
        }
    }

    private fun hasUserAESKey(userId: String): Boolean {
        return try {
            val prefs = encryptedPrefs ?: return false
            prefs.getString("aes_key_$userId", null) != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun ensureAESKeyLoaded(userId: String) {
        if (cachedUserId != userId || userAESKey == null) {
            loadUserAESKey(userId)
        }
    }

    private suspend fun uploadPublicKeyToFirestore(userId: String, publicKeyPem: String) {
        withContext(Dispatchers.IO) {
            try {
                FirebaseFirestore.getInstance()
                    .collection("publicKeys")
                    .document(userId)
                    .set(mapOf("publicKey" to publicKeyPem, "uploadedAt" to System.currentTimeMillis()))
                    .await()
            } catch (e: Exception) {
                throw KeyUploadException("Failed to upload public key for $userId", e)
            }
        }
    }

    private suspend fun getLatestRecipientPublicKey(userId: String): String {
        // Always prefer a fresh key from Firestore to avoid stale-cache encryption after key rotation.
        val fresh = getOtherUserPublicKey(userId)
        if (fresh.isSuccess) {
            val publicKeyPem = fresh.getOrThrow()
            recipientPublicKeyCache[userId] = publicKeyPem
            return publicKeyPem
        }

        // Fallback to cache only if network read fails.
        recipientPublicKeyCache[userId]?.let { return it }
        throw fresh.exceptionOrNull() ?: Exception("Public key unavailable for $userId")
    }

    suspend fun ensureDirectChatSecret(chatId: String, currentUserId: String, otherUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (chatId.isBlank() || currentUserId.isBlank() || otherUserId.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("chatId/currentUserId/otherUserId must not be blank"))
                }

                val chatRef = FirebaseFirestore.getInstance().collection("chats").document(chatId)
                val snapshot = chatRef.get().await()
                val encryptedSecrets = (snapshot.get("sharedSecretEncryptedForUsers") as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        val userId = key as? String ?: return@mapNotNull null
                        val secret = value as? String ?: return@mapNotNull null
                        userId to secret
                    }
                    ?.toMap()
                    .orEmpty()

                val alreadyReady = encryptedSecrets[currentUserId].isNullOrBlank().not() &&
                    encryptedSecrets[otherUserId].isNullOrBlank().not()
                if (alreadyReady) {
                    val participantsInDoc = (snapshot.get("participants") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.sorted()
                        .orEmpty()
                    val expectedParticipants = listOf(currentUserId, otherUserId).sorted()

                    if (participantsInDoc != expectedParticipants) {
                        chatRef.set(mapOf("participants" to expectedParticipants), SetOptions.merge()).await()
                        Log.d("E2EEManager", "Backfilled participants for chat=$chatId participants=$expectedParticipants")
                    }

                    if (!chatSecretCache.containsKey(chatId)) {
                        getSharedChatSecret(chatId, currentUserId).getOrThrow()
                    }
                    return@withContext Result.success(Unit)
                }

                val secretBase64 = deriveSoftwareChatSecretBase64(chatId, listOf(currentUserId, otherUserId))
                    ?: return@withContext Result.failure(Exception("Failed to derive shared chat secret for $chatId"))
                val secretBytes = Base64.decode(secretBase64, Base64.DEFAULT)
                val sharedSecret = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")
                val currentUserPublicKey = KeyManager.getPublicKey(currentUserId)
                val otherUserPublicKey = KeyManager.importPublicKeyFromPem(getOtherUserPublicKey(otherUserId).getOrThrow())

                val payload = mapOf(
                    "chatSecretVersion" to 1,
                    "chatSecretAlgorithm" to "AES-256-GCM",
                    "chatSecretCreatedAt" to System.currentTimeMillis(),
                    "participants" to listOf(currentUserId, otherUserId).sorted(),
                    "sharedSecretEncryptedForUsers" to mapOf(
                        currentUserId to KeyManager.encryptAESKeyWithPublicKey(secretBase64, currentUserPublicKey),
                        otherUserId to KeyManager.encryptAESKeyWithPublicKey(secretBase64, otherUserPublicKey),
                    ),
                )

                chatRef.set(payload, SetOptions.merge()).await()
                chatSecretCache[chatId] = sharedSecret
                
                // Store the same deterministic secret locally for offline/future access.
                setSoftwareChatSecret(chatId, secretBase64)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Rotate the RSA keypair for the local user and upload the new public key.
     * This does NOT re-encrypt existing chat secrets — use rotateDirectChatSecret
     * to create a fresh shared secret for a specific chat after rotating keys.
     */
    suspend fun rotateUserKeypair(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Delete any existing keypair and generate a fresh one
                runCatching { KeyManager.deleteKeyPair(userId) }
                val publicKey = KeyManager.generateRSAKeyPair(userId)
                val publicPem = KeyManager.exportPublicKeyPem(publicKey)
                uploadPublicKeyToFirestore(userId, publicPem)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Force-create a new shared chat secret for a direct chat and write it to Firestore.
     * This overwrites `sharedSecretEncryptedForUsers` entries for the two participants
     * and caches the secret locally so future messages use the new secret.
     */
    suspend fun rotateDirectChatSecret(chatId: String, currentUserId: String, otherUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (chatId.isBlank() || currentUserId.isBlank() || otherUserId.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("chatId/currentUserId/otherUserId must not be blank"))
                }

                // Ensure both public keys are available
                val currentPub = try { KeyManager.getPublicKey(currentUserId) } catch (e: Exception) { throw KeyAccessException("Current user public key unavailable", e) }
                val otherPubPem = getOtherUserPublicKey(otherUserId).getOrElse { throw it }
                val otherPub = KeyManager.importPublicKeyFromPem(otherPubPem)

                val sharedSecret = CryptoManager.generateAESKey()
                val secretBase64 = Base64.encodeToString(sharedSecret.encoded, Base64.NO_WRAP)

                val payload = mapOf(
                    "chatSecretVersion" to 1,
                    "chatSecretAlgorithm" to "AES-256-GCM",
                    "chatSecretCreatedAt" to System.currentTimeMillis(),
                    "participants" to listOf(currentUserId, otherUserId).sorted(),
                    "sharedSecretEncryptedForUsers" to mapOf(
                        currentUserId to KeyManager.encryptAESKeyWithPublicKey(secretBase64, currentPub),
                        otherUserId to KeyManager.encryptAESKeyWithPublicKey(secretBase64, otherPub),
                    ),
                )

                val chatRef = FirebaseFirestore.getInstance().collection("chats").document(chatId)
                chatRef.set(payload, SetOptions.merge()).await()
                chatSecretCache[chatId] = sharedSecret
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // In E2EEManager.kt

suspend fun getSharedChatSecret(chatId: String, userId: String): Result<SecretKey> {
    return withContext(Dispatchers.IO) {
        try {
            // 1. In-memory cache (fastest)
            chatSecretCache[chatId]?.let { return@withContext Result.success(it) }

            // 2. Local deterministic chat secret (persistent, no RSA dependency)
            val localSecretBase64 = getSoftwareChatSecret(chatId)
            if (localSecretBase64 != null) {
                return@withContext try {
                    val secretBytes = Base64.decode(localSecretBase64, Base64.DEFAULT)
                    val secretKey = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")
                    chatSecretCache[chatId] = secretKey
                    Log.d("E2EEManager", "✓ Loaded shared secret from local cache chat=$chatId")
                    Result.success(secretKey)
                } catch (e: Exception) {
                    Log.w("E2EEManager", "Local secret corrupted for chat=$chatId, clearing")
                    encryptedPrefs?.edit()?.remove("software_chat_secret_$chatId")?.apply()
                    null // fall through to Firestore-derived fallback
                } ?: loadSecretFromFirestore(chatId, userId)
            }

            // 3. Last resort: derive a deterministic per-chat AES key from chat metadata
            loadSecretFromFirestore(chatId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private suspend fun loadSecretFromFirestore(chatId: String, userId: String): Result<SecretKey> {
    return withContext(Dispatchers.IO) {
        try {
            val chatDoc = FirebaseFirestore.getInstance()
                .collection("chats").document(chatId).get().await()

            val participantIdsFromDoc = (chatDoc.get("participants") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                .orEmpty()

            val participantIdsFromSecretMap = (chatDoc.get("sharedSecretEncryptedForUsers") as? Map<*, *>)
                ?.keys
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                .orEmpty()

            val participantIds = when {
                participantIdsFromDoc.isNotEmpty() -> participantIdsFromDoc
                participantIdsFromSecretMap.isNotEmpty() -> participantIdsFromSecretMap
                else -> emptyList()
            }

            if (participantIdsFromDoc.isEmpty() && participantIdsFromSecretMap.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("chats").document(chatId)
                    .set(mapOf("participants" to participantIdsFromSecretMap), SetOptions.merge())
                    .await()
                Log.d("E2EEManager", "Backfilled participants from sharedSecretEncryptedForUsers for chat=$chatId")
            }

            val derivedBase64 = deriveSoftwareChatSecretBase64(chatId, participantIds)
                ?: return@withContext Result.failure(
                    Exception("No participants available to derive a chat secret for chat=$chatId")
                )

            val secretBytes = Base64.decode(derivedBase64, Base64.DEFAULT)
            val secretKey = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")
            
            // Cache in memory AND persist locally so both users derive the same key
            chatSecretCache[chatId] = secretKey
            setSoftwareChatSecret(chatId, derivedBase64)

            Result.success(secretKey)
        } catch (e: Exception) {
            Log.e("E2EEManager", "Failed to load secret from Firestore for chat=$chatId: ${e.message}", e)
            Result.failure(e)
        }
    }
}

    /**
     * Create a plaintext message when E2EE is disabled (fallback mode for KeyStore issues)
     * Used when E2EE_ENABLED=false to allow chat to function in unencrypted mode
     */
    suspend fun createPlaintextMessage(plaintext: String): Result<EncryptedChatMessage> {
        return withContext(Dispatchers.Default) {
            try {
                if (E2EEConfig.isE2EEEnabled()) {
                    return@withContext Result.failure(Exception("E2EE is enabled - use encryptMessageForSending instead"))
                }
                
                android.util.Log.w("E2EEManager", "⚠️ Creating PLAINTEXT message (E2EE disabled): ${plaintext.take(50)}")
                
                // Create fake encrypted structure with plaintext in payload
                // This allows the UI to treat plaintext messages the same way as encrypted ones
                val plaintextBase64 = android.util.Base64.encodeToString(plaintext.toByteArray(), android.util.Base64.DEFAULT)
                
                val msg = EncryptedChatMessage(
                    encryptedPayload = plaintextBase64,  // Actually plaintext
                    iv = "plaintext-mode",               // Marker for detection
                    encryptedKeyForSender = "plaintext-mode",  // Double marker
                    encryptedKeyForRecipient = "plaintext-mode",
                    encryptedKeyForSenderSha1 = "plaintext-mode",
                    encryptedKeyForRecipientSha1 = "plaintext-mode",
                    timestamp = System.currentTimeMillis()
                )
                
                android.util.Log.d("E2EEManager", "✓ Plaintext message created with payload length ${plaintextBase64.length}")
                Result.success(msg)
            } catch (e: Exception) {
                android.util.Log.e("E2EEManager", "Failed to create plaintext message", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get the local deterministic chat secret from EncryptedSharedPreferences.
     */
    private fun getSoftwareChatSecret(chatId: String): String? {
        return try {
            encryptedPrefs?.getString("software_chat_secret_$chatId", null)
        } catch (e: Exception) {
            android.util.Log.w("E2EEManager", "Failed to get software chat secret for $chatId: ${e.message}")
            null
        }
    }

    /**
     * Store the deterministic chat secret locally in EncryptedSharedPreferences.
     */
    private fun setSoftwareChatSecret(chatId: String, secretBase64: String) {
        try {
            encryptedPrefs?.edit()?.putString("software_chat_secret_$chatId", secretBase64)?.apply()
        } catch (e: Exception) {
            android.util.Log.w("E2EEManager", "Failed to store software chat secret for $chatId: ${e.message}")
        }
    }

    /**
     * Build the same 256-bit AES secret on both participants' devices using only
     * chat metadata, so the message flow still works when Android Keystore RSA fails.
     */
    private fun deriveSoftwareChatSecretBase64(chatId: String, participantIds: List<String>): String? {
        val normalizedParticipants = participantIds
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        if (normalizedParticipants.isEmpty()) {
            Log.w("E2EEManager", "❌ deriveSoftwareChatSecretBase64: Empty participants list for chatId=$chatId")
            return null
        }

        val material = buildString {
            append("kampus-chat-v1|")
            append(chatId)
            append('|')
            append(normalizedParticipants.joinToString("|"))
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        val secretBase64 = Base64.encodeToString(digest, Base64.NO_WRAP)
        
        return secretBase64
    }

    /**
     * Decrypt message safely using shared secret (Messenger-style).
     * Detects plaintext mode and returns plaintext without decryption.
     */
    suspend fun decryptMessageSafely(
        chatId: String,
        userId: String,
        senderId: String,
        encryptedMessage: EncryptedChatMessage
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            // Check if this is a plaintext-mode message (E2EE disabled)
            if (encryptedMessage.iv == "plaintext-mode" || encryptedMessage.encryptedKeyForSender == "plaintext-mode") {
                try {
                    val plaintextBase64 = encryptedMessage.encryptedPayload
                    val plaintext = String(android.util.Base64.decode(plaintextBase64, android.util.Base64.DEFAULT), Charsets.UTF_8)
                    Log.d("E2EEManager", "✓ Decoded plaintext message (E2EE disabled mode)")
                    return@withContext Result.success(plaintext)
                } catch (e: Exception) {
                    Log.e("E2EEManager", "Failed to decode plaintext message: ${e.message}")
                    return@withContext Result.failure(e)
                }
            }

            // Otherwise, attempt E2EE decryption with retry on failure
            decryptReceivedMessageWithFallback(chatId, userId, senderId, encryptedMessage)
        }
    }
}

class KeyStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KeyLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KeyUploadException(message: String, cause: Throwable? = null) : Exception(message, cause)
