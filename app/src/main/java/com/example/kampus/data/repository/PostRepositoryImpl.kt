package com.example.kampus.data.repository

import android.net.Uri
import com.example.kampus.ui.feed.PostItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepositoryImpl(private val firestore: FirebaseFirestore) {

    fun getFeedPosts(): Flow<Result<List<PostItem>>> = callbackFlow {
        val listener = firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        PostItem(
                            id = doc.id.hashCode(),
                            author = doc.getString("author") ?: "Unknown",
                            avatar = doc.getString("avatar") ?: "👤",
                            time = doc.getString("time") ?: "now",
                            content = doc.getString("content") ?: "",
                            mediaUris = emptyList(),
                            mediaTypes = emptyList(),
                            mediaEmojis = emptyList(),
                            likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                            comments = (doc.get("comments") as? Number)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(posts))
            }

        awaitClose { listener.remove() }
    }

    suspend fun createPost(post: Map<String, Any>): Result<String> = try {
        val ref = firestore.collection("posts").add(post).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun likePost(postId: String, isLiked: Boolean): Result<Unit> = try {
        firestore.collection("posts").document(postId).update("isLiked", isLiked).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deletePost(postId: String): Result<Unit> = try {
        firestore.collection("posts").document(postId).delete().await()
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
                        PostItem(
                            id = snapshot.id.hashCode(),
                            author = snapshot.getString("author") ?: "Unknown",
                            avatar = snapshot.getString("avatar") ?: "👤",
                            time = snapshot.getString("time") ?: "now",
                            content = snapshot.getString("content") ?: "",
                            mediaUris = emptyList(),
                            mediaTypes = emptyList(),
                            mediaEmojis = emptyList(),
                            likes = (snapshot.get("likes") as? Number)?.toInt() ?: 0,
                            comments = (snapshot.get("comments") as? Number)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null

                trySend(Result.success(post))
            }

        awaitClose { listener.remove() }
    }
}
