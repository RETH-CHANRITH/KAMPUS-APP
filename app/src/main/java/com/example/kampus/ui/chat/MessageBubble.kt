@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "NAME_SHADOWING")

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
import androidx.compose.animation.slideInHorizontally
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    selfName: String = "",
    contactName: String = "",
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null,
) {
    val bubbleEnter = if (message.isSentByMe) {
        slideInHorizontally(
            animationSpec = tween(durationMillis = 180),
            initialOffsetX = { fullWidth -> fullWidth / 4 },
        ) + fadeIn(animationSpec = tween(durationMillis = 180))
    } else {
        slideInHorizontally(
            animationSpec = tween(durationMillis = 180),
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
        ) + fadeIn(animationSpec = tween(durationMillis = 180))
    }

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
        Column(
            horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start,
        ) {
            val displayName = if (message.isSentByMe) selfName.ifBlank { "You" } else contactName
            if (displayName.isNotBlank()) {
                Text(
                    text = displayName,
                    color = if (message.isSentByMe) ThemeController.accent.color.copy(alpha = 0.85f) else TimestampColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
                )
            }

            AnimatedVisibility(
                visible = true,
                enter = bubbleEnter,
                exit = fadeOut(animationSpec = tween(durationMillis = 120)),
            ) {
                if (message.isVoice) {
                    VoiceBubble(message)
                } else {
                    TextBubble(message, onLongPress = onLongPress, onReact = onReact)
                }
            }
        }
    }
}

// ─── Message group with avatar ────────────────────────────────────────────────
@Suppress("UNUSED_PARAMETER")
@Composable
fun MessageGroupWithAvatar(
    message: Message,
    isLastInGroup: Boolean = true,
    selfName: String = "",
    contactName: String = "",
    contactProfileImageUrl: String = "",
    contactAvatarEmoji: String = "",
    selfProfileImageUrl: String = "",
    selfAvatarEmoji: String = "",
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null,
) {
    val avatarModifier = Modifier.size(28.dp)
    val showAvatar = true
    val avatarContent = if (message.isSentByMe) {
        selfProfileImageUrl to selfAvatarEmoji
    } else {
        contactProfileImageUrl to contactAvatarEmoji
    }

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
        verticalAlignment = Alignment.Bottom,
    ) {
        if (message.isSentByMe) {
            MessageBubble(
                message = message,
                selfName = selfName,
                contactName = contactName,
                modifier = Modifier.weight(1f, fill = false),
                onLongPress = onLongPress,
                onReact = onReact
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (showAvatar) {
                MessageAvatar(
                    profileImageUrl = avatarContent.first,
                    avatarEmoji = avatarContent.second,
                    modifier = avatarModifier,
                )
            } else {
                Spacer(modifier = avatarModifier)
            }
        } else {
            if (showAvatar) {
                MessageAvatar(
                    profileImageUrl = avatarContent.first,
                    avatarEmoji = avatarContent.second,
                    modifier = avatarModifier,
                )
            } else {
                Spacer(modifier = avatarModifier)
            }
            Spacer(modifier = Modifier.width(8.dp))
            MessageBubble(
                message = message,
                selfName = selfName,
                contactName = contactName,
                modifier = Modifier.weight(1f, fill = false),
                onLongPress = onLongPress,
                onReact = onReact
            )
        }
    }
}

