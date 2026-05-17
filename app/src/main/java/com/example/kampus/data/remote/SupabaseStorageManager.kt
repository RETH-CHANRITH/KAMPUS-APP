package com.example.kampus.data.remote

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SupabaseStorageManager(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {

    companion object {
        private const val PROFILE_BUCKET = "profiles"
        private const val COVER_BUCKET = "covers"
        private const val EVENTS_BUCKET = "events"
        private const val CHAT_VOICE_BUCKET = "chat-voices"
        private const val STORY_MEDIA_BUCKET = "story-media"
        // New bucket for comment images — create this in Supabase and make public
        private const val COMMENT_MEDIA_BUCKET = "comment-media"
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
     * Upload event cover image to Supabase Storage
     * @param userId User ID for organizing files
     * @param imageUri URI of the image to upload
     * @return URL of uploaded image or null if failed
     */
    suspend fun uploadEventImage(userId: String, imageUri: Uri): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val fileName = "event_${userId}_${System.currentTimeMillis()}.jpg"
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Failed to open image stream")

                val byteArray = inputStream.readBytes()
                inputStream.close()

                // Upload to Supabase Storage
                supabaseClient.storage
                    .from(EVENTS_BUCKET)
                    .upload("$userId/$fileName", byteArray)

                // Construct public URL
                val imageUrl = getPublicUrl(EVENTS_BUCKET, "$userId/$fileName")

                Result.success(imageUrl)
            } catch (e: Exception) {
                Result.failure(Exception("Event image upload failed: ${e.message}"))
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

    suspend fun uploadChatVoiceNote(userId: String, chatId: String, audioFile: File): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "voice_${userId}_${System.currentTimeMillis()}.m4a"
                val byteArray = audioFile.readBytes()

                supabaseClient.storage
                    .from(CHAT_VOICE_BUCKET)
                    .upload("$chatId/$userId/$fileName", byteArray)

                Result.success(getPublicUrl(CHAT_VOICE_BUCKET, "$chatId/$userId/$fileName"))
            } catch (e: Exception) {
                Result.failure(Exception("Voice note upload failed: ${e.message}"))
            }
        }

    suspend fun uploadStoryMedia(userId: String, mediaUri: Uri, storyType: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val extension = when (storyType.lowercase()) {
                    "video" -> "mp4"
                    else -> "jpg"
                }
                val prefix = if (storyType.lowercase() == "video") "video" else "image"
                val fileName = "${prefix}_${userId}_${System.currentTimeMillis()}.$extension"

                val inputStream = context.contentResolver.openInputStream(mediaUri)
                    ?: throw Exception("Failed to open story media stream")
                val byteArray = inputStream.readBytes()
                inputStream.close()

                supabaseClient.storage
                    .from(STORY_MEDIA_BUCKET)
                    .upload("$userId/$fileName", byteArray)

                Result.success(getPublicUrl(STORY_MEDIA_BUCKET, "$userId/$fileName"))
            } catch (e: Exception) {
                Result.failure(Exception("Story media upload failed: ${e.message}"))
            }
        }

    suspend fun uploadEventCommentImage(userId: String, eventId: String, imageUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "comment_${userId}_${System.currentTimeMillis()}.jpg"

                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Failed to open comment image stream")
                val byteArray = inputStream.readBytes()
                inputStream.close()

                // upload comment images to the dedicated comment bucket
                supabaseClient.storage
                    .from(COMMENT_MEDIA_BUCKET)
                    .upload("event-comments/$eventId/$userId/$fileName", byteArray)

                Result.success(getPublicUrl(COMMENT_MEDIA_BUCKET, "event-comments/$eventId/$userId/$fileName"))
            } catch (e: Exception) {
                Result.failure(Exception("Comment image upload failed: ${e.message}"))
            }
        }

    suspend fun uploadChatAttachment(
        userId: String,
        chatId: String,
        mediaUri: Uri,
        mimeType: String?,
        attachmentType: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedType = attachmentType.lowercase()
            val extension = when {
                normalizedType == "video" -> "mp4"
                normalizedType == "image" -> "jpg"
                normalizedType == "location" -> "txt"
                !mimeType.isNullOrBlank() -> android.webkit.MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.takeIf { it.isNotBlank() }
                    ?: "bin"
                else -> "bin"
            }
            val prefix = when (normalizedType) {
                "video" -> "video"
                "image" -> "image"
                "file" -> "file"
                else -> "attachment"
            }
            val fileName = "${prefix}_${userId}_${System.currentTimeMillis()}.$extension"

            val inputStream = context.contentResolver.openInputStream(mediaUri)
                ?: throw Exception("Failed to open attachment stream")
            val byteArray = inputStream.readBytes()
            inputStream.close()

            supabaseClient.storage
                .from(STORY_MEDIA_BUCKET)
                .upload("chat/$chatId/$userId/$fileName", byteArray)

            Result.success(getPublicUrl(STORY_MEDIA_BUCKET, "chat/$chatId/$userId/$fileName"))
        } catch (e: Exception) {
            Result.failure(Exception("Chat attachment upload failed: ${e.message}"))
        }
    }
}
