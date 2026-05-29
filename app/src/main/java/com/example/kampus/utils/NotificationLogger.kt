package com.example.kampus.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.kampus.MainActivity
import com.example.kampus.R

object NotificationLogger {
    fun showSystemNotification(
        context: Context,
        title: String,
        body: String,
        type: String,
        targetId: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "kampus_notifications"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Kampus general notifications and alerts"
                }
                manager.createNotificationChannel(channel)
            }
        }

        // Build intent to open MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            
            when (type) {
                "comment", "like", "love", "reaction" -> {
                    val postId = targetId.toIntOrNull() ?: -1
                    if (postId != -1) {
                        putExtra("openPostId", postId)
                    }
                }
                "direct_message", "chat_message", "story_reply" -> {
                    if (targetId.isNotBlank()) {
                        putExtra("openChatId", targetId)
                    }
                }
                else -> {
                    // For follows, friend requests, event updates, and reports
                    putExtra("openNotifications", true)
                }
            }
        }

        val notificationId = (title + body + type + targetId).hashCode().let { kotlin.math.abs(it) }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

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
