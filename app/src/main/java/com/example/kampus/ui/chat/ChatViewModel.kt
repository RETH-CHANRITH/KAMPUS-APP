package com.example.kampus.ui.chat

import androidx.lifecycle.ViewModel
import com.example.kampus.ui.chat.ChatItem
import com.example.kampus.ui.chat.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChatListUiState(
    val chats: List<ChatItem> = emptyList(),
    val searchQuery: String = "",
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val contactName: String = "",
    val isOnline: Boolean = false,
)

class ChatViewModel : ViewModel() {

    private val _chatListState = MutableStateFlow(ChatListUiState())
    val chatListState: StateFlow<ChatListUiState> = _chatListState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    // Sample chats data
    private val sampleChats = listOf(
        ChatItem(1,  "Joanna Evan",      "See you on the next meeting! 😊",            "34 min", 3,  true,  "JE", 0xFF8B5CF6),
        ChatItem(2,  "Lana Smith",       "I'm doing my homework, but need to take...", "1h",     0,  false, "LS", 0xFFEC4899),
        ChatItem(3,  "Marina Martinez",  "I'm watching Friends, what are u doin? 😊",  "1 hour", 1,  true,  "MM", 0xFF10B981),
        ChatItem(4,  "Alex Johnson",     "See you on the next meeting! 😊",            "2 hour", 12, false, "AJ", 0xFFF59E0B),
        ChatItem(5,  "Thomas Friellon",  "Yeup, I'm going to travel in To...",         "3h",     0,  false, "TF", 0xFFEF4444),
        ChatItem(6,  "Jamie Franco",     "I'm watching Friends, what are u doin?",     "4h",     0,  false, "JF", 0xFF06B6D4),
        ChatItem(7,  "Willow Rosenberg", "Really find the subject very interesting 😊","1 hour", 1,  true,  "WR", 0xFF3B82F6),
        ChatItem(8,  "Carlos Mendes",    "Can you send me the notes from today?",      "5h",     0,  false, "CM", 0xFF84CC16),
        ChatItem(9,  "Sophie Turner",    "The party was amazing!! 🎉",                 "Yesterday",0,false, "ST", 0xFFF97316),
    )

    // Sample messages per chat
    private val sampleMessages = mapOf(
        1 to listOf(
            Message(1, "Hi! How are you doing? 😊", "10:52", false),
            Message(2, "I'm doing great, thanks for asking!", "10:55", true),
            Message(3, "Are you coming to the meeting tomorrow?", "11:00", false),
            Message(4, "Yes of course! See you on the next meeting! 😊", "11:02", false),
            Message(5, "Perfect! I'll prepare the slides tonight.", "11:10", true),
            Message(6, "Sounds good, talk later 👋", "11:15", false),
        ),
        3 to listOf(
            Message(1, "Hey! What's up?", "09:30", true),
            Message(2, "I'm watching Friends, what are u doin? 😊", "09:35", false),
            Message(3, "Nothing much, just relaxing at home", "09:40", true),
            Message(4, "That sounds nice! Which episode?", "09:42", false),
            Message(5, "Season 5, The One in Vegas 😂", "09:45", true),
        ),
    )

    init {
        _chatListState.update { it.copy(chats = sampleChats) }
    }

    fun onSearchQueryChange(query: String) {
        _chatListState.update { state ->
            state.copy(
                searchQuery = query,
                chats = if (query.isBlank()) sampleChats
                else sampleChats.filter { it.name.contains(query, ignoreCase = true) }
            )
        }
    }

    fun openChat(chatId: Int) {
        val chat = sampleChats.firstOrNull { it.id == chatId }
        val messages = sampleMessages[chatId] ?: listOf(
            Message(1, "Hey! 👋", "10:00", false),
            Message(2, "Hi there! How can I help?", "10:02", true),
        )
        _chatState.update {
            it.copy(
                messages = messages,
                contactName = chat?.name ?: "",
                isOnline = chat?.isOnline ?: false,
            )
        }
    }

    fun onInputChange(text: String) {
        _chatState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _chatState.value.inputText.trim()
        if (text.isBlank()) return
        val newMsg = Message(
            id          = (_chatState.value.messages.size + 1),
            text        = text,
            timestamp   = "Now",
            isSentByMe  = true,
        )
        _chatState.update {
            it.copy(
                messages  = it.messages + newMsg,
                inputText = "",
            )
        }
    }
}