package com.example.kampus.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

/**
 * CryptoManager: Handles AES-256-GCM encryption/decryption for E2E chat
 * - Encrypts plaintext before sending to Firebase
 * - Decrypts ciphertext on receiver device only
 * - Uses SecureRandom IV for each encryption (non-deterministic)
 * - Output: Base64-encoded strings safe for Firestore storage
 */
object CryptoManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private val random = java.security.SecureRandom()

    /**
     * Generate a new AES-256 secret key for symmetric encryption
     * In production, this would be generated once per user and stored in Android Keystore
     */
    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE, random)
        return keyGen.generateKey()
    }

    /**
     * Encrypt plaintext message using AES-256-GCM
     * Returns: Base64-encoded encrypted payload suitable for Firestore storage
     * 
     * Format: Base64(IV || Ciphertext)
     * IV is 12 bytes of SecureRandom data
     * Ciphertext includes both encrypted data and authentication tag
     */
    fun encryptMessage(plaintext: String, secretKey: SecretKey): EncryptedMessage {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
            val ciphertextBytes = cipher.doFinal(plaintextBytes)

            // Combine IV + Ciphertext
            val combined = iv + ciphertextBytes
            val encryptedPayload = Base64.encodeToString(combined, Base64.DEFAULT)
            val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)

            EncryptedMessage(
                encryptedPayload = encryptedPayload,
                iv = ivBase64,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt message", e)
        }
    }

    /**
     * Decrypt ciphertext using AES-256-GCM
     * Expects Base64(IV || Ciphertext) format
     * Only the recipient with the matching private key can decrypt
     */
    fun decryptMessage(encryptedMessage: EncryptedMessage, secretKey: SecretKey): String {
        return try {
            val combined = Base64.decode(encryptedMessage.encryptedPayload, Base64.DEFAULT)
            
            // Extract IV (first 12 bytes)
            val iv = combined.sliceArray(0 until IV_LENGTH_BYTES)
            // Extract ciphertext (remaining bytes)
            val ciphertext = combined.sliceArray(IV_LENGTH_BYTES until combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw DecryptionException("Failed to decrypt message", e)
        }
    }

    /**
     * Encrypt binary data (images, files) using AES-256-GCM
     * Returns base64-encoded encrypted bytes for Firebase Storage
     */
    fun encryptBinary(data: ByteArray, secretKey: SecretKey): EncryptedBinary {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val ciphertextBytes = cipher.doFinal(data)
            val combined = iv + ciphertextBytes
            val encryptedPayload = Base64.encodeToString(combined, Base64.DEFAULT)
            val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)

            EncryptedBinary(
                encryptedPayload = encryptedPayload,
                iv = ivBase64,
                originalSize = data.size
            )
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt binary data", e)
        }
    }

    /**
     * Decrypt binary data using AES-256-GCM
     */
    fun decryptBinary(encryptedBinary: EncryptedBinary, secretKey: SecretKey): ByteArray {
        return try {
            val combined = Base64.decode(encryptedBinary.encryptedPayload, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until IV_LENGTH_BYTES)
            val ciphertext = combined.sliceArray(IV_LENGTH_BYTES until combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw DecryptionException("Failed to decrypt binary data", e)
        }
    }
}

data class EncryptedChatMessage(
    val encryptedPayload: String,
    val iv: String,
    val encryptedKeyForSender: String,
    val encryptedKeyForRecipient: String,
    // Backward/compat fallback: also store AES key encrypted using OAEP-SHA1
    val encryptedKeyForSenderSha1: String = "",
    val encryptedKeyForRecipientSha1: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class EncryptedMessage(
    val encryptedPayload: String,  // Base64-encoded IV + Ciphertext
    val iv: String,                 // Base64-encoded IV (redundant but useful for debugging)
    val timestamp: Long = System.currentTimeMillis()
)

data class EncryptedBinary(
    val encryptedPayload: String,   // Base64-encoded IV + Ciphertext
    val iv: String,                 // Base64-encoded IV
    val originalSize: Int           // Original byte size before encryption
)

class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
