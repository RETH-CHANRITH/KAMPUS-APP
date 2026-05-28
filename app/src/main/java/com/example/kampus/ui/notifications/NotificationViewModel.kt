package com.example.kampus.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.domain.model.AppNotification
import com.example.kampus.utils.NotificationLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ActorProfile(
	val userId: String = "",
	val displayName: String = "",
	val profileImageUrl: String = "",
	val avatarEmoji: String = "👤",
)

data class GroupedNotification(
	val id: String,
	val type: String,
	val targetId: String,
	val latestCreatedAt: Long,
	val isRead: Boolean,
	val actors: List<ActorProfile>,
	val actorUserIds: List<String>,
	val count: Int,
	val title: String,
	val body: String,
	val postImageUrl: String = "",
	val rawNotifications: List<AppNotification>,
)

data class NotificationUiState(
	val groupedNotifications: List<GroupedNotification> = emptyList(),
	val isLoading: Boolean = true,
	val error: String? = null,
)

class NotificationViewModel : ViewModel() {

	private val _uiState = MutableStateFlow(NotificationUiState())
	val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

	private var listener: ListenerRegistration? = null
	private var resolveJob: kotlinx.coroutines.Job? = null
	private val actorProfiles = mutableMapOf<String, ActorProfile>()
	private val postThumbnails = mutableMapOf<String, String>() // postId -> imageUrl

	init {
		observeNotifications()
	}

	private fun observeNotifications() {
		val userId = FirebaseAuth.getInstance().currentUser?.uid
		if (userId.isNullOrBlank()) {
			_uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
			return
		}

		listener?.remove()
		listener = NotificationLogger.observeUserNotifications(userId) { result ->
			result.onSuccess { rows ->
				val mapped = rows.map {
					AppNotification(
						id = it["id"] as? String ?: "",
						type = it["type"] as? String ?: "system",
						title = it["title"] as? String ?: "Notification",
						body = it["body"] as? String ?: "",
						toUserId = it["toUserId"] as? String ?: "",
						actorUserId = it["actorUserId"] as? String ?: "",
						actorDisplayName = it["actorDisplayName"] as? String ?: "",
						targetId = it["targetId"] as? String ?: "",
						createdAt = it["createdAt"] as? Long ?: 0L,
						isRead = it["isRead"] as? Boolean ?: false,
					)
				}
				resolveAndPublish(mapped)
			}
			result.onFailure { error ->
				_uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load notifications") }
			}
		}
	}

	private fun resolveAndPublish(rawList: List<AppNotification>) {
		resolveJob?.cancel()
		resolveJob = viewModelScope.launch {
			// Immediately publish grouped list with current cache/defaults
			publishGrouped(rawList)

			// Extract missing actors
			val missingActorIds = rawList.map { it.actorUserId }
				.filter { it.isNotBlank() && !actorProfiles.containsKey(it) }
				.distinct()

			// Extract missing post IDs
			val postTypes = listOf("like", "comment", "love", "reaction")
			val missingPostIds = rawList.filter { it.type in postTypes }
				.map { it.targetId }
				.filter { it.isNotBlank() && !postThumbnails.containsKey(it) }
				.distinct()

			if (missingActorIds.isNotEmpty() || missingPostIds.isNotEmpty()) {
				val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

				// Fetch actors
				if (missingActorIds.isNotEmpty()) {
					val chunks = missingActorIds.chunked(30)
					for (chunk in chunks) {
						runCatching {
							val snapshot = db.collection("users")
								.whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
								.get()
								.await()
							for (doc in snapshot.documents) {
								actorProfiles[doc.id] = ActorProfile(
									userId = doc.id,
									displayName = doc.getString("displayName") ?: doc.getString("name") ?: "Someone",
									profileImageUrl = doc.getString("profileImageUrl") ?: "",
									avatarEmoji = doc.getString("avatarEmoji") ?: "👤"
								)
							}
						}
					}
				}

				// Fetch posts
				if (missingPostIds.isNotEmpty()) {
					val chunks = missingPostIds.chunked(30)
					for (chunk in chunks) {
						runCatching {
							val snapshot = db.collection("posts")
								.whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
								.get()
								.await()
							for (doc in snapshot.documents) {
								val mediaUrls = (doc.get("mediaUrls") as? List<*>)?.mapNotNull { it as? String }
								val legacyUrl = doc.getString("imageUrl") ?: doc.getString("imageUri") ?: ""
								val firstUrl = mediaUrls?.firstOrNull() ?: legacyUrl
								postThumbnails[doc.id] = firstUrl
							}
						}
					}
				}

				// Re-publish with resolved details
				publishGrouped(rawList)
			}
		}
	}

