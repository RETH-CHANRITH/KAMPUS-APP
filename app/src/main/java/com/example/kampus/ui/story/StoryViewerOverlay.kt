package com.example.kampus.ui.story

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.kampus.ui.chat.ChatStory
import com.example.kampus.ui.chat.ChatViewModel
import com.example.kampus.ui.chat.StoryViewer
import com.example.kampus.ui.theme.ThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Color tokens matching Kampus Design System
private val UiIsDark get() = ThemeController.isDark
private val HBg get() = if (UiIsDark) Color(0xFF080B11) else Color(0xFFF3F4F8)
private val HCard get() = if (UiIsDark) Color(0xFF0F1520) else Color(0xFFFFFFFF)
private val HBorder get() = if (UiIsDark) Color(0xFF1A2333) else Color(0xFFD1D5DB)
private val HBlue get() = ThemeController.accent.color
private val HWhite get() = Color.White
private val HGray2 get() = if (UiIsDark) Color(0xFFE5E7EB) else Color(0xFF374151)
private val HGray4 get() = if (UiIsDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

@Composable
fun StoryViewerOverlay(
    stories: List<ChatStory>,
    startStoryId: String,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
) {
    if (stories.isEmpty()) return

    var currentIndex by remember(stories, startStoryId) {
        mutableIntStateOf(stories.indexOfFirst { it.id == startStoryId }.coerceAtLeast(0))
    }
    val progress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentStory = stories.getOrNull(currentIndex) ?: return
    val viewersFlow = remember(currentStory.id) { viewModel.observeStoryViewers(currentStory.id) }
    val viewersResult by viewersFlow.collectAsStateWithLifecycle(initialValue = Result.success(emptyList<StoryViewer>()))
    val viewers = viewersResult.getOrNull().orEmpty()

    var replyText by remember(currentStory.id) { mutableStateOf("") }
    var sendingReply by remember(currentStory.id) { mutableStateOf(false) }

    // Swipe down to close offset
    var dragOffsetY by remember { mutableStateOf(0f) }
    val localDensity = LocalDensity.current

    fun goNext() {
        if (currentIndex < stories.lastIndex) {
            currentIndex += 1
        } else {
            onDismiss()
        }
    }

    fun goPrev() {
        if (currentIndex > 0) {
            currentIndex -= 1
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(startStoryId, stories.size) {
        currentIndex = stories.indexOfFirst { it.id == startStoryId }.coerceAtLeast(0)
    }

    LaunchedEffect(currentStory.id) {
        progress.snapTo(0f)
        viewModel.markStoryViewed(currentStory)

        val durationMs = 4500L
        val tickMs = 45L
        var elapsed = 0L
        while (elapsed < durationMs) {
            if (!isHolding) {
                elapsed += tickMs
                progress.snapTo((elapsed.toFloat() / durationMs).coerceIn(0f, 1f))
            }
            delay(tickMs)
        }

        goNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (1f - (dragOffsetY / 1000f).coerceIn(0f, 0.6f))))
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetY > 150f * localDensity.density) {
                            onDismiss()
                        } else {
                            dragOffsetY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 0) {
                            dragOffsetY += dragAmount.y
                        }
                    }
                )
            }
            .systemBarsPadding(),
    ) {
        // Full screen media display (image / video / note)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp), // leave room for reply bar
            contentAlignment = Alignment.Center
        ) {
            if (currentStory.storyType == "video" && currentStory.imageUrl.isNotBlank()) {
                StoryVideoPlayer(
                    url = currentStory.imageUrl,
                    isPaused = isHolding,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (currentStory.storyType == "image" && currentStory.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = currentStory.imageUrl,
                    contentDescription = "Story media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Note-only story
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E2A3B), Color(0xFF0F172A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStory.note,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            // Overlay Text
            if (currentStory.overlayText.isNotBlank()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    Text(
                        text = currentStory.overlayText,
                        color = Color(currentStory.overlayColor),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (currentStory.overlayX * canvasWidth).roundToInt(),
                                    y = (currentStory.overlayY * canvasHeight).roundToInt(),
                                )
                            }
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom Caption/Note Overlay (if it has media AND a caption/note)
            if ((currentStory.storyType == "image" || currentStory.storyType == "video") && currentStory.note.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentStory.note,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Tap targets: Left (33%) for Previous, Right (67%) for Next
        Row(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(currentStory.id, currentIndex) {
                        detectTapGestures(
                            onTap = { goPrev() },
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            },
                        )
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
                    .pointerInput(currentStory.id, currentIndex) {
                        detectTapGestures(
                            onTap = { goNext() },
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            },
                        )
                    },
            )
        }

        // Top bar overlays: Progress bars, profile info, close button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Segmented Progress Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stories.forEachIndexed { index, story ->
                    val fill = when {
                        index < currentIndex -> 1f
                        index > currentIndex -> 0f
                        else -> progress.value
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(if (story.isMine) HBlue else HWhite),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Profile info & Timestamp & Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(currentStory.ownerAvatarColor).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (currentStory.ownerProfileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentStory.ownerProfileImageUrl,
                                contentDescription = currentStory.ownerName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(currentStory.ownerAvatarEmoji, color = HWhite)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(currentStory.ownerName, color = HWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(currentStory.createdAtLabel, color = HGray4, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HWhite)
                }
            }
        }

        // Bottom section: Replies, Reactions and Seen By Section (aligned to bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick reaction buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("❤️", "😂", "🔥", "👏", "😮").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                if (sendingReply) return@clickable
                                sendingReply = true
                                coroutineScope.launch {
                                    viewModel.createStoryReply(currentStory, emoji)
                                    sendingReply = false
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            // Inline text reply field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Send reply...", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        cursorColor = HBlue,
                        focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        val trimmed = replyText.trim()
                        if (trimmed.isBlank() || sendingReply) return@IconButton
                        sendingReply = true
                        coroutineScope.launch {
                            viewModel.createStoryReply(currentStory, trimmed)
                            replyText = ""
                            sendingReply = false
                        }
                    },
                    enabled = replyText.isNotBlank() && !sendingReply,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (replyText.isNotBlank()) HBlue else Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }

            // Seen By section (if it's my story)
            if (currentStory.isMine && viewers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Can show full viewers bottom sheet or list
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seen by ${viewers.size} ${if (viewers.size == 1) "person" else "people"}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryVideoPlayer(
    url: String,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 1f
        }
    }

    LaunchedEffect(isPaused) {
        player.playWhenReady = !isPaused
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
            }
        },
        update = { it.player = player },
    )
}
