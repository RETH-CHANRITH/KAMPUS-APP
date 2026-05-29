@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.feed
import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.components.NavItem
import com.example.kampus.ui.chat.ChatStory
import com.example.kampus.ui.chat.ChatViewModel
import com.example.kampus.ui.chat.FullCreateStoryScreen
import com.example.kampus.ui.chat.StoryEntryMode
import com.example.kampus.ui.story.StoryViewerOverlay
import com.example.kampus.ui.chat.ChatViewModel
import com.example.kampus.ui.chat.FullCreateStoryScreen
import com.example.kampus.ui.chat.StoryEntryMode
import com.example.kampus.ui.story.StoryViewerOverlay
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.utils.ActivityLogger

// ─────────────────────────────────────────────────────────────────────────────
// Palette
// ─────────────────────────────────────────────────────────────────────────────
private val UiIsDark get() = ThemeController.isDark
private val HBg get() = if (UiIsDark) Color(0xFF080B11) else Color(0xFFF3F4F8)
private val HCard get() = if (UiIsDark) Color(0xFF0F1520) else Color(0xFFFFFFFF)
private val HBorder get() = if (UiIsDark) Color(0xFF1A2333) else Color(0xFFD1D5DB)
private val HBlue get() = ThemeController.accent.color
private val HGlow get() = HBlue.copy(alpha = if (UiIsDark) 0.75f else 0.55f)
private val HWhite get() = if (UiIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val HGray2 get() = if (UiIsDark) Color(0xFFE5E7EB) else Color(0xFF374151)
private val HGray4 get() = if (UiIsDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val HGray6 get() = if (UiIsDark) Color(0xFF374151) else Color(0xFF9CA3AF)
private val HRed get() = Color(0xFFEF4444)
private val HNavBg get() = if (UiIsDark) Color(0xFF0C1018) else Color(0xFFF3F4F8)
private val HChipBg get() = if (UiIsDark) Color(0xFF111827) else Color(0xFFEFF2F7)

private fun formatPostTime(post: PostItem): String {
    val timestamp = post.timestamp
    return if (timestamp > 0L) {
        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } else {
        post.time
    }
}

private fun postMetadataLines(post: PostItem): List<String> = buildList {
    post.feelingEmoji?.takeIf { it.isNotBlank() }?.let { add("Feeling $it") }
    post.feeling?.takeIf { it.isNotBlank() }?.let { feeling ->
        if (post.feelingEmoji.isNullOrBlank()) add("Feeling $feeling")
    }
    post.location?.takeIf { it.isNotBlank() }?.let { add("📍 $it") }
    if (post.tags.isNotEmpty()) add(post.tags.take(4).joinToString(" "))
    if (post.taggedPeople.isNotEmpty()) add("With ${post.taggedPeople.take(3).joinToString(", ")}")
    if (post.visibility != PostItem.PostVisibility.PUBLIC) {
        add("Visible to ${post.visibility.name.lowercase().replaceFirstChar { it.uppercaseChar() }}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────────────────────────────────────


private data class NavItem(
    val label        : String,
    val icon         : ImageVector,
    val iconSelected : ImageVector,
)

// ─────────────────────────────────────────────────────────────────────────────
// Static data
// ─────────────────────────────────────────────────────────────────────────────


private val navItems = listOf(
    NavItem("Home",   Icons.Outlined.Home,              Icons.Filled.Home),
    NavItem("Groups", Icons.Outlined.Group,             Icons.Filled.Group),
    NavItem("Events", Icons.Outlined.CalendarMonth,     Icons.Filled.CalendarMonth),
    NavItem("Chat",   Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
)



// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onCreatePost   : () -> Unit = {},
    onProfileClick : () -> Unit = {},
    onNotifClick   : () -> Unit = {},
    onSearchClick  : () -> Unit = {},
    onPostClick    : (Int) -> Unit = {},
    onGroupsClick  : () -> Unit = {},   // ← ADDED
    onEventsClick  : () -> Unit = {},   // ← ADDED (for future use)
    onChatClick    : () -> Unit = {},   // ← ADDED (for future use)
    onAdminClick   : () -> Unit = {},
    viewModel      : FeedViewModel = viewModel(),
    chatViewModel  : ChatViewModel = viewModel(),
    chatViewModel  : ChatViewModel = viewModel(),
) {
    val vm = viewModel
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val chatListState by chatViewModel.chatListState.collectAsStateWithLifecycle()
    val chatListState by chatViewModel.chatListState.collectAsStateWithLifecycle()
    val likedPosts by vm.likedIds.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val posts = uiState.posts


    var selectedNav by remember { mutableIntStateOf(0) }
    val unreadNotifsCount by vm.unreadCount.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pendingDeletePost by remember { mutableStateOf<PostItem?>(null) }
    var confirmDeleteVisible by rememberSaveable { mutableStateOf(false) }
    var recentlyDeletedPost by remember { mutableStateOf<PostItem?>(null) }
    var pendingReportPost by remember { mutableStateOf<PostItem?>(null) }
    var pendingBlockPost by remember { mutableStateOf<PostItem?>(null) }
    var pendingSharePost by remember { mutableStateOf<PostItem?>(null) }

    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var selectedStoryId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = HBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Surface(
                    color          = HBg,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    TopBar(
                        notifCount    = unreadNotifsCount,
                        onNotifClick  = onNotifClick,
                        onSearchClick = onSearchClick,
                    )
                }
            },
            bottomBar = {
                val strings = com.example.kampus.ui.localization.rememberUiStrings()
                val isAdmin = uiState.currentUserRole == "admin"
                
                val navItems = remember(strings, isAdmin) {
                    if (isAdmin) {
                        listOf(
                            com.example.kampus.ui.components.NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
                            com.example.kampus.ui.components.NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
                            com.example.kampus.ui.components.NavItem(strings.adminPanel, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
                            com.example.kampus.ui.components.NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
                        )
                    } else {
                        listOf(
                            com.example.kampus.ui.components.NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
                            com.example.kampus.ui.components.NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
                            com.example.kampus.ui.components.NavItem(strings.events, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
                            com.example.kampus.ui.components.NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, HBg.copy(alpha = 0.98f))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                ) {
                    CampusBottomNavBar(
                        navItems       = navItems,
                        selectedIndex  = selectedNav,
                        onItemSelected = { index ->
                            selectedNav = index
                            if (isAdmin) {
                                when (index) {
                                    0 -> { /* already on Home */ }
                                    1 -> onGroupsClick()
                                    2 -> onAdminClick()
                                    3 -> onChatClick()
                                }
                            } else {
                                when (index) {
                                    0 -> { /* already on Home */ }
                                    1 -> onGroupsClick()
                                    2 -> onEventsClick()
                                    3 -> onChatClick()
                                }
                            }
                        },
                        onFabClick     = onCreatePost,
                        onProfileClick = onProfileClick,
                        isProfileSelected = false,
                    )
                }
            },
        ) { innerPadding ->
            LazyColumn(
                state          = listState,
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (uiState.isLoading && posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = HBlue)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    val imageStoriesByOwner = remember(chatListState.stories) {
                        chatListState.stories.filter { it.storyType == "image" || it.storyType == "video" }.groupBy { it.ownerId }
                    }
                    StoriesRow(
                        stories                    = stories,
                        currentUserProfileImageUrl = uiState.currentUserProfileImageUrl,
                        currentUserAvatarEmoji     = uiState.currentUserAvatarEmoji,
                        friendsAndFollowers        = uiState.friendsAndFollowers,
                        onCreateStory              = { showCreateStoryDialog = true },
                        onOpenStory                = { story -> selectedStoryId = story.id },
                        imageStoriesByOwner        = imageStoriesByOwner,
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(
                        color     = HBorder.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post    = post,
                        isLiked = post.id in likedPosts || (currentUserId.isNotBlank() && currentUserId in post.likedBy),
                        isSaved = post.id in uiState.savedPostIds,
                        onLike  = {
                            vm.toggleLike(post.id)
                        },
                        onShare = {
                            vm.incrementShareCount(post.id)
                            ActivityLogger.logAction(
                                type = "share_post",
                                text = "Shared post by ${post.author}",
                                metadata = mapOf("postId" to post.id.toString(), "author" to post.author),
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "${post.author} on Kampus")
                                putExtra(Intent.EXTRA_TEXT, "${post.author}: ${post.content}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                        },
                        
                        onMenuAction = { targetPost, action ->
                            when (action) {
                                "open" -> {
                                    onPostClick(targetPost.id)
                                }
                                "copy" -> {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("post", targetPost.content))
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post text copied") }
                                }
                                "pin" -> {
                                    vm.pinPostBackend(targetPost.id, true)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post pinned") }
                                }
                                "unpin" -> {
                                    vm.pinPostBackend(targetPost.id, false)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post unpinned") }
                                }
                                "trash" -> {
                                    // Ask for confirmation before deleting
                                    pendingDeletePost = targetPost
                                    confirmDeleteVisible = true
                                }
                                "edit" -> {
                                    ActivityLogger.logAction(type = "edit_post", text = "Edit requested for ${targetPost.id}")
                                }
                                "privacy_public" -> {
                                    vm.updatePostVisibility(targetPost.id, PostItem.PostVisibility.PUBLIC)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy set to Public") }
                                }
                                "privacy_friends" -> {
                                    vm.updatePostVisibility(targetPost.id, PostItem.PostVisibility.FRIENDS)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy set to Friends") }
                                }
                                "privacy_private" -> {
                                    vm.updatePostVisibility(targetPost.id, PostItem.PostVisibility.PRIVATE)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy set to Only Me") }
                                }
                                "privacy" -> {
                                    vm.updatePostVisibility(targetPost.id, targetPost.visibility)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy updated") }
                                }
                                "hide_profile" -> {
                                    vm.hideFromProfileBackend(targetPost.id)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Hidden from profile") }
                                }
                                "save" -> {
                                    vm.savePost(targetPost.id, true)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post saved") }
                                }
                                "unsave" -> {
                                    vm.savePost(targetPost.id, false)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post unsaved") }
                                }
                                "share" -> {
                                    vm.incrementShareCount(targetPost.id)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "${targetPost.author} on Kampus")
                                        putExtra(Intent.EXTRA_TEXT, "${targetPost.author}: ${targetPost.content}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                                }
                                "copy_link" -> {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("post_link", "https://kampus.app/post/${targetPost.id}"))
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Link copied to clipboard") }
                                }
                                "not_interested" -> {
                                    vm.notInterested(targetPost.id)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Marked as Not Interested") }
                                }
                                "hide_post" -> {
                                    vm.hidePost(targetPost.id)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post hidden") }
                                }
                                "mute_user" -> {
                                    vm.muteUser(targetPost.authorId)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Muted posts from ${targetPost.author}") }
                                }
                                "report" -> {
                                    pendingReportPost = targetPost
                                }
                                "block" -> {
                                    pendingBlockPost = targetPost
                                }
                                else -> {
                                    ActivityLogger.logAction(type = "post_menu_action", text = "$action for ${targetPost.id}")
                                }
                            }
                        },
                        onComment = { onPostClick(post.id) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
                item {
                    Spacer(Modifier.height(8.dp))
                    val imageStoriesByOwner = remember(chatListState.stories) {
                        chatListState.stories.filter { it.storyType == "image" || it.storyType == "video" }.groupBy { it.ownerId }
                    }
                    StoriesRow(
                        following                  = uiState.following,
                        currentUserProfileImageUrl = uiState.currentUserProfileImageUrl,
                        currentUserAvatarEmoji     = uiState.currentUserAvatarEmoji,
                        onCreateStory              = { showCreateStoryDialog = true },
                        onOpenStory                = { story -> selectedStoryId = story.id },
                        imageStoriesByOwner        = imageStoriesByOwner,
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(
                        color     = HBorder.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post    = post,
                        isLiked = post.id in likedPosts || (currentUserId.isNotBlank() && currentUserId in post.likedBy),
                        isSaved = post.id in uiState.savedPostIds,
                        onLike  = {
                            vm.toggleLike(post.id)
                        },
                        onShare = {
                            pendingSharePost = post
                        },
                        onOpenSharedPost = { sharedPostId ->
                            onPostClick(sharedPostId)
                        },
                        
                        onMenuAction = { targetPost, action ->
                            when (action) {
                                "open" -> {
                                    onPostClick(targetPost.id)
                                }
                                "copy" -> {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("post", targetPost.content))
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post text copied") }
                                }
                                "pin" -> {
                                    vm.pinPostBackend(targetPost.id, true)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post pinned") }
                                }
                                "unpin" -> {
                                    vm.pinPostBackend(targetPost.id, false)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post unpinned") }
                                }
                                "trash" -> {
                                    // Ask for confirmation before deleting
                                    pendingDeletePost = targetPost
                                    confirmDeleteVisible = true
                                }
                                "edit" -> {
                                    ActivityLogger.logAction(type = "edit_post", text = "Edit requested for ${targetPost.id}")
                                }
                                "privacy_public" -> {
                                    vm.updatePostVisibility(targetPost.id, PostItem.PostVisibility.PUBLIC)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy set to Public") }
                                }
                                "privacy_friends" -> {
                                    vm.updatePostVisibility(targetPost.id, PostItem.PostVisibility.FRIENDS)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy set to Friends") }
                                }
                                "privacy_private" -> {
                                    vm.updatePostVisibility(targetPost.id, PostItem.PostVisibility.PRIVATE)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy set to Only Me") }
                                }
                                "privacy" -> {
                                    vm.updatePostVisibility(targetPost.id, targetPost.visibility)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Privacy updated") }
                                }
                                "hide_profile" -> {
                                    vm.hideFromProfileBackend(targetPost.id)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Hidden from profile") }
                                }
                                "save" -> {
                                    vm.savePost(targetPost.id, true)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post saved") }
                                }
                                "unsave" -> {
                                    vm.savePost(targetPost.id, false)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post unsaved") }
                                }
                                "share" -> {
                                    pendingSharePost = targetPost
                                }
                                "copy_link" -> {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("post_link", "https://kampus.app/post/${targetPost.id}"))
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Link copied to clipboard") }
                                }
                                "not_interested" -> {
                                    vm.notInterested(targetPost.id)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Marked as Not Interested") }
                                }
                                "hide_post" -> {
                                    vm.hidePost(targetPost.id)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Post hidden") }
                                }
                                "mute_user" -> {
                                    vm.muteUser(targetPost.authorId)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Muted posts from ${targetPost.author}") }
                                }
                                "report" -> {
                                    pendingReportPost = targetPost
                                }
                                "block" -> {
                                    pendingBlockPost = targetPost
                                }
                                else -> {
                                    ActivityLogger.logAction(type = "post_menu_action", text = "$action for ${targetPost.id}")
                                }
                            }
                        },
                        onComment = { onPostClick(post.id) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Confirmation dialog for destructive action
        if (confirmDeleteVisible && pendingDeletePost != null) {
            val target = pendingDeletePost!!
            AlertDialog(
                onDismissRequest = { confirmDeleteVisible = false; pendingDeletePost = null },
                title = { Text("Move to trash", color = HWhite) },
                text = { Text("Items in your trash are deleted after 30 days.", color = HGray4) },
                confirmButton = {
                    TextButton(onClick = {
                        // perform delete
                        recentlyDeletedPost = target
                        vm.deletePost(target.id)
                        vm.hideFromProfileBackend(target.id)
                        coroutineScope.launch {
                            val res = snackbarHostState.showSnackbar("Post moved to trash", actionLabel = "UNDO")
                            if (res == SnackbarResult.ActionPerformed) {
                                recentlyDeletedPost?.let { vm.restorePost(it) }
                                recentlyDeletedPost = null
                            }
                        }
                        confirmDeleteVisible = false
                        pendingDeletePost = null
                    }) { Text("Move to trash") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteVisible = false; pendingDeletePost = null }) { Text("Cancel") }
                }
            )
        }
        // Confirmation dialog for destructive action
        if (confirmDeleteVisible && pendingDeletePost != null) {
            val target = pendingDeletePost!!
            AlertDialog(
                onDismissRequest = { confirmDeleteVisible = false; pendingDeletePost = null },
                title = { Text("Move to trash", color = HWhite) },
                text = { Text("Items in your trash are deleted after 30 days.", color = HGray4) },
                confirmButton = {
                    TextButton(onClick = {
                        // perform delete
                        recentlyDeletedPost = target
                        vm.deletePost(target.id)
                        vm.hideFromProfileBackend(target.id)
                        coroutineScope.launch {
                            val res = snackbarHostState.showSnackbar("Post moved to trash", actionLabel = "UNDO")
                            if (res == SnackbarResult.ActionPerformed) {
                                recentlyDeletedPost?.let { vm.restorePost(it) }
                                recentlyDeletedPost = null
                            }
                        }
                        confirmDeleteVisible = false
                        pendingDeletePost = null
                    }) { Text("Move to trash") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteVisible = false; pendingDeletePost = null }) { Text("Cancel") }
                }
            )
        }

        if (pendingReportPost != null) {
            val target = pendingReportPost!!
            AlertDialog(
                onDismissRequest = { pendingReportPost = null },
                title = { Text("Report Post", color = HWhite) },
                text = { Text("Are you sure you want to report this post? We will review it within 24 hours.", color = HGray4) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.reportPost(target.id)
                        pendingReportPost = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Post reported. Thank you!") }
                    }) {
                        Text("Report", color = HRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingReportPost = null }) {
                        Text("Cancel", color = HWhite)
                    }
                },
                containerColor = HCard
            )
        }
        if (pendingReportPost != null) {
            val target = pendingReportPost!!
            AlertDialog(
                onDismissRequest = { pendingReportPost = null },
                title = { Text("Report Post", color = HWhite) },
                text = { Text("Are you sure you want to report this post? We will review it within 24 hours.", color = HGray4) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.reportPost(target.id)
                        pendingReportPost = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Post reported. Thank you!") }
                    }) {
                        Text("Report", color = HRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingReportPost = null }) {
                        Text("Cancel", color = HWhite)
                    }
                },
                containerColor = HCard
            )
        }

        if (pendingBlockPost != null) {
            val target = pendingBlockPost!!
            AlertDialog(
                onDismissRequest = { pendingBlockPost = null },
                title = { Text("Block ${target.author}?", color = HWhite) },
                text = { Text("You will no longer see posts or receive messages from this user.", color = HGray4) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.blockUser(target.authorId)
                        pendingBlockPost = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Blocked ${target.author}") }
                    }) {
                        Text("Block", color = HRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingBlockPost = null }) {
                        Text("Cancel", color = HWhite)
                    }
                },
                containerColor = HCard
            )
        }

        if (showCreateStoryDialog) {
            FullCreateStoryScreen(
                onDismiss = { showCreateStoryDialog = false },
                onStoryCreated = { showCreateStoryDialog = false },
                entryMode = StoryEntryMode.STORY,
                viewModel = chatViewModel,
            )
        }

        val activeStory = chatListState.stories.firstOrNull { it.id == selectedStoryId }
        if (activeStory != null) {
            val userStories = remember(chatListState.stories, activeStory.ownerId) {
                chatListState.stories.filter { 
                    it.ownerId == activeStory.ownerId && (it.storyType == "image" || it.storyType == "video")
                }
            }
            StoryViewerOverlay(
                stories = userStories,
                startStoryId = activeStory.id,
                viewModel = chatViewModel,
                onDismiss = { selectedStoryId = null },
            )
        }
        if (pendingBlockPost != null) {
            val target = pendingBlockPost!!
            AlertDialog(
                onDismissRequest = { pendingBlockPost = null },
                title = { Text("Block ${target.author}?", color = HWhite) },
                text = { Text("You will no longer see posts or receive messages from this user.", color = HGray4) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.blockUser(target.authorId)
                        pendingBlockPost = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Blocked ${target.author}") }
                    }) {
                        Text("Block", color = HRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingBlockPost = null }) {
                        Text("Cancel", color = HWhite)
                    }
                },
                containerColor = HCard
            )
        }

        pendingSharePost?.let { post ->
            ShareComposerDialog(
                originalPost = post,
                onDismiss = { pendingSharePost = null },
                onShareNow = { draft ->
                    val finalText = buildString {
                        if (draft.caption.isNotBlank()) {
                            append(draft.caption)
                        }
                        if (post.content.isNotBlank()) {
                            if (isNotBlank()) append("\n\n")
                            append("Shared from ")
                            append(post.author)
                            append("\n")
                            append(post.content)
                        }
                    }
                    val result = vm.addPost(
                        text = finalText,
                        mediaUris = draft.mediaUris,
                        mediaTypes = draft.mediaTypes,
                        visibility = draft.visibility,
                        taggedPeople = draft.taggedPeople,
                        sharedOriginalPostId = post.id,
                        sharedOriginalAuthor = post.author,
                        sharedOriginalAuthorId = post.authorId,
                        sharedOriginalAvatar = post.avatar,
                        sharedOriginalProfileImageUrl = post.profileImageUrl,
                        sharedOriginalTime = post.time,
                        sharedOriginalTimestamp = post.timestamp,
                        sharedOriginalContent = post.content,
                        sharedOriginalMediaUris = post.mediaUris,
                        sharedOriginalMediaTypes = post.mediaTypes,
                        sharedOriginalMediaEmojis = post.mediaEmojis,
                        sharedOriginalLikes = post.likes,
                        sharedOriginalComments = post.comments,
                        sharedOriginalShares = post.shares,
                        sharedOriginalVisibility = post.visibility,
                        sharedOriginalIsVerified = post.isVerified,
                    )
                    result.onSuccess {
                        vm.incrementShareCount(post.id)
                        ActivityLogger.logAction(
                            type = "share_post",
                            text = "Shared post by ${post.author}",
                            metadata = mapOf("postId" to post.id.toString(), "author" to post.author),
                        )
                    }
                    result
                },
            )
        }

        if (showCreateStoryDialog) {
            FullCreateStoryScreen(
                onDismiss = { showCreateStoryDialog = false },
                onStoryCreated = { showCreateStoryDialog = false },
                entryMode = StoryEntryMode.STORY,
                viewModel = chatViewModel,
            )
        }

        val activeStory = chatListState.stories.firstOrNull { it.id == selectedStoryId }
        if (activeStory != null) {
            val userStories = remember(chatListState.stories, activeStory.ownerId) {
                chatListState.stories.filter { 
                    it.ownerId == activeStory.ownerId && (it.storyType == "image" || it.storyType == "video")
                }
            }
            StoryViewerOverlay(
                stories = userStories,
                startStoryId = activeStory.id,
                viewModel = chatViewModel,
                onDismiss = { selectedStoryId = null },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(
    notifCount    : Int,
    onNotifClick  : () -> Unit,
    onSearchClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(HBlue, HGlow),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end   = androidx.compose.ui.geometry.Offset(80f, 80f),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎓", fontSize = 20.sp)
            }
            Text(
                "KAMPUS",
                color         = HWhite,
                fontSize      = 21.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(HCard)
                    .border(1.dp, HBorder, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSearchClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Search, "Search", tint = HGray4, modifier = Modifier.size(20.dp))
            }

            Box(modifier = Modifier.size(42.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HCard)
                        .border(
                            1.dp,
                            if (notifCount > 0) HBlue.copy(0.45f) else HBorder,
                            CircleShape,
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNotifClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (notifCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        "Notifications",
                        tint     = if (notifCount > 0) HBlue else HGray4,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (notifCount > 0) {
                    val badgeText = if (notifCount > 99) "99+" else "$notifCount"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .clip(CircleShape)
                            .background(HRed)
                            .border(1.5.dp, HBg, CircleShape)
                            .padding(horizontal = if (badgeText.length > 1) 4.dp else 0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badgeText,
                            color = HWhite,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Friends / Followers Row  (real-time + real profiles)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoriesRow(
    following: List<FriendUserItem>,
    currentUserProfileImageUrl: String,
    currentUserAvatarEmoji: String,
    friendsAndFollowers: List<FriendUserItem> = emptyList(),
) {
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // “You” bubble — always first
        item {
            val myStories = imageStoriesByOwner[currentUserId].orEmpty()
            val hasActiveStories = myStories.isNotEmpty()
            MeBubble(
                profileImageUrl = currentUserProfileImageUrl,
                avatarEmoji     = currentUserAvatarEmoji,
            )
        }

        if (friendsAndFollowers.isNotEmpty()) {
            // Real Firestore friends / followers
            items(friendsAndFollowers, key = { it.userId }) { friend ->
                FriendBubble(friend = friend)
            }
        } else {
            // Fallback: story data while Firestore loads
            items(stories) { story ->
                StoryBubble(story)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Me Bubble  (“You” with animated glow + plus button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MeBubble(
    profileImageUrl : String,
    avatarEmoji     : String,
    hasActiveStories: Boolean,
    onClick         : () -> Unit,
    onCreateClick   : () -> Unit,
) {
    // Subtle pulsing glow on the ring
    val infiniteTransition = rememberInfiniteTransition(label = "me_ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue    = 0.60f,
        targetValue     = 1.00f,
        animationSpec   = infiniteRepeatable(
            animation   = tween(1400, easing = FastOutSlowInEasing),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "ring_alpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Glowing ring (orange/purple sweep gradient if hasActiveStories, else standard blue)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasActiveStories) {
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFF97316).copy(alpha = ringAlpha),
                                    Color(0xFFEC4899).copy(alpha = ringAlpha),
                                    Color(0xFF8B5CF6).copy(alpha = ringAlpha),
                                    Color(0xFFF97316).copy(alpha = ringAlpha)
                                )
                            )
                        } else {
                            Brush.sweepGradient(
                                listOf(
                                    HBlue.copy(alpha = ringAlpha),
                                    Color(0xFF93C5FD).copy(alpha = ringAlpha),
                                    HGlow.copy(alpha = ringAlpha),
                                    HBlue.copy(alpha = ringAlpha),
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Inner avatar
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(HCard)
                        .border(2.5.dp, HBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model              = profileImageUrl,
                            contentDescription = "Your profile",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(
                            text     = avatarEmoji.ifBlank { "👤" },
                            fontSize = 28.sp,
                        )
                    }
                }
            }
            // Blue “+” badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(HBlue, HBlue.copy(0.75f)))
                    )
                    .border(2.dp, HBg, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onCreateClick
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = null,
                    tint               = HWhite,
                    modifier           = Modifier.size(13.dp),
                )
            }
        }

        Text(
            text       = "You",
            color      = HWhite,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Friend Bubble  (real profile from Firestore, with online dot)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FriendBubble(
    friend: FriendUserItem,
    hasActiveStories: Boolean,
    onClick: () -> Unit,
) {
    // Pop-in scale on first composition
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "bubble_scale",
    )
    LaunchedEffect(friend.userId) { visible = true }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Outer ring — sweep gradient (orange/purple gradient when active stories exist, blue when online, gray when offline)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasActiveStories)
                            Brush.sweepGradient(
                                listOf(Color(0xFFF97316), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFFF97316))
                            )
                        else if (friend.isOnline)
                            Brush.sweepGradient(
                                listOf(HBlue, Color(0xFF93C5FD), HGlow, HBlue)
                            )
                        else
                            Brush.sweepGradient(
                                listOf(HGray6.copy(0.55f), HGray6.copy(0.55f))
                            )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Inner avatar
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(HChipBg)
                        .border(2.5.dp, HBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (friend.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model              = friend.profileImageUrl,
                            contentDescription = friend.name,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(
                            text     = friend.avatarEmoji.ifBlank { "👤" },
                            fontSize = 28.sp,
                        )
                    }
                }
            }

            // Green online indicator
            if (friend.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, HBg, CircleShape),
                )
            }
        }

        Text(
            text       = friend.name.split(" ").firstOrNull() ?: friend.name,
            color      = HGray2,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
    }
}



@Composable
private fun RealtimeAvatar(
    userId: String,
    fallbackAvatar: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var profileImageUrl by remember(userId) { mutableStateOf("") }
    var avatarEmoji by remember(userId) { mutableStateOf(fallbackAvatar) }

    DisposableEffect(userId) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (userId.isNotBlank()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            listener = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        profileImageUrl = snapshot.getString("profileImageUrl").orEmpty()
                        val emoji = snapshot.getString("avatarEmoji") ?: snapshot.getString("avatar")
                        if (!emoji.isNullOrBlank()) {
                            avatarEmoji = emoji
                        }
                    }
                }
        }
        onDispose {
            listener?.remove()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(HBlue.copy(0.3f), HGray6.copy(0.45f))))
            .border(1.5.dp, HBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(avatarEmoji.ifBlank { "👤" }, fontSize = (size.value * 0.45f).sp)
        }
    }
}

@Composable
private fun RealtimeAvatar(
    userId: String,
    fallbackAvatar: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    var profileImageUrl by remember(userId) { mutableStateOf("") }
    var avatarEmoji by remember(userId) { mutableStateOf(fallbackAvatar) }

    DisposableEffect(userId) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (userId.isNotBlank()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            listener = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        profileImageUrl = snapshot.getString("profileImageUrl").orEmpty()
                        val emoji = snapshot.getString("avatarEmoji") ?: snapshot.getString("avatar")
                        if (!emoji.isNullOrBlank()) {
                            avatarEmoji = emoji
                        }
                    }
                }
        }
        onDispose {
            listener?.remove()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(HBlue.copy(0.3f), HGray6.copy(0.45f))))
            .border(1.5.dp, HBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(avatarEmoji.ifBlank { "👤" }, fontSize = (size.value * 0.45f).sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Post Card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCard(
    post: PostItem,
    isLiked: Boolean,
    isSaved: Boolean,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit = {},
    onOpenSharedPost: (Int) -> Unit = {},
    onMenuAction: (PostItem, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val likeScale by animateFloatAsState(
        if (isLiked) 1.35f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ls",
    )
    val likeColor by animateColorAsState(
        if (isLiked) HRed else HGray4, tween(200), label = "lc",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(HCard)
            .border(1.dp, HBorder, RoundedCornerShape(22.dp)),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RealtimeAvatar(
                    userId = post.authorId,
                    fallbackAvatar = post.avatar,
                    size = 44.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(post.author, color = HWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (post.isVerified) {
                            Icon(Icons.Default.Verified, "Verified", tint = HBlue, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text(formatPostTime(post), color = HGray4, fontSize = 12.sp)
                }
            }
            var showMenu by rememberSaveable { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val coroutineScope = rememberCoroutineScope()

            // Helper function to close menu properly
            fun closeMenu() {
                coroutineScope.launch { sheetState.hide() }
                showMenu = false
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showMenu = true
                        coroutineScope.launch { sheetState.show() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MoreVert, "More", tint = HGray4, modifier = Modifier.size(18.dp))

                if (showMenu) {
                    ModalBottomSheet(
                        onDismissRequest = { closeMenu() },
                        sheetState = sheetState,
                    ) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                        ) {
                            // Drag handle
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HBorder.copy(alpha = 0.35f))
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = HCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text("Post Options", color = HWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp, top = 6.dp, end = 18.dp, bottom = 2.dp))
                                    Text("Choose how you want to interact with this post.", color = HGray4, fontSize = 13.sp, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp))

                                    MenuItemLarge(
                                        icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        label = if (isSaved) "Unsave Post" else "Save Post",
                                        tint = HBlue,
                                        subtitle = if (isSaved) "Remove from saved items" else "Save this post to your profile"
                                    ) {
                                        onMenuAction(post, if (isSaved) "unsave" else "save")
                                        closeMenu()
                                    }

                                    HorizontalDivider(color = HBorder.copy(alpha = 0.5f))

                                    MenuItemLarge(icon = Icons.Outlined.Share, label = "Share", tint = HGray2, subtitle = "Share this post with others") {
                                        onMenuAction(post, "share")
                                        closeMenu()
                                    }
                                    MenuItemLarge(icon = Icons.Outlined.Link, label = "Copy Link", tint = HGray2, subtitle = "Copy post link to clipboard") {
                                        onMenuAction(post, "copy_link")
                                        closeMenu()
                                    }

                                    HorizontalDivider(color = HBorder.copy(alpha = 0.5f))

                                    MenuItemLarge(icon = Icons.Outlined.SentimentDissatisfied, label = "Not Interested", tint = HGray2, subtitle = "Show fewer posts like this") {
                                        onMenuAction(post, "not_interested")
                                        closeMenu()
                                    }
                                    MenuItemLarge(icon = Icons.Outlined.VisibilityOff, label = "Hide Post", tint = HGray2, subtitle = "Hide this post from your feed") {
                                        onMenuAction(post, "hide_post")
                                        closeMenu()
                                    }
                                    MenuItemLarge(
                                        icon = Icons.AutoMirrored.Outlined.VolumeOff,
                                        label = "Mute User",
                                        tint = HGray2,
                                        subtitle = "Stop seeing posts from ${post.author}"
                                    ) {
                                        onMenuAction(post, "mute_user")
                                        closeMenu()
                                    }

                                    HorizontalDivider(color = HBorder.copy(alpha = 0.5f))

                                    MenuItemLarge(icon = Icons.Outlined.Flag, label = "Report Post", tint = HRed, subtitle = "Report if this post violates guidelines") {
                                        onMenuAction(post, "report")
                                        closeMenu()
                                    }
                                    MenuItemLarge(icon = Icons.Outlined.Block, label = "Block User", tint = HRed, subtitle = "Block ${post.author} and their posts") {
                                        onMenuAction(post, "block")
                                        closeMenu()
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }
        }

        // Display multiple media items in a horizontal scrollable gallery
        if (post.mediaUris.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HNavBg)
                    .border(1.dp, HBorder, RoundedCornerShape(18.dp)),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(post.mediaUris.size) { index ->
                    val mediaUri = post.mediaUris[index]
                    val mediaType = post.mediaTypes.getOrNull(index) ?: PostItem.MediaType.IMAGE

                    if (mediaType == PostItem.MediaType.VIDEO) {
                        // Tap-to-play thumbnail: no ExoPlayer created on compose
                        var videoPlaying by remember(mediaUri) { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(220.dp)
                                .fillParentMaxHeight()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    videoPlaying = true
                                },
                        ) {
                            if (videoPlaying) {
                                val player = remember(mediaUri) {
                                    ExoPlayer.Builder(context).build().apply {
                                        setMediaItem(MediaItem.fromUri(mediaUri))
                                        prepare()
                                        playWhenReady = true
                                    }
                                }
                                DisposableEffect(player) { onDispose { player.release() } }
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
                                // Thumbnail preview
                                AsyncImage(
                                    model = mediaUri,
                                    contentDescription = "Video thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)),
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.Center)
                                        .size(52.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(30.dp))
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(220.dp)
                                .fillParentMaxHeight()
                        ) {
                            AsyncImage(
                                model = mediaUri,
                                contentDescription = "Post media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        } else if (isLegacyVideoPost(post)) {
            // Backward compatibility: single media from old model
            @Suppress("DEPRECATION")
            val imageUri = post.imageUri!!
            Spacer(Modifier.height(6.dp))
            // Tap-to-play thumbnail: no ExoPlayer until user taps
            var legacyVideoPlaying by remember(imageUri) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HNavBg)
                    .border(1.dp, HBorder, RoundedCornerShape(18.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        legacyVideoPlaying = true
                    },
            ) {
                if (legacyVideoPlaying) {
                    val player = remember(imageUri) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(imageUri))
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(player) { onDispose { player.release() } }
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
                        model = imageUri,
                        contentDescription = "Video thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                            .size(52.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        } else if (isLegacyImagePost(post)) {
            // Backward compatibility: single image from old model
            @Suppress("DEPRECATION")
            val imageUri = post.imageUri!!
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HNavBg)
                    .border(1.dp, HBorder, RoundedCornerShape(18.dp)),
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Post image",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        val imageEmoji = getLegacyImageEmoji(post)
        if (imageEmoji != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(HGray6.copy(0.28f), Color(0xFF050810)))),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(imageEmoji, fontSize = 80.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF050810).copy(0.92f))))
                )
                Row(
                    modifier              = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    ImageAction(
                        icon    = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        label   = "${post.likes}",
                        tint    = likeColor, scale = likeScale, onClick = onLike,
                    )
                    ImageAction(Icons.Outlined.ChatBubbleOutline, "${post.comments}", HWhite.copy(0.8f))
                    ImageAction(Icons.AutoMirrored.Outlined.Send, "", HWhite.copy(0.8f), onClick = onShare)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                post.content, color = HGray2, fontSize = 14.sp, lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(Modifier.height(14.dp))
        } else {
            // For shared posts: strip the "Shared from X\n" prefix from content
            // so the sharer's caption is shown cleanly above the embedded card.
            val displayContent = if (post.sharedOriginalPostId != null) {
                val sharedPrefix = "Shared from ${post.sharedOriginalAuthor}"
                val stripped = post.content
                    .substringBefore("\n\n$sharedPrefix")
                    .substringBefore("\n$sharedPrefix")
                    .trim()
                stripped
            } else {
                post.content
            }

            if (displayContent.isNotBlank()) {
                Text(
                    displayContent, color = HGray2, fontSize = 15.sp, lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }

            val metadataLines = postMetadataLines(post)
            if (metadataLines.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    metadataLines.forEach { line ->
                        Text(line, color = HGray4, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }

            if (post.sharedOriginalPostId != null) {
                Spacer(Modifier.height(10.dp))
                SharedOriginalCard(
                    author = post.sharedOriginalAuthor ?: "Unknown",
                    authorId = post.sharedOriginalAuthorId.orEmpty(),
                    avatar = post.sharedOriginalAvatar ?: post.avatar,
                    profileImageUrl = post.sharedOriginalProfileImageUrl.orEmpty(),
                    time = post.sharedOriginalTime ?: "now",
                    content = post.sharedOriginalContent.orEmpty(),
                    mediaUris = post.sharedOriginalMediaUris,
                    mediaTypes = post.sharedOriginalMediaTypes,
                    mediaEmojis = post.sharedOriginalMediaEmojis,
                    likes = post.sharedOriginalLikes ?: 0,
                    comments = post.sharedOriginalComments ?: 0,
                    shares = post.sharedOriginalShares ?: 0,
                    isVerified = post.sharedOriginalIsVerified ?: false,
                    onLike = { onLike() },
                    onComment = { onComment() },
                    onShare = { onShare() },
                    onClick = { onOpenSharedPost(post.sharedOriginalPostId) },
                )
                Spacer(Modifier.height(6.dp))
            } else {
                Spacer(Modifier.height(10.dp))
            }

            HorizontalDivider(color = HBorder.copy(0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                TextAction(
                    icon    = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label   = "${post.likes}",
                    tint    = likeColor, scale = likeScale, onClick = onLike,
                )
                TextAction(Icons.Outlined.ChatBubbleOutline, "${post.comments}", HGray4, onClick = onComment)
                TextAction(Icons.AutoMirrored.Outlined.Send, "${post.shares}", HGray4, onClick = onShare)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SharedOriginalCard(
    author: String,
    authorId: String,
    avatar: String,
    profileImageUrl: String,
    time: String,
    content: String,
    mediaUris: List<android.net.Uri>,
    mediaTypes: List<PostItem.MediaType>,
    mediaEmojis: List<String>,
    likes: Int,
    comments: Int,
    shares: Int,
    isVerified: Boolean,
    onLike: () -> Unit = {},
    onComment: () -> Unit = {},
    onShare: () -> Unit = {},
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    // Optimistic local like state
    var localLikes by remember(likes) { mutableIntStateOf(likes) }
    var isLocalLiked by remember { mutableStateOf(false) }
    val likeIconScale by animateFloatAsState(
        targetValue = if (isLocalLiked) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fb_like_scale",
    )
    val likeIconColor by animateColorAsState(
        targetValue = if (isLocalLiked) Color(0xFF2196F3) else HGray4,
        animationSpec = tween(180),
        label = "fb_like_color",
    )

    // Card press animation
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.988f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fb_card_scale",
    )

    // Match Facebook's dark embedded card color
    val cardBg = if (UiIsDark) Color(0xFF1C1E22) else Color(0xFFF2F3F5)
    val cardBorder = if (UiIsDark) Color(0xFF3A3B3C) else Color(0xFFCDD0D5)

    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RealtimeAvatar(userId = authorId, fallbackAvatar = avatar, size = 36.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        author,
                        color = HWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isVerified) {
                        Icon(Icons.Default.Verified, "Verified", tint = HBlue, modifier = Modifier.size(13.dp))
                    }
                }
                Text(time, color = HGray4, fontSize = 11.sp)
            }
        }

        // ── Caption ───────────────────────────────────────────────────────────
        if (content.isNotBlank()) {
            Text(
                content,
                color = HGray2,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // ── Media — edge-to-edge (no horizontal padding) ──────────────────────
        when {
            mediaUris.isNotEmpty() -> {
                val firstUri = mediaUris.first()
                val firstType = mediaTypes.getOrNull(0) ?: PostItem.MediaType.IMAGE
                Spacer(Modifier.height(6.dp))
                if (firstType == PostItem.MediaType.VIDEO) {
                    // Show a lightweight video thumbnail with play button.
                    // NO ExoPlayer created here — avoids blocking the main thread.
                    // Tapping the card opens the full post where video plays.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF0D0D0D)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Try to show a thumbnail frame via AsyncImage (works for remote URLs)
                        AsyncImage(
                            model = firstUri,
                            contentDescription = "Video thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        // Dark overlay + play button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        if (mediaUris.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text("+${mediaUris.size - 1} more", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    if (mediaUris.size == 1) {
                        // Single image: full bleed, edge-to-edge
                        AsyncImage(
                            model = firstUri,
                            contentDescription = "Shared original media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 300.dp),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                            Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                AsyncImage(
                                    model = firstUri,
                                    contentDescription = "Shared media 1",
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AsyncImage(
                                        model = mediaUris[1],
                                        contentDescription = "Shared media 2",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    // "+N more" overlay for 3+ images
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
                                                fontSize = 22.sp,
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
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(HGray6.copy(0.2f), Color(0xFF050810).copy(0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(mediaEmojis.joinToString(" "), fontSize = 48.sp)
                }
            }
        }

        // ── Bottom action bar — Facebook style ────────────────────────────────
        HorizontalDivider(color = cardBorder.copy(alpha = 0.7f), thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LEFT: 👍 count + comment bubble + forward arrow
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Thumbs-up + count
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            isLocalLiked = !isLocalLiked
                            localLikes = if (isLocalLiked) localLikes + 1 else (localLikes - 1).coerceAtLeast(0)
                            onLike()
                        }
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isLocalLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        "Like",
                        tint = likeIconColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { scaleX = likeIconScale; scaleY = likeIconScale },
                    )
                    if (localLikes > 0) {
                        Text("$localLikes", color = likeIconColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Chat bubble — comment
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onComment),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, "Comment", tint = HGray4, modifier = Modifier.size(20.dp))
                }

                // Forward / share arrow
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onShare),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, "Share", tint = HGray4, modifier = Modifier.size(19.dp))
                }
            }

            // RIGHT: stacked reaction emoji bubbles (👍 ❤️ 😆)
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                listOf("", "", "").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (UiIsDark) Color(0xFF3A3B3C) else Color(0xFFE4E6EB))
                            .border(1.5.dp, cardBg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, tint: Color, subtitle: String? = null, onClick: () -> Unit = {}) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
            Column {
                Text(label, color = HWhite, fontWeight = FontWeight.Medium)
                if (!subtitle.isNullOrEmpty()) {
                    Text(subtitle, color = HGray4, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuItemLarge(icon: ImageVector, label: String, tint: Color, subtitle: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HNavBg)
                .border(1.dp, HBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(label, color = HWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, color = HGray4, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ImageAction(icon: ImageVector, label: String, tint: Color, scale: Float = 1f, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        if (label.isNotEmpty()) Text(label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TextAction(icon: ImageVector, label: String, tint: Color, scale: Float = 1f, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(18.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        if (label.isNotEmpty()) Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Nav
// ─────────────────────────────────────────────────────────────────────────────
@Composable
// ─────────────────────────────────────────────────────────────────────────────
// Legacy/Deprecated property helpers (for backward compatibility)
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
private fun isLegacyVideoPost(post: PostItem): Boolean {
    return post.imageUri != null && post.mediaType == PostItem.MediaType.VIDEO
}

@Suppress("DEPRECATION")
private fun isLegacyImagePost(post: PostItem): Boolean {
    return post.imageUri != null
}

@Suppress("DEPRECATION")
private fun getLegacyImageEmoji(post: PostItem): String? {
    return post.imageEmoji
}