	private fun publishGrouped(rawList: List<AppNotification>) {
		val grouped = mutableListOf<GroupedNotification>()
		val processedIds = mutableSetOf<String>()

		for (notif in rawList) {
			if (notif.id in processedIds) continue

			// Determine group members
			val groupMembers = if (notif.targetId.isNotBlank() && notif.type in listOf("like", "comment", "love", "reaction", "story_reply", "mention", "chat_message")) {
				rawList.filter { it.type == notif.type && it.targetId == notif.targetId }
			} else if (notif.type == "follow" || notif.type == "friend_request") {
				rawList.filter { it.type == notif.type }
			} else {
				listOf(notif)
			}

			processedIds.addAll(groupMembers.map { it.id })

			val actorIds = groupMembers.map { it.actorUserId }.distinct()
			val actors = actorIds.map { id ->
				actorProfiles[id] ?: ActorProfile(
					userId = id,
					displayName = groupMembers.firstOrNull { it.actorUserId == id }?.actorDisplayName ?: "Someone",
					avatarEmoji = "👤"
				)
			}

			val latestNotif = groupMembers.maxByOrNull { it.createdAt } ?: notif
			val allRead = groupMembers.all { it.isRead }
			val count = groupMembers.size

			val title: String
			val body: String
			val firstActorName = actors.firstOrNull()?.displayName ?: notif.actorDisplayName.ifBlank { "Someone" }

			when (notif.type) {
				"like", "love", "reaction" -> {
					title = if (count > 1) "$firstActorName and ${count - 1} others" else firstActorName
					body = "liked your post"
				}
				"comment" -> {
					title = if (count > 1) "$firstActorName and ${count - 1} others" else firstActorName
					body = if (count > 1) "commented on your post" else "commented: \"${notif.body}\""
				}
				"chat_message", "direct_message" -> {
					title = firstActorName
					body = if (count > 1) "sent you $count messages" else notif.body
				}
				"follow", "friend_request" -> {
					title = if (count > 1) "$firstActorName and ${count - 1} others" else firstActorName
					body = if (notif.type == "friend_request") "sent you a follow request" else "started following you"
				}
				"story_reply" -> {
					title = firstActorName
					body = "replied to your story: \"${notif.body}\""
				}
				"mention" -> {
					title = if (count > 1) "$firstActorName and ${count - 1} others" else firstActorName
					body = "mentioned you in a post"
				}
				else -> {
					title = notif.title
					body = notif.body
				}
			}

			val postImageUrl = postThumbnails[notif.targetId] ?: ""

			grouped.add(
				GroupedNotification(
					id = latestNotif.id,
					type = notif.type,
					targetId = notif.targetId,
					latestCreatedAt = latestNotif.createdAt,
					isRead = allRead,
					actors = actors,
					actorUserIds = actorIds,
					count = count,
					title = title,
					body = body,
					postImageUrl = postImageUrl,
					rawNotifications = groupMembers
				)
			)
		}

		_uiState.update { it.copy(groupedNotifications = grouped, isLoading = false, error = null) }
	}

	fun markGroupAsRead(grouped: GroupedNotification) {
		val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
		viewModelScope.launch {
			runCatching {
				grouped.rawNotifications.forEach { notif ->
					if (!notif.isRead) {
						NotificationLogger.markNotificationRead(userId, notif.id)
					}
				}
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		listener?.remove()
	}
}
