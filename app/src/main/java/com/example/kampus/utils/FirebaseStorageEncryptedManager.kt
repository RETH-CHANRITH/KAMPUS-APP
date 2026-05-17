/**
 * FirebaseStorageEncryptedManager.kt
 * 
 * Handles encrypted media upload/download to Firebase Storage
 * 
 * Flow:
 * 1. Compress image → Encrypt with AES-256-GCM → Upload encrypted blob to Storage
 * 2. Download encrypted blob from Storage → Decrypt → Display to user
 * 3. Store reference path + IV in Firestore message
 */

package com.example.kampus.utils

import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseStorageEncryptedManager {

    private const val ENCRYPTED_MEDIA_PATH = "encrypted-media"

    /**
     * Upload encrypted image to Firebase Storage
     * 
     * Steps:
     * 1. Compress image to target size
     * 2. Encrypt compressed bytes with AES-256-GCM
     * 3. Upload encrypted blob to gs://bucket/encrypted-media/{userId}/{messageId}
     * 4. Return storage reference path + IV
     */
    suspend fun uploadEncryptedImage(
        userId: String,
        messageId: String,
        imageBytes: ByteArray,
        aesKey: javax.crypto.SecretKey,
    ): Result<EncryptedMediaReference> {
        return withContext(Dispatchers.IO) {
            try {
                // Compress first
                val compressedBytes = ImageEncryptionManager.compressImageBytesToTarget(imageBytes)

                // Encrypt in background
                val encryptedBinary = withContext(Dispatchers.Default) {
                    CryptoManager.encryptBinary(compressedBytes, aesKey)
                }

                // Convert to ByteArray for upload
                val encryptedBytes = android.util.Base64.decode(encryptedBinary.encryptedPayload, android.util.Base64.DEFAULT)

                // Upload to Firebase Storage
                val storagePath = "$ENCRYPTED_MEDIA_PATH/$userId/$messageId"
                val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)

                storageRef.putBytes(encryptedBytes).await()

                Result.success(EncryptedMediaReference(
                    storagePath = storagePath,
                    iv = encryptedBinary.iv,
                    timestamp = System.currentTimeMillis(),
                    originalSize = imageBytes.size
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Download and decrypt image from Firebase Storage
     * 
     * Steps:
     * 1. Download encrypted blob from Storage
     * 2. Decrypt with AES-256-GCM using IV
     * 3. Return Bitmap for display
     */
    suspend fun downloadDecryptedImage(
        mediaReference: EncryptedMediaReference,
        aesKey: javax.crypto.SecretKey,
    ): Result<Bitmap> {
        return withContext(Dispatchers.IO) {
            try {
                // Download encrypted bytes
                val storageRef = FirebaseStorage.getInstance().reference.child(mediaReference.storagePath)
                val encryptedBytes = storageRef.getBytes(Long.MAX_VALUE).await()

                // Convert to EncryptedBinary format
                val encryptedPayload = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT)
                val encryptedBinary = EncryptedBinary(
                    encryptedPayload = encryptedPayload,
                    iv = mediaReference.iv,
                    originalSize = mediaReference.originalSize
                )

                // Decrypt in background
                val bitmap = withContext(Dispatchers.Default) {
                    ImageEncryptionManager.decryptImage(encryptedBinary, aesKey)
                }

                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete encrypted media from Firebase Storage
     * Call when message is deleted or conversation cleared
     */
    suspend fun deleteEncryptedMedia(storagePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                FirebaseStorage.getInstance().reference.child(storagePath).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get file size of encrypted media in storage
     * Useful for download progress tracking
     */
    suspend fun getEncryptedMediaSize(storagePath: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = FirebaseStorage.getInstance().reference.child(storagePath).metadata.await()
                Result.success(metadata.sizeBytes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * Reference to encrypted media in Firebase Storage
 * Stored in Firestore message doc
 */
data class EncryptedMediaReference(
    val storagePath: String,           // gs://bucket/encrypted-media/{userId}/{messageId}
    val iv: String,                     // Base64-encoded IV for decryption
    val timestamp: Long = System.currentTimeMillis(),
    val originalSize: Int = 0           // Original file size before compression
)
