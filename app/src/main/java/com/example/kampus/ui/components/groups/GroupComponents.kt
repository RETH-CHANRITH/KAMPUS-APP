package com.example.kampus.ui.components.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kampus.data.model.Group
import com.example.kampus.data.model.GroupMember
import com.example.kampus.data.model.GroupPost
import com.example.kampus.data.model.GroupPrivacy
import com.example.kampus.data.model.GroupPrivacy.PRIVATE
import com.example.kampus.data.model.GroupPrivacy.PUBLIC
import com.example.kampus.data.model.MemberRole
import com.example.kampus.data.model.MembershipStatus
import com.example.kampus.data.model.PostReportReason
import com.example.kampus.ui.theme.KampusColors as C
import com.example.kampus.ui.theme.KampusType as T
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun PrivacyBadge(privacy: GroupPrivacy) {
    val isPublic = privacy == PUBLIC
    val bg = if (isPublic) C.BadgePublicBackground else C.BadgePrivateBackground
    val fg = if (isPublic) C.BadgePublicText else C.BadgePrivateText
    val icon = if (isPublic) Icons.Filled.Public else Icons.Outlined.Lock

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
        Text(text = if (isPublic) "Public" else "Private", color = fg, style = T.Caption)
    }
}

@Composable
fun GroupCard(
    group: Group,
    membershipStatus: MembershipStatus,
    currentUserRole: MemberRole,
    onJoinClick: () -> Unit,
    onCardClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = C.Surface),
        border = BorderStroke(1.dp, C.Border),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(152.dp)) {
                if (group.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = group.coverImageUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.matchParentSize().background(
                            Brush.linearGradient(
                                listOf(C.SurfaceElevated, C.Primary.copy(alpha = 0.6f), C.Surface)
                            )
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Message, contentDescription = null, tint = C.TextHint.copy(alpha = 0.25f), modifier = Modifier.size(44.dp))
                    }
                }
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(group.name, color = C.TextPrimary, style = T.HeadingMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PrivacyBadge(group.privacy)
                }
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(group.category, color = C.TextMuted, style = T.Caption)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(label = formatCount(group.memberCount), icon = { Icon(Icons.Filled.Public, null, tint = C.TextMuted, modifier = Modifier.size(13.dp)) })
                    StatChip(label = formatCount(group.postCount), icon = { Icon(Icons.Outlined.Message, null, tint = C.TextMuted, modifier = Modifier.size(13.dp)) })
                }

                GroupActionButton(
                    membershipStatus = membershipStatus,
                    currentUserRole = currentUserRole,
                    privacy = group.privacy,
                    onJoinClick = onJoinClick,
                )
            }
        }
    }
}

