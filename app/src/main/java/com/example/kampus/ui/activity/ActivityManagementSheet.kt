@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.activity

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kampus.ui.theme.ThemeController
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Color Palette - Professional Dark Theme
// ─────────────────────────────────────────────────────────────────────────────
private val BgDeep = Color(0xFF0B1020)
private val BgCard = Color(0xFF1B2138)
private val BgCardAlt = Color(0xFF252E48)
private val AccentBlue = Color(0xFF248BFF)
private val AccentBlueLight = Color(0xFF3D9EFF)
private val AccentGlow = AccentBlue.copy(alpha = 0.3f)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFB0B8C8)
private val TextGrayDark = Color(0xFF7A8397)
private val BorderColor = Color(0xFF2A3550)
private val SuccessGreen = Color(0xFF10B981)
private val ErrorRed = Color(0xFFEF4444)
private val WarningOrange = Color(0xFFF59E0B)

/**
 * Activity Management Sheet - Opens from 3-dot menu on Recent Activity card
 * Features: Pin, Edit, Privacy, Delete, Archive, Share, Reactions, Comments, Analytics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityManagementSheet(
    activityId: String,
    onDismiss: () -> Unit,
    viewModel: ActivityManagementViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity by viewModel.selectedActivity.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val reactions by viewModel.reactions.collectAsStateWithLifecycle()
    
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showPrivacyMenu by rememberSaveable { mutableStateOf(false) }
    var isComposingComment by rememberSaveable { mutableStateOf(false) }
    var commentText by rememberSaveable { mutableStateOf("") }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activityId) {
        viewModel.selectActivity(activityId)
    }

    // Extract values from delegated properties to avoid smart cast issues
    val activityValue = activity
    val commentsValue = comments
    val reactionsValue = reactions
    val uiStateValue = uiState

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgDeep,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        activityValue?.let { currentActivity ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(BgDeep)
            ) {
                // ─────────────────────────────────────────────────────────────
                // Header with Title and Close
                // ─────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activity Manager",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        coroutineScope.launch { sheetState.hide() }
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Divider(color = BorderColor, thickness = 1.dp)

                // ─────────────────────────────────────────────────────────────
                // Activity Preview Card
                // ─────────────────────────────────────────────────────────────
                ActivityPreviewCard(currentActivity)

                Divider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // ─────────────────────────────────────────────────────────────
                // Tab Navigation
                // ─────────────────────────────────────────────────────────────
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    containerColor = BgDeep,
                    contentColor = AccentBlue,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "⚙️ Manage",
                                color = if (selectedTab == 0) AccentBlue else TextGray
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "❤️ Reactions (${reactionsValue.values.sum()})",
                                color = if (selectedTab == 1) AccentBlue else TextGray
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "💬 Comments (${commentsValue.size})",
                                color = if (selectedTab == 2) AccentBlue else TextGray
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Text(
                                "📊 Analytics",
                                color = if (selectedTab == 3) AccentBlue else TextGray
                            )
                        }
                    )
                }

                // ─────────────────────────────────────────────────────────────
                // Tab Content
                // ─────────────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> ManagementTab(
                            activity = currentActivity,
                            onPin = {
                                viewModel.togglePin(activityId)
                            },
                            onEdit = {
                                viewModel.startEditing(activityId)
                            },
                            onPrivacy = {
                                showPrivacyMenu = true
                            },
                            onArchive = {
                                viewModel.archiveActivity(activityId)
                                coroutineScope.launch { sheetState.hide() }
                                onDismiss()
                            },
                            onDelete = {
                                showDeleteConfirm = true
                            },
                            viewModel = viewModel
                        )
                        1 -> ReactionsTab(
                            activity = currentActivity,
                            reactions = reactionsValue,
                            onToggleLike = {
                                viewModel.toggleLike(activityId)
                            }
                        )
                        2 -> CommentsTab(
                            comments = commentsValue,
                            isComposingComment = isComposingComment,
                            commentText = commentText,
                            onCommentChange = { commentText = it },
                            onToggleCompose = { isComposingComment = !isComposingComment },
                            onAddComment = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(
                                        activityId,
                                        commentText,
                                        "", // userId - should come from FirebaseAuth
                                        "Current User", // username
                                        "👤" // userAvatar
                                    )
                                    commentText = ""
                                    isComposingComment = false
                                }
                            },
                            onDeleteComment = { commentId ->
                                viewModel.deleteComment(activityId, commentId)
                            }
                        )
                        3 -> AnalyticsTab(activity = currentActivity)
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // Delete Confirmation Dialog
            // ─────────────────────────────────────────────────────────────
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text(
                            "Delete Activity?",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "This action cannot be undone. Your activity will be permanently deleted.",
                            color = TextGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteActivity(activityId)
                                showDeleteConfirm = false
                                coroutineScope.launch { sheetState.hide() }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Delete", color = TextWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextGray)
                        ) {
                            Text("Cancel")
                        }
                    },
                    containerColor = BgCard,
                    tonalElevation = 0.dp
                )
            }

            // ─────────────────────────────────────────────────────────────
            // Privacy Menu
            // ─────────────────────────────────────────────────────────────
            if (showPrivacyMenu) {
                PrivacyMenu(
                    currentPrivacy = currentActivity.privacy,
                    onPrivacySelected = { newPrivacy ->
                        viewModel.updatePrivacy(activityId, newPrivacy)
                        showPrivacyMenu = false
                    },
                    onDismiss = { showPrivacyMenu = false }
                )
            }
        }
    }
}

/**
 * Activity Preview Card - Shows activity summary
 */
