package com.example.kampus.ui.screens.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
<<<<<<< HEAD
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
=======
>>>>>>> 16d62ee (done admin features)
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
<<<<<<< HEAD
import androidx.compose.foundation.layout.width
=======
>>>>>>> 16d62ee (done admin features)
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
<<<<<<< HEAD
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
=======
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
>>>>>>> 16d62ee (done admin features)
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
<<<<<<< HEAD
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
=======
>>>>>>> 16d62ee (done admin features)
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
<<<<<<< HEAD
import androidx.compose.material.icons.outlined.ChatBubbleOutline
=======
>>>>>>> 16d62ee (done admin features)
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
<<<<<<< HEAD
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.kampus.data.model.GroupPostComment
=======
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
>>>>>>> 16d62ee (done admin features)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
<<<<<<< HEAD
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.AsyncImage
=======
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
>>>>>>> 16d62ee (done admin features)
import com.example.kampus.data.model.GroupPost
import com.example.kampus.data.model.MembershipStatus
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.components.groups.GroupActionButton
import com.example.kampus.ui.components.groups.PrivacyBadge
import com.example.kampus.ui.components.groups.ReportPostDialog
import com.example.kampus.ui.components.groups.UserAvatar
import com.example.kampus.ui.components.groups.formatCount
import com.example.kampus.ui.components.groups.timeAgo
import com.example.kampus.ui.theme.KampusColors as C
import com.example.kampus.ui.theme.KampusType as T
import com.example.kampus.utils.rememberImagePickerLauncher
<<<<<<< HEAD
import com.example.kampus.utils.rememberImagePickerLauncher
=======
>>>>>>> 16d62ee (done admin features)
import com.example.kampus.viewmodel.GroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onOpenAdminPanel: (String) -> Unit,
    onHomeClick: () -> Unit = {},
    onEventsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCreatePost: () -> Unit = {},
) {
    val state by viewModel.groupDetailUiState.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
<<<<<<< HEAD
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    var postText by remember { mutableStateOf("") }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<GroupPost?>(null) }
    var activeCommentPost by remember { mutableStateOf<GroupPost?>(null) }
    val comments by viewModel.commentsState.collectAsStateWithLifecycle()
    val commentsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
=======
    var postText by remember { mutableStateOf("") }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<GroupPost?>(null) }
>>>>>>> 16d62ee (done admin features)
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val imagePicker = rememberImagePickerLauncher { uri ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetail(groupId)
        viewModel.loadAdminPanel(groupId)
    }

    LaunchedEffect(state.postSubmitted) {
        if (state.postSubmitted) postText = ""
    }

    LaunchedEffect(state.uploadError) {
        state.uploadError?.let { snackbarHostState.showSnackbar(it) }
    }

<<<<<<< HEAD
    LaunchedEffect(activeCommentPost) {
        val post = activeCommentPost
        if (post != null) {
            viewModel.observeComments(groupId, post.id)
        } else {
            viewModel.stopObservingComments()
        }
    }

    val group = state.group

    androidx.compose.material3.Scaffold(
    androidx.compose.material3.Scaffold(
=======
    val group = state.group

    androidx.compose.material3.Scaffold(
>>>>>>> 16d62ee (done admin features)
        containerColor = C.Background,
        snackbarHost = {
            androidx.compose.material3.SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    containerColor = C.Surface,
                    contentColor = C.TextPrimary,
                    snackbarData = data,
                )
            }
        },
<<<<<<< HEAD
        snackbarHost = {
            androidx.compose.material3.SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    containerColor = C.Surface,
                    contentColor = C.TextPrimary,
                    snackbarData = data,
                )
            }
        },
