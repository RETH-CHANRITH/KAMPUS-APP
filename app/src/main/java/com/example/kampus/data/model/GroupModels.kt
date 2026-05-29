package com.example.kampus.data.model

enum class GroupPrivacy(val label: String) {
    PUBLIC("Public"),
    PRIVATE("Private"),
}

enum class MemberRole(val label: String) {
    ADMIN("Admin"),
    MEMBER("Member"),
}

enum class MembershipStatus(val label: String) {
    NONE("None"),
    PENDING("Pending"),
    MEMBER("Member"),
}

enum class PostReportReason(val label: String) {
    SPAM("Spam"),
    HATE_SPEECH("Hate Speech"),
    VIOLENCE("Violence"),
    NUDITY("Nudity"),
    FALSE_INFO("False Info"),
    OTHER("Other"),
}

data class Group(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val coverImageUrl: String = "",
    val privacy: GroupPrivacy,
    val memberCount: Int,
    val postCount: Int,
    val adminId: String,
    val createdAt: Long,
)

data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String = "",
    val role: MemberRole,
    val status: MembershipStatus,
    val joinedAt: Long,
)

data class JoinRequest(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String = "",
    val requestedAt: Long,
)

data class GroupPost(
    val id: String,
    val groupId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String = "",
    val content: String,
    val imageUrl: String = "",
    val likeCount: Int,
    val commentCount: Int,
    val isLikedByCurrentUser: Boolean,
    val isReported: Boolean,
    val reportCount: Int,
    val createdAt: Long,
)

data class GroupPostComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String = "",
    val content: String,
    val createdAt: Long,
)

data class PostReport(
    val id: String,
    val postId: String,
    val groupId: String,
    val reportedByUserId: String,
    val reason: PostReportReason,
    val additionalNote: String = "",
    val createdAt: Long,
)