@Composable
private fun ActivityPreviewCard(activity: ActivityItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard, RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    activity.userAvatar,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BgCardAlt)
                        .padding(8.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        activity.username,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Posted ${formatTime(activity.createdAt)}",
                        color = TextGrayDark,
                        fontSize = 12.sp
                    )
                }
            }
            if (activity.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        Text(
            activity.title.ifEmpty { activity.description },
            color = TextWhite,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(icon = "❤️", count = activity.likes, label = "Likes")
            StatItem(icon = "💬", count = activity.comments, label = "Comments")
            StatItem(icon = "📤", count = activity.shares, label = "Shares")
            StatItem(icon = "👁️", count = activity.views, label = "Views")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy Badge
        Row(modifier = Modifier.fillMaxWidth()) {
            Surface(
                color = AccentGlow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    "🔒 ${activity.privacy.name}",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp, 4.dp)
                )
            }
            
            if (activity.isArchived) {
                Surface(
                    color = WarningOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        "📦 Archived",
                        color = WarningOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Stat Item - Shows count with icon
 */
@Composable
private fun StatItem(icon: String, count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(count.toString(), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = TextGrayDark, fontSize = 10.sp)
    }
}

/**
 * Management Tab - Pin, Edit, Privacy, Archive, Delete
 */
@Composable
private fun ManagementTab(
    activity: ActivityItem,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onPrivacy: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    viewModel: ActivityManagementViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pin Action
        item {
            ManagementActionButton(
                icon = if (activity.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                label = if (activity.isPinned) "Unpin Activity" else "Pin Activity",
                description = if (activity.isPinned) "Remove from top" else "Keep at the top",
                onClick = onPin,
                color = AccentBlue
            )
        }

        // Edit Action
        item {
            ManagementActionButton(
                icon = Icons.Default.Edit,
                label = "Edit Activity",
                description = "Update content, images, and tags",
                onClick = onEdit,
                color = AccentBlueLight
            )
        }

        // Privacy Action
        item {
            ManagementActionButton(
                icon = Icons.Default.Lock,
                label = "Privacy Settings",
                description = activity.privacy.name,
                onClick = onPrivacy,
                color = SuccessGreen
            )
        }

        // Hide from Profile
        item {
            ManagementActionButton(
                icon = Icons.Default.VisibilityOff,
                label = "Hide from Profile",
                description = if (activity.hiddenFromProfile) "Currently hidden" else "Remove from profile view",
                onClick = {
                    viewModel.hideFromProfile(activity.id, !activity.hiddenFromProfile)
                },
                color = TextGrayDark
            )
        }

        // Archive Action
        item {
            ManagementActionButton(
                icon = Icons.Default.Archive,
                label = "Archive Activity",
                description = "Move to archive (can restore later)",
                onClick = onArchive,
                color = WarningOrange
            )
        }

        // Delete Action (Red)
        item {
            ManagementActionButton(
                icon = Icons.Default.Delete,
                label = "Delete Activity",
                description = "Permanently remove (cannot undo)",
                onClick = onDelete,
                color = ErrorRed,
                isDestructive = true
            )
        }

        // Featured Toggle
        item {
            ManagementActionButton(
                icon = if (activity.isFeatured) Icons.Default.Star else Icons.Outlined.StarOutline,
                label = if (activity.isFeatured) "Remove from Featured" else "Feature Activity",
                description = "Show in featured section",
                onClick = {
                    viewModel.updatePrivacy(activity.id, activity.privacy)
                },
                color = Color(0xFFFFD700)
            )
        }

        // Share Action
        item {
            ManagementActionButton(
                icon = Icons.Default.Share,
                label = "Share Activity",
                description = "Share to other users",
                onClick = {
                    viewModel.shareActivity(activity.id)
                },
                color = AccentBlue
            )
        }
    }
}

/**
 * Management Action Button with icon and description
 */
@Composable
private fun ManagementActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    color: Color,
    isDestructive: Boolean = false
) {
    var isPressed by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
            .scale(if (isPressed) 0.98f else 1f),
        color = if (isDestructive) BgCardAlt else BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDestructive) color.copy(alpha = 0.5f) else BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(label, color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(description, color = TextGrayDark, fontSize = 12.sp)
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Action",
                tint = TextGrayDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            isPressed = false
        }
    }
}

