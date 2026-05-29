package com.example.kampus.domain.model

import java.time.LocalDateTime

data class User(
    val id: String = "",
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
    val stats: UserStats = UserStats(),
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val role: String = "student",
    val lastActive: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

data class UserStats(
    val posts: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val friendRequests: Int = 0,
)

data class Friend(
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val avatarEmoji: String = "👤",
    val profileImageUrl: String = "",
    val isOnline: Boolean = false,
    val isMutual: Boolean = false,
    val addedAt: LocalDateTime? = null,
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserHandle: String = "",
    val fromUserAvatar: String = "",
    val fromUserProfileImageUrl: String = "",
    val toUserId: String = "",
    val toUserName: String = "",
    val toUserHandle: String = "",
    val toUserAvatar: String = "👤",
    val toUserProfileImageUrl: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: LocalDateTime? = null,
)

enum class FriendRequestStatus {
    PENDING, ACCEPTED, REJECTED, BLOCKED
}
