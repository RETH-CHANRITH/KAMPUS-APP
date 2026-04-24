package com.example.kampus.data.remote

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseStorageManager(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {

    companion object {
        private const val PROFILE_BUCKET = "profiles"
        private const val COVER_BUCKET = "covers"
        private const val SUPABASE_PROJECT_ID = "wcygigxevxohizwstkfg"
        private const val SUPABASE_URL = "https://$SUPABASE_PROJECT_ID.supabase.co"
    }

    /**
     * Construct public URL for uploaded image
     */
    private fun getPublicUrl(bucket: String, filePath: String): String {
        return "$SUPABASE_URL/storage/v1/object/public/$bucket/$filePath"
    }

    /**
     * Upload profile picture to Supabase Storage
     * @param userId User ID for organizing files
     * @param imageUri URI of the image to upload
     * @return URL of uploaded image or null if failed
     */
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Failed to open image stream")

                val byteArray = inputStream.readBytes()
                inputStream.close()

                // Upload to Supabase Storage
                supabaseClient.storage
                    .from(PROFILE_BUCKET)
                    .upload("$userId/$fileName", byteArray)

                // Construct public URL
                val imageUrl = getPublicUrl(PROFILE_BUCKET, "$userId/$fileName")

                Result.success(imageUrl)
            } catch (e: Exception) {
                Result.failure(Exception("Profile image upload failed: ${e.message}"))
            }
        }

    /**
     * Upload cover image to Supabase Storage
     * @param userId User ID for organizing files
     * @param imageUri URI of the image to upload
     * @return URL of uploaded image or null if failed
     */
    suspend fun uploadCoverImage(userId: String, imageUri: Uri): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val fileName = "cover_${userId}_${System.currentTimeMillis()}.jpg"
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Failed to open image stream")

                val byteArray = inputStream.readBytes()
                inputStream.close()

                // Upload to Supabase Storage
                supabaseClient.storage
                    .from(COVER_BUCKET)
                    .upload("$userId/$fileName", byteArray)

                // Construct public URL
                val imageUrl = getPublicUrl(COVER_BUCKET, "$userId/$fileName")

                Result.success(imageUrl)
            } catch (e: Exception) {
                Result.failure(Exception("Cover image upload failed: ${e.message}"))
            }
        }

    /**
     * Delete old image from Supabase Storage
     * @param bucket Bucket name (profiles or covers)
     * @param filePath File path to delete
     */
    suspend fun deleteImage(bucket: String, filePath: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.storage
                    .from(bucket)
                    .delete(filePath)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Delete image failed: ${e.message}"))
            }
        }
}
