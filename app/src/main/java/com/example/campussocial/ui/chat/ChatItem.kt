package com.example.campussocial.ui.chat

data class ChatItem(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val avatarInitials: String = "",
    val avatarColor: Long = 0xFF3B82F6,
)

data class Message(
    val id: Int,
    val text: String,
    val timestamp: String,
    val isSentByMe: Boolean,
    val isVoice: Boolean = false,
    val voiceDuration: String = "",
)