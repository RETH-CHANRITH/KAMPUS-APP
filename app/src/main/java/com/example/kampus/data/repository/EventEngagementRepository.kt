package com.example.kampus.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.example.kampus.utils.ActivityLogger
import com.example.kampus.utils.NotificationLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EventEngagementSummary(
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val savesCount: Int = 0,
    val interestedCount: Int = 0,
    val sharesCount: Int = 0,
)

data class EventMemberEngagement(
    val liked: Boolean = false,
    val saved: Boolean = false,
    val interested: Boolean = false,
)

data class EventComment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorEmoji: String = "👤",
    val authorProfileImageUrl: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val createdAt: Long? = null,
    val parentCommentId: String? = null,
    val likesCount: Int = 0,
    val likedByCurrentUser: Boolean = false,
    val replies: List<EventComment> = emptyList(),
)

class EventEngagementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val summaryRoot = firestore.collection("event_engagements")

    fun observeSummary(eventId: String): Flow<Result<EventEngagementSummary>> = callbackFlow {
        val listener: ListenerRegistration = summaryRoot.document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                trySend(
                    Result.success(
                        EventEngagementSummary(
                            likesCount = (snapshot?.getLong("likesCount") ?: 0L).toInt(),
                            commentsCount = (snapshot?.getLong("commentsCount") ?: 0L).toInt(),
                            savesCount = (snapshot?.getLong("savesCount") ?: 0L).toInt(),
                            interestedCount = (snapshot?.getLong("interestedCount") ?: 0L).toInt(),
                            sharesCount = (snapshot?.getLong("sharesCount") ?: 0L).toInt(),
                        ),
                    ),
                )
            }

        awaitClose { listener.remove() }
    }

    fun observeMemberState(eventId: String, userId: String): Flow<Result<EventMemberEngagement>> = callbackFlow {
        val listener: ListenerRegistration = summaryRoot.document(eventId)
            .collection("members")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                trySend(
                    Result.success(
                        EventMemberEngagement(
                            liked = snapshot?.getBoolean("liked") ?: false,
                            saved = snapshot?.getBoolean("saved") ?: false,
                            interested = snapshot?.getBoolean("interested") ?: false,
                        ),
                    ),
                )
            }

        awaitClose { listener.remove() }
    }

    fun observeComments(eventId: String, currentUserId: String? = null): Flow<Result<List<EventComment>>> = callbackFlow {
        val listener: ListenerRegistration = summaryRoot.document(eventId)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val mapped = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val text = doc.getString("text")?.trim().orEmpty()
                    if (text.isBlank()) return@mapNotNull null

                    @Suppress("UNCHECKED_CAST")
                    val likedBy = (doc.get("likedBy") as? List<String>).orEmpty()
                    val likesCount = (doc.getLong("likesCount") ?: likedBy.size.toLong()).toInt()

                    EventComment(
                        id = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "Unknown",
                        authorEmoji = doc.getString("authorEmoji") ?: "👤",
                        authorProfileImageUrl = doc.getString("authorProfileImageUrl") ?: "",
                        text = text,
                        imageUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() },
                        createdAt = doc.getLong("createdAt"),
                        parentCommentId = doc.getString("parentCommentId")?.takeIf { it.isNotBlank() },
                        likesCount = likesCount,
                        likedByCurrentUser = currentUserId != null && likedBy.contains(currentUserId),
                    )
                }

                launch {
                    val enriched = enrichEventCommentsWithProfiles(mapped)
                    val repliesByParent = enriched
                        .filter { !it.parentCommentId.isNullOrBlank() }
                        .groupBy { it.parentCommentId!! }

                    val comments = enriched
                        .filter { it.parentCommentId.isNullOrBlank() }
                        .map { comment ->
                            comment.copy(
                                replies = repliesByParent[comment.id].orEmpty().sortedBy { it.createdAt ?: 0L },
                            )
                        }

                    trySend(Result.success(comments))
                }
            }

        awaitClose { listener.remove() }
    }

    private suspend fun enrichEventCommentsWithProfiles(comments: List<EventComment>): List<EventComment> = coroutineScope {
        val cachedProfiles = comments
            .map { it.authorId }
            .filter { it.isNotBlank() }
            .distinct()
            .associateWith { authorId ->
                async {
                    val profileImageUrl = firestore.collection("users")
                        .document(authorId)
                        .get()
                        .await()
                        .getString("profileImageUrl")
                        ?.takeIf { it.isNotBlank() }
                        .orEmpty()
                    profileImageUrl
                }
            }

        comments.map { comment ->
            if (comment.authorProfileImageUrl.isNotBlank() || comment.authorId.isBlank()) {
                comment
            } else {
                val fallbackImageUrl = cachedProfiles[comment.authorId]?.await().orEmpty()
                if (fallbackImageUrl.isBlank()) comment else comment.copy(authorProfileImageUrl = fallbackImageUrl)
            }
        }
    }

    suspend fun toggleFlag(
        eventId: String,
        userId: String,
        flagField: String,
        summaryField: String,
    ): Result<Boolean> = try {
        val summaryRef = summaryRoot.document(eventId)
        val memberRef = summaryRef.collection("members").document(userId)

        val nextValue = firestore.runTransaction { transaction ->
            val memberSnapshot = transaction.get(memberRef)
            val currentValue = memberSnapshot.getBoolean(flagField) ?: false
            val next = !currentValue

            val summarySnapshot = transaction.get(summaryRef)
            val currentCount = (summarySnapshot.getLong(summaryField) ?: 0L).toInt()
            val updatedCount = (currentCount + if (next) 1 else -1).coerceAtLeast(0)

            transaction.set(
                memberRef,
                mapOf(
                    flagField to next,
                    "userId" to userId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                summaryRef,
                mapOf(
                    summaryField to updatedCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            next
        }.await()

        Result.success(nextValue)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addComment(
        eventId: String,
        userId: String,
        authorName: String,
        authorEmoji: String,
        authorProfileImageUrl: String = "",
        text: String,
        imageUrl: String? = null,
        parentCommentId: String? = null,
        eventOwnerId: String = "",
    ): Result<Unit> = try {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            Result.failure(IllegalArgumentException("Comment cannot be empty"))
        } else {
            val summaryRef = summaryRoot.document(eventId)
            val commentRef = summaryRef.collection("comments").document()
            val parentAuthorId = parentCommentId
                ?.takeIf { it.isNotBlank() }
                ?.let { parentId ->
                    summaryRef.collection("comments").document(parentId).get().await().getString("authorId").orEmpty()
                }
                .orEmpty()
            val isReply = parentAuthorId.isNotBlank()

            ActivityLogger.logAction(
                type = if (isReply) "reply" else "comment",
                text = if (isReply) "Replied to an event comment" else "Commented on an event",
                metadata = mapOf(
                    "eventId" to eventId,
                    "parentCommentId" to parentCommentId.orEmpty(),
                    "previewTitle" to (cleanText.takeIf { it.isNotBlank() } ?: "Comment"),
                    "previewSubtitle" to authorName,
                ),
            )

            firestore.runTransaction { transaction ->
                val summarySnapshot = transaction.get(summaryRef)
                val currentCount = (summarySnapshot.getLong("commentsCount") ?: 0L).toInt()

                transaction.set(
                    commentRef,
                    mapOf(
                        "authorId" to userId,
                        "authorName" to authorName,
                        "authorEmoji" to authorEmoji,
                        "authorProfileImageUrl" to authorProfileImageUrl,
                        "text" to cleanText,
                        "imageUrl" to imageUrl,
                        "parentCommentId" to parentCommentId,
                        "likesCount" to 0,
                        "likedBy" to emptyList<String>(),
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )

                transaction.set(
                    summaryRef,
                    mapOf(
                        "commentsCount" to (currentCount + 1),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }.await()

            val recipientId = parentAuthorId.takeIf { it.isNotBlank() && it != userId }
                ?: eventOwnerId.takeIf { it.isNotBlank() && it != userId }

            if (!recipientId.isNullOrBlank()) {
                runCatching {
                    NotificationLogger.notifyUser(
                        toUserId = recipientId,
                        type = if (isReply) "reply" else "comment",
                        title = if (isReply) "New reply" else "New comment",
                        body = if (isReply) {
                            "${authorName.ifBlank { "Someone" }} replied to your comment"
                        } else {
                            "${authorName.ifBlank { "Someone" }} commented on your event"
                        },
                        targetId = eventId,
                    )
                }
            }

            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteComment(eventId: String, commentId: String, userId: String): Result<Unit> = try {
        val summaryRef = summaryRoot.document(eventId)
        val commentRef = summaryRef.collection("comments").document(commentId)

        firestore.runTransaction { transaction ->
            val commentSnapshot = transaction.get(commentRef)
            if (!commentSnapshot.exists()) {
                throw IllegalStateException("Comment not found")
            }

            val authorId = commentSnapshot.getString("authorId").orEmpty()
            if (authorId != userId) {
                throw IllegalStateException("You can only delete your own comment")
            }

            val summarySnapshot = transaction.get(summaryRef)
            val currentCount = (summarySnapshot.getLong("commentsCount") ?: 0L).toInt()
            val updatedCount = (currentCount - 1).coerceAtLeast(0)

            transaction.delete(commentRef)
            transaction.set(
                summaryRef,
                mapOf(
                    "commentsCount" to updatedCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleCommentLike(
        eventId: String,
        commentId: String,
        userId: String,
    ): Result<Boolean> = try {
        val commentRef = summaryRoot.document(eventId).collection("comments").document(commentId)

        val liked = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Comment not found")
            }

            @Suppress("UNCHECKED_CAST")
            val currentLikedBy = (snapshot.get("likedBy") as? List<String>).orEmpty()
            val currentlyLiked = currentLikedBy.contains(userId)
            val updatedLikedBy = if (currentlyLiked) {
                currentLikedBy.filterNot { it == userId }
            } else {
                currentLikedBy + userId
            }

            transaction.set(
                commentRef,
                mapOf(
                    "likedBy" to updatedLikedBy,
                    "likesCount" to updatedLikedBy.size,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            !currentlyLiked
        }.await()

        if (liked) {
            val commentSnapshot = commentRef.get().await()
            val recipientId = commentSnapshot.getString("authorId").orEmpty()
            if (recipientId.isNotBlank() && recipientId != userId) {
                runCatching {
                    NotificationLogger.notifyUser(
                        toUserId = recipientId,
                        type = "like",
                        title = "New like",
                        body = "Someone liked your comment",
                        targetId = commentId,
                    )
                }
            }
        }

        Result.success(liked)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun incrementShare(eventId: String): Result<Unit> = try {
        summaryRoot.document(eventId)
            .set(
                mapOf(
                    "sharesCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
