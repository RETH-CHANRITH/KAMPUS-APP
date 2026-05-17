package com.example.kampus.data.repository

import com.example.kampus.ui.chat.ChatItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)



class ChatRepositoryImpl(private val firestore: FirebaseFirestore) {

    fun getChats(): Flow<Result<List<ChatItem>>> = callbackFlow {
        val listener = firestore.collection("chats")
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            lastMessage = doc.getString("lastMessage") ?: "No messages",
                            lastMessageType = doc.getString("lastMessageType") ?: "message",
                            lastCallType = doc.getString("lastCallType") ?: "",
                            lastCallStatus = doc.getString("lastCallStatus") ?: "",
                            lastMessageSenderName = doc.getString("lastMessageSenderName") ?: "",
                            timestamp = doc.getString("timestamp") ?: "now",
                            avatarInitials = doc.getString("avatarInitials") ?: "UN",
                            avatarColor = (doc.get("avatarColor") as? Number)?.toLong() ?: 0xFF3B82F6,
                            unreadCount = (doc.get("unreadCount") as? Number)?.toInt() ?: 0,
                            isOnline = doc.getBoolean("isOnline") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(chats))
            }

        awaitClose { listener.remove() }
    }

    fun getChatById(chatId: String): Flow<Result<ChatItem?>> = callbackFlow {
        val listener = firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val chat = if (snapshot != null && snapshot.exists()) {
                    try {
                        ChatItem(
                            id = snapshot.id,
                            name = snapshot.getString("name") ?: "Unknown",
                            lastMessage = snapshot.getString("lastMessage") ?: "No messages",
                            lastMessageType = snapshot.getString("lastMessageType") ?: "message",
                            lastCallType = snapshot.getString("lastCallType") ?: "",
                            lastCallStatus = snapshot.getString("lastCallStatus") ?: "",
                            lastMessageSenderName = snapshot.getString("lastMessageSenderName") ?: "",
                            timestamp = snapshot.getString("timestamp") ?: "now",
                            avatarInitials = snapshot.getString("avatarInitials") ?: "UN",
                            avatarColor = (snapshot.get("avatarColor") as? Number)?.toLong() ?: 0xFF3B82F6,
                            unreadCount = (snapshot.get("unreadCount") as? Number)?.toInt() ?: 0,
                            isOnline = snapshot.getBoolean("isOnline") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null

                trySend(Result.success(chat))
            }

        awaitClose { listener.remove() }
    }

    fun getMessages(chatId: String): Flow<Result<List<Message>>> = callbackFlow {
        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0,
                            isRead = doc.getBoolean("isRead") ?: false
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
        val ref = firestore.collection("chats").document(chatId)
            .collection("messages").add(message).await()
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
}
