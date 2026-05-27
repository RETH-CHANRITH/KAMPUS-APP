package com.example.kampus.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.PostEngagementRepository
import com.example.kampus.di.SupabaseModule
import com.example.kampus.ui.feed.PostItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PostComment(
    val id: String,
    val postId: Int,
    val userId: String,
    val username: String,
    val userAvatar: String,
    val userProfileImageUrl: String = "",
    val text: String,
    val imageUrl: String? = null,
    val createdAt: Long,
    val parentCommentId: String? = null,
    val likesCount: Int = 0,
    val likedByCurrentUser: Boolean = false,
    val replies: List<PostComment> = emptyList(),
)

data class PostDetailUiState(
    val post: PostItem? = null,
    val comments: List<PostComment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class PostViewModel : ViewModel() {

    private data class AuthorIdentity(
        val displayName: String,
        val avatarEmoji: String,
        val profileImageUrl: String,
    )

    private val firestore = FirebaseFirestore.getInstance()
    private val postEngagementRepository = PostEngagementRepository(firestore)
    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    @Volatile
    private var currentUserIdentity = AuthorIdentity("You", "👤", "")

    private var postListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            currentUserIdentity = resolveCurrentUserIdentity()
            normalizeCurrentPost()
        }
    }

    private data class CommentAuthorProfile(
        val username: String,
        val avatar: String,
        val profileImageUrl: String,
    )

    fun observePost(postId: Int) {
        postListener?.remove()
        commentsListener?.remove()
        _uiState.update { it.copy(isLoading = true, error = null) }

        postListener = firestore.collection("posts")
            .whereEqualTo("id", postId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val doc = snapshot?.documents?.firstOrNull()
                if (doc == null) {
                    _uiState.update { it.copy(isLoading = false, post = null) }
                    return@addSnapshotListener
                }

                val mediaUrls = (doc.get("mediaUrls") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                val legacyMediaUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() }
                    ?: doc.getString("imageUri")?.takeIf { it.isNotBlank() }
                val resolvedMediaUrls = if (mediaUrls.isNotEmpty()) mediaUrls else legacyMediaUrl?.let { listOf(it) }.orEmpty()
                val mediaTypes = ((doc.get("mediaTypes") as? List<*>)?.mapNotNull { value ->
                    when ((value as? String)?.lowercase()) {
                        "video" -> PostItem.MediaType.VIDEO
                        "image" -> PostItem.MediaType.IMAGE
                        else -> null
                    }
                } ?: emptyList())
                val resolvedMediaTypes = if (resolvedMediaUrls.isNotEmpty() && mediaTypes.size == resolvedMediaUrls.size) mediaTypes else resolvedMediaUrls.map { PostItem.MediaType.IMAGE }

                val post = PostItem(
                    id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                    author = doc.getString("author") ?: "Unknown",
                    avatar = doc.getString("avatar") ?: "👤",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    time = doc.getString("time") ?: "now",
                    content = doc.getString("content") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    mediaUris = resolvedMediaUrls.map(Uri::parse),
                    mediaTypes = resolvedMediaTypes,
                    mediaEmojis = emptyList(),
                    likes = (doc.getLong("likes") ?: 0L).toInt(),
                    comments = (doc.getLong("comments") ?: 0L).toInt(),
                    shares = (doc.getLong("shares") ?: 0L).toInt(),
                    isVerified = doc.getBoolean("isVerified") ?: false,
                    feeling = doc.getString("feeling"),
                    location = doc.getString("location"),
                    tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                    allowComments = doc.getBoolean("allowComments") ?: true,
                    taggedPeople = (doc.get("taggedPeople") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                    feelingEmoji = doc.getString("feelingEmoji"),
                    isPinned = doc.getBoolean("isPinned") ?: false,
                )

                _uiState.update { it.copy(post = normalizeForCurrentUser(post), isLoading = false, error = null) }

                viewModelScope.launch {
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    val engagementSnapshot = postEngagementRepository
                        .loadSnapshot(postId.toString(), currentUserId)
                        .getOrNull()

                    if (engagementSnapshot != null) {
                        _uiState.update { state ->
                            state.copy(
                                post = state.post?.copy(
                                    likes = engagementSnapshot.likesCount ?: state.post?.likes ?: 0,
                                ),
                            )
                        }
                    }
                }

                commentsListener?.remove()
                commentsListener = firestore.collection("post_comments")
                    .whereEqualTo("postId", postId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { commentSnapshot, commentError ->
                        if (commentError != null) {
                            _uiState.update { it.copy(error = commentError.message) }
                        } else {
                            val mappedComments = commentSnapshot?.documents?.mapNotNull { commentDoc ->
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val likedBy = (commentDoc.get("likedBy") as? List<String>).orEmpty()
                                    PostComment(
                                        id = commentDoc.id,
                                        postId = (commentDoc.getLong("postId") ?: postId.toLong()).toInt(),
                                        userId = commentDoc.getString("userId") ?: "",
                                        username = commentDoc.getString("username") ?: "Someone",
                                        userAvatar = commentDoc.getString("userAvatar") ?: "👤",
                                        userProfileImageUrl = commentDoc.getString("userProfileImageUrl") ?: "",
                                        text = commentDoc.getString("text") ?: "",
                                        imageUrl = commentDoc.getString("imageUrl")?.takeIf { it.isNotBlank() },
                                        createdAt = commentDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        parentCommentId = commentDoc.getString("parentCommentId")?.takeIf { it.isNotBlank() },
                                        likesCount = (commentDoc.getLong("likesCount") ?: likedBy.size.toLong()).toInt(),
                                        likedByCurrentUser = likedBy.contains(FirebaseAuth.getInstance().currentUser?.uid),
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }.orEmpty()

                            val repliesByParent = mappedComments
                                .filter { !it.parentCommentId.isNullOrBlank() }
                                .groupBy { it.parentCommentId!! }

                            val comments = mappedComments
                                .filter { it.parentCommentId.isNullOrBlank() }
                                .sortedByDescending { it.createdAt }
                                .map { comment ->
                                    comment.copy(
                                        replies = repliesByParent[comment.id].orEmpty().sortedBy { it.createdAt },
                                    )
                                }

                            _uiState.update { it.copy(comments = comments, error = null) }
                        }
                    }
            }
    }

    suspend fun addComment(postId: Int, text: String, imageUri: Uri? = null, parentCommentId: String? = null): Result<Unit> {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(IllegalStateException("Sign in to comment"))

            val commentText = text.trim()
            if (commentText.isBlank()) {
                return Result.failure(IllegalArgumentException("Comment cannot be empty"))
            }

            val profile = resolveCurrentUserProfile(user.uid)
            val imageUrl = imageUri?.let { selectedImage ->
                SupabaseModule.getStorageManager()
                    .uploadPostCommentImage(user.uid, postId.toString(), selectedImage)
                    .getOrElse { error -> return Result.failure(error) }
            }

            val username = profile.username
            firestore.collection("post_comments")
                .document()
                .set(
                    mapOf(
                        "postId" to postId,
                        "userId" to user.uid,
                        "username" to username,
                        "userAvatar" to profile.avatar,
                        "userProfileImageUrl" to profile.profileImageUrl,
                        "text" to commentText,
                        "imageUrl" to imageUrl,
                        "parentCommentId" to parentCommentId,
                        "likesCount" to 0,
                        "likedBy" to emptyList<String>(),
                        "createdAt" to System.currentTimeMillis(),
                    )
                )
                .await()

            firestore.collection("posts")
                .whereEqualTo("id", postId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.reference
                ?.update("comments", FieldValue.increment(1))
                ?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLike(postId: Int, commentId: String): Result<Boolean> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(IllegalStateException("Sign in to like a comment"))

            val commentRef = firestore.collection("post_comments").document(commentId)
            val liked = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                if (!snapshot.exists()) throw IllegalStateException("Comment not found")

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
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                )

                !currentlyLiked
            }.await()

            Result.success(liked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReply(postId: Int, parentCommentId: String, text: String, imageUri: Uri? = null): Result<Unit> {
        return addComment(postId = postId, text = text, imageUri = imageUri, parentCommentId = parentCommentId)
    }

    private suspend fun resolveCurrentUserProfile(userId: String): CommentAuthorProfile {
        val user = FirebaseAuth.getInstance().currentUser
        val profileSnapshot = firestore.collection("users").document(userId).get().await()

        val username = profileSnapshot.getString("displayName")
            ?: user?.displayName
            ?: "You"
        val avatar = profileSnapshot.getString("avatarEmoji")
            ?: user?.photoUrl?.let { "👤" }
            ?: "👤"
        val profileImageUrl = profileSnapshot.getString("profileImageUrl")
            ?: user?.photoUrl?.toString().orEmpty()

        return CommentAuthorProfile(
            username = username,
            avatar = avatar,
            profileImageUrl = profileImageUrl,
        )
    }

    override fun onCleared() {
        super.onCleared()
        postListener?.remove()
        commentsListener?.remove()
    }

    private suspend fun resolveCurrentUserIdentity(): AuthorIdentity {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return AuthorIdentity("You", "👤", "")
        val profile = firestore.collection("users").document(currentUser.uid).get().await()

        return AuthorIdentity(
            displayName = profile.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: currentUser.displayName?.takeIf { it.isNotBlank() }
                ?: "You",
            avatarEmoji = profile.getString("avatarEmoji")?.takeIf { it.isNotBlank() }
                ?: "👤",
            profileImageUrl = profile.getString("profileImageUrl")?.takeIf { it.isNotBlank() }
                ?: currentUser.photoUrl?.toString().orEmpty(),
        )
    }

    private fun normalizeForCurrentUser(post: PostItem): PostItem {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return post
        if (post.authorId != currentUserId) return post

        return post.copy(
            author = currentUserIdentity.displayName,
            avatar = currentUserIdentity.avatarEmoji,
            profileImageUrl = currentUserIdentity.profileImageUrl,
        )
    }

    private fun normalizeCurrentPost() {
        val currentPost = _uiState.value.post ?: return
        _uiState.update { it.copy(post = normalizeForCurrentUser(currentPost)) }
    }
}