=======
>>>>>>> 16d62ee (done admin features)
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
                val currentUserRole = com.example.kampus.ui.components.rememberCurrentUserRole()
                val isAdmin = currentUserRole.equals("admin", ignoreCase = true)
                val navItems = com.example.kampus.ui.components.rememberCampusNavItems(isAdmin)

                CampusBottomNavBar(
                    selectedIndex = 1, // Groups tab
                    navItems = navItems,
                    onItemSelected = { index ->
                        when {
                            isAdmin -> when (index) {
                                0 -> onHomeClick()
                                1 -> { /* already on groups */ }
                                2 -> onAdminClick()
                                3 -> onChatClick()
                            }
                            else -> when (index) {
                                0 -> onHomeClick()
                                1 -> { /* already on groups */ }
                                2 -> onEventsClick()
                                3 -> onChatClick()
                            }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(32.dp))
                    Text("Loading group...", color = C.TextSecondary, style = T.BodyMedium)
                }
<<<<<<< HEAD
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(32.dp))
                    Text("Loading group...", color = C.TextSecondary, style = T.BodyMedium)
                }
=======
>>>>>>> 16d62ee (done admin features)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
<<<<<<< HEAD
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Cover image ──────────────────────────────────────────────────
            // ── Cover image ──────────────────────────────────────────────────
=======
        ) {
            // ── Cover image ──────────────────────────────────────────────────
>>>>>>> 16d62ee (done admin features)
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    if (group.coverImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = group.coverImageUrl,
<<<<<<< HEAD
                        AsyncImage(
                            model = group.coverImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            contentScale = ContentScale.Crop,
=======
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
>>>>>>> 16d62ee (done admin features)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    listOf(C.SurfaceElevated, C.Primary.copy(alpha = 0.6f), C.Background)
                                )
                            )
                        )
<<<<<<< HEAD
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    listOf(C.SurfaceElevated, C.Primary.copy(alpha = 0.6f), C.Background)
                                )
                            )
                        )
                    }

                    // Gradient overlay for readability
                    // Gradient overlay for readability
=======
                    }

                    // Gradient overlay for readability
>>>>>>> 16d62ee (done admin features)
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Transparent, C.Background.copy(alpha = 0.88f))
                            )
<<<<<<< HEAD
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Transparent, C.Background.copy(alpha = 0.88f))
                            )
=======
>>>>>>> 16d62ee (done admin features)
                        )
                    )

                    // Back button
<<<<<<< HEAD
                    // Back button
=======
>>>>>>> 16d62ee (done admin features)
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 14.dp, top = 10.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.28f)),
<<<<<<< HEAD
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 14.dp, top = 10.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.28f)),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }

                    // Admin panel button
                    // Admin panel button
=======
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }

                    // Admin panel button
>>>>>>> 16d62ee (done admin features)
                    if (state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN) {
                        IconButton(
                            onClick = { onOpenAdminPanel(group.id) },
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(end = 14.dp, top = 10.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(C.Primary),
<<<<<<< HEAD
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(end = 14.dp, top = 10.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(C.Primary),
=======
>>>>>>> 16d62ee (done admin features)
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, null, tint = Color.White)
                        }
                    }

                    // Group name + privacy badge at bottom of cover
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(group.name, color = Color.White, style = T.HeadingLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
<<<<<<< HEAD
                    // Group name + privacy badge at bottom of cover
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(group.name, color = Color.White, style = T.HeadingLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
=======
>>>>>>> 16d62ee (done admin features)
                        PrivacyBadge(group.privacy)
                    }
                }
            }

            // ── Group meta info ──────────────────────────────────────────────
<<<<<<< HEAD
            // ── Group meta info ──────────────────────────────────────────────
=======
>>>>>>> 16d62ee (done admin features)
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Stats chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
<<<<<<< HEAD
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Stats chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
=======
>>>>>>> 16d62ee (done admin features)
                        StatChip(text = formatCount(group.memberCount) + " members")
                        StatChip(text = formatCount(group.postCount) + " posts")
                        StatChip(text = group.category)
                    }

                    // Description
                    if (group.description.isNotBlank()) {
                        Text(group.description, color = C.TextSecondary, style = T.BodyMedium)
                    }
