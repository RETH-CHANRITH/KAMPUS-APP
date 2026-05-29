package com.example.kampus.ui.events

import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.di.SupabaseModule
import com.example.kampus.data.repository.EventRepositoryImpl
import com.example.kampus.data.repository.EventEngagementRepository
import com.example.kampus.domain.model.Event
import com.example.kampus.domain.repository.IEventRepository
import com.example.kampus.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────────────────────────────────────
data class EventUiState(
    val events          : List<EventItem> = emptyList(),
    val interestedIds   : Set<Int>        = emptySet(),
    val likedIds        : Set<Int>        = emptySet(),
    val savedIds        : Set<Int>        = emptySet(),
    val activeFilter    : String          = "All",
    val searchQuery     : String          = "",
    val isLoading       : Boolean         = false,
    val isCreating      : Boolean         = false,
    val errorMessage    : String?         = null,
    val successMessage  : String?         = null,
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────
class EventViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    val filters = listOf("All", "Today", "This Week", "Music", "Tech", "Art", "Campus")

    private val eventRepository: IEventRepository = EventRepositoryImpl()
    private val engagementRepository = EventEngagementRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val summaryJobs = mutableMapOf<String, Job>()
    private val memberJobs = mutableMapOf<String, Job>()
    private var currentUserDisplayName: String = "You"
    private var currentUserEmoji: String = "🙂"
    private var currentUserProfileImageUrl: String = ""
    private val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    init {
        refreshCurrentUserProfile()
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            eventRepository.getEvents().collect { result ->
                result.onSuccess { events ->
                    val eventItems = events.map { event -> event.toEventItem() }
                    _uiState.update {
                        it.copy(
                            events = eventItems,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                    ensureEngagementObservers(eventItems)
                }
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load events",
                        )
                    }
                }
            }
        }
    }

    private fun Event.toEventItem(): EventItem {
        // Convert Unix timestamps to readable dates
        val primaryTimestamp = startDate ?: createdAt
        val dateLabel = primaryTimestamp?.let { timestamp ->
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(timestamp * 1000))
        } ?: "TBA"
        
        val timeLabel = when {
            startDate != null && endDate != null -> {
                val startText = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date(startDate * 1000))
                val endText = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date(endDate * 1000))
                "$startText – $endText"
            }
            primaryTimestamp != null -> java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date(primaryTimestamp * 1000))
            else -> ""
        }
        
        val organizerTime = createdAt?.let { timestamp ->
            val seconds = System.currentTimeMillis() / 1000 - timestamp
            when {
                seconds < 60 -> "Just now"
                seconds < 3600 -> "${seconds / 60}m ago"
                seconds < 86400 -> "${seconds / 3600}h ago"
                else -> "${seconds / 86400}d ago"
            }
        } ?: "Just now"

        return EventItem(
            id = id?.hashCode() ?: title.hashCode(),
            title = title,
            category = EventCategory.CAMPUS,
            date = dateLabel,
            time = timeLabel,
            location = location.orEmpty(),
            interested = 0,
            likes = 0,
            comments = 0,
            shares = 0,
            remoteId = id.orEmpty(),
            coverEmoji = if (imageUrl.isNullOrBlank()) "🎉" else "🖼️",
            coverImageUrl = imageUrl.orEmpty(),
            coverColor1 = Color(0xFF1A0A2E),
            coverColor2 = Color(0xFF4A1060),
            description = description.orEmpty(),
            organizer = ownerId.ifBlank { "Anonymous" },  // Will be replaced with actual name in UI
            organizerEmoji = "🙋",
            organizerTime = organizerTime,
            // Extended fields from Supabase
            eventType = eventType.orEmpty(),
            capacity = capacity ?: 0,
            registrationDeadline = registrationDeadline.orEmpty(),
            website = website.orEmpty(),
            onlineEvent = onlineEvent,
            certificateAvailable = certificateAvailable,
            paidEvent = paidEvent,
            allowGuest = allowGuest,
            tags = tags.orEmpty(),
            speaker = speaker.orEmpty(),
        )
    }

    private fun refreshCurrentUserProfile() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                currentUserDisplayName = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                    ?: firebaseAuth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: "You"
                currentUserEmoji = doc.getString("avatarEmoji")?.takeIf { it.isNotBlank() } ?: "🙂"
                currentUserProfileImageUrl = doc.getString("profileImageUrl")?.takeIf { it.isNotBlank() }
                    ?: firebaseAuth.currentUser?.photoUrl?.toString().orEmpty()
            } catch (_: Exception) {
                currentUserDisplayName = firebaseAuth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "You"
                currentUserEmoji = "🙂"
                currentUserProfileImageUrl = firebaseAuth.currentUser?.photoUrl?.toString().orEmpty()
            }
        }
    }

    private fun ensureEngagementObservers(events: List<EventItem>) {
        events.forEach { event ->
            observeEventSummary(event)
            observeCurrentUserState(event)
        }
    }

    private fun observeEventSummary(event: EventItem) {
        val remoteId = event.remoteId
        if (remoteId.isBlank() || summaryJobs.containsKey(remoteId)) return

        summaryJobs[remoteId] = viewModelScope.launch {
            engagementRepository.observeSummary(remoteId).collect { result ->
                result.onSuccess { summary ->
                    _uiState.update { state ->
                        state.copy(
                            events = state.events.map { item ->
                                if (item.remoteId == remoteId) {
                                    item.copy(
                                        likes = summary.likesCount,
                                        comments = summary.commentsCount,
                                        interested = summary.interestedCount,
                                        shares = summary.sharesCount,
                                    )
                                } else {
                                    item
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun observeCurrentUserState(event: EventItem) {
        val remoteId = event.remoteId
        val userId = currentUserId ?: return
        val jobKey = "$remoteId:$userId"
        if (remoteId.isBlank() || memberJobs.containsKey(jobKey)) return

        memberJobs[jobKey] = viewModelScope.launch {
            engagementRepository.observeMemberState(remoteId, userId).collect { result ->
                result.onSuccess { member ->
                    val localId = _uiState.value.events.firstOrNull { it.remoteId == remoteId }?.id
                        ?: return@onSuccess

                    _uiState.update { state ->
                        state.copy(
                            likedIds = if (member.liked) state.likedIds + localId else state.likedIds - localId,
                            savedIds = if (member.saved) state.savedIds + localId else state.savedIds - localId,
                            interestedIds = if (member.interested) state.interestedIds + localId else state.interestedIds - localId,
                        )
                    }
                }
            }
        }
    }

    private fun bumpEventCount(remoteId: String, likesDelta: Int = 0, commentsDelta: Int = 0, interestedDelta: Int = 0) {
        _uiState.update { state ->
            state.copy(
                events = state.events.map { item ->
                    if (item.remoteId != remoteId) {
                        item
                    } else {
                        item.copy(
                            likes = (item.likes + likesDelta).coerceAtLeast(0),
                            comments = (item.comments + commentsDelta).coerceAtLeast(0),
                            interested = (item.interested + interestedDelta).coerceAtLeast(0),
                        )
                    }
                },
            )
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun toggleInterested(event: EventItem) {
        val userId = currentUserId ?: return
        val wasInterested = event.id in _uiState.value.interestedIds

        _uiState.update { state ->
            state.copy(
                interestedIds = if (event.id in state.interestedIds) state.interestedIds - event.id else state.interestedIds + event.id,
            )
        }
        bumpEventCount(event.remoteId, interestedDelta = if (wasInterested) -1 else 1)

        viewModelScope.launch {
            val result = engagementRepository.toggleFlag(
                eventId = event.remoteId,
                userId = userId,
                flagField = "interested",
                summaryField = "interestedCount",
            )
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        interestedIds = if (wasInterested) state.interestedIds + event.id else state.interestedIds - event.id,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update interest",
                    )
                }
                bumpEventCount(event.remoteId, interestedDelta = if (wasInterested) 1 else -1)
                return@launch
            }

            if (!wasInterested) {
                ActivityLogger.logAction(
                    type = "interested_event",
                    text = "Interested in an event",
                    metadata = mapOf("eventId" to event.remoteId),
                )
            }
        }
    }

    fun toggleLike(event: EventItem) {
        val userId = currentUserId ?: return
        val wasLiked = event.id in _uiState.value.likedIds

        _uiState.update { state ->
            state.copy(
                likedIds = if (event.id in state.likedIds) state.likedIds - event.id else state.likedIds + event.id,
            )
        }
        bumpEventCount(event.remoteId, likesDelta = if (wasLiked) -1 else 1)

        viewModelScope.launch {
            val result = engagementRepository.toggleFlag(
                eventId = event.remoteId,
                userId = userId,
                flagField = "liked",
                summaryField = "likesCount",
            )
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        likedIds = if (wasLiked) state.likedIds + event.id else state.likedIds - event.id,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update like",
                    )
                }
                bumpEventCount(event.remoteId, likesDelta = if (wasLiked) 1 else -1)
                return@launch
            }

            if (!wasLiked) {
                ActivityLogger.logAction(
                    type = "like_event",
                    text = "Liked event: ${event.title.ifBlank { "Untitled event" }}",
                    metadata = mapOf(
                        "eventId" to event.remoteId,
                        "previewTitle" to (event.title.ifBlank { "Event" }),
                        "previewSubtitle" to (event.location.ifBlank { "Event" }),
                        "previewImageUrl" to (event.coverImageUrl.ifBlank { "" }),
                        "likeCount" to (event.likes + 1).toString(),
                    ),
                )
            }
        }
    }

    fun toggleSave(event: EventItem) {
        val userId = currentUserId ?: return
        val wasSaved = event.id in _uiState.value.savedIds

        _uiState.update { state ->
            state.copy(
                savedIds = if (event.id in state.savedIds) state.savedIds - event.id else state.savedIds + event.id,
            )
        }

        viewModelScope.launch {
            val result = engagementRepository.toggleFlag(
                eventId = event.remoteId,
                userId = userId,
                flagField = "saved",
                summaryField = "savesCount",
            )
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        savedIds = if (wasSaved) state.savedIds + event.id else state.savedIds - event.id,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update save",
                    )
                }
            }
        }
    }

    fun shareEvent(event: EventItem) {
        _uiState.update { state ->
            state.copy(
                events = state.events.map { item ->
                    if (item.remoteId == event.remoteId) item.copy(shares = (item.shares + 1).coerceAtLeast(0)) else item
                },
            )
        }

        ActivityLogger.logAction(
            type = "share_event",
            text = "Shared event: ${event.title.ifBlank { "Untitled event" }}",
            metadata = mapOf("eventId" to event.remoteId),
        )

        viewModelScope.launch {
            val result = engagementRepository.incrementShare(event.remoteId)
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        events = state.events.map { item ->
                            if (item.remoteId == event.remoteId) item.copy(shares = (item.shares - 1).coerceAtLeast(0)) else item
                        },
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to update share",
                    )
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    suspend fun createEvent(data: NewEventData): Result<String> {
        if (_uiState.value.isCreating) {
            return Result.failure(IllegalStateException("Event creation already in progress"))
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        _uiState.update { it.copy(isCreating = true, errorMessage = null, successMessage = null) }

        try {
            var uploadedImageUrl: String? = null

            if (data.coverImageUri != null) {
                eventRepository.uploadEventImage(currentUserId, data.coverImageUri)
                    .onSuccess { url ->
                        Log.d("EventViewModel", "Event image uploaded successfully: $url")
                        uploadedImageUrl = url
                    }
                    .onFailure { error ->
                        Log.e("EventViewModel", "Failed to upload event image: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isCreating = false,
                                errorMessage = error.message ?: "Event image upload failed",
                            )
                        }
                        return Result.failure(error)
                    }
            }

            val eventPayload = Event(
                title = data.title.trim(),
                description = data.description.trim().ifBlank { null },
                location = data.location.trim().ifBlank { null },
                imageUrl = uploadedImageUrl,
                ownerId = currentUserId,
                allowGuest = data.allowGuest,
                startDate = null,
                endDate = null,
            )

            Log.d("EventViewModel", "Creating event payload: ${eventPayload.title}")
            val createdEventResult = eventRepository.createEvent(eventPayload)
            val result = createdEventResult.map { createdEvent -> createdEvent.id ?: "" }

            createdEventResult.onSuccess { createdEvent ->
                Log.d("EventViewModel", "createEvent succeeded, id=${createdEvent.id}")
                val createdItem = createdEvent.toEventItem()
                _uiState.update {
                    it.copy(
                        events = listOf(createdItem) + it.events.filterNot { item -> item.remoteId == createdItem.remoteId },
                        isCreating = false,
                        successMessage = "Event created successfully",
                        errorMessage = null,
                    )
                }
                ensureEngagementObservers(listOf(createdItem))
                ActivityLogger.logAction(
                    type = "create_event",
                    text = "Created event: ${data.title.ifBlank { "Untitled" }}",
                    metadata = mapOf("eventId" to (createdEvent.id ?: "")),
                )
            }
            createdEventResult.onFailure { error ->
                Log.e("EventViewModel", "createEvent failed: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        errorMessage = error.message ?: "Failed to create event",
                    )
                }
            }

            return result
        } catch (e: Exception) {
            Log.e("EventViewModel", "Unexpected error while creating event: ${e.message}", e)
            _uiState.update {
                it.copy(
                    isCreating = false,
                    errorMessage = e.message ?: "Unexpected error while creating event",
                )
            }
            return Result.failure(e)
        }
    }

    suspend fun addEvent(data: NewEventData): Result<String> = createEvent(data)

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val result = eventRepository.deleteEvent(eventId)
        result.onSuccess {
            _uiState.update { state ->
                state.copy(events = state.events.filterNot { event -> event.id.toString() == eventId })
            }
        }
        result.onFailure { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "Failed to delete event") }
        }
        return result
    }

    fun observeComments(eventRemoteId: String) = engagementRepository.observeComments(eventRemoteId, currentUserId)

    suspend fun addComment(event: EventItem, comment: String, imageUri: Uri? = null): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not authenticated"))
        val imageUrl = imageUri?.let { selectedImage ->
            val uploadResult = withContext(Dispatchers.IO) {
                SupabaseModule.getStorageManager().uploadEventCommentImage(
                    userId = userId,
                    eventId = event.remoteId,
                    imageUri = selectedImage,
                )
            }

            uploadResult.getOrElse { error ->
                return Result.failure(error)
            }
        }

        val result = engagementRepository.addComment(
            eventId = event.remoteId,
            userId = userId,
            authorName = currentUserDisplayName,
            authorEmoji = currentUserEmoji,
            authorProfileImageUrl = currentUserProfileImageUrl,
            text = comment,
            imageUrl = imageUrl,
            eventOwnerId = event.ownerId,
        )

        result.onSuccess {
            bumpEventCount(event.remoteId, commentsDelta = 1)
            ActivityLogger.logAction(
                type = "comment_event",
                text = "Commented on event: ${event.title.ifBlank { "Untitled event" }}",
                metadata = mapOf("eventId" to event.remoteId),
            )
        }

        return result
    }

    suspend fun addReply(event: EventItem, parentCommentId: String, reply: String, imageUri: Uri? = null): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not authenticated"))
        val imageUrl = imageUri?.let { selectedImage ->
            val uploadResult = withContext(Dispatchers.IO) {
                SupabaseModule.getStorageManager().uploadEventCommentImage(
                    userId = userId,
                    eventId = event.remoteId,
                    imageUri = selectedImage,
                )
            }

            uploadResult.getOrElse { error ->
                return Result.failure(error)
            }
        }

        val result = engagementRepository.addComment(
            eventId = event.remoteId,
            userId = userId,
            authorName = currentUserDisplayName,
            authorEmoji = currentUserEmoji,
            authorProfileImageUrl = currentUserProfileImageUrl,
            text = reply,
            imageUrl = imageUrl,
            parentCommentId = parentCommentId,
            eventOwnerId = event.ownerId,
        )

        result.onSuccess {
            bumpEventCount(event.remoteId, commentsDelta = 1)
            ActivityLogger.logAction(
                type = "reply_event_comment",
                text = "Replied on event: ${event.title.ifBlank { "Untitled event" }}",
                metadata = mapOf("eventId" to event.remoteId),
            )
        }

        return result
    }

    suspend fun toggleCommentLike(event: EventItem, commentId: String): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not authenticated"))
        val result = engagementRepository.toggleCommentLike(
            eventId = event.remoteId,
            commentId = commentId,
            userId = userId,
        )

        result.onSuccess { liked ->
            if (liked) {
                ActivityLogger.logAction(
                    type = "like_event_comment",
                    text = "Liked a comment on event: ${event.title.ifBlank { "Untitled event" }}",
                    metadata = mapOf("eventId" to event.remoteId, "commentId" to commentId),
                )
            }
        }

        return result
    }

    suspend fun deleteComment(event: EventItem, commentId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("User not authenticated"))
        val result = engagementRepository.deleteComment(
            eventId = event.remoteId,
            commentId = commentId,
            userId = userId,
        )

        result.onSuccess {
            bumpEventCount(event.remoteId, commentsDelta = -1)
            ActivityLogger.logAction(
                type = "delete_event_comment",
                text = "Deleted a comment on event: ${event.title.ifBlank { "Untitled event" }}",
                metadata = mapOf("eventId" to event.remoteId, "commentId" to commentId),
            )
        }

        return result
    }

    // ── Computed ──────────────────────────────────────────────────────────────
    fun filteredEvents(filter: String, query: String): List<EventItem> {
        val base = _uiState.value.events
        val byFilter = when (filter) {
            "Music"     -> base.filter { it.category == EventCategory.MUSIC }
            "Tech"      -> base.filter { it.category == EventCategory.TECH }
            "Art"       -> base.filter { it.category == EventCategory.ART }
            "Campus"    -> base.filter { it.category == EventCategory.CAMPUS }
            "Today"     -> base.filter { it.date.contains("March 25") }
            "This Week" -> base.filter { it.date.contains("March") }
            else        -> base
        }
        return if (query.isBlank()) byFilter
        else byFilter.filter {
            it.title.contains(query, true) ||
                    it.location.contains(query, true) ||
                    it.category.label.contains(query, true)
        }
    }
}