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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.kampus.call.RingtonePlayer
import com.example.kampus.call.CallManager
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kampus.ui.theme.ThemeController
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.kampus.call.WebRtcClient
import com.google.android.datatransport.BuildConfig

// ─────────────────────────────────────────────────────────────────────────────
//  CallScreen  — entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CallScreen(
    chatId: String,
    callType: String,
    callId: String = "",
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    LaunchedEffect(chatId) { viewModel.openChat(chatId) }

    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val callState by CallManager.sessionState.collectAsStateWithLifecycle()
    var now by remember(chatId) { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    var hasMicrophonePermission by remember(chatId) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasCameraPermission by remember(chatId) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasMicrophonePermission = permissions[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    val needsCameraPermission = callType.equals("video", ignoreCase = true) || callState.isVideo
    val readyToStartCall = hasMicrophonePermission && (!needsCameraPermission || hasCameraPermission)

    LaunchedEffect(chatId, callId, callType, readyToStartCall) {
        if (!readyToStartCall) {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (needsCameraPermission) add(Manifest.permission.CAMERA)
            }.toTypedArray()
            permissionLauncher.launch(permissions)
            return@LaunchedEffect
        }
        CallManager.start(chatId, callId, callType)
    }

    LaunchedEffect(callState.status) {
        when (callState.status.uppercase()) {
            "DECLINED", "MISSED", "ENDED", "FAILED" -> {
                delay(900)
                onBack()
            }
        }
    }

    LaunchedEffect(callState.status, callState.connectedAt) {
        while (callState.status.equals("CONNECTED", ignoreCase = true) && callState.connectedAt > 0L) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val accent = ThemeController.accent.color
    val isVideo = callState.isVideo || callType.equals("video", ignoreCase = true)

    val timerText = if (callState.connectedAt > 0L && callState.status.equals("CONNECTED", ignoreCase = true)) {
        formatCallTimer(((now - callState.connectedAt) / 1000L).coerceAtLeast(0L))
    } else {
        "Connecting..."
    }

    val liveDurationText = if (callState.connectedAt > 0L) {
        formatCallTimer(((now - callState.connectedAt) / 1000L).coerceAtLeast(0L))
    } else {
        "00:00"
    }

    val statusText = when (callState.status.uppercase()) {
        "RINGING"      -> "Ringing..."
        "ACCEPTED"     -> "Connecting..."
        "CONNECTED"    -> timerText
        "RECONNECTING" -> "Reconnecting..."
        "DECLINED"     -> "Declined"
        "MISSED"       -> "Missed call"
        "ENDED"        -> "Call ended"
        "FAILED"       -> "Call failed"
        else           -> callState.message.ifBlank { timerText }
    }

    val name   = chatState.contactName.ifBlank { "Calling" }
    val avatar = chatState.contactAvatarEmoji.ifBlank { "👤" }
    val image  = chatState.contactProfileImageUrl

    val headerPulse = rememberInfiniteTransition(label = "header_pulse")
    val topGlow by headerPulse.animateFloat(
        initialValue = 0.10f,
        targetValue  = 0.22f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "top_glow",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050A14), Color(0xFF08111F), Color(0xFF0C1730)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = topGlow), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.40f),
                        ),
                    ),
                ),
        )

        if (isVideo) {
            VideoHero(
                name        = name,
                timerText   = statusText,
                liveDurationText = liveDurationText,
                cameraEnabled = callState.isCameraEnabled,
                muted       = callState.isMuted,
                speakerOn   = callState.speakerOn,
                connectionStateText = when {
                    callState.isReconnecting                   -> "Reconnecting..."
                    callState.connectionQuality.isNotBlank()   -> callState.connectionQuality
                    else                                       -> timerText
                },
                onEndCall = {
                    CallManager.endCall()
                    onBack()
                },
            )
        } else {
            VoiceBody(
                name           = name,
                avatar         = avatar,
                image          = image,
                timerText      = statusText,
                muted          = callState.isMuted,
                speakerOn      = callState.speakerOn,
                onMuteToggle   = { CallManager.toggleMute() },
                onSpeakerToggle = { CallManager.toggleSpeaker() },
                onEndCall = {
                    CallManager.endCall()
                    onBack()
                },
                accent = accent,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  IncomingCallScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IncomingCallScreen(
    chatId: String,
    callType: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    LaunchedEffect(chatId) { viewModel.openChat(chatId) }

    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val accent    = ThemeController.accent.color
    val isVideo   = callType.equals("video", ignoreCase = true)
    val context   = LocalContext.current

    val pulseTransition = rememberInfiniteTransition(label = "incoming_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "incoming_pulse_scale",
    )
    val glow by pulseTransition.animateFloat(
        initialValue = 0.75f, targetValue = 1.0f,
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
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
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
                                model              = chatState.contactProfileImageUrl,
                                contentDescription = chatState.contactName,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text     = chatState.contactAvatarEmoji.ifBlank { "👤" },
                                color    = Color.White,
                                fontSize = 40.sp,
                            )
                        }
                    }
                }

                Text(
                    text       = chatState.contactName.ifBlank { "Unknown contact" },
                    color      = Color.White,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text     = if (isVideo) "Incoming video call" else "Incoming voice call",
                    color    = Color.White.copy(alpha = 0.68f),
                    fontSize = 15.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Text(
                        text     = "Ringing through WebRTC / WebSocket",
                        color    = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            LaunchedEffect(Unit) {
                try {
                    val ringtoneRes = context.resources
                        .getIdentifier("ringtone_incoming", "raw", context.packageName)
                    if (ringtoneRes != 0) RingtonePlayer.play(context, ringtoneRes)
                } catch (_: Exception) { }
            }
            DisposableEffect(Unit) { onDispose { RingtonePlayer.stop() } }

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IncomingActionButton(
                    icon           = Icons.Filled.CallEnd,
                    label          = "Decline",
                    containerColor = Color(0xFFF04444),
                    onClick        = onDecline,
                )
                IncomingActionButton(
                    icon           = Icons.Filled.Call,
                    label          = "Accept",
                    containerColor = Color(0xFF22C55E),
                    onClick        = {
                        RingtonePlayer.stop()
                        onAccept()
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VoiceBody  — FIXED: avatar+info centred, dock pinned to bottom
// ─────────────────────────────────────────────────────────────────────────────

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
    val wave1 by transition.animateFloat(0.40f, 1.0f,  infiniteRepeatable(tween(900),  RepeatMode.Reverse), label = "wave1")
    val wave2 by transition.animateFloat(0.55f, 0.95f, infiniteRepeatable(tween(1150), RepeatMode.Reverse), label = "wave2")
    val wave3 by transition.animateFloat(0.65f, 1.0f,  infiniteRepeatable(tween(1350), RepeatMode.Reverse), label = "wave3")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        // Avatar + name + timer + bars — centred on screen with slight upward offset
        Column(
            modifier            = Modifier
                .align(Alignment.Center)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            CallAvatar(name, avatar, image, accent, 188.dp)
            Spacer(modifier = Modifier.height(22.dp))
            Text(text = name,      color = Color.White,                     fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = timerText, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)
            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.Bottom,
                ) {
                    VoiceBar(wave1, accent)
                    VoiceBar(wave2, accent)
                    VoiceBar(wave3, accent)
                }
            }
        }

        // Dock pinned to bottom — same as video call
        VoiceControlDock(
            modifier        = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            muted           = muted,
            speakerOn       = speakerOn,
            onMuteToggle    = onMuteToggle,
            onSpeakerToggle = onSpeakerToggle,
            onEndCall       = onEndCall,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VideoHero  — video call full screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoHero(
    name: String,
    timerText: String,
    liveDurationText: String,
    cameraEnabled: Boolean,
    muted: Boolean,
    speakerOn: Boolean,
    connectionStateText: String,
    onEndCall: () -> Unit,
) {
    val context        = LocalContext.current
    val remoteRenderer = remember { SurfaceViewRenderer(context) }
    val localRenderer  = remember { SurfaceViewRenderer(context) }

    DisposableEffect(remoteRenderer, localRenderer) {
        CallManager.bindVideoRenderers(localRenderer, remoteRenderer)
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory  = { remoteRenderer },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.40f),
                            0.25f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1.00f to Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )

        if (!cameraEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 112.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(text = "Camera paused", color = Color.White.copy(alpha = 0.80f), fontSize = 11.sp)
            }
        }

        DraggableLocalPreview(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                ) {
                    AndroidView(
                        factory  = { localRenderer },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )

        TopCallBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
                .padding(top = 54.dp),
            name                = name,
            durationText        = liveDurationText,
            connectionStateText = connectionStateText,
            onBack              = onEndCall,
        )

        if (BuildConfig.DEBUG) {
            DebugPanel(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 14.dp),
            )
        }

        BottomCallDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 38.dp),
            isMuted         = muted,
            speakerOn       = speakerOn,
            cameraEnabled   = cameraEnabled,
            onMuteToggle    = { CallManager.toggleMute() },
            onSpeakerToggle = { CallManager.toggleSpeaker() },
            onCameraToggle  = { CallManager.toggleCamera() },
            onSwitchCamera  = { CallManager.switchCamera() },
            onEndCall       = onEndCall,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TopCallBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopCallBar(
    modifier: Modifier = Modifier,
    name: String,
    durationText: String,
    connectionStateText: String,
    onBack: () -> Unit,
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = Color.White,
                modifier           = Modifier.size(20.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = name,
                color      = Color.White,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text       = durationText,
                color      = Color.White.copy(alpha = 0.95f),
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text     = connectionStateText,
                color    = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.MoreHoriz,
                contentDescription = "More",
                tint               = Color.White,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BottomCallDock
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomCallDock(
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    speakerOn: Boolean,
    cameraEnabled: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        DockAction(
            icon    = if (speakerOn) Icons.AutoMirrored.Filled.VolumeUp
                      else           Icons.AutoMirrored.Filled.VolumeOff,
            onClick = onSpeakerToggle,
        )
        DockAction(
            icon    = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            onClick = onMuteToggle,
        )
        DockAction(
            icon    = if (cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            onClick = onCameraToggle,
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30))
                .clickable { onEndCall() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.CallEnd,
                contentDescription = "End call",
                tint               = Color.White,
                modifier           = Modifier.size(26.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DebugPanel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DebugPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "DBG", color = Color.White, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.36f))
                .padding(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { CallManager.setPreferredVideoQuality(WebRtcClient.VideoQuality.LOW) },
                    contentAlignment = Alignment.Center,
                ) { Text("L", color = Color.White) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { CallManager.setPreferredVideoQuality(WebRtcClient.VideoQuality.MEDIUM) },
                    contentAlignment = Alignment.Center,
                ) { Text("M", color = Color.White) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { CallManager.setPreferredVideoQuality(WebRtcClient.VideoQuality.HIGH) },
                    contentAlignment = Alignment.Center,
                ) { Text("H", color = Color.White) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B82F6))
                        .clickable { CallManager.restartLocalCapture() },
                    contentAlignment = Alignment.Center,
                ) { Text("R", color = Color.White) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981))
                        .clickable { CallManager.setAudioRouteTo("SPEAKER") },
                    contentAlignment = Alignment.Center,
                ) { Text("SP", color = Color.White, fontSize = 10.sp) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VoiceControlDock  — modifier param added so BottomCenter alignment works
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoiceControlDock(
    modifier: Modifier = Modifier,
    muted: Boolean,
    speakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF16191E).copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        DockAction(
            icon    = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
            onClick = onMuteToggle,
        )
        DockAction(
            icon    = if (speakerOn) Icons.AutoMirrored.Filled.VolumeUp
                      else           Icons.AutoMirrored.Filled.VolumeOff,
            onClick = onSpeakerToggle,
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B30))
                .clickable { onEndCall() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.CallEnd,
                contentDescription = "End call",
                tint               = Color.White,
                modifier           = Modifier.size(26.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DockAction
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Color.White,
            modifier           = Modifier.size(22.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CallAvatar
// ─────────────────────────────────────────────────────────────────────────────

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
                    model              = image,
                    contentDescription = name,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                )
            } else {
                Text(text = avatar.ifBlank { "👤" }, color = Color.White, fontSize = 38.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VoiceBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoiceBar(level: Float, accent: Color) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(16.dp + (14.dp * level))
            .clip(RoundedCornerShape(99.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.60f),
                    ),
                ),
            ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  IncomingActionButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IncomingActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = Color.White,
                modifier           = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text       = label,
            color      = Color.White.copy(alpha = 0.85f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  formatCallTimer
// ─────────────────────────────────────────────────────────────────────────────

private fun formatCallTimer(seconds: Long): String {
    val hours            = seconds / 3600
    val minutes          = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0)
        String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    else
        String.format("%02d:%02d", minutes, remainingSeconds)
}