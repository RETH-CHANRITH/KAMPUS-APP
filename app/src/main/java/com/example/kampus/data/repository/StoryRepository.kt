package com.example.kampus.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kampus.di.SupabaseModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    private suspend fun compressImageToFile(context: Context, inputUri: Uri, maxDim: Int = 1920): File =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val input = resolver.openInputStream(inputUri) ?: throw IllegalArgumentException("Cannot open uri")
            val original = BitmapFactory.decodeStream(input)
            input.close()

            val (width, height) = original.width to original.height
            val scale = if (width > height) maxDim.toFloat() / width else maxDim.toFloat() / height
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(original, (width * scale).toInt(), (height * scale).toInt(), true) else original

            val outFile = File(context.cacheDir, "story_upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                fos.flush()
            }
            outFile
        }

    private suspend fun createThumbnailFile(context: Context, inputUri: Uri, size: Int = 320): File =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val input = resolver.openInputStream(inputUri) ?: throw IllegalArgumentException("Cannot open uri")
            val original = BitmapFactory.decodeStream(input)
            input.close()

            val thumb = Bitmap.createScaledBitmap(original, size, (size * (original.height.toFloat() / original.width)).toInt(), true)
            val outFile = File(context.cacheDir, "story_thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 75, fos)
                fos.flush()
            }
            outFile
        }

    private suspend fun createVideoThumbnailFile(context: Context, inputUri: Uri, size: Int = 320): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, inputUri)
                val timeUs = 1_000_000L // frame at 1 second
                val original = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                if (original != null) {
                    val thumb = Bitmap.createScaledBitmap(original, size, (size * (original.height.toFloat() / original.width)).toInt(), true)
                    val outFile = File(context.cacheDir, "story_video_thumb_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outFile).use { fos ->
                        thumb.compress(Bitmap.CompressFormat.JPEG, 75, fos)
                        fos.flush()
                    }
                    outFile
                } else null
            }.getOrNull()
        }

    private suspend fun uploadFileWithProgress(ref: StorageReference, file: Uri, onProgress: (Double) -> Unit) =
        suspendCancellableCoroutine { cont ->
            val task = ref.putFile(file)
            val listener = task.addOnProgressListener { snap ->
                val progress = if (snap.totalByteCount > 0) {
                    (snap.bytesTransferred.toDouble() / snap.totalByteCount.toDouble())
                } else 0.0
                try {
                    onProgress(progress)
                } catch (ignored: Throwable) {
                }
            }
            task.addOnSuccessListener { snap ->
                cont.resume(snap)
            }.addOnFailureListener { ex ->
                cont.resumeWithException(ex)
            }
            cont.invokeOnCancellation { task.cancel() }
        }

    /**
     * Upload image/video story with compression, thumbnail, and real-time progress callbacks.
     * Returns the created storyId on success.
     */
    suspend fun uploadStory(
        context: Context,
        fileUri: Uri,
        caption: String = "",
        overlayText: String = "",
        overlayX: Float = 0f,
        overlayY: Float = 0f,
        overlayColor: Long = 0xFFFFFFFF,
        privacy: String = "friends",
        onProgress: (Double) -> Unit = {},
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        val storyId = firestore.collection("stories").document().id
        val now = System.currentTimeMillis()
        val expiresAt = now + 86_400_000L

        val isVideo = runCatching {
            val type = context.contentResolver.getType(fileUri)
            type?.startsWith("video") == true || fileUri.toString().endsWith(".mp4", ignoreCase = true)
        }.getOrDefault(false)

        val supabaseStorage = SupabaseModule.getStorageManager()
        var mediaUrl = ""
        var thumbUrl = ""

        onProgress(0.15) // Compression / preparation started

        if (isVideo) {
            // Upload raw video to Supabase
            onProgress(0.30)
            val mediaResult = supabaseStorage.uploadStoryMedia(uid, fileUri, "video")
            mediaUrl = mediaResult.getOrThrow()
            onProgress(0.80)

            // Video thumbnail
            val videoThumb = createVideoThumbnailFile(context, fileUri)
            if (videoThumb != null) {
                val thumbResult = supabaseStorage.uploadStoryMedia(uid, Uri.fromFile(videoThumb), "image")
                thumbUrl = thumbResult.getOrThrow()
                try { videoThumb.delete() } catch (_: Throwable) {}
            }
            onProgress(1.00)
        } else {
            // Compress and upload image
            val compressed = compressImageToFile(context, fileUri)
            val thumb = createThumbnailFile(context, fileUri)
            onProgress(0.30)

            val mediaResult = supabaseStorage.uploadStoryMedia(uid, Uri.fromFile(compressed), "image")
            mediaUrl = mediaResult.getOrThrow()
            onProgress(0.80)

            val thumbResult = supabaseStorage.uploadStoryMedia(uid, Uri.fromFile(thumb), "image")
            thumbUrl = thumbResult.getOrThrow()

            try { compressed.delete() } catch (_: Throwable) {}
            try { thumb.delete() } catch (_: Throwable) {}
            onProgress(1.00)
        }

        // Get owner profile metadata
        val userDoc = firestore.collection("users").document(uid).get().await()
        val ownerName = userDoc.getString("displayName") ?: auth.currentUser?.displayName ?: "User"
        val ownerAvatarEmoji = userDoc.getString("avatarEmoji") ?: "👤"
        val ownerProfileImageUrl = userDoc.getString("profileImageUrl") ?: auth.currentUser?.photoUrl?.toString().orEmpty()
        val ownerAvatarColor = (userDoc.get("avatarColor") as? Number)?.toLong() ?: 0xFF3B82F6

        val storyData = mapOf(
            "ownerId" to uid,
            "userId" to uid,
            "note" to caption,
            "mediaType" to (if (isVideo) "video" else "image"),
            "storyType" to (if (isVideo) "video" else "image"),
            "imageUrl" to mediaUrl,
            "thumbUrl" to thumbUrl,
            "mediaStoragePath" to "stories/$uid/$storyId",
            "privacy" to privacy,
            "createdAt" to now,
            "expiresAt" to expiresAt,
            "ownerName" to ownerName,
            "ownerAvatarEmoji" to ownerAvatarEmoji,
            "ownerProfileImageUrl" to ownerProfileImageUrl,
            "ownerAvatarColor" to ownerAvatarColor,
            "overlayText" to overlayText,
            "overlayX" to overlayX,
            "overlayY" to overlayY,
            "overlayColor" to overlayColor,
        )

        firestore.collection("stories").document(storyId).set(storyData).await()

        storyId
    }

    /**
     * Enqueue a background upload using WorkManager. Worker will call back into repository.
     */
    fun enqueueBackgroundUpload(context: Context, fileUri: Uri, caption: String = "", privacy: String = "friends") {
        val data = Data.Builder()
            .putString("fileUri", fileUri.toString())
            .putString("caption", caption)
            .putString("privacy", privacy)
            .build()

        val work = OneTimeWorkRequestBuilder<com.example.kampus.data.repository.StoryUploadWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}
