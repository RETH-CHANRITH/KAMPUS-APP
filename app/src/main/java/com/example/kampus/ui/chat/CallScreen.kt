package com.example.kampus.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.data.repository.CallStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.kampus.call.RingtonePlayer
import androidx.compose.ui.geometry.Offset
import com.example.kampus.ui.chat.DraggableLocalPreview
import com.example.kampus.ui.chat.CallHistoryCard
import com.example.kampus.ui.chat.CallHistoryItem
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kampus.ui.theme.ThemeController
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    chatId: String,
    callType: String,
    callId: String = "",
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    LaunchedEffect(chatId) {
        viewModel.openChat(chatId)
    }

    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    var callStartedAt by remember(chatId) { mutableLongStateOf(0L) }
    var now by remember(chatId) { mutableLongStateOf(System.currentTimeMillis()) }
    var muted by remember(chatId) { mutableStateOf(false) }
    var speakerOn by remember(chatId) { mutableStateOf(true) }
    var cameraFront by remember(chatId) { mutableStateOf(true) }
    var active by remember(chatId) { mutableStateOf(false) }
    var remoteCallStatus by remember(callId) { mutableStateOf<CallStatus?>(null) }

    LaunchedEffect(callId) {
        if (callId.isBlank()) {
            delay(900)
            active = true
            callStartedAt = System.currentTimeMillis()
            return@LaunchedEffect
        }

        viewModel.observeCallStatus(chatId, callId).collect { result ->
            result.onSuccess { status ->
                remoteCallStatus = status
                when (status) {
                    CallStatus.ACCEPTED -> {
                        if (!active) {
                            active = true
                            callStartedAt = System.currentTimeMillis()
                        }
                    }

                    CallStatus.DECLINED,
                    CallStatus.MISSED,
                    CallStatus.ENDED -> {
                        active = false
                    }

                    CallStatus.RINGING -> {
                    }
                }
            }
        }
    }

    LaunchedEffect(remoteCallStatus) {
        when (remoteCallStatus) {
            CallStatus.DECLINED -> {
                delay(1100)
                onBack()
            }

            CallStatus.MISSED -> {
                delay(1100)
                onBack()
            }

            CallStatus.ENDED -> {
                delay(800)
                onBack()
            }

            else -> {
            }
        }
    }

    LaunchedEffect(active) {
        while (active) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val accent = ThemeController.accent.color
    val isVideo = callType.equals("video", ignoreCase = true)
    val timerText = formatCallTimer(((now - callStartedAt) / 1000L).coerceAtLeast(0L))
    val statusText = when (remoteCallStatus) {
        CallStatus.RINGING -> "Ringing..."
        CallStatus.ACCEPTED -> timerText
        CallStatus.DECLINED -> "Declined"
        CallStatus.MISSED -> "Missed call"
        CallStatus.ENDED -> "Call ended"
        null -> timerText
    }
    val name = chatState.contactName.ifBlank { "Calling" }
    val avatar = chatState.contactAvatarEmoji.ifBlank { "👤" }
    val image = chatState.contactProfileImageUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040814), Color(0xFF08111F), Color(0xFF0D1B34)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (isVideo) "Video call" else "Voice call",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            if (isVideo) {
                VideoBody(
                    name = name,
                    avatar = avatar,
                    image = image,
                    timerText = statusText,
                    muted = muted,
                    speakerOn = speakerOn,
                    cameraFront = cameraFront,
                    onMuteToggle = { muted = !muted },
                    onSpeakerToggle = { speakerOn = !speakerOn },
                    onSwitchCamera = { cameraFront = !cameraFront },
                    onEndCall = {
                        if (callId.isNotBlank()) viewModel.endCall(chatId, callId)
                        onBack()
                    },
                    accent = accent,
                )
            } else {
                VoiceBody(
                    name = name,
                    avatar = avatar,
                    image = image,
                    timerText = statusText,
                    muted = muted,
                    speakerOn = speakerOn,
                    onMuteToggle = { muted = !muted },
                    onSpeakerToggle = { speakerOn = !speakerOn },
                    onEndCall = {
                        if (callId.isNotBlank()) viewModel.endCall(chatId, callId)
                        onBack()
                    },
                    accent = accent,
                )
            }
            // lightweight call history preview
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Recent calls", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
                CallHistoryCard(CallHistoryItem("Alex Morgan", "2h ago", "Missed"))
                CallHistoryCard(CallHistoryItem("Sam Lee", "Yesterday", "Outgoing"))
            }
        }
    }
}
@Composable
fun IncomingCallScreen(
    chatId: String,
    callType: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    LaunchedEffect(chatId) {
        viewModel.openChat(chatId)
    }

    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val accent = ThemeController.accent.color
    val isVideo = callType.equals("video", ignoreCase = true)
    val context = LocalContext.current
    val pulseTransition = rememberInfiniteTransition(label = "incoming_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "incoming_pulse_scale",
    )
    val glow by pulseTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "incoming_glow",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050A14), Color(0xFF08111F), Color(0xFF0D1B34)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(
                    modifier = Modifier
                        .size((176.dp * glow) * pulse)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(138.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (chatState.contactProfileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = chatState.contactProfileImageUrl,
                                contentDescription = chatState.contactName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(text = chatState.contactAvatarEmoji.ifBlank { "👤" }, color = Color.White, fontSize = 40.sp)
                        }
                    }
                }

                Text(
                    text = chatState.contactName.ifBlank { "Unknown contact" },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isVideo) "Incoming video call" else "Incoming voice call",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 15.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "Ringing through WebRTC / WebSocket",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            
            LaunchedEffect(Unit) {
                try {
                    val ringtoneRes = context.resources.getIdentifier("ringtone_incoming", "raw", context.packageName)
                    if (ringtoneRes != 0) {
                        RingtonePlayer.play(context, ringtoneRes)
                    }
                } catch (_: Exception) {
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    RingtonePlayer.stop()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ControlButton(
                    icon = Icons.Filled.CallEnd,
                    label = "Decline",
                    onClick = onDecline,
                    containerColor = Color(0xFFF04444),
                    iconTint = Color.White,
                    labelColor = Color.White,
                )
                ControlButton(
                    icon = Icons.Filled.Call,
                    label = "Accept",
                    onClick = {
                        RingtonePlayer.stop()
                        onAccept()
                    },
                    containerColor = Color(0xFF22C55E),
                    iconTint = Color.White,
                    labelColor = Color.White,
                )
            }
        }
    }
}


