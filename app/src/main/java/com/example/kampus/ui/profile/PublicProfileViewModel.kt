package com.example.kampus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.UserRepositoryImpl
import com.example.kampus.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

private fun Any?.toLongOrNullValue(): Long? = when (this) {
    is Number -> toLong()
    is String -> toLongOrNull()
    is com.google.firebase.Timestamp -> toDate().time
    else -> null
}

data class PublicProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val email: String = "",
    val phone: String = "",
    val faculty: String = "",
    val year: String = "",
    val location: String = "",
    val avatarEmoji: String = "👤",
    val profileImageUrl: String = "",
    val coverImageUrl: String = "",
    val posts: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val hasOutgoingRequest: Boolean = false,
    val outgoingRequestId: String = "",
    val hasIncomingRequest: Boolean = false,
    val incomingRequestId: String = "",
    val isActionLoading: Boolean = false,
    val activities: List<ProfileActivityItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class PublicProfileViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepositoryImpl(firestore, auth)
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var activityListener: ListenerRegistration? = null
    private var postsCountListener: ListenerRegistration? = null
    private var followersCountListener: ListenerRegistration? = null
    private var followingCountListener: ListenerRegistration? = null
    private var followingRelationListener: ListenerRegistration? = null
    private var outgoingRequestListener: ListenerRegistration? = null
    private var incomingRequestListener: ListenerRegistration? = null
    // Raw count flows used for debounce to avoid UI jitter on rapid updates
    private val followersRaw = MutableStateFlow<Int?>(null)
    private val followingRaw = MutableStateFlow<Int?>(null)
    private val postsRaw = MutableStateFlow<Int?>(null)
    private var countsCollectorJob: Job? = null
    private var appliedInitialProfileSnapshot = false

    fun observeUser(userId: String) {
        userListener?.remove()
        activityListener?.remove()
        postsCountListener?.remove()
        followersCountListener?.remove()
        followingCountListener?.remove()
        followingRelationListener?.remove()
        outgoingRequestListener?.remove()
        incomingRequestListener?.remove()
        appliedInitialProfileSnapshot = false

        val currentUserId = auth.currentUser?.uid
        val isOwnProfile = currentUserId == userId

        _uiState.update {
            it.copy(
                userId = userId,
                isOwnProfile = isOwnProfile,
                isLoading = true,
                error = null,
                isFollowing = false,
                hasOutgoingRequest = false,
                outgoingRequestId = "",
                hasIncomingRequest = false,
                incomingRequestId = "",
            )
        }

        // reset any previous collectors for counts
        countsCollectorJob?.cancel()
        countsCollectorJob = viewModelScope.launch {
            launch {
                followersRaw.debounce(300).distinctUntilChanged().collect { v ->
                    v?.let { _uiState.update { s -> s.copy(followers = it) } }
                }
            }
            launch {
                followingRaw.debounce(300).distinctUntilChanged().collect { v ->
                    v?.let { _uiState.update { s -> s.copy(following = it) } }
                }
            }
            launch {
                postsRaw.debounce(300).distinctUntilChanged().collect { v ->
                    v?.let { _uiState.update { s -> s.copy(posts = it) } }
                }
            }
        }

        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    _uiState.update { it.copy(isLoading = false, error = "Profile not found") }
                    return@addSnapshotListener
                }

                val statsMap = snapshot.get("stats") as? Map<*, *>
                _uiState.update { state ->
                    state.copy(
                        userId = userId,
                        displayName = snapshot.getString("displayName").orEmpty(),
                        handle = snapshot.getString("handle").orEmpty(),
                        bio = snapshot.getString("bio").orEmpty(),
                        email = snapshot.getString("email").orEmpty(),
                        phone = snapshot.getString("phone").orEmpty(),
                        faculty = snapshot.getString("faculty").orEmpty(),
                        year = snapshot.getString("year").orEmpty(),
                        location = snapshot.getString("location").orEmpty(),
                        avatarEmoji = snapshot.getString("avatarEmoji") ?: "👤",
                        profileImageUrl = snapshot.getString("profileImageUrl").orEmpty(),
                        coverImageUrl = snapshot.getString("coverImageUrl").orEmpty(),
                        posts = if (!appliedInitialProfileSnapshot) (statsMap?.get("posts") as? Number)?.toInt() ?: 0 else state.posts,
                        followers = if (!appliedInitialProfileSnapshot) (statsMap?.get("followers") as? Number)?.toInt() ?: 0 else state.followers,
                        following = if (!appliedInitialProfileSnapshot) (statsMap?.get("following") as? Number)?.toInt() ?: 0 else state.following,
                        isLoading = false,
                        error = null,
                    )
                }
                if (!appliedInitialProfileSnapshot) {
                    appliedInitialProfileSnapshot = true
                }
            }

        if (!isOwnProfile && currentUserId != null) {
            observeRelationship(currentUserId = currentUserId, targetUserId = userId)
        }

        followersCountListener = firestore.collection("users")
            .document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep fallback count from stats when listener cannot read.
                    return@addSnapshotListener
                }
                followersRaw.value = snapshot?.size() ?: 0
            }

        followingCountListener = firestore.collection("users")
            .document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep fallback count from stats when listener cannot read.
                    return@addSnapshotListener
                }
                followingRaw.value = snapshot?.size() ?: 0
            }

        postsCountListener = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                postsRaw.value = snapshot?.size() ?: 0
            }

        activityListener = firestore.collection("users").document(userId)
            .collection("activities")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error.message) }
                    return@addSnapshotListener
                }

                val activities = snapshot?.documents?.map { doc ->
                    val createdAt = doc.get("createdAt").toLongOrNullValue() ?: 0L

                    ProfileActivityItem(
                        type = doc.getString("type") ?: "activity",
                        text = doc.getString("text") ?: "Did an activity",
                        createdAt = createdAt,
                        sourceId = doc.id,
                        eventId = doc.get("eventId").toLongOrNullValue()?.toInt(),
                        postId = doc.get("postId").toLongOrNullValue()?.toInt(),
                        previewTitle = doc.getString("previewTitle") ?: "",
                        previewSubtitle = doc.getString("previewSubtitle") ?: "",
                        previewImageUrl = doc.getString("previewImageUrl") ?: "",
                        likeCount = doc.get("likeCount").toLongOrNullValue()?.toInt() ?: 0,
                        commentCount = doc.get("commentCount").toLongOrNullValue()?.toInt() ?: 0,
                        shareCount = doc.get("shareCount").toLongOrNullValue()?.toInt() ?: 0,
                    )
                } ?: emptyList()

                _uiState.update { it.copy(activities = activities) }
            }
    }

    fun likeActivity(activity: ProfileActivityItem) {
        updateActivityCounter(activity, "likeCount", 1L)
        ActivityLogger.logAction(
            type = "like_activity",
            text = "Liked recent activity",
            metadata = mapOf("activityId" to activity.sourceId.orEmpty(), "activityType" to activity.type),
        )
    }

    fun shareActivity(activity: ProfileActivityItem) {
        updateActivityCounter(activity, "shareCount", 1L)
        ActivityLogger.logAction(
            type = "share_activity",
            text = "Shared recent activity",
            metadata = mapOf("activityId" to activity.sourceId.orEmpty(), "activityType" to activity.type),
        )
    }

    private fun updateActivityCounter(activity: ProfileActivityItem, field: String, delta: Long) {
        val activityId = activity.sourceId ?: return
        val previousActivities = _uiState.value.activities

        _uiState.update { state ->
            state.copy(
                activities = state.activities.map { item ->
                    if (item.sourceId != activityId) {
                        item
                    } else {
                        when (field) {
                            "likeCount" -> item.copy(likeCount = (item.likeCount + delta).coerceAtLeast(0).toInt())
                            "commentCount" -> item.copy(commentCount = (item.commentCount + delta).coerceAtLeast(0).toInt())
                            "shareCount" -> item.copy(shareCount = (item.shareCount + delta).coerceAtLeast(0).toInt())
                            else -> item
                        }
                    }
                }
            )
        }

        val ownerId = _uiState.value.userId
        if (ownerId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(ownerId)
                    .collection("activities")
                    .document(activityId)
                    .update(field, FieldValue.increment(delta))
            }.onFailure { error ->
                android.util.Log.e("PublicProfileVM", "Failed to update $field for $activityId: ${error.message}")
                _uiState.update { state ->
                    state.copy(activities = previousActivities)
                }
            }
        }
    }

    private fun observeRelationship(currentUserId: String, targetUserId: String) {
        android.util.Log.d("PublicProfileVM", "Observing relationship: currentUserId=$currentUserId, targetUserId=$targetUserId")

        followingRelationListener = firestore.collection("users")
            .document(currentUserId)
            .collection("following")
            .document(targetUserId)
            .addSnapshotListener { snapshot, error ->
                val isFollowing = snapshot?.exists() == true
                android.util.Log.d("PublicProfileVM", "Following listener: exists=$isFollowing, error=$error")
                _uiState.update { it.copy(isFollowing = isFollowing) }
            }

        outgoingRequestListener = firestore.collection("users")
            .document(currentUserId)
            .collection("outgoingFriendRequests")
            .whereEqualTo("toUserId", targetUserId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                val doc = snapshot?.documents?.firstOrNull()
                android.util.Log.d("PublicProfileVM", "Outgoing listener: hasDoc=${doc != null}, error=$error")
                _uiState.update {
                    it.copy(
                        hasOutgoingRequest = doc != null,
                        outgoingRequestId = doc?.id.orEmpty(),
                    )
                }
            }

        incomingRequestListener = firestore.collection("users")
            .document(currentUserId)
            .collection("friendRequests")
            .whereEqualTo("fromUserId", targetUserId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                val doc = snapshot?.documents?.firstOrNull()
                android.util.Log.d("PublicProfileVM", "Incoming listener: hasDoc=${doc != null}, error=$error")
                _uiState.update {
                    it.copy(
                        hasIncomingRequest = doc != null,
                        incomingRequestId = doc?.id.orEmpty(),
                    )
                }
            }
    }

    fun sendOrCancelRequest() {
        val state = _uiState.value
        val currentUserId = auth.currentUser?.uid ?: return
        if (state.isOwnProfile || state.userId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null) }

            val result = if (state.hasOutgoingRequest && state.outgoingRequestId.isNotBlank()) {
                userRepository.cancelFriendRequest(state.outgoingRequestId)
            } else {
                userRepository.sendFriendRequest(currentUserId, state.userId)
            }

            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }

            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    fun acceptIncomingRequest() {
        val requestId = _uiState.value.incomingRequestId
        if (requestId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null) }
            val result = userRepository.acceptFriendRequest(requestId)
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    fun rejectIncomingRequest() {
        val requestId = _uiState.value.incomingRequestId
        if (requestId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null) }
            val result = userRepository.rejectFriendRequest(requestId)
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        activityListener?.remove()
        followersCountListener?.remove()
        followingCountListener?.remove()
        followingRelationListener?.remove()
        outgoingRequestListener?.remove()
        incomingRequestListener?.remove()
    }
}
