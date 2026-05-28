package com.example.kampus.ui.screens.groups

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kampus.data.model.GroupPost
import com.example.kampus.data.model.MembershipStatus
import com.example.kampus.data.model.PostReportReason
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.components.groups.ConfirmationDialog
import com.example.kampus.ui.components.groups.GroupActionButton
import com.example.kampus.ui.components.groups.GroupPostItem
import com.example.kampus.ui.components.groups.KampusSearchBar
import com.example.kampus.ui.components.groups.PrivacyBadge
import com.example.kampus.ui.components.groups.ReportPostDialog
import com.example.kampus.ui.components.groups.UserAvatar
import com.example.kampus.ui.components.groups.formatCount
import com.example.kampus.ui.components.groups.timeAgo
import com.example.kampus.ui.theme.KampusColors as C
import com.example.kampus.ui.theme.KampusType as T
import com.example.kampus.viewmodel.GroupsViewModel

@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onOpenAdminPanel: (String) -> Unit,
    onHomeClick: () -> Unit = {},
    onEventsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCreatePost: () -> Unit = {},
) {
    val state by viewModel.groupDetailUiState.collectAsStateWithLifecycle()
    var postText by remember { mutableStateOf("") }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<GroupPost?>(null) }
    var showImageStubNotice by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetail(groupId)
        viewModel.loadAdminPanel(groupId)
    }

    LaunchedEffect(state.postSubmitted) {
        if (state.postSubmitted) postText = ""
    }

    val group = state.group

    Scaffold(
        containerColor = C.Background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, C.Background.copy(alpha = 0.98f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            ) {
                CampusBottomNavBar(
                    selectedIndex = 1, // Groups tab
                    onItemSelected = { index ->
                        when (index) {
                            0 -> onHomeClick()
                            1 -> { /* already on groups */ }
                            2 -> onEventsClick()
                            3 -> onChatClick()
                        }
                    },
                    onFabClick = onCreatePost,
                    onProfileClick = onProfileClick,
                    isProfileSelected = false,
                )
            }
        },
    ) { padding ->
        if (group == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Group not found", color = C.TextSecondary, style = T.HeadingSmall)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    if (group.coverImageUrl.isNotBlank()) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(group.coverImageUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(C.SurfaceElevated, C.Primary.copy(alpha = 0.6f), C.Background))))
                    }

                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, C.Background.copy(alpha = 0.85f)))
                        )
                    )

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp).align(Alignment.TopStart).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)),
                    ) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }

                    if (state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN) {
                        IconButton(
                            onClick = { onOpenAdminPanel(group.id) },
                            modifier = Modifier.statusBarsPadding().padding(end = 16.dp, top = 12.dp).align(Alignment.TopEnd).clip(CircleShape).background(C.Primary),
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, null, tint = Color.White)
                        }
                    }

                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.name, color = C.TextPrimary, style = T.HeadingLarge)
                        PrivacyBadge(group.privacy)
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip(text = formatCount(group.memberCount) + " members")
                        StatChip(text = formatCount(group.postCount) + " posts")
                        StatChip(text = group.category)
                    }

                    Text(group.description, color = C.TextSecondary, style = T.BodyMedium)

                    GroupActionButton(
                        membershipStatus = state.membershipStatus,
                        currentUserRole = state.currentUserRole,
                        privacy = group.privacy,
                        onJoinClick = {
                            when (state.membershipStatus) {
                                MembershipStatus.PENDING -> viewModel.cancelJoinRequest(group.id)
                                MembershipStatus.MEMBER -> { }
                                MembershipStatus.NONE -> viewModel.joinOrRequestGroup(group.id)
                            }
                        },
                    )

                    if (state.membershipStatus == MembershipStatus.MEMBER && state.currentUserRole != com.example.kampus.data.model.MemberRole.ADMIN) {
                        TextButton(onClick = { showLeaveDialog = true }) { Text("Leave Group", color = C.Error) }
                    }

                    if (state.membershipStatus == MembershipStatus.PENDING) {
                        PendingApprovalBanner()
                    }
                }
            }

            if (state.membershipStatus == MembershipStatus.MEMBER) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            minLines = 3,
                            placeholder = { Text("Share something with the group") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = C.Surface,
                                unfocusedContainerColor = C.Surface,
                                focusedTextColor = C.TextPrimary,
                                unfocusedTextColor = C.TextPrimary,
                                focusedIndicatorColor = C.Primary,
                                unfocusedIndicatorColor = C.Border,
                                cursorColor = C.Primary,
                            ),
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showImageStubNotice = true }) { Icon(Icons.Filled.AddPhotoAlternate, null, tint = C.TextSecondary) }
                            Button(
                                onClick = { viewModel.createPost(group.id, postText, null) },
                                enabled = postText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = C.Primary, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Post") }
                        }
                    }
                }
            }

            item {
                val reportedCount = state.posts.count { it.isReported || it.reportCount > 0 }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent Posts", color = C.TextPrimary, style = T.HeadingSmall)
                    if (state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN && reportedCount > 0) {
                        Badge(containerColor = C.Error.copy(alpha = 0.16f), contentColor = C.Error) { Text("$reportedCount reported") }
                    }
                }
            }

            if (state.posts.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No posts yet", color = C.TextSecondary, style = T.HeadingSmall)
                        Text("Be the first to share something with this group.", color = C.TextMuted, style = T.BodyMedium)
                    }
                }
            } else {
                items(state.posts, key = { it.id }) { post ->
                    GroupPostItem(
                        post = post,
                        isAdmin = state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN,
                        onLikeClick = { viewModel.toggleLikePost(group.id, post.id) },
                        onReportClick = { reportTarget = post },
                        onDeleteClick = { viewModel.deletePost(group.id, post.id) },
                        onRemoveAuthorClick = { viewModel.removeMemberFromGroup(group.id, post.authorId) },
                    )
                }
            }
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                containerColor = C.Surface,
                title = { Text("Leave group", color = C.TextPrimary) },
                text = { Text("Are you sure you want to leave this group?", color = C.TextSecondary) },
                confirmButton = {
                    Button(onClick = { viewModel.leaveGroup(group.id); showLeaveDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = C.Error, contentColor = Color.White)) { Text("Leave") }
                },
                dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel", color = C.TextSecondary) } },
            )
        }

        reportTarget?.let { post ->
            ReportPostDialog(
                onDismiss = { reportTarget = null },
                onReport = { reason, note ->
                    viewModel.reportPost(group.id, post.id, reason, note)
                    reportTarget = null
                },
            )
        }

        if (showImageStubNotice) {
            AlertDialog(
                onDismissRequest = { showImageStubNotice = false },
                containerColor = C.Surface,
                title = { Text("Image attachment", color = C.TextPrimary) },
                text = { Text("Image picking is stubbed for now. The post can still be published without an image.", color = C.TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { showImageStubNotice = false }) { Text("OK", color = C.Primary) }
                },
            )
        }
    }
}

@Composable
private fun PendingApprovalBanner() {
    Surface(
        color = C.Warning.copy(alpha = 0.15f),
        contentColor = C.Warning,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, C.Warning.copy(alpha = 0.35f)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.HourglassTop, null, tint = C.Warning, modifier = Modifier.size(16.dp))
            Text("Your request is waiting for admin approval.", color = C.TextSecondary, style = T.BodySmall)
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Surface(color = C.Surface, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, C.Border)) {
        Text(text, color = C.TextSecondary, style = T.Caption, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}