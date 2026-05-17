package com.example.kampus.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kampus.ui.theme.ThemeController

private val PubIsDark get() = ThemeController.isDark
private val Bg get() = if (PubIsDark) Color(0xFF080B11) else Color(0xFFF3F4F8)
private val Card get() = if (PubIsDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
private val Border get() = if (PubIsDark) Color(0xFF2C3552) else Color(0xFFD1D5DB)
private val TextPrimary get() = if (PubIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val TextSecondary get() = if (PubIsDark) Color(0xFF99A1AF) else Color(0xFF6B7280)
private val Blue get() = Color(0xFF0D7FFF)

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenActivity: (ProfileActivityItem) -> Unit,
    viewModel: PublicProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.observeUser(userId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        if (state.isLoading) {
            android.util.Log.d("PublicProfileScreen", "Profile still loading for userId=${state.userId}")
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
            return@Surface
        }

        android.util.Log.d("PublicProfileScreen", "Profile loaded: userId=${state.userId}, isOwnProfile=${state.isOwnProfile}, isFollowing=${state.isFollowing}, hasOutgoing=${state.hasOutgoingRequest}, hasIncoming=${state.hasIncomingRequest}")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Brush.linearGradient(
                                if (PubIsDark) listOf(Color(0xFF1a1f3a), Color(0xFF080B11)) else listOf(Color(0xFF9FB9D8), Bg)
                            )
                        ),
                ) {
                    if (state.coverImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = state.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (PubIsDark) Color.White.copy(alpha = 0.1f) else Card)
                        .border(1.dp, if (PubIsDark) Color.White.copy(alpha = 0.16f) else Border, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 48.dp)
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(4.dp, Bg, CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF20A4FF), Color(0xFF7C3AED)))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = state.profileImageUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Text(text = state.avatarEmoji, fontSize = 36.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = state.displayName.ifBlank { "Unknown user" }, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                if (state.handle.isNotBlank()) {
                    Text(text = state.handle, color = TextSecondary, fontSize = 16.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(text = state.location.ifBlank { "Location not set" }, color = TextSecondary, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(value = state.posts.toString(), label = "Posts")
                    StatItem(value = state.followers.toString(), label = "Followers")
                    StatItem(value = state.following.toString(), label = "Following")
                }
            }

            // ACTION BUTTONS SECTION - Always visible after stats
            if (!state.isOwnProfile) {
                android.util.Log.d("PublicProfileScreen", "Rendering action section: isOwnProfile=${state.isOwnProfile}, hasIncoming=${state.hasIncomingRequest}")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.hasIncomingRequest) {
                        android.util.Log.d("PublicProfileScreen", "Showing Accept/Reject buttons")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { viewModel.acceptIncomingRequest() },
                                enabled = !state.isActionLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue,
                                    contentColor = TextPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "Accept Request", fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = { viewModel.rejectIncomingRequest() },
                                enabled = !state.isActionLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Card,
                                    contentColor = TextPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "Reject", fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        android.util.Log.d("PublicProfileScreen", "Showing Follow button: isFollowing=${state.isFollowing}, hasOutgoing=${state.hasOutgoingRequest}")
                        Button(
                            onClick = { viewModel.sendOrCancelRequest() },
                            enabled = !state.isActionLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.hasOutgoingRequest || state.isFollowing) Card else Blue,
                                contentColor = TextPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val actionText = when {
                                state.isFollowing -> "Following"
                                state.hasOutgoingRequest -> "Request Sent (Tap to Cancel)"
                                else -> "Follow"
                            }
                            Text(text = actionText, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "About", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card)
                        .border(1.dp, Border, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    val hasAny = state.bio.isNotBlank() || state.faculty.isNotBlank() || state.year.isNotBlank() || state.location.isNotBlank()
                    if (hasAny) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (state.bio.isNotBlank()) {
                                AboutInfoRow(
                                    icon = Icons.Outlined.ModeEdit,
                                    text = state.bio,
                                )
                            }
                            if (state.faculty.isNotBlank()) {
                                AboutInfoRow(
                                    icon = Icons.Outlined.Stars,
                                    text = state.faculty,
                                )
                            }
                            if (state.year.isNotBlank()) {
                                val yearText = if (state.year.startsWith("Year", ignoreCase = true)) state.year else "Year ${state.year}"
                                AboutInfoRow(
                                    icon = Icons.Outlined.CalendarMonth,
                                    text = yearText,
                                )
                            }
                            if (state.location.isNotBlank()) {
                                AboutInfoRow(
                                    icon = Icons.Outlined.LocationOn,
                                    text = state.location,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No details yet",
                            color = TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Recent Activity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                val filteredActivities = state.activities.filter { it.type != "share_profile" }
                if (filteredActivities.isEmpty()) {
                    Text(text = "No public activity yet.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    filteredActivities.forEach { activity ->
                        PublicActivityRow(
                            profileUserId = state.userId.ifBlank { userId },
                            displayName = state.displayName,
                            avatarEmoji = state.avatarEmoji,
                            activity = activity,
                            viewModel = viewModel,
                            onOpenActivity = { onOpenActivity(activity) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Text(text = text, color = TextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublicActivityRow(
    profileUserId: String,
    displayName: String,
    avatarEmoji: String,
    activity: ProfileActivityItem,
    viewModel: PublicProfileViewModel,
    onOpenActivity: () -> Unit,
) {
    val context = LocalContext.current
    val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMenuSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    val accent = when (activity.type) {
        "create_event" -> Color(0xFF22C55E)
        "create_group" -> Color(0xFF8B5CF6)
        "create_post" -> Blue
        "share_profile" -> Color(0xFFF97316)
        else -> TextSecondary
    }
    val previewTitle = when {
        activity.previewTitle.isNotBlank() -> activity.previewTitle
        activity.type == "create_event" -> "Event"
        activity.type == "create_group" -> "Group"
        activity.type == "create_post" -> "Post"
        else -> "Activity"
    }
    val previewSubtitle = when {
        activity.previewSubtitle.isNotBlank() -> activity.previewSubtitle
        activity.type == "create_event" -> "Shared from your Events"
        activity.type == "create_group" -> "Shared from your Groups"
        activity.type == "create_post" -> "Shared from your Feed"
        else -> "Shared on your profile"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .border(1.dp, Border.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .padding(end = 52.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = 0.95f),
                                    Blue.copy(alpha = 0.7f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = avatarEmoji.ifBlank { displayName.take(1).uppercase() },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = displayName.ifBlank { "Unknown user" },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = formatActivityTime(activity.createdAt),
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(TextSecondary.copy(alpha = 0.8f)),
                        )
                        Text(
                            text = "Public",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Text(
                text = activity.text,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.clickable(onClick = onOpenActivity),
            )

            ActivityPreviewPanel(
                accent = accent,
                previewTitle = previewTitle,
                previewSubtitle = previewSubtitle,
                previewImageUrl = activity.previewImageUrl,
                activity = activity,
                onClick = onOpenActivity,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityAction(
                    text = "Like",
                    count = activity.likeCount,
                    icon = Icons.Outlined.FavoriteBorder,
                    accent = Color(0xFFE11D48),
                    onClick = { viewModel.likeActivity(activity) },
                )
                ActivityAction(
                    text = "Comment",
                    count = activity.commentCount,
                    icon = Icons.Outlined.ChatBubbleOutline,
                    accent = Blue,
                    onClick = onOpenActivity,
                )
                ActivityAction(
                    text = "Share",
                    count = activity.shareCount,
                    icon = Icons.Outlined.Share,
                    accent = Color(0xFF8B5CF6),
                    onClick = { showShareSheet = true },
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(52.dp)
                .zIndex(10f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showMenuSheet = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }

    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = menuSheetState,
            containerColor = Card,
            scrimColor = Color.Black.copy(alpha = 0.55f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoundedSheetGroup {
                    SheetOptionItem("Unpin post", icon = Icons.Outlined.Stars, tint = TextPrimary) { showMenuSheet = false }
                    SheetOptionItem("Save video", icon = Icons.Outlined.BookmarkBorder, tint = TextPrimary) { showMenuSheet = false }
                    SheetOptionItem("Edit post", icon = Icons.Outlined.ModeEdit, tint = TextPrimary) { showMenuSheet = false }
                    SheetOptionItem("Edit privacy", icon = Icons.Outlined.Lock, tint = TextPrimary) { showMenuSheet = false }
                    SheetOptionItem("Move to archive", icon = Icons.Outlined.Archive, tint = TextPrimary) { showMenuSheet = false }
                    SheetOptionItem(
                        label = "Move to trash",
                        subtitle = "Items in your trash are deleted after 30 days.",
                        icon = Icons.Outlined.Delete,
                        tint = Color(0xFFE11D48),
                    ) { showMenuSheet = false }
                    SheetOptionItem("Get notified about this post", icon = Icons.Outlined.NotificationsNone, tint = TextPrimary) { showMenuSheet = false }
                }

                RoundedSheetGroup {
                    SheetOptionItem(
                        label = "Snooze $displayName for 30 days",
                        subtitle = "Temporarily stop seeing posts.",
                        icon = Icons.Outlined.NotificationsNone,
                        tint = TextPrimary,
                    ) { showMenuSheet = false }
                    SheetOptionItem(
                        label = "Hide all from $displayName",
                        subtitle = "Stop seeing posts from this person.",
                        icon = Icons.Outlined.BookmarkBorder,
                        tint = TextPrimary,
                    ) { showMenuSheet = false }
                }

                RoundedSheetGroup {
                    SheetOptionItem("Add to album", icon = Icons.Outlined.AddCircle, tint = TextPrimary) { showMenuSheet = false }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = shareSheetState,
            containerColor = Card,
            scrimColor = Color.Black.copy(alpha = 0.55f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Share",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                RoundedSheetGroup {
                    SheetOptionItem(
                        label = "Share now",
                        subtitle = "Post this to your feed",
                        icon = Icons.AutoMirrored.Outlined.Send,
                        tint = TextPrimary,
                    ) {
                        viewModel.shareActivity(activity)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out ${displayName}'s recent activity on KAMPUS: https://kampus.app/profile/$profileUserId",
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share activity"))
                        showShareSheet = false
                    }
                    SheetOptionItem(
                        label = "Send message",
                        subtitle = "Share in chat",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        tint = TextPrimary,
                    ) {
                        showShareSheet = false
                    }
                    SheetOptionItem(
                        label = "Copy link",
                        subtitle = "Copy the profile link",
                        icon = Icons.Outlined.ContentCopy,
                        tint = TextPrimary,
                    ) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "Profile link",
                                "https://kampus.app/profile/$profileUserId",
                            ),
                        )
                        showShareSheet = false
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RoundedSheetGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (PubIsDark) Color(0xFF2A2D34) else Card)
            .border(1.dp, Border.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun SheetOptionItem(
    label: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ActivityPreviewPanel(
    accent: Color,
    previewTitle: String,
    previewSubtitle: String,
    previewImageUrl: String,
    activity: ProfileActivityItem,
    onClick: () -> Unit,
) {
    if (previewImageUrl.isNotBlank()) {
        AsyncImage(
            model = previewImageUrl,
            contentDescription = previewTitle,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop,
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF111827),
                            accent.copy(alpha = 0.18f),
                            Color(0xFF020617),
                        ),
                    ),
                )
                .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when (activity.type) {
                        "create_event" -> Icons.Outlined.CalendarMonth
                        "create_group" -> Icons.Outlined.Stars
                        "create_post" -> Icons.Outlined.ModeEdit
                        else -> Icons.Outlined.Stars
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = previewTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = previewSubtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }

            Text(
                text = when (activity.type) {
                    "create_event" -> "Open"
                    "create_group" -> "View"
                    "create_post" -> "Post"
                    else -> "Open"
                },
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ActivityAction(
    text: String,
    count: Int = 0,
    icon: ImageVector? = null,
    accent: Color = TextSecondary,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Text(
            text = if (count > 0) count.toString() else text,
            color = if (count > 0) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (count > 0) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

private fun formatActivityTime(timestamp: Long): String {
    if (timestamp <= 0L) return "just now"
    val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}

