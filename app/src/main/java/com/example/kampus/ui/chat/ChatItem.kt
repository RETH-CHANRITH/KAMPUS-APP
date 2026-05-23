package com.example.kampus.ui.chat

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val lastMessageType: String = "message",
    val lastCallType: String = "",
    val lastCallStatus: String = "",
    val lastMessageSenderName: String = "",
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val avatarInitials: String = "",
    val avatarColor: Long = 0xFF3B82F6,
)

data class Message(
    val id: String = "",
    val remoteMessageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaName: String = "",
    val mediaType: String = "",
    val voiceUrl: String = "",
    val voiceDuration: String = "",
    val timestamp: String = "",
    val timestampMillis: Long = 0L,
    val isSentByMe: Boolean = false,
    val isVoice: Boolean = false,
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val isLiveLocation: Boolean = false,
    val locationLatitude: Double = 0.0,
    val locationLongitude: Double = 0.0,
    val locationRemainingSeconds: Long = 0L,
    val hiddenTextFor: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),
    val deliveryState: MessageDeliveryState = MessageDeliveryState.Sending,
)

enum class MessageDeliveryState {
    Sending,
    Delivered,
    Seen,
    Failed
}