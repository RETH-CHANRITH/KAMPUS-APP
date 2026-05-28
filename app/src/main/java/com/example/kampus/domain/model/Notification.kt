package com.example.kampus.domain.model

data class AppNotification(
	val id: String,
	val type: String,
	val title: String,
	val body: String,
	val toUserId: String = "",
	val actorUserId: String = "",
	val actorDisplayName: String = "",
	val targetId: String = "",
	val createdAt: Long = 0L,
	val isRead: Boolean = false,
	val actorProfileImageUrl: String = "",
	val actorAvatarEmoji: String = "",
	val postImageUrl: String = "",
)

