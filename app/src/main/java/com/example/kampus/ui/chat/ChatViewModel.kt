package com.example.kampus.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.CallStatus
import com.example.kampus.data.repository.ChatRepositoryImpl
import com.example.kampus.data.repository.Message as RepositoryMessage
import com.example.kampus.ui.chat.ChatItem
import com.example.kampus.ui.chat.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatListUiState(
    val chats: List<ChatItem> = emptyList(),
    val searchQuery: String = "",
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val contactName: String = "",
    val isOnline: Boolean = false,
    val contactAvatarEmoji: String = "👤",
    val contactProfileImageUrl: String = "",
)

class ChatViewModel : ViewModel() {

    private val _chatListState = MutableStateFlow(ChatListUiState())
    val chatListState: StateFlow<ChatListUiState> = _chatListState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val chatRepository = ChatRepositoryImpl(FirebaseFirestore.getInstance())
    private val auth = FirebaseAuth.getInstance()
    private var currentChatId: String? = null

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.getChats().collect { result ->
                result.onSuccess { chats ->
                    _chatListState.update { it.copy(chats = chats) }
                }
                result.onFailure { error ->
                    // Handle error
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _chatListState.update { state ->
            val filteredChats = if (query.isBlank()) {
                state.chats
            } else {
                state.chats.filter { it.name.contains(query, ignoreCase = true) }
            }
            state.copy(searchQuery = query, chats = filteredChats)
        }
    }

    fun openChat(chatId: String) {
        val chat = _chatListState.value.chats.firstOrNull { it.id == chatId }
        currentChatId = chatId
        
        _chatState.update {
            it.copy(
                messages = emptyList(),
                contactName = chat?.name ?: "",
                isOnline = chat?.isOnline ?: false,
                contactAvatarEmoji = chat?.avatarInitials ?: "👤",
            )
        }

        if (chat == null) {
            viewModelScope.launch {
                chatRepository.getChatById(chatId).collect { result ->
                    result.onSuccess { fetched ->
                        fetched?.let { chatItem ->
                            _chatState.update { state ->
                                state.copy(
                                    contactName = chatItem.name,
                                    isOnline = chatItem.isOnline,
                                    contactAvatarEmoji = chatItem.avatarInitials,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Load messages for this chat
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { result ->
                result.onSuccess { messages ->
                    val messageItems = messages.map { msg ->
                        Message(
                            id = msg.id.hashCode(),
                            text = msg.text,
                            timestamp = formatTimestamp(msg.timestamp),
                            isSentByMe = msg.senderId == auth.currentUser?.uid
                        )
                    }
                    _chatState.update { state ->
                        state.copy(messages = messageItems)
                    }
                }
            }
        }
    }

    suspend fun getOrCreateDirectChatWithUser(otherUserId: String): String? {
        val currentUserId = auth.currentUser?.uid ?: return null
        return chatRepository.getOrCreateDirectChat(currentUserId, otherUserId).getOrNull()
    }

    fun observeCallStatus(chatId: String, callId: String): Flow<Result<CallStatus>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("calls")
            .document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(Result.success(CallStatus.RINGING))
                    return@addSnapshotListener
                }

                val status = when (snapshot.getString("status")?.uppercase()) {
                    "ACCEPTED" -> CallStatus.ACCEPTED
                    "DECLINED" -> CallStatus.DECLINED
                    "MISSED" -> CallStatus.MISSED
                    "ENDED" -> CallStatus.ENDED
                    else -> CallStatus.RINGING
                }
                trySend(Result.success(status))
            }

        awaitClose { listener.remove() }
    }

    fun endCall(chatId: String, callId: String) {
        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .collection("calls")
                    .document(callId)
                    .update("status", CallStatus.ENDED.name)
                    .await()
            }
        }
    }

    suspend fun createStory(
        note: String,
        imageUri: String? = null,
        overlayText: String = "",
        overlayX: Float = 0f,
        overlayY: Float = 0f,
        overlayColor: Long = 0L,
        privacy: String = "friends",
        storyType: String = "note",
    ): Result<String> = try {
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not signed in"))
        val storyData = mutableMapOf<String, Any>(
            "userId" to userId,
            "note" to note,
            "overlayText" to overlayText,
            "overlayX" to overlayX,
            "overlayY" to overlayY,
            "overlayColor" to overlayColor,
            "privacy" to privacy,
            "storyType" to storyType,
            "createdAt" to System.currentTimeMillis(),
        )
        if (!imageUri.isNullOrBlank()) {
            storyData["imageUri"] = imageUri
        }
        val ref = FirebaseFirestore.getInstance()
            .collection("stories")
            .add(storyData)
            .await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun saveStoryDraft(
        note: String,
        overlayText: String = "",
        overlayX: Float = 0f,
        overlayY: Float = 0f,
        overlayColor: Long = 0L,
    ): Result<Unit> = try {
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not signed in"))
        FirebaseFirestore.getInstance()
            .collection("storyDrafts")
            .document(userId)
            .set(
                mapOf(
                    "note" to note,
                    "overlayText" to overlayText,
                    "overlayX" to overlayX,
                    "overlayY" to overlayY,
                    "overlayColor" to overlayColor,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun onInputChange(text: String) {
        _chatState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _chatState.value.inputText.trim()
        if (text.isBlank() || currentChatId == null) return

        val senderId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        // Create repository message
        val repositoryMessage = RepositoryMessage(
            id = "",
            senderId = senderId,
            text = text,
            timestamp = timestamp,
            isRead = false
        )

        // Send to Firestore
        viewModelScope.launch {
            currentChatId?.let { chatId ->
                chatRepository.sendMessage(chatId, repositoryMessage)
                    .onSuccess {
                        // Message saved to Firestore
                        _chatState.update { it.copy(inputText = "") }
                    }
                    .onFailure { error ->
                        // Handle error
                    }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return when {
            timestamp == 0L -> "now"
            else -> {
                val now = System.currentTimeMillis()
                val diffMillis = now - timestamp
                val diffMinutes = diffMillis / (1000 * 60)
                val diffHours = diffMinutes / 60
                val diffDays = diffHours / 24

                when {
                    diffMinutes < 1 -> "now"
                    diffMinutes < 60 -> "${diffMinutes}m ago"
                    diffHours < 24 -> "${diffHours}h ago"
                    diffDays < 7 -> "${diffDays}d ago"
                    else -> {
                        val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(timestamp))
                    }
                }
            }
        }
    }
}