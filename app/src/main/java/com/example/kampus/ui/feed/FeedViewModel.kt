package com.example.kampus.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.di.SupabaseModule
import com.example.kampus.data.repository.PostEngagementRepository
import com.example.kampus.data.repository.PostRepositoryImpl
import com.example.kampus.ui.chat.ChatStory
import com.example.kampus.utils.ActivityLogger
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * UI State for feed screen
 */
data class FeedUiState(
    val posts: List<PostItem> = emptyList(),
    val stories: List<ChatStory> = emptyList(),
    val likedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserProfileImageUrl: String = "",
    val currentUserAvatarEmoji: String = "👤",
)

/**
 * ViewModel for Feed/Home screen
 * Manages post data, likes, and feed interactions
 */
class FeedViewModel : ViewModel() {

    private data class AuthorIdentity(
        val displayName: String,
        val avatarEmoji: String,
        val profileImageUrl: String,
    )

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
    private val postEngagementRepository = PostEngagementRepository(FirebaseFirestore.getInstance())
    private var storiesListener: ListenerRegistration? = null
    @Volatile
    private var currentUserIdentity = AuthorIdentity("You", "🧑", "")

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            postRepository.backfillLegacyPostAuthorIds()
            currentUserIdentity = resolveCurrentUserIdentity()
            backfillCurrentUserPostIdentity(currentUserIdentity)
            normalizeOwnedPosts(currentUserIdentity)
        }
        observeStories()
        loadCurrentUserStoryProfile()
        loadPosts()
    }

    override fun onCleared() {
        storiesListener?.remove()
        storiesListener = null
        super.onCleared()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            postRepository.getFeedPosts().collect { result ->
                result.onSuccess { posts ->
                    val normalizedPosts = posts.map { normalizeForCurrentUser(it) }
                    val mergedPosts = buildList {
                        addAll(normalizedPosts)
                        _posts.value.forEach { localPost ->
                            if (normalizedPosts.none { it.id == localPost.id }) {
                                add(normalizeForCurrentUser(localPost))
                            }
                        }
                    }

                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                    val engagementSnapshots = postEngagementRepository
                        .loadSnapshots(mergedPosts.map { it.id }, currentUserId.ifBlank { null })
                        .getOrDefault(emptyMap())

                    val enrichedPosts = mergedPosts.map { post ->
                        val engagement = engagementSnapshots[post.id]
                        if (engagement != null) {
                            post.copy(
                                likes = engagement.likesCount ?: post.likes,
                                likedBy = if (currentUserId.isNotBlank() && engagement.likedByCurrentUser) listOf(currentUserId) else emptyList(),
                            )
                        } else {
                            post
                        }
                    }

                    _likedIds.value = enrichedPosts
                        .filter { currentUserId.isNotBlank() && currentUserId in it.likedBy }
                        .map { it.id }
                        .toSet()

                    _posts.update { enrichedPosts }
                    _uiState.update { it.copy(posts = enrichedPosts, isLoading = false) }
                }
                result.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            }
        }
    }

    private fun observeStories() {
        storiesListener?.remove()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        storiesListener = FirebaseFirestore.getInstance()
            .collection("stories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(64)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val now = System.currentTimeMillis()
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    val createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: now
                    val expiresAt = (doc.get("expiresAt") as? Number)?.toLong() ?: (createdAt + 86_400_000L)
                    if (expiresAt < now) return@mapNotNull null

                    val ownerId = doc.getString("userId") ?: doc.getString("ownerId") ?: ""
                    ChatStory(
                        id = doc.id,
                        ownerId = ownerId,
                        ownerName = doc.getString("ownerName") ?: doc.getString("userName") ?: "User",
                        ownerAvatarEmoji = doc.getString("ownerAvatarEmoji") ?: "👤",
                        ownerAvatarColor = (doc.get("ownerAvatarColor") as? Number)?.toLong() ?: 0xFF3B82F6,
                        ownerProfileImageUrl = doc.getString("ownerProfileImageUrl") ?: "",
                        note = doc.getString("note") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: doc.getString("imageUri") ?: "",
                        storyType = doc.getString("storyType") ?: if ((doc.getString("imageUrl") ?: doc.getString("imageUri")).isNullOrBlank()) "note" else "image",
                        overlayText = doc.getString("overlayText") ?: "",
                        overlayX = (doc.get("overlayX") as? Number)?.toFloat() ?: 0f,
                        overlayY = (doc.get("overlayY") as? Number)?.toFloat() ?: 0f,
                        overlayColor = (doc.get("overlayColor") as? Number)?.toLong() ?: 0xFFFFFFFF,
                        createdAtMillis = createdAt,
                        createdAtLabel = formatRelativeTime(createdAt),
                        isMine = ownerId == currentUserId,
                    )
                } ?: emptyList()

                _uiState.update { it.copy(stories = stories) }
            }
    }

    private fun loadCurrentUserStoryProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        _uiState.update {
            it.copy(
                currentUserProfileImageUrl = user.photoUrl?.toString().orEmpty(),
                currentUserAvatarEmoji = "👤",
            )
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

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
                metadata = mapOf(
                    "postId" to post.id.toString(),
                    "author" to post.author,
                    "previewTitle" to (post.content.takeIf { it.isNotBlank() } ?: "Post"),
                    "previewSubtitle" to post.author,
                    "previewImageUrl" to (post.getFirstMediaUri()?.toString() ?: ""),
                    "likeCount" to (post.likes + 1).toString(),
                ),
            )
        }

        // Update like count in post
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) {
                    val likeDelta = if (isNowLiked) 1 else -1
                    val updatedLikedBy = if (isNowLiked) {
                        (post.likedBy + currentUserId)
                    } else {
                        post.likedBy - currentUserId
                    }
                    post.copy(
                        likes = (post.likes + likeDelta).coerceAtLeast(0),
                        likedBy = updatedLikedBy.distinct(),
                    )
                } else {
                    post
                }
            }
        }

        _uiState.update { state ->
            state.copy(posts = _posts.value)
        }

        viewModelScope.launch {
            val result = postEngagementRepository.toggleLike(postId.toString(), currentUserId)
            result.onSuccess { liked ->
                _likedIds.update { currentLiked ->
                    if (liked) currentLiked + postId else currentLiked - postId
                }
            }
            result.onFailure { error ->
                _likedIds.update { currentLiked ->
                    if (isNowLiked) currentLiked - postId else currentLiked + postId
                }
                _posts.update { postList ->
                    postList.map { post ->
                        if (post.id == postId) {
                            val revertDelta = if (isNowLiked) -1 else 1
                            val revertedLikedBy = if (isNowLiked) {
                                post.likedBy - currentUserId
                            } else {
                                (post.likedBy + currentUserId).distinct()
                            }
                            post.copy(
                                likes = (post.likes + revertDelta).coerceAtLeast(0),
                                likedBy = revertedLikedBy.distinct(),
                            )
                        } else {
                            post
                        }
                    }
                }
                _uiState.update { state ->
                    state.copy(posts = _posts.value, errorMessage = error.message)
                }
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
    suspend fun createPostWithMultipleMedia(
        content: String,
        mediaUris: List<Uri> = emptyList(),
        mediaTypes: List<PostItem.MediaType> = emptyList(),
        mediaEmojis: List<String> = emptyList(),
        visibility: PostItem.PostVisibility = PostItem.PostVisibility.PUBLIC,
        allowComments: Boolean = true,
        taggedPeople: List<String> = emptyList(),
        feelingEmoji: String? = null,
        location: String? = null,
    ): Result<String> {
        // Validate content
        if (content.isBlank() && mediaUris.isEmpty()) {
            val error = IllegalArgumentException("Post content or media is required")
            setError(error.message ?: "Post content or media is required")
            return Result.failure(error)
        }

        // Validate that media types match the selected media count.
        if (mediaUris.isNotEmpty() && mediaTypes.size != mediaUris.size) {
            val error = IllegalArgumentException("Media uris and types must have same length")
            setError(error.message ?: "Media uris, types, and emojis must have same length")
            return Result.failure(error)
        }

        val normalizedMediaEmojis = when {
            mediaUris.isEmpty() -> emptyList()
            mediaEmojis.size == mediaUris.size -> mediaEmojis
            else -> List(mediaUris.size) { index -> mediaEmojis.getOrNull(index).orEmpty() }
        }

        try {
            val identity = resolveCurrentUserIdentity()
            // Create new post with multiple media
            val newPost = PostItem(
                id = generatePostId(),
                author = identity.displayName,
                avatar = identity.avatarEmoji,
                profileImageUrl = identity.profileImageUrl,
                time = "now",
                content = content,
                timestamp = System.currentTimeMillis(),
                mediaUris = mediaUris,
                mediaTypes = mediaTypes,
                mediaEmojis = normalizedMediaEmojis,
                likes = 0,
                comments = 0,
                shares = 0,
                isVerified = false,
                feeling = null,
                location = location,
                tags = extractHashtags(content),
                visibility = visibility,
                allowComments = allowComments,
                taggedPeople = taggedPeople,
                feelingEmoji = feelingEmoji,
            )

            val persistResult = persistPost(newPost)
            persistResult.onSuccess {
                _posts.update { currentPosts ->
                    listOf(newPost) + currentPosts.filterNot { it.id == newPost.id }
                }
                _uiState.update { it.copy(posts = _posts.value) }

                ActivityLogger.logAction(
                    type = "create_post",
                    text = "Created a post",
                    metadata = mapOf("postId" to newPost.id),
                )
                clearError()
            }
            persistResult.onFailure { error ->
                setError(error.message ?: "Failed to create post")
            }

            return persistResult
        } catch (e: Exception) {
            setError("Failed to create post: ${e.message}")
            return Result.failure(e)
        }
    }

    /**
     * Create a new post
     * Backward compatible single-media version
     * For multi-media, use createPostWithMultipleMedia() instead
     */
    suspend fun createPost(
        content: String,
        mediaUri: Uri? = null,
        mediaType: PostItem.MediaType? = null,
        visibility: PostItem.PostVisibility = PostItem.PostVisibility.PUBLIC,
        allowComments: Boolean = true,
        taggedPeople: List<String> = emptyList(),
        feelingEmoji: String? = null,
        location: String? = null,
    ): Result<String> {
        // Validate content
        if (content.isBlank() && mediaUri == null) {
            val error = IllegalArgumentException("Post content or media is required")
            setError(error.message ?: "Post content or media is required")
            return Result.failure(error)
        }

        try {
            val identity = resolveCurrentUserIdentity()
            // Create new post
            val newPost = PostItem(
                id = generatePostId(),
                author = identity.displayName,
                avatar = identity.avatarEmoji,
                profileImageUrl = identity.profileImageUrl,
                time = "now",
                content = content,
                timestamp = System.currentTimeMillis(),
                imageUri = mediaUri,
                mediaType = mediaType,
                likes = 0,
                comments = 0,
                shares = 0,
                isVerified = false,
                feeling = null,
                location = location,
                tags = extractHashtags(content),
                visibility = visibility,
                allowComments = allowComments,
                taggedPeople = taggedPeople,
                feelingEmoji = feelingEmoji,
            )

            val persistResult = persistPost(newPost)
            persistResult.onSuccess {
                _posts.update { currentPosts ->
                    listOf(newPost) + currentPosts.filterNot { it.id == newPost.id }
                }
                _uiState.update { it.copy(posts = _posts.value) }

                ActivityLogger.logAction(
                    type = "create_post",
                    text = "Created a post",
                    metadata = mapOf("postId" to newPost.id),
                )
                clearError()
            }
            persistResult.onFailure { error ->
                setError(error.message ?: "Failed to create post")
            }

            return persistResult
        } catch (e: Exception) {
            setError("Failed to create post: ${e.message}")
            return Result.failure(e)
        }
    }

    /**
     * Add post with MULTIPLE media - NavGraph compatible
     * Preferred method for multi-media support
     */
    suspend fun addPost(
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
    ): Result<String> {
        return createPostWithMultipleMedia(
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
    suspend fun addPost(
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
    ): Result<String> {
        return createPost(
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
        viewModelScope.launch {
            persistPost(post)
        }
        
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

    fun incrementShareCount(postId: Int) {
        _posts.update { postList ->
            postList.map { post ->
                if (post.id == postId) post.copy(shares = post.shares + 1) else post
            }
        }
        _uiState.update { it.copy(posts = _posts.value) }

        viewModelScope.launch {
            val updatedShareCount = _posts.value.firstOrNull { it.id == postId }?.shares ?: 0
            val result = postRepository.updatePostShares(postId.toString(), updatedShareCount)
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "share_post_sync_failed",
                    text = "Failed to sync share for post $postId: ${error.message}",
                    metadata = mapOf("postId" to postId.toString(), "error" to error.message.toString()),
                )
            }
        }
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
                timestamp = System.currentTimeMillis(),
                imageEmoji = "🎨",
                likes = 156,
                comments = 23,
                shares = 6,
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
                timestamp = System.currentTimeMillis(),
                imageUri = null,
                mediaType = null,
                likes = 89,
                comments = 12,
                shares = 2,
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
                timestamp = System.currentTimeMillis(),
                imageEmoji = "🎨",
                likes = 234,
                comments = 45,
                shares = 9,
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
                timestamp = System.currentTimeMillis(),
                imageUri = null,
                mediaType = null,
                likes = 120,
                comments = 67,
                shares = 4,
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
                timestamp = System.currentTimeMillis(),
                imageEmoji = "🚀",
                likes = 312,
                comments = 89,
                shares = 11,
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
        val candidate = UUID.randomUUID().mostSignificantBits xor UUID.randomUUID().leastSignificantBits
        return (candidate and Long.MAX_VALUE).toInt().takeIf { it != 0 } ?: 1
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

    private fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "now"
        val now = System.currentTimeMillis()
        val diffMillis = (now - timestamp).coerceAtLeast(0L)
        val diffMinutes = diffMillis / 60000L
        val diffHours = diffMinutes / 60L
        val diffDays = diffHours / 24L

        return when {
            diffMinutes < 1 -> "now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> getCurrentTime()
        }
    }

    private suspend fun persistPost(post: PostItem): Result<String> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("Post sync failed: user not authenticated"))

        val storageManager = runCatching { SupabaseModule.getStorageManager() }.getOrNull()
        val mediaSources = buildList {
            if (post.mediaUris.isNotEmpty()) {
                post.mediaUris.forEachIndexed { index, uri ->
                    add(uri to (post.mediaTypes.getOrNull(index) ?: PostItem.MediaType.IMAGE))
                }
            } else {
                @Suppress("DEPRECATION")
                post.imageUri?.let { uri ->
                    add(uri to (post.mediaType ?: PostItem.MediaType.IMAGE))
                }
            }
        }

        val uploadedMediaUrls = mutableListOf<String>()
        val uploadedMediaTypes = mutableListOf<String>()

        if (mediaSources.isNotEmpty()) {
            if (storageManager == null) {
                return Result.failure(IllegalStateException("Post saved locally, media sync failed: storage is unavailable"))
            }

            for ((mediaUri, mediaType) in mediaSources) {
                val uploadResult = storageManager.uploadStoryMedia(
                    userId = currentUserId,
                    mediaUri = mediaUri,
                    storyType = if (mediaType == PostItem.MediaType.VIDEO) "video" else "image",
                )

                val uploadedUrl = uploadResult.getOrElse { error ->
                    return Result.failure(IllegalStateException("Post saved locally, media sync failed: ${error.message}"))
                }

                uploadedMediaUrls += uploadedUrl
                uploadedMediaTypes += mediaType.name
            }
        }

        val payload = mutableMapOf<String, Any>(
            "id" to post.id,
            "author" to post.author,
            "authorId" to currentUserId,
            "avatar" to post.avatar,
            "profileImageUrl" to post.profileImageUrl,
            "time" to post.time,
            "content" to post.content,
            "privacy" to post.visibility.name.lowercase(),
            "likes" to post.likes,
            "comments" to post.comments,
            "shares" to post.shares,
            "timestamp" to System.currentTimeMillis(),
            "userId" to currentUserId,
        )

        if (uploadedMediaUrls.isNotEmpty()) {
            payload["mediaUrls"] = uploadedMediaUrls
            payload["mediaTypes"] = uploadedMediaTypes
        }

        val result = postRepository.createPost(payload)
        result.onSuccess {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .update("stats.posts", FieldValue.increment(1))

            ActivityLogger.logAction(
                type = "post_persisted",
                text = "Post ${post.id} synced to Firestore",
                metadata = mapOf("postId" to post.id.toString()),
            )
        }

        return result
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

    private suspend fun resolveCurrentUserIdentity(): AuthorIdentity {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return AuthorIdentity("You", "🧑", "")

        val profile = runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .await()
        }.getOrNull()

        val displayName = profile?.getString("displayName")
            ?.takeIf { it.isNotBlank() }
            ?: currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: "You"
        val avatarEmoji = profile?.getString("avatarEmoji")
            ?.takeIf { it.isNotBlank() }
            ?: "🧑"
        val profileImageUrl = profile?.getString("profileImageUrl")
            ?.takeIf { it.isNotBlank() }
            ?: currentUser.photoUrl?.toString().orEmpty()

        return AuthorIdentity(displayName, avatarEmoji, profileImageUrl)
    }

    private suspend fun backfillCurrentUserPostIdentity(identity: AuthorIdentity) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postsSnapshot = FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", currentUserId)
            .get()
            .await()

        val postsToUpdate = postsSnapshot.documents.filter { doc ->
            val author = doc.getString("author").orEmpty()
            author.isBlank() || author == "You" || author == "Unknown"
        }

        if (postsToUpdate.isEmpty()) return

        val batch = FirebaseFirestore.getInstance().batch()
        postsToUpdate.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf(
                    "author" to identity.displayName,
                    "avatar" to identity.avatarEmoji,
                    "profileImageUrl" to identity.profileImageUrl,
                ),
            )
        }
        batch.commit().await()
    }

    private fun normalizeForCurrentUser(post: PostItem): PostItem {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return post
        if (post.authorId != currentUserId) return post
        return if (post.author == "You" || post.author.isBlank() || post.author == "Unknown") {
            post.copy(
                author = currentUserIdentity.displayName,
                avatar = currentUserIdentity.avatarEmoji,
                profileImageUrl = currentUserIdentity.profileImageUrl,
            )
        } else {
            post.copy(
                avatar = currentUserIdentity.avatarEmoji.ifBlank { post.avatar },
                profileImageUrl = currentUserIdentity.profileImageUrl.ifBlank { post.profileImageUrl },
            )
        }
    }

    private fun normalizeOwnedPosts(identity: AuthorIdentity) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updatedPosts = _posts.value.map { post ->
            if (post.authorId == currentUserId && (post.author == "You" || post.author.isBlank() || post.author == "Unknown")) {
                post.copy(
                    author = identity.displayName,
                    avatar = identity.avatarEmoji,
                    profileImageUrl = identity.profileImageUrl,
                )
            } else {
                post
            }
        }
        _posts.update { updatedPosts }
        _uiState.update { it.copy(posts = updatedPosts) }
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
