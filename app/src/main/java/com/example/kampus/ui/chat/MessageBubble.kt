package com.example.kampus.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.Spring
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import com.example.kampus.ui.chat.Message
import com.example.kampus.ui.theme.ThemeController
import coil.compose.AsyncImage
import java.io.IOException
import com.google.firebase.auth.FirebaseAuth

// ─── Colour tokens ───────────────────────────────────────────────────────────
private val UiIsDark        get() = ThemeController.isDark
private val BubbleSent      get() = ThemeController.accent.color
private val BubbleReceived  get() = if (UiIsDark) Color(0xFF1E2A3B) else Color(0xFFE5E7EB)
private val TimestampColor  get() = if (UiIsDark) Color(0xFF6B7A99) else Color(0xFF6B7280)

// ─── Voice waveform bar heights (static decoration) ──────────────────────────
private val waveHeights = listOf(
    0.4f, 0.7f, 0.5f, 1.0f, 0.6f, 0.8f, 0.5f, 0.9f,
    0.4f, 0.7f, 0.6f, 0.8f, 0.5f, 0.3f, 0.7f, 0.9f,
    0.6f, 0.4f, 0.8f, 0.5f,
)

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongPress != null && message.isVoice) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onLongPress,
                    )
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = if (message.isSentByMe) Arrangement.End else Arrangement.Start,
    ) {
        if (message.isVoice) {
            VoiceBubble(message)
        } else {
            TextBubble(message, onLongPress = onLongPress, onReact = onReact)
        }
    }
}

// ─── Message group with avatar ────────────────────────────────────────────────
@Composable
fun MessageGroupWithAvatar(
    message: Message,
    isLastInGroup: Boolean = true,
    contactProfileImageUrl: String = "",
    contactAvatarEmoji: String = "",
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null,
) {
    if (message.isSentByMe) {
        // Sent messages: no avatar needed
        MessageBubble(message = message, modifier = modifier, onLongPress = onLongPress, onReact = onReact)
    } else {
        // Received messages: show avatar on last message in group
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (onLongPress != null && message.isVoice) {
                        Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onLongPress,
                        )
                    } else {
                        Modifier
                    }
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (isLastInGroup) {
                Surface(
                    shape = CircleShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 8.dp),
                ) {
                    if (contactProfileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = contactProfileImageUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    } else if (contactAvatarEmoji.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (UiIsDark) Color(0xFF1E2A3B) else Color(0xFFE5E7EB),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = contactAvatarEmoji,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = Color(0xFF8B5CF6),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(36.dp))
            }

            MessageBubble(message = message, onLongPress = onLongPress, onReact = onReact)
        }
    }
}

