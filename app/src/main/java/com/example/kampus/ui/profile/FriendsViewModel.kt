package com.example.kampus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.UserRepositoryImpl
import com.example.kampus.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FriendsUiState(
    val friends: List<FriendItemData> = emptyList(),
    val followers: List<FriendItemData> = emptyList(),
    val following: List<FriendItemData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class FriendItemData(
    val userId: String,
    val name: String,
    val handle: String,
    val mutualFriendsCount: Int,
    val avatarEmoji: String = "👤",
    val profileImageUrl: String = "",
    val isOnline: Boolean = false,
)

class FriendsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepositoryImpl(firestore, auth)
    
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()
    
    private var currentUserId: String? = null
    private var allFriendsIds: Set<String> = emptySet()
    private var friendProfileListeners: List<ListenerRegistration> = emptyList()
    private var followerProfileListeners: List<ListenerRegistration> = emptyList()
    private var followingProfileListeners: List<ListenerRegistration> = emptyList()
    
    init {
        observeFriendsAndFollowers()
    }
    
    private fun observeFriendsAndFollowers() {
        currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _uiState.update { it.copy(error = "User not authenticated") }
            return
        }
        
        _uiState.update { it.copy(isLoading = true) }
        
        // Observe followers
        viewModelScope.launch {
            userRepository.getFollowers(currentUserId!!).collect { result ->
                result.onSuccess { followers ->
                    val followersWithMutuals = followers.map { follower ->
                        FriendItemData(
                            userId = follower.userId,
                            name = follower.displayName,
                            handle = follower.handle,
                            mutualFriendsCount = 0, // Will be calculated
                            avatarEmoji = follower.avatarEmoji,
                            profileImageUrl = follower.profileImageUrl,
                            isOnline = follower.isOnline,
                        )
                    }
                    _uiState.update { state ->
                        val updated = state.copy(followers = followersWithMutuals, isLoading = false)
                        updated.withComputedFriends()
                    }
                    observeFollowerProfiles(followersWithMutuals.map { it.userId })
                    calculateMutualFriends(followersWithMutuals, isFriends = false)
                }
                result.onFailure { error ->
                    _uiState.update { state -> state.copy(error = error.message) }
                }
            }
        }

        // Observe following
        viewModelScope.launch {
            userRepository.getFollowing(currentUserId!!).collect { result ->
                result.onSuccess { following ->
                    val followingWithMutuals = following.map { followee ->
                        FriendItemData(
                            userId = followee.userId,
                            name = followee.displayName,
                            handle = followee.handle,
                            mutualFriendsCount = 0, // Will be calculated
                            avatarEmoji = followee.avatarEmoji,
                            profileImageUrl = followee.profileImageUrl,
                            isOnline = followee.isOnline,
                        )
                    }
                    _uiState.update { state ->
                        val updated = state.copy(following = followingWithMutuals, isLoading = false)
                        updated.withComputedFriends()
                    }
                    observeFollowingProfiles(followingWithMutuals.map { it.userId })
                    calculateMutualFriends(followingWithMutuals, isFollowing = true)
                }
                result.onFailure { error ->
                    _uiState.update { state -> state.copy(error = error.message) }
                }
            }
        }
    }

    private fun observeFriendProfiles(userIds: List<String>) {
        friendProfileListeners.forEach { it.remove() }
        friendProfileListeners = userIds.map { userId ->
            firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val user = snapshot.toUserProfile()
                    _uiState.update { state ->
                        state.copy(
                            friends = state.friends.map { item ->
                                if (item.userId == userId) item.fromUser(user) else item
                            }
                        )
                    }
                }
        }
    }

    private fun observeFollowerProfiles(userIds: List<String>) {
        followerProfileListeners.forEach { it.remove() }
        followerProfileListeners = userIds.map { userId ->
            firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val user = snapshot.toUserProfile()
                    _uiState.update { state ->
                        state.copy(
                            followers = state.followers.map { item ->
                                if (item.userId == userId) item.fromUser(user) else item
                            }
                        ).withComputedFriends()
                    }
                }
        }
    }

    private fun observeFollowingProfiles(userIds: List<String>) {
        followingProfileListeners.forEach { it.remove() }
        followingProfileListeners = userIds.map { userId ->
            firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val user = snapshot.toUserProfile()
                    _uiState.update { state ->
                        state.copy(
                            following = state.following.map { item ->
                                if (item.userId == userId) item.fromUser(user) else item
                            }
                        ).withComputedFriends()
                    }
                }
        }
    }
    
    private fun calculateMutualFriends(items: List<FriendItemData>, isFriends: Boolean = false, isFollowing: Boolean = false) {
        viewModelScope.launch {
            val updatedItems = items.map { item ->
                val mutualCount = getMutualFriendsCount(item.userId)
                item.copy(mutualFriendsCount = mutualCount)
            }
            
            if (isFriends) {
                _uiState.update { state -> state.copy(friends = updatedItems).withComputedFriends() }
            } else if (isFollowing) {
                _uiState.update { state -> state.copy(following = updatedItems) }
            } else {
                _uiState.update { state -> state.copy(followers = updatedItems) }
            }
        }
    }
    
    private suspend fun getMutualFriendsCount(targetUserId: String): Int {
        return try {
            val targetUserFriends = firestore.collection("users")
                .document(targetUserId)
                .collection("friends")
                .get()
                .await()
            
            val targetFriendsIds = targetUserFriends.documents.mapNotNull { it.getString("userId") }.toSet()
            (allFriendsIds intersect targetFriendsIds).size
        } catch (e: Exception) {
            0
        }
    }
    
    fun followBack(followerUserId: String) {
        viewModelScope.launch {
            try {
                if (currentUserId == null) return@launch
                
                val followerData = _uiState.value.followers.find { it.userId == followerUserId } ?: return@launch

                // Follow back: add to current user's following.
                firestore.collection("users")
                    .document(currentUserId!!)
                    .collection("following")
                    .document(followerUserId)
                    .set(mapOf(
                        "userId" to followerUserId,
                        "displayName" to followerData.name,
                        "handle" to followerData.handle,
                        "avatarEmoji" to followerData.avatarEmoji,
                        "profileImageUrl" to followerData.profileImageUrl,
                        "isOnline" to followerData.isOnline,
                        "isMutual" to true,
                        "addedAt" to com.google.firebase.Timestamp.now(),
                    ))
                    .await()

                // Keep follower visible in Followers tab; add to Following tab if not already present.
                _uiState.update { state ->
                    val alreadyFollowing = state.following.any { it.userId == followerUserId }
                    state.copy(
                        following = if (alreadyFollowing) state.following else state.following + followerData,
                    ).withComputedFriends()
                }
                observeFollowingProfiles(_uiState.value.following.map { it.userId })
            } catch (e: Exception) {
                _uiState.update { state -> state.copy(error = e.message) }
            }
        }
    }

    fun unfollow(followingUserId: String) {
        viewModelScope.launch {
            try {
                if (currentUserId == null) return@launch

                // Delete from current user's following
                firestore.collection("users")
                    .document(currentUserId!!)
                    .collection("following")
                    .document(followingUserId)
                    .delete()
                    .await()

                // Best-effort remote cleanup; ignore security-rule failure for cross-user writes.
                runCatching {
                    firestore.collection("users")
                    .document(followingUserId)
                    .collection("followers")
                    .document(currentUserId!!)
                    .delete()
                    .await()
                }

                // Update local state
                _uiState.update { state ->
                    state.copy(
                        following = state.following.filter { it.userId != followingUserId },
                    ).withComputedFriends()
                }
            } catch (e: Exception) {
                _uiState.update { state -> state.copy(error = e.message) }
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            try {
                if (currentUserId == null) return@launch

                val blockedData = mapOf(
                    "userId" to userId,
                    "blockedAt" to com.google.firebase.Timestamp.now(),
                )

                firestore.collection("users")
                    .document(currentUserId!!)
                    .collection("blockedUsers")
                    .document(userId)
                    .set(blockedData)
                    .await()

                firestore.collection("users")
                    .document(currentUserId!!)
                    .collection("following")
                    .document(userId)
                    .delete()
                    .await()

                firestore.collection("users")
                    .document(currentUserId!!)
                    .collection("followers")
                    .document(userId)
                    .delete()
                    .await()

                _uiState.update { state ->
                    state.copy(
                        following = state.following.filter { it.userId != userId },
                        followers = state.followers.filter { it.userId != userId },
                    ).withComputedFriends()
                }
            } catch (e: Exception) {
                _uiState.update { state -> state.copy(error = e.message) }
            }
        }
    }

    private fun FriendsUiState.withComputedFriends(): FriendsUiState {
        val followingMap = following.associateBy { it.userId }
        val mutual = followers.mapNotNull { follower ->
            val followingItem = followingMap[follower.userId] ?: return@mapNotNull null
            followingItem.copy(
                name = if (followingItem.name.isNotEmpty()) followingItem.name else follower.name,
                handle = if (followingItem.handle.isNotEmpty()) followingItem.handle else follower.handle,
                avatarEmoji = if (followingItem.avatarEmoji.isNotEmpty()) followingItem.avatarEmoji else follower.avatarEmoji,
                profileImageUrl = if (followingItem.profileImageUrl.isNotEmpty()) followingItem.profileImageUrl else follower.profileImageUrl,
                isOnline = followingItem.isOnline || follower.isOnline,
            )
        }
        allFriendsIds = mutual.map { it.userId }.toSet()
        return copy(friends = mutual.distinctBy { it.userId })
    }

    private fun FriendItemData.fromUser(user: User): FriendItemData {
        return copy(
            name = user.displayName.ifEmpty { name },
            handle = user.handle.ifEmpty { handle },
            avatarEmoji = user.avatarEmoji.ifEmpty { avatarEmoji },
            profileImageUrl = user.profileImageUrl,
            isOnline = user.isOnline,
        )
    }

    private fun DocumentSnapshot.toUserProfile(): User {
        val statsMap = get("stats") as? Map<*, *>
        val stats = com.example.kampus.domain.model.UserStats(
            posts = (statsMap?.get("posts") as? Number)?.toInt() ?: 0,
            followers = (statsMap?.get("followers") as? Number)?.toInt() ?: 0,
            following = (statsMap?.get("following") as? Number)?.toInt() ?: 0,
            friendRequests = (statsMap?.get("friendRequests") as? Number)?.toInt() ?: 0,
        )

        return User(
            id = id,
            displayName = getString("displayName") ?: "",
            handle = getString("handle") ?: "",
            bio = getString("bio") ?: "",
            email = getString("email") ?: "",
            phone = getString("phone") ?: "",
            faculty = getString("faculty") ?: "",
            year = getString("year") ?: "",
            location = getString("location") ?: "",
            avatarEmoji = getString("avatarEmoji") ?: "👤",
            profileImageUrl = getString("profileImageUrl") ?: "",
            coverImageUrl = getString("coverImageUrl") ?: "",
            stats = stats,
            isVerified = getBoolean("isVerified") ?: false,
            isOnline = getBoolean("isOnline") ?: false,
        )
    }
}
