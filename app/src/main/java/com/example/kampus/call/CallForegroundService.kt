package com.example.kampus.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import com.example.kampus.MainActivity
import com.example.kampus.R

class CallForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startAsForeground("Call active", "Preparing call...")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val chatId = intent?.getStringExtra(EXTRA_CHAT_ID).orEmpty()
        val callType = intent?.getStringExtra(EXTRA_CALL_TYPE).orEmpty().ifBlank { "voice" }
        val notificationText = intent?.getStringExtra(EXTRA_STATUS).orEmpty().ifBlank { "Connected" }
        val title = if (callType.equals("video", ignoreCase = true)) {
            "Video call in progress"
        } else {
            "Voice call in progress"
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (chatId.isNotBlank()) {
                putExtra("openChatId", chatId)
            }
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            91,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag(),
        )

        startAsForeground(title, notificationText, contentIntent)
        return START_STICKY
    }

    private fun startAsForeground(title: String, text: String, contentIntent: PendingIntent? = null) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        val foregroundType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active calls",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps ongoing voice and video calls alive"
            },
        )
    }

    private fun immutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    companion object {
        const val ACTION_START = "com.example.kampus.call.action.START"
        const val ACTION_STOP = "com.example.kampus.call.action.STOP"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_STATUS = "extra_status"

        private const val CHANNEL_ID = "kampus_call_service"
        private const val NOTIFICATION_ID = 9201

        fun start(context: Context, chatId: String, callId: String, callType: String, status: String) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CHAT_ID, chatId)
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_STATUS, status)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}