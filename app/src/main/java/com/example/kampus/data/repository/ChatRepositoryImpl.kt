package com.example.kampus.data.repository

import com.example.kampus.ui.chat.ChatItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Message(
    val id: String = "",
    val remoteMessageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val encryptedPayload: String = "",
    val iv: String = "",
    val encryptedKeyForSender: String = "",
    val encryptedKeyForRecipient: String = "",
    val encryptedKeyForSenderSha1: String = "",
    val encryptedKeyForRecipientSha1: String = "",
    val isEncrypted: Boolean = false,
    val mediaUrl: String = "",
    val mediaName: String = "",
    val mediaType: String = "",
    val voiceUrl: String = "",
    val voiceDuration: String = "",
    val messageType: String = "message",
    val callType: String = "",
    val callStatus: String = "",
    val timestamp: Long = 0,
    val isEdited: Boolean = false,
    val isLiveLocation: Boolean = false,
    val locationLatitude: Double = 0.0,
    val locationLongitude: Double = 0.0,
    val locationRemainingSeconds: Long = 0L,
    val hiddenTextFor: List<String> = emptyList(),
    val deletedForUsers: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),
    val isRead: Boolean = false,
    val seen: Boolean = false,
)

class ChatRepositoryImpl(private val firestore: FirebaseFirestore) {
    private val auth = FirebaseAuth.getInstance()

