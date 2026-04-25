package com.example.kampus.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.UserRepositoryImpl
import com.example.kampus.domain.model.User
import com.example.kampus.domain.model.Friend
import com.example.kampus.domain.model.FriendRequest
import com.example.kampus.domain.repository.IUserRepository
import com.example.kampus.utils.ActivityLogger
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
	val activities: List<ProfileActivityItem> = emptyList(),
	val isOnline: Boolean = false,
	val isLoading: Boolean = false,
	val error: String? = null,
	val settings: SettingsState = SettingsState(),
)

data class ProfileActivityItem(
	val type: String,
	val text: String,
	val createdAt: Long,
	val eventId: Int? = null,
	val postId: Int? = null,
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

class ProfileViewModel(
	private val userRepository: IUserRepository = UserRepositoryImpl(
		FirebaseFirestore.getInstance(),
		FirebaseAuth.getInstance()
	)
) : ViewModel() {
	private val _uiState = MutableStateFlow(ProfileUiState())
	val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
	private var activityListener: ListenerRegistration? = null

	private val currentUserId: String?
		get() = FirebaseAuth.getInstance().currentUser?.uid

	init {
		initializeRealTimeUpdates()
	}

	private fun initializeRealTimeUpdates() {
		currentUserId?.let { userId ->
			// Don't show loading - data comes from real-time listeners
			
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
									posts = sanitizedUser.stats.posts,
									followers = sanitizedUser.stats.followers,
									following = sanitizedUser.stats.following,
									friendRequests = sanitizedUser.stats.friendRequests,
								),
								isLoading = false,
							)
						}

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
						_uiState.update { it.copy(friends = friends) }
					}
				}
			}

			// Observe followers in real-time
			viewModelScope.launch {
				userRepository.getFollowers(userId).collect { result ->
					result.onSuccess { followers ->
						_uiState.update { it.copy(followers = followers) }
					}
				}
			}

			// Observe following in real-time
			viewModelScope.launch {
				userRepository.getFollowing(userId).collect { result ->
					result.onSuccess { following ->
						_uiState.update { it.copy(following = following) }
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
		}
	}

	private fun observeRecentActivities(userId: String) {
		activityListener?.remove()
		activityListener = FirebaseFirestore.getInstance()
			.collection("users")
			.document(userId)
			.collection("activities")
			.orderBy("createdAt", Query.Direction.DESCENDING)
			.limit(20)
			.addSnapshotListener { snapshot, error ->
				if (error != null) {
					_uiState.update { it.copy(error = error.message ?: "Failed to load activity") }
					return@addSnapshotListener
				}

				val activities = snapshot?.documents
					?.mapNotNull { doc ->
						val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
							?: (doc.getLong("createdAt") ?: 0L)

						ProfileActivityItem(
							type = doc.getString("type") ?: "activity",
							text = doc.getString("text") ?: "Did an activity",
							createdAt = createdAt,
							eventId = doc.getLong("eventId")?.toInt(),
							postId = doc.getLong("postId")?.toInt(),
						)
					}
					?: emptyList()

				_uiState.update { it.copy(activities = activities) }
			}
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
			result.onFailure { error ->
				_uiState.update { it.copy(error = error.message) }
			}
		}
	}

	fun rejectFriendRequest(requestId: String) {
		viewModelScope.launch {
			val result = userRepository.rejectFriendRequest(requestId)
			result.onFailure { error ->
				_uiState.update { it.copy(error = error.message) }
			}
		}
	}

	fun cancelFriendRequest(requestId: String) {
		viewModelScope.launch {
			val result = userRepository.cancelFriendRequest(requestId)
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
				android.util.Log.d("ProfileViewModel", "Sending friend request from $userId to $toUserId")
				val result = userRepository.sendFriendRequest(userId, toUserId)
				result.onSuccess {
					android.util.Log.d("ProfileViewModel", "Friend request sent successfully from $userId to $toUserId")
				}
				result.onFailure { error ->
					android.util.Log.e("ProfileViewModel", "Error sending friend request: ${error.message}")
					_uiState.update { it.copy(error = error.message) }
				}
			}
		}
	}

	fun clearError() {
		_uiState.update { it.copy(error = null) }
	}

	fun logShareProfileActivity() {
		ActivityLogger.logAction(
			type = "share_profile",
			text = "Shared profile",
		)
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

	override fun onCleared() {
		super.onCleared()
		activityListener?.remove()
	}
}
