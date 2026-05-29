package com.example.kampus.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.PostRepositoryImpl
import com.example.kampus.data.repository.EventRepositoryImpl
import com.example.kampus.data.repository.UserRepositoryImpl
import com.example.kampus.domain.model.User
import com.example.kampus.domain.model.Friend
import com.example.kampus.domain.model.FriendRequest
import com.example.kampus.domain.repository.IEventRepository
import com.example.kampus.domain.repository.IUserRepository
import com.example.kampus.utils.ActivityLogger
import com.example.kampus.utils.NotificationLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.kampus.data.repository.EventEngagementRepository
import com.example.kampus.ui.feed.PostItem
import kotlin.math.max

data class ProfileUiState(
	val userId: String = "",
	val displayName: String = "",
	val handle: String = "",
	val bio: String = "",
	val email: String = "",
	val phone: String = "",
	val faculty: String = "",
	val year: String = "",
	val location: String = "",
	val avatarEmoji: String = "🎓",
	val profileImageUrl: String = "",
	val coverImageUrl: String = "",
	val stats: ProfileStats = ProfileStats(),
	val friends: List<Friend> = emptyList(),
	val followers: List<Friend> = emptyList(),
	val following: List<Friend> = emptyList(),
	val friendRequests: List<FriendRequest> = emptyList(),
	val outgoingFriendRequests: List<FriendRequest> = emptyList(),
	val blockedUsers: List<Friend> = emptyList(),
	val activities: List<ProfileActivityItem> = emptyList(),
	val isOnline: Boolean = false,
	val isLoading: Boolean = false,
	val error: String? = null,
	val settings: SettingsState = SettingsState(),
	val notificationSettings: NotificationSettingsState = NotificationSettingsState(),
	val privacySettings: PrivacySettingsState = PrivacySettingsState(),
	val timelinePosts: List<PostItem> = emptyList(),
)

data class ProfileActivityItem(
	val type: String,
	val text: String,
	val createdAt: Long,
	val updatedAt: Long = createdAt,
	val sourceId: String? = null,
	val eventId: Int? = null,
	val postId: Int? = null,
	val previewTitle: String = "",
	val previewSubtitle: String = "",
	val previewImageUrl: String = "",
	val likeCount: Int = 0,
	val commentCount: Int = 0,
	val shareCount: Int = 0,
	val postVisibility: com.example.kampus.ui.feed.PostItem.PostVisibility? = null,
	val isPinned: Boolean = false,
	val isArchived: Boolean = false,
	val currentUserLoved: Boolean = false,
	val loveList: List<String> = emptyList(),
	val commentsList: List<ActivityComment> = emptyList(),
	val eventDate: String = "",
	val eventTime: String = "",
	val eventLocation: String = "",
	val eventInterestedCount: Int = 0,
	val currentUserInterested: Boolean = false,
	val mediaUrls: List<String> = emptyList(),
	val mediaTypes: List<String> = emptyList(),
	val groupId: String? = null,
	val sharedOriginalPostId: Int? = null,
	val sharedOriginalAuthor: String? = null,
	val sharedOriginalAuthorId: String? = null,
	val sharedOriginalAvatar: String? = null,
	val sharedOriginalProfileImageUrl: String? = null,
	val sharedOriginalTime: String? = null,
	val sharedOriginalTimestamp: Long? = null,
	val sharedOriginalContent: String? = null,
	val sharedOriginalMediaUrls: List<String> = emptyList(),
	val sharedOriginalMediaTypes: List<String> = emptyList(),
	val sharedOriginalMediaEmojis: List<String> = emptyList(),
	val sharedOriginalLikes: Int? = null,
	val sharedOriginalComments: Int? = null,
	val sharedOriginalShares: Int? = null,
	val sharedOriginalVisibility: com.example.kampus.ui.feed.PostItem.PostVisibility? = null,
	val sharedOriginalIsVerified: Boolean? = null,
)

data class ActivityComment(
	val id: String = "",
	val userId: String = "",
	val userName: String = "",
	val userAvatar: String = "",
	val text: String = "",
	val createdAt: Long = 0L,
)

data class ProfileStats(
	val posts: Int = 0,
	val followers: Int = 0,
	val following: Int = 0,
	val friendRequests: Int = 0,
)

data class SettingsState(
	val pushNotifications: Boolean = true,
	val emailUpdates: Boolean = false,
	val privateAccount: Boolean = false,
	val darkModeLocked: Boolean = true,
)

data class NotificationSettingsState(
	val pushNotifications: Boolean = true,
	val likes: Boolean = true,
	val comments: Boolean = true,
	val newFollowers: Boolean = true,
	val mentions: Boolean = true,
	val directMessages: Boolean = true,
	val groupActivity: Boolean = false,
	val emailNotifications: Boolean = true,
	val weeklyDigest: Boolean = false,
	val smsNotifications: Boolean = false,
)

data class PrivacySettingsState(
	val privateAccount: Boolean = false,
	val activityStatus: Boolean = true,
	val allowTagging: Boolean = true,
	val allowMentions: Boolean = true,
	val twoFactorAuthentication: Boolean = false,
)

