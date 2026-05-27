@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.feed

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kampus.ui.theme.ThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class FbComposerTheme { A_LIGHT, B_DARK }

private enum class PickerType { FEELING, PEOPLE, LOCATION, EVENT, GIF }

@Composable
fun CreatePostScreen(
    onClose: () -> Unit,
    onPost: suspend (text: String, mediaUris: List<Uri>, mediaTypes: List<PostItem.MediaType>, visibility: PostItem.PostVisibility, allowComments: Boolean, taggedPeople: List<String>, feelingEmoji: String?, location: String?) -> Result<String>,
) {
    val p = getComposerPalette()
    val strings = com.example.kampus.ui.localization.rememberUiStrings()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var text by remember { mutableStateOf("") }

    // Support multiple media items
    var mediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaTypes by remember { mutableStateOf<List<PostItem.MediaType>>(emptyList()) }

    // Keep old single media for backward compatibility with onPost callback
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf<PostItem.MediaType?>(null) }

    var visibility by remember { mutableStateOf(PostItem.PostVisibility.PUBLIC) }
    val allowComments = true

    var taggedPeople by remember { mutableStateOf(emptyList<String>()) }
    var locationText by remember { mutableStateOf("") }
    var feelingEmoji by remember { mutableStateOf<String?>(null) }
    var eventText by remember { mutableStateOf("") }
    var musicText by remember { mutableStateOf("") }

    var pickerShown by remember { mutableStateOf<PickerType?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitSuccess by remember { mutableStateOf<String?>(null) }

    val canPost = text.isNotBlank() || mediaUris.isNotEmpty()

    // Track selected index for editing
    var selectedMediaIndex by remember { mutableStateOf<Int?>(null) }

    // Track if edit menu is shown
    var showEditMenu by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                val mime = context.contentResolver.getType(uri)
                val type = when {
                    mime?.startsWith("video/") == true -> PostItem.MediaType.VIDEO
                    mime?.startsWith("image/") == true -> PostItem.MediaType.IMAGE
                    else -> null
                }

                if (type != null) {
                    // If editing existing media at selectedMediaIndex, replace it
                    if (selectedMediaIndex != null && selectedMediaIndex!! < mediaUris.size) {
                        mediaUris = mediaUris.toMutableList().apply {
                            set(selectedMediaIndex!!, uri)
                        }
                        mediaTypes = mediaTypes.toMutableList().apply {
                            set(selectedMediaIndex!!, type)
                        }
                        selectedMediaIndex = null
                    } else {
                        // Otherwise add new media to list
                        mediaUris = mediaUris + uri
                        mediaTypes = mediaTypes + type
                    }

                    // Keep old single media for backward compatibility
                    imageUri = mediaUris.firstOrNull()
                    mediaType = mediaTypes.firstOrNull()
                }
            }
        }
    )

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val out = MediaCropper.getOutput(result.data)
                if (out != null) {
                    // If editing specific media index, update that item
                    if (selectedMediaIndex != null && selectedMediaIndex!! < mediaUris.size) {
                        mediaUris = mediaUris.toMutableList().apply {
                            set(selectedMediaIndex!!, out)
                        }
                        mediaTypes = mediaTypes.toMutableList().apply {
                            set(selectedMediaIndex!!, PostItem.MediaType.IMAGE)
                        }
                        selectedMediaIndex = null
                    } else {
                        // Backward compatibility: update single media
                        imageUri = out
                        mediaType = PostItem.MediaType.IMAGE
                    }
                }
            }
        },
    )

    // Pickers modal
    when (pickerShown) {
        PickerType.FEELING -> {
            Dialog(
                onDismissRequest = { pickerShown = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    FeelingEmojiPicker(
                        p = p,
                        onSelect = { emoji ->
                            feelingEmoji = emoji
                            pickerShown = null
                        },
                        onClose = { pickerShown = null },
                    )
                }
            }
        }
        PickerType.PEOPLE -> {
            Dialog(
                onDismissRequest = { pickerShown = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    PeoplePicker(
                        p = p,
                        selected = taggedPeople,
                        onSelect = { people ->
                            taggedPeople = people
                            pickerShown = null
                        },
                        onClose = { pickerShown = null },
                    )
                }
            }
        }
        PickerType.LOCATION -> {
            Dialog(
                onDismissRequest = { pickerShown = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    LocationPicker(
                        p = p,
                        onSelect = { loc ->
                            locationText = loc
                            pickerShown = null
                        },
                        onClose = { pickerShown = null },
                    )
                }
            }
        }
        PickerType.EVENT -> {
            Dialog(
                onDismissRequest = { pickerShown = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    EventPicker(
                        p = p,
                        onSelect = { event ->
                            eventText = event
                            pickerShown = null
                        },
                        onClose = { pickerShown = null },
                    )
                }
            }
        }
        PickerType.GIF -> {
            Dialog(
                onDismissRequest = { pickerShown = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    GifPicker(
                        p = p,
                        onSelect = { gif ->
                            // For now, just close. Can integrate GIF as media later.
                            pickerShown = null
                        },
                        onClose = { pickerShown = null },
                    )
                }
            }
        }
        null -> {}
    }

    Surface(color = p.bg) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            CreatePostTopBar(
                p = p,
                onClose = onClose,
                isDarkMode = ThemeController.isDark,
            )

            // Header row (avatar + name + tags)
            CreatePostHeader(
                p = p,
                taggedPeople = taggedPeople,
                locationText = locationText,
                feelingEmoji = feelingEmoji,
                eventText = eventText,
                visibility = visibility,
                onVisibilityChange = { visibility = it },
            )

            // Chips row (horizontally scrollable)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(
                    listOf(
                        Pair(Icons.Default.SentimentSatisfied, "Feeling") to PickerType.FEELING,
                        Pair(Icons.Default.PersonAdd, "People") to PickerType.PEOPLE,
                        Pair(Icons.Default.Place, "Location") to PickerType.LOCATION,
                        Pair(Icons.Default.CalendarMonth, "Event") to PickerType.EVENT,
                        Pair(Icons.Default.PhotoLibrary, "Album") to null,
                    )
                ) { (iconLabel, pickerType) ->
                    val (icon, label) = iconLabel
                    FbChipPill(
                        p = p,
                        icon = icon,
                        label = label,
                        onClick = {
                            pickerType?.let { pickerShown = it }
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Media gallery - show all selected media in a scrollable row
            if (mediaUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(mediaUris.size) { index ->
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(240.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(p.card)
                                .border(1.dp, p.border, RoundedCornerShape(18.dp)),
                        ) {
                            // Display image or video
                            if (mediaTypes.getOrNull(index) == PostItem.MediaType.VIDEO) {
                                val player = remember(mediaUris[index]) {
                                    ExoPlayer.Builder(context).build().apply {
                                        setMediaItem(MediaItem.fromUri(mediaUris[index]))
                                        prepare()
                                        playWhenReady = false
                                    }
                                }
                                DisposableEffect(player) {
                                    onDispose { player.release() }
                                }
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = true
                                            this.player = player
                                        }
                                    },
                                    update = { it.player = player },
                                )
                            } else {
                                AsyncImage(
                                    model = mediaUris[index],
                                    contentDescription = "Selected media $index",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }

                            // Delete button for this media
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(p.bg.copy(alpha = 0.8f))
                                    .border(1.dp, p.border, CircleShape)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        // Remove this media item
                                        mediaUris = mediaUris.filterIndexed { i, _ -> i != index }
                                        mediaTypes = mediaTypes.filterIndexed { i, _ -> i != index }
                                        // Update single media for compatibility
                                        imageUri = mediaUris.firstOrNull()
                                        mediaType = mediaTypes.firstOrNull()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Remove",
                                    tint = p.text,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Edit/Replace button for this media
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.primary.copy(alpha = 0.85f))
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            showEditMenu = index
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "Edit",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                // Edit menu dropdown
                                DropdownMenu(
                                    expanded = showEditMenu == index,
                                    onDismissRequest = { showEditMenu = null },
                                    modifier = Modifier.background(p.card),
                                ) {
                                    // Crop option (only for images)
                                    if (mediaTypes.getOrNull(index) == PostItem.MediaType.IMAGE) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        Icons.Default.CropSquare,
                                                        "Crop",
                                                        tint = p.text,
                                                        modifier = Modifier.size(18.dp).padding(end = 8.dp)
                                                    )
                                                    Text(strings.crop, color = p.text)
                                                }
                                            },
                                            onClick = {
                                                selectedMediaIndex = index
                                                val mediaUri = mediaUris[index]
                                                val cropIntent = MediaCropper.createCropIntent(context, mediaUri)
                                                cropLauncher.launch(cropIntent)
                                                showEditMenu = null
                                            },
                                        )
                                    }

                                    // Change/Replace option
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    Icons.Default.PhotoLibrary,
                                                    "Change",
                                                    tint = p.text,
                                                    modifier = Modifier.size(18.dp).padding(end = 8.dp)
                                                )
                                                Text(strings.change, color = p.text)
                                            }
                                        },
                                        onClick = {
                                            selectedMediaIndex = index
                                            galleryLauncher.launch("*/*")
                                            showEditMenu = null
                                        },
                                    )
                                }
                            }

                            // Media count badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(p.primary)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${index + 1}/${mediaUris.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Add more media button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.card)
                        .border(1.dp, p.border, RoundedCornerShape(16.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            selectedMediaIndex = null // Add new, don't replace
                            galleryLauncher.launch("*/*")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Add, "Add more", tint = p.primary, modifier = Modifier.size(18.dp))
                        Text(strings.addMorePhotos, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(10.dp))
            } else {
                // Show initial gallery upload button when no media selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.card)
                        .border(2.dp, p.border, RoundedCornerShape(16.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            galleryLauncher.launch("*/*")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            "Add media",
                            tint = p.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(strings.addPhotosOrVideos, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(strings.tapToSelect, color = p.textMuted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            // Text input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = p.text, fontSize = 18.sp),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (text.isBlank()) {
                            Text(
                                text = "What's on your mind?",
                                color = p.placeholder,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        inner()
                    }
                )
            }

            if (isSubmitting || !submitError.isNullOrBlank() || !submitSuccess.isNullOrBlank()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSubmitting) {
                        Text("Uploading and syncing to Home…", color = p.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (!submitSuccess.isNullOrBlank()) {
                        Text(submitSuccess!!, color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (!submitError.isNullOrBlank()) {
                        Text(submitError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Bottom action bar (facebook-like)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(p.bg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionPill(p, "Gallery") { galleryLauncher.launch("*/*") }
                    ActionPill(p, "GIF") { pickerShown = PickerType.GIF }
                    ActionPill(p, "Video") { galleryLauncher.launch("video/*") }
                    ActionPill(p, "Live")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (canPost) p.primary else p.card)
                            .border(1.dp, if (canPost) p.primary else p.border, RoundedCornerShape(12.dp))
                            .clickable(
                                enabled = canPost && !isSubmitting,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                scope.launch {
                                    if (isSubmitting) return@launch
                                    isSubmitting = true
                                    submitError = null
                                    submitSuccess = null

                                    val result = onPost(
                                        text.trim(),
                                        mediaUris,
                                        mediaTypes,
                                        visibility,
                                        allowComments,
                                        taggedPeople,
                                        feelingEmoji,
                                        locationText,
                                    )

                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        submitSuccess = "Posted and synced to Firebase"
                                        delay(700)
                                        onClose()
                                    } else {
                                        submitError = result.exceptionOrNull()?.message ?: "Failed to sync post"
                                    }
                                }
                            },
                    ) {
                        Text(
                            text = "Post",
                            color = if (canPost && !isSubmitting) Color.White else p.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostChip(p: ComposerPalette, label: String, emoji: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, fontSize = 14.sp)
            Text(label, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PostChip(p: ComposerPalette, label: String, emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(999.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, fontSize = 14.sp)
            Text(label, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun FbChipPill(
    p: ComposerPalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(999.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = p.textMuted, modifier = Modifier.size(18.dp))
        Text(label, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoundIconButton(
    p: ComposerPalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(p.bg.copy(alpha = 0.65f))
            .border(1.dp, p.border.copy(alpha = 0.6f), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = p.text, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RowScope.ActionPill(p: ComposerPalette, label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionSmallPill(p: ComposerPalette, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) p.card else p.bg)
            .border(1.dp, if (selected) p.border else p.border.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
