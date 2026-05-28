package com.example.kampus.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Spacer
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kampus.ui.theme.ThemeController
import java.text.SimpleDateFormat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.collections.isNotEmpty
import kotlin.math.max
import kotlin.math.roundToInt

// ─── Colour tokens ────────────────────────────────────────────────────────────
private val UiIsDark        get() = ThemeController.isDark
private val BgDeep          get() = if (UiIsDark) Color(0xFF070D18) else Color(0xFFF3F4F8)
private val HeaderBg        get() = if (UiIsDark) Color(0xFF0C1627) else Color(0xFFFFFFFF)
private val InputBg         get() = if (UiIsDark) Color(0xFF0C1627) else Color(0xFFFFFFFF)
private val InputFieldBg    get() = if (UiIsDark) Color(0xFF111C2C) else Color(0xFFF9FAFB)
private val AccentBlue      get() = ThemeController.accent.color
private val TextPrimary     get() = if (UiIsDark) Color(0xFFEFF3FF) else Color(0xFF111827)
private val TextSecondary   get() = if (UiIsDark) Color(0xFF6B7A99) else Color(0xFF6B7280)
private val OnlineGreen     get() = Color(0xFF22C55E)
private val IconButton      get() = if (UiIsDark) Color(0xFF1A2540) else Color(0xFFE5E7EB)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatScreen(
    chatId    : String,
    onBack    : () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onCallAgainClick: (String, String) -> Unit = { _, _ -> },
    onDiagnosticsClick: () -> Unit = {},
    onRotateKeysClick: () -> Unit = {},
    chatViewModel : ChatViewModel = viewModel(),
) {
    LaunchedEffect(chatId) { chatViewModel.openChat(chatId) }
    val targetMessageId by chatViewModel.targetMessageId.collectAsState()
    val state        by chatViewModel.chatState.collectAsStateWithLifecycle()
    val chatListState by chatViewModel.chatListState.collectAsStateWithLifecycle()
    val listState    = rememberLazyListState()
    val scope        = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingDeleteMessages = remember { mutableStateMapOf<String, Message>() }
    val pendingDeleteJobs = remember { mutableStateMapOf<String, Job>() }
    val messageRenderVersions = remember { mutableStateMapOf<String, Int>() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val visibleMessages = remember(state.messages, pendingDeleteMessages.size, currentUserId) {
        state.messages.filterNot {
            pendingDeleteMessages.containsKey(it.id) || (currentUserId != null && it.deletedForUsers.contains(currentUserId))
        }
    }
    var shouldAutoScroll by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf<Message?>(null) }
    var editTargetMessage by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var recordingPausedAt by remember { mutableStateOf(0L) }
    var recordingPausedAccumulatedMs by remember { mutableStateOf(0L) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(isRecording, recordingStartedAt, isRecordingPaused, recordingPausedAt, recordingPausedAccumulatedMs) {
        if (!isRecording) {
            recordingElapsedMs = 0L
            return@LaunchedEffect
        }

        while (isRecording) {
            val pauseAdjustment = recordingPausedAccumulatedMs + if (isRecordingPaused && recordingPausedAt > 0L) {
                System.currentTimeMillis() - recordingPausedAt
            } else 0L
            recordingElapsedMs = System.currentTimeMillis() - recordingStartedAt - pauseAdjustment
            delay(250)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasAudioPermission = granted
            if (granted) {
                startVoiceRecording(context) { file, mediaRecorder, startedAt ->
                    recordingFile = file
                    recorder = mediaRecorder
                    recordingStartedAt = startedAt
                    recordingElapsedMs = 0L
                    recordingPausedAt = 0L
                    recordingPausedAccumulatedMs = 0L
                    isRecordingPaused = false
                    isRecording = true
                }
            }
        }
    )

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    @Suppress("DEPRECATION")
    fun sendLiveLocation() {
        if (!hasLocationPermission) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Couldn't get current location")
                    }
                    return@addOnSuccessListener
                }

                val label = try {
                    val geocoder = android.location.Geocoder(context, Locale.getDefault())
                    val result = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val place = result?.firstOrNull()
                    when {
                        place == null -> "Current location"
                        !place.featureName.isNullOrBlank() -> place.featureName
                        !place.locality.isNullOrBlank() -> place.locality
                        else -> "Current location"
                    }
                } catch (_: Exception) {
                    "Current location"
                }

                chatViewModel.sendLiveLocationMessage(location.latitude, location.longitude, label)
            }
            .addOnFailureListener {
                scope.launch {
                    snackbarHostState.showSnackbar("Couldn't send live location")
                }
            }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                chatViewModel.sendAttachmentMessage(
                    mediaUri = uri,
                    attachmentType = "image",
                    mediaName = "Photo",
                    mimeType = context.contentResolver.getType(uri),
                )
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                chatViewModel.sendAttachmentMessage(
                    mediaUri = uri,
                    attachmentType = "video",
                    mediaName = "Video",
                    mimeType = context.contentResolver.getType(uri),
                )
            }
        }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "File"
                chatViewModel.sendAttachmentMessage(
                    mediaUri = uri,
                    attachmentType = "file",
                    mediaName = fileName,
                    mimeType = context.contentResolver.getType(uri),
                )
            }
        }
    )

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
                chatViewModel.sendAttachmentMessage(
                    mediaUri = uri,
                    attachmentType = "image",
                    mediaName = "Camera photo",
                    mimeType = "image/jpeg",
                )
            } else {
                android.util.Log.e("ChatScreen", "Camera capture failed: ${result.resultCode}")
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (granted) {
                val outputUri = createTempChatCapture(context)
                pendingCameraUri = outputUri
                val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                takePhotoLauncher.launch(intent)
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasLocationPermission = granted
            if (granted) {
                sendLiveLocation()
            }
        }
    )

    fun stopAndSendVoice() {
        val file = recordingFile ?: return
        val startedAt = recordingStartedAt
        val adjustedPauseMs = recordingPausedAccumulatedMs + if (isRecordingPaused && recordingPausedAt > 0L) {
            System.currentTimeMillis() - recordingPausedAt
        } else 0L
        val currentRecorder = recorder
        recorder = null
        recordingFile = null
        isRecording = false
        isRecordingPaused = false
        recordingPausedAt = 0L
        recordingPausedAccumulatedMs = 0L

        try {
            currentRecorder?.stop()
        } catch (_: Exception) {
        } finally {
            try {
                currentRecorder?.release()
            } catch (_: Exception) {
            }
        }

        val durationSeconds = max(1L, ((System.currentTimeMillis() - startedAt - adjustedPauseMs) / 1000L))
        val fileSizeKb = (file.length() / 1024).coerceAtLeast(1)
        android.util.Log.d("VoiceRecording", "🎙️  Recording stopped: duration=${durationSeconds}s, fileSize=${fileSizeKb}KB")
        chatViewModel.sendVoiceMessage(file, formatVoiceDuration(durationSeconds))
    }

    fun toggleVoicePauseResume() {
        val currentRecorder = recorder ?: return
        if (!isRecording || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        try {
            if (isRecordingPaused) {
                currentRecorder.resume()
                recordingPausedAccumulatedMs += if (recordingPausedAt > 0L) {
                    System.currentTimeMillis() - recordingPausedAt
                } else 0L
                recordingPausedAt = 0L
                isRecordingPaused = false
            } else {
                currentRecorder.pause()
                recordingPausedAt = System.currentTimeMillis()
                isRecordingPaused = true
            }
        } catch (e: Exception) {
            android.util.Log.w("VoiceRecording", "Pause/resume failed: ${e.message}")
        }
    }

    fun queueDeleteMessage(message: Message) {
        if (pendingDeleteMessages.containsKey(message.id)) return

        pendingDeleteMessages[message.id] = message
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        val deleteJob = scope.launch {
            delay(4000)
            val result = chatViewModel.deleteMessage(message)
            pendingDeleteJobs.remove(message.id)
            result.onSuccess {
                pendingDeleteMessages.remove(message.id)
                messageRenderVersions.remove(message.id)
            }
            result.onFailure { error ->
                pendingDeleteMessages.remove(message.id)
                snackbarHostState.showSnackbar(error.message ?: "Failed to delete message")
            }
        }
        pendingDeleteJobs[message.id] = deleteJob

        scope.launch {
            val action = snackbarHostState.showSnackbar(
                message = "Message deleted",
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Long,
            )

            if (action == SnackbarResult.ActionPerformed) {
                pendingDeleteJobs.remove(message.id)?.cancel()
                pendingDeleteMessages.remove(message.id)
                messageRenderVersions[message.id] = (messageRenderVersions[message.id] ?: 0) + 1
                return@launch
            }
        }
    }

    fun deleteMessageForMe(message: Message) {
        if (pendingDeleteMessages.containsKey(message.id)) return
        pendingDeleteMessages[message.id] = message
        scope.launch {
            val result = chatViewModel.deleteMessageForMe(message)
            result.onSuccess {
                pendingDeleteMessages.remove(message.id)
            }
            result.onFailure { error ->
                pendingDeleteMessages.remove(message.id)
                snackbarHostState.showSnackbar(error.message ?: "Failed to delete message")
            }
        }
    }

    fun beginEditMessage(message: Message) {
        editTargetMessage = message
        editText = message.text
    }

    fun saveEditedMessage() {
        val target = editTargetMessage ?: return
        val newValue = editText.trim()
        if (newValue.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Message cannot be empty") }
            return
        }
        scope.launch {
            chatViewModel.editMessage(target, newValue)
                .onSuccess {
                    editTargetMessage = null
                    editText = ""
                    snackbarHostState.showSnackbar("Message updated")
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(error.message ?: "Failed to edit message")
                }
        }
    }

    // Drop stale pending deletions once server snapshot no longer contains them.
    LaunchedEffect(state.messages) {
        val currentIds = state.messages.map { it.id }.toSet()
        pendingDeleteMessages.keys
            .filterNot { it in currentIds }
            .forEach {
                pendingDeleteJobs.remove(it)?.cancel()
                pendingDeleteMessages.remove(it)
                messageRenderVersions.remove(it)
            }
    }

    // Track whether user is near bottom; only then should new messages auto-scroll.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= info.totalItemsCount - 3 || info.totalItemsCount == 0
        }.collect { nearBottom ->
            shouldAutoScroll = nearBottom
        }
    }

    // Reset scroll flag whenever switching to a new chat
    LaunchedEffect(chatId) {
        shouldAutoScroll = true
    }

    // Scroll to bottom when message list size changes and shouldAutoScroll is true
    LaunchedEffect(visibleMessages.size, shouldAutoScroll) {
        if (visibleMessages.isNotEmpty() && shouldAutoScroll) {
            scope.launch {
                val totalItems = listState.layoutInfo.totalItemsCount
                if (totalItems > 0) {
                    listState.scrollToItem(totalItems - 1)
                } else {
                    listState.scrollToItem(visibleMessages.size * 2)
                }
            }
        }
    }

    LaunchedEffect(targetMessageId, visibleMessages.size) {
        val target = targetMessageId ?: return@LaunchedEffect
        val index = visibleMessages.indexOfFirst { it.id == target }
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    Scaffold(
        containerColor = BgDeep,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 84.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Color(0xFF101A2E),
                        contentColor = Color.White,
                        actionColor = AccentBlue,
                        dismissActionContentColor = Color.White.copy(alpha = 0.75f),
                    )
                },
            )
        },
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(innerPadding)
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        ChatTopBar(
            contactName = state.contactName,
            contactUserId = state.contactUserId,
            contactProfileImageUrl = state.contactProfileImageUrl,
            contactAvatarEmoji = state.contactAvatarEmoji,
            isOnline    = state.isOnline,
            isTyping    = state.isTyping,
            onBack      = onBack,
            onOpenProfile = onOpenProfile,
            onDiagnosticsClick = onDiagnosticsClick,
            onRotateKeysClick = onRotateKeysClick,
            onVoiceCallClick = onVoiceCallClick,
            onVideoCallClick = onVideoCallClick,
            onTestCallHistoryClick = {
                Log.d("ChatScreen", "Creating test call history for chatId=$chatId")
                chatViewModel.createTestCallHistory(chatId)
            },
        )

        // ── Message list ───────────────────────────────────────────────────
        LazyColumn(
            state           = listState,
            modifier        = Modifier.weight(1f),
            contentPadding  = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val timelineMessages = visibleMessages.sortedBy { it.timestampMillis }
            val regularMessages = timelineMessages.filter { !it.isCallInvite }
            val firstUnreadIncomingIndex = timelineMessages.indexOfFirst { !it.isSentByMe && !it.isRead }
            var lastDateKey: String? = null

            timelineMessages.forEachIndexed { index, msg ->
                val dateKey = messageDateKey(msg.timestampMillis)
                if (dateKey != lastDateKey) {
                    item(key = "date_${dateKey}_$index") {
                        DateSeparator(text = messageDateLabel(msg.timestampMillis))
                    }
                    lastDateKey = dateKey
                }

                if (index == firstUnreadIncomingIndex) {
                    item(key = "unread_${msg.id}") {
                        UnreadMessagesSeparator()
                    }
                }

                if (msg.isCallInvite) {
                    item(key = "call_${msg.id}:${messageRenderVersions[msg.id] ?: 0}") {
                        CallHistoryCard(
                            message = msg,
                            onCallBack = { callType ->
                                onCallAgainClick(chatId, callType)
                            },
                        )
                    }
                } else {
                    val regularIndex = regularMessages.indexOfFirst { it.id == msg.id }
                    val isLastInGroup = regularIndex == regularMessages.lastIndex ||
                        regularMessages.getOrNull(regularIndex + 1)?.isSentByMe != msg.isSentByMe

                    item(key = "msg_${msg.id}:${messageRenderVersions[msg.id] ?: 0}") {
                        SwipeToDeleteMessageRow(
                            message = msg,
                            isLastInGroup = isLastInGroup,
                            selfName = chatListState.currentUserName,
                            contactName = state.contactName,
                            contactProfileImageUrl = state.contactProfileImageUrl,
                            contactAvatarEmoji = state.contactAvatarEmoji,
                            selfProfileImageUrl = chatListState.currentUserProfileImageUrl,
                            selfAvatarEmoji = chatListState.currentUserAvatarEmoji,
                            onDelete = { queueDeleteMessage(msg) },
                            onLongPress = {
                                actionMessage = msg
                            },
                            onReact = { remoteId, emoji -> chatViewModel.toggleReactionOnMessage(remoteId, emoji) },
                        )
                    }
                }
            }
        }

        // ── Input bar ──────────────────────────────────────────────────────
            when (val uploadState = state.voiceUploadState) {
                is VoiceUploadState.Uploading -> VoiceUploadStatusBanner(
                    state = uploadState,
                    onRetry = { chatViewModel.retryVoiceUpload() },
                )
                is VoiceUploadState.Failed -> VoiceUploadStatusBanner(
                    state = uploadState,
                    onRetry = { chatViewModel.retryVoiceUpload() },
                )
                VoiceUploadState.Idle -> Unit
            }

            ChatInputBar(
                text = state.inputText,
                onTextChange = chatViewModel::onInputChange,
                onAttachClick = {
                    showAttachmentSheet = true
                },
                onSend = {
                    chatViewModel.sendMessage()
                },
                onMicClick = {
                    if (!hasAudioPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (!isRecording) {
                            startVoiceRecording(context) { file, mediaRecorder, startedAt ->
                                recordingFile = file
                                recorder = mediaRecorder
                                recordingStartedAt = startedAt
                                isRecording = true
                            }
                        } else {
                            // toggle: stop and send
                            stopAndSendVoice()
                        }
                    }
                },
                onMicPressStart = {
                    if (!hasAudioPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        startVoiceRecording(context) { file, mediaRecorder, startedAt ->
                            recordingFile = file
                            recorder = mediaRecorder
                            recordingStartedAt = startedAt
                            recordingElapsedMs = 0L
                            recordingPausedAt = 0L
                            recordingPausedAccumulatedMs = 0L
                            isRecordingPaused = false
                            isRecording = true
                        }
                    }
                },
                onMicRelease = {
                    if (isRecording) stopAndSendVoice()
                },
                onMicCancel = {
                    // stop and discard
                    try {
                        recorder?.stop()
                    } catch (_: Exception) {}
                    try { recorder?.release() } catch (_: Exception) {}
                    recordingFile?.delete()
                    recorder = null
                    recordingFile = null
                    recordingElapsedMs = 0L
                    recordingPausedAt = 0L
                    recordingPausedAccumulatedMs = 0L
                    isRecordingPaused = false
                    isRecording = false
                },
                isRecording = isRecording,
                isRecordingPaused = isRecordingPaused,
                onMicPauseResume = { toggleVoicePauseResume() },
                recordingDurationText = if (isRecording) formatVoiceDuration(max(1L, recordingElapsedMs / 1000L)) else "",
            )
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = Color(0xFF0E1626),
            contentColor = TextPrimary,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Attach something",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Share media, files, or your location",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }

                    IconButton(onClick = { showAttachmentSheet = false }) {
                        Icon(Icons.Default.Delete, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                AttachmentSheetAction(
                    icon = Icons.Default.Image,
                    title = "Gallery photo",
                    subtitle = "Pick an image from your phone",
                    onClick = {
                        showAttachmentSheet = false
                        imagePickerLauncher.launch("image/*")
                    },
                )

                AttachmentSheetAction(
                    icon = Icons.Default.Videocam,
                    title = "Video",
                    subtitle = "Pick a video clip",
                    onClick = {
                        showAttachmentSheet = false
                        videoPickerLauncher.launch("video/*")
                    },
                )

                AttachmentSheetAction(
                    icon = Icons.Default.PhotoCamera,
                    title = "Camera photo",
                    subtitle = "Take a new picture",
                    onClick = {
                        showAttachmentSheet = false
                        if (hasCameraPermission) {
                            val outputUri = createTempChatCapture(context)
                            pendingCameraUri = outputUri
                            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }
                            takePhotoLauncher.launch(intent)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                )

                AttachmentSheetAction(
                    icon = Icons.Default.Description,
                    title = "File",
                    subtitle = "Send documents and downloads",
                    onClick = {
                        showAttachmentSheet = false
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                )

                AttachmentSheetAction(
                    icon = Icons.Default.Place,
                    title = "Location",
                    subtitle = "Share live location",
                    onClick = {
                        showAttachmentSheet = false
                        if (hasLocationPermission) {
                            sendLiveLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                )
            }
        }
    }

    val selectedMessage = actionMessage
    if (selectedMessage != null) {
        val previewText = selectedMessage.text.trim().ifBlank { if (selectedMessage.isVoice) "Voice message" else "Message" }
        ModalBottomSheet(
            onDismissRequest = { actionMessage = null },
            containerColor = Color(0xFF121A2A),
            contentColor = TextPrimary,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Emoji reaction picker bar at top
                val reactions = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF1E2A3B))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    reactions.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    chatViewModel.toggleReactionOnMessage(selectedMessage.remoteMessageId, emoji)
                                    actionMessage = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }

                    // Add button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF2A3A4B))
                            .clickable { /* Open extended emoji picker */ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "+", fontSize = 20.sp, color = TextSecondary)
                    }
                }

                // Message preview
                Surface(
                    color = Color(0xFF182235),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (selectedMessage.isSentByMe) "Your message" else "Message",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = previewText,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Action rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (selectedMessage.isSentByMe && !selectedMessage.isVoice) {
                        ActionSheetRow(
                            icon = Icons.Default.Edit,
                            label = "Edit message",
                            onClick = {
                                actionMessage = null
                                beginEditMessage(selectedMessage)
                            },
                        )
                    }

                    ActionSheetRow(
                        icon = Icons.Default.VisibilityOff,
                        label = "Hide text",
                        onClick = {
                            actionMessage = null
                            chatViewModel.toggleHideTextOnMessage(selectedMessage.remoteMessageId)
                        },
                    )

                    ActionSheetRow(
                        icon = Icons.Default.PersonOff,
                        label = "Delete for me",
                        onClick = {
                            actionMessage = null
                            deleteMessageForMe(selectedMessage)
                        },
                    )

                    if (selectedMessage.isSentByMe) {
                        ActionSheetRow(
                            icon = Icons.Default.DeleteForever,
                            label = "Delete for everyone",
                            labelColor = Color(0xFFFF6B6B),
                            onClick = {
                                actionMessage = null
                                queueDeleteMessage(selectedMessage)
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    val editingMessage = editTargetMessage
    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = {
                editTargetMessage = null
                editText = ""
            },
            containerColor = HeaderBg,
            title = {
                Text(text = "Edit message", color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                    singleLine = false,
                )
            },
            confirmButton = {
                TextButton(onClick = { saveEditedMessage() }) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editTargetMessage = null
                        editText = ""
                    }
                ) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteMessageRow(
    message: Message,
    isLastInGroup: Boolean = true,
    selfName: String = "",
    contactName: String = "",
    contactProfileImageUrl: String = "",
    contactAvatarEmoji: String = "",
    selfProfileImageUrl: String = "",
    selfAvatarEmoji: String = "",
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    onReact: ((remoteMessageId: String, emoji: String) -> Unit)? = null,
) {
    val canDelete = message.isSentByMe && message.remoteMessageId.isNotBlank()

    // For non-deletable messages (from other users), just render normally with long-press support
    if (!canDelete) {
        MessageGroupWithAvatar(
            message = message, 
            isLastInGroup = isLastInGroup,
            selfName = selfName,
            contactName = contactName,
            contactProfileImageUrl = contactProfileImageUrl,
            contactAvatarEmoji = contactAvatarEmoji,
            selfProfileImageUrl = selfProfileImageUrl,
            selfAvatarEmoji = selfAvatarEmoji,
            onLongPress = onLongPress,
            onReact = onReact
        )
        return
    }

    val deleteActionWidth = if (message.isVoice) 268.dp else 286.dp
    var deleteTriggered by remember(message.id) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { true },
        positionalThreshold = { totalDistance -> totalDistance * 0.25f },
    )

    LaunchedEffect(dismissState.currentValue, message.id) {
        if (!deleteTriggered && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            deleteTriggered = true
            onDelete()
            dismissState.reset()
        }
    }

    val showDeleteBackground =
        dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
            dismissState.targetValue != SwipeToDismissBoxValue.Settled
    val iconScale by animateFloatAsState(
        targetValue = if (showDeleteBackground) 1f else 0.82f,
        animationSpec = tween(durationMillis = 180),
        label = "delete_icon_scale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (showDeleteBackground) 1f else 0.58f,
        animationSpec = tween(durationMillis = 180),
        label = "delete_icon_alpha",
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            if (showDeleteBackground) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .width(deleteActionWidth)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFEF4444),
                                        Color(0xFFB91C1C),
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Delete",
                            color = Color.White.copy(alpha = iconAlpha),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete message",
                            tint = Color.White.copy(alpha = iconAlpha),
                            modifier = Modifier
                                .size(18.dp)
                                .scale(iconScale)
                                .alpha(iconAlpha),
                        )
                    }
                }
            }
        },
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(
                initialOffsetX = { if (message.isSentByMe) it else -it }
            ) + fadeIn(),
        ) {
            MessageGroupWithAvatar(
                message = message,
                isLastInGroup = isLastInGroup,
                selfName = selfName,
                contactName = contactName,
                contactProfileImageUrl = contactProfileImageUrl,
                contactAvatarEmoji = contactAvatarEmoji,
                selfProfileImageUrl = selfProfileImageUrl,
                selfAvatarEmoji = selfAvatarEmoji,
                onLongPress = onLongPress,
                onReact = onReact
            )
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    labelColor: Color = TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = labelColor, modifier = Modifier.size(20.dp))
        Text(text = label, color = labelColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Top bar ─────────────────────────────────────────────────────────────────
@Composable
private fun ChatTopBar(
    contactName : String,
    contactUserId : String,
    contactProfileImageUrl : String,
    contactAvatarEmoji : String,
    isOnline    : Boolean,
    isTyping    : Boolean,
    onBack      : () -> Unit,
    onOpenProfile: (String) -> Unit,
    onDiagnosticsClick: () -> Unit = {},
    onRotateKeysClick: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onTestCallHistoryClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(HeaderBg, HeaderBg.copy(alpha = 0.95f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = TextPrimary,
            )
        }

        Spacer(Modifier.width(4.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.25f))
                .clickable(enabled = contactUserId.isNotBlank()) { onOpenProfile(contactUserId) },
            contentAlignment = Alignment.Center,
        ) {
            if (contactProfileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = contactProfileImageUrl,
                    contentDescription = contactName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    text       = contactAvatarEmoji.ifBlank { contactName.take(2).uppercase() },
                    color      = AccentBlue,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Name & status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = contactName,
                color      = TextPrimary,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        when {
                            isTyping -> AccentBlue.copy(alpha = 0.12f)
                            isOnline -> OnlineGreen.copy(alpha = 0.10f)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            isTyping -> AccentBlue.copy(alpha = 0.28f)
                            isOnline -> OnlineGreen.copy(alpha = 0.22f)
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                AnimatedContent(
                    targetState = isTyping,
                    transitionSpec = {
                        fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f) togetherWith
                            fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.98f)
                    },
                    label = "chat-status",
                ) { typing ->
                    if (typing) {
                        TypingStatusRow()
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            if (isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen),
                                )
                            }
                            Text(
                                text     = if (isOnline) "Online" else "Offline",
                                color    = if (isOnline) OnlineGreen else TextSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        // Call & Video buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onVoiceCallClick) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onVideoCallClick) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
        }

        // Debug menu
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Test Call History") }, onClick = {
                        expanded = false
                        onTestCallHistoryClick()
                    })
                    DropdownMenuItem(text = { Text("Diagnostics") }, onClick = {
                        expanded = false
                        onDiagnosticsClick()
                    })
                    DropdownMenuItem(text = { Text("Rotate Keys") }, onClick = {
                        expanded = false
                        onRotateKeysClick()
                    })
            }
        }
    }

    // Thin divider
    HorizontalDivider(color = Color(0xFF1A2540), thickness = 0.5.dp)
}