class ProfileViewModel(
	private val userRepository: IUserRepository = UserRepositoryImpl(
		FirebaseFirestore.getInstance(),
		FirebaseAuth.getInstance()
	),
	private val eventRepository: IEventRepository = EventRepositoryImpl(),
	private val postRepository: PostRepositoryImpl = PostRepositoryImpl(FirebaseFirestore.getInstance()),
) : ViewModel() {
	private companion object {
		val HIDDEN_ACTIVITY_TYPES = setOf(
			"delete_post",
			"edit_post",
			"edit_privacy",
			"hide_from_profile",
			"archive_activity",
			"activity_notifications",
			"pin_post",
			"unpin_post",
		)
	}

	private val _uiState = MutableStateFlow(ProfileUiState())
	val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
	private var activityListener: ListenerRegistration? = null
	private var postActivityListener: ListenerRegistration? = null
	private var groupActivityListener: ListenerRegistration? = null
	private var notificationSettingsListener: ListenerRegistration? = null
	private var privacySettingsListener: ListenerRegistration? = null
	private var blockedUsersListener: ListenerRegistration? = null
	private val eventEngagementListeners = mutableMapOf<String, ListenerRegistration>()

	// High-fidelity feed post and event activity cache
	private var firestoreActivities = emptyList<ProfileActivityItem>()
	private var eventActivities = emptyList<ProfileActivityItem>()
	private var userTimelinePosts = emptyList<com.example.kampus.ui.feed.PostItem>()

	private val currentUserId: String?
		get() = FirebaseAuth.getInstance().currentUser?.uid

	init {
		initializeRealTimeUpdates()
	}

	private fun initializeRealTimeUpdates() {
		currentUserId?.let { userId ->
			// Don't show loading - data comes from real-time listeners

			var latestPostsCount = 0
			var latestFollowersCount = 0
			var latestFollowingCount = 0
			var latestFriendsCount = 0

			fun publishStats() {
				val relationshipFallback = max(latestFriendsCount, max(latestFollowersCount, latestFollowingCount))
				_uiState.update { state ->
					state.copy(
						stats = state.stats.copy(
							followers = max(state.stats.followers, relationshipFallback),
							following = max(state.stats.following, relationshipFallback),
						),
					)
				}
			}

			viewModelScope.launch {
				userRepository.syncProfileStats(userId)
			}
			
			fun isLegacyDemoProfile(user: User): Boolean {
				return user.bio == "Computer Science student. Building KAMPUS one screen at a time." ||
					user.location == "London UK" ||
					(user.stats.posts == 181 && user.stats.followers == 1496 && user.stats.following == 428)
			}

			fun scrubLegacyProfile(user: User): User {
				if (!isLegacyDemoProfile(user)) return user
				return user.copy(
					bio = "",
					phone = "",
					faculty = "",
					year = "",
					location = "",
					stats = user.stats.copy(
						posts = 0,
						followers = 0,
						following = 0,
						friendRequests = 0,
					),
				)
			}

			// Observe profile updates
			viewModelScope.launch {
				userRepository.getCurrentUserProfile().collect { result ->
					result.onSuccess { user ->
						val sanitizedUser = scrubLegacyProfile(user)
						_uiState.update {
							it.copy(
								userId = sanitizedUser.id,
								displayName = sanitizedUser.displayName,
								handle = sanitizedUser.handle,
								bio = sanitizedUser.bio,
								email = sanitizedUser.email,
								phone = sanitizedUser.phone,
								faculty = sanitizedUser.faculty,
								year = sanitizedUser.year,
								location = sanitizedUser.location,
								avatarEmoji = sanitizedUser.avatarEmoji,
								profileImageUrl = sanitizedUser.profileImageUrl,
								coverImageUrl = sanitizedUser.coverImageUrl,
								isOnline = sanitizedUser.isOnline,
								stats = ProfileStats(
									posts = max(sanitizedUser.stats.posts, it.stats.posts),
									// Keep live counters sourced from dedicated real-time listeners, but never
									// throw away a populated server-side snapshot during startup.
									followers = max(it.stats.followers, sanitizedUser.stats.followers),
									following = max(it.stats.following, sanitizedUser.stats.following),
									friendRequests = it.stats.friendRequests,
								),
								isLoading = false,
							)
						}

							latestPostsCount = max(latestPostsCount, sanitizedUser.stats.posts)
							latestFollowersCount = max(latestFollowersCount, sanitizedUser.stats.followers)
							latestFollowingCount = max(latestFollowingCount, sanitizedUser.stats.following)
							publishStats()

						if (sanitizedUser != user) {
							viewModelScope.launch {
								try {
									FirebaseFirestore.getInstance()
										.collection("users")
										.document(user.id)
										.update(
											mapOf(
												"bio" to "",
												"phone" to "",
												"faculty" to "",
												"year" to "",
												"location" to "",
												"stats.posts" to 0,
												"stats.followers" to 0,
												"stats.following" to 0,
												"stats.friendRequests" to 0,
											)
										)
										.await()
								} catch (_: Exception) {
									// Ignore cleanup failures; the UI already shows the sanitized state.
								}
							}
						}
					}
					result.onFailure { error ->
						_uiState.update { it.copy(error = error.message, isLoading = false) }
					}
				}
			}

			// Observe friends in real-time
			viewModelScope.launch {
				userRepository.getFriends(userId).collect { result ->
					result.onSuccess { friends ->
						latestFriendsCount = friends.size
						_uiState.update { it.copy(friends = friends) }
						publishStats()
					}
				}
			}

			// Observe followers in real-time
			viewModelScope.launch {
				userRepository.getFollowers(userId).collect { result ->
					result.onSuccess { followers ->
						latestFollowersCount = followers.size
						_uiState.update {
							it.copy(
								followers = followers,
							)
						}
						publishStats()
					}
				}
			}

			viewModelScope.launch {
				userRepository.getFollowersCount(userId).collect { result ->
					result.onSuccess { count ->
						latestFollowersCount = count
						publishStats()
					}
				}
			}

			// Observe following in real-time
			viewModelScope.launch {
				userRepository.getFollowing(userId).collect { result ->
					result.onSuccess { following ->
						latestFollowingCount = following.size
						_uiState.update {
							it.copy(
								following = following,
							)
						}
						publishStats()
					}
				}
			}

			viewModelScope.launch {
				userRepository.getFollowingCount(userId).collect { result ->
					result.onSuccess { count ->
						latestFollowingCount = count
						publishStats()
					}
				}
			}

			viewModelScope.launch {
				userRepository.getFriendsCount(userId).collect { result ->
					result.onSuccess { count ->
						latestFriendsCount = count
						publishStats()
					}
				}
			}

			// Observe friend requests in real-time
			viewModelScope.launch {
				android.util.Log.d("ProfileViewModel", "Attaching friendRequests listener for user: $userId")
				userRepository.getFriendRequests(userId).collect { result ->
					result.onSuccess { requests ->
						android.util.Log.d("ProfileViewModel", "Received ${requests.size} friend requests for user: $userId")
						_uiState.update {
							it.copy(
								friendRequests = requests,
								stats = it.stats.copy(friendRequests = requests.size)
							)
						}
					}
					result.onFailure { error ->
						android.util.Log.e("ProfileViewModel", "Error loading friend requests: ${error.message}")
					}
				}
			}

			// Observe outgoing friend requests in real-time
			viewModelScope.launch {
				userRepository.getOutgoingFriendRequests(userId).collect { result ->
					result.onSuccess { requests ->
						_uiState.update { it.copy(outgoingFriendRequests = requests) }
					}
					result.onFailure { error ->
						android.util.Log.e("ProfileViewModel", "Error loading outgoing friend requests for $userId: ${error.message}")
					}
				}
			}

			// Observe online status in real-time
			viewModelScope.launch {
				userRepository.observeUserOnlineStatus(userId).collect { result ->
					result.onSuccess { isOnline ->
						_uiState.update { it.copy(isOnline = isOnline) }
					}
				}
			}

			observeRecentActivities(userId)
			observeTimelinePosts(userId)
			observeNotificationSettings(userId)
			observePrivacySettings(userId)
			observeBlockedUsers(userId)
		}
	}

	private fun observeTimelinePosts(userId: String) {
		viewModelScope.launch {
			postRepository.getFeedPosts().collect { result ->
				result.onSuccess { posts ->
					val profilePosts = posts
						.filter { it.authorId == userId }
						.sortedWith(
							compareByDescending<PostItem> { it.isPinned }
								.thenByDescending { it.id }
						)
						.take(30)
					_uiState.update {
						it.copy(
							timelinePosts = profilePosts,
						)
					}
				}
				result.onFailure { error ->
					_uiState.update { it.copy(error = error.message ?: "Failed to load profile timeline") }
				}
			}
		}
	}

	private fun observePrivacySettings(userId: String) {
		privacySettingsListener?.remove()
		privacySettingsListener = FirebaseFirestore.getInstance()
			.collection("users")
			.document(userId)
			.addSnapshotListener { snapshot, error ->
				if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

				val raw = snapshot.get("privacySettings") as? Map<*, *> ?: emptyMap<String, Any>()
				val parsed = PrivacySettingsState(
					privateAccount = (raw["privateAccount"] as? Boolean) ?: snapshot.getBoolean("privateAccount") ?: false,
					activityStatus = (raw["activityStatus"] as? Boolean) ?: snapshot.getBoolean("isOnline") ?: true,
					allowTagging = (raw["allowTagging"] as? Boolean) ?: true,
					allowMentions = (raw["allowMentions"] as? Boolean) ?: true,
					twoFactorAuthentication = (raw["twoFactorAuthentication"] as? Boolean) ?: false,
				)

				_uiState.update { it.copy(privacySettings = parsed) }
			}
	}

	private fun updatePrivacySettings(fields: Map<String, Any>) {
		val userId = currentUserId ?: return
		viewModelScope.launch {
			runCatching {
				FirebaseFirestore.getInstance()
					.collection("users")
					.document(userId)
					.update(fields + mapOf("updatedAt" to System.currentTimeMillis()))
					.await()
			}.onFailure { error ->
				_uiState.update { it.copy(error = error.message ?: "Failed to update privacy settings") }
			}
		}
	}

	fun setPrivateAccount(enabled: Boolean) {
		updatePrivacySettings(mapOf("privacySettings.privateAccount" to enabled))
	}

	fun setActivityStatus(enabled: Boolean) {
		updatePrivacySettings(mapOf("privacySettings.activityStatus" to enabled, "isOnline" to enabled))
	}

	fun setAllowTagging(enabled: Boolean) {
		updatePrivacySettings(mapOf("privacySettings.allowTagging" to enabled))
	}

	fun setAllowMentions(enabled: Boolean) {
		updatePrivacySettings(mapOf("privacySettings.allowMentions" to enabled))
	}

	fun setTwoFactorAuthentication(enabled: Boolean) {
		updatePrivacySettings(mapOf("privacySettings.twoFactorAuthentication" to enabled))
	}

	private fun observeNotificationSettings(userId: String) {
		notificationSettingsListener?.remove()
		notificationSettingsListener = FirebaseFirestore.getInstance()
			.collection("users")
			.document(userId)
			.addSnapshotListener { snapshot, error ->
				if (error != null || snapshot == null || !snapshot.exists()) {
					return@addSnapshotListener
				}

				val raw = snapshot.get("notificationSettings") as? Map<*, *> ?: emptyMap<String, Any>()
				val parsed = NotificationSettingsState(
					pushNotifications = (raw["pushNotifications"] as? Boolean) ?: true,
					likes = (raw["likes"] as? Boolean) ?: true,
					comments = (raw["comments"] as? Boolean) ?: true,
					newFollowers = (raw["newFollowers"] as? Boolean) ?: true,
					mentions = (raw["mentions"] as? Boolean) ?: true,
					directMessages = (raw["directMessages"] as? Boolean) ?: true,
					groupActivity = (raw["groupActivity"] as? Boolean) ?: false,
					emailNotifications = (raw["emailNotifications"] as? Boolean) ?: true,
					weeklyDigest = (raw["weeklyDigest"] as? Boolean) ?: false,
					smsNotifications = (raw["smsNotifications"] as? Boolean) ?: false,
				)

				_uiState.update { it.copy(notificationSettings = parsed) }
			}
	}

	private fun updateNotificationSettings(fields: Map<String, Any>) {
		val userId = currentUserId ?: return
		viewModelScope.launch {
			runCatching {
				FirebaseFirestore.getInstance()
					.collection("users")
					.document(userId)
					.update(fields + mapOf("updatedAt" to System.currentTimeMillis()))
					.await()
			}.onFailure { error ->
				_uiState.update { it.copy(error = error.message ?: "Failed to update notification settings") }
			}
		}
	}

	fun setPushNotificationsEnabled(enabled: Boolean) {
		updateNotificationSettings(
			mapOf(
				"notificationSettings.pushNotifications" to enabled,
				"notificationSettings.likes" to enabled,
				"notificationSettings.comments" to enabled,
				"notificationSettings.newFollowers" to enabled,
				"notificationSettings.mentions" to enabled,
				"notificationSettings.directMessages" to enabled,
				"notificationSettings.groupActivity" to enabled,
			)
		)
	}

	fun setEmailNotificationsEnabled(enabled: Boolean) {
		updateNotificationSettings(
			mapOf(
				"notificationSettings.emailNotifications" to enabled,
				"notificationSettings.weeklyDigest" to if (enabled) _uiState.value.notificationSettings.weeklyDigest else false,
			)
		)
	}

	fun setNotificationToggle(key: String, enabled: Boolean) {
		updateNotificationSettings(mapOf("notificationSettings.$key" to enabled))
	}

	private fun publishRecentActivities() {
		val userId = currentUserId ?: return

		// 1. Process firestoreActivities: only filter out "create_post" since those come
		// from userTimelinePosts (real-time feed). Keep create_event, share_post, share_profile, etc.
		val allowedFirestoreTypes = setOf("create_event", "share_post", "share_profile", "create_group")
		val filteredFirestore = firestoreActivities.filter {
			it.type in allowedFirestoreTypes
		}

		// 2. Process userTimelinePosts: map them to ProfileActivityItems of type "create_post"
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
				previewSubtitle = "Shared from your feed",
				previewImageUrl = post.mediaUris.firstOrNull()?.toString() ?: post.imageUri?.toString() ?: "",
				likeCount = post.likes,
				commentCount = post.comments.coerceAtLeast(existing?.commentsList?.size ?: 0),
				shareCount = post.shares,
				postVisibility = post.visibility,
				isPinned = post.isPinned || (existing?.isPinned == true),
				currentUserLoved = post.likedBy.contains(userId),
				loveList = post.likedBy,
				commentsList = existing?.commentsList ?: emptyList(),
				mediaUrls = post.mediaUris.map { it.toString() },
				mediaTypes = post.mediaTypes.map { it.name },
				groupId = null,
				sharedOriginalPostId = post.sharedOriginalPostId,
				sharedOriginalAuthor = post.sharedOriginalAuthor,
				sharedOriginalAuthorId = post.sharedOriginalAuthorId,
				sharedOriginalAvatar = post.sharedOriginalAvatar,
				sharedOriginalProfileImageUrl = post.sharedOriginalProfileImageUrl,
				sharedOriginalTime = post.sharedOriginalTime,
				sharedOriginalTimestamp = post.sharedOriginalTimestamp,
				sharedOriginalContent = post.sharedOriginalContent,
				sharedOriginalMediaUrls = post.sharedOriginalMediaUris.map { it.toString() },
				sharedOriginalMediaTypes = post.sharedOriginalMediaTypes.map { it.name },
				sharedOriginalMediaEmojis = post.sharedOriginalMediaEmojis,
				sharedOriginalLikes = post.sharedOriginalLikes,
				sharedOriginalComments = post.sharedOriginalComments,
				sharedOriginalShares = post.sharedOriginalShares,
				sharedOriginalVisibility = post.sharedOriginalVisibility,
				sharedOriginalIsVerified = post.sharedOriginalIsVerified,
			)
		}

		// 3. Combine all
		val allActivities = filteredFirestore + eventActivities + mappedTimelinePosts

		// 4. Deduplicate using a unique key
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

		val total = userTimelinePosts.size + eventActivities.size
		_uiState.update { it.copy(activities = combined, stats = it.stats.copy(posts = total)) }
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

	private fun observeRecentActivities(userId: String) {
		activityListener?.remove()
		postActivityListener?.remove()
		groupActivityListener?.remove()

		firestoreActivities = emptyList()
		eventActivities = emptyList()
		userTimelinePosts = emptyList()

		// Real-time listener for the user's feed posts
		viewModelScope.launch {
			postRepository.getFeedPosts().collect { result ->
				result.onSuccess { posts ->
					val filtered = posts.filter { it.authorId == userId }
					val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
					val engagementSnapshots = com.example.kampus.data.repository.PostEngagementRepository(FirebaseFirestore.getInstance())
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

		// Main Firestore activities listener - handles all activity types
		activityListener = FirebaseFirestore.getInstance()
			.collection("users")
			.document(userId)
			.collection("activities")
			.orderBy("createdAt", Query.Direction.DESCENDING)
			.limit(20) // Fetch limit 20 for efficiency
			.addSnapshotListener { snapshot, error ->
				if (error != null) {
					_uiState.update { it.copy(error = error.message ?: "Failed to load activity") }
					return@addSnapshotListener
				}

				val activities = snapshot?.documents
					?.mapNotNull { doc ->
						val createdAt = normalizeTimestamp(parseTimestampField(doc, "createdAt"))
						val updatedAt = normalizeTimestamp(doc.getLong("updatedAt") ?: createdAt)
						val eventIdStr = doc.getString("eventId")
						val eventId = eventIdStr?.toIntOrNull() ?: eventIdStr?.hashCode()
						val postId = parseIntField(doc, "postId")
						val type = doc.getString("type") ?: "activity"
						if (type in HIDDEN_ACTIVITY_TYPES) return@mapNotNull null
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
							"share_post" -> doc.getString("previewSubtitle") ?: if (author.isNotBlank()) "Shared from $author" else "Shared from your feed"
							"share_profile" -> doc.getString("previewSubtitle") ?: "Shared on your profile"
							"create_post" -> doc.getString("previewSubtitle") ?: "Shared from your feed"
							"create_event" -> doc.getString("previewSubtitle") ?: "Shared from your events"
							"create_group" -> doc.getString("previewSubtitle") ?: "Shared from your groups"
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
							currentUserLoved = loveList.contains(userId),
							loveList = loveList,
							commentsList = commentsList,
							mediaUrls = mediaUrls,
							mediaTypes = mediaTypes,
							groupId = groupId,
						)
					}
					?: emptyList()

				firestoreActivities = activities
				publishRecentActivities()
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
	}

	private fun observeEventEngagement(eventId: String) {
		if (eventEngagementListeners.containsKey(eventId + "_summary")) return
		val userId = currentUserId ?: return

		val firestore = FirebaseFirestore.getInstance()
		
		// 1. Listen to event summary (likeCount, commentCount, etc.)
		val summaryListener = firestore.collection("event_engagements").document(eventId)
			.addSnapshotListener { snapshot, error ->
				if (error != null || snapshot == null) return@addSnapshotListener
				
				val likesCount = (snapshot.getLong("likesCount") ?: 0L).toInt()
				val commentsCount = (snapshot.getLong("commentsCount") ?: 0L).toInt()
				val interestedCount = (snapshot.getLong("interestedCount") ?: 0L).toInt()
				val sharesCount = (snapshot.getLong("sharesCount") ?: 0L).toInt()

				// Fetch current user interest state
				firestore.collection("event_engagements").document(eventId)
					.collection("members").document(userId).get()
					.addOnSuccessListener { memberSnapshot ->
						val isInterested = memberSnapshot.getBoolean("interested") ?: false
						val isLiked = memberSnapshot.getBoolean("liked") ?: false
						
						updateLocalActivityBySourceId(eventId) { current ->
							current.copy(
								likeCount = likesCount,
								shareCount = sharesCount,
								eventInterestedCount = interestedCount,
								currentUserLoved = isLiked,
								loveList = if (isLiked) listOf(userId) else emptyList(),
								currentUserInterested = isInterested
							)
						}
					}
			}
		
		// 2. Listen to event comments sub-collection
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

	private fun removeLocalActivity(activity: ProfileActivityItem) {
		firestoreActivities = firestoreActivities.filterNot { it.sourceId == activity.sourceId }
		eventActivities = eventActivities.filterNot { it.sourceId == activity.sourceId }
		publishRecentActivities()
	}

	private suspend fun updateActivityBackends(activity: ProfileActivityItem, updates: Map<String, Any>) {
		when (activity.type) {
			"create_post" -> {
				val postUpdates = updates.toMutableMap()
				if (updates.containsKey("loveList")) {
					postUpdates["likedBy"] = updates["loveList"]!!
					postUpdates.remove("loveList")
				}
				if (updates.containsKey("likeCount")) {
					postUpdates["likes"] = updates["likeCount"]!!
					postUpdates.remove("likeCount")
				}
				if (updates.containsKey("commentCount")) {
					postUpdates["comments"] = updates["commentCount"]!!
					postUpdates.remove("commentCount")
				}
				if (updates.containsKey("commentsList")) {
					postUpdates.remove("commentsList")
				}
				if (updates.containsKey("shareCount")) {
					postUpdates["shares"] = updates["shareCount"]!!
					postUpdates.remove("shareCount")
				}
				if (postUpdates.isNotEmpty()) {
					postRef(activity)?.update(postUpdates as Map<String, Any>)?.await()
				}
				activityRef(activity)?.update(updates)?.await()
			}
			else -> activityRef(activity)?.update(updates)?.await()
		}
	}

	private suspend fun deleteActivityBackends(activity: ProfileActivityItem) {
		when (activity.type) {
			"create_post" -> {
				postRef(activity)?.delete()?.await()
				activityRef(activity)?.delete()?.await()
			}
			else -> activityRef(activity)?.delete()?.await()
		}
	}

	private fun normalizeTimestamp(timestamp: Long): Long {
		if (timestamp <= 0L) return 0L
		return if (timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
	}

	fun togglePushNotifications() {
		_uiState.update { state ->
			state.copy(settings = state.settings.copy(pushNotifications = !state.settings.pushNotifications))
		}
	}

	fun toggleEmailUpdates() {
		_uiState.update { state ->
			state.copy(settings = state.settings.copy(emailUpdates = !state.settings.emailUpdates))
		}
	}

	fun togglePrivateAccount() {
		_uiState.update { state ->
			state.copy(settings = state.settings.copy(privateAccount = !state.settings.privateAccount))
		}
	}

	fun acceptFriendRequest(requestId: String) {
		viewModelScope.launch {
			val result = userRepository.acceptFriendRequest(requestId)
			result.onSuccess {
				_uiState.update { state ->
					state.copy(
						friendRequests = state.friendRequests.filterNot { it.id == requestId },
						outgoingFriendRequests = state.outgoingFriendRequests.filterNot { it.id == requestId },
						stats = state.stats.copy(friendRequests = (state.stats.friendRequests - 1).coerceAtLeast(0)),
					)
				}
			}
			result.onFailure { error ->
				_uiState.update { it.copy(error = error.message) }
			}
		}
	}

	fun rejectFriendRequest(requestId: String) {
		viewModelScope.launch {
			val result = userRepository.rejectFriendRequest(requestId)
			result.onSuccess {
				_uiState.update { state ->
					state.copy(
						friendRequests = state.friendRequests.filterNot { it.id == requestId },
						outgoingFriendRequests = state.outgoingFriendRequests.filterNot { it.id == requestId },
						stats = state.stats.copy(friendRequests = (state.stats.friendRequests - 1).coerceAtLeast(0)),
					)
				}
			}
			result.onFailure { error ->
				_uiState.update { it.copy(error = error.message) }
			}
		}
	}

	fun cancelFriendRequest(requestId: String) {
		viewModelScope.launch {
			val result = userRepository.cancelFriendRequest(requestId)
			result.onSuccess {
				_uiState.update { state ->
					state.copy(
						outgoingFriendRequests = state.outgoingFriendRequests.filterNot { it.id == requestId },
					)
				}
			}
			result.onFailure { error ->
				_uiState.update { it.copy(error = error.message) }
			}
		}
	}

	fun removeFriend(friendId: String) {
		currentUserId?.let { userId ->
			viewModelScope.launch {
				val result = userRepository.removeFriend(userId, friendId)
				result.onFailure { error ->
					_uiState.update { it.copy(error = error.message) }
				}
			}
		}
	}

	fun sendFriendRequest(toUserId: String) {
		currentUserId?.let { userId ->
			viewModelScope.launch {
				android.util.Log.d("ProfileViewModel", "Processing follow/request from $userId to $toUserId")
				// Check if target is a private account. If not, perform immediate follow to keep UX fast.
				val privacyRes = userRepository.isUserPrivate(toUserId)
				privacyRes.onSuccess { isPrivate ->
					if (!isPrivate) {
						// Public account: follow immediately
						val followRes = userRepository.followUser(userId, toUserId)
						followRes.onSuccess {
							android.util.Log.d("ProfileViewModel", "Followed user directly: $userId -> $toUserId")
							runCatching {
								NotificationLogger.notifyUser(
									toUserId = toUserId,
									type = "follow",
									title = "New Follower",
									body = "You have a new follower",
									targetId = userId,
								)
							}
						}
						followRes.onFailure { error ->
							android.util.Log.e("ProfileViewModel", "Error following user: ${error.message}")
							_uiState.update { it.copy(error = error.message) }
						}
					} else {
						// Private account: send friend request as before
						val result = userRepository.sendFriendRequest(userId, toUserId)
						result.onSuccess {
							android.util.Log.d("ProfileViewModel", "Friend request sent successfully from $userId to $toUserId")
							runCatching {
								NotificationLogger.notifyUser(
									toUserId = toUserId,
									type = "friend_request",
									title = "New Follow Request",
									body = "Someone sent you a follow request",
									targetId = userId,
								)
							}
						}
						result.onFailure { error ->
							android.util.Log.e("ProfileViewModel", "Error sending friend request: ${error.message}")
							_uiState.update { it.copy(error = error.message) }
						}
					}
				}
				privacyRes.onFailure { error ->
					android.util.Log.e("ProfileViewModel", "Failed to check privacy for $toUserId: ${error.message}")
					// Fallback to sending a request to be safe
					val result = userRepository.sendFriendRequest(userId, toUserId)
					result.onFailure { err -> _uiState.update { it.copy(error = err.message) } }
				}
			}
		}
	}

	fun clearError() {
		_uiState.update { it.copy(error = null) }
	}

	private fun activityRef(activity: ProfileActivityItem): com.google.firebase.firestore.DocumentReference? {
		val userId = currentUserId ?: return null
		val sourceId = activity.sourceId ?: return null
		return FirebaseFirestore.getInstance().collection("users").document(userId).collection("activities").document(sourceId)
	}

	private fun postRef(activity: ProfileActivityItem): com.google.firebase.firestore.DocumentReference? {
		val documentId = activity.postId?.toString() ?: activity.sourceId ?: return null
		return FirebaseFirestore.getInstance().collection("posts").document(documentId)
	}

	fun pinPostActivity(activity: ProfileActivityItem, isPinned: Boolean = true) {
		viewModelScope.launch {
			try {
				updateLocalActivity(activity) { current ->
					current.copy(isPinned = isPinned)
				}
				updateActivityBackends(
					activity,
					mapOf(
						"isPinned" to isPinned,
						"pinnedAt" to if (isPinned) System.currentTimeMillis() else 0L,
					),
				)
				ActivityLogger.logAction(
					type = if (isPinned) "pin_post" else "unpin_post",
					text = if (isPinned) "Pinned post ${activity.postId ?: activity.sourceId ?: activity.text}" else "Unpinned post ${activity.postId ?: activity.sourceId ?: activity.text}",
				)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to update pin state") }
			}
		}
	}

	fun editPostActivity(activity: ProfileActivityItem, newContent: String) {
		viewModelScope.launch {
			try {
				val rawId = activity.postId?.toString() ?: activity.sourceId ?: return@launch
				val cleanPostId = rawId.replace("post_", "")
				val updatedAt = System.currentTimeMillis()

				// Update userTimelinePosts list immediately so the UI is responsive
				val cleanPostIdInt = cleanPostId.toIntOrNull()
				if (cleanPostIdInt != null) {
					userTimelinePosts = userTimelinePosts.map { post ->
						if (post.id == cleanPostIdInt) post.copy(content = newContent) else post
					}
				}

				updateLocalActivity(activity) { current ->
					current.copy(text = newContent, updatedAt = updatedAt)
				}
				postRepository.updatePostContent(cleanPostId, newContent)
				updateActivityBackends(
					activity,
					mapOf(
						"text" to newContent,
						"updatedAt" to updatedAt,
					),
				)
				ActivityLogger.logAction(
					type = "edit_post",
					text = "Edited post content for $cleanPostId",
				)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to update activity") }
			}
		}
	}

	fun updatePostVisibility(activity: ProfileActivityItem, visibility: com.example.kampus.ui.feed.PostItem.PostVisibility) {
		viewModelScope.launch {
			try {
				val payload = mapOf(
					"visibility" to visibility.name.lowercase(),
					"updatedAt" to System.currentTimeMillis(),
				)
				updateLocalActivity(activity) { current ->
					current.copy(postVisibility = visibility)
				}
				updateActivityBackends(activity, payload)
				ActivityLogger.logAction(
					type = "edit_privacy",
					text = "Updated visibility for ${activity.postId ?: activity.sourceId ?: activity.text} to $visibility",
				)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to update privacy") }
			}
		}
	}

	fun deletePostFromProfile(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				removeLocalActivity(activity)
				val rawId = activity.postId?.toString() ?: activity.sourceId
				if (rawId != null) {
					val cleanPostId = rawId.replace("post_", "")

					// Remove from userTimelinePosts immediately for responsive UI
					val cleanPostIdInt = cleanPostId.toIntOrNull()
					if (cleanPostIdInt != null) {
						userTimelinePosts = userTimelinePosts.filterNot { it.id == cleanPostIdInt }
					}

					// Delete from both Supabase and Firestore collections
					postRepository.deletePost(cleanPostId)
				}
				deleteActivityBackends(activity)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to delete activity") }
			}
		}
	}

	fun hideFromProfile(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				val payload = mapOf(
					"hiddenFromProfile" to true,
					"updatedAt" to System.currentTimeMillis(),
				)
				removeLocalActivity(activity)
				updateActivityBackends(activity, payload)
				ActivityLogger.logAction(
					type = "hide_from_profile",
					text = "Hid post ${activity.postId ?: activity.sourceId ?: activity.text} from profile",
				)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to hide activity") }
			}
		}
	}

	fun archiveActivity(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				val payload = mapOf(
					"archivedAt" to System.currentTimeMillis(),
					"hiddenFromProfile" to true,
					"updatedAt" to System.currentTimeMillis(),
				)
				removeLocalActivity(activity)
				updateActivityBackends(activity, payload)
				ActivityLogger.logAction(
					type = "archive_activity",
					text = "Archived ${activity.postId ?: activity.sourceId ?: activity.text}",
				)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to archive activity") }
			}
		}
	}

	fun toggleActivityNotifications(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				val payload = mapOf(
					"notificationsEnabled" to true,
					"updatedAt" to System.currentTimeMillis(),
				)
				updateLocalActivity(activity) { current ->
					current.copy(updatedAt = System.currentTimeMillis())
				}
				updateActivityBackends(activity, payload)
				ActivityLogger.logAction(
					type = "activity_notifications",
					text = "Toggled notifications for ${activity.postId ?: activity.sourceId ?: activity.text}",
				)
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to update notifications") }
			}
		}
	}

	fun logShareProfileActivity() {
		ActivityLogger.logAction(
			type = "share_profile",
			text = "Shared profile",
		)
	}

	fun toggleActivityLove(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				currentUserId?.let { userId ->
					val isLoved = activity.currentUserLoved
					val newLoveList = activity.loveList.toMutableList()
					
					if (isLoved) {
						newLoveList.remove(userId)
					} else {
						if (!newLoveList.contains(userId)) newLoveList.add(userId)
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
							userId = userId,
							flagField = "liked",
							summaryField = "likesCount"
						)
					} else {
						updateActivityBackends(activity, payload)
					}
				}
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to love activity") }
			}
		}
	}

	fun toggleActivityInterest(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				currentUserId?.let { userId ->
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
							userId = userId,
							flagField = "interested",
							summaryField = "interestedCount"
						)
					}
				}
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to update interest") }
			}
		}
	}

	fun addActivityComment(activity: ProfileActivityItem, commentText: String) {
		viewModelScope.launch {
			try {
				currentUserId?.let { userId ->
					val user = _uiState.value
					val newComment = ActivityComment(
						id = "${userId}_${System.currentTimeMillis()}",
						userId = userId,
						userName = user.displayName.ifBlank { "You" },
						userAvatar = user.avatarEmoji,
						text = commentText,
						createdAt = System.currentTimeMillis(),
					)
					
					val newCommentsList = (activity.commentsList + newComment).takeLast(10) // Keep last 10
					val payload = mapOf(
						"commentsList" to newCommentsList.map {
							mapOf(
								"id" to it.id,
								"userId" to it.userId,
								"userName" to it.userName,
								"userAvatar" to it.userAvatar,
								"text" to it.text,
								"createdAt" to it.createdAt,
							)
						},
						"commentCount" to newCommentsList.size,
						"updatedAt" to System.currentTimeMillis(),
					)
					
					updateLocalActivity(activity) { current ->
						current.copy(
							commentsList = newCommentsList,
							commentCount = newCommentsList.size,
							updatedAt = System.currentTimeMillis(),
						)
					}
					
					if (activity.type == "create_event") {
						val eventEngagementRepo = EventEngagementRepository()
						eventEngagementRepo.addComment(
							eventId = activity.sourceId ?: "",
							userId = userId,
							authorName = user.displayName.ifBlank { "You" },
							authorEmoji = user.avatarEmoji,
							text = commentText,
							eventOwnerId = userId // Event creator is user
						)
					} else {
						updateActivityBackends(activity, payload)
					}
				}
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to add comment") }
			}
		}
	}

	fun shareActivity(activity: ProfileActivityItem) {
		viewModelScope.launch {
			try {
				currentUserId?.let { userId ->
					val newShareCount = activity.shareCount + 1
					val payload = mapOf(
						"shareCount" to newShareCount,
						"updatedAt" to System.currentTimeMillis(),
					)
					
					updateLocalActivity(activity) { current ->
						current.copy(
							shareCount = newShareCount,
							updatedAt = System.currentTimeMillis(),
						)
					}
					
					updateActivityBackends(activity, payload)
					
					val shareLink = "${getShareProfileLink()}/activity/${activity.sourceId}"
					val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
						type = "text/plain"
						putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this activity on KAMPUS")
						putExtra(android.content.Intent.EXTRA_TEXT, "Check out this ${activity.type.replace("create_", "")} on KAMPUS: $shareLink")
					}
					// Note: Actual sharing happens in UI layer with context
				}
			} catch (error: Exception) {
				_uiState.update { it.copy(error = error.message ?: "Failed to share activity") }
			}
		}
	}

	fun getShareProfileLink(): String? {
		val uid = currentUserId ?: return null
		return "https://kampus.app/profile/$uid"
	}

	fun updateCoverImage(imageUrl: String) {
		currentUserId?.let { userId ->
			viewModelScope.launch {
				val result = userRepository.updateCoverImage(userId, imageUrl)
				result.onFailure { error ->
					_uiState.update { it.copy(error = "Failed to update cover image: ${error.message}") }
				}
			}
		}
	}

	fun uploadCoverImageToSupabase(imageUri: Uri, context: android.content.Context) {
		if (currentUserId == null) {
			_uiState.update { it.copy(error = "User not authenticated. Please log in first.") }
			return
		}
		
		currentUserId?.let { userId ->
			viewModelScope.launch {
				try {
					android.util.Log.d("ProfileViewModel", "Starting cover image upload for user: $userId")
					_uiState.update { it.copy(isLoading = true, error = null) }
					
					// Get Supabase storage manager
					val storageManager = try {
						com.example.kampus.di.SupabaseModule.getStorageManager()
					} catch (e: Exception) {
						android.util.Log.e("ProfileViewModel", "Failed to get storage manager: ${e.message}")
						_uiState.update { it.copy(isLoading = false, error = "Storage initialization failed: ${e.message}") }
						return@launch
					}
					
					android.util.Log.d("ProfileViewModel", "Storage manager obtained, uploading image: $imageUri")
					
					// Upload to Supabase
					val uploadResult = storageManager.uploadCoverImage(userId, imageUri)
					
					uploadResult.onSuccess { imageUrl ->
						android.util.Log.d("ProfileViewModel", "Upload successful! Image URL: $imageUrl")
						
						// Save to Firestore BEFORE updating UI
						viewModelScope.launch {
							try {
								android.util.Log.d("ProfileViewModel", "Attempting to save cover image URL to Firestore: $imageUrl")
								val firestoreResult = userRepository.updateCoverImage(userId, imageUrl)
								
								firestoreResult.onSuccess {
									android.util.Log.d("ProfileViewModel", "✓ Firestore save successful!")
									// UI will update automatically via real-time listener
									_uiState.update { 
										it.copy(
											isLoading = false,
											error = null
										)
									}
								}
								
								firestoreResult.onFailure { error ->
									android.util.Log.e("ProfileViewModel", "✗ Firestore save FAILED: ${error.message}", error)
									_uiState.update { 
										it.copy(
											isLoading = false, 
											error = "Failed to save image: ${error.message}"
										)
									}
								}
							} catch (e: Exception) {
								android.util.Log.e("ProfileViewModel", "✗ Exception during Firestore save: ${e.message}", e)
								_uiState.update { 
									it.copy(
										isLoading = false,
										error = "Exception: ${e.message}"
									)
								}
							}
						}
					}
					uploadResult.onFailure { error ->
						android.util.Log.e("ProfileViewModel", "✗ Upload to Supabase failed: ${error.message}")
						_uiState.update { it.copy(isLoading = false, error = "Upload failed: ${error.message}") }
					}
				} catch (e: Exception) {
					android.util.Log.e("ProfileViewModel", "✗ Exception during upload: ${e.message}", e)
					_uiState.update { it.copy(isLoading = false, error = "Exception: ${e.message}") }
				}
			}
		}
	}

	fun uploadProfileImageToSupabase(imageUri: Uri, context: android.content.Context) {
		if (currentUserId == null) {
			_uiState.update { it.copy(error = "User not authenticated. Please log in first.") }
			return
		}
		
		currentUserId?.let { userId ->
			viewModelScope.launch {
				try {
					android.util.Log.d("ProfileViewModel", "Starting profile image upload for user: $userId")
					_uiState.update { it.copy(isLoading = true, error = null) }
					
					// Get Supabase storage manager
					val storageManager = try {
						com.example.kampus.di.SupabaseModule.getStorageManager()
					} catch (e: Exception) {
						android.util.Log.e("ProfileViewModel", "Failed to get storage manager: ${e.message}")
						_uiState.update { it.copy(isLoading = false, error = "Storage initialization failed: ${e.message}") }
						return@launch
					}
					
					android.util.Log.d("ProfileViewModel", "Storage manager obtained, uploading image: $imageUri")
					
					// Upload to Supabase
					val uploadResult = storageManager.uploadProfileImage(userId, imageUri)
					
					uploadResult.onSuccess { imageUrl ->
						android.util.Log.d("ProfileViewModel", "Upload successful! Image URL: $imageUrl")

						viewModelScope.launch {
							try {
								android.util.Log.d("ProfileViewModel", "Attempting to save profile image URL to Firestore: $imageUrl")
								val firestoreResult = userRepository.updateProfileImage(userId, imageUrl)

								firestoreResult.onSuccess {
									android.util.Log.d("ProfileViewModel", "✓ Firestore save successful!")
									// Real-time profile listener will pick this up, but update immediately for responsive UI.
									_uiState.update {
										it.copy(
											profileImageUrl = imageUrl,
											isLoading = false,
											error = null,
										)
									}
								}

								firestoreResult.onFailure { error ->
									android.util.Log.e("ProfileViewModel", "✗ Firestore save FAILED: ${error.message}", error)
									_uiState.update {
										it.copy(
											isLoading = false,
											error = "Failed to save image: ${error.message}",
										)
									}
								}
							} catch (e: Exception) {
								android.util.Log.e("ProfileViewModel", "✗ Exception during Firestore save: ${e.message}", e)
								_uiState.update {
									it.copy(
										isLoading = false,
										error = "Exception: ${e.message}",
									)
								}
							}
						}
					}
					uploadResult.onFailure { error ->
						android.util.Log.e("ProfileViewModel", "Upload failed: ${error.message}")
						_uiState.update { it.copy(isLoading = false, error = "Upload failed: ${error.message}") }
					}
				} catch (e: Exception) {
					android.util.Log.e("ProfileViewModel", "Exception during upload: ${e.message}", e)
					_uiState.update { it.copy(isLoading = false, error = "Exception: ${e.message}") }
				}
			}
		}
	}

	fun updateProfile(updatedUser: User, onComplete: (Result<Unit>) -> Unit = {}) {
		currentUserId?.let { userId ->
			val currentState = _uiState.value
			val hasChanges =
				currentState.displayName != updatedUser.displayName ||
				currentState.bio != updatedUser.bio ||
				currentState.email != updatedUser.email ||
				currentState.phone != updatedUser.phone ||
				currentState.faculty != updatedUser.faculty ||
				currentState.year != updatedUser.year ||
				currentState.location != updatedUser.location

			if (!hasChanges) {
				onComplete(Result.success(Unit))
				return
			}

			viewModelScope.launch {
				_uiState.update { it.copy(isLoading = true, error = null) }
				val result = userRepository.updateProfile(updatedUser)
				result.onSuccess {
					// UI updates automatically via real-time listener
					_uiState.update { it.copy(isLoading = false) }
					onComplete(Result.success(Unit))
				}
				result.onFailure { error ->
					_uiState.update { it.copy(isLoading = false, error = error.message) }
					onComplete(Result.failure(error))
				}
			}
		}
	}

	private fun observeBlockedUsers(userId: String) {
		blockedUsersListener?.remove()
		blockedUsersListener = FirebaseFirestore.getInstance()
			.collection("users")
			.document(userId)
			.collection("blockedUsers")
			.addSnapshotListener { snapshot, error ->
				if (error != null) {
					android.util.Log.e("ProfileViewModel", "Error loading blocked users: ${error.message}")
					return@addSnapshotListener
				}

				if (snapshot == null) return@addSnapshotListener

				val blockedUsers = snapshot.documents.mapNotNull { doc ->
					val blockedUserId = doc.id
					val displayName = doc.getString("displayName") ?: ""
					val handle = doc.getString("handle") ?: ""
					val profileImageUrl = doc.getString("profileImageUrl") ?: ""
					val avatarEmoji = doc.getString("avatarEmoji") ?: "🎓"
					Friend(
						userId = blockedUserId,
						displayName = displayName,
						handle = handle,
						profileImageUrl = profileImageUrl,
						avatarEmoji = avatarEmoji,
						isOnline = false
					)
				}

				_uiState.update { it.copy(blockedUsers = blockedUsers) }
			}
	}

	fun unblockUser(blockedUserId: String) {
		currentUserId?.let { userId ->
			viewModelScope.launch {
				try {
					FirebaseFirestore.getInstance()
						.collection("users")
						.document(userId)
						.collection("blockedUsers")
						.document(blockedUserId)
						.delete()
						.await()
				} catch (e: Exception) {
					android.util.Log.e("ProfileViewModel", "Error unblocking user: ${e.message}")
					_uiState.update { it.copy(error = "Failed to unblock user: ${e.message}") }
				}
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		activityListener?.remove()
		notificationSettingsListener?.remove()
		privacySettingsListener?.remove()
		blockedUsersListener?.remove()
		eventEngagementListeners.values.forEach { it.remove() }
		eventEngagementListeners.clear()
	}
}
