@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.ActivityRepositoryImpl
import com.example.kampus.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

/**
 * UI State for activity management
 */
data class ActivityManagementUiState(
    val activities: List<ActivityItem> = emptyList(),
    val pinnedActivities: List<ActivityItem> = emptyList(),
    val selectedActivity: ActivityItem? = null,
    val comments: List<ActivityComment> = emptyList(),
    val reactions: Map<String, Int> = emptyMap(),
    val userReaction: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val syncStatus: ActivityItem.SyncStatus = ActivityItem.SyncStatus.SYNCED,
    val lastSyncTime: Long = 0,
    val isEditing: Boolean = false,
    val editingActivityId: String? = null,
    val typingUsers: List<String> = emptyList(), // For real-time typing indicator
    val offlineQueue: List<ActivityItem> = emptyList(),
)

/**
 * ViewModel for activity management - handles realtime sync and all operations
 */
class ActivityManagementViewModel(
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // State Management
    // ─────────────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(ActivityManagementUiState())
    val uiState: StateFlow<ActivityManagementUiState> = _uiState.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities.asStateFlow()

    private val _pinnedActivities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val pinnedActivities: StateFlow<List<ActivityItem>> = _pinnedActivities.asStateFlow()

    private val _selectedActivity = MutableStateFlow<ActivityItem?>(null)
    val selectedActivity: StateFlow<ActivityItem?> = _selectedActivity.asStateFlow()

    private val _comments = MutableStateFlow<List<ActivityComment>>(emptyList())
    val comments: StateFlow<List<ActivityComment>> = _comments.asStateFlow()

    private val _reactions = MutableStateFlow<Map<String, Int>>(emptyMap())
    val reactions: StateFlow<Map<String, Int>> = _reactions.asStateFlow()

    private val repository = ActivityRepositoryImpl(FirebaseFirestore.getInstance())

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    init {
        loadActivities()
        loadPinnedActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            repository.getUserActivities(userId).collect { result ->
                result.onSuccess { activities ->
                    _activities.update { activities }
                    _uiState.update {
                        it.copy(
                            activities = activities,
                            isLoading = false,
                            syncStatus = ActivityItem.SyncStatus.SYNCED,
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }
                }
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                            syncStatus = ActivityItem.SyncStatus.FAILED
                        )
                    }
                    ActivityLogger.logAction(
                        type = "load_activities_failed",
                        text = "Failed to load activities: ${error.message}",
                        metadata = mapOf("userId" to userId, "error" to (error.message ?: "unknown"))
                    )
                }
            }
        }
    }

    private fun loadPinnedActivities() {
        viewModelScope.launch {
            repository.getPinnedActivities(userId).collect { result ->
                result.onSuccess { pinnedActivities ->
                    _pinnedActivities.update { pinnedActivities }
                    _uiState.update { it.copy(pinnedActivities = pinnedActivities) }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity Selection & Details
    // ─────────────────────────────────────────────────────────────────────────

    fun selectActivity(activityId: String) {
        viewModelScope.launch {
            repository.getActivityById(activityId).collect { result ->
                result.onSuccess { activity ->
                    _selectedActivity.update { activity }
                    _uiState.update { it.copy(selectedActivity = activity) }
                    if (activity != null) {
                        loadActivityComments(activityId)
                        loadActivityReactions(activityId)
                    }
                }
            }
        }
    }

    fun clearSelection() {
        _selectedActivity.update { null }
        _comments.update { emptyList() }
        _reactions.update { emptyMap() }
        _uiState.update {
            it.copy(
                selectedActivity = null,
                comments = emptyList(),
                reactions = emptyMap()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Activity
    // ─────────────────────────────────────────────────────────────────────────

    fun createActivity(activity: ActivityItem) {
        _uiState.update { it.copy(isLoading = true, syncStatus = ActivityItem.SyncStatus.SYNCING) }

        viewModelScope.launch {
            val result = repository.createActivity(activity)
            result.onSuccess { activityId ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        syncStatus = ActivityItem.SyncStatus.SYNCED
                    )
                }
                ActivityLogger.logAction(
                    type = "activity_created",
                    text = "Activity created: ${activity.title}",
                    metadata = mapOf(
                        "activityId" to activityId,
                        "type" to activity.activityType.name,
                        "privacy" to activity.privacy.name
                    )
                )
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                        syncStatus = ActivityItem.SyncStatus.FAILED
                    )
                }
                ActivityLogger.logAction(
                    type = "activity_creation_failed",
                    text = "Failed to create activity: ${error.message}",
                    metadata = mapOf("error" to (error.message ?: "unknown"))
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pin/Unpin Activity - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    fun togglePin(activityId: String) {
        val activity = _activities.value.firstOrNull { it.id == activityId }
        val isCurrentlyPinned = activity?.isPinned ?: false
        val newPinState = !isCurrentlyPinned

        // Update local state immediately for UI responsiveness
        _activities.update { activities ->
            activities.map { activity ->
                if (activity.id == activityId) {
                    activity.copy(isPinned = newPinState, updatedAt = System.currentTimeMillis())
                } else activity
            }
        }

        if (newPinState) {
            _pinnedActivities.update { it + (activity?.copy(isPinned = true) ?: return) }
        } else {
            _pinnedActivities.update { it.filterNot { pinnedActivity -> pinnedActivity.id == activityId } }
        }

        ActivityLogger.logAction(
            type = if (newPinState) "activity_pinned" else "activity_unpinned",
            text = "Activity ${if (newPinState) "pinned" else "unpinned"}",
            metadata = mapOf("activityId" to activityId)
        )

        // Persist to Firestore asynchronously
        viewModelScope.launch {
            val result = repository.pinActivity(activityId, newPinState)
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "pin_update_failed",
                    text = "Failed to update pin status: ${error.message}",
                    metadata = mapOf("activityId" to activityId, "error" to (error.message ?: "unknown"))
                )
                // Revert on failure
                _activities.update { activities ->
                    activities.map { activity ->
                        if (activity.id == activityId) {
                            activity.copy(isPinned = isCurrentlyPinned)
                        } else activity
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit Activity - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    fun startEditing(activityId: String) {
        _uiState.update {
            it.copy(isEditing = true, editingActivityId = activityId)
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(isEditing = false, editingActivityId = null)
        }
    }

    fun editActivity(
        activityId: String,
        newTitle: String,
        newDescription: String,
        newTags: List<String> = emptyList(),
        newLocation: String? = null
    ) {
        _uiState.update { it.copy(syncStatus = ActivityItem.SyncStatus.SYNCING) }

        viewModelScope.launch {
            val result = repository.editActivity(
                activityId,
                newTitle,
                newDescription,
                newTags,
                newLocation
            )

            result.onSuccess {
                // Update local state
                _activities.update { activities ->
                    activities.map { activity ->
                        if (activity.id == activityId) {
                            activity.copy(
                                title = newTitle,
                                description = newDescription,
                                tags = newTags,
                                location = newLocation,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else activity
                    }
                }

                _uiState.update {
                    it.copy(
                        isEditing = false,
                        editingActivityId = null,
                        syncStatus = ActivityItem.SyncStatus.SYNCED
                    )
                }

                ActivityLogger.logAction(
                    type = "activity_edited",
                    text = "Activity updated: $newTitle",
                    metadata = mapOf("activityId" to activityId)
                )
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message,
                        syncStatus = ActivityItem.SyncStatus.FAILED
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privacy - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    fun updatePrivacy(activityId: String, privacy: ActivityItem.ActivityPrivacy) {
        // Update locally immediately
        _activities.update { activities ->
            activities.map { activity ->
                if (activity.id == activityId) {
                    activity.copy(privacy = privacy, updatedAt = System.currentTimeMillis())
                } else activity
            }
        }

        ActivityLogger.logAction(
            type = "privacy_changed",
            text = "Activity privacy changed to ${privacy.name}",
            metadata = mapOf("activityId" to activityId, "privacy" to privacy.name)
        )

        // Persist asynchronously
        viewModelScope.launch {
            val result = repository.updatePrivacy(activityId, privacy)
            result.onFailure { error ->
                ActivityLogger.logAction(
                    type = "privacy_update_failed",
                    text = "Failed to update privacy: ${error.message}",
                    metadata = mapOf("error" to (error.message ?: "unknown"))
                )
            }
        }
    }

    fun hideFromProfile(activityId: String, isHidden: Boolean = true) {
        _activities.update { activities ->
            activities.map { activity ->
                if (activity.id == activityId) {
                    activity.copy(hiddenFromProfile = isHidden)
                } else activity
            }
        }

        viewModelScope.launch {
            repository.hideFromProfile(activityId, isHidden)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Archive - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    fun archiveActivity(activityId: String) {
        _activities.update { it.filterNot { activity -> activity.id == activityId } }

        ActivityLogger.logAction(
            type = "activity_archived",
            text = "Activity archived",
            metadata = mapOf("activityId" to activityId)
        )

        viewModelScope.launch {
            repository.archiveActivity(activityId)
        }
    }

    fun restoreActivity(activityId: String) {
        viewModelScope.launch {
            val result = repository.restoreActivity(activityId)
            result.onSuccess {
                loadActivities() // Refresh
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete Activity - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteActivity(activityId: String) {
        // Remove from UI immediately
        val deletedActivity = _activities.value.firstOrNull { it.id == activityId }
        _activities.update { it.filterNot { activity -> activity.id == activityId } }

        ActivityLogger.logAction(
            type = "activity_deleted",
            text = "Activity deleted",
            metadata = mapOf("activityId" to activityId)
        )

        viewModelScope.launch {
            val result = repository.deleteActivity(activityId)
            result.onFailure { error ->
                // Restore on failure
                if (deletedActivity != null) {
                    _activities.update { it + deletedActivity }
                }
                ActivityLogger.logAction(
                    type = "delete_failed",
                    text = "Failed to delete activity: ${error.message}",
                    metadata = mapOf("error" to (error.message ?: "unknown"))
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reactions & Likes - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleLike(activityId: String) {
        val currentReaction = _uiState.value.userReaction
        val hasLiked = currentReaction == "like"

        // Update counters optimistically
        _activities.update { activities ->
            activities.map { activity ->
                if (activity.id == activityId) {
                    activity.copy(
                        likes = if (hasLiked) activity.likes - 1 else activity.likes + 1,
                        isLikedByUser = !hasLiked
                    )
                } else activity
            }
        }

        viewModelScope.launch {
            val result = if (hasLiked) {
                repository.removeReaction(activityId, userId, "like")
            } else {
                repository.addReaction(activityId, userId, "like")
            }

            result.onSuccess {
                _uiState.update {
                    it.copy(userReaction = if (hasLiked) null else "like")
                }
                ActivityLogger.logAction(
                    type = if (hasLiked) "activity_unliked" else "activity_liked",
                    text = "Activity ${if (hasLiked) "unliked" else "liked"}",
                    metadata = mapOf("activityId" to activityId)
                )
            }
            result.onFailure { error ->
                // Revert on failure
                _activities.update { activities ->
                    activities.map { activity ->
                        if (activity.id == activityId) {
                            activity.copy(
                                likes = if (hasLiked) activity.likes + 1 else activity.likes - 1,
                                isLikedByUser = hasLiked
                            )
                        } else activity
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comments - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadActivityComments(activityId: String) {
        viewModelScope.launch {
            repository.getActivityComments(activityId).collect { result ->
                result.onSuccess { comments ->
                    _comments.update { comments }
                    _uiState.update { it.copy(comments = comments) }
                }
            }
        }
    }

    fun addComment(activityId: String, text: String, userId: String, username: String, userAvatar: String) {
        val comment = ActivityComment(
            activityId = activityId,
            userId = userId,
            username = username,
            userAvatar = userAvatar,
            text = text,
            createdAt = System.currentTimeMillis()
        )

        // Add to UI immediately
        _comments.update { it + comment }

        viewModelScope.launch {
            val result = repository.addComment(activityId, comment)
            result.onSuccess { commentId ->
                ActivityLogger.logAction(
                    type = "comment_added",
                    text = "Comment added",
                    metadata = mapOf("activityId" to activityId, "commentId" to commentId)
                )
            }
            result.onFailure { error ->
                _comments.update { it.filterNot { c -> c.id == comment.id } }
            }
        }
    }

    fun deleteComment(activityId: String, commentId: String) {
        _comments.update { it.filterNot { it.id == commentId } }

        viewModelScope.launch {
            repository.deleteComment(activityId, commentId)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reactions Loading - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadActivityReactions(activityId: String) {
        viewModelScope.launch {
            repository.getActivityReactions(activityId).collect { result ->
                result.onSuccess { reactions ->
                    _reactions.update { reactions }
                    _uiState.update { it.copy(reactions = reactions) }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Share & Analytics
    // ─────────────────────────────────────────────────────────────────────────

    fun shareActivity(activityId: String) {
        viewModelScope.launch {
            repository.incrementShareCount(activityId)
            ActivityLogger.logAction(
                type = "activity_shared",
                text = "Activity shared",
                metadata = mapOf("activityId" to activityId)
            )
        }
    }

    fun trackView(activityId: String) {
        viewModelScope.launch {
            repository.incrementViewCount(activityId)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Offline Support
    // ─────────────────────────────────────────────────────────────────────────

    fun syncOfflineChanges() {
        _uiState.update { it.copy(syncStatus = ActivityItem.SyncStatus.SYNCING) }

        viewModelScope.launch {
            val offlineQueue = _uiState.value.offlineQueue
            var successCount = 0

            for (activity in offlineQueue) {
                val result = repository.createActivity(activity)
                if (result.isSuccess) {
                    successCount++
                }
            }

            _uiState.update {
                it.copy(
                    offlineQueue = emptyList(),
                    syncStatus = ActivityItem.SyncStatus.SYNCED,
                    lastSyncTime = System.currentTimeMillis()
                )
            }

            ActivityLogger.logAction(
                type = "offline_sync_complete",
                text = "Synced $successCount offline activities",
                metadata = mapOf("count" to successCount.toString())
            )
        }
    }

    fun retryFailedSync(activityId: String) {
        viewModelScope.launch {
            val activity = _activities.value.firstOrNull { it.id == activityId } ?: return@launch
            val result = repository.createActivity(activity)
            result.onSuccess {
                loadActivities()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh & Reload
    // ─────────────────────────────────────────────────────────────────────────

    fun refresh() {
        loadActivities()
        loadPinnedActivities()
    }
}