@Composable
private fun TypingStatusRow() {
    var dotCount by remember { mutableIntStateOf(1) }
    val pulseTransition = rememberInfiniteTransition(label = "typing-pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "typing-pulse-alpha",
    )

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(350)
            dotCount = if (dotCount >= 3) 1 else dotCount + 1
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = pulseAlpha)),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = "Typing${".".repeat(dotCount)}",
            color = AccentBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActionIconBtn(
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    description : String,
    onClick     : () -> Unit = {},
    bgColor     : Color = IconButton,
    iconTint    : Color = TextPrimary,
    size        : androidx.compose.ui.unit.Dp = 36.dp,
    iconSize    : androidx.compose.ui.unit.Dp = 18.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = iconTint,
            modifier           = Modifier.size(iconSize),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF121D31),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ActionIconBtn(
                icon = icon,
                description = title,
                bgColor = Color(0xFF1B2841),
                iconTint = AccentBlue,
                size = 42.dp,
                iconSize = 22.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun createTempChatCapture(context: Context): Uri {
    val outputFile = File(context.cacheDir, "chat_capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        outputFile,
    )
}

@Composable
private fun VoiceUploadStatusBanner(
    state: VoiceUploadState,
    onRetry: () -> Unit,
) {
    val title: String
    val subtitle: String
    val accent: Color
    val actionLabel: String

    when (state) {
        is VoiceUploadState.Uploading -> {
            title = "Uploading voice note"
            subtitle = "Sending audio to Supabase storage…"
            accent = Color(0xFF60A5FA)
            actionLabel = "Uploading"
        }
        is VoiceUploadState.Failed -> {
            title = "Voice upload failed"
            subtitle = state.error.ifBlank { "Tap retry to send again." }
            accent = Color(0xFFEF4444)
            actionLabel = "Retry"
        }
        VoiceUploadState.Idle -> {
            title = ""
            subtitle = ""
            accent = Color.Transparent
            actionLabel = ""
        }
    }

    if (state is VoiceUploadState.Idle) return

    Surface(
        color = Color(0xFF111C2C),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state is VoiceUploadState.Uploading) Icons.Default.Mic else Icons.Default.Delete,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            if (state is VoiceUploadState.Failed) {
                Text(
                    text = actionLabel,
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickableNoRipple { onRetry() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ─── Input bar ────────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    text         : String,
    onTextChange : (String) -> Unit,
    onAttachClick: () -> Unit,
    onSend       : () -> Unit,
    onMicClick   : () -> Unit,
    onMicPressStart: () -> Unit,
    onMicRelease: () -> Unit,
    onMicCancel  : () -> Unit,
    isRecording  : Boolean,
    isRecordingPaused: Boolean,
    onMicPauseResume: () -> Unit,
    recordingDurationText: String = "",
) {
    val waveformTransition = rememberInfiniteTransition(label = "recording_waveform")
    val bar1 by waveformTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2 by waveformTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3 by waveformTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )
    val bar4 by waveformTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar4",
    )
    val bar5 by waveformTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar5",
    )
    val bar6 by waveformTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar6",
    )

    HorizontalDivider(color = Color(0xFF1A2540), thickness = 0.5.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InputBg)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // + button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(IconButton)
                .clickableNoRipple { onAttachClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Attach",
                tint               = TextSecondary,
                modifier           = Modifier.size(20.dp),
            )
        }

        // Mic button (press-and-hold to record, tap to toggle)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isRecording) Color(0xFFEF4444) else IconButton)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // start when pressed
                            onMicPressStart()
                            try {
                                // wait for release or cancellation
                                awaitRelease()
                                // released normally
                                onMicRelease()
                            } catch (_: Exception) {
                                // cancelled (dragged away), treat as release
                                onMicRelease()
                            }
                        },
                        onTap = {
                            // simple tap toggles recording (start/stop)
                            onMicClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Voice",
                tint               = if (isRecording) Color.White else TextSecondary,
                modifier           = Modifier.size(20.dp),
            )
        }

        // Recording preview or text field
        if (isRecording) {
            val density = LocalDensity.current
            var dragOffsetX by remember { mutableStateOf(0f) }
            val dragThresholdPx = with(density) { 110.dp.toPx() }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragOffsetX = 0f },
                            onDragEnd = {
                                if (dragOffsetX < -dragThresholdPx) {
                                    onMicCancel()
                                }
                                dragOffsetX = 0f
                            },
                            onDragCancel = { dragOffsetX = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-dragThresholdPx * 1.4f, 0f)
                                if (dragOffsetX < -dragThresholdPx) {
                                    onMicCancel()
                                    dragOffsetX = 0f
                                }
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF202B43))
                        .clickableNoRipple { onMicCancel() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Discard recording",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = AccentBlue.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(999.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )

                        RecordingWaveBars(
                            amplitudes = listOf(bar1, bar2, bar3, bar4, bar5, bar6),
                            color = Color.White,
                        )

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickableNoRipple { onMicPauseResume() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isRecordingPaused) "Resume recording" else "Pause recording",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Text(
                            text = recordingDurationText.ifBlank { "0:00" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF202B43))
                        .clickableNoRipple { onMicRelease() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send recording",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(InputFieldBg)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        "Write now…",
                        color    = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterStart).padding(vertical = 10.dp),
                    )
                }
                TextField(
                    value         = text,
                    onValueChange = onTextChange,
                    singleLine    = false,
                    maxLines      = 4,
                    colors        = TextFieldDefaults.colors(
                        unfocusedContainerColor  = Color.Transparent,
                        focusedContainerColor    = Color.Transparent,
                        unfocusedIndicatorColor  = Color.Transparent,
                        focusedIndicatorColor    = Color.Transparent,
                        unfocusedTextColor       = TextPrimary,
                        focusedTextColor         = TextPrimary,
                        cursorColor              = AccentBlue,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Send button
        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter   = scaleIn() + fadeIn(),
            exit    = scaleOut() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AccentBlue, Color(0xFF6D28D9))
                        )
                    )
                    .clickableNoRipple { onSend() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordingWaveBars(
    amplitudes: List<Float>,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val baseHeights = listOf(8.dp, 12.dp, 6.dp, 14.dp, 9.dp, 11.dp)
        amplitudes.take(baseHeights.size).forEachIndexed { index, amplitude ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(baseHeights[index] * amplitude)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

// ─── Helper: clickable without ripple ─────────────────────────────────────────
private fun Modifier.clickableNoRipple(onClick: () -> Unit) =
    this.then(
        Modifier.clickable(
            interactionSource = null,
            indication        = null,
            onClick           = onClick,
        )
    )

@Suppress("DEPRECATION")
private fun startVoiceRecording(
    context: Context,
    onStarted: (File, MediaRecorder, Long) -> Unit,
) {
    val outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
    recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        // Increased bitrate from 128kbps to 256kbps for better audio quality
        setAudioEncodingBitRate(256000)
        setAudioSamplingRate(48000)
        setOutputFile(outputFile.absolutePath)
        prepare()
        start()
    }
    android.util.Log.d("VoiceRecording", "🎤 Started recording to: ${outputFile.absolutePath}")
    onStarted(outputFile, recorder, System.currentTimeMillis())
}

private fun formatVoiceDuration(seconds: Long): String {
    val safeSeconds = max(1L, seconds)
    val minutes = safeSeconds / 60L
    val remainingSeconds = safeSeconds % 60L
    return if (minutes > 0) {
        "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
    } else {
        "0:${remainingSeconds.toString().padStart(2, '0')}"
    }
}

private fun messageDateKey(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(timestampMillis.coerceAtLeast(0L)))
}

private fun messageDateLabel(timestampMillis: Long): String {
    val safeTimestamp = timestampMillis.coerceAtLeast(0L)
    val now = System.currentTimeMillis()
    val diffDays = ((now - safeTimestamp) / 86_400_000L).coerceAtLeast(0L)

    return when {
        diffDays <= 0L -> "TODAY"
        diffDays == 1L -> "YESTERDAY"
        diffDays < 7L -> "${diffDays} DAYS AGO"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(safeTimestamp)).uppercase(Locale.getDefault())
    }
}

