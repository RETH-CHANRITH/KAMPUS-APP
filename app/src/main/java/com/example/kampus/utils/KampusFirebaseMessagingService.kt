package com.example.kampus.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kampus.MainActivity
import com.example.kampus.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KampusFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.cachePendingToken(applicationContext, token)
        serviceScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (!userId.isNullOrBlank()) {
                FcmTokenManager.syncCurrentDeviceToken(applicationContext, userId)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val messageType = data["type"] ?: message.data["messageType"] ?: ""
        val isChatMessage = messageType == "chat_message" || data.containsKey("chatId")
        val isCallInvite = messageType == "call_invite"

        val title = data["title"]
            ?: message.notification?.title
            ?: getString(R.string.app_name)

        val body = data["body"]
            ?: data["message"]
            ?: message.notification?.body
            ?: ""

        if (title.isBlank() && body.isBlank()) return

        if (isCallInvite) {
            showIncomingCallNotification(
                title = if (title.isBlank()) "Incoming call" else title,
                body = if (body.isBlank()) "Tap to answer" else body,
                chatId = data["chatId"].orEmpty(),
                callType = data["callType"].orEmpty().ifBlank { "voice" },
                callId = data["callId"].orEmpty(),
            )
        } else if (isChatMessage) {
            showChatNotification(
                title = title,
                body = body,
                chatId = data["chatId"].orEmpty(),
                senderId = data["senderId"].orEmpty(),
            )
        } else {
            showGenericNotification(title, body)
        }
    }

    private fun showChatNotification(title: String, body: String, chatId: String, senderId: String) {
        showNotification(
            notificationId = stableNotificationId(chatId.ifBlank { senderId.ifBlank { title } }),
            title = title,
            body = body,
            chatId = chatId,
        )
    }

    private fun showGenericNotification(title: String, body: String) {
        showNotification(
            notificationId = stableNotificationId(title + body),
            title = title,
            body = body,
            chatId = "",
        )
    }

    private fun showIncomingCallNotification(
        title: String,
        body: String,
        chatId: String,
        callType: String,
        callId: String,
    ) {
        ensureChannel()

        val deepLink = Uri.parse("kampus://incoming_call/$chatId/$callType?callId=$callId")
        val intent = Intent(Intent.ACTION_VIEW, deepLink, this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val notificationId = stableNotificationId("call_$callId$chatId")
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun showNotification(notificationId: Int, title: String, body: String, chatId: String) {
        ensureChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (chatId.isNotBlank()) {
                putExtra("openChatId", chatId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Chat message notifications"
        }
        manager.createNotificationChannel(channel)
    }

    private fun stableNotificationId(seed: String): Int {
        return seed.hashCode().takeIf { it != Int.MIN_VALUE }?.let { kotlin.math.abs(it) } ?: 0
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    companion object {
        private const val CHANNEL_ID = "kampus_messages"
    }
}
