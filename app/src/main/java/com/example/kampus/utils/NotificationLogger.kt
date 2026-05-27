package com.example.kampus.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
object NotificationLogger {
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun notifyUser(
        toUserId: String,
        type: String,
        title: String,
        body: String,
        targetId: String = "",
    ) {
        val actorId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (toUserId.isBlank() || actorId.isBlank() || toUserId == actorId) return

        val actorProfile = runCatching {
            firestore.collection("users").document(actorId).get().await()
        }.getOrNull()

        val recipientProfile = runCatching {
            firestore.collection("users").document(toUserId).get().await()
        }.getOrNull()

        // Respect recipient notification settings before writing the event.
        val settings = recipientProfile?.get("notificationSettings") as? Map<*, *>
        val pushEnabled = (settings?.get("pushNotifications") as? Boolean) ?: true
        val typeEnabled = when (type) {
            "like" -> (settings?.get("likes") as? Boolean) ?: true
            "comment" -> (settings?.get("comments") as? Boolean) ?: true
            "love", "reaction" -> (settings?.get("likes") as? Boolean) ?: true
            "follow", "friend_request" -> (settings?.get("newFollowers") as? Boolean) ?: true
            "mention" -> (settings?.get("mentions") as? Boolean) ?: true
            "direct_message" -> (settings?.get("directMessages") as? Boolean) ?: true
            "chat_message", "call_invite" -> (settings?.get("directMessages") as? Boolean) ?: true
            else -> true
        }

        if (!pushEnabled || !typeEnabled) return

        val payload = mapOf(
            "type" to type,
            "title" to title,
            "body" to body,
            "toUserId" to toUserId,
            "actorUserId" to actorId,
            "actorDisplayName" to (actorProfile?.getString("displayName") ?: "Someone"),
            "targetId" to targetId,
            "createdAt" to System.currentTimeMillis(),
            "isRead" to false,
        )

        firestore.collection("users")
            .document(toUserId)
            .collection("notifications")
            .add(payload)
            .await()
    }

    fun observeUserNotifications(userId: String, onChange: (Result<List<Map<String, Any>>>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onChange(Result.failure(error))
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.map { doc ->
                    mapOf(
                        "id" to doc.id,
                        "type" to (doc.getString("type") ?: "system"),
                        "title" to (doc.getString("title") ?: "Notification"),
                        "body" to (doc.getString("body") ?: ""),
                        "toUserId" to (doc.getString("toUserId") ?: ""),
                        "actorUserId" to (doc.getString("actorUserId") ?: ""),
                        "actorDisplayName" to (doc.getString("actorDisplayName") ?: ""),
                        "targetId" to (doc.getString("targetId") ?: ""),
                        "createdAt" to (doc.getLong("createdAt") ?: 0L),
                        "isRead" to (doc.getBoolean("isRead") ?: false),
                    )
                } ?: emptyList()

                onChange(Result.success(items))
            }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        if (userId.isBlank() || notificationId.isBlank()) return
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }
}
