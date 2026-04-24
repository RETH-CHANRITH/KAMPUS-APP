package com.example.kampus.domain.repository

import com.example.kampus.domain.model.User
import com.example.kampus.domain.model.Friend
import com.example.kampus.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    // Current user profile
    fun getCurrentUserProfile(): Flow<Result<User>>
    fun getUserProfile(userId: String): Flow<Result<User>>
    
    // Stats - Real-time
    fun getUserStats(userId: String): Flow<Result<User>>
    
    // Friends - Real-time
    fun getFriends(userId: String): Flow<Result<List<Friend>>>
    fun getFriendsCount(userId: String): Flow<Result<Int>>
    
    // Followers - Real-time
    fun getFollowers(userId: String): Flow<Result<List<Friend>>>
    fun getFollowersCount(userId: String): Flow<Result<Int>>
    
    // Following - Real-time
    fun getFollowing(userId: String): Flow<Result<List<Friend>>>
    fun getFollowingCount(userId: String): Flow<Result<Int>>
    
    // Friend Requests - Real-time
    fun getFriendRequests(userId: String): Flow<Result<List<FriendRequest>>>
        fun getOutgoingFriendRequests(userId: String): Flow<Result<List<FriendRequest>>>
    fun getFriendRequestsCount(userId: String): Flow<Result<Int>>
    
    // Friend request actions
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>
        suspend fun cancelFriendRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(userId: String, friendId: String): Result<Unit>
    
    // Profile updates
    suspend fun updateProfile(user: User): Result<Unit>
    suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit>
    suspend fun updateCoverImage(userId: String, imageUrl: String): Result<Unit>
    
    // Search
    fun searchUsers(query: String): Flow<Result<List<User>>>
    
    // Online status - Real-time
    fun observeUserOnlineStatus(userId: String): Flow<Result<Boolean>>
}
