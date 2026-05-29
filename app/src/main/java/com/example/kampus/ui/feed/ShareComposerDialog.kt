package com.example.kampus.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal data class ShareComposerDraft(
    val caption: String,
    val mediaUris: List<Uri>,
    val mediaTypes: List<PostItem.MediaType>,
    val visibility: PostItem.PostVisibility,
    val taggedPeople: List<String>,
)

private data class ShareComposerUser(
    val displayName: String,
    val avatarEmoji: String,
    val profileImageUrl: String,
)

private enum class ShareAudience(val label: String, val visibility: PostItem.PostVisibility) {
    FRIENDS_EXCEPT("Friends except...", PostItem.PostVisibility.FRIENDS),
    PUBLIC("Public", PostItem.PostVisibility.PUBLIC),
    FRIENDS("Friends", PostItem.PostVisibility.FRIENDS),
    FOLLOWERS("Followers", PostItem.PostVisibility.FOLLOWERS),
    ONLY_ME("Only me", PostItem.PostVisibility.PRIVATE),
}

@Composable
internal fun ShareComposerDialog(
    originalPost: PostItem? = null,
    sourceTitle: String = "",
    sourceBody: String = "",
    sourceAvatarUrl: String = "",
    sourceAvatarText: String = "",
    sourceLabel: String = "Shared item",
    onDismiss: () -> Unit,
    onShareNow: suspend (ShareComposerDraft) -> Result<String>,
) {
    val p = getComposerPalette()
    val strings = com.example.kampus.ui.localization.rememberUiStrings()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser by rememberCurrentShareComposerUser()
    var caption by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf(ShareAudience.FRIENDS_EXCEPT) }
    var selectedPeople by remember { mutableStateOf(emptyList<String>()) }
    var attachedUris by remember { mutableStateOf(emptyList<Uri>()) }
    var attachedTypes by remember { mutableStateOf(emptyList<PostItem.MediaType>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }

    val canShare = true

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                val mime = context.contentResolver.getType(uri)
                val mediaType = when {
                    mime?.startsWith("video/") == true -> PostItem.MediaType.VIDEO
                    mime?.startsWith("image/") == true -> PostItem.MediaType.IMAGE
                    else -> null
                }
                if (mediaType != null) {
                    attachedUris = attachedUris + uri
                    attachedTypes = attachedTypes + mediaType
                }
            }
        },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + scaleOut(targetScale = 0.96f),
            ) {
                Surface(
                    color = p.bg,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 18.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 560.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(p.card)
                                        .border(1.dp, p.border, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (currentUser.profileImageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = currentUser.profileImageUrl,
                                            contentDescription = currentUser.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Text(
                                            text = currentUser.avatarEmoji.ifBlank { currentUser.displayName.take(1).uppercase() },
                                            color = p.text,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = currentUser.displayName,
                                        color = p.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Share to Feed",
                                        color = p.textMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(p.card)
                                    .border(1.dp, p.border, CircleShape)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = p.text, modifier = Modifier.size(18.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.card)
                                .border(1.dp, p.border, RoundedCornerShape(999.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    menuExpanded = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = p.textMuted, modifier = Modifier.size(14.dp))
                                Text(audience.label, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = p.textMuted, modifier = Modifier.size(14.dp))
                            }

                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                ShareAudience.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            audience = option
                                            menuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(p.card)
                                .border(1.dp, p.border, RoundedCornerShape(18.dp))
                                .padding(14.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BasicTextField(
                                    value = caption,
                                    onValueChange = { caption = it },
                                    textStyle = TextStyle(color = p.text, fontSize = 16.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (caption.isBlank()) {
                                            Text(
                                                text = "Say something about this...",
                                                color = p.placeholder,
                                                fontSize = 16.sp,
                                            )
                                        }
                                        inner()
                                    },
                                )

                                if (originalPost != null) {
                                    // Beautiful Facebook-style embedded preview card of the original post
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(p.bg)
                                            .border(1.dp, p.border, RoundedCornerShape(12.dp)),
                                    ) {
                                        Column {
                                            // Header
                                            Row(
                                                modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                // Mini avatar
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(p.card)
                                                        .border(1.dp, p.border, CircleShape),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (originalPost.profileImageUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = originalPost.profileImageUrl,
                                                            contentDescription = originalPost.author,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                    } else {
                                                        Text(originalPost.avatar.ifBlank { "👤" }, color = p.text, fontSize = 14.sp)
                                                    }
                                                }
                                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    ) {
                                                        Text(
                                                            originalPost.author,
                                                            color = p.text,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                        if (originalPost.isVerified) {
                                                            Icon(Icons.Default.Verified, "Verified", tint = p.primary, modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                    Text(originalPost.time, color = p.textMuted, fontSize = 10.sp)
                                                }
                                            }

                                            // Caption
                                            if (originalPost.content.isNotBlank()) {
                                                Text(
                                                    originalPost.content,
                                                    color = p.text,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                )
                                            }

                                            // Media
                                            val mediaUris = originalPost.mediaUris
                                            val mediaTypes = originalPost.mediaTypes
                                            val mediaEmojis = originalPost.mediaEmojis
                                            when {
                                                mediaUris.isNotEmpty() -> {
                                                    val firstUri = mediaUris.first()
                                                    val firstType = mediaTypes.getOrNull(0) ?: PostItem.MediaType.IMAGE
                                                    Spacer(Modifier.height(6.dp))
                                                    if (firstType == PostItem.MediaType.VIDEO) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(160.dp)
                                                                .background(Color(0xFF0D0D0D)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            AsyncImage(
                                                                model = firstUri,
                                                                contentDescription = "Video thumbnail",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop,
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(Color.Black.copy(alpha = 0.35f)),
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(42.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color.Black.copy(alpha = 0.65f)),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    Icons.Filled.PlayArrow,
                                                                    contentDescription = "Play",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(24.dp),
                                                                )
                                                            }
                                                            if (mediaUris.size > 1) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomStart)
                                                                        .padding(8.dp)
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color.Black.copy(alpha = 0.65f))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                                ) {
                                                                    Text("+${mediaUris.size - 1} more", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        if (mediaUris.size == 1) {
                                                            AsyncImage(
                                                                model = firstUri,
                                                                contentDescription = "Post media",
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .heightIn(min = 140.dp, max = 220.dp),
                                                                contentScale = ContentScale.Crop,
                                                            )
                                                        } else {
                                                            Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                AsyncImage(
                                                                    model = firstUri,
                                                                    contentDescription = "Media 1",
                                                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                                                    contentScale = ContentScale.Crop,
                                                                )
                                                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                                    AsyncImage(
                                                                        model = mediaUris[1],
                                                                        contentDescription = "Media 2",
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        contentScale = ContentScale.Crop,
                                                                    )
                                                                    if (mediaUris.size > 2) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .fillMaxSize()
                                                                                .background(Color.Black.copy(alpha = 0.5f)),
                                                                            contentAlignment = Alignment.Center,
                                                                        ) {
                                                                            Text(
                                                                                "+${mediaUris.size - 2}",
                                                                                color = Color.White,
                                                                                fontSize = 18.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                mediaEmojis.isNotEmpty() -> {
                                                    Spacer(Modifier.height(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(80.dp)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    listOf(p.border.copy(0.2f), Color(0xFF050810).copy(0.4f))
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(mediaEmojis.joinToString(" "), fontSize = 32.sp)
                                                    }
                                                }
                                            }

                                            // Stats line
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text("👍", fontSize = 10.sp)
                                                    Text("❤️", fontSize = 10.sp)
                                                    Text("${originalPost.likes} likes", color = p.textMuted, fontSize = 11.sp)
                                                }
                                                Text("${originalPost.comments} comments • ${originalPost.shares} shares", color = p.textMuted, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else if (sourceTitle.isNotBlank() || sourceBody.isNotBlank()) {
                                    // Fallback for events/other shares
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(p.bg)
                                            .border(1.dp, p.border.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                            .padding(12.dp),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(p.card)
                                                        .border(1.dp, p.border, CircleShape),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (sourceAvatarUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = sourceAvatarUrl,
                                                            contentDescription = sourceTitle,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                    } else {
                                                        Text(sourceAvatarText.ifBlank { sourceTitle.take(1).uppercase() }, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(sourceTitle, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(sourceLabel, color = p.textMuted, fontSize = 11.sp)
                                                }
                                            }
                                            if (sourceBody.isNotBlank()) {
                                                Text(sourceBody, color = p.text, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }

                                if (attachedUris.isNotEmpty()) {
                                    Text(
                                        text = "${attachedUris.size} attachment${if (attachedUris.size > 1) "s" else ""} ready",
                                        color = p.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        if (selectedPeople.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Tagging:", color = p.textMuted, fontSize = 11.sp)
                                selectedPeople.take(3).forEach { person ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(p.card)
                                            .border(1.dp, p.border, RoundedCornerShape(999.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text(person, color = p.text, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = p.border.copy(alpha = 0.55f), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RoundActionIcon(
                                    icon = Icons.Default.PhotoLibrary,
                                    contentDescription = "Add photo",
                                    onClick = { mediaLauncher.launch("*/*") },
                                    p = p,
                                )
                                RoundActionIcon(
                                    icon = Icons.Default.PersonAdd,
                                    contentDescription = "Tag people",
                                    onClick = { showPeoplePicker = true },
                                    p = p,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (canShare) p.primary else p.card)
                                    .border(1.dp, if (canShare) p.primary else p.border, RoundedCornerShape(14.dp))
                                    .clickable(
                                        enabled = canShare && !isSubmitting,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        scope.launch {
                                            isSubmitting = true
                                            val result = onShareNow(
                                                ShareComposerDraft(
                                                    caption = caption.trim(),
                                                    mediaUris = attachedUris,
                                                    mediaTypes = attachedTypes,
                                                    visibility = audience.visibility,
                                                    taggedPeople = selectedPeople,
                                                )
                                            )
                                            isSubmitting = false
                                            if (result.isSuccess) {
                                                onDismiss()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isSubmitting) "Sharing..." else "Share now",
                                    color = if (canShare && !isSubmitting) Color.White else p.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPeoplePicker) {
            Dialog(
                onDismissRequest = { showPeoplePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.95f),
                ) {
                    PeoplePicker(
                        p = p,
                        selected = selectedPeople,
                        onSelect = { selectedPeople = it },
                        onClose = { showPeoplePicker = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCurrentShareComposerUser(): androidx.compose.runtime.State<ShareComposerUser> {
    val auth = FirebaseAuth.getInstance()
    return produceState(
        initialValue = ShareComposerUser(
            displayName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "You",
            avatarEmoji = "👤",
            profileImageUrl = auth.currentUser?.photoUrl?.toString().orEmpty(),
        ),
        key1 = auth.currentUser?.uid,
    ) {
        val uid = auth.currentUser?.uid ?: return@produceState
        val authUser = auth.currentUser
        val fallbackName = authUser?.displayName?.takeIf { it.isNotBlank() } ?: "You"
        val fallbackImage = authUser?.photoUrl?.toString().orEmpty()
        val snapshot = runCatching {
            FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        }.getOrNull()

        val displayName = snapshot?.getString("displayName")?.takeIf { it.isNotBlank() } ?: fallbackName
        val avatarEmoji = snapshot?.getString("avatarEmoji")?.takeIf { it.isNotBlank() } ?: "👤"
        val profileImageUrl = snapshot?.getString("profileImageUrl")?.takeIf { it.isNotBlank() } ?: fallbackImage

        value = ShareComposerUser(displayName, avatarEmoji, profileImageUrl)
    }
}

@Composable
private fun RoundActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    p: ComposerPalette,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(p.card)
            .border(1.dp, p.border, CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = p.text, modifier = Modifier.size(18.dp))
    }
}