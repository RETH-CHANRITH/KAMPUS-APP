package com.example.kampus.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class PostEngagementSummary(
    val likesCount: Int = 0,
)

data class PostMemberEngagement(
    val liked: Boolean = false,
)

data class PostEngagementSnapshot(
    val likesCount: Int? = null,
    val likedByCurrentUser: Boolean = false,
)

class PostEngagementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val summaryRoot = firestore.collection("post_engagements")

    fun observeSummary(postId: String): Flow<Result<PostEngagementSummary>> = callbackFlow {
        val listener: ListenerRegistration = summaryRoot.document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                trySend(
                    Result.success(
                        PostEngagementSummary(
                            likesCount = (snapshot?.getLong("likesCount") ?: 0L).toInt(),
                        ),
                    ),
                )
            }

        awaitClose { listener.remove() }
    }

    fun observeMemberState(postId: String, userId: String): Flow<Result<PostMemberEngagement>> = callbackFlow {
        val listener: ListenerRegistration = summaryRoot.document(postId)
            .collection("members")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                trySend(
                    Result.success(
                        PostMemberEngagement(
                            liked = snapshot?.getBoolean("liked") ?: false,
                        ),
                    ),
                )
            }

        awaitClose { listener.remove() }
    }

    suspend fun loadSnapshot(postId: String, userId: String? = null): Result<PostEngagementSnapshot> = try {
        val summarySnapshot = summaryRoot.document(postId).get().await()
        val memberLiked = if (userId.isNullOrBlank()) {
            false
        } else {
            summaryRoot.document(postId)
                .collection("members")
                .document(userId)
                .get()
                .await()
                .getBoolean("liked") ?: false
        }

        Result.success(
            PostEngagementSnapshot(
                likesCount = if (summarySnapshot.exists()) (summarySnapshot.getLong("likesCount") ?: 0L).toInt() else null,
                likedByCurrentUser = memberLiked,
            ),
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loadSnapshots(postIds: List<Int>, userId: String? = null): Result<Map<Int, PostEngagementSnapshot>> = try {
        val snapshots = coroutineScope {
            postIds.distinct().associateWith { postId ->
                async { loadSnapshot(postId.toString(), userId).getOrNull() }
            }.mapValues { (_, deferred) -> deferred.await() }
        }.mapNotNull { (postId, snapshot) ->
            snapshot?.let { postId to it }
        }.toMap()

        Result.success(snapshots)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> = try {
        val summaryRef = summaryRoot.document(postId)
        val memberRef = summaryRef.collection("members").document(userId)

        val nextValue = firestore.runTransaction { transaction ->
            val memberSnapshot = transaction.get(memberRef)
            val currentLiked = memberSnapshot.getBoolean("liked") ?: false
            val nextLiked = !currentLiked

            val summarySnapshot = transaction.get(summaryRef)
            val currentCount = (summarySnapshot.getLong("likesCount") ?: 0L).toInt()
            val updatedCount = (currentCount + if (nextLiked) 1 else -1).coerceAtLeast(0)

            transaction.set(
                memberRef,
                mapOf(
                    "liked" to nextLiked,
                    "userId" to userId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            transaction.set(
                summaryRef,
                mapOf(
                    "likesCount" to updatedCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )

            nextLiked
        }.await()

        Result.success(nextValue)
    } catch (e: Exception) {
        Result.failure(e)
    }
}