<<<<<<< HEAD
                    // Description
                    if (group.description.isNotBlank()) {
                        Text(group.description, color = C.TextSecondary, style = T.BodyMedium)
                    }

                    // Join / Leave / Pending button
                    // Join / Leave / Pending button
=======

                    // Join / Leave / Pending button
>>>>>>> 16d62ee (done admin features)
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

                    // Leave group (non-admin members only)
<<<<<<< HEAD
                    // Leave group (non-admin members only)
=======
>>>>>>> 16d62ee (done admin features)
                    if (state.membershipStatus == MembershipStatus.MEMBER && state.currentUserRole != com.example.kampus.data.model.MemberRole.ADMIN) {
                        TextButton(onClick = { showLeaveDialog = true }) {
                            Text("Leave Group", color = C.Error, style = T.LabelMedium)
                        }
<<<<<<< HEAD
                        TextButton(onClick = { showLeaveDialog = true }) {
                            Text("Leave Group", color = C.Error, style = T.LabelMedium)
                        }
                    }

                    // Pending approval banner
                    // Pending approval banner
=======
                    }

                    // Pending approval banner
>>>>>>> 16d62ee (done admin features)
                    if (state.membershipStatus == MembershipStatus.PENDING) {
                        PendingApprovalBanner()
                    }
                }
            }

            // ── Post composer (members only) ─────────────────────────────────
<<<<<<< HEAD
            // ── Post composer (members only) ─────────────────────────────────
