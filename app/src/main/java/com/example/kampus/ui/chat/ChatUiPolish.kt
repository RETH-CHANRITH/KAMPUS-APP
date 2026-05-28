package com.example.kampus.ui.chat

import com.example.kampus.ui.localization.UiStrings

fun chatPreviewText(chat: ChatItem, strings: UiStrings, currentUserName: String = ""): String {
    val sender = chat.lastMessageSenderName.trim()
    val isGroup = chat.otherUserId.isBlank() || chat.name.contains("group", ignoreCase = true)
    val prefix = if (chat.isLastMessageFromMe) {
        if (currentUserName.isNotBlank()) currentUserName else if (sender.equals("You", ignoreCase = true)) strings.you else sender
    } else {
        if (sender.isBlank() || sender.equals("You", ignoreCase = true)) {
            if (isGroup) "" else chat.name
        } else {
            sender
        }
    }

    if (chat.lastMessageType.equals("call", ignoreCase = true)) {
        val callKind = when {
            chat.lastCallType.equals("video", ignoreCase = true) -> "Video"
            chat.lastCallType.equals("voice", ignoreCase = true) -> "Audio"
            else -> "Call"
        }
        val callState = when {
            chat.lastCallStatus.equals("missed", ignoreCase = true) -> "Missed"
            chat.lastCallStatus.equals("declined", ignoreCase = true) -> "Declined"
            chat.lastCallStatus.equals("ended", ignoreCase = true) -> "Ended"
            chat.lastCallStatus.equals("accepted", ignoreCase = true) -> "Live"
            else -> "Ringing"
        }
        val who = prefix
        return when {
            who.isBlank() -> "$callKind call · $callState"
            callState == "Live" -> "$who started a $callKind call"
            else -> "$who • $callKind call · $callState"
        }
    }

    return if (prefix.isBlank()) chat.lastMessage else "$prefix: ${chat.lastMessage}"
}

