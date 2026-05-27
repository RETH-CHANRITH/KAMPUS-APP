package com.example.kampus.data.repository

import android.net.Uri
import android.util.Log
import com.example.kampus.ui.feed.PostItem
import com.example.kampus.di.SupabaseModule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class SupabasePostRow(
    @SerialName("id") val id: Long,
    @SerialName("author_id") val authorId: String,
    @SerialName("author") val author: String,
    @SerialName("avatar") val avatar: String = "👤",
    @SerialName("profile_image_url") val profileImageUrl: String = "",
    @SerialName("time") val time: String = "now",
    @SerialName("content") val content: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("likes") val likes: Int = 0,
    @SerialName("comments") val comments: Int = 0,
    @SerialName("shares") val shares: Int = 0,
    @SerialName("visibility") val visibility: String = "public",
    @SerialName("allow_comments") val allowComments: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("feeling") val feeling: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("tagged_people") val taggedPeople: List<String> = emptyList(),
    @SerialName("feeling_emoji") val feelingEmoji: String? = null,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    @SerialName("media_types") val mediaTypes: List<String> = emptyList(),
    @SerialName("media_emojis") val mediaEmojis: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

private fun parseStringList(rawValue: Any?): List<String> {
    return (rawValue as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
}

private fun parseMediaTypes(rawValue: Any?): List<PostItem.MediaType> {
    return parseStringList(rawValue).map { value ->
        when (value.lowercase()) {
            "video" -> PostItem.MediaType.VIDEO
            else -> PostItem.MediaType.IMAGE
        }
    }
}

private fun postItemToSupabaseRow(post: PostItem, authorId: String): SupabasePostRow {
    return SupabasePostRow(
        id = post.id.toLong(),
        authorId = authorId,
        author = post.author,
        avatar = post.avatar,
        profileImageUrl = post.profileImageUrl,
        time = post.time,
        content = post.content,
        timestamp = post.timestamp,
        likes = post.likes,
        comments = post.comments,
        shares = post.shares,
        visibility = post.visibility.name.lowercase(),
        allowComments = post.allowComments,
        isVerified = post.isVerified,
        isPinned = post.isPinned,
        feeling = post.feeling,
        location = post.location,
        tags = post.tags,
        taggedPeople = post.taggedPeople,
        feelingEmoji = post.feelingEmoji,
        mediaUrls = post.mediaUris.map(Uri::toString),
        mediaTypes = post.mediaTypes.map { it.name.lowercase() },
        mediaEmojis = post.mediaEmojis,
    )
}

private fun supabaseRowToPostItem(row: SupabasePostRow): PostItem {
    val resolvedMediaUris = row.mediaUrls.map(Uri::parse)
    val resolvedMediaTypes = if (row.mediaTypes.size == resolvedMediaUris.size) {
        row.mediaTypes.map { value ->
            when (value.lowercase()) {
                "video" -> PostItem.MediaType.VIDEO
                else -> PostItem.MediaType.IMAGE
            }
        }
    } else {
        resolvedMediaUris.map { PostItem.MediaType.IMAGE }
    }

    return PostItem(
        id = row.id.toInt(),
        author = row.author.ifBlank { "Unknown" },
        authorId = row.authorId,
        avatar = row.avatar.ifBlank { "👤" },
        profileImageUrl = row.profileImageUrl,
        time = row.time.ifBlank { "now" },
        content = row.content,
        timestamp = row.timestamp,
        mediaUris = resolvedMediaUris,
        mediaTypes = resolvedMediaTypes,
        mediaEmojis = row.mediaEmojis,
        likes = row.likes,
        comments = row.comments,
        shares = row.shares,
        isVerified = row.isVerified,
        feeling = row.feeling,
        location = row.location,
        tags = row.tags,
        visibility = runCatching { PostItem.PostVisibility.valueOf(row.visibility.uppercase()) }
            .getOrDefault(PostItem.PostVisibility.PUBLIC),
        allowComments = row.allowComments,
        taggedPeople = row.taggedPeople,
        feelingEmoji = row.feelingEmoji,
        isPinned = row.isPinned,
    )
}

private fun postMapToSupabaseRow(post: Map<String, Any>): SupabasePostRow? {
    val id = (post["id"] as? Number)?.toLong()
        ?: (post["postId"] as? Number)?.toLong()
        ?: (post["timestamp"] as? Number)?.toLong()
        ?: return null
    return SupabasePostRow(
        id = id,
        authorId = (post["authorId"] as? String).orEmpty(),
        author = (post["author"] as? String).orEmpty(),
        avatar = (post["avatar"] as? String).orEmpty().ifBlank { "👤" },
        profileImageUrl = (post["profileImageUrl"] as? String).orEmpty(),
        time = (post["time"] as? String).orEmpty().ifBlank { "now" },
        content = (post["content"] as? String).orEmpty(),
        timestamp = (post["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        likes = (post["likes"] as? Number)?.toInt() ?: 0,
        comments = (post["comments"] as? Number)?.toInt() ?: 0,
        shares = (post["shares"] as? Number)?.toInt() ?: 0,
        visibility = (post["privacy"] as? String ?: post["visibility"] as? String ?: "public").lowercase(),
        allowComments = post["allowComments"] as? Boolean ?: true,
        isVerified = post["isVerified"] as? Boolean ?: false,
        isPinned = post["isPinned"] as? Boolean ?: false,
        feeling = post["feeling"] as? String,
        location = post["location"] as? String,
        tags = (post["tags"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        taggedPeople = (post["taggedPeople"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        feelingEmoji = post["feelingEmoji"] as? String,
        mediaUrls = (post["mediaUrls"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        mediaTypes = (post["mediaTypes"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        mediaEmojis = (post["mediaEmojis"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
    )
}

class PostRepositoryImpl(private val firestore: FirebaseFirestore) {

    private val supabase = SupabaseModule.getSupabaseClient()

    private fun resolvePostId(documentId: String, storedId: Any?): Int {
        return when (storedId) {
            is Number -> storedId.toInt()
            is String -> storedId.toIntOrNull() ?: documentId.hashCode()
            else -> documentId.hashCode()
        }
    }

    suspend fun backfillLegacyPostAuthorIds(): Int {
        val snapshot = firestore.collection("posts").get().await()
        var updatedCount = 0
        val batchSize = 400
        var batch = firestore.batch()
        var pendingWrites = 0

        snapshot.documents.forEach { doc ->
            val authorId = doc.getString("authorId").orEmpty()
            val legacyUserId = doc.getString("userId").orEmpty()

            if (authorId.isBlank() && legacyUserId.isNotBlank()) {
                batch.update(doc.reference, "authorId", legacyUserId)
                updatedCount++
                pendingWrites++

                if (pendingWrites >= batchSize) {
                    batch.commit().await()
                    batch = firestore.batch()
                    pendingWrites = 0
                }
            }
        }

        if (pendingWrites > 0) {
            batch.commit().await()
        }

        return updatedCount
    }

    @OptIn(SupabaseExperimental::class)
    fun getFeedPosts(): Flow<Result<List<PostItem>>> = callbackFlow {
        val cache = linkedMapOf<Int, PostItem>()

        suspend fun emitSnapshot() {
            trySend(Result.success(cache.values.sortedByDescending { it.timestamp }))
        }

        val supabaseChannel = supabase.channel("posts-realtime")
        val postgresChanges = supabaseChannel.postgresChangeFlow<PostgresAction>("public") {
            table = "posts"
        }
        val supabaseJob = launch {
            try {
                val supabasePosts = supabase
                    .from("posts")
                    .select()
                    .decodeList<SupabasePostRow>()

                supabasePosts.forEach { row ->
                    cache[row.id.toInt()] = supabaseRowToPostItem(row)
                }
                emitSnapshot()
            } catch (error: Exception) {
                trySend(Result.failure(error))
            }

            postgresChanges.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> action.decodeRecordOrNull<SupabasePostRow>()?.let { row ->
                        cache[row.id.toInt()] = supabaseRowToPostItem(row)
                        emitSnapshot()
                    }

                    is PostgresAction.Update -> action.decodeRecordOrNull<SupabasePostRow>()?.let { row ->
                        cache[row.id.toInt()] = supabaseRowToPostItem(row)
                        emitSnapshot()
                    }

                    is PostgresAction.Delete -> action.decodeOldRecordOrNull<SupabasePostRow>()?.let { row ->
                        cache.remove(row.id.toInt())
                        emitSnapshot()
                    }

                    is PostgresAction.Select -> action.decodeRecordOrNull<SupabasePostRow>()?.let { row ->
                        cache[row.id.toInt()] = supabaseRowToPostItem(row)
                        emitSnapshot()
                    }
                }
            }
        }

        launch {
            supabaseChannel.subscribe(true)
        }

        val firestoreListener = firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val resolvedId = resolvePostId(doc.id, doc.get("id"))
                        val mediaUrls = parseStringList(doc.get("mediaUrls"))
                        val legacyMediaUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("imageUri")?.takeIf { it.isNotBlank() }
                        val resolvedMediaUrls = if (mediaUrls.isNotEmpty()) mediaUrls else legacyMediaUrl?.let { listOf(it) }.orEmpty()
                        val resolvedMediaTypes = if (resolvedMediaUrls.isNotEmpty()) {
                            val storedTypes = parseMediaTypes(doc.get("mediaTypes"))
                            if (storedTypes.size == resolvedMediaUrls.size) storedTypes else resolvedMediaUrls.map { PostItem.MediaType.IMAGE }
                        } else {
                            emptyList()
                        }
                        PostItem(
                            id = resolvedId,
                            author = doc.getString("author") ?: "Unknown",
                            authorId = doc.getString("authorId") ?: doc.getString("userId") ?: "",
                            avatar = doc.getString("avatar") ?: "👤",
                            profileImageUrl = doc.getString("profileImageUrl") ?: "",
                            time = doc.getString("time") ?: "now",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            mediaUris = resolvedMediaUrls.map(Uri::parse),
                            mediaTypes = resolvedMediaTypes,
                            mediaEmojis = emptyList(),
                            likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                            comments = (doc.get("comments") as? Number)?.toInt() ?: 0,
                            shares = (doc.get("shares") as? Number)?.toInt() ?: 0,
                            likedBy = parseStringList(doc.get("likedBy")),
                            isVerified = doc.getBoolean("isVerified") ?: false,
                            isPinned = doc.getBoolean("isPinned") ?: false,
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.forEach { post ->
                    cache[post.id] = post
                }

                cache.values.sortedByDescending { it.timestamp }.let { posts ->
                    trySend(Result.success(posts))
                }
            }

        awaitClose {
            firestoreListener.remove()
            supabaseJob.cancel()
            launch {
                supabase.realtime.removeChannel(supabaseChannel)
            }
        }
    }

    suspend fun createPost(post: Map<String, Any>): Result<String> = try {
        val supabaseRow = postMapToSupabaseRow(post)
            ?: return Result.failure(IllegalArgumentException("Invalid post payload"))

        val insertedRow = supabase
            .from("posts")
            .insert(supabaseRow) { select() }
            .decodeSingle<SupabasePostRow>()

        runCatching {
            firestore.collection("posts").document(supabaseRow.id.toString()).set(post).await()
        }.onFailure { error ->
            Log.w("PostRepositoryImpl", "Firestore mirror write failed: ${error.message}")
        }

        Result.success(insertedRow.id.toString())
    } catch (e: Exception) {
        Log.e("PostRepositoryImpl", "createPost failed: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun likePost(postId: String, isLiked: Boolean): Result<Unit> = try {
        // This helper is retained for callers that still invoke it, but the
        // app now syncs real like counts through updatePostLikes().
        runCatching {
            firestore.collection("posts").document(postId).update("isLiked", isLiked).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deletePost(postId: String): Result<Unit> = try {
        runCatching {
            supabase.from("posts").delete {
                filter { eq("id", postId.toLong()) }
            }
        }
        runCatching {
            firestore.collection("posts").document(postId).delete().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getPostById(postId: String): Flow<Result<PostItem?>> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val post = if (snapshot != null && snapshot.exists()) {
                    try {
                        val resolvedId = resolvePostId(snapshot.id, snapshot.get("id"))
                        val mediaUrls = parseStringList(snapshot.get("mediaUrls"))
                        val legacyMediaUrl = snapshot.getString("imageUrl")?.takeIf { it.isNotBlank() }
                            ?: snapshot.getString("imageUri")?.takeIf { it.isNotBlank() }
                        val resolvedMediaUrls = if (mediaUrls.isNotEmpty()) mediaUrls else legacyMediaUrl?.let { listOf(it) }.orEmpty()
                        val resolvedMediaTypes = if (resolvedMediaUrls.isNotEmpty()) {
                            val storedTypes = parseMediaTypes(snapshot.get("mediaTypes"))
                            if (storedTypes.size == resolvedMediaUrls.size) storedTypes else resolvedMediaUrls.map { PostItem.MediaType.IMAGE }
                        } else {
                            emptyList()
                        }
                        PostItem(
                            id = resolvedId,
                            author = snapshot.getString("author") ?: "Unknown",
                            authorId = snapshot.getString("authorId") ?: snapshot.getString("userId") ?: "",
                            avatar = snapshot.getString("avatar") ?: "👤",
                            profileImageUrl = snapshot.getString("profileImageUrl") ?: "",
                            time = snapshot.getString("time") ?: "now",
                            content = snapshot.getString("content") ?: "",
                            timestamp = snapshot.getLong("timestamp") ?: 0L,
                            mediaUris = resolvedMediaUrls.map(Uri::parse),
                            mediaTypes = resolvedMediaTypes,
                            mediaEmojis = emptyList(),
                            likes = (snapshot.get("likes") as? Number)?.toInt() ?: 0,
                            comments = (snapshot.get("comments") as? Number)?.toInt() ?: 0,
                            shares = (snapshot.get("shares") as? Number)?.toInt() ?: 0,
                            likedBy = parseStringList(snapshot.get("likedBy")),
                            isPinned = snapshot.getBoolean("isPinned") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null

                trySend(Result.success(post))
            }

        awaitClose { listener.remove() }
    }

    suspend fun updatePostLikes(
        postId: String,
        updatedLikeCount: Int,
        userId: String? = null,
        isLiked: Boolean? = null,
    ): Result<Unit> = try {
        supabase.from("posts").update(mapOf("likes" to updatedLikeCount)) {
            filter { eq("id", postId.toLong()) }
        }
        val firestoreUpdates = hashMapOf<String, Any>("likes" to updatedLikeCount)
        if (!userId.isNullOrBlank() && isLiked != null) {
            firestoreUpdates["likedBy"] = if (isLiked) {
                com.google.firebase.firestore.FieldValue.arrayUnion(userId)
            } else {
                com.google.firebase.firestore.FieldValue.arrayRemove(userId)
            }
        }
        runCatching {
            firestore.collection("posts").document(postId).set(firestoreUpdates, SetOptions.merge()).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePostShares(postId: String, updatedShareCount: Int): Result<Unit> = try {
        supabase.from("posts").update(mapOf("shares" to updatedShareCount)) {
            filter { eq("id", postId.toLong()) }
        }
        runCatching {
            firestore.collection("posts").document(postId).update("shares", updatedShareCount).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePostPin(postId: String, isPinned: Boolean): Result<Unit> = try {
        supabase.from("posts").update(mapOf("is_pinned" to isPinned)) {
            filter { eq("id", postId.toLong()) }
        }
        runCatching {
            firestore.collection("posts").document(postId).update("isPinned", isPinned).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePostVisibility(postId: String, visibility: String): Result<Unit> = try {
        supabase.from("posts").update(mapOf("visibility" to visibility)) {
            filter { eq("id", postId.toLong()) }
        }
        runCatching {
            firestore.collection("posts").document(postId).update("visibility", visibility).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun hidePostFromProfile(postId: String, hide: Boolean): Result<Unit> = try {
        supabase.from("posts").update(mapOf("hidden_from_profile" to hide)) {
            filter { eq("id", postId.toLong()) }
        }
        runCatching {
            firestore.collection("posts").document(postId).update("hiddenFromProfile", hide).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