    fun getChats(): Flow<Result<List<ChatItem>>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            trySend(Result.success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }
        var latestLoadJob: kotlinx.coroutines.Job? = null
        val listener = firestore.collection("chats")
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                latestLoadJob?.cancel()
                latestLoadJob = launch {
                    val chats = snapshot?.documents?.mapNotNull { doc ->
                        val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        if (participants.isNotEmpty() && !participants.contains(currentUserId)) {
                            null
                        } else {
                            mapChatDocument(doc, currentUserId)
                        }
                    } ?: emptyList()

                    trySend(Result.success(chats))
                }
            }

        awaitClose {
            latestLoadJob?.cancel()
            listener.remove()
        }
    }

    fun getChatById(chatId: String): Flow<Result<ChatItem?>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        var latestLoadJob: kotlinx.coroutines.Job? = null
        val listener = firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                latestLoadJob?.cancel()
                latestLoadJob = launch {
                    val chat = if (snapshot != null && snapshot.exists()) {
                        mapChatDocument(snapshot, currentUserId)
                    } else null

                    trySend(Result.success(chat))
                }
            }

        awaitClose {
            latestLoadJob?.cancel()
            listener.remove()
        }
    }

    fun getMessages(chatId: String): Flow<Result<List<Message>>> = callbackFlow {
        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val reactionsRaw = doc.get("reactions") as? Map<*, *> ?: emptyMap<Any, Any>()
                        val reactions = reactionsRaw.entries.associate { (key, value) ->
                            key.toString() to ((value as? List<*>)?.filterIsInstance<String>() ?: emptyList())
                        }

                        Message(
                            id = doc.id,
                            remoteMessageId = doc.getString("remoteMessageId") ?: doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            text = doc.getString("text") ?: "",
                            encryptedPayload = doc.getString("encryptedPayload") ?: "",
                            iv = doc.getString("iv") ?: "",
                            encryptedKeyForSender = doc.getString("encryptedKeyForSender") ?: "",
                            encryptedKeyForRecipient = doc.getString("encryptedKeyForRecipient") ?: "",
                            encryptedKeyForSenderSha1 = doc.getString("encryptedKeyForSenderSha1") ?: "",
                            encryptedKeyForRecipientSha1 = doc.getString("encryptedKeyForRecipientSha1") ?: "",
                            isEncrypted = doc.getBoolean("isEncrypted") ?: false,
                            mediaUrl = doc.getString("mediaUrl") ?: doc.getString("attachmentUrl") ?: "",
                            mediaName = doc.getString("mediaName") ?: "",
                            mediaType = doc.getString("mediaType") ?: "",
                            voiceUrl = doc.getString("voiceUrl") ?: "",
                            voiceDuration = doc.getString("voiceDuration") ?: "",
                            messageType = doc.getString("messageType") ?: "message",
                            callType = doc.getString("callType") ?: "",
                            callStatus = doc.getString("callStatus") ?: "",
                            timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0,
                            isEdited = doc.getBoolean("isEdited") ?: false,
                            isLiveLocation = doc.getBoolean("isLiveLocation") ?: false,
                            locationLatitude = (doc.get("locationLatitude") as? Number)?.toDouble() ?: 0.0,
                            locationLongitude = (doc.get("locationLongitude") as? Number)?.toDouble() ?: 0.0,
                            locationRemainingSeconds = (doc.get("locationRemainingSeconds") as? Number)?.toLong() ?: 0L,
                            hiddenTextFor = (doc.get("hiddenTextFor") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            deletedForUsers = (doc.get("deletedForUsers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            reactions = reactions,
                            isRead = doc.getBoolean("isRead") ?: doc.getBoolean("seen") ?: false,
                            seen = doc.getBoolean("seen") ?: doc.getBoolean("isRead") ?: false,
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(messages))
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, message: Message): Result<String> = try {
        val now = if (message.timestamp > 0L) message.timestamp else System.currentTimeMillis()
        val clientMessageId = message.remoteMessageId.ifBlank { "local-$now" }
        val payload = mutableMapOf<String, Any>(
            "remoteMessageId" to clientMessageId,
            "clientMessageId" to clientMessageId,
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "text" to message.text,
            "encryptedPayload" to message.encryptedPayload,
            "iv" to message.iv,
            "encryptedKeyForSender" to message.encryptedKeyForSender,
            "encryptedKeyForRecipient" to message.encryptedKeyForRecipient,
            "encryptedKeyForSenderSha1" to message.encryptedKeyForSenderSha1,
            "encryptedKeyForRecipientSha1" to message.encryptedKeyForRecipientSha1,
            "isEncrypted" to message.isEncrypted,
            "mediaUrl" to message.mediaUrl,
            "mediaName" to message.mediaName,
            "mediaType" to message.mediaType,
            "voiceUrl" to message.voiceUrl,
            "voiceDuration" to message.voiceDuration,
            "messageType" to message.messageType,
            "callType" to message.callType,
            "callStatus" to message.callStatus,
            "timestamp" to now,
            "isEdited" to message.isEdited,
            "isLiveLocation" to message.isLiveLocation,
            "locationLatitude" to message.locationLatitude,
            "locationLongitude" to message.locationLongitude,
            "locationRemainingSeconds" to message.locationRemainingSeconds,
            "hiddenTextFor" to message.hiddenTextFor,
            "deletedForUsers" to message.deletedForUsers,
            "reactions" to message.reactions,
            "isRead" to message.isRead,
            "seen" to (message.seen || message.isRead),
        )

        if (message.text.isBlank()) {
            payload.remove("text")
        }
        if (message.encryptedPayload.isBlank()) {
            payload.remove("encryptedPayload")
            payload.remove("iv")
            payload.remove("encryptedKeyForSender")
            payload.remove("encryptedKeyForRecipient")
            payload.remove("encryptedKeyForSenderSha1")
            payload.remove("encryptedKeyForRecipientSha1")
            payload["isEncrypted"] = false
        }

            val ref = firestore.collection("chats").document(chatId)
                .collection("messages")
                .document(clientMessageId)
            ref.set(payload).await()

        firestore.collection("chats").document(chatId).update(
            mapOf(
                "lastMessage" to when {
                    message.messageType.equals("call_invite", ignoreCase = true) -> "Call"
                    message.mediaType.startsWith("image") -> "Photo"
                    message.mediaType.startsWith("video") -> "Video"
                    message.voiceUrl.isNotBlank() || message.voiceDuration.isNotBlank() || message.mediaType.startsWith("audio") -> "Voice message"
                    message.isEncrypted && message.encryptedPayload.isNotBlank() -> "Message"
                    message.text.isNotBlank() -> message.text
                    else -> "Attachment"
                },
                "lastMessageType" to if (message.messageType.equals("call_invite", ignoreCase = true)) "call" else "message",
                "lastCallType" to message.callType,
                "lastCallStatus" to message.callStatus,
                "lastMessageSenderId" to message.senderId,
                "lastMessageSenderName" to if (message.senderId == auth.currentUser?.uid) "You" else "",
                "lastMessageTime" to now,
                "timestamp" to formatRelativeTime(now),
            )
        ).await()

        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendRawMessage(chatId: String, payload: Map<String, Any>): Result<String> = try {
        val now = (payload["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val mutablePayload = payload.toMutableMap()
        val clientMessageId = (mutablePayload["clientMessageId"] as? String).orEmpty().ifBlank {
            (mutablePayload["remoteMessageId"] as? String).orEmpty().ifBlank { "local-$now" }
        }
        mutablePayload["clientMessageId"] = clientMessageId
        mutablePayload["remoteMessageId"] = (mutablePayload["remoteMessageId"] as? String).orEmpty().ifBlank { clientMessageId }
        mutablePayload["timestamp"] = now
        mutablePayload.putIfAbsent("seen", mutablePayload["isRead"] as? Boolean ?: false)
        val ref = firestore.collection("chats").document(chatId)
            .collection("messages")
            .document(clientMessageId)
        ref.set(mutablePayload).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createChat(chat: Map<String, Any>): Result<String> = try {
        val ref = firestore.collection("chats").add(chat).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getOrCreateDirectChat(currentUserId: String, otherUserId: String): Result<String> = try {
        val existing = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .await()
            .documents
            .firstOrNull { doc ->
                val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                participants.contains(otherUserId)
            }

        if (existing != null) {
            Result.success(existing.id)
        } else {
            val otherUserDoc = firestore.collection("users").document(otherUserId).get().await()
            val otherName = otherUserDoc.getString("displayName") ?: "Unknown"
            val initials = otherName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
                .ifEmpty { "UN" }

            val now = System.currentTimeMillis()
            val chatData = mapOf(
                "name" to otherName,
                "lastMessage" to "Say hi!",
                "lastMessageType" to "message",
                "lastCallType" to "",
                "lastCallStatus" to "",
                "lastMessageSenderName" to "",
                "timestamp" to "now",
                "lastMessageTime" to now,
                "participants" to listOf(currentUserId, otherUserId),
                "avatarInitials" to initials,
                "avatarColor" to 0xFF3B82F6,
                "unreadCount" to 0,
                "isOnline" to (otherUserDoc.getBoolean("isOnline") ?: false),
            )

            val created = firestore.collection("chats").add(chatData).await()
            Result.success(created.id)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markAsRead(chatId: String): Result<Unit> = try {
        firestore.collection("chats").document(chatId).update("unreadCount", 0).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun mapChatDocument(doc: DocumentSnapshot, currentUserId: String?): ChatItem? {
        return try {
            val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val otherUserId = if (participants.size <= 2) {
                currentUserId?.let { currentId -> participants.firstOrNull { participant -> participant != currentId } }
                    ?: doc.getString("otherUserId")
                    ?: ""
            } else {
                doc.getString("otherUserId") ?: ""
            }
            val lastMessageTime = (doc.get("lastMessageTime") as? Number)?.toLong() ?: 0L
            val timestampLabel = doc.getString("timestamp")
                ?: if (lastMessageTime > 0L) formatRelativeTime(lastMessageTime) else "now"
            val lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""

            val profile = if (otherUserId.isNotBlank() && otherUserId != currentUserId) {
                resolveUserIdentity(otherUserId)
            } else {
                null
            }

            ChatItem(
                id = doc.id,
                name = profile?.displayName ?: doc.getString("name") ?: if (otherUserId.isNotBlank()) otherUserId else "Unknown",
                lastMessage = doc.getString("lastMessage") ?: "No messages",
                lastMessageType = doc.getString("lastMessageType") ?: "message",
                lastCallType = doc.getString("lastCallType") ?: "",
                lastCallStatus = doc.getString("lastCallStatus") ?: "",
                lastMessageSenderName = doc.getString("lastMessageSenderName") ?: "",
                timestamp = timestampLabel,
                unreadCount = (doc.get("unreadCount") as? Number)?.toInt() ?: 0,
                isOnline = profile?.isOnline ?: doc.getBoolean("isOnline") ?: false,
                avatarInitials = profile?.avatarEmoji ?: doc.getString("avatarInitials") ?: "UN",
                avatarColor = (doc.get("avatarColor") as? Number)?.toLong() ?: 0xFF3B82F6,
                avatarEmoji = profile?.avatarEmoji ?: doc.getString("avatarEmoji") ?: "👤",
                profileImageUrl = profile?.profileImageUrl ?: doc.getString("profileImageUrl") ?: "",
                isPinned = doc.getBoolean("isPinned") ?: false,
                isMuted = doc.getBoolean("isMuted") ?: false,
                isArchived = doc.getBoolean("isArchived") ?: false,
                otherUserId = otherUserId,
                isLastMessageFromMe = currentUserId != null && lastMessageSenderId == currentUserId,
                lastMessageSenderId = lastMessageSenderId,
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveUserIdentity(userId: String): UserIdentity? {
        if (userId.isBlank()) return null
        val snapshot = firestore.collection("users").document(userId).get().await()
        if (!snapshot.exists()) return null
        val displayName = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("name")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("username")?.takeIf { it.isNotBlank() }
            ?: userId
        return UserIdentity(
            displayName = displayName,
            avatarEmoji = snapshot.getString("avatarEmoji") ?: "👤",
            profileImageUrl = snapshot.getString("profileImageUrl") ?: "",
            isOnline = snapshot.getBoolean("isOnline") ?: false,
        )
    }

    private data class UserIdentity(
        val displayName: String,
        val avatarEmoji: String,
        val profileImageUrl: String,
        val isOnline: Boolean,
    )

    private fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "now"
        val now = System.currentTimeMillis()
        val diffMillis = (now - timestamp).coerceAtLeast(0L)
        val diffMinutes = diffMillis / (1000L * 60L)
        val diffHours = diffMinutes / 60L
        val diffDays = diffHours / 24L

        return when {
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
