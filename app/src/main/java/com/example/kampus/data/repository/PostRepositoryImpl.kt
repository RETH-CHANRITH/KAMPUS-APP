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
    @SerialName("shared_original_post_id") val sharedOriginalPostId: Long? = null,
    @SerialName("shared_original_author") val sharedOriginalAuthor: String? = null,
    @SerialName("shared_original_author_id") val sharedOriginalAuthorId: String? = null,
    @SerialName("shared_original_avatar") val sharedOriginalAvatar: String? = null,
    @SerialName("shared_original_profile_image_url") val sharedOriginalProfileImageUrl: String? = null,
    @SerialName("shared_original_time") val sharedOriginalTime: String? = null,
    @SerialName("shared_original_timestamp") val sharedOriginalTimestamp: Long? = null,
    @SerialName("shared_original_content") val sharedOriginalContent: String? = null,
    @SerialName("shared_original_media_urls") val sharedOriginalMediaUrls: List<String> = emptyList(),
    @SerialName("shared_original_media_types") val sharedOriginalMediaTypes: List<String> = emptyList(),
    @SerialName("shared_original_media_emojis") val sharedOriginalMediaEmojis: List<String> = emptyList(),
    @SerialName("shared_original_likes") val sharedOriginalLikes: Int? = null,
    @SerialName("shared_original_comments") val sharedOriginalComments: Int? = null,
    @SerialName("shared_original_shares") val sharedOriginalShares: Int? = null,
    @SerialName("shared_original_visibility") val sharedOriginalVisibility: String? = null,
    @SerialName("shared_original_is_verified") val sharedOriginalIsVerified: Boolean? = null,
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
        sharedOriginalPostId = post.sharedOriginalPostId?.toLong(),
        sharedOriginalAuthor = post.sharedOriginalAuthor,
        sharedOriginalAuthorId = post.sharedOriginalAuthorId,
        sharedOriginalAvatar = post.sharedOriginalAvatar,
        sharedOriginalProfileImageUrl = post.sharedOriginalProfileImageUrl,
        sharedOriginalTime = post.sharedOriginalTime,
        sharedOriginalTimestamp = post.sharedOriginalTimestamp,
        sharedOriginalContent = post.sharedOriginalContent,
        sharedOriginalMediaUrls = post.sharedOriginalMediaUris.map(Uri::toString),
        sharedOriginalMediaTypes = post.sharedOriginalMediaTypes.map { it.name.lowercase() },
        sharedOriginalMediaEmojis = post.sharedOriginalMediaEmojis,
        sharedOriginalLikes = post.sharedOriginalLikes,
        sharedOriginalComments = post.sharedOriginalComments,
        sharedOriginalShares = post.sharedOriginalShares,
        sharedOriginalVisibility = post.sharedOriginalVisibility?.name?.lowercase(),
        sharedOriginalIsVerified = post.sharedOriginalIsVerified,
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
        sharedOriginalPostId = row.sharedOriginalPostId?.toInt(),
        sharedOriginalAuthor = row.sharedOriginalAuthor,
        sharedOriginalAuthorId = row.sharedOriginalAuthorId,
        sharedOriginalAvatar = row.sharedOriginalAvatar,
        sharedOriginalProfileImageUrl = row.sharedOriginalProfileImageUrl,
        sharedOriginalTime = row.sharedOriginalTime,
        sharedOriginalTimestamp = row.sharedOriginalTimestamp,
        sharedOriginalContent = row.sharedOriginalContent,
        sharedOriginalMediaUris = row.sharedOriginalMediaUrls.map(Uri::parse),
        sharedOriginalMediaTypes = row.sharedOriginalMediaTypes.map { value ->
            when (value.lowercase()) {
                "video" -> PostItem.MediaType.VIDEO
                else -> PostItem.MediaType.IMAGE
            }
        },
        sharedOriginalMediaEmojis = row.sharedOriginalMediaEmojis,
        sharedOriginalLikes = row.sharedOriginalLikes,
        sharedOriginalComments = row.sharedOriginalComments,
        sharedOriginalShares = row.sharedOriginalShares,
        sharedOriginalVisibility = row.sharedOriginalVisibility?.let { value ->
            runCatching { PostItem.PostVisibility.valueOf(value.uppercase()) }.getOrNull()
        },
        sharedOriginalIsVerified = row.sharedOriginalIsVerified,
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
        sharedOriginalPostId = (post["sharedOriginalPostId"] as? Number)?.toLong(),
        sharedOriginalAuthor = post["sharedOriginalAuthor"] as? String,
        sharedOriginalAuthorId = post["sharedOriginalAuthorId"] as? String,
        sharedOriginalAvatar = post["sharedOriginalAvatar"] as? String,
        sharedOriginalProfileImageUrl = post["sharedOriginalProfileImageUrl"] as? String,
        sharedOriginalTime = post["sharedOriginalTime"] as? String,
        sharedOriginalTimestamp = (post["sharedOriginalTimestamp"] as? Number)?.toLong(),
        sharedOriginalContent = post["sharedOriginalContent"] as? String,
        sharedOriginalMediaUrls = (post["sharedOriginalMediaUrls"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        sharedOriginalMediaTypes = (post["sharedOriginalMediaTypes"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        sharedOriginalMediaEmojis = (post["sharedOriginalMediaEmojis"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        sharedOriginalLikes = (post["sharedOriginalLikes"] as? Number)?.toInt(),
        sharedOriginalComments = (post["sharedOriginalComments"] as? Number)?.toInt(),
        sharedOriginalShares = (post["sharedOriginalShares"] as? Number)?.toInt(),
        sharedOriginalVisibility = post["sharedOriginalVisibility"] as? String,
        sharedOriginalIsVerified = post["sharedOriginalIsVerified"] as? Boolean,
    )
}

class PostRepositoryImpl(private val firestore: FirebaseFirestore) {

    private val supabase = SupabaseModule.getSupabaseClient()

    private fun resolvePostId(documentId: String, storedId: Any?): Int {
        val parsedStoredId = when (storedId) {
            is Number -> storedId.toInt()
            is String -> storedId.toIntOrNull()
            else -> null
        }
        return parsedStoredId ?: documentId.toIntOrNull() ?: documentId.hashCode()
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
        val currentCommentCounts = hashMapOf<Int, Int>()
        val currentEngagementLikes = hashMapOf<Int, Int>()

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
                    val postItem = supabaseRowToPostItem(row)
                    val realComments = currentCommentCounts[postItem.id] ?: 0
                    val realLikes = currentEngagementLikes[postItem.id] ?: postItem.likes
                    cache[postItem.id] = postItem.copy(comments = realComments, likes = realLikes)
                }
                emitSnapshot()
            } catch (error: Exception) {
                trySend(Result.failure(error))
            }

            postgresChanges.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> action.decodeRecordOrNull<SupabasePostRow>()?.let { row ->
                        val postItem = supabaseRowToPostItem(row)
                        val realComments = currentCommentCounts[postItem.id] ?: 0
                        val realLikes = currentEngagementLikes[postItem.id] ?: postItem.likes
                        cache[postItem.id] = postItem.copy(comments = realComments, likes = realLikes)
                        emitSnapshot()
                    }

                    is PostgresAction.Update -> action.decodeRecordOrNull<SupabasePostRow>()?.let { row ->
                        val postItem = supabaseRowToPostItem(row)
                        val realComments = currentCommentCounts[postItem.id] ?: 0
                        val realLikes = currentEngagementLikes[postItem.id] ?: postItem.likes
                        cache[postItem.id] = postItem.copy(comments = realComments, likes = realLikes)
                        emitSnapshot()
                    }

                    is PostgresAction.Delete -> action.decodeOldRecordOrNull<SupabasePostRow>()?.let { row ->
                        cache.remove(row.id.toInt())
                        emitSnapshot()
                    }

                    is PostgresAction.Select -> action.decodeRecordOrNull<SupabasePostRow>()?.let { row ->
                        val postItem = supabaseRowToPostItem(row)
                        val realComments = currentCommentCounts[postItem.id] ?: 0
                        val realLikes = currentEngagementLikes[postItem.id] ?: postItem.likes
                        cache[postItem.id] = postItem.copy(comments = realComments, likes = realLikes)
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

                snapshot?.documents?.forEach { doc ->
                    try {
                        val resolvedId = resolvePostId(doc.id, doc.get("id"))
                        val comments = currentCommentCounts[resolvedId] ?: (doc.get("comments") as? Number)?.toInt() ?: 0
                        val likes = currentEngagementLikes[resolvedId] ?: (doc.get("likes") as? Number)?.toInt() ?: 0
                        val shares = (doc.get("shares") as? Number)?.toInt() ?: 0
                        val likedBy = parseStringList(doc.get("likedBy"))
                        val isPinned = doc.getBoolean("isPinned") ?: false

                        val existing = cache[resolvedId]
                        if (existing != null) {
                            cache[resolvedId] = existing.copy(
                                comments = comments,
                                likes = likes,
                                shares = shares,
                                likedBy = likedBy,
                                isPinned = isPinned,
                                sharedOriginalPostId = (doc.get("sharedOriginalPostId") as? Number)?.toInt() ?: existing.sharedOriginalPostId,
                                sharedOriginalAuthor = doc.getString("sharedOriginalAuthor") ?: existing.sharedOriginalAuthor,
                                sharedOriginalAuthorId = doc.getString("sharedOriginalAuthorId") ?: existing.sharedOriginalAuthorId,
                                sharedOriginalAvatar = doc.getString("sharedOriginalAvatar") ?: existing.sharedOriginalAvatar,
                                sharedOriginalProfileImageUrl = doc.getString("sharedOriginalProfileImageUrl") ?: existing.sharedOriginalProfileImageUrl,
                                sharedOriginalTime = doc.getString("sharedOriginalTime") ?: existing.sharedOriginalTime,
                                sharedOriginalTimestamp = (doc.get("sharedOriginalTimestamp") as? Number)?.toLong() ?: existing.sharedOriginalTimestamp,
                                sharedOriginalContent = doc.getString("sharedOriginalContent") ?: existing.sharedOriginalContent,
                                sharedOriginalMediaUris = if (parseStringList(doc.get("sharedOriginalMediaUrls")).isNotEmpty()) {
                                    parseStringList(doc.get("sharedOriginalMediaUrls")).map(Uri::parse)
                                } else existing.sharedOriginalMediaUris,
                                sharedOriginalMediaTypes = if (parseStringList(doc.get("sharedOriginalMediaTypes")).isNotEmpty()) {
                                    parseMediaTypes(doc.get("sharedOriginalMediaTypes"))
                                } else existing.sharedOriginalMediaTypes,
                                sharedOriginalMediaEmojis = if (parseStringList(doc.get("sharedOriginalMediaEmojis")).isNotEmpty()) {
                                    parseStringList(doc.get("sharedOriginalMediaEmojis"))
                                } else existing.sharedOriginalMediaEmojis,
                                sharedOriginalLikes = (doc.get("sharedOriginalLikes") as? Number)?.toInt() ?: existing.sharedOriginalLikes,
                                sharedOriginalComments = (doc.get("sharedOriginalComments") as? Number)?.toInt() ?: existing.sharedOriginalComments,
                                sharedOriginalShares = (doc.get("sharedOriginalShares") as? Number)?.toInt() ?: existing.sharedOriginalShares,
                                sharedOriginalVisibility = doc.getString("sharedOriginalVisibility")?.let { value ->
                                    runCatching { PostItem.PostVisibility.valueOf(value.uppercase()) }.getOrNull()
                                } ?: existing.sharedOriginalVisibility,
                                sharedOriginalIsVerified = doc.getBoolean("sharedOriginalIsVerified") ?: existing.sharedOriginalIsVerified,
                            )
                        } else {
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
                            cache[resolvedId] = PostItem(
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
                                likes = likes,
                                comments = comments,
                                shares = shares,
                                likedBy = likedBy,
                                isVerified = doc.getBoolean("isVerified") ?: false,
                                isPinned = isPinned,
                                sharedOriginalPostId = (doc.get("sharedOriginalPostId") as? Number)?.toInt(),
                                sharedOriginalAuthor = doc.getString("sharedOriginalAuthor"),
                                sharedOriginalAuthorId = doc.getString("sharedOriginalAuthorId"),
                                sharedOriginalAvatar = doc.getString("sharedOriginalAvatar"),
                                sharedOriginalProfileImageUrl = doc.getString("sharedOriginalProfileImageUrl"),
                                sharedOriginalTime = doc.getString("sharedOriginalTime"),
                                sharedOriginalTimestamp = (doc.get("sharedOriginalTimestamp") as? Number)?.toLong(),
                                sharedOriginalContent = doc.getString("sharedOriginalContent"),
                                sharedOriginalMediaUris = parseStringList(doc.get("sharedOriginalMediaUrls")).map(Uri::parse),
                                sharedOriginalMediaTypes = parseMediaTypes(doc.get("sharedOriginalMediaTypes")),
                                sharedOriginalMediaEmojis = parseStringList(doc.get("sharedOriginalMediaEmojis")),
                                sharedOriginalLikes = (doc.get("sharedOriginalLikes") as? Number)?.toInt(),
                                sharedOriginalComments = (doc.get("sharedOriginalComments") as? Number)?.toInt(),
                                sharedOriginalShares = (doc.get("sharedOriginalShares") as? Number)?.toInt(),
                                sharedOriginalVisibility = doc.getString("sharedOriginalVisibility")?.let { value ->
                                    runCatching { PostItem.PostVisibility.valueOf(value.uppercase()) }.getOrNull()
                                },
                                sharedOriginalIsVerified = doc.getBoolean("sharedOriginalIsVerified"),
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("PostRepositoryImpl", "Error parsing Firestore doc during merge", e)
                    }
                }

                launch { emitSnapshot() }
            }

        val commentsListener = firestore.collection("post_comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PostRepositoryImpl", "Comments snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                val commentCounts = snapshot?.documents?.mapNotNull { doc ->
                    doc.getLong("postId")
                }?.groupBy { it }?.mapValues { it.value.size } ?: emptyMap()

                currentCommentCounts.clear()
                commentCounts.forEach { (postId, count) ->
                    currentCommentCounts[postId.toInt()] = count
                }

                cache.keys.forEach { key ->
                    val count = currentCommentCounts[key] ?: 0
                    val existing = cache[key]
                    if (existing != null && existing.comments != count) {
                        cache[key] = existing.copy(comments = count)
                    }
                }

                launch { emitSnapshot() }
            }

        val engagementsListener = firestore.collection("post_engagements")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PostRepositoryImpl", "Engagements snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach { doc ->
                    val resolvedId = doc.id.toIntOrNull() ?: return@forEach
                    val likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
                    currentEngagementLikes[resolvedId] = likesCount
                }

                cache.keys.forEach { key ->
                    val likes = currentEngagementLikes[key]
                    if (likes != null) {
                        val existing = cache[key]
                        if (existing != null && existing.likes != likes) {
                            cache[key] = existing.copy(likes = likes)
                        }
                    }
                }

                launch { emitSnapshot() }
            }

        awaitClose {
            firestoreListener.remove()
            commentsListener.remove()
            engagementsListener.remove()
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
                            isPinned = snapshot.getBoolean("isPinned") ?: false,
                            sharedOriginalPostId = (snapshot.get("sharedOriginalPostId") as? Number)?.toInt(),
                            sharedOriginalAuthor = snapshot.getString("sharedOriginalAuthor"),
                            sharedOriginalAuthorId = snapshot.getString("sharedOriginalAuthorId"),
                            sharedOriginalAvatar = snapshot.getString("sharedOriginalAvatar"),
                            sharedOriginalProfileImageUrl = snapshot.getString("sharedOriginalProfileImageUrl"),
                            sharedOriginalTime = snapshot.getString("sharedOriginalTime"),
                            sharedOriginalTimestamp = (snapshot.get("sharedOriginalTimestamp") as? Number)?.toLong(),
                            sharedOriginalContent = snapshot.getString("sharedOriginalContent"),
                            sharedOriginalMediaUris = parseStringList(snapshot.get("sharedOriginalMediaUrls")).map(Uri::parse),
                            sharedOriginalMediaTypes = parseMediaTypes(snapshot.get("sharedOriginalMediaTypes")),
                            sharedOriginalMediaEmojis = parseStringList(snapshot.get("sharedOriginalMediaEmojis")),
                            sharedOriginalLikes = (snapshot.get("sharedOriginalLikes") as? Number)?.toInt(),
                            sharedOriginalComments = (snapshot.get("sharedOriginalComments") as? Number)?.toInt(),
                            sharedOriginalShares = (snapshot.get("sharedOriginalShares") as? Number)?.toInt(),
                            sharedOriginalVisibility = snapshot.getString("sharedOriginalVisibility")?.let { value ->
                                runCatching { PostItem.PostVisibility.valueOf(value.uppercase()) }.getOrNull()
                            },
                            sharedOriginalIsVerified = snapshot.getBoolean("sharedOriginalIsVerified")
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

    suspend fun updatePostContent(postId: String, content: String): Result<Unit> = try {
        supabase.from("posts").update(mapOf("content" to content)) {
            filter { eq("id", postId.toLong()) }
        }
        runCatching {
            firestore.collection("posts").document(postId).update("content", content).await()
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

    suspend fun syncPostFromSupabaseToFirestore(postId: Int): Result<PostItem?> {
        return try {
            val row = supabase.from("posts")
                .select() {
                    filter { eq("id", postId.toLong()) }
                }
                .decodeSingleOrNull<SupabasePostRow>()

            if (row != null) {
                val postItem = supabaseRowToPostItem(row)
                val payload = hashMapOf<String, Any>(
                    "id" to postItem.id.toLong(),
                    "author" to postItem.author,
                    "authorId" to postItem.authorId,
                    "avatar" to postItem.avatar,
                    "profileImageUrl" to postItem.profileImageUrl,
                    "time" to postItem.time,
                    "content" to postItem.content,
                    "privacy" to "public",
                    "likes" to postItem.likes,
                    "comments" to postItem.comments,
                    "shares" to postItem.shares,
                    "timestamp" to postItem.timestamp,
                )
                if (row.mediaUrls.isNotEmpty()) {
                    payload["mediaUrls"] = row.mediaUrls
                    payload["mediaTypes"] = row.mediaTypes
                }
                postItem.sharedOriginalPostId?.let { payload["sharedOriginalPostId"] = it }
                postItem.sharedOriginalAuthor?.let { payload["sharedOriginalAuthor"] = it }
                postItem.sharedOriginalAuthorId?.let { payload["sharedOriginalAuthorId"] = it }
                postItem.sharedOriginalAvatar?.let { payload["sharedOriginalAvatar"] = it }
                postItem.sharedOriginalProfileImageUrl?.let { payload["sharedOriginalProfileImageUrl"] = it }
                postItem.sharedOriginalTime?.let { payload["sharedOriginalTime"] = it }
                postItem.sharedOriginalTimestamp?.let { payload["sharedOriginalTimestamp"] = it }
                postItem.sharedOriginalContent?.let { payload["sharedOriginalContent"] = it }
                if (postItem.sharedOriginalMediaUris.isNotEmpty()) {
                    payload["sharedOriginalMediaUrls"] = postItem.sharedOriginalMediaUris.map(Uri::toString)
                }
                if (postItem.sharedOriginalMediaTypes.isNotEmpty()) {
                    payload["sharedOriginalMediaTypes"] = postItem.sharedOriginalMediaTypes.map { it.name.lowercase() }
                }
                if (postItem.sharedOriginalMediaEmojis.isNotEmpty()) {
                    payload["sharedOriginalMediaEmojis"] = postItem.sharedOriginalMediaEmojis
                }
                postItem.sharedOriginalLikes?.let { payload["sharedOriginalLikes"] = it }
                postItem.sharedOriginalComments?.let { payload["sharedOriginalComments"] = it }
                postItem.sharedOriginalShares?.let { payload["sharedOriginalShares"] = it }
                postItem.sharedOriginalVisibility?.let { payload["sharedOriginalVisibility"] = it.name.lowercase() }
                postItem.sharedOriginalIsVerified?.let { payload["sharedOriginalIsVerified"] = it }
                firestore.collection("posts").document(postId.toString()).set(payload, SetOptions.merge()).await()
                Result.success(postItem)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "syncPostFromSupabaseToFirestore failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
