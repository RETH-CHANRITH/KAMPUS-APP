package com.example.kampus.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * ImageEncryptionManager: Handles image compression and encryption
 * 
 * Workflow:
 * 1. Compress image to target size (~500KB max) using quality reduction
 * 2. Encrypt compressed bytes using AES-256-GCM
 * 3. Upload encrypted blob to Firebase Storage
 * 4. Store reference + IV in Firestore
 * 5. On receive: Download → Decrypt → Display
 * 
 * Performance: Compression significantly reduces encryption payload
 */
object ImageEncryptionManager {

    private const val TARGET_MAX_SIZE_KB = 500
    private const val TARGET_MAX_SIZE_BYTES = TARGET_MAX_SIZE_KB * 1024
    private const val INITIAL_QUALITY = 90
    private const val MIN_QUALITY = 30

    /**
     * Compress image to target size
     * Returns Bitmap compressed to ≤ 500KB
     * Uses JPEG quality reduction algorithm
     */
    fun compressImageToTarget(bitmap: Bitmap): ByteArray {
        return try {
            var quality = INITIAL_QUALITY
            var compressedBytes: ByteArray

            do {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                compressedBytes = baos.toByteArray()

                quality -= 5
                if (quality < MIN_QUALITY) break
            } while (compressedBytes.size > TARGET_MAX_SIZE_BYTES && quality >= MIN_QUALITY)

            compressedBytes
        } catch (e: Exception) {
            throw ImageCompressionException("Failed to compress image", e)
        }
    }

    /**
     * Compress image from ByteArray (raw image file)
     * Decode → Compress → Return bytes
     */
    fun compressImageBytesToTarget(imageBytes: ByteArray): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: throw ImageCompressionException("Failed to decode image bytes")
            compressImageToTarget(bitmap)
        } catch (e: Exception) {
            if (e is ImageCompressionException) throw e
            throw ImageCompressionException("Failed to process image bytes", e)
        }
    }

    /**
     * Encrypt compressed image using AES-256-GCM
     * Returns EncryptedBinary with encrypted payload + IV
     */
    fun encryptImage(compressedImageBytes: ByteArray, aesKey: javax.crypto.SecretKey): EncryptedBinary {
        return CryptoManager.encryptBinary(compressedImageBytes, aesKey)
    }

    /**
     * Decrypt image using AES-256-GCM
     * Returns decompressed Bitmap
     */
    fun decryptImage(encryptedBinary: EncryptedBinary, aesKey: javax.crypto.SecretKey): Bitmap {
        return try {
            val decryptedBytes = CryptoManager.decryptBinary(encryptedBinary, aesKey)
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                ?: throw ImageDecryptionException("Failed to decode decrypted image bytes")
        } catch (e: Exception) {
            if (e is ImageDecryptionException) throw e
            throw ImageDecryptionException("Failed to decrypt image", e)
        }
    }

    /**
     * Calculate compression ratio for analytics
     */
    fun calculateCompressionRatio(originalSize: Int, compressedSize: Int): Float {
        return if (originalSize > 0) {
            (1.0f - (compressedSize.toFloat() / originalSize.toFloat())) * 100f
        } else 0f
    }
}

class ImageCompressionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ImageDecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
