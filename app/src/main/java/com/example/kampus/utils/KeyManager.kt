package com.example.kampus.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * KeyManager: Handles RSA-2048 key pair generation and management
 *
 * Architecture:
 * 1. Generate RSA-2048 key pair on first user creation
 * 2. Store private key in Android Keystore (hardware-backed if available)
 * 3. Upload public key to Firestore at `publicKeys/{userId}`
 * 4. Fetch other users' public keys for encryption
 * 5. Use RSA-OAEP to encrypt per-message AES keys (hybrid encryption)
 *
 * Privacy Model:
 * - Private key NEVER leaves device
 * - Firebase never has access to private keys
 * - Messages encrypted with recipient's public key
 * - Only recipient can decrypt with their private key
 */
object KeyManager {

    private const val KEYSTORE_ALIAS_PREFIX = "kampus_rsa_"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val RSA_KEY_SIZE = 2048
    private const val RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val RSA_ALGORITHM_SHA1 = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Generate a new RSA-2048 key pair for a user
     * Stores in Android Keystore (secure enclave when available)
     * Should be called once on user signup
     */
    fun generateRSAKeyPair(userId: String): PublicKey {
        return try {
            val alias = getKeyAlias(userId)
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    generateWithStrongBox(alias)
                } catch (_: Exception) {
                    generateWithoutStrongBox(alias)
                }
            } else {
                generateWithoutStrongBox(alias)
            }
        } catch (e: Exception) {
            throw KeyGenerationException("Failed to generate RSA key pair for $userId", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateWithStrongBox(alias: String): PublicKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(RSA_KEY_SIZE)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)  // Support both SHA256 and SHA1
            .setIsStrongBoxBacked(true)   // hardware-backed when available
            .build()
        val kg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
        kg.initialize(spec)
        return kg.generateKeyPair().public
    }

    private fun generateWithoutStrongBox(alias: String): PublicKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(RSA_KEY_SIZE)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)  // Support both SHA256 and SHA1
            // No StrongBox — works on all devices including emulators
            .build()
        val kg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
        kg.initialize(spec)
        return kg.generateKeyPair().public
    }

    /**
     * Get user's private key from Android Keystore
     * Should only be called on user's own device
     */
    fun getPrivateKey(userId: String): PrivateKey {
        return try {
            val alias = getKeyAlias(userId)
            val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
                ?: throw KeyNotFoundException("Private key not found for $userId")
            entry.privateKey
        } catch (e: Exception) {
            throw KeyAccessException("Failed to access private key for $userId", e)
        }
    }

    /**
     * Get user's public key from Android Keystore or cache
     */
    fun getPublicKey(userId: String): PublicKey {
        return try {
            val alias = getKeyAlias(userId)
            val certificate = keyStore.getCertificate(alias)
            certificate?.publicKey
                ?: throw PublicKeyNotFound("Public key certificate not found for $userId")
        } catch (e: Exception) {
            throw KeyAccessException("Failed to access public key for $userId", e)
        }
    }

    /**
     * Export public key as PEM-encoded string for Firestore storage
     */
    fun exportPublicKeyPem(publicKey: PublicKey): String {
        return try {
            val encoded = publicKey.encoded
            val base64 = Base64.encodeToString(encoded, Base64.DEFAULT)
            val pem = StringBuilder()
            pem.append("-----BEGIN PUBLIC KEY-----\n")
            var index = 0
            while (index < base64.length) {
                pem.append(base64.substring(index, minOf(index + 64, base64.length)))
                pem.append("\n")
                index += 64
            }
            pem.append("-----END PUBLIC KEY-----")
            pem.toString()
        } catch (e: Exception) {
            throw KeyExportException("Failed to export public key as PEM", e)
        }
    }

    /**
     * Import public key from Firestore (PEM-encoded)
     */
    fun importPublicKeyFromPem(pemString: String): PublicKey {
        return try {
            val cleaned = pemString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
                .trim()

            val decoded = Base64.decode(cleaned, Base64.DEFAULT)

            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val keySpec = java.security.spec.X509EncodedKeySpec(decoded)
            keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            throw KeyImportException("Failed to import public key from PEM", e)
        }
    }

    /**
     * Encrypt per-message AES key using recipient's public key (hybrid encryption)
     * Receiver can decrypt with their private key
     */
    fun encryptAESKeyWithPublicKey(aesKeyBase64: String, recipientPublicKey: PublicKey): String {
        return try {
            encryptAESKeyWithPublicKeyVariant(aesKeyBase64, recipientPublicKey, RSA_ALGORITHM)
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt AES key with public key", e)
        }
    }

    fun encryptAESKeyWithPublicKeyVariant(aesKeyBase64: String, recipientPublicKey: PublicKey, algorithm: String): String {
        return try {
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
            val aesKeyBytes = Base64.decode(aesKeyBase64, Base64.DEFAULT)
            val encryptedBytes = cipher.doFinal(aesKeyBytes)
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt AES key with public key (alg=$algorithm)", e)
        }
    }

    /**
     * Decrypt AES key using user's private key (hybrid decryption)
     */
    fun decryptAESKeyWithPrivateKey(encryptedKeyBase64: String, userId: String): String {
        val privateKey = try {
            getPrivateKey(userId)
        } catch (e: Exception) {
            throw DecryptionException("Failed to access private key for $userId", e)
        }

        val encryptedBytes = try {
            Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            throw DecryptionException("Invalid base64 for encrypted AES key", e)
        }

        val algorithms = listOf(RSA_ALGORITHM, RSA_ALGORITHM_SHA1)
        val exceptions = mutableListOf<Exception>()

        for (alg in algorithms) {
            // Retry up to 3 times per algorithm — KeyStore2 UNKNOWN_ERROR is often transient
            repeat(3) { attempt ->
                try {
                    val cipher = Cipher.getInstance(alg) // fresh instance each time
                    cipher.init(Cipher.DECRYPT_MODE, privateKey)
                    val aesKeyBytes = cipher.doFinal(encryptedBytes)
                    return Base64.encodeToString(aesKeyBytes, Base64.DEFAULT)
                } catch (e: Exception) {
                    Log.w("KeyManager", "Decrypt attempt ${attempt+1} failed (alg=$alg): ${e.message}")
                    if (attempt == 2) exceptions.add(e)
                    Thread.sleep(50L * (attempt + 1)) // brief backoff
                }
            }
        }

        val root = DecryptionException(
            "Failed to decrypt AES key with private key after trying algorithms: $algorithms"
        )
        exceptions.forEach { root.addSuppressed(it) }
        throw root
    }

    /**
     * Check if user's key pair exists
     */
    fun hasKeyPair(userId: String): Boolean {
        return try {
            val alias = getKeyAlias(userId)
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete user's key pair (e.g., on account deletion)
     */
    fun deleteKeyPair(userId: String) {
        try {
            val alias = getKeyAlias(userId)
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            throw KeyDeletionException("Failed to delete key pair for $userId", e)
        }
    }

    /**
     * Verify the RSA key pair can still encrypt/decrypt a small payload.
     * This is a cheap health check used to repair broken or incompatible keys.
     */
    fun verifyKeyPairRoundTrip(userId: String): Boolean {
        return try {
            val publicKey = getPublicKey(userId)
            val testKeyBytes = ByteArray(32) { index -> (index + 1).toByte() }
            val testBase64 = Base64.encodeToString(testKeyBytes, Base64.NO_WRAP)
            val encrypted = encryptAESKeyWithPublicKey(testBase64, publicKey)
            val decryptedBase64 = decryptAESKeyWithPrivateKey(encrypted, userId)
            val decryptedBytes = Base64.decode(decryptedBase64, Base64.DEFAULT)
            SecretKeySpec(decryptedBytes, 0, decryptedBytes.size, "AES")
            decryptedBase64.trim() == testBase64.trim()
        } catch (_: Exception) {
            false
        }
    }

    private fun getKeyAlias(userId: String): String = "$KEYSTORE_ALIAS_PREFIX$userId"
}

class KeyGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KeyAccessException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KeyNotFoundException(message: String) : Exception(message)
class PublicKeyNotFound(message: String) : Exception(message)
class KeyExportException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KeyImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KeyDeletionException(message: String, cause: Throwable? = null) : Exception(message, cause)