// ─── Text bubble ─────────────────────────────────────────────────────────────
@Composable
private fun TextBubble(message: Message, onLongPress: (() -> Unit)? = null, onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null) {
    val bubbleColor = if (message.isSentByMe) BubbleSent else BubbleReceived
    val isImageAttachment = message.mediaType.equals("image", ignoreCase = true) ||
        (message.mediaType.isBlank() && message.text.equals("Photo", ignoreCase = true))
    val shouldShowAttachment = message.mediaUrl.isNotBlank() || isImageAttachment
    val shape = if (message.isSentByMe)
        RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    else
        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

    Column(
        horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start,
        modifier = Modifier.animateContentSize(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = { onLongPress?.invoke() },
                )
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shouldShowAttachment) {
                    AttachmentPreview(message = message)
                }

                // Only show text if this is not an image attachment.
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isHiddenForMe = currentUserId != null && message.hiddenTextFor.contains(currentUserId)

                if (!isHiddenForMe && message.text.isNotBlank() && !isImageAttachment) {
                    Text(
                        text       = message.text,
                        color      = if (message.isSentByMe) Color.White else if (UiIsDark) Color.White else Color(0xFF111827),
                        fontSize   = 15.sp,
                        lineHeight = 22.sp,
                    )
                } else if (isHiddenForMe && !isImageAttachment) {
                    Text(
                        text = "[Hidden]",
                        color = TimestampColor,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Show all persisted reactions as tappable chips (emoji + count). Highlight if current user reacted.
        if (message.reactions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                message.reactions.entries.forEach { entry ->
                    val emoji = entry.key
                    val users = entry.value
                    val count = users.size
                    val mine = currentUserId != null && users.contains(currentUserId)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (mine) ThemeController.accent.color.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                            .clickable { onReact?.invoke(message.remoteMessageId, emoji) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (count > 1) "$emoji $count" else emoji, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text     = message.timestamp,
                color    = TimestampColor,
                fontSize = 11.sp,
            )
            if (message.isEdited) {
                Text(
                    text = "edited",
                    color = TimestampColor,
                    fontSize = 10.sp,
                )
            }
            if (message.isSentByMe) {
                DeliveryStatusChip(
                    deliveryState = when {
                        message.deliveryState == MessageDeliveryState.Delivered && message.isRead -> MessageDeliveryState.Seen
                        else -> message.deliveryState
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionPicker(
    reaction: String,
    isSentByMe: Boolean,
    onSelect: (String) -> Unit,
) {
    val reactions = listOf("👍", "❤️", "😂", "😮", "😢")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSentByMe) Color(0xFF0F172A) else Color(0xFF111827))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        reactions.forEach { emoji ->
            val selected = reaction == emoji
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) {
                            ThemeController.accent.color.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onSelect(emoji) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = emoji, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AttachmentPreview(message: Message) {
    val mediaType = if (message.mediaType.isBlank() && message.text == "Photo") "image" else message.mediaType.lowercase()
    val previewShape = RoundedCornerShape(14.dp)
    val previewBackground = if (message.isSentByMe) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val attachmentTextColor = if (message.isSentByMe) Color.White else if (UiIsDark) Color.White else Color(0xFF111827)
    val attachmentSubtextColor = if (message.isSentByMe) Color.White.copy(alpha = 0.78f) else if (UiIsDark) Color.White.copy(alpha = 0.78f) else Color(0xFF4B5563)
    val attachmentIconTint = if (message.isSentByMe) Color.White else if (UiIsDark) Color.White else Color(0xFF111827)

    when (mediaType) {
        "image" -> {
            Surface(
                color = previewBackground,
                shape = previewShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    ProfessionalPhotoDisplay(message = message)
                }
            }
        }

        "video" -> {
            Surface(
                color = previewBackground,
                shape = previewShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = attachmentIconTint)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Video", color = attachmentTextColor, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = message.mediaName.ifBlank { "Tap to open" },
                            color = attachmentSubtextColor,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        "file" -> {
            Surface(
                color = previewBackground,
                shape = previewShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = attachmentIconTint)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("File", color = attachmentTextColor, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = message.mediaName.ifBlank { "Document" },
                            color = attachmentSubtextColor,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        "location" -> {
            if (message.isLiveLocation && message.locationRemainingSeconds > 0) {
                LiveLocationDisplay(message = message)
            } else {
                StaticLocationDisplay(message = message, attachmentTextColor = attachmentTextColor, attachmentSubtextColor = attachmentSubtextColor, attachmentIconTint = attachmentIconTint)
            }
        }

        else -> {
            Surface(
                color = previewBackground,
                shape = previewShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = attachmentIconTint)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Attachment", color = attachmentTextColor, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = message.mediaName.ifBlank { message.mediaUrl.takeLast(18) },
                            color = attachmentSubtextColor,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─── Professional photo display with floating share button ──────────────────
@Composable
private fun ProfessionalPhotoDisplay(message: Message) {
    Box {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            shadowElevation = 3.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .widthIn(max = 280.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
            ) {
                if (message.mediaUrl.isNotBlank()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = message.mediaName.ifBlank { "Photo" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 320.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.98f,
                    )
                } else {
                    // Placeholder when URL missing — looks clean and offers retry affordance
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F1724)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No image",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Photo not available",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap to retry",
                            color = ThemeController.accent.color,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable {
                                    try {
                                        android.widget.Toast.makeText(context, "No image URL to load", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                }
                        )
                    }
                }
            }
        }
    }
}

// ─── Voice bubble ─────────────────────────────────────────────────────────────
@Composable
private fun VoiceBubble(message: Message) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPosition by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }
    
    // Real-time progress update every 50ms
    LaunchedEffect(playing) {
        while (playing) {
            try {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    currentPosition = mp.currentPosition.toFloat()
                    if (duration == 0f) {
                        duration = mp.duration.toFloat().coerceAtLeast(1f)
                    }
                }
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(50)
        }
        currentPosition = 0f
    }
    
    val progress = if (duration > 0) (currentPosition / duration).coerceIn(0f, 1f) else 0f

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    val bubbleColor = if (message.isSentByMe) BubbleSent else Color(0xFF2563EB)
    val playButtonColor = if (message.isSentByMe) Color.White else Color.White
    val wavePlayedColor = if (message.isSentByMe) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val waveUnplayedColor = if (message.isSentByMe) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.4f)

    Column(
        horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start,
        modifier = Modifier.animateContentSize()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isSentByMe) 18.dp else 4.dp,
                        bottomEnd = if (message.isSentByMe) 4.dp else 18.dp,
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Play button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (message.isSentByMe) 
                                Color.White.copy(alpha = 0.25f) 
                            else 
                                Color(0xFF60A5FA)
                        )
                        .clickable(enabled = message.voiceUrl.isNotBlank()) {
                            if (message.voiceUrl.isNotBlank()) {
                                if (playing) {
                                    try {
                                        mediaPlayer?.stop()
                                    } catch (_: Exception) {
                                    }
                                    try {
                                        mediaPlayer?.release()
                                    } catch (_: Exception) {
                                    }
                                    mediaPlayer = null
                                    playing = false
                                } else {
                                    android.util.Log.d("VoicePlayback", "🎵 Starting playback: url=${message.voiceUrl}")
                                    try {
                                        mediaPlayer?.release()
                                    } catch (_: Exception) {
                                    }
                                    val player = MediaPlayer()
                                    player.setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                            .build()
                                    )

                                    player.setOnPreparedListener { mp ->
                                        try {
                                            mp.start()
                                            android.util.Log.d("VoicePlayback", "▶️ Playback started (prepared)")
                                            playing = true
                                        } catch (e: Exception) {
                                            android.util.Log.e("VoicePlayback", "❌ Start after prepared failed: ${e.message}")
                                        }
                                    }

                                    player.setOnCompletionListener { mp ->
                                        android.util.Log.d("VoicePlayback", "✅ Playback completed")
                                        playing = false
                                        try {
                                            mp.release()
                                        } catch (_: Exception) {
                                        }
                                        mediaPlayer = null
                                    }

                                    player.setOnErrorListener { mp, what, extra ->
                                        android.util.Log.e("VoicePlayback", "❌ Playback error: what=$what extra=$extra")
                                        playing = false
                                        try {
                                            mp.release()
                                        } catch (_: Exception) {
                                        }
                                        mediaPlayer = null
                                        true
                                    }

                                    try {
                                        player.setDataSource(message.voiceUrl)
                                        player.prepareAsync()
                                    } catch (e: Exception) {
                                        android.util.Log.w("VoicePlayback", "⚠️ URL playback failed, trying cache fallback: ${e.message}")
                                        try {
                                            val cacheFile = java.io.File(
                                                context.cacheDir,
                                                "voice_${message.remoteMessageId}_${message.timestampMillis}.m4a"
                                            )
                                            if (!cacheFile.exists()) {
                                                android.util.Log.d("VoicePlayback", "📥 Downloading to cache: ${cacheFile.absolutePath}")
                                                java.net.URL(message.voiceUrl).openStream().use { input ->
                                                    java.io.FileOutputStream(cacheFile).use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                android.util.Log.d("VoicePlayback", "✅ Downloaded: ${cacheFile.length()} bytes")
                                            }

                                            val cachePlayer = MediaPlayer()
                                            cachePlayer.setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                    .build()
                                            )
                                            cachePlayer.setOnPreparedListener { mp ->
                                                try {
                                                    mp.start()
                                                    android.util.Log.d("VoicePlayback", "▶️ Playback started from cache (prepared)")
                                                    playing = true
                                                } catch (pe: Exception) {
                                                    android.util.Log.e("VoicePlayback", "❌ Start from cache failed: ${pe.message}")
                                                }
                                            }
                                            cachePlayer.setOnCompletionListener { mp ->
                                                android.util.Log.d("VoicePlayback", "✅ Cache playback completed")
                                                playing = false
                                                try { mp.release() } catch (_: Exception) {}
                                                mediaPlayer = null
                                            }
                                            cachePlayer.setOnErrorListener { mp, what, extra ->
                                                android.util.Log.e("VoicePlayback", "❌ Cache playback error: what=$what extra=$extra")
                                                playing = false
                                                try { mp.release() } catch (_: Exception) {}
                                                mediaPlayer = null
                                                true
                                            }

                                            cachePlayer.setDataSource(cacheFile.absolutePath)
                                            cachePlayer.prepareAsync()
                                            mediaPlayer = cachePlayer
                                        } catch (fallbackE: Exception) {
                                            android.util.Log.e("VoicePlayback", "❌ Fallback failed: ${fallbackE.message}")
                                        }
                                    }

                                    mediaPlayer = player
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Stop" else "Play",
                        tint = playButtonColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Waveform bars - responsive to actual playback
                Row(
                    modifier = Modifier.height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                ) {
                    waveHeights.forEachIndexed { index, height ->
                        val fraction = index.toFloat() / waveHeights.size
                        val isPlayed = progress > fraction
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight(height)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(
                                    if (isPlayed) wavePlayedColor else waveUnplayedColor
                                )
                        )
                    }
                }

                if (message.voiceDuration.isNotBlank()) {
                    Text(
                        text = message.voiceDuration,
                        color = if (message.isSentByMe) Color.White else TimestampColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = if (playing) "Playing..." else "Tap to play",
                    color = if (message.isSentByMe) Color.White.copy(alpha = 0.85f) else TimestampColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = message.timestamp,
                color = TimestampColor,
                fontSize = 11.sp,
            )
            if (message.isSentByMe) {
                DeliveryStatusChip(
                    deliveryState = when {
                        message.deliveryState == MessageDeliveryState.Delivered && message.isRead -> MessageDeliveryState.Seen
                        else -> message.deliveryState
                    }
                )
            }
        }
    }

}

@Composable
private fun DeliveryStatusChip(deliveryState: MessageDeliveryState) {
    AnimatedContent(targetState = deliveryState, label = "delivery_status") { state ->
        when (state) {
            MessageDeliveryState.Sending -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Sending",
                        tint = TimestampColor,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Sending",
                        color = TimestampColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            MessageDeliveryState.Delivered -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Delivered",
                        tint = TimestampColor,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Delivered",
                        color = TimestampColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            MessageDeliveryState.Seen -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Seen",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Seen",
                        color = Color(0xFF60A5FA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            MessageDeliveryState.Failed -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Failed",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Failed",
                        color = Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ─── Live Location Display with Map Preview and Timer ──────────────────────
@Composable
private fun LiveLocationDisplay(message: Message) {
    val context = LocalContext.current
    val durationSeconds = message.locationRemainingSeconds.coerceAtLeast(0L)
    var tick by remember(message.timestampMillis, durationSeconds) { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(message.timestampMillis, durationSeconds) {
        while (true) {
            tick = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val elapsedSeconds = ((tick - message.timestampMillis) / 1000L).coerceAtLeast(0L)
    val remainingTime = (durationSeconds - elapsedSeconds).coerceAtLeast(0L)
    val minutes = remainingTime / 60
    val seconds = remainingTime % 60
    val timeText = when {
        remainingTime <= 0L -> "Expired"
        minutes > 0 -> "$minutes min remaining"
        else -> "$seconds secs remaining"
    }
    
    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E3A8A))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Map preview (placeholder)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable {
                    openLocationInMaps(
                        context = context,
                        latitude = message.locationLatitude,
                        longitude = message.locationLongitude,
                        label = message.mediaName.ifBlank { "Shared location" },
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.mediaName.ifBlank { "Shared location" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Lat: ${String.format("%.4f", message.locationLatitude)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                Text(
                    text = "Lng: ${String.format("%.4f", message.locationLongitude)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
        
        // Timer and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Live",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Live location",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = timeText,
                color = Color(0xFF60A5FA),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // Action button
        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF60A5FA)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (message.isSentByMe) "Stop sharing" else "Open map",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Static Location Display (non-live) ───────────────────────────────────
@Composable
private fun StaticLocationDisplay(
    message: Message,
    attachmentTextColor: androidx.compose.ui.graphics.Color,
    attachmentSubtextColor: androidx.compose.ui.graphics.Color,
    attachmentIconTint: androidx.compose.ui.graphics.Color
) {
    val context = LocalContext.current
    val previewShape = RoundedCornerShape(14.dp)
    val previewBackground = if (message.isSentByMe) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    
    Surface(
        color = previewBackground,
        shape = previewShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                openLocationInMaps(
                    context = context,
                    latitude = message.locationLatitude,
                    longitude = message.locationLongitude,
                    label = message.mediaName.ifBlank { "Shared location" },
                )
            },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = attachmentIconTint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Location", color = attachmentTextColor, fontWeight = FontWeight.SemiBold)
                Text(
                    text = message.mediaName.ifBlank { "Shared location" },
                    color = attachmentSubtextColor,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun openLocationInMaps(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    label: String,
) {
    val encodedLabel = Uri.encode(label)
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    try {
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            )
            context.startActivity(browserIntent)
        }
    } catch (_: Exception) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"))
            )
        }
    }
}