@Composable
private fun VoiceBody(
    name: String,
    avatar: String,
    image: String,
    timerText: String,
    muted: Boolean,
    speakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
    accent: Color,
) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val wave1 by transition.animateFloat(0.40f, 1.0f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "wave1")
    val wave2 by transition.animateFloat(0.55f, 0.95f, infiniteRepeatable(tween(1150), RepeatMode.Reverse), label = "wave2")
    val wave3 by transition.animateFloat(0.65f, 1.0f, infiniteRepeatable(tween(1350), RepeatMode.Reverse), label = "wave3")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        CallAvatar(name, avatar, image, accent, 188.dp)
        Text(text = name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(text = timerText, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                VoiceBar(wave1, accent)
                VoiceBar(wave2, accent)
                VoiceBar(wave3, accent)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        CallControlRow(
            muted = muted,
            speakerOn = speakerOn,
            onMuteToggle = onMuteToggle,
            onSpeakerToggle = onSpeakerToggle,
            onSwitchCamera = null,
            onEndCall = onEndCall,
        )
    }
}

@Composable
private fun VideoBody(
    name: String,
    avatar: String,
    image: String,
    timerText: String,
    muted: Boolean,
    speakerOn: Boolean,
    cameraFront: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
    accent: Color,
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text = name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text(text = timerText, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 18.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0E1730), Color(0xFF13264B), Color(0xFF08111F)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(34.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CallAvatar(name, avatar, image, accent, 136.dp)
                    Text(text = "Remote video", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Encrypted live connection", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                }
            }
        }

        // Draggable local preview (placeholder) to simulate small local camera view
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            DraggableLocalPreview(content = {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Self", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                        Text(text = if (cameraFront) "Front" else "Rear", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Brush.radialGradient(colors = listOf(Color(0xFF6EA6FF), Color(0xFF0F1831)))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, tint = Color.White)
                    }
                }
            }, initialOffset = Offset(-24f, 8f))
        }

        CallControlRow(
            muted = muted,
            speakerOn = speakerOn,
            onMuteToggle = onMuteToggle,
            onSpeakerToggle = onSpeakerToggle,
            onSwitchCamera = onSwitchCamera,
            onEndCall = onEndCall,
        )
    }
}

@Composable
private fun CallAvatar(
    name: String,
    avatar: String,
    image: String,
    accent: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.30f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (image.isNotBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(text = avatar.ifBlank { "👤" }, color = Color.White, fontSize = 38.sp)
            }
        }
    }
}

@Composable
private fun VoiceBar(level: Float, accent: Color) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height((16.dp + (14.dp * level)))
            .clip(RoundedCornerShape(99.dp))
            .background(Brush.verticalGradient(colors = listOf(accent.copy(alpha = 0.95f), Color.White.copy(alpha = 0.60f)))),
    )
}

@Composable
private fun CallControlRow(
    muted: Boolean,
    speakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onSwitchCamera: (() -> Unit)?,
    onEndCall: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            icon = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = if (muted) "Unmute" else "Mute",
            onClick = onMuteToggle,
        )
        Spacer(modifier = Modifier.width(12.dp))
        ControlButton(
            icon = if (speakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            label = if (speakerOn) "Speaker" else "Earpiece",
            onClick = onSpeakerToggle,
        )
        if (onSwitchCamera != null) {
            Spacer(modifier = Modifier.width(12.dp))
            ControlButton(
                icon = Icons.Filled.SwapHoriz,
                label = "Switch",
                onClick = onSwitchCamera,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        ControlButton(
            icon = Icons.Filled.CallEnd,
            label = "End",
            onClick = onEndCall,
            containerColor = Color(0xFFEF4444),
            labelColor = Color.White,
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = Color(0xFF15233E),
    iconTint: Color = Color.White,
    labelColor: Color = Color.White,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = iconTint)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = labelColor.copy(alpha = 0.88f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatCallTimer(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds) else String.format("%02d:%02d", minutes, remainingSeconds)
}
