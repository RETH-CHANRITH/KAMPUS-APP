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
     * Toggle like status for a post
     */
    fun toggleLike(postId: Int) {
        _likedIds.update { currentLiked ->
            if (postId in currentLiked) {
                currentLiked - postId
            } else {
                currentLiked + postId
            }
        }

        // Update like count in post
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) {
                    val isNowLiked = postId in _likedIds.value
                    val likeDelta = if (isNowLiked) 1 else -1
                    post.copy(likes = (post.likes + likeDelta).coerceAtLeast(0))
                } else {
                    post
                }
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
     * Delete a post
     */
    fun deletePost(postId: Int) {
        _posts.update { currentPosts ->
            currentPosts.filterNot { it.id == postId }
        }

        // Remove from liked if applicable
        _likedIds.update { it - postId }
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
    }

    /**
     * Reload all posts from backend
     * TODO: Connect to Firebase/backend when ready
     */
    fun refreshFeed() {
        _uiState.update { it.copy(isLoading = true) }

        try {
            // TODO: Fetch from Firebase/backend repository
            // For now, use mock data
            loadMockPosts()
            clearError()
        } catch (e: Exception) {
            setError("Failed to refresh feed: ${e.message}")
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
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