@Composable
private fun DateSeparator(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (ThemeController.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(
                    color = if (ThemeController.isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UnreadMessagesSeparator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = ThemeController.accent.color.copy(alpha = 0.22f),
        )
        Text(
            text = "Unread messages",
            color = ThemeController.accent.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(ThemeController.accent.color.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = ThemeController.accent.color.copy(alpha = 0.22f),
        )
    }
}

@Composable
private fun CallHistoryCard(
    message: Message,
    onCallBack: (String) -> Unit = {},
) {
    val isDark = ThemeController.isDark
    val isVideo = message.callType.equals("video", ignoreCase = true)
    val isMissedOrDeclined = message.callStatus.equals("missed", ignoreCase = true) || 
                             message.callStatus.equals("declined", ignoreCase = true)
    
    // Professional color palette
    val cardBg = if (isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6)
    val accentColor = when {
        isMissedOrDeclined -> Color(0xFFEF4444) // Red for missed/declined
        else -> Color(0xFF3B82F6) // Blue for successful calls
    }
    val accentLight = accentColor.copy(alpha = 0.15f)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1F2937)
    val textSecondary = if (isDark) Color(0xFFD1D5DB) else Color(0xFF6B7280)
    
    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "card-scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        label = "card-elevation"
    )
    
    // Format call duration or time
    val timeDisplay = when {
        !isMissedOrDeclined && message.voiceDuration.isNotEmpty() -> {
            "Lasted ${message.voiceDuration}"
        }
        else -> message.timestamp
    }
    
    val statusText = when {
        isMissedOrDeclined && message.callStatus.equals("missed") -> "Missed call"
        isMissedOrDeclined && message.callStatus.equals("declined") -> "Declined"
        else -> "Call ended"
    }
    
    val callTypeLabel = if (isVideo) "Video Call" else "Voice Call"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            },
                            onTap = {
                                onCallBack(if (isVideo) "video" else "audio")
                            }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Call icon and type header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon circle with gradient background
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = accentLight,
                                shape = RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Filled.Videocam else Icons.Filled.Call,
                            contentDescription = callTypeLabel,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    
                    // Call info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = callTypeLabel,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            
                            // Status badge
                            Surface(
                                modifier = Modifier
                                    .padding(start = 8.dp),
                                color = accentLight,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = statusText,
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        
                        Text(
                            text = timeDisplay,
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                
                // Divider
                HorizontalDivider(
                    color = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                
                // Action button
                Button(
                    onClick = {
                        onCallBack(if (isVideo) "video" else "audio")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.85f),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                    ),
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Filled.Videocam else Icons.Filled.Call,
                        contentDescription = "Call",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 6.dp),
                    )
                    Text(
                        text = if (isVideo) "Video call again" else "Call again",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}