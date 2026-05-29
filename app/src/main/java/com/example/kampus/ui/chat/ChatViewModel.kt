package com.example.kampus.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.di.SupabaseModule
import com.example.kampus.data.repository.StoryRepository
import com.example.kampus.data.repository.CallStatus
import com.example.kampus.data.repository.ChatRepositoryImpl
import com.example.kampus.data.repository.Message as RepositoryMessage
import com.example.kampus.ui.chat.ChatItem
import com.example.kampus.ui.chat.Message
import com.example.kampus.utils.E2EEConfig
import com.example.kampus.utils.E2EEManager
import com.example.kampus.utils.EncryptedChatMessage
import com.example.kampus.utils.NotificationLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.io.File

data class ChatListUiState(
    val allChats: List<ChatItem> = emptyList(),
    val chats: List<ChatItem> = emptyList(),
    val searchQuery: String = "",
    val stories: List<ChatStory> = emptyList(),
    val currentUserProfileImageUrl: String = "",
    val currentUserAvatarEmoji: String = "👤",
    val currentUserName: String = "",
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val contactName: String = "",
    val contactUserId: String = "",
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val contactAvatarEmoji: String = "👤",
    val contactProfileImageUrl: String = "",
    val voiceUploadState: VoiceUploadState = VoiceUploadState.Idle,
)

sealed interface VoiceUploadState {
    data object Idle : VoiceUploadState
    data class Uploading(val filePath: String, val duration: String) : VoiceUploadState
    data class Failed(val filePath: String, val duration: String, val error: String) : VoiceUploadState
}

data class IncomingCallInvite(
    val chatId: String = "",
    val callType: String = "voice",
    val callId: String = "",
)

class ChatViewModel : ViewModel() {

    private val _chatListState = MutableStateFlow(ChatListUiState())
    val chatListState: StateFlow<ChatListUiState> = _chatListState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _chatPreviewState = MutableStateFlow(ChatPreviewUiState())
    val chatPreviewState: StateFlow<ChatPreviewUiState> = _chatPreviewState.asStateFlow()

    private val _incomingCallState = MutableStateFlow<IncomingCallInvite?>(null)
    val incomingCallState: StateFlow<IncomingCallInvite?> = _incomingCallState.asStateFlow()

    private val _storyUploadProgress = MutableStateFlow<Double?>(null)
    val storyUploadProgress: StateFlow<Double?> = _storyUploadProgress.asStateFlow()

    private val chatRepository = ChatRepositoryImpl(FirebaseFirestore.getInstance())
    private val auth = FirebaseAuth.getInstance()
    private var currentChatId: String? = null
    private var storiesListener: ListenerRegistration? = null
    private var incomingCallsListener: ListenerRegistration? = null
    private var contactUserListener: ListenerRegistration? = null
    private var contactPresenceListener: ListenerRegistration? = null
    private var chatTypingListener: ListenerRegistration? = null
    private var previewMessagesJob: Job? = null
    private var messagesJob: Job? = null
    private val _targetMessageId = MutableStateFlow<String?>(null)
    val targetMessageId = _targetMessageId.asStateFlow()
    private var typingDebounceJob: Job? = null
    private var selfTypingActive: Boolean = false
    private var chatLoadJob: Job? = null
    private var authStateListener: AuthStateListener? = null
    private val chatListUserListeners = mutableMapOf<String, ListenerRegistration>()
    private val chatListPresenceListeners = mutableMapOf<String, ListenerRegistration>()
    private val pendingOutgoingMessages = mutableMapOf<String, Message>()

    init {
        observeAuthState()
        loadChats()
        observeStories()
        loadCurrentUserStoryProfile()
    }

    private fun loadChats() {
        chatLoadJob?.cancel()
        chatLoadJob = viewModelScope.launch {
            chatRepository.getChats().collect { result ->
                result.onSuccess { chats ->
                    _chatListState.update { state ->
                        val filtered = filterChats(chats, state.searchQuery)
                        state.copy(allChats = chats, chats = filtered)
                    }
                    syncChatListRealtimeListeners(
                        chats.mapNotNull { it.otherUserId.takeIf(String::isNotBlank) }.toSet(),
                    )
                }
                result.onFailure { _ ->
                    // Handle error
                }
            }
        }
    }

    private fun observeAuthState() {
        authStateListener?.let(auth::removeAuthStateListener)
        authStateListener = AuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId.isNullOrBlank()) {
                clearAccountScopedState()
                return@AuthStateListener
            }
            refreshForActiveUser()
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    private fun refreshForActiveUser() {
        val activeChatId = currentChatId
        clearAccountScopedState(keepStoryCache = false)
        loadChats()
        observeStories()
        loadCurrentUserStoryProfile()
        startIncomingCallListener()
        activeChatId?.let { openChat(it) }
    }

