package com.example.kampus.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.PostRepositoryImpl
import com.example.kampus.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * UI State for feed screen
 */
data class FeedUiState(
    val posts: List<PostItem> = emptyList(),
    val likedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * ViewModel for Feed/Home screen
 * Manages post data, likes, and feed interactions
 */
class FeedViewModel : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Separate StateFlows for backward compatibility with existing UI
    private val _posts = MutableStateFlow<List<PostItem>>(emptyList())
    val posts: StateFlow<List<PostItem>> = _posts.asStateFlow()

    private val _likedIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedIds: StateFlow<Set<Int>> = _likedIds.asStateFlow()

    private val postRepository = PostRepositoryImpl(FirebaseFirestore.getInstance())

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            postRepository.getFeedPosts().collect { result ->
                result.onSuccess { posts ->
                    _posts.update { posts }
                    _uiState.update { it.copy(posts = posts, isLoading = false) }
                }
                result.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public Methods - Post Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Toggle like status for a post - with real-time Firestore sync
     */
    fun toggleLike(postId: Int) {
        val post = _posts.value.firstOrNull { it.id == postId }

        _likedIds.update { currentLiked ->
            if (postId in currentLiked) {
                currentLiked - postId
            } else {
                currentLiked + postId
            }
        }

        val isNowLiked = postId in _likedIds.value
        if (isNowLiked && post != null) {
            ActivityLogger.logAction(
                type = "like_post",
                text = "Liked a post by ${post.author}",
                metadata = mapOf("postId" to post.id.toString(), "author" to post.author),
            )
        }

        // Update like count in post
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) {
                    val likeDelta = if (isNowLiked) 1 else -1
                    post.copy(likes = (post.likes + likeDelta).coerceAtLeast(0))
                } else {
                    post
                }
            }
        }

        _uiState.update { state ->
            state.copy(posts = _posts.value)
        }

        // Persist like count to Firestore for real-time sync
        viewModelScope.launch {
            val updatedLikeCount = _posts.value.firstOrNull { it.id == postId }?.likes ?: 0
            val result = postRepository.updatePostLikes(postId.toString(), updatedLikeCount)
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "like_post_sync_failed",
                    text = "Failed to sync like for post $postId: ${error.message}",
                    metadata = mapOf("postId" to postId.toString(), "error" to error.message.toString()),
                )
            }
        }
    }

    /**
     * Create a new post with MULTIPLE media items
     * Preferred method for multi-media support
     */
    fun createPostWithMultipleMedia(
        content: String,
        mediaUris: List<Uri> = emptyList(),
        mediaTypes: List<PostItem.MediaType> = emptyList(),
        mediaEmojis: List<String> = emptyList(),
        visibility: PostItem.PostVisibility = PostItem.PostVisibility.PUBLIC,
        allowComments: Boolean = true,
        taggedPeople: List<String> = emptyList(),
        feelingEmoji: String? = null,
        location: String? = null,
    ) {
        // Validate content
        if (content.isBlank() && mediaUris.isEmpty()) {
            setError("Post content or media is required")
            return
        }

        // Validate that all lists have same length
        if (mediaUris.isNotEmpty() && (mediaTypes.size != mediaUris.size || mediaEmojis.size != mediaUris.size)) {
            setError("Media uris, types, and emojis must have same length")
            return
        }

        try {
            // Create new post with multiple media
            val newPost = PostItem(
                id = generatePostId(),
                author = "You",
                avatar = "🧑",
                time = "now",
                content = content,
                mediaUris = mediaUris,
                mediaTypes = mediaTypes,
                mediaEmojis = mediaEmojis,
                likes = 0,
                comments = 0,
                isVerified = false,
                feeling = null,
                location = location,
                tags = extractHashtags(content),
                visibility = visibility,
                allowComments = allowComments,
                taggedPeople = taggedPeople,
                feelingEmoji = feelingEmoji,
            )

            // Add to top of feed
            _posts.update { currentPosts ->
                listOf(newPost) + currentPosts
            }
            _uiState.update { it.copy(posts = _posts.value) }

            persistPost(newPost)

            ActivityLogger.logAction(
                type = "create_post",
                text = "Created a post",
                metadata = mapOf("postId" to newPost.id),
            )

            clearError()
        } catch (e: Exception) {
            setError("Failed to create post: ${e.message}")
        }
    }

    /**
     * Create a new post
     * Backward compatible single-media version
     * For multi-media, use createPostWithMultipleMedia() instead
     */
    fun createPost(
        content: String,
        mediaUri: Uri? = null,
        mediaType: PostItem.MediaType? = null,
        visibility: PostItem.PostVisibility = PostItem.PostVisibility.PUBLIC,
        allowComments: Boolean = true,
        taggedPeople: List<String> = emptyList(),
        feelingEmoji: String? = null,
        location: String? = null,
    ) {
        // Validate content
        if (content.isBlank() && mediaUri == null) {
            setError("Post content or media is required")
            return
        }

        try {
            // Create new post
            val newPost = PostItem(
                id = generatePostId(),
                author = "You",
                avatar = "🧑",
                time = "now",
                content = content,
                imageUri = mediaUri,
                mediaType = mediaType,
                likes = 0,
                comments = 0,
                isVerified = false,
                feeling = null,
                location = location,
                tags = extractHashtags(content),
                visibility = visibility,
                allowComments = allowComments,
                taggedPeople = taggedPeople,
                feelingEmoji = feelingEmoji,
            )

            // Add to top of feed
            _posts.update { currentPosts ->
                listOf(newPost) + currentPosts
            }
            _uiState.update { it.copy(posts = _posts.value) }

            persistPost(newPost)

            ActivityLogger.logAction(
                type = "create_post",
                text = "Created a post",
                metadata = mapOf("postId" to newPost.id),
            )

            clearError()
        } catch (e: Exception) {
            setError("Failed to create post: ${e.message}")
        }
    }

    /**
     * Add post with MULTIPLE media - NavGraph compatible
     * Preferred method for multi-media support
     */
    fun addPost(
        text: String,
        mediaUris: List<Uri> = emptyList(),
        mediaTypes: List<PostItem.MediaType> = emptyList(),
        mediaEmojis: List<String> = emptyList(),
        visibility: PostItem.PostVisibility = PostItem.PostVisibility.PUBLIC,
        allowComments: Boolean = true,
        tags: List<String> = emptyList(),
        taggedPeople: List<String> = emptyList(),
        feelingEmoji: String? = null,
        location: String? = null,
    ) {
        createPostWithMultipleMedia(
            content = text,
            mediaUris = mediaUris,
            mediaTypes = mediaTypes,
            mediaEmojis = mediaEmojis,
            visibility = visibility,
            allowComments = allowComments,
            taggedPeople = taggedPeople,
            feelingEmoji = feelingEmoji,
            location = location,
        )
    }

    /**
     * Add post - Backward compatible single-media version
     * Wraps createPost with parameter mapping for NavGraph.kt compatibility
     */
    @Deprecated("Use addPost with List<Uri> parameters for multi-media support")
    fun addPost(
        text: String,
        imageUri: Uri? = null,
        mediaType: PostItem.MediaType? = null,
        feeling: String? = null,
        location: String? = null,
        visibility: PostItem.PostVisibility = PostItem.PostVisibility.PUBLIC,
        allowComments: Boolean = true,
        tags: List<String> = emptyList(),
        taggedPeople: List<String> = emptyList(),
        feelingEmoji: String? = null,
    ) {
        createPost(
            content = text,
            mediaUri = imageUri,
            mediaType = mediaType,
            visibility = visibility,
            allowComments = allowComments,
            taggedPeople = taggedPeople,
            feelingEmoji = feelingEmoji,
            location = location,
        )
    }

    /**
     * Delete a post - removes from local state and persists deletion to Firestore
     */
    fun deletePost(postId: Int) {
        // Remove from local state immediately for responsive UI
        _posts.update { currentPosts ->
            currentPosts.filterNot { it.id == postId }
        }
        _uiState.update { it.copy(posts = _posts.value) }

        // Remove from liked if applicable
        _likedIds.update { it - postId }

        // Persist deletion to Firestore
        viewModelScope.launch {
            val postIdString = postId.toString()
            val result = postRepository.deletePost(postIdString)
            result.onSuccess {
                ActivityLogger.logAction(
                    type = "delete_post",
                    text = "Deleted post $postId",
                    metadata = mapOf("postId" to postIdString),
                )
            }
            result.onFailure { error ->
                // If deletion fails, log the error but keep it removed from UI
                ActivityLogger.logAction(
                    type = "delete_post_failed",
                    text = "Failed to delete post $postId: ${error.message}",
                    metadata = mapOf("postId" to postIdString, "error" to error.message.toString()),
                )
            }
        }
    }

    fun pinPostBackend(postId: Int, isPinned: Boolean = true) {
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) post.copy(isPinned = isPinned) else post
            }
        }
        _uiState.update { it.copy(posts = _posts.value) }
        
        ActivityLogger.logAction(
            type = if (isPinned) "pin_post" else "unpin_post",
            text = if (isPinned) "Pinned post $postId" else "Unpinned post $postId",
        )
        
        // Persist to Firestore for real-time sync
        viewModelScope.launch {
            val result = postRepository.updatePostPin(postId.toString(), isPinned)
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "pin_post_sync_failed",
                    text = "Failed to ${if (isPinned) "pin" else "unpin"} post $postId: ${error.message}",
                    metadata = mapOf("postId" to postId.toString(), "isPinned" to isPinned.toString()),
                )
            }
        }
    }

    fun updatePostVisibility(postId: Int, visibility: PostItem.PostVisibility) {
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) post.copy(visibility = visibility) else post
            }
        }
        _uiState.update { it.copy(posts = _posts.value) }
        
        ActivityLogger.logAction(
            type = "edit_privacy",
            text = "Updated visibility for post $postId to $visibility",
        )
        
        // Persist to Firestore for real-time sync
        viewModelScope.launch {
            val result = postRepository.updatePostVisibility(postId.toString(), visibility.toString())
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "visibility_update_failed",
                    text = "Failed to update visibility for post $postId: ${error.message}",
                    metadata = mapOf("postId" to postId.toString(), "visibility" to visibility.toString()),
                )
            }
        }
    }

    fun hideFromProfileBackend(postId: Int) {
        // Update local state to hide from profile
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) post.copy(visibility = PostItem.PostVisibility.PRIVATE) else post
            }
        }
        _uiState.update { it.copy(posts = _posts.value) }
        
        ActivityLogger.logAction(
            type = "hide_from_profile",
            text = "Hide post $postId from profile",
        )
        
        // Persist to Firestore for real-time sync
        viewModelScope.launch {
            val result = postRepository.hidePostFromProfile(postId.toString(), true)
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "hide_from_profile_failed",
                    text = "Failed to hide post $postId from profile: ${error.message}",
                    metadata = mapOf("postId" to postId.toString(), "error" to error.message.toString()),
                )
            }
        }
    }

    /**
     * Restore a deleted post back to the feed
     */
    fun restorePost(post: PostItem) {
        _posts.update { current -> listOf(post) + current.filterNot { it.id == post.id } }
        _uiState.update { it.copy(posts = _posts.value) }
        
        // Persist restoration to Firestore
        persistPost(post)
        
        ActivityLogger.logAction(
            type = "restore_post",
            text = "Restored post ${post.id}",
            metadata = mapOf("postId" to post.id.toString()),
        )
    }

    /**
     * Update like count for a specific post
     */
    fun updatePostLikes(postId: Int, likeCount: Int) {
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) {
                    post.copy(likes = likeCount)
                } else {
                    post
                }
            }
        }
        _uiState.update { it.copy(posts = _posts.value) }
    }

    /**
     * Update comment count for a specific post
     */
    fun updatePostComments(postId: Int, commentCount: Int) {
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) {
                    post.copy(comments = commentCount)
                } else {
                    post
                }
            }
        }
        _uiState.update { it.copy(posts = _posts.value) }
    }

    /**
     * Reload all posts from backend
     * TODO: Connect to Firebase/backend when ready
     */
    fun refreshFeed() {
        // The feed is already backed by a realtime Firestore listener.
        // Keep this as a lightweight state reset for callers that still invoke refresh.
        clearError()
        _uiState.update { it.copy(isLoading = false, posts = _posts.value) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Methods - Helper Functions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Load mock posts for development/testing
     */
    private fun loadMockPosts() {
        val mockPosts = listOf(
            PostItem(
                id = 1,
                author = "Sarah Johnson",
                avatar = "👩‍💻",
                time = "2 hours ago",
                content = "Just finished my project! Really excited about the result 🎉",
                imageEmoji = "🎨",
                likes = 156,
                comments = 23,
                isVerified = true,
                feeling = null,
                location = "San Francisco, CA",
                tags = listOf("#project", "#excited"),
                visibility = PostItem.PostVisibility.PUBLIC,
                allowComments = true,
                taggedPeople = listOf("John Doe", "Emily Smith"),
                feelingEmoji = "😊",
            ),
            PostItem(
                id = 2,
                author = "Alex Kumar",
                avatar = "👨‍🔬",
                time = "4 hours ago",
                content = "Coffee and coding! Perfect Friday vibes ☕✨",
                imageUri = null,
                mediaType = null,
                likes = 89,
                comments = 12,
                isVerified = false,
                feeling = null,
                location = "New York, NY",
                tags = listOf("#coding", "#friday"),
                visibility = PostItem.PostVisibility.PUBLIC,
                allowComments = true,
                taggedPeople = emptyList(),
                feelingEmoji = "🥳",
            ),
            PostItem(
                id = 3,
                author = "Luna Martinez",
                avatar = "👩‍🎨",
                time = "6 hours ago",
                content = "New design system live! Check it out and let me know what you think",
                imageEmoji = "🎨",
                likes = 234,
                comments = 45,
                isVerified = true,
                feeling = null,
                location = "Los Angeles, CA",
                tags = listOf("#design", "#ui"),
                visibility = PostItem.PostVisibility.PUBLIC,
                allowComments = true,
                taggedPeople = listOf("Sarah Johnson"),
                feelingEmoji = "😎",
            ),
            PostItem(
                id = 4,
                author = "Marcus Chen",
                avatar = "👨‍🎓",
                time = "1 day ago",
                content = "Starting a new learning journey in mobile development! Any tips? 👇",
                imageUri = null,
                mediaType = null,
                likes = 120,
                comments = 67,
                isVerified = false,
                feeling = null,
                location = "Seattle, WA",
                tags = listOf("#learning", "#mobile"),
                visibility = PostItem.PostVisibility.PUBLIC,
                allowComments = true,
                taggedPeople = emptyList(),
                feelingEmoji = "🤔",
            ),
            PostItem(
                id = 5,
                author = "Zara Patel",
                avatar = "👩‍🚀",
                time = "1 day ago",
                content = "Just launched my first SaaS product! Feeling both nervous and excited 🚀",
                imageEmoji = "🚀",
                likes = 312,
                comments = 89,
                isVerified = true,
                feeling = null,
                location = "Austin, TX",
                tags = listOf("#startup", "#launch"),
                visibility = PostItem.PostVisibility.PUBLIC,
                allowComments = true,
                taggedPeople = listOf("Alex Kumar", "Marcus Chen"),
                feelingEmoji = null,
            ),
        )

        _posts.update { mockPosts }
        _uiState.update { it.copy(posts = mockPosts) }
    }

    /**
     * Generate unique post ID
     */
    private fun generatePostId(): Int {
        return _posts.value.maxOfOrNull { it.id }?.plus(1) ?: 1
    }

    /**
     * Extract hashtags from post content
     */
    private fun extractHashtags(content: String): List<String> {
        return Regex("""#\w+""").findAll(content).map { it.value }.toList()
    }

    /**
     * Get current timestamp for posts
     */
    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun persistPost(post: PostItem) {
        viewModelScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val payload = mapOf(
                "id" to post.id,
                "author" to post.author,
                "avatar" to post.avatar,
                "time" to post.time,
                "content" to post.content,
                "likes" to post.likes,
                "comments" to post.comments,
                "timestamp" to System.currentTimeMillis(),
                "userId" to (currentUserId ?: ""),
            )

            val result = postRepository.createPost(payload)
            result.onFailure { error ->
                setError("Post saved locally, sync failed: ${error.message}")
            }
            result.onSuccess {
                ActivityLogger.logAction(
                    type = "post_persisted",
                    text = "Post ${post.id} synced to Firestore",
                    metadata = mapOf("postId" to post.id.toString()),
                )
            }
        }
    }

    /**
     * Set error message
     */
    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TODO: Backend Integration Methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TODO: Inject repository when available
     * private val postRepository: IPostRepository by inject()
     *
     * Fetch posts from Firebase/backend
     * suspend fun syncPostsFromBackend() {
     *     val result = postRepository.getFeedPosts()
     *     _posts.value = result.data ?: emptyList()
     * }
     *
     * Upload new post to Firebase
     * suspend fun uploadPost(post: PostItem) {
     *     val result = postRepository.createPost(post)
     *     if (result.isSuccess) {
     *         createPost(...)
     *     }
     * }
     *
     * Sync likes with backend
     * suspend fun syncLikes(postId: Int, isLiked: Boolean) {
     *     postRepository.toggleLike(postId, isLiked)
     * }
     */
}
