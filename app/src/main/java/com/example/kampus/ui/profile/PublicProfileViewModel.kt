package com.example.kampus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.UserRepositoryImpl
import com.example.kampus.data.repository.PostRepositoryImpl
import com.example.kampus.data.repository.EventRepositoryImpl
import com.example.kampus.data.repository.EventEngagementRepository
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
    private val postRepository = PostRepositoryImpl(firestore)
    private val eventRepository = EventRepositoryImpl()

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
    private val eventEngagementListeners = mutableMapOf<String, ListenerRegistration>()

    // High-fidelity feed post and event activity cache
    private var firestoreActivities = emptyList<ProfileActivityItem>()
    private var eventActivities = emptyList<ProfileActivityItem>()
    private var userTimelinePosts = emptyList<com.example.kampus.ui.feed.PostItem>()

    // Raw count flows used for debounce to avoid UI jitter on rapid updates
    private val followersRaw = MutableStateFlow<Int?>(null)
    private val followingRaw = MutableStateFlow<Int?>(null)
    private val postsRaw = MutableStateFlow<Int?>(null)
    private var countsCollectorJob: Job? = null
    private var appliedInitialProfileSnapshot = false

    private fun normalizeTimestamp(time: Long): Long {
        if (time <= 0L) return 0L
        return if (time < 100_000_000_000L) time * 1000L else time
    }

    private fun parseIntField(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Int? {
        return when (val value = doc.get(field)) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun parseTimestampField(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
        return when (val value = doc.get(field)) {
            is com.google.firebase.Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    fun observeUser(userId: String) {
        userListener?.remove()
        activityListener?.remove()
        postsCountListener?.remove()
        followersCountListener?.remove()
        followingCountListener?.remove()
        followingRelationListener?.remove()
        outgoingRequestListener?.remove()
        incomingRequestListener?.remove()
        eventEngagementListeners.values.forEach { it.remove() }
        eventEngagementListeners.clear()

        appliedInitialProfileSnapshot = false
        firestoreActivities = emptyList()
        eventActivities = emptyList()
        userTimelinePosts = emptyList()

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
                    v?.let {
                        publishRecentActivities()
                    }
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
                    return@addSnapshotListener
                }
                followersRaw.value = snapshot?.size() ?: 0
            }

        followingCountListener = firestore.collection("users")
            .document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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
                publishRecentActivities()
            }

        // Real-time listener for the target user's feed posts
        viewModelScope.launch {
            postRepository.getFeedPosts().collect { result ->
                result.onSuccess { posts ->
                    val filtered = posts.filter { it.authorId == userId }
                    val currentUserId = auth.currentUser?.uid.orEmpty()
                    val engagementSnapshots = com.example.kampus.data.repository.PostEngagementRepository(firestore)
                        .loadSnapshots(filtered.map { it.id }, currentUserId.ifBlank { null })
                        .getOrDefault(emptyMap())

                    userTimelinePosts = filtered.map { post ->
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
                    publishRecentActivities()
                }
            }
        }

        // Real-time event activities listener
        viewModelScope.launch {
            eventRepository.getEvents().collect { result ->
                result.onSuccess { events ->
                    eventActivities = events
                        .filter { it.ownerId == userId }
                        .mapNotNull { event ->
                            val rawCreatedAt = event.createdAt ?: 0L
                            val createdAt = normalizeTimestamp(if (rawCreatedAt < 100_000_000_000L) rawCreatedAt * 1000L else rawCreatedAt)
                            if (createdAt <= 0L) return@mapNotNull null
                            event.id?.let { observeEventEngagement(it) }
                            val rawStart = event.startDate ?: 0L
                            val startMs = if (rawStart < 100_000_000_000L && rawStart > 0L) rawStart * 1000L else rawStart
                            val sdfDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
                            val sdfTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                            val dateStr = if (startMs > 0L) sdfDate.format(java.util.Date(startMs)) else ""
                            val timeStr = if (startMs > 0L) sdfTime.format(java.util.Date(startMs)) else ""
                            ProfileActivityItem(
                                type = "create_event",
                                text = "Created event: ${event.title.ifBlank { "Untitled event" }}",
                                createdAt = createdAt,
                                sourceId = event.id,
                                eventId = event.id?.hashCode() ?: event.title.hashCode(),
                                previewTitle = event.title.ifBlank { "Untitled event" },
                                previewSubtitle = event.location.orEmpty().ifBlank { "Event" },
                                previewImageUrl = event.imageUrl.orEmpty(),
                                eventDate = dateStr,
                                eventTime = timeStr,
                                eventLocation = event.location.orEmpty(),
                                groupId = null,
                            )
                        }
                    publishRecentActivities()
                }
            }
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

                val activities = snapshot?.documents?.mapNotNull { doc ->
                    val createdAt = normalizeTimestamp(parseTimestampField(doc, "createdAt"))
                    val updatedAt = normalizeTimestamp(doc.getLong("updatedAt") ?: createdAt)
                    val eventIdStr = doc.getString("eventId")
                    val eventId = eventIdStr?.toIntOrNull() ?: eventIdStr?.hashCode()
                    val postId = parseIntField(doc, "postId")
                    val type = doc.getString("type") ?: "activity"
                    val hiddenTypes = setOf("delete_post", "edit_post", "edit_privacy", "hide_from_profile", "archive_activity", "activity_notifications", "pin_post", "unpin_post")
                    if (type in hiddenTypes) return@mapNotNull null
                    val author = doc.getString("author").orEmpty()
                    val hiddenFromProfile = doc.getBoolean("hiddenFromProfile") == true
                    val archivedAt = doc.getLong("archivedAt") ?: 0L
                    if (hiddenFromProfile || archivedAt > 0L) return@mapNotNull null
                    val previewImageUrl = doc.getString("previewImageUrl") ?: doc.getString("imageUrl") ?: doc.getString("mediaUrl") ?: ""
                    val rawVisibility = doc.getString("visibility")?.uppercase()
                    val visibility = rawVisibility?.let { runCatching { com.example.kampus.ui.feed.PostItem.PostVisibility.valueOf(it) }.getOrNull() }

                    val previewTitle = when (type) {
                        "share_post" -> doc.getString("previewTitle") ?: if (author.isNotBlank()) "Shared post by $author" else "Shared post"
                        "share_profile" -> doc.getString("previewTitle") ?: "Shared profile"
                        "create_post" -> doc.getString("previewTitle") ?: "Post"
                        "create_event" -> doc.getString("previewTitle") ?: "Event"
                        "create_group" -> doc.getString("previewTitle") ?: "Group"
                        else -> doc.getString("previewTitle") ?: "Activity"
                    }

                    val previewSubtitle = when (type) {
                        "share_post" -> doc.getString("previewSubtitle") ?: if (author.isNotBlank()) "Shared from $author" else "Shared from feed"
                        "share_profile" -> doc.getString("previewSubtitle") ?: "Shared on profile"
                        "create_post" -> doc.getString("previewSubtitle") ?: "Shared from feed"
                        "create_event" -> doc.getString("previewSubtitle") ?: "Shared from events"
                        "create_group" -> doc.getString("previewSubtitle") ?: "Shared from groups"
                        else -> doc.getString("previewSubtitle") ?: "Choose what to do with this activity."
                    }

                    @Suppress("UNCHECKED_CAST")
                    val loveList = doc.get("loveList") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val commentsList = (doc.get("commentsList") as? List<Map<String, Any>> ?: emptyList()).mapNotNull { comment ->
                        try {
                            ActivityComment(
                                id = comment["id"] as? String ?: "",
                                userId = comment["userId"] as? String ?: "",
                                userName = comment["userName"] as? String ?: "",
                                userAvatar = comment["userAvatar"] as? String ?: "",
                                text = comment["text"] as? String ?: "",
                                createdAt = (comment["createdAt"] as? Number)?.toLong() ?: 0L,
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val mediaUrls = (doc.get("mediaUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val mediaTypes = (doc.get("mediaTypes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val groupId = doc.getString("groupId")

                    ProfileActivityItem(
                        type = type,
                        text = when (type) {
                            "share_post" -> doc.getString("text") ?: if (author.isNotBlank()) "Shared post by $author" else "Shared a post"
                            "share_profile" -> doc.getString("text") ?: "Shared profile"
                            "create_post" -> doc.getString("text") ?: "Created a new post"
                            "create_event" -> doc.getString("text") ?: "Created an event"
                            "create_group" -> doc.getString("text") ?: "Created a group"
                            else -> doc.getString("text") ?: "Did an activity"
                        },
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        sourceId = doc.id,
                        eventId = eventId,
                        postId = postId,
                        previewTitle = previewTitle,
                        previewSubtitle = previewSubtitle,
                        previewImageUrl = previewImageUrl,
                        likeCount = ((doc.get("likeCount") as? Number)?.toInt()
                            ?: doc.getString("likeCount")?.toIntOrNull()
                            ?: 0),
                        commentCount = ((doc.get("commentCount") as? Number)?.toInt()
                            ?: doc.getString("commentCount")?.toIntOrNull()
                            ?: 0),
                        shareCount = ((doc.get("shareCount") as? Number)?.toInt()
                            ?: doc.getString("shareCount")?.toIntOrNull()
                            ?: 0),
                        postVisibility = visibility,
                        isPinned = doc.getBoolean("isPinned") == true,
                        isArchived = archivedAt > 0L,
                        currentUserLoved = loveList.contains(currentUserId.orEmpty()),
                        loveList = loveList,
                        commentsList = commentsList,
                        mediaUrls = mediaUrls,
                        mediaTypes = mediaTypes,
                        groupId = groupId,
                    )
                } ?: emptyList()

                firestoreActivities = activities
                publishRecentActivities()
            }
    }

    private fun observeEventEngagement(eventId: String) {
        if (eventEngagementListeners.containsKey(eventId + "_summary")) return
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Listen to event summary
        val summaryListener = firestore.collection("event_engagements").document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val likesCount = (snapshot.getLong("likesCount") ?: 0L).toInt()
                val commentsCount = (snapshot.getLong("commentsCount") ?: 0L).toInt()
                val interestedCount = (snapshot.getLong("interestedCount") ?: 0L).toInt()
                val sharesCount = (snapshot.getLong("sharesCount") ?: 0L).toInt()

                firestore.collection("event_engagements").document(eventId)
                    .collection("members").document(currentUserId).get()
                    .addOnSuccessListener { memberSnapshot ->
                        val isInterested = memberSnapshot.getBoolean("interested") ?: false
                        val isLiked = memberSnapshot.getBoolean("liked") ?: false
                        
                        updateLocalActivityBySourceId(eventId) { current ->
                            current.copy(
                                likeCount = likesCount,
                                shareCount = sharesCount,
                                eventInterestedCount = interestedCount,
                                currentUserLoved = isLiked,
                                loveList = if (isLiked) listOf(currentUserId) else emptyList(),
                                currentUserInterested = isInterested
                            )
                        }
                    }
            }
        
        // 2. Listen to event comments
        val commentsListener = firestore.collection("event_engagements").document(eventId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val comments = snapshot.documents.mapNotNull { doc ->
                    val text = doc.getString("text") ?: ""
                    if (text.isBlank()) return@mapNotNull null
                    ActivityComment(
                        id = doc.id,
                        userId = doc.getString("authorId") ?: "",
                        userName = doc.getString("authorName") ?: "Anonymous",
                        userAvatar = doc.getString("authorEmoji") ?: "👤",
                        text = text,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }
                
                updateLocalActivityBySourceId(eventId) { current ->
                    current.copy(
                        commentsList = comments,
                        commentCount = comments.size
                    )
                }
            }

        eventEngagementListeners[eventId + "_summary"] = summaryListener
        eventEngagementListeners[eventId + "_comments"] = commentsListener
    }

    private fun updateLocalActivityBySourceId(sourceId: String, transform: (ProfileActivityItem) -> ProfileActivityItem) {
        firestoreActivities = firestoreActivities.map { current ->
            if (current.sourceId == sourceId || current.eventId?.toString() == sourceId || current.postId?.toString() == sourceId) transform(current) else current
        }
        eventActivities = eventActivities.map { current ->
            if (current.sourceId == sourceId || current.eventId?.toString() == sourceId || current.postId?.toString() == sourceId) transform(current) else current
        }
        publishRecentActivities()
    }

    private fun updateLocalActivity(activity: ProfileActivityItem, transform: (ProfileActivityItem) -> ProfileActivityItem) {
        firestoreActivities = firestoreActivities.map { current ->
            if (current.sourceId == activity.sourceId) transform(current) else current
        }
        eventActivities = eventActivities.map { current ->
            if (current.sourceId == activity.sourceId) transform(current) else current
        }
        publishRecentActivities()
    }

    private fun publishRecentActivities() {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return

        val allowedFirestoreTypes = setOf("create_event", "share_post", "share_profile", "create_group", "comment", "reply")
        val filteredFirestore = firestoreActivities.filter {
            it.type in allowedFirestoreTypes
        }

        val mappedTimelinePosts = userTimelinePosts.map { post ->
            val existing = firestoreActivities.find { it.postId == post.id && it.type == "create_post" }
            ProfileActivityItem(
                type = "create_post",
                text = post.content,
                createdAt = if (post.timestamp > 0L) post.timestamp else System.currentTimeMillis(),
                updatedAt = if (post.timestamp > 0L) post.timestamp else System.currentTimeMillis(),
                postId = post.id,
                sourceId = existing?.sourceId ?: "post_${post.id}",
                previewTitle = "Post",
                previewSubtitle = "Shared from feed",
                previewImageUrl = post.mediaUris.firstOrNull()?.toString() ?: post.imageUri?.toString() ?: "",
                likeCount = post.likes,
                commentCount = post.comments.coerceAtLeast(existing?.commentsList?.size ?: 0),
                shareCount = post.shares,
                postVisibility = post.visibility,
                isPinned = post.isPinned || (existing?.isPinned == true),
                currentUserLoved = post.likedBy.contains(auth.currentUser?.uid.orEmpty()),
                loveList = post.likedBy,
                commentsList = existing?.commentsList ?: emptyList(),
                mediaUrls = post.mediaUris.map { it.toString() },
                mediaTypes = post.mediaTypes.map { it.name },
                groupId = null,
            )
        }

        val allActivities = filteredFirestore + eventActivities + mappedTimelinePosts

        val uniqueActivities = mutableMapOf<String, ProfileActivityItem>()
        for (activity in allActivities) {
            val key = when {
                activity.eventId != null -> "event|${activity.eventId}"
                activity.postId != null -> "post|${activity.postId}"
                else -> activity.type + "|" + activity.sourceId
            }
            if (key.isNotEmpty()) {
                val existing = uniqueActivities[key]
                if (existing == null) {
                    uniqueActivities[key] = activity
                } else {
                    val isActivityRealTime = activity.type == "create_post" || (activity.type == "create_event" && activity.sourceId == activity.eventId?.toString())
                    val realTime = if (isActivityRealTime) activity else existing
                    val staticLog = if (isActivityRealTime) existing else activity
                    
                    val merged = realTime.copy(
                        sourceId = if (staticLog.sourceId?.startsWith("post_") == false && staticLog.sourceId != staticLog.eventId?.toString()) {
                            staticLog.sourceId
                        } else {
                            realTime.sourceId
                        },
                        isPinned = staticLog.isPinned || realTime.isPinned,
                        commentsList = if (realTime.commentsList.isEmpty() && staticLog.commentsList.isNotEmpty()) staticLog.commentsList else realTime.commentsList,
                        commentCount = if (realTime.commentCount == 0 && staticLog.commentCount > 0) staticLog.commentCount else realTime.commentCount
                    )
                    uniqueActivities[key] = merged
                }
            }
        }

        val combined = uniqueActivities.values
            .sortedWith(
                compareByDescending<ProfileActivityItem> { it.isPinned }
                    .thenByDescending { it.createdAt }
            )
            .take(20)

        val total = (postsRaw.value ?: 0) + eventActivities.size
        _uiState.update { it.copy(activities = combined, posts = total) }
    }

    fun likeActivity(activity: ProfileActivityItem) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val isLoved = activity.currentUserLoved
                val newLoveList = activity.loveList.toMutableList()
                
                if (isLoved) {
                    newLoveList.remove(currentUserId)
                } else {
                    if (!newLoveList.contains(currentUserId)) newLoveList.add(currentUserId)
                }
                
                val payload = mapOf(
                    "loveList" to newLoveList,
                    "likeCount" to newLoveList.size,
                    "updatedAt" to System.currentTimeMillis(),
                )
                
                updateLocalActivity(activity) { current ->
                    current.copy(
                        loveList = newLoveList,
                        likeCount = newLoveList.size,
                        currentUserLoved = !isLoved,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                
                if (activity.type == "create_event") {
                    val eventEngagementRepo = EventEngagementRepository()
                    eventEngagementRepo.toggleFlag(
                        eventId = activity.sourceId ?: "",
                        userId = currentUserId,
                        flagField = "liked",
                        summaryField = "likesCount"
                    )
                } else if (activity.type == "create_post") {
                    val postEngagementRepo = com.example.kampus.data.repository.PostEngagementRepository(firestore)
                    postEngagementRepo.toggleLike(activity.postId.toString(), currentUserId)
                    val postRepo = PostRepositoryImpl(firestore)
                    postRepo.updatePostLikes(activity.postId.toString(), newLoveList.size, currentUserId, !isLoved)
                } else {
                    val ownerId = _uiState.value.userId
                    if (ownerId.isNotBlank() && activity.sourceId != null) {
                        firestore.collection("users")
                            .document(ownerId)
                            .collection("activities")
                            .document(activity.sourceId)
                            .update(payload)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PublicProfileVM", "Failed to toggle love/interest: ${e.message}")
            }
        }
    }

    fun toggleActivityInterest(activity: ProfileActivityItem) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val isInterested = activity.currentUserInterested
                val newInterestedCount = if (isInterested) {
                    (activity.eventInterestedCount - 1).coerceAtLeast(0)
                } else {
                    activity.eventInterestedCount + 1
                }
                
                updateLocalActivity(activity) { current ->
                    current.copy(
                        eventInterestedCount = newInterestedCount,
                        currentUserInterested = !isInterested,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                
                if (activity.type == "create_event") {
                    val eventEngagementRepo = EventEngagementRepository()
                    eventEngagementRepo.toggleFlag(
                        eventId = activity.sourceId ?: "",
                        userId = currentUserId,
                        flagField = "interested",
                        summaryField = "interestedCount"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PublicProfileVM", "Failed to update interest: ${e.message}")
            }
        }
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
        eventEngagementListeners.values.forEach { it.remove() }
        eventEngagementListeners.clear()
    }
}