    private fun clearAccountScopedState(keepStoryCache: Boolean = false) {
        currentChatId = null
        typingDebounceJob?.cancel()
        typingDebounceJob = null
        previewMessagesJob?.cancel()
        previewMessagesJob = null
        messagesJob?.cancel()
        messagesJob = null
        selfTypingActive = false
        chatTypingListener?.remove()
        chatTypingListener = null
        contactUserListener?.remove()
        contactUserListener = null
        contactPresenceListener?.remove()
        contactPresenceListener = null
        incomingCallsListener?.remove()
        incomingCallsListener = null
        chatListUserListeners.values.forEach { it.remove() }
        chatListUserListeners.clear()
        chatListPresenceListeners.values.forEach { it.remove() }
        chatListPresenceListeners.clear()
        pendingOutgoingMessages.clear()
        _chatState.value = ChatUiState()
        _chatPreviewState.value = ChatPreviewUiState()
        _incomingCallState.value = null
        _chatListState.update { state ->
            state.copy(
                allChats = emptyList(),
                chats = emptyList(),
                currentUserProfileImageUrl = if (keepStoryCache) state.currentUserProfileImageUrl else "",
                currentUserAvatarEmoji = if (keepStoryCache) state.currentUserAvatarEmoji else "👤",
                stories = if (keepStoryCache) state.stories else emptyList(),
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _chatListState.update { state ->
            state.copy(searchQuery = query, chats = filterChats(state.allChats, query))
        }
    }

    fun openChat(chatId: String) {
        val previousChatId = currentChatId
        if (!previousChatId.isNullOrBlank() && previousChatId != chatId) {
            viewModelScope.launch { setTypingState(previousChatId, false) }
        }
        typingDebounceJob?.cancel()
        selfTypingActive = false
        messagesJob?.cancel()
        pendingOutgoingMessages.clear()

        val chat = _chatListState.value.chats.firstOrNull { it.id == chatId }
        currentChatId = chatId
        
        _chatState.update {
            it.copy(
                messages = emptyList(),
                contactName = chat?.name ?: "",
                contactUserId = chat?.otherUserId ?: "",
                isOnline = chat?.isOnline ?: false,
                contactAvatarEmoji = chat?.avatarInitials ?: "👤",
                contactProfileImageUrl = chat?.profileImageUrl ?: "",
                isTyping = false,
            )
        }

        chat?.otherUserId?.takeIf { it.isNotBlank() }?.let { observeContactProfile(it) }
        observeChatTyping(chatId)

        if (chat == null) {
            viewModelScope.launch {
                chatRepository.getChatById(chatId).collect { result ->
                    result.onSuccess { fetched ->
                        fetched?.let { chatItem ->
                            if (chatItem.otherUserId.isNotBlank()) {
                                observeContactProfile(chatItem.otherUserId)
                            }
                            _chatState.update { state ->
                                state.copy(
                                    contactName = chatItem.name,
                                    contactUserId = chatItem.otherUserId,
                                    isOnline = chatItem.isOnline,
                                    contactAvatarEmoji = chatItem.avatarInitials,
                                    contactProfileImageUrl = chatItem.profileImageUrl,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Load messages for this chat
        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { result ->
                result.onSuccess { messages ->
                    val currentUserId = auth.currentUser?.uid.orEmpty()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        // Warm up the chat secret cache once so that all decryptMessageSafely calls hit the memory cache
                        E2EEManager.getSharedChatSecret(chatId, currentUserId)
                        
                        val messageItems = messages.map { msg ->
                            val decryptedText = decryptMessageForUi(
                                chatId = chatId,
                                currentUserId = currentUserId,
                                repositoryMessage = msg,
                            )
                            Message(
                                id = msg.id,
                                remoteMessageId = msg.remoteMessageId.ifBlank { msg.id },
                                senderId = msg.senderId,
                                receiverId = msg.receiverId,
                                text = decryptedText,
                                encryptedPayload = msg.encryptedPayload,
                                iv = msg.iv,
                                encryptedKeyForSender = msg.encryptedKeyForSender,
                                encryptedKeyForRecipient = msg.encryptedKeyForRecipient,
                                encryptedKeyForSenderSha1 = msg.encryptedKeyForSenderSha1,
                                encryptedKeyForRecipientSha1 = msg.encryptedKeyForRecipientSha1,
                                isEncrypted = msg.isEncrypted,
                                mediaUrl = msg.mediaUrl,
                                mediaName = msg.mediaName,
                                mediaType = msg.mediaType,
                                voiceUrl = msg.voiceUrl,
                                voiceDuration = msg.voiceDuration,
                                storyId = msg.storyId,
                                storyImage = msg.storyImage,
                                storyCaption = msg.storyCaption,
                                storyReplyText = msg.storyReplyText.ifBlank { msg.text },
                                storyOwnerId = msg.storyOwnerId,
                                storyOwnerName = msg.storyOwnerName,
                                storyThumbnail = msg.storyThumbnail,
                                isStoryReply = msg.messageType.equals("story_reply", ignoreCase = true),
                                timestamp = formatTimestamp(msg.timestamp),
                                timestampMillis = msg.timestamp,
                                isSentByMe = msg.senderId == currentUserId,
                                isVoice = msg.voiceUrl.isNotBlank() || msg.voiceDuration.isNotBlank(),
                                isCallInvite = msg.messageType.equals("call_invite", ignoreCase = true),
                                callType = msg.callType,
                                callStatus = msg.callStatus,
                                isRead = msg.isRead || msg.seen,
                                isEdited = msg.isEdited,
                                isLiveLocation = msg.isLiveLocation,
                                locationLatitude = msg.locationLatitude,
                                locationLongitude = msg.locationLongitude,
                                locationRemainingSeconds = msg.locationRemainingSeconds,
                                hiddenTextFor = msg.hiddenTextFor,
                                deletedForUsers = msg.deletedForUsers,
                                reactions = msg.reactions,
                                seen = msg.isRead || msg.seen,
                                deliveryState = if (msg.isRead || msg.seen) MessageDeliveryState.Seen else MessageDeliveryState.Delivered,
                            )
                        }
                        val mergedMessages = mergeMessagesWithPending(messageItems)
                        markIncomingMessagesAsSeen(chatId, messageItems)
                        _chatState.update { state ->
                            state.copy(messages = mergedMessages)
                        }
                    }
                }
            }
        }
    }

    /**
     * Open a chat and focus a specific message id once messages are loaded.
     */
    fun openChatAndFocusMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            openChat(chatId)
            // Delay a bit to allow messages job to start and load
            delay(300)
            _targetMessageId.value = messageId
            // Clear it after a short time so it can be re-used
            delay(5000)
            _targetMessageId.value = null
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
        context: Context,
        note: String,
        imageUri: String? = null,
        overlayText: String = "",
        overlayX: Float = 0f,
        overlayY: Float = 0f,
        overlayColor: Long = 0L,
        privacy: String = "friends",
        storyType: String = "note",
    ): Result<String> {
        val hasMedia = !imageUri.isNullOrBlank() && (storyType == "image" || storyType == "video" || imageUri.startsWith("content:") || imageUri.startsWith("file:"))
        val res = if (hasMedia) {
            _storyUploadProgress.value = 0.0
            val uploadRes = StoryRepository().uploadStory(
                context = context,
                fileUri = Uri.parse(imageUri),
                caption = note,
                overlayText = overlayText,
                overlayX = overlayX,
                overlayY = overlayY,
                overlayColor = overlayColor,
                privacy = privacy,
                onProgress = { p -> _storyUploadProgress.value = p }
            )
            _storyUploadProgress.value = null
            uploadRes
        } else {
            runCatching {
                val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in")
                val now = System.currentTimeMillis()
                val expiresAt = now + 86_400_000L
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()

                val storyData = mutableMapOf<String, Any>(
                    "ownerId" to userId,
                    "userId" to userId,
                    "note" to note,
                    "ownerName" to (userDoc.getString("displayName")?.ifBlank { null }
                        ?: auth.currentUser?.displayName?.ifBlank { null }
                        ?: auth.currentUser?.email?.substringBefore("@")
                        ?: "User"),
                    "ownerAvatarEmoji" to (userDoc.getString("avatarEmoji")?.ifBlank { null } ?: "👤"),
                    "ownerProfileImageUrl" to (userDoc.getString("profileImageUrl")?.ifBlank { null } ?: ""),
                    "ownerAvatarColor" to ((userDoc.get("avatarColor") as? Number)?.toLong() ?: 0xFF3B82F6),
                    "overlayText" to overlayText,
                    "overlayX" to overlayX,
                    "overlayY" to overlayY,
                    "overlayColor" to overlayColor,
                    "privacy" to privacy,
                    "storyType" to storyType,
                    "createdAt" to now,
                    "expiresAt" to expiresAt,
                )
                FirebaseFirestore.getInstance()
                    .collection("stories")
                    .add(storyData)
                    .await()
                    .id
            }
        }

        res.onSuccess { storyId ->
            val userId = auth.currentUser?.uid
            if (!userId.isNullOrBlank()) {
                viewModelScope.launch {
                    runCatching {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .get()
                            .await()
                        val senderName = userDoc.getString("displayName") ?: auth.currentUser?.displayName ?: "Someone"

                        val followersSnapshot = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("followers")
                            .get()
                            .await()
                        for (doc in followersSnapshot.documents) {
                            val followerId = doc.id
                            if (followerId.isNotBlank() && followerId != userId) {
                                runCatching {
                                    NotificationLogger.notifyUser(
                                        toUserId = followerId,
                                        type = "story",
                                        title = "New Story",
                                        body = "$senderName added a new story",
                                        targetId = storyId,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        return res
    }

    suspend fun saveStoryDraft(
        note: String,
        overlayText: String = "",
        overlayX: Float = 0f,
        overlayY: Float = 0f,
        overlayColor: Long = 0L,
    ): Result<Unit> = runCatching {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in")
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
        Unit
    }

    suspend fun createStoryReply(story: ChatStory, replyText: String): Result<String> = runCatching {
        val senderId = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in")
        val receiverId = story.ownerId
        val trimmedReply = replyText.trim()
        if (trimmedReply.isBlank()) {
            throw IllegalArgumentException("Reply cannot be blank")
        }
        if (story.id.isBlank() || story.ownerId.isBlank()) {
            throw IllegalArgumentException("Story context is missing")
        }

        val replyId = FirebaseFirestore.getInstance().collection("storyReplies").document().id
        val userDoc = FirebaseFirestore.getInstance()
            .collection("users")
            .document(senderId)
            .get()
            .await()

        val senderName = userDoc.getString("displayName") ?: auth.currentUser?.displayName ?: "User"

        val payload = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "storyOwnerId" to receiverId,
            "storyId" to story.id,
            "storyImage" to story.imageUrl,
            "storyThumbnail" to story.imageUrl,
            "storyCaption" to story.note,
            "message" to trimmedReply,
            "replyText" to trimmedReply,
            "mediaPreview" to story.imageUrl,
            "storyOwnerName" to story.ownerName,
            "senderName" to senderName,
            "createdAt" to System.currentTimeMillis(),
            "replyId" to replyId,
        )

        FirebaseFirestore.getInstance()
            .collection("storyReplies")
            .document(replyId)
            .set(payload)
            .await()

        val chatId = getOrCreateDirectChatWithUser(receiverId) ?: throw IllegalStateException("Cannot resolve chat")
        val now = System.currentTimeMillis()
        val clientMessageId = "local-$now"
        val repositoryMessage = RepositoryMessage(
            id = clientMessageId,
            remoteMessageId = clientMessageId,
            senderId = senderId,
            receiverId = receiverId,
            text = trimmedReply,
            messageType = "story_reply",
            storyId = story.id,
            storyImage = story.imageUrl,
            storyCaption = story.note,
            storyReplyText = trimmedReply,
            storyOwnerId = story.ownerId,
            storyOwnerName = story.ownerName,
            storyThumbnail = story.imageUrl,
            timestamp = now,
            isRead = false,
            seen = false,
        )

        chatRepository.sendMessage(chatId, repositoryMessage).getOrThrow()

        runCatching {
            NotificationLogger.notifyUser(
                toUserId = receiverId,
                type = "story_reply",
                title = senderName,
                body = "$senderName replied to your story: \"$trimmedReply\"",
                targetId = chatId,
            )
        }

        replyId
    }

    fun onInputChange(text: String) {
        _chatState.update { it.copy(inputText = text) }

        val chatId = currentChatId ?: return
        val shouldShowTyping = text.isNotBlank()

        typingDebounceJob?.cancel()
        if (!shouldShowTyping) {
            viewModelScope.launch { setTypingState(chatId, false) }
            return
        }

        viewModelScope.launch { setTypingState(chatId, true) }
        typingDebounceJob = viewModelScope.launch {
            delay(900)
            setTypingState(chatId, false)
        }
    }

    fun sendMessage() {
        val text = _chatState.value.inputText.trim()
        if (text.isBlank()) return

        val senderId = auth.currentUser?.uid ?: return
        val chatId = currentChatId ?: return
        val draftText = _chatState.value.inputText
        val clientMessageId = "local-${System.currentTimeMillis()}"
        val pendingMessage = createPendingOutgoingMessage(
            messageId = clientMessageId,
            senderId = senderId,
            text = text,
            isEncrypted = E2EEConfig.isE2EEEnabled(),
            timestampMillis = System.currentTimeMillis(),
        )
        enqueuePendingOutgoingMessage(pendingMessage)
        typingDebounceJob?.cancel()
        _chatState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            setTypingState(chatId, false)
            val repositoryMessage = buildOutgoingMessage(chatId, senderId, text, clientMessageId)

            chatRepository.sendMessage(chatId, repositoryMessage)
                .onSuccess { savedMessageId ->
                    if (repositoryMessage.isEncrypted) {
                        E2EEManager.storeLocalPlaintext(savedMessageId, text)
                    }
                    removePendingOutgoingMessage(savedMessageId)
                    runCatching {
                        val recipientId = resolveOtherUserId(chatId)
                        NotificationLogger.notifyUser(
                            toUserId = recipientId,
                            type = "chat_message",
                            title = _chatState.value.contactName.ifBlank { "New message" },
                            body = text,
                            targetId = chatId,
                        )
                    }
                }
                .onFailure {
                    // keep current UI text so user can retry
                    markPendingOutgoingMessageFailed(clientMessageId)
                    _chatState.update { state ->
                        if (state.inputText.isBlank()) state.copy(inputText = draftText) else state
                    }
                }
        }
    }

    fun sendLiveLocationMessage(latitude: Double, longitude: Double, label: String) {
        sendRichMessage(
            text = "📍 $label",
            mediaType = "location",
            messageType = "live_location",
            isLiveLocation = true,
            latitude = latitude,
            longitude = longitude,
            locationRemainingSeconds = 15 * 60L,
        )
    }

    fun sendAttachmentMessage(
        mediaUri: Uri,
        attachmentType: String,
        mediaName: String,
        mimeType: String? = null,
    ) {
        val chatId = currentChatId ?: return
        val userId = auth.currentUser?.uid ?: return
        val type = attachmentType.lowercase()

        viewModelScope.launch {
            val uploadResult = runCatching {
                SupabaseModule.getStorageManager().uploadChatAttachment(
                    userId = userId,
                    chatId = chatId,
                    mediaUri = mediaUri,
                    mimeType = mimeType,
                    attachmentType = type,
                )
            }.getOrElse { error ->
                Result.failure(error)
            }

            uploadResult
                .onSuccess { remoteUrl ->
                    val mediaType = when (type) {
                        "image" -> mimeType ?: "image/jpeg"
                        "video" -> mimeType ?: "video/mp4"
                        "audio" -> mimeType ?: "audio/*"
                        else -> mimeType ?: "application/octet-stream"
                    }

                    val previewText = when (type) {
                        "image" -> "📷 Photo"
                        "video" -> "🎬 Video"
                        "audio" -> "🎵 Audio"
                        else -> "📎 ${mediaName.ifBlank { "File" }}"
                    }

                    sendRichMessage(
                        text = previewText,
                        mediaType = mediaType,
                        mediaUrl = remoteUrl,
                        mediaName = mediaName,
                    )
                }
                .onFailure { error ->
                    Log.e("ChatViewModel", "Attachment upload failed: ${error.message}", error)
                }
        }
    }

    fun sendVoiceMessage(file: File, duration: String) {
        val chatId = currentChatId ?: return
        val userId = auth.currentUser?.uid ?: return

        _chatState.update { it.copy(voiceUploadState = VoiceUploadState.Uploading(file.absolutePath, duration)) }

        viewModelScope.launch {
            val uploadResult = runCatching {
                SupabaseModule.getStorageManager().uploadChatVoiceNote(userId, chatId, file)
            }.getOrElse { error ->
                Result.failure(error)
            }

            uploadResult
                .onSuccess { remoteUrl ->
                    sendRichMessage(
                        text = "🎙️ Voice message",
                        mediaType = "audio/m4a",
                        mediaUrl = remoteUrl,
                        voiceUrl = remoteUrl,
                        mediaName = file.name,
                        voiceDuration = duration,
                    )
                    _chatState.update { state ->
                        when (val uploadState = state.voiceUploadState) {
                            is VoiceUploadState.Uploading -> if (uploadState.filePath == file.absolutePath) {
                                state.copy(voiceUploadState = VoiceUploadState.Idle)
                            } else state
                            else -> state
                        }
                    }
                    runCatching { file.delete() }
                }
                .onFailure { error ->
                    _chatState.update {
                        it.copy(
                            voiceUploadState = VoiceUploadState.Failed(
                                filePath = file.absolutePath,
                                duration = duration,
                                error = error.message ?: "Voice upload failed",
                            )
                        )
                    }
                    Log.e("VoiceRecording", "Voice upload failed: ${error.message}", error)
                }
        }
    }

    fun retryVoiceUpload() {
        val uploadState = _chatState.value.voiceUploadState
        if (uploadState is VoiceUploadState.Failed) {
            val file = File(uploadState.filePath)
            if (file.exists()) {
                sendVoiceMessage(file, uploadState.duration)
            }
        }
    }

    suspend fun deleteMessage(message: Message): Result<Unit> {
        val chatId = currentChatId ?: return Result.failure(IllegalStateException("No active chat"))
        val messageId = message.id.ifBlank { message.remoteMessageId }
        if (messageId.isBlank()) return Result.failure(IllegalArgumentException("Message id missing"))
        return runCatching {
            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun deleteMessageForMe(message: Message): Result<Unit> {
        val chatId = currentChatId ?: return Result.failure(IllegalStateException("No active chat"))
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val messageId = message.id.ifBlank { message.remoteMessageId }
        if (messageId.isBlank()) return Result.failure(IllegalArgumentException("Message id missing"))
        val updateTime = System.currentTimeMillis()
        return runCatching {
            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "deletedForUsers" to FieldValue.arrayUnion(userId),
                        "updatedAt" to updateTime
                    )
                )
                .await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun editMessage(message: Message, newValue: String): Result<Unit> {
        val chatId = currentChatId ?: return Result.failure(IllegalStateException("No active chat"))
        val messageId = message.id.ifBlank { message.remoteMessageId }
        if (messageId.isBlank()) return Result.failure(IllegalArgumentException("Message id missing"))
        val updateTime = System.currentTimeMillis()
        return runCatching {
            val updatePayload = if (message.isEncrypted && message.encryptedPayload.isNotBlank()) {
                val recipientId = resolveOtherUserId(chatId)
                if (!E2EEConfig.isE2EEEnabled() || recipientId.isBlank()) {
                    mapOf(
                        "text" to newValue,
                        "isEncrypted" to false,
                        "encryptedPayload" to "",
                        "iv" to "",
                        "encryptedKeyForSender" to "",
                        "encryptedKeyForRecipient" to "",
                        "encryptedKeyForSenderSha1" to "",
                        "encryptedKeyForRecipientSha1" to "",
                        "editedAt" to updateTime,
                        "editedBy" to (auth.currentUser?.uid ?: ""),
                        "updatedAt" to updateTime,
                    )
                } else {
                    val encrypted = E2EEManager.encryptMessageForSending(
                        chatId = chatId,
                        senderId = auth.currentUser?.uid.orEmpty(),
                        recipientId = recipientId,
                        plaintext = newValue,
                    ).getOrThrow()

                    E2EEManager.storeLocalPlaintext(messageId, newValue)

                    mapOf(
                        "text" to newValue,
                        "isEncrypted" to true,
                        "encryptedPayload" to encrypted.encryptedPayload,
                        "iv" to encrypted.iv,
                        "encryptedKeyForSender" to encrypted.encryptedKeyForSender,
                        "encryptedKeyForRecipient" to encrypted.encryptedKeyForRecipient,
                        "encryptedKeyForSenderSha1" to encrypted.encryptedKeyForSenderSha1,
                        "encryptedKeyForRecipientSha1" to encrypted.encryptedKeyForRecipientSha1,
                        "editedAt" to updateTime,
                        "editedBy" to (auth.currentUser?.uid ?: ""),
                        "updatedAt" to updateTime,
                    )
                }
            } else {
                mapOf(
                    "text" to newValue,
                    "editedAt" to updateTime,
                    "editedBy" to (auth.currentUser?.uid ?: ""),
                    "updatedAt" to updateTime,
                )
            }

            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(updatePayload)
                .await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) },
        )
    }

    fun createTestCallHistory(chatId: String) {
        val senderId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val samples = listOf(
                mapOf(
                    "remoteMessageId" to "test-call-${now}-1",
                    "senderId" to senderId,
                    "text" to "Call ended",
                    "messageType" to "call_invite",
                    "callType" to "voice",
                    "callStatus" to "ended",
                    "voiceDuration" to "2:31",
                    "timestamp" to now - 120000L,
                    "isRead" to true,
                ),
                mapOf(
                    "remoteMessageId" to "test-call-${now}-2",
                    "senderId" to senderId,
                    "text" to "Missed call",
                    "messageType" to "call_invite",
                    "callType" to "video",
                    "callStatus" to "missed",
                    "timestamp" to now - 90000L,
                    "isRead" to true,
                ),
                mapOf(
                    "remoteMessageId" to "test-call-${now}-3",
                    "senderId" to senderId,
                    "text" to "Declined call",
                    "messageType" to "call_invite",
                    "callType" to "voice",
                    "callStatus" to "declined",
                    "timestamp" to now - 60000L,
                    "isRead" to true,
                ),
            )

            samples.forEach { payload ->
                chatRepository.sendRawMessage(chatId, payload)
            }
        }
    }

    fun initiateCallBack(chatId: String, callType: String) {
        startOutgoingCall(chatId, if (callType.equals("audio", ignoreCase = true)) "voice" else callType) {}
    }

    fun toggleReactionOnMessage(remoteMessageId: String, emoji: String) {
        if (remoteMessageId.isBlank() || emoji.isBlank()) return
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val chatId = currentChatId ?: return

        viewModelScope.launch {
            val target = _chatState.value.messages.firstOrNull {
                it.remoteMessageId == remoteMessageId || it.id == remoteMessageId
            } ?: return@launch

            val updated = target.reactions.toMutableMap()
            val existing = updated[emoji].orEmpty().toMutableSet()
            val wasAdded = if (existing.contains(currentUserId)) {
                existing.remove(currentUserId)
                false
            } else {
                existing.add(currentUserId)
                true
            }
            if (existing.isEmpty()) updated.remove(emoji) else updated[emoji] = existing.toList()

            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(target.id)
                    .update("reactions", updated)
                    .await()

                if (wasAdded) {
                    runCatching {
                        NotificationLogger.notifyUser(
                            toUserId = target.senderId,
                            type = "reaction",
                            title = "New reaction",
                            body = "$emoji reaction on your message",
                            targetId = target.remoteMessageId.ifBlank { target.id },
                        )
                    }
                }
            }
        }
    }

    fun toggleHideTextOnMessage(remoteMessageId: String) {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val chatId = currentChatId ?: return
        if (remoteMessageId.isBlank() || currentUserId.isBlank()) return

        viewModelScope.launch {
            val target = _chatState.value.messages.firstOrNull {
                it.remoteMessageId == remoteMessageId || it.id == remoteMessageId
            } ?: return@launch

            val hidden = target.hiddenTextFor.toMutableSet()
            val updateValue = if (hidden.contains(currentUserId)) {
                FieldValue.arrayRemove(currentUserId)
            } else {
                FieldValue.arrayUnion(currentUserId)
            }

            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(target.id)
                    .update("hiddenTextFor", updateValue)
                    .await()
            }
        }
    }

    fun startIncomingCallListener() {
        incomingCallsListener?.remove()
        val userId = auth.currentUser?.uid ?: return
        incomingCallsListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("incomingCalls")
            .whereEqualTo("status", "RINGING")
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                val doc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                _incomingCallState.value = IncomingCallInvite(
                    chatId = doc.getString("chatId").orEmpty(),
                    callType = doc.getString("callType") ?: "voice",
                    callId = doc.id,
                )
            }
    }

    fun startOutgoingCall(chatId: String, callType: String, onReady: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("ChatViewModel", "startOutgoingCall initiated: chatId=$chatId, callType=$callType, callerId=$userId")
        viewModelScope.launch {
            val calleeId = resolveOtherUserId(chatId)
            android.util.Log.d("ChatViewModel", "startOutgoingCall: resolved calleeId=$calleeId")
            if (calleeId.isBlank()) {
                android.util.Log.e("ChatViewModel", "startOutgoingCall: resolved calleeId is blank! Returning early.")
                return@launch
            }
            val callDoc = FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("calls")
                .document()

            val payload = mapOf(
                "callerId" to userId,
                "calleeId" to calleeId,
                "callType" to callType,
                "status" to "RINGING",
                "startedAt" to System.currentTimeMillis(),
            )
            runCatching {
                android.util.Log.d("ChatViewModel", "startOutgoingCall: writing call doc at chats/$chatId/calls/${callDoc.id}")
                callDoc.set(payload).await()
                android.util.Log.d("ChatViewModel", "startOutgoingCall: call doc written successfully")

                android.util.Log.d("ChatViewModel", "startOutgoingCall: sending call invite message...")
                val sendMsgResult = chatRepository.sendMessage(
                    chatId,
                    RepositoryMessage(
                        remoteMessageId = callDoc.id,
                        senderId = userId,
                        text = if (callType.equals("video", ignoreCase = true)) "Video call" else "Voice call",
                        messageType = "call_invite",
                        callType = callType,
                        callStatus = "ringing",
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                    ),
                )
                android.util.Log.d("ChatViewModel", "startOutgoingCall: send invite message completed. Success=${sendMsgResult.isSuccess}")

                android.util.Log.d("ChatViewModel", "startOutgoingCall: writing incomingCall doc for callee=$calleeId")
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(calleeId)
                    .collection("incomingCalls")
                    .document(callDoc.id)
                    .set(
                        mapOf(
                            "chatId" to chatId,
                            "callId" to callDoc.id,
                            "callerId" to userId,
                            "calleeId" to calleeId,
                            "callType" to callType,
                            "status" to "RINGING",
                            "startedAt" to System.currentTimeMillis(),
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                    .await()
                android.util.Log.d("ChatViewModel", "startOutgoingCall: incomingCall doc written successfully")

                runCatching {
                    NotificationLogger.notifyUser(
                        toUserId = calleeId,
                        type = "call_invite",
                        title = if (callType.equals("video", ignoreCase = true)) "Incoming video call" else "Incoming voice call",
                        body = "Tap to answer",
                        targetId = callDoc.id,
                    )
                }
                android.util.Log.d("ChatViewModel", "startOutgoingCall: calling onReady for callId=${callDoc.id}")
                onReady(callDoc.id)
            }.onFailure { e ->
                android.util.Log.e("ChatViewModel", "startOutgoingCall error during execution: ", e)
            }
        }
    }

    fun requestDiagnostics() {
        // No-op placeholder used by debug menu.
    }

    fun rotateKeysForCurrentChat() {
        // No-op placeholder used by debug menu.
    }

    fun acceptIncomingCall(chatId: String, callId: String) {
        if (chatId.isBlank() || callId.isBlank()) return
        viewModelScope.launch {
            val userId = auth.currentUser?.uid.orEmpty()
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .collection("calls")
                    .document(callId)
                    .update(
                        mapOf(
                            "status" to "ACCEPTED",
                            "acceptedAt" to System.currentTimeMillis(),
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                    .await()
                if (userId.isNotBlank()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("incomingCalls")
                        .document(callId)
                        .update(
                            mapOf(
                                "status" to "ACCEPTED",
                                "acceptedAt" to System.currentTimeMillis(),
                                "updatedAt" to System.currentTimeMillis(),
                            ),
                        )
                        .await()
                }
            }
        }
    }

    fun declineIncomingCall(chatId: String, callId: String) {
        if (chatId.isBlank() || callId.isBlank()) return
        viewModelScope.launch {
            val userId = auth.currentUser?.uid.orEmpty()
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .collection("calls")
                    .document(callId)
                    .update(
                        mapOf(
                            "status" to "DECLINED",
                            "declinedAt" to System.currentTimeMillis(),
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                    .await()
                if (userId.isNotBlank()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("incomingCalls")
                        .document(callId)
                        .update(
                            mapOf(
                                "status" to "DECLINED",
                                "declinedAt" to System.currentTimeMillis(),
                                "updatedAt" to System.currentTimeMillis(),
                            ),
                        )
                        .await()
                }
            }
        }
    }

    fun consumeIncomingCall() {
        _incomingCallState.value = null
    }

    fun openChatPreview(chatId: String) {
        val chat = _chatListState.value.chats.firstOrNull { it.id == chatId } ?: return
        previewMessagesJob?.cancel()
        _chatPreviewState.value = ChatPreviewUiState(
            chatId = chat.id,
            chatName = chat.name,
            profileImageUrl = chat.profileImageUrl,
            avatarEmoji = chat.avatarEmoji.ifBlank { chat.avatarInitials.ifBlank { "👤" } },
            timestamp = chat.timestamp,
            isPinned = chat.isPinned,
            isMuted = chat.isMuted,
            isArchived = chat.isArchived,
            otherUserId = chat.otherUserId,
            isLoading = true,
        )

        previewMessagesJob = viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid.orEmpty()
            chatRepository.getMessages(chatId).collect { result ->
                result
                    .onSuccess { messages ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            // Warm up the chat secret cache once so that all decryptMessageSafely calls hit the memory cache
                            E2EEManager.getSharedChatSecret(chatId, currentUserId)
                            
                            val previewMsgItems = messages.takeLast(12).map { msg ->
                                val decryptedText = decryptMessageForUi(
                                    chatId = chatId,
                                    currentUserId = currentUserId,
                                    repositoryMessage = msg,
                                )
                                Message(
                                    id = msg.id,
                                    senderId = msg.senderId,
                                    text = decryptedText,
                                    encryptedPayload = msg.encryptedPayload,
                                    iv = msg.iv,
                                    encryptedKeyForSender = msg.encryptedKeyForSender,
                                    encryptedKeyForRecipient = msg.encryptedKeyForRecipient,
                                    encryptedKeyForSenderSha1 = msg.encryptedKeyForSenderSha1,
                                    encryptedKeyForRecipientSha1 = msg.encryptedKeyForRecipientSha1,
                                    isEncrypted = msg.isEncrypted,
                                    timestamp = formatTimestamp(msg.timestamp),
                                    timestampMillis = msg.timestamp,
                                    isSentByMe = msg.senderId == currentUserId,
                                    mediaUrl = msg.mediaUrl,
                                    mediaName = msg.mediaName,
                                    mediaType = msg.mediaType,
                                    voiceUrl = msg.voiceUrl,
                                    voiceDuration = msg.voiceDuration,
                                    isVoice = msg.voiceUrl.isNotBlank() || msg.voiceDuration.isNotBlank(),
                                    isCallInvite = msg.messageType.equals("call_invite", ignoreCase = true),
                                    callType = msg.callType,
                                    callStatus = msg.callStatus,
                                    isRead = msg.isRead,
                                    isEdited = msg.isEdited,
                                    isLiveLocation = msg.isLiveLocation,
                                    locationLatitude = msg.locationLatitude,
                                    locationLongitude = msg.locationLongitude,
                                    locationRemainingSeconds = msg.locationRemainingSeconds,
                                    hiddenTextFor = msg.hiddenTextFor,
                                    deletedForUsers = msg.deletedForUsers,
                                    reactions = msg.reactions,
                                )
                            }
                            
                            _chatPreviewState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    error = null,
                                    messages = previewMsgItems,
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _chatPreviewState.update { state ->
                            state.copy(isLoading = false, error = error.message ?: "Failed to load preview")
                        }
                    }
            }
        }
    }

    fun dismissChatPreview() {
        previewMessagesJob?.cancel()
        previewMessagesJob = null
        _chatPreviewState.value = ChatPreviewUiState()
    }

    fun markChatAsUnread(chatId: String) {
        _chatListState.update { state ->
            state.copy(
                allChats = state.allChats.map { chat ->
                    if (chat.id == chatId) chat.copy(unreadCount = if (chat.unreadCount > 0) chat.unreadCount else 1) else chat
                },
                chats = state.chats.map { chat ->
                    if (chat.id == chatId) chat.copy(unreadCount = if (chat.unreadCount > 0) chat.unreadCount else 1) else chat
                },
            )
        }
    }

    fun togglePinChat(chatId: String, isPinned: Boolean) {
        _chatListState.update { state ->
            state.copy(
                allChats = state.allChats.map { chat -> if (chat.id == chatId) chat.copy(isPinned = isPinned) else chat },
                chats = state.chats.map { chat -> if (chat.id == chatId) chat.copy(isPinned = isPinned) else chat },
            )
        }
        if (_chatPreviewState.value.chatId == chatId) {
            _chatPreviewState.update { it.copy(isPinned = isPinned) }
        }
        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("chats").document(chatId).update("isPinned", isPinned).await()
            }
        }
    }

    fun toggleMuteChat(chatId: String, isMuted: Boolean) {
        _chatListState.update { state ->
            state.copy(
                allChats = state.allChats.map { chat -> if (chat.id == chatId) chat.copy(isMuted = isMuted) else chat },
                chats = state.chats.map { chat -> if (chat.id == chatId) chat.copy(isMuted = isMuted) else chat },
            )
        }
        if (_chatPreviewState.value.chatId == chatId) {
            _chatPreviewState.update { it.copy(isMuted = isMuted) }
        }
        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("chats").document(chatId).update("isMuted", isMuted).await()
            }
        }
    }

    fun toggleArchiveChat(chatId: String, isArchived: Boolean) {
        _chatListState.update { state ->
            state.copy(
                allChats = state.allChats.map { chat -> if (chat.id == chatId) chat.copy(isArchived = isArchived) else chat },
                chats = state.chats.map { chat -> if (chat.id == chatId) chat.copy(isArchived = isArchived) else chat },
            )
        }
        if (_chatPreviewState.value.chatId == chatId) {
            _chatPreviewState.update { it.copy(isArchived = isArchived) }
        }
        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("chats").document(chatId).update("isArchived", isArchived).await()
            }
        }
    }

    fun deleteChat(chatId: String) {
        _chatListState.update { state ->
            state.copy(
                allChats = state.allChats.filterNot { it.id == chatId },
                chats = state.chats.filterNot { it.id == chatId },
            )
        }
        if (_chatPreviewState.value.chatId == chatId) {
            dismissChatPreview()
        }
        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("chats").document(chatId).update("deleted", true).await()
            }
        }
    }

    fun blockUser(userId: String) {
        if (userId.isBlank()) return
        val currentUser = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser)
                    .collection("blockedUsers")
                    .document(userId)
                    .set(mapOf("createdAt" to System.currentTimeMillis()))
                    .await()
            }
        }
    }