@Composable
fun GroupActionButton(
    membershipStatus: MembershipStatus,
    currentUserRole: MemberRole,
    privacy: GroupPrivacy,
    onJoinClick: () -> Unit,
) {
    val buttonColor by animateColorAsState(
        targetValue = when (membershipStatus) {
            MembershipStatus.NONE -> C.Primary
            MembershipStatus.PENDING -> C.Warning
            MembershipStatus.MEMBER -> C.SurfaceElevated
        },
        animationSpec = tween(220),
        label = "group_action_color",
    )

    when (membershipStatus) {
        MembershipStatus.NONE -> {
            Button(
                onClick = onJoinClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (privacy == PUBLIC) "Join Group" else "Request to Join", style = T.LabelMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        MembershipStatus.PENDING -> {
            OutlinedButton(
                onClick = onJoinClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = C.Warning),
                border = BorderStroke(1.dp, C.Warning.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Filled.HourglassTop, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Requested", style = T.LabelMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        MembershipStatus.MEMBER -> {
            OutlinedButton(
                onClick = onJoinClick,
                enabled = false,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = C.TextSecondary),
                border = BorderStroke(1.dp, C.Border),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (currentUserRole == MemberRole.ADMIN) "Admin ✓" else "Joined", style = T.LabelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GroupPostItem(
    post: GroupPost,
    isAdmin: Boolean,
    onLikeClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveAuthorClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val likeTint by animateColorAsState(if (post.isLikedByCurrentUser) C.Error else C.TextMuted, tween(180), label = "post_like")

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = C.Surface),
        border = BorderStroke(1.dp, C.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UserAvatar(post.authorAvatarUrl, post.authorName, 40.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(post.authorName, color = C.TextPrimary, style = T.HeadingSmall)
                        Text(timeAgo(post.createdAt), color = C.TextMuted, style = T.Caption)
                        AnimatedVisibility(post.isReported || post.reportCount > 0) {
                            Badge(containerColor = C.Error.copy(alpha = 0.16f), contentColor = C.Error) {
                                Text("Reported")
                            }
                        }
                    }
                    Text(post.content, color = C.TextSecondary, style = T.BodyMedium)
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = C.TextMuted)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (isAdmin) {
                            DropdownMenuItem(text = { Text("Delete post") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { expanded = false; onDeleteClick() })
                            DropdownMenuItem(text = { Text("Remove from group") }, leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) }, onClick = { expanded = false; onRemoveAuthorClick() })
                        } else {
                            DropdownMenuItem(text = { Text("Report post") }, leadingIcon = { Icon(Icons.Outlined.Flag, null) }, onClick = { expanded = false; onReportClick() })
                        }
                    }
                }
            }

            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(10.dp)),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                AssistChip(onClick = onLikeClick, label = { Text(formatCount(post.likeCount)) }, leadingIcon = { Icon(if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = likeTint) })
                AssistChip(onClick = {}, label = { Text(formatCount(post.commentCount)) }, leadingIcon = { Icon(Icons.Outlined.Message, null, tint = C.TextMuted) })
            }
        }
    }
}

@Composable
fun UserAvatar(imageUrl: String?, name: String, size: Dp) {
    val initials = remember(name) { name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "K" } }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(C.SurfaceElevated).border(1.dp, C.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.matchParentSize())
        } else {
            Text(initials, color = C.TextPrimary, style = T.LabelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun KampusSearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = C.TextHint) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = C.TextMuted) },
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = C.Surface,
            unfocusedContainerColor = C.Surface,
            disabledContainerColor = C.Surface,
            focusedTextColor = C.TextPrimary,
            unfocusedTextColor = C.TextPrimary,
            focusedIndicatorColor = C.Primary,
            unfocusedIndicatorColor = C.Border,
            cursorColor = C.Primary,
        ),
    )
}

@Composable
fun ReportPostDialog(
    onDismiss: () -> Unit,
    onReport: (PostReportReason, String) -> Unit,
) {
    var selectedReason by remember { mutableStateOf(PostReportReason.SPAM) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text("Report post", color = C.TextPrimary, style = T.HeadingSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PostReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedReason = reason }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                        Text(reason.label, color = C.TextPrimary, style = T.BodyMedium)
                    }
                }
                if (selectedReason == PostReportReason.OTHER) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Add a short note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = C.SurfaceElevated,
                            unfocusedContainerColor = C.SurfaceElevated,
                            focusedTextColor = C.TextPrimary,
                            unfocusedTextColor = C.TextPrimary,
                            focusedIndicatorColor = C.Primary,
                            unfocusedIndicatorColor = C.Border,
                            cursorColor = C.Primary,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(selectedReason, note) },
                colors = ButtonDefaults.buttonColors(containerColor = C.Error, contentColor = Color.White),
            ) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.TextSecondary) } },
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    body: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text(title, color = C.TextPrimary, style = T.HeadingSmall) },
        text = { Text(body, color = C.TextSecondary, style = T.BodyMedium) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = Color.White)) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.TextSecondary) } },
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    body: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmActionDialog(
        title = title,
        body = body,
        confirmText = confirmText,
        confirmColor = confirmColor,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun StatChip(label: String, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon()
        Text(label, color = C.TextSecondary, style = T.Caption)
    }
}

fun formatCount(value: Int): String {
    return when {
        value >= 1_000_000 -> "${(value / 1_000_000.0).formatOneDecimal()}M"
        value >= 1_000 -> "${(value / 1_000.0).formatOneDecimal()}K"
        else -> value.toString()
    }
}

fun timeAgo(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        diff < 60_000L -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${(days / 7)}w ago"
    }
}

private fun Double.formatOneDecimal(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}