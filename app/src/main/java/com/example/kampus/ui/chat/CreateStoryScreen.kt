@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.example.kampus.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kampus.ui.theme.ThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private val UiIsDark get() = ThemeController.isDark
private val HBg get() = if (UiIsDark) Color(0xFF080B11) else Color(0xFFF3F4F8)
private val HCard get() = if (UiIsDark) Color(0xFF0F1520) else Color(0xFFFFFFFF)
private val HBorder get() = if (UiIsDark) Color(0xFF1A2333) else Color(0xFFD1D5DB)
private val HBlue get() = ThemeController.accent.color
private val HWhite get() = if (UiIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val HGray2 get() = if (UiIsDark) Color(0xFFE5E7EB) else Color(0xFF374151)
private val HGray4 get() = if (UiIsDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val HDark get() = if (UiIsDark) Color(0xFF000000) else Color(0xFFFFFFFF)

private const val TOOL_DRAW = "draw"
private const val TOOL_TEXT = "text"

enum class StoryFlowState {
    CAMERA,
    EDIT,
    PRIVACY,
    COMPLETE,
}

enum class StoryEntryMode {
    STORY,
    NOTE,
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Long,
    val width: Float,
)

@Composable
fun FullCreateStoryScreen(
    onDismiss: () -> Unit,
    onStoryCreated: () -> Unit,
    entryMode: StoryEntryMode = StoryEntryMode.STORY,
    viewModel: ChatViewModel = viewModel(),
) {
    if (entryMode == StoryEntryMode.NOTE) {
        NotesComposerScreen(
            onDismiss = onDismiss,
            onStoryCreated = onStoryCreated,
            viewModel = viewModel,
        )
        return
    }

    var flowState by remember { mutableStateOf(StoryFlowState.CAMERA) }
    var capturedImageUri by remember { mutableStateOf<String?>(null) }
    var storyCaption by remember { mutableStateOf("") }
    var storyOverlayText by remember { mutableStateOf("") }
    var storyOverlayX by remember { mutableFloatStateOf(0.5f) }
    var storyOverlayY by remember { mutableFloatStateOf(0.5f) }
    var storyOverlayColor by remember { mutableLongStateOf(0xFFFFFFFF) }
    var selectedPrivacy by remember { mutableStateOf("friends") }
    var storyType by remember { mutableStateOf("note") }

    when (flowState) {
        StoryFlowState.CAMERA -> {
            StoryCameraScreen(
                onCapture = { uri, note, type ->
                    capturedImageUri = uri
                    storyCaption = note
                    storyOverlayText = note
                    storyType = type
                    flowState = StoryFlowState.EDIT
                },
                onDismiss = onDismiss,
            )
        }

        StoryFlowState.EDIT -> {
            StoryEditScreen(
                imageUri = capturedImageUri,
                storyType = storyType,
                initialCaption = storyCaption,
                initialOverlayText = storyOverlayText,
                initialOverlayX = storyOverlayX,
                initialOverlayY = storyOverlayY,
                initialOverlayColor = storyOverlayColor,
                onCaption = { caption, overlayText, overlayX, overlayY, overlayColor ->
                    storyCaption = caption
                    storyOverlayText = overlayText
                    storyOverlayX = overlayX
                    storyOverlayY = overlayY
                    storyOverlayColor = overlayColor
                    flowState = StoryFlowState.PRIVACY
                },
                onBack = { flowState = StoryFlowState.CAMERA },
            )
        }

        StoryFlowState.PRIVACY -> {
            StoryPrivacyScreen(
                selectedPrivacy = selectedPrivacy,
                onPrivacySelected = { privacy ->
                    selectedPrivacy = privacy
                    flowState = StoryFlowState.COMPLETE
                },
                onBack = { flowState = StoryFlowState.EDIT },
            )
        }

        StoryFlowState.COMPLETE -> {
            val uploadProgress by viewModel.storyUploadProgress.collectAsState()
            val context = LocalContext.current
            
            LaunchedEffect(storyCaption, capturedImageUri, selectedPrivacy, storyType) {
                val result = viewModel.createStory(
                    context = context,
                    note = storyCaption,
                    imageUri = capturedImageUri,
                    overlayText = storyOverlayText,
                    overlayX = storyOverlayX,
                    overlayY = storyOverlayY,
                    overlayColor = storyOverlayColor,
                    privacy = selectedPrivacy,
                    storyType = storyType,
                )
                if (result.isSuccess) {
                    delay(120)
                    onStoryCreated()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        color = HBlue,
                        strokeWidth = 4.dp,
                    )
                    Text(
                        text = "Sharing story... ${((uploadProgress ?: 0.0) * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesComposerScreen(
    onDismiss: () -> Unit,
    onStoryCreated: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var displayName by remember { mutableStateOf("You") }
    var profileImageUrl by remember { mutableStateOf("") }
    var avatarEmoji by remember { mutableStateOf("👤") }
    var noteText by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var isSharing by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(auth.currentUser?.uid) {
        val userId = auth.currentUser?.uid
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (userId != null) {
            listener = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        displayName = snapshot.getString("displayName").orEmpty().ifBlank {
                            auth.currentUser?.displayName.orEmpty().ifBlank { "You" }
                        }
                        profileImageUrl = snapshot.getString("profileImageUrl").orEmpty().ifBlank {
                            auth.currentUser?.photoUrl?.toString().orEmpty()
                        }
                        avatarEmoji = snapshot.getString("avatarEmoji").orEmpty().ifBlank { "👤" }
                    }
                }
        }
        onDispose {
            listener?.remove()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF3B82F6))
                }
                Text(
                    text = "New note",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(
                    onClick = {
                        val cleanNote = noteText.trim()
                        if (cleanNote.isBlank() || isSharing) return@TextButton
                        isSharing = true
                        shareError = null
                        coroutineScope.launch {
                            val moodPrefix = selectedMood?.takeIf { it.isNotBlank() }?.let { "$it " }.orEmpty()
                            val result = viewModel.createStory(
                                context = context,
                                note = "$moodPrefix$cleanNote",
                                privacy = "friends",
                                storyType = "note",
                            )
                            isSharing = false
                            result.onSuccess {
                                onStoryCreated()
                            }.onFailure { error: Throwable ->
                                shareError = error.message ?: "Failed to create note"
                            }
                        }
                    },
                    enabled = noteText.isNotBlank() && !isSharing,
                ) {
                    Text(
                        text = if (isSharing) "Sharing..." else "Share",
                        color = if (noteText.isNotBlank()) Color(0xFF3B82F6) else Color(0xFF6B7280),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (shareError != null) {
                    Text(
                        text = shareError.orEmpty(),
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF26272A))
                        .border(1.dp, Color(0xFF3A3B40), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it.take(140) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "What's on your mind?",
                                color = Color(0xFF7C8089),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color(0xFF3B82F6),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        singleLine = false,
                        maxLines = 3,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(avatarEmoji, fontSize = 42.sp)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotePillButton(label = "GIF", selected = false, onClick = {})
                    NotePillButton(label = "☺", selected = false, onClick = {})
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf("🎂", "🙂", "🥰", "🥺", "😊", "🥳", "🤔").forEach { mood ->
                        NoteMoodButton(
                            emoji = mood,
                            selected = selectedMood == mood,
                            onClick = {
                                selectedMood = if (selectedMood == mood) null else mood
                            },
                        )
                    }
                }

                Text(
                    text = "Friends can see your note for 24 hours. Change",
                    color = Color(0xFFB5B7BE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NotePillButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFF3B82F6) else Color(0xFF242427))
            .border(1.dp, Color(0xFF3A3B40), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NoteMoodButton(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color(0xFF26272A))
            .border(1.dp, Color(0xFF3A3B40), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

@Composable
private fun NoteMoodCard(
    emoji: String,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF26272A))
            .border(1.dp, Color(0xFF3A3B40), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF15161A)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Column {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF9CA3AF), fontSize = 10.sp)
        }
    }
}

@Composable
private fun StoryCameraScreen(
    onCapture: (String?, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var currentUserDisplayName by remember { mutableStateOf("Your Story") }
    var currentUserProfileImageUrl by remember { mutableStateOf("") }
    var currentUserAvatarEmoji by remember { mutableStateOf("👤") }
    var inlineNote by remember { mutableStateOf("") }
    var showTextComposer by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )

    LaunchedEffect(auth.currentUser?.uid) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        runCatching {
            firestore.collection("users").document(userId).get().await()
        }.onSuccess { doc ->
            currentUserDisplayName = doc.getString("displayName").orEmpty().ifBlank {
                auth.currentUser?.displayName.orEmpty().ifBlank { "Your Story" }
            }
            currentUserProfileImageUrl = doc.getString("profileImageUrl").orEmpty().ifBlank {
                auth.currentUser?.photoUrl?.toString().orEmpty()
            }
            currentUserAvatarEmoji = doc.getString("avatarEmoji").orEmpty().ifBlank { "👤" }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) onCapture(uri.toString(), inlineNote.trim(), "image")
        },
    )

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) onCapture(uri.toString(), inlineNote.trim(), "video")
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HDark),
    ) {
        if (hasCameraPermission) {
            CameraPreviewSurface(imageCaptureRef = imageCaptureRef)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = HWhite)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HCard),
                    contentAlignment = Alignment.Center,
                ) {
                    if (currentUserProfileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = currentUserProfileImageUrl,
                            contentDescription = currentUserDisplayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(currentUserAvatarEmoji, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(currentUserDisplayName, color = HWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = HWhite)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CameraToolButton(icon = "Aa", label = "Text") { showTextComposer = true }
            CameraToolButton(icon = "🎵", label = "Music") {}
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    if (!hasCameraPermission) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        return@Button
                    }
                    val imageCapture = imageCaptureRef.value ?: return@Button
                    val (outputFile, outputUri) = createTempStoryCapture(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onCapture(outputUri.toString(), inlineNote.trim(), "image")
                            }

                            override fun onError(exception: ImageCaptureException) {
                            }
                        },
                    )
                },
                modifier = Modifier.size(72.dp).clip(CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = HWhite),
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Capture",
                    tint = HDark,
                    modifier = Modifier.size(32.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HCard)
                        .border(1.dp, HBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("🖼", fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HCard)
                        .border(1.dp, HBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { pickVideoLauncher.launch("video/*") }) {
                        Text("🎬", fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.width(16.dp))

                IconButton(
                    onClick = {
                        val caption = inlineNote.trim().ifBlank { "Campus moment" }
                        onCapture(null, caption, "note")
                    },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(HBlue),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = HWhite)
                }
            }
        }

        if (showTextComposer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showTextComposer = false },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HCard)
                    .border(1.dp, HBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Text("Add text", color = HWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = inlineNote,
                    onValueChange = { inlineNote = it.take(120) },
                    placeholder = { Text("What's on your mind?", color = HGray4) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HWhite,
                        unfocusedTextColor = HWhite,
                        focusedBorderColor = HBlue,
                        unfocusedBorderColor = HBorder,
                        cursorColor = HBlue,
                        focusedContainerColor = HBg,
                        unfocusedContainerColor = HBg,
                    ),
                    maxLines = 4,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showTextComposer = false }) {
                        Text("Done", color = HBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraToolButton(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HCard.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = HGray2, fontSize = 10.sp)
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
private fun StoryEditScreen(
    imageUri: String?,
    storyType: String,
    initialCaption: String,
    initialOverlayText: String,
    initialOverlayX: Float,
    initialOverlayY: Float,
    initialOverlayColor: Long,
    onCaption: (String, String, Float, Float, Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    var caption by remember(initialCaption) { mutableStateOf(initialCaption) }
    var overlayText by remember(initialOverlayText) { mutableStateOf(initialOverlayText.ifBlank { initialCaption }) }
    var overlayX by remember(initialOverlayX) { mutableFloatStateOf(initialOverlayX) }
    var overlayY by remember(initialOverlayY) { mutableFloatStateOf(initialOverlayY) }
    var overlayColor by remember(initialOverlayColor) { mutableLongStateOf(initialOverlayColor) }
    var selectedTool by remember { mutableStateOf(TOOL_TEXT) }
    var isSaving by remember { mutableStateOf(false) }

    // Debounced auto-save: collect changes and save after 2s of inactivity
    LaunchedEffect(viewModel) {
        snapshotFlow { Triple(caption, overlayText, overlayColor) }
            .debounce(2000)
            .collectLatest { (c, t, col) ->
                isSaving = true
                viewModel.saveStoryDraft(
                    note = c,
                    overlayText = t,
                    overlayX = overlayX,
                    overlayY = overlayY,
                    overlayColor = col,
                )
                isSaving = false
            }
    }
    var drawColor by remember { mutableLongStateOf(0xFF60A5FA) }
    var drawStrokeWidth by remember { mutableStateOf(6f) }
    var drawScalingMode by remember { mutableStateOf(false) }
    val drawStrokes = remember { mutableStateListOf<DrawStroke>() }
    var currentStrokePoints by remember { mutableStateOf(listOf<Offset>()) }
    var overlayTextSize by remember { mutableStateOf(28f) }
    var overlayTextScale by remember { mutableStateOf(1f) }

    // Stickers
    data class StickerItem(val id: Int, val emoji: String, var x: Float, var y: Float, var scale: Float = 1f)
    val stickers = remember { mutableStateListOf<StickerItem>() }
    var selectedStickerId by remember { mutableStateOf<Int?>(null) }

    // Music
    data class MusicTrack(val id: String, val title: String, val artist: String)
    val musicTracks = listOf(
        MusicTrack("t1", "Baby Waow", "POTG"),
        MusicTrack("t2", "No Limit", "Maxsickboy"),
        MusicTrack("t3", "Memories Never Die", "Neko Fuzz"),
        MusicTrack("t4", "Dance With Me", "Alex Blue"),
    )
    var selectedMusic by remember { mutableStateOf<MusicTrack?>(null) }

    // Filters
    data class FilterItem(val id: String, val label: String, val colorOverlay: Long)
    val filters = listOf(
        FilterItem("none", "None", 0x00000000),
        FilterItem("warm", "Warm", 0x33FFB86B),
        FilterItem("cool", "Cool", 0x33006AFF),
        FilterItem("mono", "Mono", 0x33FFFFFF),
    )
    var selectedFilter by remember { mutableStateOf<FilterItem?>(filters.first()) }

    val overlayColors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFF60A5FA, 0xFF34D399, 0xFFFDE047, 0xFFF9A8D4,
        0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFA07A, 0xFF98D8C8, 0xFFF7DC6F, 0xFFBB8FCE,
    )
    val drawColors = listOf(
        0xFF60A5FA, 0xFFFFFFFF, 0xFF000000, 0xFF34D399, 0xFFFDE047, 0xFFF9A8D4,
        0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFA07A, 0xFF98D8C8, 0xFFF7DC6F, 0xFFBB8FCE,
    )
    var musicSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HBg),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding(),
        ) {
            val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageUri.isNullOrBlank() && storyType == "image") {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Story image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // simple filter preview overlay
                    selectedFilter?.let { f ->
                        if (f.colorOverlay != 0x00000000L) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(Color(f.colorOverlay)))
                        }
                    }
                } else if (!imageUri.isNullOrBlank() && storyType == "video") {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Video selected", color = HWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Text Story Preview", color = HGray4, fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
                }

                if (drawStrokes.isNotEmpty() || currentStrokePoints.size > 1 || selectedTool == TOOL_DRAW) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedTool) {
                                if (selectedTool != TOOL_DRAW) return@pointerInput
                                detectTapGestures(onLongPress = { drawScalingMode = true })
                            }
                            .pointerInput(selectedTool) {
                                if (selectedTool != TOOL_DRAW) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStrokePoints = listOf(offset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (drawScalingMode) {
                                            drawStrokeWidth = (drawStrokeWidth - dragAmount.y / 10f).coerceIn(1f, 72f)
                                        } else {
                                            currentStrokePoints = currentStrokePoints + change.position
                                        }
                                    },
                                    onDragEnd = {
                                        if (!drawScalingMode && currentStrokePoints.size > 1) {
                                            drawStrokes.add(DrawStroke(currentStrokePoints, drawColor, drawStrokeWidth))
                                        }
                                        currentStrokePoints = emptyList()
                                        drawScalingMode = false
                                    },
                                    onDragCancel = {
                                        currentStrokePoints = emptyList()
                                        drawScalingMode = false
                                    },
                                )
                            },
                    ) {
                        // if a filter is selected, draw the base image first then apply overlay after drawing
                        drawStrokes.forEach { strokeItem ->
                            if (strokeItem.points.size > 1) {
                                val path = Path().apply {
                                    moveTo(strokeItem.points.first().x, strokeItem.points.first().y)
                                    strokeItem.points.drop(1).forEach { point -> lineTo(point.x, point.y) }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(strokeItem.color),
                                    style = Stroke(width = strokeItem.width, cap = StrokeCap.Round),
                                )
                            }
                        }

                        if (currentStrokePoints.size > 1) {
                            val path = Path().apply {
                                moveTo(currentStrokePoints.first().x, currentStrokePoints.first().y)
                                currentStrokePoints.drop(1).forEach { point -> lineTo(point.x, point.y) }
                            }
                            drawPath(
                                path = path,
                                color = Color(drawColor),
                                style = Stroke(width = drawStrokeWidth, cap = StrokeCap.Round),
                            )
                        }
                    }
                }

                if (overlayText.isNotBlank()) {
                    Text(
                        text = overlayText,
                        color = Color(overlayColor),
                        fontSize = (overlayTextSize * overlayTextScale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                IntOffset(
                                    x = (overlayX * canvasWidth).roundToInt(),
                                    y = (overlayY * canvasHeight).roundToInt(),
                                )
                            }
                            .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                            .pointerInput(canvasWidth, canvasHeight) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    overlayX = ((overlayX * canvasWidth + pan.x) / canvasWidth).coerceIn(0.02f, 0.90f)
                                    overlayY = ((overlayY * canvasHeight + pan.y) / canvasHeight).coerceIn(0.05f, 0.90f)
                                    overlayTextScale = (overlayTextScale * zoom).coerceIn(0.5f, 4f)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }

                // Stickers rendering
                stickers.forEach { sticker ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(sticker.x.roundToInt(), sticker.y.roundToInt()) }
                            .size(80.dp)
                            .pointerInput(sticker.id) {
                                detectDragGestures(
                                    onDragStart = { selectedStickerId = sticker.id },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        sticker.x += dragAmount.x
                                        sticker.y += dragAmount.y
                                        selectedStickerId = sticker.id
                                    },
                                    onDragEnd = { selectedStickerId = sticker.id },
                                )
                            }
                            .border(
                                width = if (selectedStickerId == sticker.id) 2.dp else 0.dp,
                                color = if (selectedStickerId == sticker.id) HBlue else Color.Transparent,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = sticker.emoji,
                            fontSize = (36.sp * sticker.scale),
                        )
                        if (selectedStickerId == sticker.id) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = HBlue,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .clickable {
                                        stickers.remove(sticker)
                                        selectedStickerId = null
                                    }
                                    .padding(2.dp),
                            )
                        }
                    }
                }

                if (selectedTool == TOOL_TEXT) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Write on image", color = HWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        OutlinedTextField(
                            value = overlayText,
                            onValueChange = {
                                overlayText = it.take(140)
                                caption = it.take(140)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type text to place on the story", color = HGray4) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HWhite,
                                unfocusedTextColor = HWhite,
                                focusedBorderColor = HBlue,
                                unfocusedBorderColor = HBorder,
                                cursorColor = HBlue,
                                focusedContainerColor = HBg.copy(alpha = 0.7f),
                                unfocusedContainerColor = HBg.copy(alpha = 0.7f),
                            ),
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            overlayColors.forEach { colorValue ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorValue))
                                        .border(
                                            width = if (overlayColor == colorValue) 2.dp else 1.dp,
                                            color = if (overlayColor == colorValue) HBlue else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape,
                                        )
                                        .clickable { overlayColor = colorValue },
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Size:", color = HGray4, fontSize = 12.sp)
                            IconButton(onClick = { overlayTextSize = (overlayTextSize - 2f).coerceAtLeast(12f) }) {
                                Text("A-", color = HWhite)
                            }
                            Text("${overlayTextSize.toInt()}", color = HWhite, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            IconButton(onClick = { overlayTextSize = (overlayTextSize + 2f).coerceAtMost(96f) }) {
                                Text("A+", color = HWhite)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Drag the text to position it", color = HGray4, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    overlayText = if (overlayText.isBlank()) "New text" else overlayText + "\nNew text"
                                    caption = overlayText
                                }) {
                                }
                                TextButton(onClick = { onCaption(caption, overlayText, overlayX, overlayY, overlayColor) }) {
                                    Text("Done", color = HBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (selectedTool == TOOL_DRAW) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Draw on image", color = HWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            drawColors.forEach { colorValue ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorValue))
                                        .border(
                                            width = if (drawColor == colorValue) 2.dp else 1.dp,
                                            color = if (drawColor == colorValue) HBlue else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape,
                                        )
                                        .clickable { drawColor = colorValue },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Long-press on canvas and drag up/down to adjust brush size", color = HGray4, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { drawStrokeWidth = (drawStrokeWidth - 1f).coerceAtLeast(1f) }) {
                                    Text("-", color = HWhite)
                                }
                                Text("${drawStrokeWidth.toInt()}", color = HWhite, modifier = Modifier.align(Alignment.CenterVertically))
                                IconButton(onClick = { drawStrokeWidth = (drawStrokeWidth + 1f).coerceAtMost(72f) }) {
                                    Text("+", color = HWhite)
                                }
                            }

                            Row {
                                TextButton(onClick = {
                                    if (drawStrokes.isNotEmpty()) drawStrokes.removeAt(drawStrokes.lastIndex)
                                }) {
                                    Text("Undo", color = HWhite)
                                }
                                TextButton(onClick = { drawStrokes.clear() }) {
                                    Text("Clear", color = HWhite)
                                }
                                TextButton(onClick = { onCaption(caption, overlayText, overlayX, overlayY, overlayColor) }) {
                                    Text("Done", color = HBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (selectedTool == "sticker") {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Add a sticker", color = HWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val stickerEmojis = listOf("😊", "🔥", "❤️", "👍", "😂", "🎉", "✨", "🌟", "💯", "🚀", "👀", "🎶")
                        Column {
                            for (rowIndex in stickerEmojis.indices step 4) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    stickerEmojis.drop(rowIndex).take(4).forEach { emoji ->
                                        Button(onClick = {
                                            stickers.add(StickerItem(stickers.size + 1, emoji, canvasWidth / 2f - 40f, canvasHeight / 2f - 40f))
                                        }, modifier = Modifier.height(36.dp).weight(1f)) {
                                            Text(emoji)
                                        }
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Drag to move, tap × to delete", color = HGray4, fontSize = 12.sp)
                            TextButton(onClick = { /* nothing */ }) { Text("Done", color = HBlue) }
                        }
                    }
                } else if (selectedTool == "music") {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Pick a track", color = HWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = musicSearchQuery,
                            onValueChange = { musicSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search music...", color = HGray4) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HWhite,
                                unfocusedTextColor = HWhite,
                                focusedBorderColor = HBlue,
                                unfocusedBorderColor = HBorder,
                                cursorColor = HBlue,
                                focusedContainerColor = HBg.copy(alpha = 0.7f),
                                unfocusedContainerColor = HBg.copy(alpha = 0.7f),
                            ),
                            singleLine = true,
                        )
                        val filteredTracks = musicTracks.filter { t ->
                            t.title.contains(musicSearchQuery, ignoreCase = true) || t.artist.contains(musicSearchQuery, ignoreCase = true)
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredTracks) { t ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedMusic?.id == t.id) HBlue.copy(alpha = 0.18f) else HBg.copy(alpha = 0.45f))
                                        .border(1.dp, if (selectedMusic?.id == t.id) HBlue else HBorder, RoundedCornerShape(14.dp))
                                        .clickable { selectedMusic = t }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(HCard),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("♪", color = HWhite, fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(t.title, color = HWhite, fontWeight = FontWeight.SemiBold)
                                        Text(t.artist, color = HGray4, fontSize = 12.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(HGray4.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("▶", color = HWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Selected:", color = HGray4)
                            Text(selectedMusic?.title ?: "None", color = HWhite)
                        }
                    }
                } else if (selectedTool == "filter") {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Filters", color = HWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filters.forEach { f ->
                                Button(onClick = { selectedFilter = f }, modifier = Modifier.height(36.dp)) {
                                    Text(f.label)
                                }
                            }
                        }
                    }
                }

                if (selectedTool == "sticker" && selectedStickerId != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp, bottom = 170.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(HCard)
                            .border(1.dp, HBorder, RoundedCornerShape(999.dp))
                            .clickable {
                                stickers.removeAll { it.id == selectedStickerId }
                                selectedStickerId = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Delete sticker", color = HWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = HWhite)
            }
            Text("Edit Story", color = HWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onCaption(caption, overlayText, overlayX, overlayY, overlayColor) }) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = HBlue)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HCard)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    TOOL_DRAW to "✏️ Draw",
                    TOOL_TEXT to "📝 Text",
                    "sticker" to "😊 Sticker",
                    "music" to "🎵 Music",
                    "filter" to "🎨 Filter",
                ).forEach { (toolKey, label) ->
                    Button(
                        onClick = {
                            selectedTool = toolKey
                            if (toolKey == TOOL_TEXT && overlayText.isBlank()) {
                                overlayText = caption.ifBlank { "Type here" }
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTool == toolKey) HBlue else HBorder,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(label, fontSize = 12.sp, color = HWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewSurface(imageCaptureRef: MutableState<ImageCapture?>) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                imageCaptureRef.value = imageCapture
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

private fun createTempStoryCapture(context: Context): Pair<File, Uri> {
    val file = File(context.cacheDir, "story_${System.currentTimeMillis()}.jpg")
    return file to Uri.fromFile(file)
}

@Composable
private fun StoryPrivacyScreen(
    selectedPrivacy: String,
    onPrivacySelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedPrivacyState by remember { mutableStateOf(selectedPrivacy) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Back", tint = HWhite)
                }
                Text("Share to Story", color = HWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(40.dp))
            }

            Spacer(Modifier.height(24.dp))

            val options = listOf(
                Triple("public", "🌍 Public", "Anyone can see"),
                Triple("friends", "👥 Friends", "Friends can see"),
                Triple("only-me", "🔒 Only Me", "Only you can see"),
                Triple("close-friends", "⭐ Close Friends", "Selected friends"),
            )

            options.forEach { option ->
                PrivacyOptionCard(
                    icon = option.second.substringBefore(" "),
                    title = option.second.substringAfter(" "),
                    subtitle = option.third,
                    isSelected = selectedPrivacyState == option.first,
                    onClick = { selectedPrivacyState = option.first },
                )
            }
        }

        Button(
            onClick = { onPrivacySelected(selectedPrivacyState) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Share Now", color = HWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PrivacyOptionCard(
    icon: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) HBlue.copy(alpha = 0.14f) else HCard)
            .border(2.dp, if (isSelected) HBlue else HBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) HBlue else HCard),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, fontSize = 20.sp)
            }

            Column(Modifier.weight(1f)) {
                Text(title, color = HWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = HGray4, fontSize = 13.sp)
            }

            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = HBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}