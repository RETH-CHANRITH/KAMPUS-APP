package com.example.kampus.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kampus.data.model.GroupMember
import com.example.kampus.data.model.GroupPost
import com.example.kampus.data.model.JoinRequest
import com.example.kampus.data.model.MemberRole
import com.example.kampus.ui.components.groups.ConfirmActionDialog
import com.example.kampus.ui.components.groups.UserAvatar
import com.example.kampus.ui.components.groups.formatCount
import com.example.kampus.ui.components.groups.timeAgo
import com.example.kampus.ui.theme.KampusColors as C
import com.example.kampus.ui.theme.KampusType as T
import com.example.kampus.viewmodel.GroupsViewModel

@Composable
fun AdminPanelScreen(
    groupId: String,
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.adminPanelUiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmBody by remember { mutableStateOf("") }
    var confirmButtonText by remember { mutableStateOf("") }
    var confirmButtonColor by remember { mutableStateOf(C.Primary) }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(groupId) { viewModel.loadAdminPanel(groupId) }
    LaunchedEffect(state.actionSuccess) {
        state.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeAdminActionSuccess()
        }
    }

    Scaffold(
        containerColor = C.Background,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = C.TextPrimary) }
                Icon(Icons.Filled.Shield, null, tint = C.Primary)
                Text("Admin Panel", color = C.TextPrimary, style = T.HeadingLarge)
            }

            TabRow(
                selectedTab = selectedTab,
                requestsCount = state.pendingRequests.size,
                reportedCount = state.reportedPosts.size,
                onSelect = { selectedTab = it },
            )

            when (selectedTab) {
                0 -> RequestsTab(
                    requests = state.pendingRequests,
                    onApprove = { request ->
                        confirmTitle = "Approve request"
                        confirmBody = "Allow ${request.userName} to join this group?"
                        confirmButtonText = "Approve"
                        confirmButtonColor = C.Success
                        confirmAction = { viewModel.approveJoinRequest(groupId, request) }
                    },
                    onReject = { request ->
                        confirmTitle = "Reject request"
                        confirmBody = "Remove the pending join request from ${request.userName}?"
                        confirmButtonText = "Reject"
                        confirmButtonColor = C.Error
                        confirmAction = { viewModel.rejectJoinRequest(groupId, request) }
                    },
                )

                1 -> ReportedTab(
                    reportedPosts = state.reportedPosts,
                    onRemoveUser = { post ->
                        confirmTitle = "Remove user"
                        confirmBody = "Remove ${post.authorName} from this group?"
                        confirmButtonText = "Remove"
                        confirmButtonColor = C.Warning
                        confirmAction = { viewModel.removeMemberFromGroup(groupId, post.authorId) }
                    },
                    onDeletePost = { post ->
                        confirmTitle = "Delete post"
                        confirmBody = "Delete this reported post from the group?"
                        confirmButtonText = "Delete"
                        confirmButtonColor = C.Error
                        confirmAction = { viewModel.deletePost(groupId, post.id) }
                    },
                )

                2 -> MembersTab(
                    members = state.members,
                    onRemove = { member ->
                        confirmTitle = "Remove member"
                        confirmBody = "Remove ${member.userName} from this group?"
                        confirmButtonText = "Remove"
                        confirmButtonColor = C.Warning
                        confirmAction = { viewModel.removeMemberFromGroup(groupId, member.userId) }
                    },
                )
            }
        }

        if (confirmAction != null) {
            ConfirmActionDialog(
                title = confirmTitle,
                body = confirmBody,
                confirmText = confirmButtonText,
                confirmColor = confirmButtonColor,
                onDismiss = { confirmAction = null },
                onConfirm = {
                    confirmAction?.invoke()
                    confirmAction = null
                },
            )
        }
    }
}

@Composable
private fun TabRow(
    selectedTab: Int,
    requestsCount: Int,
    reportedCount: Int,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AdminTab("Requests", selectedTab == 0, count = requestsCount, badgeColor = C.Warning, modifier = Modifier.weight(1f), onClick = { onSelect(0) })
        AdminTab("Reported", selectedTab == 1, count = reportedCount, badgeColor = C.Error, modifier = Modifier.weight(1f), onClick = { onSelect(1) })
        AdminTab("Members", selectedTab == 2, modifier = Modifier.weight(1f), onClick = { onSelect(2) })
    }
}