// ─── Text bubble ─────────────────────────────────────────────────────────────
@Composable
private fun TextBubble(message: Message, onLongPress: (() -> Unit)? = null, onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null) {
    val bubbleColor = if (message.isSentByMe) BubbleSent else BubbleReceived
    val isImageAttachment = message.mediaType.startsWith("image", ignoreCase = true) ||
        (message.mediaType.isBlank() && message.text.equals("Photo", ignoreCase = true))
    val shouldShowAttachment = message.mediaUrl.isNotBlank() || isImageAttachment
    val shape = if (message.isSentByMe)
        RoundedCornerShape(22.dp, 8.dp, 22.dp, 22.dp)
    else
        RoundedCornerShape(8.dp, 22.dp, 22.dp, 22.dp)

    Column(
        horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start,
        modifier = Modifier.animateContentSize(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = { onLongPress?.invoke() },
                )
                .clip(shape)
                .background(bubbleColor)
                .border(1.dp, Color.White.copy(alpha = if (message.isSentByMe) 0.04f else 0.06f), shape)
                .padding(horizontal = 13.dp, vertical = 9.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.isStoryReply || message.storyId.isNotBlank()) {
                    StoryReplyPreview(message = message)
                }

                if (shouldShowAttachment) {
                    AttachmentPreview(message = message)
                }

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isHiddenForMe = currentUserId != null && message.hiddenTextFor.contains(currentUserId)

                if (!isHiddenForMe && message.text.isNotBlank() && !isImageAttachment && !message.isStoryReply) {
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
private fun StoryReplyPreview(message: Message) {
    val previewUrl = message.storyImage.ifBlank { message.storyThumbnail }
    val previewCaption = message.storyCaption.ifBlank { "Story" }
    val replyText = message.storyReplyText.ifBlank { message.text }
    val previewBackground = if (message.isSentByMe) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val previewTextColor = if (message.isSentByMe) Color.White else if (UiIsDark) Color.White else Color(0xFF111827)
    val previewSubtextColor = if (message.isSentByMe) Color.White.copy(alpha = 0.78f) else if (UiIsDark) Color.White.copy(alpha = 0.78f) else Color(0xFF4B5563)

    Surface(
        color = previewBackground,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (message.isSentByMe) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (previewUrl.isNotBlank()) {
                        AsyncImage(
                            model = previewUrl,
                            contentDescription = "Story preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = previewTextColor)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Story reply", color = previewTextColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(previewCaption, color = previewSubtextColor, fontSize = 12.sp, maxLines = 2)
                }
            }

            if (replyText.isNotBlank()) {
                Text(
                    text = replyText,
                    color = previewTextColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun MessageAvatar(
    profileImageUrl: String,
    avatarEmoji: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (UiIsDark) Color(0xFF1E2A3B) else Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = avatarEmoji.ifBlank { "👤" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
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
    val attachmentContext = LocalContext.current
    val mediaType = if (message.mediaType.isBlank() && message.text == "Photo") "image" else message.mediaType.lowercase()
    val previewShape = RoundedCornerShape(14.dp)
    val previewBackground = if (message.isSentByMe) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val attachmentTextColor = if (message.isSentByMe) Color.White else if (UiIsDark) Color.White else Color(0xFF111827)
    val attachmentSubtextColor = if (message.isSentByMe) Color.White.copy(alpha = 0.78f) else if (UiIsDark) Color.White.copy(alpha = 0.78f) else Color(0xFF4B5563)
    val attachmentIconTint = if (message.isSentByMe) Color.White else if (UiIsDark) Color.White else Color(0xFF111827)
    var showPhotoDialog by remember(message.remoteMessageId, message.mediaUrl) { mutableStateOf(false) }

    when {
        mediaType.startsWith("image", ignoreCase = true) -> {
            Surface(
                color = previewBackground,
                shape = previewShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = message.mediaUrl.isNotBlank()) { showPhotoDialog = true },
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    ProfessionalPhotoDisplay(message = message)
                }
            }
            if (showPhotoDialog && message.mediaUrl.isNotBlank()) {
                Dialog(
                    onDismissRequest = { showPhotoDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable { showPhotoDialog = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(attachmentContext)
                                .data(message.mediaUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = message.mediaName.ifBlank { "Photo" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }

        mediaType.startsWith("video", ignoreCase = true) -> {
            Surface(
                color = previewBackground,
                shape = previewShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = message.mediaUrl.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.parse(message.mediaUrl), "video/*")
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            attachmentContext.startActivity(intent)
                        } catch (e: Exception) {
                            try { android.widget.Toast.makeText(attachmentContext, "Unable to open video", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                        }
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

        mediaType.startsWith("audio", ignoreCase = true) || mediaType.startsWith("application", ignoreCase = true) -> {
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

        mediaType.startsWith("location", ignoreCase = true) -> {
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
    val photoContext = LocalContext.current
    val imageModel = remember(message.mediaUrl) {
        val url = message.mediaUrl.trim()
        when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("file:") || url.startsWith("content:") -> Uri.parse(url)
            url.isNotBlank() && java.io.File(url).exists() -> java.io.File(url)
            else -> url
        }
    }
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
                        model = ImageRequest.Builder(photoContext)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = message.mediaName.ifBlank { "Photo" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 320.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.98f,
                        onError = {
                            android.util.Log.w("MessageBubble", "Photo load failed for ${message.mediaUrl}", it.result.throwable)
                        },
                    )
                } else {
                    // Placeholder when URL missing — looks clean and offers retry affordance
                    val placeholderContext = photoContext
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
                                        android.widget.Toast.makeText(placeholderContext, "No image URL to load", android.widget.Toast.LENGTH_SHORT).show()
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
    val voiceContext = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackSource = remember(message.voiceUrl, message.mediaUrl) {
        message.voiceUrl.ifBlank { message.mediaUrl }
    }
    var playing by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
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
                        .clickable(enabled = playbackSource.isNotBlank() && !preparing) {
                                if (playing) {
                                    try { mediaPlayer?.stop() } catch (_: Exception) {}
                                    try { mediaPlayer?.release() } catch (_: Exception) {}
                                    mediaPlayer = null
                                    playing = false
                                } else {
                                    preparing = true
                                    android.util.Log.d("VoicePlayback", "🎵 Starting playback: url=$playbackSource")
                                    try { mediaPlayer?.release() } catch (_: Exception) {}
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
                                            preparing = false
                                        } catch (e: Exception) {
                                            android.util.Log.e("VoicePlayback", "❌ Start after prepared failed: ${e.message}")
                                            preparing = false
                                        }
                                    }

                                    player.setOnCompletionListener { mp ->
                                        android.util.Log.d("VoicePlayback", "✅ Playback completed")
                                        playing = false
                                        preparing = false
                                        try { mp.release() } catch (_: Exception) {}
                                        mediaPlayer = null
                                    }

                                    player.setOnErrorListener { mp, _, _ ->
                                        android.util.Log.e("VoicePlayback", "❌ Playback error")
                                        playing = false
                                        preparing = false
                                        try { mp.release() } catch (_: Exception) {}
                                        mediaPlayer = null
                                        true
                                    }

                                    // Try direct URL first; if that fails, download on IO dispatcher and play local file
                                    try {
                                        // Accept file:// URIs, http(s) URLs, or plain file paths
                                        when {
                                            playbackSource.startsWith("file:") -> {
                                                val localFile = java.io.File(Uri.parse(playbackSource).path.orEmpty())
                                                if (!localFile.exists()) {
                                                    if (message.mediaUrl.isNotBlank() && message.mediaUrl.startsWith("http")) {
                                                        player.setDataSource(message.mediaUrl)
                                                    } else {
                                                        android.widget.Toast.makeText(voiceContext, "Voice file is no longer available", android.widget.Toast.LENGTH_SHORT).show()
                                                        try { player.release() } catch (_: Exception) {}
                                                        return@clickable
                                                    }
                                                } else {
                                                    player.setDataSource(localFile.absolutePath)
                                                }
                                            }
                                            playbackSource.startsWith("http") -> {
                                                player.setDataSource(playbackSource)
                                            }
                                            else -> {
                                                val localFile = java.io.File(playbackSource)
                                                if (!localFile.exists()) {
                                                    if (message.mediaUrl.isNotBlank() && message.mediaUrl.startsWith("http")) {
                                                        player.setDataSource(message.mediaUrl)
                                                    } else {
                                                        android.widget.Toast.makeText(voiceContext, "Voice file is no longer available", android.widget.Toast.LENGTH_SHORT).show()
                                                        try { player.release() } catch (_: Exception) {}
                                                        return@clickable
                                                    }
                                                } else {
                                                    player.setDataSource(localFile.absolutePath)
                                                }
                                            }
                                        }
                                        player.prepareAsync()
                                        mediaPlayer = player
                                    } catch (e: Exception) {
                                        android.util.Log.w("VoicePlayback", "⚠️ URL/file playback failed, scheduling cache download if remote: ${e.message}")
                                        // Only attempt HTTP download fallback for remote URLs
                                        if (playbackSource.startsWith("http")) {
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val cacheFile = java.io.File(voiceContext.cacheDir, "voice_${message.remoteMessageId}_${message.timestampMillis}.m4a")
                                                    if (!cacheFile.exists()) {
                                                        android.util.Log.d("VoicePlayback", "📥 Downloading to cache (bg): ${cacheFile.absolutePath}")
                                                        java.net.URL(playbackSource).openStream().use { input ->
                                                            java.io.FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                                                        }
                                                        android.util.Log.d("VoicePlayback", "✅ Downloaded (bg): ${cacheFile.length()} bytes")
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        try {
                                                            val cachePlayer = MediaPlayer()
                                                            cachePlayer.setAudioAttributes(
                                                                AudioAttributes.Builder()
                                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                                    .build()
                                                            )
                                                            cachePlayer.setOnPreparedListener { mp -> try { mp.start(); playing = true } catch (_: Exception) {} }
                                                            cachePlayer.setOnPreparedListener { mp -> try { mp.start(); playing = true; preparing = false } catch (_: Exception) { preparing = false } }
                                                            cachePlayer.setOnCompletionListener { mp -> try { mp.release() } catch (_: Exception) {}; mediaPlayer = null; playing = false; preparing = false }
                                                            cachePlayer.setOnErrorListener { mp, _, _ -> try { mp.release() } catch (_: Exception) {}; mediaPlayer = null; playing = false; preparing = false; true }
                                                            cachePlayer.setDataSource(cacheFile.absolutePath)
                                                            mediaPlayer = cachePlayer
                                                            cachePlayer.prepareAsync()
                                                        } catch (pe: Exception) {
                                                            android.util.Log.e("VoicePlayback", "❌ Start from cache failed: ${pe.message}")
                                                            preparing = false
                                                            try { player.release() } catch (_: Exception) {}
                                                        }
                                                    }
                                                } catch (fallbackE: Exception) {
                                                    android.util.Log.e("VoicePlayback", "❌ Background fallback failed: ${fallbackE.message}")
                                                    preparing = false
                                                    try { player.release() } catch (_: Exception) {}
                                                }
                                            }
                                        } else {
                                            android.util.Log.w("VoicePlayback", "Not attempting download fallback because voiceUrl is not remote: $playbackSource")
                                            preparing = false
                                            try { player.release() } catch (_: Exception) {}
                                        }
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
    val locationContext = LocalContext.current
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
                        context = locationContext,
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
    val staticLocationContext = LocalContext.current
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
                    context = staticLocationContext,
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