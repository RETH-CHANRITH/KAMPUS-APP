package com.example.kampus.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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

    private suspend fun uploadFileWithProgress(ref: StorageReference, file: Uri, onProgress: (Double) -> Unit) =
        suspendCancellableCoroutine { cont ->
            val task = ref.putFile(file)
            val listener = task.addOnProgressListener { snap ->
                val progress = (snap.bytesTransferred.toDouble() / snap.totalByteCount.toDouble())
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
     * Upload image story with compression, thumbnail, resumable upload (via Storage SDK) and progress callback.
     * Returns the created storyId on success.
     */
    suspend fun uploadImageStory(
        context: Context,
        fileUri: Uri,
        caption: String = "",
        privacy: String = "friends",
        onProgress: (Double) -> Unit = {},
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        val storyId = firestore.collection("stories").document().id
        val now = System.currentTimeMillis()
        val expiresAt = now + 86_400_000L

        val basePath = "stories/$uid/$storyId"
        val mediaRef = storage.reference.child("$basePath/media.jpg")
        val thumbRef = storage.reference.child("$basePath/thumb.jpg")

        // Compress and create temp files
        val compressed = compressImageToFile(context, fileUri)
        val thumb = createThumbnailFile(context, fileUri)

        // Upload media with progress
        uploadFileWithProgress(mediaRef, Uri.fromFile(compressed)) { p -> onProgress(p * 0.85) }
        val mediaUrl = mediaRef.downloadUrl.await().toString()

        // Upload thumb (small part of progress)
        uploadFileWithProgress(thumbRef, Uri.fromFile(thumb)) { p -> onProgress(0.85 + p * 0.15) }
        val thumbUrl = thumbRef.downloadUrl.await().toString()

        // Denormalized owner metadata (best-effort)
        val userDoc = firestore.collection("users").document(uid).get().await()
        val ownerName = userDoc.getString("displayName") ?: auth.currentUser?.displayName ?: "User"
        val ownerAvatarEmoji = userDoc.getString("avatarEmoji") ?: "👤"
        val ownerProfileImageUrl = userDoc.getString("profileImageUrl") ?: auth.currentUser?.photoUrl?.toString().orEmpty()
        val ownerAvatarColor = (userDoc.get("avatarColor") as? Number)?.toLong() ?: 0xFF3B82F6

        val storyData = mapOf(
            "ownerId" to uid,
            "userId" to uid,
            "note" to caption,
            "mediaType" to "image",
            "imageUrl" to mediaUrl,
            "thumbUrl" to thumbUrl,
            "mediaStoragePath" to basePath,
            "privacy" to privacy,
            "createdAt" to now,
            "expiresAt" to expiresAt,
            "ownerName" to ownerName,
            "ownerAvatarEmoji" to ownerAvatarEmoji,
            "ownerProfileImageUrl" to ownerProfileImageUrl,
            "ownerAvatarColor" to ownerAvatarColor,
        )

        firestore.collection("stories").document(storyId).set(storyData).await()

        // Clean temporary files
        try { compressed.delete() } catch (_: Throwable) {}
        try { thumb.delete() } catch (_: Throwable) {}

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