@Composable
private fun AdminTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    count: Int = 0,
    badgeColor: Color? = null,
    onClick: () -> Unit,
) {
    val bg = if (selected) C.Primary else C.Surface
    val fg = if (selected) Color.White else C.TextSecondary
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, C.Border, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = fg, style = T.LabelMedium, fontWeight = FontWeight.SemiBold)
            if (count > 0) {
                Badge(containerColor = (badgeColor ?: C.Primary).copy(alpha = 0.16f), contentColor = badgeColor ?: C.Primary) { Text(count.toString()) }
            }
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<JoinRequest>,
    onApprove: (JoinRequest) -> Unit,
    onReject: (JoinRequest) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        items(requests, key = { it.id }) { request ->
            RequestCard(request = request, onApprove = { onApprove(request) }, onReject = { onReject(request) })
        }
    }
}

@Composable
private fun RequestCard(request: JoinRequest, onApprove: () -> Unit, onReject: () -> Unit) {
    Surface(color = C.Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, C.Border)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAvatar(request.userAvatarUrl, request.userName, 44.dp, userId = request.userId)
            Column(modifier = Modifier.weight(1f)) {
                Text(request.userName, color = C.TextPrimary, style = T.HeadingSmall)
                Text("Requested ${timeAgo(request.requestedAt)}", color = C.TextMuted, style = T.BodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onReject) { Icon(Icons.Filled.Close, null, tint = C.Error) }
                IconButton(onClick = onApprove) { Icon(Icons.Filled.Check, null, tint = C.Success) }
            }
        }
    }
}

@Composable
private fun ReportedTab(
    reportedPosts: List<GroupPost>,
    onRemoveUser: (GroupPost) -> Unit,
    onDeletePost: (GroupPost) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item {
            Surface(color = C.Warning.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, C.Warning.copy(alpha = 0.32f))) {
                Text("${reportedPosts.size} flagged posts need attention.", color = C.Warning, style = T.BodyMedium, modifier = Modifier.padding(14.dp))
            }
        }
        items(reportedPosts, key = { it.id }) { post ->
            ReportedPostCard(post = post, onRemoveUser = { onRemoveUser(post) }, onDeletePost = { onDeletePost(post) })
        }
    }
}

@Composable
private fun ReportedPostCard(post: GroupPost, onRemoveUser: () -> Unit, onDeletePost: () -> Unit) {
    Surface(color = C.Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, C.Border)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserAvatar(post.authorAvatarUrl, post.authorName, 40.dp, userId = post.authorId)
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, color = C.TextPrimary, style = T.HeadingSmall)
                    Text("${post.reportCount} reports", color = C.Error, style = T.Caption)
                }
            }
            Text(post.content, color = C.TextSecondary, style = T.BodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRemoveUser, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, C.Warning)) { Text("Remove User", color = C.Warning) }
                Button(onClick = onDeletePost, colors = ButtonDefaults.buttonColors(containerColor = C.Error, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) { Text("Delete Post") }
            }
        }
    }
}

@Composable
private fun MembersTab(members: List<GroupMember>, onRemove: (GroupMember) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        items(members, key = { it.id }) { member ->
            MemberRow(member = member, onRemove = { onRemove(member) })
        }
    }
}

@Composable
private fun MemberRow(member: GroupMember, onRemove: () -> Unit) {
    Surface(color = C.Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, C.Border)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAvatar(member.userAvatarUrl, member.userName, 44.dp, userId = member.userId)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(member.userName, color = C.TextPrimary, style = T.HeadingSmall)
                    if (member.role == MemberRole.ADMIN) {
                        Badge(containerColor = C.Primary.copy(alpha = 0.16f), contentColor = C.Primary) { Text("Admin") }
                    }
                }
                Text("Joined ${timeAgo(member.joinedAt)}", color = C.TextMuted, style = T.BodySmall)
            }
            if (member.role != MemberRole.ADMIN) {
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, null, tint = C.Error) }
            }
        }
    }
}