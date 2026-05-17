@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.feed
import android.content.Intent
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.feed.MenuItemLarge
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

// ─────────────────────────────────────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────────────────────────────────────
data class StoryItem(
    val name   : String,
    val emoji  : String,
    val isMe   : Boolean = false,
    val hasNew : Boolean = true,
)

private data class NavItem(
    val label        : String,
    val icon         : ImageVector,
    val iconSelected : ImageVector,
)

// ─────────────────────────────────────────────────────────────────────────────
// Static data
// ─────────────────────────────────────────────────────────────────────────────
private val stories = listOf(
    StoryItem("You",      "🧑‍💻", isMe = true, hasNew = false),
    StoryItem("Jacob",    "👨‍🎓", hasNew = true),
    StoryItem("Luna",     "👩‍🎨", hasNew = true),
    StoryItem("John",     "🧑‍🔬", hasNew = false),
    StoryItem("Mia",      "👩‍💻", hasNew = true),
    StoryItem("Netaliya", "👩‍🎤", hasNew = true),
    StoryItem("Carlos",   "🧑‍🏫", hasNew = false),
)

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
    onGroupsClick  : () -> Unit = {},   // ← ADDED
    onEventsClick  : () -> Unit = {},   // ← ADDED (for future use)
    onChatClick    : () -> Unit = {},   // ← ADDED (for future use)
) {
    val vm: FeedViewModel = viewModel()
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val likedPosts by vm.likedIds.collectAsState()
    val posts = uiState.posts

    var selectedNav by remember { mutableIntStateOf(0) }
    var notifCount  by remember { mutableIntStateOf(3) }
    val listState   = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pendingDeletePost by remember { mutableStateOf<PostItem?>(null) }
    var confirmDeleteVisible by rememberSaveable { mutableStateOf(false) }
    var recentlyDeletedPost by remember { mutableStateOf<PostItem?>(null) }

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
                    notifCount    = notifCount,
                    onNotifClick  = { notifCount = 0; onNotifClick() },
                    onSearchClick = onSearchClick,
                )
            }
        },
        bottomBar = {
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
                    selectedIndex  = selectedNav,
                    onItemSelected = { index ->
                        selectedNav = index
                        when (index) {
                            0 -> { /* already on Home, do nothing */ }
                            1 -> onGroupsClick()
                            2 -> onEventsClick()
                            3 -> onChatClick()
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
                StoriesRow()
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
                    isLiked = post.id in likedPosts,
                    onLike  = {
                        vm.toggleLike(post.id)
                    },
                    onShare = {
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
                            else -> {
                                ActivityLogger.logAction(type = "post_menu_action", text = "$action for ${targetPost.id}")
                            }
                        }
                    },
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
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .align(Alignment.TopEnd)
                            .offset(2.dp, (-2).dp)
                            .clip(CircleShape)
                            .background(HRed)
                            .border(1.5.dp, HBg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$notifCount", color = HWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stories
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StoriesRow() {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(stories) { StoryBubble(it) }
    }
}

@Composable
private fun StoryBubble(story: StoryItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier            = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Box(modifier = Modifier.size(66.dp)) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(
                        if (story.hasNew)
                            Brush.sweepGradient(listOf(HBlue, Color(0xFF93C5FD), HGlow, HBlue))
                        else
                            Brush.sweepGradient(listOf(HGray6.copy(0.6f), HGray6.copy(0.6f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(59.dp)
                        .clip(CircleShape)
                        .background(HCard)
                        .border(2.5.dp, HBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(story.emoji, fontSize = 26.sp)
                }
            }
            if (story.isMe) {
                Box(
                    modifier = Modifier
                        .size(21.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(HBlue)
                        .border(2.dp, HBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, "Add", tint = HWhite, modifier = Modifier.size(13.dp))
                }
            }
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
    onLike: () -> Unit,
    onShare: () -> Unit,
    onMenuAction: (PostItem, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(HBlue.copy(0.3f), HGray6.copy(0.45f))))
                        .border(1.5.dp, HBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(post.avatar, fontSize = 20.sp)
                }
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
                    Text(post.time, color = HGray4, fontSize = 12.sp)
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

                            // Top group (main post actions)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = HCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text("Post options", color = HWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp, top = 6.dp, end = 18.dp, bottom = 2.dp))
                                    Text("Choose what to do with this post.", color = HGray4, fontSize = 13.sp, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp))

                                    MenuItemLarge(icon = Icons.Outlined.OpenInNew, label = "Open post", tint = HBlue) {
                                        onMenuAction(post, "open")
                                        closeMenu()
                                    }
                                    MenuItemLarge(icon = Icons.Outlined.ContentCopy, label = "Copy post text", tint = HGray2) {
                                        onMenuAction(post, "copy")
                                        closeMenu()
                                    }

                                    Divider(color = HBorder.copy(alpha = 0.5f))

                                    MenuItemLarge(icon = Icons.Default.BookmarkAdd, label = if (post.isPinned) "Unpin post" else "Pin post", tint = HBlue, subtitle = "Keep this post at the top") {
                                        onMenuAction(post, if (post.isPinned) "unpin" else "pin")
                                        closeMenu()
                                    }
                                    MenuItemLarge(icon = Icons.Default.Edit, label = "Edit post", tint = HGray2, subtitle = "Update the content or media") {
                                        onMenuAction(post, "edit")
                                        closeMenu()
                                    }
                                    MenuItemLarge(icon = Icons.Default.Lock, label = "Privacy settings", tint = HGray2, subtitle = "Choose who can see this post") {
                                        onMenuAction(post, "privacy")
                                        closeMenu()
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Privacy presets
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = HCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    MenuItemLarge(icon = Icons.Outlined.Public, label = "Public", tint = Color(0xFF4CAF50), subtitle = "Anyone can see this post") {
                                        onMenuAction(post, "privacy_public")
                                        closeMenu()
                                    }
                                    Divider(color = HBorder.copy(alpha = 0.5f))
                                    MenuItemLarge(icon = Icons.Outlined.Group, label = "Friends", tint = HBlue, subtitle = "Only your friends can see it") {
                                        onMenuAction(post, "privacy_friends")
                                        closeMenu()
                                    }
                                    Divider(color = HBorder.copy(alpha = 0.5f))
                                    MenuItemLarge(icon = Icons.Outlined.Lock, label = "Only me", tint = HGray2, subtitle = "Visible only to you") {
                                        onMenuAction(post, "privacy_private")
                                        closeMenu()
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Destructive action
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = HCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    MenuItemLarge(icon = Icons.Default.PersonOff, label = "Hide from profile", tint = HGray2, subtitle = "Remove it from your profile only") {
                                        onMenuAction(post, "hide_profile")
                                        closeMenu()
                                    }
                                    Divider(color = HBorder.copy(alpha = 0.5f))
                                    MenuItemLarge(icon = Icons.Default.Delete, label = "Delete post", tint = HRed, subtitle = "This removes the post from the feed") {
                                        onMenuAction(post, "trash")
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
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(220.dp)
                                .fillParentMaxHeight()
                        ) {
                            val player = remember(mediaUri) {
                                ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(mediaUri))
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
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HNavBg)
                    .border(1.dp, HBorder, RoundedCornerShape(18.dp)),
            ) {
                val player = remember(imageUri) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(imageUri))
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
                        label   = "${post.likes + if (isLiked) 1 else 0}",
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
            Text(
                post.content, color = HGray2, fontSize = 15.sp, lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = HBorder.copy(0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                TextAction(
                    icon    = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label   = "${post.likes + if (isLiked) 1 else 0}",
                    tint    = likeColor, scale = likeScale, onClick = onLike,
                )
                TextAction(Icons.Outlined.ChatBubbleOutline, "${post.comments}", HGray4)
                TextAction(Icons.AutoMirrored.Outlined.Send, "Share", HGray4, onClick = onShare)
            }
            Spacer(Modifier.height(4.dp))
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