    fun observeStoryViewers(storyId: String): Flow<Result<List<StoryViewer>>> = callbackFlow {
        val listener = FirebaseFirestore.getInstance()
            .collection("stories")
            .document(storyId)
            .collection("viewers")
            .orderBy("viewedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val viewers = snapshot?.documents?.map { doc ->
                    StoryViewer(
                        userId = doc.id,
                        userName = doc.getString("userName") ?: "User",
                        userAvatarEmoji = doc.getString("userAvatarEmoji") ?: "👤",
                        userProfileImageUrl = doc.getString("userProfileImageUrl") ?: "",
                        viewedAtMillis = (doc.get("viewedAt") as? Number)?.toLong() ?: 0L,
                        viewedAtLabel = formatTimestamp((doc.get("viewedAt") as? Number)?.toLong() ?: 0L),
                    )
                } ?: emptyList()

                trySend(Result.success(viewers))
            }

        awaitClose { listener.remove() }
    }

    fun markStoryViewed(story: ChatStory) {
        val userId = auth.currentUser?.uid ?: return
        if (story.id.isBlank()) return

        viewModelScope.launch {
            val payload = mapOf(
                "userName" to (auth.currentUser?.displayName ?: "You"),
                "userAvatarEmoji" to _chatListState.value.currentUserAvatarEmoji,
                "userProfileImageUrl" to _chatListState.value.currentUserProfileImageUrl,
                "viewedAt" to System.currentTimeMillis(),
            )

            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("stories")
                    .document(story.id)
                    .collection("viewers")
                    .document(userId)
                    .set(payload)
                    .await()
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

    private fun observeStories() {
        storiesListener?.remove()
        val currentUserId = auth.currentUser?.uid.orEmpty()

        storiesListener = FirebaseFirestore.getInstance()
            .collection("stories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(64)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val now = System.currentTimeMillis()
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    val createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: now
                    val expiresAt = (doc.get("expiresAt") as? Number)?.toLong() ?: (createdAt + 86_400_000L)
                    if (expiresAt < now) return@mapNotNull null

                    val ownerId = doc.getString("userId") ?: doc.getString("ownerId") ?: ""
                    ChatStory(
                        id = doc.id,
                        ownerId = ownerId,
                        ownerName = doc.getString("ownerName") ?: doc.getString("userName") ?: "User",
                        ownerAvatarEmoji = doc.getString("ownerAvatarEmoji") ?: "👤",
                        ownerAvatarColor = (doc.get("ownerAvatarColor") as? Number)?.toLong() ?: 0xFF3B82F6,
                        ownerProfileImageUrl = doc.getString("ownerProfileImageUrl") ?: "",
                        note = doc.getString("note") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: doc.getString("imageUri") ?: "",
                        storyType = doc.getString("storyType") ?: if ((doc.getString("imageUrl") ?: doc.getString("imageUri")).isNullOrBlank()) "note" else "image",
                        overlayText = doc.getString("overlayText") ?: "",
                        overlayX = (doc.get("overlayX") as? Number)?.toFloat() ?: 0f,
                        overlayY = (doc.get("overlayY") as? Number)?.toFloat() ?: 0f,
                        overlayColor = (doc.get("overlayColor") as? Number)?.toLong() ?: 0xFFFFFFFF,
                        createdAtMillis = createdAt,
                        createdAtLabel = formatTimestamp(createdAt),
                        isMine = ownerId == currentUserId,
                    )
                } ?: emptyList()

                _chatListState.update { it.copy(stories = stories) }
            }
    }

    private fun loadCurrentUserStoryProfile() {
        val user = auth.currentUser ?: return
        val fallbackEmoji = "👤"
        _chatListState.update {
            it.copy(
                currentUserProfileImageUrl = user.photoUrl?.toString().orEmpty(),
                currentUserAvatarEmoji = fallbackEmoji,
                currentUserName = user.displayName.orEmpty(),
            )
        }

        viewModelScope.launch {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()
            }.onSuccess { doc ->
                _chatListState.update { state ->
                    state.copy(
                        currentUserProfileImageUrl = doc.getString("profileImageUrl") ?: state.currentUserProfileImageUrl,
                        currentUserAvatarEmoji = doc.getString("avatarEmoji") ?: state.currentUserAvatarEmoji,
                        currentUserName = doc.getString("displayName") ?: doc.getString("name") ?: state.currentUserName,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        currentChatId?.let { chatId ->
            viewModelScope.launch { setTypingState(chatId, false) }
        }
        typingDebounceJob?.cancel()
        storiesListener?.remove()
        storiesListener = null
        incomingCallsListener?.remove()
        incomingCallsListener = null
        contactUserListener?.remove()
        contactUserListener = null
        contactPresenceListener?.remove()
        contactPresenceListener = null
        chatTypingListener?.remove()
        chatTypingListener = null
        previewMessagesJob?.cancel()
        previewMessagesJob = null
        chatListUserListeners.values.forEach { it.remove() }
        chatListUserListeners.clear()
        chatListPresenceListeners.values.forEach { it.remove() }
        chatListPresenceListeners.clear()
        authStateListener?.let(auth::removeAuthStateListener)
        authStateListener = null
        chatLoadJob?.cancel()
        chatLoadJob = null
        super.onCleared()
    }

    private fun sendRichMessage(
        text: String,
        mediaType: String,
        mediaUrl: String = "",
        voiceUrl: String = "",
        mediaName: String = "",
        voiceDuration: String = "",
        messageType: String = "message",
        callType: String = "",
        callStatus: String = "",
        isLiveLocation: Boolean = false,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        locationRemainingSeconds: Long = 0L,
    ) {
        val chatId = currentChatId ?: return
        val now = System.currentTimeMillis()
        val senderId = auth.currentUser?.uid.orEmpty()
        val clientMessageId = "local-$now"
        enqueuePendingOutgoingMessage(
            createPendingOutgoingMessage(
                messageId = clientMessageId,
                senderId = senderId,
                text = text,
                mediaUrl = mediaUrl,
                mediaName = mediaName,
                mediaType = mediaType,
                voiceUrl = voiceUrl,
                voiceDuration = voiceDuration,
                isEncrypted = false,
                isLiveLocation = isLiveLocation,
                timestampMillis = now,
            )
        )
        typingDebounceJob?.cancel()

        viewModelScope.launch {
            setTypingState(chatId, false)
            val recipientId = resolveOtherUserId(chatId)
            val repositoryMessage = RepositoryMessage(
                remoteMessageId = clientMessageId,
                senderId = senderId,
                receiverId = recipientId,
                text = text,
                mediaUrl = mediaUrl,
                mediaName = mediaName,
                mediaType = mediaType,
                voiceUrl = voiceUrl,
                voiceDuration = voiceDuration,
                messageType = messageType,
                callType = callType,
                callStatus = callStatus,
                timestamp = now,
                isEdited = false,
                isLiveLocation = isLiveLocation,
                locationLatitude = latitude,
                locationLongitude = longitude,
                locationRemainingSeconds = locationRemainingSeconds,
                hiddenTextFor = emptyList(),
                reactions = emptyMap(),
                isRead = false,
                seen = false,
                isEncrypted = false,
            )

            chatRepository.sendMessage(chatId, repositoryMessage)
                .onSuccess {
                    removePendingOutgoingMessage(clientMessageId)
                    runCatching {
                        NotificationLogger.notifyUser(
                            toUserId = recipientId,
                            type = if (messageType == "call_invite") "call_invite" else "chat_message",
                            title = _chatState.value.contactName.ifBlank { "New message" },
                            body = text,
                            targetId = chatId,
                        )
                    }
                }
        }
    }

    private fun filterChats(chats: List<ChatItem>, query: String): List<ChatItem> {
        if (query.isBlank()) return chats
        return chats.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true)
        }
    }

    private suspend fun resolveOtherUserId(chatId: String): String {
        val inStateId = _chatState.value.contactUserId
        if (inStateId.isNotBlank()) {
            return inStateId
        }
        val doc = FirebaseFirestore.getInstance().collection("chats").document(chatId).get().await()
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val otherId = if (participants.size <= 2) {
            participants.firstOrNull { it != currentUserId }.orEmpty().ifBlank { doc.getString("otherUserId").orEmpty() }
        } else {
            doc.getString("otherUserId").orEmpty()
        }
        if (otherId.isNotBlank()) {
            _chatState.update { it.copy(contactUserId = otherId) }
        }
        return otherId
    }

    private fun observeContactProfile(userId: String) {
        contactUserListener?.remove()
        contactPresenceListener?.remove()
        contactUserListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val displayName = snapshot.getString("displayName")
                val avatarEmoji = snapshot.getString("avatarEmoji")
                val profileImageUrl = snapshot.getString("profileImageUrl")
                _chatState.update { state ->
                    state.copy(
                        contactName = displayName ?: state.contactName,
                        contactAvatarEmoji = avatarEmoji ?: state.contactAvatarEmoji,
                        contactProfileImageUrl = profileImageUrl ?: state.contactProfileImageUrl,
                    )
                }
                patchChatListForUserProfile(userId, displayName, avatarEmoji, profileImageUrl)
            }

        contactPresenceListener = FirebaseFirestore.getInstance()
            .collection("presence")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                val isOnline = snapshot?.getBoolean("isOnline") ?: false
                _chatState.update { state -> state.copy(isOnline = isOnline) }
                patchChatListForUserPresence(userId, isOnline)
            }
    }

    private fun observeChatTyping(chatId: String) {
        chatTypingListener?.remove()
        val currentUserId = auth.currentUser?.uid.orEmpty()
        chatTypingListener = FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .addSnapshotListener { snapshot, _ ->
                val typingIds = (snapshot?.get("typingUserIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()
                val otherUserTyping = typingIds.any { it != currentUserId }
                _chatState.update { state -> state.copy(isTyping = otherUserTyping) }
            }
    }

    private suspend fun setTypingState(chatId: String, typing: Boolean) {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        if (chatId.isBlank() || currentUserId.isBlank()) return
        if (selfTypingActive == typing) return

        selfTypingActive = typing
        runCatching {
            val updatePayload: Map<String, Any> = if (typing) {
                mapOf(
                    "typingUserIds" to FieldValue.arrayUnion(currentUserId),
                    "typingUpdatedAt" to System.currentTimeMillis(),
                )
            } else {
                mapOf(
                    "typingUserIds" to FieldValue.arrayRemove(currentUserId),
                    "typingUpdatedAt" to System.currentTimeMillis(),
                )
            }
            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .update(updatePayload)
                .await()
        }
    }

    private fun syncChatListRealtimeListeners(userIds: Set<String>) {
        val targetIds = userIds.filter { it.isNotBlank() }.toSet()

        chatListUserListeners.keys.toList().filterNot(targetIds::contains).forEach { userId ->
            chatListUserListeners.remove(userId)?.remove()
        }
        chatListPresenceListeners.keys.toList().filterNot(targetIds::contains).forEach { userId ->
            chatListPresenceListeners.remove(userId)?.remove()
        }

        targetIds.forEach { userId ->
            if (!chatListUserListeners.containsKey(userId)) {
                val listener = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                        patchChatListForUserProfile(
                            userId = userId,
                            displayName = snapshot.getString("displayName"),
                            avatarEmoji = snapshot.getString("avatarEmoji"),
                            profileImageUrl = snapshot.getString("profileImageUrl"),
                        )
                    }
                chatListUserListeners[userId] = listener
            }

            if (!chatListPresenceListeners.containsKey(userId)) {
                val listener = FirebaseFirestore.getInstance()
                    .collection("presence")
                    .document(userId)
                    .addSnapshotListener { snapshot, _ ->
                        patchChatListForUserPresence(userId, snapshot?.getBoolean("isOnline") ?: false)
                    }
                chatListPresenceListeners[userId] = listener
            }
        }
    }

    private fun patchChatListForUserProfile(
        userId: String,
        displayName: String?,
        avatarEmoji: String?,
        profileImageUrl: String?,
    ) {
        if (userId.isBlank()) return
        _chatListState.update { state ->
            val patch: (ChatItem) -> ChatItem = { chat ->
                if (chat.otherUserId != userId) {
                    chat
                } else {
                    chat.copy(
                        name = displayName ?: chat.name,
                        avatarEmoji = avatarEmoji ?: chat.avatarEmoji,
                        avatarInitials = avatarEmoji ?: chat.avatarInitials,
                        profileImageUrl = profileImageUrl ?: chat.profileImageUrl,
                    )
                }
            }
            state.copy(
                allChats = state.allChats.map(patch),
                chats = state.chats.map(patch),
            )
        }
    }

    private fun patchChatListForUserPresence(userId: String, isOnline: Boolean) {
        if (userId.isBlank()) return
        _chatListState.update { state ->
            val patch: (ChatItem) -> ChatItem = { chat ->
                if (chat.otherUserId != userId) chat else chat.copy(isOnline = isOnline)
            }
            state.copy(
                allChats = state.allChats.map(patch),
                chats = state.chats.map(patch),
            )
        }
        if (_chatState.value.contactUserId == userId) {
            _chatState.update { state -> state.copy(isOnline = isOnline) }
        }
    }

    private suspend fun buildOutgoingMessage(chatId: String, senderId: String, plaintext: String, messageId: String): RepositoryMessage {
        val now = System.currentTimeMillis()
        val recipientId = resolveOtherUserId(chatId)

        if (!E2EEConfig.isE2EEEnabled() || recipientId.isBlank()) {
            return RepositoryMessage(
                remoteMessageId = messageId,
                senderId = senderId,
                receiverId = recipientId,
                text = plaintext,
                timestamp = now,
                isRead = false,
                seen = false,
                isEncrypted = false,
            )
        }

        val encrypted = E2EEManager.encryptMessageForSending(
            chatId = chatId,
            senderId = senderId,
            recipientId = recipientId,
            plaintext = plaintext,
        ).getOrElse {
            return RepositoryMessage(
                remoteMessageId = messageId,
                senderId = senderId,
                receiverId = recipientId,
                text = plaintext,
                timestamp = now,
                isRead = false,
                seen = false,
                isEncrypted = false,
            )
        }

        return RepositoryMessage(
            remoteMessageId = messageId,
            senderId = senderId,
            receiverId = recipientId,
            text = "",
            encryptedPayload = encrypted.encryptedPayload,
            iv = encrypted.iv,
            encryptedKeyForSender = encrypted.encryptedKeyForSender,
            encryptedKeyForRecipient = encrypted.encryptedKeyForRecipient,
            encryptedKeyForSenderSha1 = encrypted.encryptedKeyForSenderSha1,
            encryptedKeyForRecipientSha1 = encrypted.encryptedKeyForRecipientSha1,
            isEncrypted = true,
            timestamp = encrypted.timestamp,
            isRead = false,
            seen = false,
        )
    }

    private fun createPendingOutgoingMessage(
        messageId: String,
        senderId: String,
        text: String,
        mediaUrl: String = "",
        mediaName: String = "",
        mediaType: String = "",
        voiceUrl: String = "",
        voiceDuration: String = "",
        isEncrypted: Boolean,
        isLiveLocation: Boolean = false,
        timestampMillis: Long,
    ): Message {
        return Message(
            id = messageId,
            remoteMessageId = messageId,
            senderId = senderId,
            receiverId = resolveCurrentRecipientId(),
            text = text,
            mediaUrl = mediaUrl,
            mediaName = mediaName,
            mediaType = mediaType,
            voiceUrl = voiceUrl,
            voiceDuration = voiceDuration,
            timestamp = formatTimestamp(timestampMillis),
            timestampMillis = timestampMillis,
            isSentByMe = true,
            isVoice = voiceUrl.isNotBlank() || voiceDuration.isNotBlank(),
            isCallInvite = false,
            isRead = false,
            isEdited = false,
            isLiveLocation = isLiveLocation,
            hiddenTextFor = emptyList(),
            deletedForUsers = emptyList(),
            reactions = emptyMap(),
            seen = false,
            deliveryState = MessageDeliveryState.Sending,
            isEncrypted = isEncrypted,
        )
    }

    private fun enqueuePendingOutgoingMessage(message: Message) {
        pendingOutgoingMessages[message.id] = message
        _chatState.update { state ->
            state.copy(messages = mergeMessagesWithPending(state.messages))
        }
    }

    private fun removePendingOutgoingMessage(messageId: String) {
        if (pendingOutgoingMessages.remove(messageId) != null) {
            _chatState.update { state ->
                state.copy(messages = mergeMessagesWithPending(state.messages))
            }
        }
    }

    private fun markPendingOutgoingMessageFailed(messageId: String) {
        val current = pendingOutgoingMessages[messageId] ?: return
        pendingOutgoingMessages[messageId] = current.copy(deliveryState = MessageDeliveryState.Failed)
        _chatState.update { state ->
            state.copy(messages = mergeMessagesWithPending(state.messages))
        }
    }

    private fun mergeMessagesWithPending(serverMessages: List<Message>): List<Message> {
        if (pendingOutgoingMessages.isEmpty()) return serverMessages
        val serverIds = serverMessages.map { it.id }.toHashSet()
        val pending = pendingOutgoingMessages.values
            .filter { it.id !in serverIds }
            .sortedBy { it.timestampMillis }
        return (serverMessages + pending).sortedBy { it.timestampMillis }
    }

    private fun resolveCurrentRecipientId(): String {
        return _chatState.value.contactUserId
    }

    private fun markIncomingMessagesAsSeen(chatId: String, messages: List<Message>) {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        if (chatId.isBlank() || currentUserId.isBlank()) return

        val unreadIncoming = messages.filter { message ->
            message.senderId != currentUserId && !(message.isRead || message.seen)
        }
        if (unreadIncoming.isEmpty()) return

        viewModelScope.launch {
            unreadIncoming.forEach { message ->
                runCatching {
                    FirebaseFirestore.getInstance()
                        .collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(message.id)
                        .update(
                            mapOf(
                                "isRead" to true,
                                "seen" to true,
                            ),
                        )
                        .await()
                }
            }
        }
    }

    private suspend fun decryptMessageForUi(
        chatId: String,
        currentUserId: String,
        repositoryMessage: RepositoryMessage,
    ): String {
        val localMessageId = repositoryMessage.id.ifBlank { repositoryMessage.remoteMessageId }
        if (!repositoryMessage.isEncrypted || repositoryMessage.encryptedPayload.isBlank()) {
            return repositoryMessage.text
        }

        if (currentUserId.isBlank()) return repositoryMessage.text

        if (repositoryMessage.senderId == currentUserId && localMessageId.isNotBlank()) {
            E2EEManager.loadLocalPlaintext(localMessageId)?.let { cachedPlaintext ->
                return cachedPlaintext
            }
        }

        val decrypted = E2EEManager.decryptMessageSafely(
            chatId = chatId,
            userId = currentUserId,
            senderId = repositoryMessage.senderId,
            encryptedMessage = EncryptedChatMessage(
                encryptedPayload = repositoryMessage.encryptedPayload,
                iv = repositoryMessage.iv,
                encryptedKeyForSender = repositoryMessage.encryptedKeyForSender,
                encryptedKeyForRecipient = repositoryMessage.encryptedKeyForRecipient,
                encryptedKeyForSenderSha1 = repositoryMessage.encryptedKeyForSenderSha1,
                encryptedKeyForRecipientSha1 = repositoryMessage.encryptedKeyForRecipientSha1,
                timestamp = repositoryMessage.timestamp,
            ),
        )

        return decrypted.getOrNull()?.also { plaintext ->
            if (repositoryMessage.senderId == currentUserId && localMessageId.isNotBlank()) {
                E2EEManager.storeLocalPlaintext(localMessageId, plaintext)
            }
        } ?: run {
            if (repositoryMessage.senderId == currentUserId && localMessageId.isNotBlank()) {
                E2EEManager.loadLocalPlaintext(localMessageId) ?: repositoryMessage.text
            } else {
                repositoryMessage.text.ifBlank { "[Unable to decrypt]" }
            }
        }
    }
}