=======
>>>>>>> 16d62ee (done admin features)
            if (state.membershipStatus == MembershipStatus.MEMBER) {
                item {
                    PostComposer(
                        postText = postText,
                        onTextChange = { postText = it },
                        selectedImageUri = selectedImageUri,
                        isLoading = state.isLoading,
                        currentUserProfileImageUrl = state.currentUserProfileImageUrl,
                        currentUserName = state.currentUserName,
                        currentUserId = viewModel.currentUserId,
                        onPickImage = { imagePicker.launch("image/*") },
                        onClearImage = { viewModel.onImageSelected(null) },
                        onPost = {
                            if (postText.isNotBlank() || selectedImageUri != null) {
                                viewModel.createPost(group.id, postText)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Recent posts header ──────────────────────────────────────────
<<<<<<< HEAD
                    PostComposer(
                        postText = postText,
                        onTextChange = { postText = it },
                        selectedImageUri = selectedImageUri,
                        isLoading = state.isLoading,
                        currentUserProfileImageUrl = state.currentUserProfileImageUrl,
                        currentUserName = state.currentUserName,
                        currentUserId = viewModel.currentUserId,
                        onPickImage = { imagePicker.launch("image/*") },
                        onClearImage = { viewModel.onImageSelected(null) },
                        onPost = {
                            if (postText.isNotBlank() || selectedImageUri != null) {
                                viewModel.createPost(group.id, postText)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Recent posts header ──────────────────────────────────────────
=======
>>>>>>> 16d62ee (done admin features)
            item {
                val reportedCount = state.posts.count { it.isReported || it.reportCount > 0 }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
<<<<<<< HEAD
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
=======
>>>>>>> 16d62ee (done admin features)
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent Posts", color = C.TextPrimary, style = T.HeadingSmall, fontWeight = FontWeight.Bold)
<<<<<<< HEAD
                    Text("Recent Posts", color = C.TextPrimary, style = T.HeadingSmall, fontWeight = FontWeight.Bold)
                    if (state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN && reportedCount > 0) {
                        Badge(containerColor = C.Error.copy(alpha = 0.16f), contentColor = C.Error) {
                            Text("$reportedCount reported")
                        }
                        Badge(containerColor = C.Error.copy(alpha = 0.16f), contentColor = C.Error) {
                            Text("$reportedCount reported")
=======
                    if (state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN && reportedCount > 0) {
                        Badge(containerColor = C.Error.copy(alpha = 0.16f), contentColor = C.Error) {
                            Text("$reportedCount reported")
>>>>>>> 16d62ee (done admin features)
                        }
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────────────────
<<<<<<< HEAD
            // ── Empty state ──────────────────────────────────────────────────
=======
>>>>>>> 16d62ee (done admin features)
            if (state.posts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
<<<<<<< HEAD
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("No posts yet", color = C.TextSecondary, style = T.HeadingSmall)
                        Text("Be the first to share something!", color = C.TextMuted, style = T.BodyMedium)
                        Text("Be the first to share something!", color = C.TextMuted, style = T.BodyMedium)
=======
                        Text("No posts yet", color = C.TextSecondary, style = T.HeadingSmall)
                        Text("Be the first to share something!", color = C.TextMuted, style = T.BodyMedium)
>>>>>>> 16d62ee (done admin features)
                    }
                }
            } else {
                items(state.posts, key = { it.id }) { post ->
                    GroupPostCard(
<<<<<<< HEAD
                    GroupPostCard(
=======
>>>>>>> 16d62ee (done admin features)
                        post = post,
                        isAdmin = state.currentUserRole == com.example.kampus.data.model.MemberRole.ADMIN,
                        onLikeClick = { viewModel.toggleLikePost(group.id, post.id) },
                        onCommentClick = { activeCommentPost = post },
                        onReportClick = { reportTarget = post },
                        onDeleteClick = { viewModel.deletePost(group.id, post.id) },
                        onRemoveAuthorClick = { viewModel.removeMemberFromGroup(group.id, post.authorId) },
                        modifier = Modifier.padding(horizontal = 14.dp),
<<<<<<< HEAD
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Spacer(Modifier.height(10.dp))
=======
                    )
                    Spacer(Modifier.height(10.dp))
>>>>>>> 16d62ee (done admin features)
                }
            }
        }

        // ── Leave dialog ─────────────────────────────────────────────────────
<<<<<<< HEAD
        // ── Leave dialog ─────────────────────────────────────────────────────
=======
>>>>>>> 16d62ee (done admin features)
        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                containerColor = C.Surface,
                title = { Text("Leave group", color = C.TextPrimary) },
                text = { Text("Are you sure you want to leave this group?", color = C.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.leaveGroup(group.id); showLeaveDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Error, contentColor = Color.White),
                    ) { Text("Leave") }
<<<<<<< HEAD
                    Button(
                        onClick = { viewModel.leaveGroup(group.id); showLeaveDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Error, contentColor = Color.White),
                    ) { Text("Leave") }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("Cancel", color = C.TextSecondary)
                    }
=======
>>>>>>> 16d62ee (done admin features)
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("Cancel", color = C.TextSecondary)
                    }
                },
            )
        }

        // ── Report dialog ────────────────────────────────────────────────────
<<<<<<< HEAD
        // ── Report dialog ────────────────────────────────────────────────────
=======
>>>>>>> 16d62ee (done admin features)
        reportTarget?.let { post ->
            ReportPostDialog(
                onDismiss = { reportTarget = null },
                onReport = { reason, note ->
                    viewModel.reportPost(group.id, post.id, reason, note)
                    reportTarget = null
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Compact Post Composer with real user profile
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PostComposer(
    postText: String,
    onTextChange: (String) -> Unit,
    selectedImageUri: android.net.Uri?,
    isLoading: Boolean,
    currentUserProfileImageUrl: String,
    currentUserName: String,
    currentUserId: String,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onPost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, C.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── User avatar + text field row ───────────────────────────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Real user avatar
                UserAvatar(
                    imageUrl = currentUserProfileImageUrl.takeIf { it.isNotBlank() },
                    name = currentUserName.ifBlank { "You" },
                    size = 36.dp,
                    userId = currentUserId,
                )

                // Compact text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.SurfaceElevated)
                        .border(1.dp, if (postText.isNotBlank()) C.Primary.copy(alpha = 0.5f) else C.Border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = postText,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = C.TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(C.Primary),
                        maxLines = 6,
                        decorationBox = { inner ->
                            if (postText.isEmpty()) {
                                Text(
                                    "Share something with the group…",
                                    color = C.TextHint,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            // ── Selected image preview ─────────────────────────────────────
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 46.dp) // indent to align with text field
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, C.Border, RoundedCornerShape(10.dp)),
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // Remove button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClearImage,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            // ── Action row: image picker + post button ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Image picker icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedImageUri != null) C.Primary.copy(alpha = 0.15f) else C.SurfaceElevated)
                        .border(1.dp, if (selectedImageUri != null) C.Primary.copy(alpha = 0.4f) else C.Border, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPickImage,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Add photo",
                        tint = if (selectedImageUri != null) C.Primary else C.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Post button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (postText.isNotBlank() || selectedImageUri != null)
                                Brush.linearGradient(listOf(C.Primary, C.Primary.copy(alpha = 0.8f)))
                            else
                                Brush.linearGradient(listOf(C.SurfaceElevated, C.SurfaceElevated))
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isLoading && (postText.isNotBlank() || selectedImageUri != null),
                            onClick = onPost,
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Post",
                                tint = if (postText.isNotBlank() || selectedImageUri != null) Color.White else C.TextHint,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                "Post",
                                color = if (postText.isNotBlank() || selectedImageUri != null) Color.White else C.TextHint,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Post Card — shows real avatar, image, like/comment counts
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupPostCard(
    post: GroupPost,
    isAdmin: Boolean,
    onLikeClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val likeColor by animateColorAsState(
        targetValue = if (post.isLikedByCurrentUser) C.Error else C.TextMuted,
        animationSpec = tween(180),
        label = "like_color",
    )

    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, C.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Author row ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Real profile avatar via Coil
                UserAvatar(
                    imageUrl = post.authorAvatarUrl.takeIf { it.isNotBlank() },
                    name = post.authorName,
                    size = 40.dp,
                    userId = post.authorId,
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = post.authorName,
                            color = C.TextPrimary,
                            style = T.HeadingSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(post.isReported || post.reportCount > 0) {
                            Badge(containerColor = C.Error.copy(alpha = 0.15f), contentColor = C.Error) {
                                Text("Reported", fontSize = 10.sp)
                            }
                        }
                    }
                    Text(timeAgo(post.createdAt), color = C.TextMuted, style = T.Caption)
                }

                // Kebab menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = C.TextMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Delete post", color = C.Error) },
                                onClick = { menuExpanded = false; onDeleteClick() },
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from group") },
                                onClick = { menuExpanded = false; onRemoveAuthorClick() },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report post") },
                                onClick = { menuExpanded = false; onReportClick() },
                            )
                        }
                    }
                }
            }

            // ── Content text ───────────────────────────────────────────────
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    color = C.TextSecondary,
                    style = T.BodyMedium,
                    lineHeight = 22.sp,
                )
            }

            // ── Post image (if present) ────────────────────────────────────
            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, C.Border, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            // ── Like + comment row ─────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Like chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (post.isLikedByCurrentUser) C.Error.copy(alpha = 0.1f) else C.SurfaceElevated)
                        .border(1.dp, if (post.isLikedByCurrentUser) C.Error.copy(alpha = 0.3f) else C.Border, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLikeClick,
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = formatCount(post.likeCount),
                        color = likeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Comment count chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(C.SurfaceElevated)
                        .border(1.dp, C.Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = C.TextMuted,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = formatCount(post.commentCount),
                        color = C.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pending banner & stat chip helpers
// ─────────────────────────────────────────────────────────────────────────────

<<<<<<< HEAD
        // ── Comments Bottom Sheet ─────────────────────────────────────────────
        activeCommentPost?.let { post ->
            ModalBottomSheet(
                onDismissRequest = { activeCommentPost = null },
                sheetState = commentsSheetState,
                containerColor = C.Background,
                contentColor = C.TextPrimary,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(C.Border.copy(alpha = 0.5f))
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .imePadding()
                ) {
                    Text(
                        text = "Comments",
                        color = Color.White,
                        style = T.HeadingSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet. Start the conversation!",
                                        color = C.TextMuted,
                                        style = T.BodyMedium
                                    )
                                }
                            }
                        } else {
                            items(comments, key = { it.id }) { comment ->
                                GroupCommentRow(comment = comment)
                            }
                        }
                    }

                    var newCommentText by remember { mutableStateOf("") }
                    val keyboardController = LocalSoftwareKeyboardController.current

                    Surface(
                        color = C.Surface,
                        border = BorderStroke(1.dp, C.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserAvatar(
                                imageUrl = state.currentUserProfileImageUrl.takeIf { it.isNotBlank() },
                                name = state.currentUserName.ifBlank { "You" },
                                size = 32.dp,
                                userId = viewModel.currentUserId
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(C.SurfaceElevated)
                                    .border(1.dp, if (newCommentText.isNotBlank()) C.Primary.copy(alpha = 0.5f) else C.Border, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = newCommentText,
                                    onValueChange = { newCommentText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(
                                        color = C.TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(C.Primary),
                                    decorationBox = { inner ->
                                        if (newCommentText.isEmpty()) {
                                            Text(
                                                text = "Write a comment...",
                                                color = C.TextHint,
                                                fontSize = 14.sp
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        viewModel.addComment(group.id, post.id, newCommentText)
                                        newCommentText = ""
                                        keyboardController?.hide()
                                    }
                                },
                                enabled = newCommentText.isNotBlank(),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (newCommentText.isNotBlank()) C.Primary else C.SurfaceElevated)
                                    .size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (newCommentText.isNotBlank()) Color.White else C.TextHint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Compact Post Composer with real user profile
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PostComposer(
    postText: String,
    onTextChange: (String) -> Unit,
    selectedImageUri: android.net.Uri?,
    isLoading: Boolean,
    currentUserProfileImageUrl: String,
    currentUserName: String,
    currentUserId: String,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onPost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, C.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── User avatar + text field row ───────────────────────────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Real user avatar
                UserAvatar(
                    imageUrl = currentUserProfileImageUrl.takeIf { it.isNotBlank() },
                    name = currentUserName.ifBlank { "You" },
                    size = 36.dp,
                    userId = currentUserId,
                )

                // Compact text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.SurfaceElevated)
                        .border(1.dp, if (postText.isNotBlank()) C.Primary.copy(alpha = 0.5f) else C.Border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = postText,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = C.TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(C.Primary),
                        maxLines = 6,
                        decorationBox = { inner ->
                            if (postText.isEmpty()) {
                                Text(
                                    "Share something with the group…",
                                    color = C.TextHint,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            // ── Selected image preview ─────────────────────────────────────
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 46.dp) // indent to align with text field
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, C.Border, RoundedCornerShape(10.dp)),
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // Remove button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClearImage,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            // ── Action row: image picker + post button ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Image picker icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedImageUri != null) C.Primary.copy(alpha = 0.15f) else C.SurfaceElevated)
                        .border(1.dp, if (selectedImageUri != null) C.Primary.copy(alpha = 0.4f) else C.Border, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPickImage,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Add photo",
                        tint = if (selectedImageUri != null) C.Primary else C.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Post button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (postText.isNotBlank() || selectedImageUri != null)
                                Brush.linearGradient(listOf(C.Primary, C.Primary.copy(alpha = 0.8f)))
                            else
                                Brush.linearGradient(listOf(C.SurfaceElevated, C.SurfaceElevated))
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isLoading && (postText.isNotBlank() || selectedImageUri != null),
                            onClick = onPost,
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Post",
                                tint = if (postText.isNotBlank() || selectedImageUri != null) Color.White else C.TextHint,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                "Post",
                                color = if (postText.isNotBlank() || selectedImageUri != null) Color.White else C.TextHint,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Post Card — shows real avatar, image, like/comment counts
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupPostCard(
    post: GroupPost,
    isAdmin: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val likeColor by animateColorAsState(
        targetValue = if (post.isLikedByCurrentUser) C.Error else C.TextMuted,
        animationSpec = tween(180),
        label = "like_color",
    )

    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, C.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Author row ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Real profile avatar via Coil
                UserAvatar(
                    imageUrl = post.authorAvatarUrl.takeIf { it.isNotBlank() },
                    name = post.authorName,
                    size = 40.dp,
                    userId = post.authorId,
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = post.authorName,
                            color = C.TextPrimary,
                            style = T.HeadingSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(post.isReported || post.reportCount > 0) {
                            Badge(containerColor = C.Error.copy(alpha = 0.15f), contentColor = C.Error) {
                                Text("Reported", fontSize = 10.sp)
                            }
                        }
                    }
                    Text(timeAgo(post.createdAt), color = C.TextMuted, style = T.Caption)
                }

                // Kebab menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = C.TextMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Delete post", color = C.Error) },
                                onClick = { menuExpanded = false; onDeleteClick() },
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from group") },
                                onClick = { menuExpanded = false; onRemoveAuthorClick() },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report post") },
                                onClick = { menuExpanded = false; onReportClick() },
                            )
                        }
                    }
                }
            }

            // ── Content text ───────────────────────────────────────────────
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    color = C.TextSecondary,
                    style = T.BodyMedium,
                    lineHeight = 22.sp,
                )
            }

            // ── Post image (if present) ────────────────────────────────────
            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, C.Border, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            // ── Like + comment row ─────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Like chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (post.isLikedByCurrentUser) C.Error.copy(alpha = 0.1f) else C.SurfaceElevated)
                        .border(1.dp, if (post.isLikedByCurrentUser) C.Error.copy(alpha = 0.3f) else C.Border, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLikeClick,
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = formatCount(post.likeCount),
                        color = likeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Comment count chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(C.SurfaceElevated)
                        .border(1.dp, C.Border, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCommentClick,
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = C.TextMuted,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = formatCount(post.commentCount),
                        color = C.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pending banner & stat chip helpers
// ─────────────────────────────────────────────────────────────────────────────

=======
>>>>>>> 16d62ee (done admin features)
@Composable
private fun PendingApprovalBanner() {
    Surface(
        color = C.Warning.copy(alpha = 0.13f),
        contentColor = C.Warning,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, C.Warning.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.HourglassTop, null, tint = C.Warning, modifier = Modifier.size(15.dp))
<<<<<<< HEAD
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.HourglassTop, null, tint = C.Warning, modifier = Modifier.size(15.dp))
=======
>>>>>>> 16d62ee (done admin features)
            Text("Your request is waiting for admin approval.", color = C.TextSecondary, style = T.BodySmall)
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, C.Border),
    ) {
        Text(
            text = text,
            color = C.TextSecondary,
            style = T.Caption,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
<<<<<<< HEAD
    Surface(
        color = C.Surface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, C.Border),
    ) {
        Text(
            text = text,
            color = C.TextSecondary,
            style = T.Caption,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun GroupCommentRow(comment: GroupPostComment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .border(1.dp, C.Border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            imageUrl = comment.authorAvatarUrl.takeIf { it.isNotBlank() },
            name = comment.authorName,
            size = 32.dp,
            userId = comment.authorId
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.authorName,
                    color = C.TextPrimary,
                    style = T.HeadingSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeAgo(comment.createdAt),
                    color = C.TextMuted,
                    style = T.Caption
                )
            }
            Text(
                text = comment.content,
                color = C.TextSecondary,
                style = T.BodyMedium,
                lineHeight = 18.sp
            )
        }
=======
>>>>>>> 16d62ee (done admin features)
    }
}