/**
 * Reactions Tab
 */
@Composable
private fun ReactionsTab(
    activity: ActivityItem,
    reactions: Map<String, Int>,
    onToggleLike: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Reactions Overview",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            // Reaction Stats Grid
            Column(modifier = Modifier.fillMaxWidth()) {
                listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                    val count = reactions[emoji] ?: 0
                    if (count > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(BgCard, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 24.sp)
                            Text("$count people", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Divider(color = BorderColor)
        }

        item {
            // Quick Reaction Buttons
            Text(
                "Add Reaction",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { onToggleLike() },
                        color = BgCard,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Comments Tab
 */
@Composable
private fun CommentsTab(
    comments: List<ActivityComment>,
    isComposingComment: Boolean,
    commentText: String,
    onCommentChange: (String) -> Unit,
    onToggleCompose: () -> Unit,
    onAddComment: () -> Unit,
    onDeleteComment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Divider(color = BorderColor)
        }

        // Comment Input Field
        item {
            if (isComposingComment) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .padding(12.dp)
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = onCommentChange,
                        placeholder = { Text("Write a comment...", color = TextGrayDark) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgCardAlt, RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BgCardAlt,
                            unfocusedContainerColor = BgCardAlt,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = AccentBlue,
                            focusedIndicatorColor = AccentBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onToggleCompose) {
                            Text("Cancel", color = TextGrayDark)
                        }
                        Button(
                            onClick = onAddComment,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Post", color = TextWhite)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onToggleCompose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Add Comment", color = TextWhite)
                }
            }
        }

        // Comments List
        items(comments) { comment ->
            CommentItem(
                comment = comment,
                onDelete = { onDeleteComment(comment.id) }
            )
        }

        if (comments.isEmpty()) {
            item {
                Text(
                    "No comments yet. Be the first!",
                    color = TextGrayDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Comment Item
 */
@Composable
private fun CommentItem(
    comment: ActivityComment,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Text(comment.userAvatar, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                Column {
                    Text(comment.username, color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(formatTime(comment.createdAt), color = TextGrayDark, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextGrayDark, modifier = Modifier.size(16.dp))
            }
        }
        Text(comment.text, color = TextWhite, modifier = Modifier.padding(top = 8.dp))
    }
}

/**
 * Analytics Tab
 */
@Composable
private fun AnalyticsTab(activity: ActivityItem) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Performance Metrics",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            AnalyticsCard(
                icon = "👁️",
                label = "Views",
                value = activity.views.toString(),
                trend = "+12%"
            )
        }

        item {
            AnalyticsCard(
                icon = "❤️",
                label = "Likes",
                value = activity.likes.toString(),
                trend = "+5%"
            )
        }

        item {
            AnalyticsCard(
                icon = "💬",
                label = "Comments",
                value = activity.comments.toString(),
                trend = "+3%"
            )
        }

        item {
            AnalyticsCard(
                icon = "📤",
                label = "Shares",
                value = activity.shares.toString(),
                trend = "New"
            )
        }

        item {
            AnalyticsCard(
                icon = "📊",
                label = "Reach",
                value = activity.reach.toString(),
                trend = "Growing"
            )
        }

        item {
            Divider(color = BorderColor)
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(16.dp)
            ) {
                Text("Engagement Rate", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = 0.65f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentBlue,
                    trackColor = BgCardAlt
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("65% of followers engaged", color = TextGrayDark, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Analytics Card
 */
@Composable
private fun AnalyticsCard(
    icon: String,
    label: String,
    value: String,
    trend: String
) {
    Surface(
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(label, color = TextGrayDark, fontSize = 12.sp)
                    Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Surface(
                color = SuccessGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    trend,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp, 4.dp)
                )
            }
        }
    }
}

/**
 * Privacy Menu Dialog
 */
@Composable
private fun PrivacyMenu(
    currentPrivacy: ActivityItem.ActivityPrivacy,
    onPrivacySelected: (ActivityItem.ActivityPrivacy) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Privacy Level",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(ActivityItem.ActivityPrivacy.values()) { privacy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPrivacySelected(privacy) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = privacy == currentPrivacy,
                            onClick = { onPrivacySelected(privacy) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentBlue,
                                unselectedColor = TextGrayDark
                            )
                        )
                        Text(
                            privacy.name,
                            color = TextWhite,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AccentBlue)
            }
        },
        containerColor = BgCard,
        tonalElevation = 0.dp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper Functions
// ─────────────────────────────────────────────────────────────────────────────

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
