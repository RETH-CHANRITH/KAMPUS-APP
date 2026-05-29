package com.example.kampus.ui.admin

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color

data class AppStat(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val trend: String
)

data class AdminUser(
    val uid: String,
    val name: String,
    val major: String,
    val role: String,
    val isBanned: Boolean,
    val avatarInitial: String
)

data class ReportedItem(
    val id: String,
    val type: String,
    val contentId: String,
    val groupId: String = "",
    val reportCreatedAt: Long = 0L,
    val reason: String,
    val reportedBy: String,
    val content: String,
    val contentAuthor: String = "",
    val contentCreatedAt: Long = 0L,
    val contentUpdatedAt: Long = 0L,
    val contentVisibility: String = "",
    val contentLikeCount: Int = 0,
    val contentCommentCount: Int = 0,
    val contentShareCount: Int = 0,
    val contentImageUrl: String = "",
    val contentBody: String = "",
    val status: String = "open"
)
