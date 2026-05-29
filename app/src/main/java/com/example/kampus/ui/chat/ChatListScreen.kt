@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.chat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.geometry.Offset
import com.example.kampus.ui.feed.FeedViewModel
import com.example.kampus.ui.feed.FriendUserItem
import com.example.kampus.ui.localization.rememberUiStrings
import com.example.kampus.ui.theme.ThemeController
import com.example.kampus.ui.chat.ChatViewModel
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.chat.StoryEntryMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Palette — identical to HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val UiIsDark get() = ThemeController.isDark
private val HBg      get() = if (UiIsDark) Color(0xFF080B11) else Color(0xFFF3F4F8)
private val HCard    get() = if (UiIsDark) Color(0xFF0F1520) else Color(0xFFFFFFFF)
private val HBorder  get() = if (UiIsDark) Color(0xFF1A2333) else Color(0xFFD1D5DB)
private val HBlue    get() = ThemeController.accent.color
private val HGlow    get() = HBlue.copy(alpha = if (UiIsDark) 0.75f else 0.55f)
private val HWhite   get() = if (UiIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val HGray2   get() = if (UiIsDark) Color(0xFFE5E7EB) else Color(0xFF374151)
private val HGray4   get() = if (UiIsDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val HGray6   get() = if (UiIsDark) Color(0xFF374151) else Color(0xFF9CA3AF)
private val HRed     get() = Color(0xFFEF4444)
private val HNavBg   get() = if (UiIsDark) Color(0xFF0C1018) else Color(0xFFFFFFFF)
val HSearch          get() = if (UiIsDark) Color(0xFF23262A) else Color(0xFFFFFFFF)
private val HSearchBorder get() = if (UiIsDark) HBorder.copy(alpha = 0.7f) else Color(0xFFD1D5DB)
private val HChipBg  get() = if (UiIsDark) Color(0xFF171A1F) else Color(0xFFE5E7EB)
private val HChipActive get() = HBlue

// ─────────────────────────────────────────────────────────────────────────────
// Nav items — same order as HomeScreen (Home / Groups / Events / Chat)
// ─────────────────────────────────────────────────────────────────────────────
private data class NavItem(
    val label        : String,
    val icon         : ImageVector,
    val iconSelected : ImageVector,
)

private val navItems = listOf(
    NavItem("Home",   Icons.Outlined.Home,              Icons.Filled.Home),
    NavItem("Groups", Icons.Outlined.Group,             Icons.Filled.Group),
    NavItem("Events", Icons.Outlined.CalendarMonth,     Icons.Filled.CalendarMonth),
    NavItem("Chat",   Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
)

private enum class ChatFilter {
    All,
    Unread,
    Groups,
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ChatListScreen(
    onChatClick    : (String) -> Unit,
    onHomeClick    : () -> Unit,
    onGroupsClick  : () -> Unit,
    onEventsClick  : () -> Unit,
    onAdminClick   : () -> Unit = {},
    onProfileClick : () -> Unit,
    onCreatePost   : () -> Unit = {},
    viewModel      : ChatViewModel = viewModel(),
) {
    val state      by viewModel.chatListState.collectAsStateWithLifecycle()
    val previewState by viewModel.chatPreviewState.collectAsStateWithLifecycle()
    val feedViewModel: FeedViewModel = viewModel()
    val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val strings = rememberUiStrings()
    var selectedFilter by remember { mutableStateOf(ChatFilter.All) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var selectedStoryId by remember { mutableStateOf<String?>(null) }

    val filteredChats = remember(state.chats, selectedFilter) {
        when (selectedFilter) {
            ChatFilter.All -> state.chats
            ChatFilter.Unread -> state.chats.filter { it.unreadCount > 0 }
            ChatFilter.Groups -> state.chats.filter { isGroupLikeChat(it) }
        }
    }

    Scaffold(
        containerColor = HBg,
        bottomBar = {
            if (!showCreateStoryDialog && selectedStoryId == null) {
                val currentUserRole = com.example.kampus.ui.components.rememberCurrentUserRole()
                val isAdmin = currentUserRole.equals("admin", ignoreCase = true)

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
                    val navItems = com.example.kampus.ui.components.rememberCampusNavItems(isAdmin)

                    CampusBottomNavBar(
                        selectedIndex  = 3,          // Chat tab is index 3
                        navItems       = navItems,
                        onItemSelected = { index ->
                            when {
                                isAdmin -> when (index) {
                                    0 -> onHomeClick()
                                    1 -> onGroupsClick()
                                    2 -> onAdminClick()
                                    3 -> { /* already here */ }
                                }
                                else -> when (index) {
                                    0 -> onHomeClick()
                                    1 -> onGroupsClick()
                                    2 -> onEventsClick()
                                    3 -> { /* already here */ }
                                }
                            }
                        },
                        onFabClick     = onCreatePost,
                        onProfileClick = onProfileClick,
                        isProfileSelected = false,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HBg)
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            ChatListHeader(unreadTotal = state.chats.count { it.unreadCount > 0 })

            ChatSearchBar(
                query         = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder   = strings.search,
            )

            ChatStoriesRow(
                stories = feedState.stories,
                friendsAndFollowers = feedState.friendsAndFollowers,
                currentUserProfileImageUrl = feedState.currentUserProfileImageUrl,
                currentUserAvatarEmoji = feedState.currentUserAvatarEmoji,
                onCreateNote = {
                    showCreateNoteDialog = true
                },
                onCreateStory = {
                    showCreateStoryDialog = true
                },
                onOpenStory = { selectedStoryId = it.id },
                currentVibeLabel = "Current vibe",
                onChatClick = onChatClick,
                viewModel = viewModel,
                chats = state.chats,
            )

            ChatFilters(
                selected = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                strings = strings
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp, top = 2.dp),
            ) {
                itemsIndexed(
                    items = filteredChats,
                    key = { _, chat -> chat.id },
                ) { index, chat ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(220, delayMillis = index * 25)) +
                            slideInVertically(
                                animationSpec = tween(220, delayMillis = index * 25, easing = EaseOutCubic),
                                initialOffsetY = { it / 4 },
                            ),
                        exit = fadeOut(tween(120)),
                    ) {
                        ChatRow(
                            chat    = chat,
                            onClick = { onChatClick(chat.id) },
                            onLongPress = { viewModel.openChatPreview(chat.id) },
                            onTogglePin = { viewModel.togglePinChat(chat.id, !chat.isPinned) },
                            strings = strings,
                            currentUserName = state.currentUserName,
                        )
                    }
                }
            }
        }

        if (showCreateStoryDialog) {
            FullCreateStoryScreen(
                onDismiss = { showCreateStoryDialog = false },
                onStoryCreated = { showCreateStoryDialog = false },
                entryMode = StoryEntryMode.STORY,
                viewModel = viewModel,
            )
        }

        if (showCreateNoteDialog) {
            FullCreateStoryScreen(
                onDismiss = { showCreateNoteDialog = false },
                onStoryCreated = { showCreateNoteDialog = false },
                entryMode = StoryEntryMode.NOTE,
                viewModel = viewModel,
            )
        }

        val activeStory = state.stories.firstOrNull { it.id == selectedStoryId }
        if (activeStory != null) {
            val userStories = remember(state.stories, activeStory.ownerId) {
                state.stories.filter { 
                    it.ownerId == activeStory.ownerId && (it.storyType == "image" || it.storyType == "video")
                }
            }
            StoryViewerOverlay(
                stories = userStories,
                startStoryId = activeStory.id,
                viewModel = viewModel,
                onDismiss = { selectedStoryId = null },
            )
        }

        if (previewState.chatId.isNotBlank()) {
            ChatPreviewOverlay(
                preview = previewState,
                onDismiss = viewModel::dismissChatPreview,
                onOpenChat = { chatId ->
                    viewModel.dismissChatPreview()
                    onChatClick(chatId)
                },
                onMarkUnread = viewModel::markChatAsUnread,
                onTogglePin = { chatId, isPinned -> viewModel.togglePinChat(chatId, isPinned) },
                onMute = { chatId, isMuted -> viewModel.toggleMuteChat(chatId, isMuted) },
                onArchive = { chatId, isArchived -> viewModel.toggleArchiveChat(chatId, isArchived) },
                onDelete = { chatId -> viewModel.deleteChat(chatId) },
                onBlock = { userId -> viewModel.blockUser(userId) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatListHeader(unreadTotal: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text          = "Messages",
            color         = HWhite,
            fontSize      = 28.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (unreadTotal > 0) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(HBlue.copy(alpha = 0.2f))
                        .border(1.dp, HBlue.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = if (unreadTotal > 99) "99+" else unreadTotal.toString(),
                        color = HBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(HChipBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HWhite,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatSearchBar(
    query         : String,
    onQueryChange : (String) -> Unit,
    placeholder   : String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(HSearch)
            .border(1.dp, HSearchBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Outlined.Search,
            contentDescription = null,
            tint               = HGray4,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        TextField(
            value         = query,
            onValueChange = onQueryChange,
            placeholder   = {
                Text(placeholder, color = HGray4, fontSize = 14.sp)
            },
            singleLine = true,
            colors     = TextFieldDefaults.colors(
                unfocusedContainerColor  = Color.Transparent,
                focusedContainerColor    = Color.Transparent,
                unfocusedIndicatorColor  = Color.Transparent,
                focusedIndicatorColor    = Color.Transparent,
                unfocusedTextColor       = HWhite,
                focusedTextColor         = HWhite,
                cursorColor              = HBlue,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MessengerSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (UiIsDark) Color(0xFF23262A) else Color(0xFFE5E7EB)
    val textColor = if (UiIsDark) Color(0xFFEFF3FF) else Color(0xFF111827)
    val borderColor = if (UiIsDark) Color(0xFF2D3748) else Color(0xFFD1D5DB)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        
        Canvas(
            modifier = Modifier
                .size(width = 8.dp, height = 4.dp)
                .offset(y = (-0.5).dp)
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(path = path, color = bubbleColor)
            
            // Border lines for the triangle tail
            drawLine(
                color = borderColor,
                start = Offset(0f, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = borderColor,
                start = Offset(size.width / 2, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun ChatStoriesRow(
    stories: List<ChatStory>,
    friendsAndFollowers: List<FriendUserItem>,
    currentUserProfileImageUrl: String,
    currentUserAvatarEmoji: String,
    onCreateNote: () -> Unit,
    onCreateStory: () -> Unit,
    onOpenStory: (ChatStory) -> Unit,
    currentVibeLabel: String,
    onChatClick: (String) -> Unit,
    viewModel: ChatViewModel,
    chats: List<ChatItem>,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    
    // Group stories/notes by owner
    val notesByOwner = remember(stories) {
        stories
            .filter { it.storyType == "note" }
            .groupBy { it.ownerId }
            .mapValues { (_, ownerStories) -> ownerStories.firstOrNull() }
    }
    val imageStoriesByOwner = remember(stories) {
        stories.filter { it.storyType == "image" }.groupBy { it.ownerId }
    }

    // Combine friendsAndFollowers and active chats, ensuring uniqueness
    val combinedFriends = remember(friendsAndFollowers, chats) {
        val list = mutableListOf<FriendUserItem>()
        friendsAndFollowers.forEach { friend ->
            if (friend.userId != currentUserId) {
                list.add(friend)
            }
        }
        chats.forEach { chat ->
            if (chat.otherUserId.isNotBlank() && chat.otherUserId != currentUserId && list.none { it.userId == chat.otherUserId }) {
                list.add(
                    FriendUserItem(
                        userId = chat.otherUserId,
                        name = chat.name,
                        avatarEmoji = chat.avatarEmoji,
                        profileImageUrl = chat.profileImageUrl,
                        isOnline = chat.isOnline
                    )
                )
            }
        }
        list
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // "You" item
        item {
            val myNote = notesByOwner[currentUserId]?.note.orEmpty()
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .width(80.dp)
                    .clickable(
                        onClick = onCreateNote,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            ) {
                // Speech bubble for "You"
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Show my note if set, else show "add status..."
                    MessengerSpeechBubble(
                        text = myNote.ifBlank { "add status..." }
                    )
                }
                
                Spacer(modifier = Modifier.height((-4).dp))
                
                // Avatar with blue border and blue plus sign
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(HBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(55.dp)
                                .clip(CircleShape)
                                .background(HBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(HChipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentUserProfileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = currentUserProfileImageUrl,
                                        contentDescription = "Your profile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(text = currentUserAvatarEmoji.ifBlank { "👤" }, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(HBlue)
                            .border(1.5.dp, HBg, CircleShape)
                            .clickable(
                                onClick = onCreateStory,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "You",
                    color = HWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Friends list
        items(combinedFriends, key = { it.userId }) { friend ->
            val note = notesByOwner[friend.userId]?.note.orEmpty()
            val hasImageStory = imageStoriesByOwner[friend.userId]?.isNotEmpty() == true
            val story = imageStoriesByOwner[friend.userId]?.firstOrNull()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .width(80.dp)
                    .clickable(
                        onClick = {
                            if (hasImageStory && story != null) {
                                onOpenStory(story)
                            } else {
                                coroutineScope.launch {
                                    val chatId = viewModel.getOrCreateDirectChatWithUser(friend.userId)
                                    if (chatId != null) {
                                        onChatClick(chatId)
                                    }
                                }
                            }
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            ) {
                // Speech bubble for friend (only show if note is not empty!)
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (note.isNotBlank()) {
                        MessengerSpeechBubble(text = note)
                    }
                }
                
                Spacer(modifier = Modifier.height((-4).dp))
                
                // Avatar with story ring / online indicator
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(60.dp)
                ) {
                    val storyGradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFFFF5722),
                            Color(0xFF9C27B0)
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasImageStory) storyGradient 
                                else if (friend.isOnline) Brush.sweepGradient(listOf(HBlue, Color(0xFF93C5FD), HGlow, HBlue))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (hasImageStory || friend.isOnline) 55.dp else 60.dp)
                                .clip(CircleShape)
                                .background(HBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (hasImageStory || friend.isOnline) 50.dp else 56.dp)
                                    .clip(CircleShape)
                                    .background(HChipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (friend.profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = friend.profileImageUrl,
                                        contentDescription = friend.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(text = friend.avatarEmoji.ifBlank { "👤" }, fontSize = 22.sp)
                                }
                            }
                        }
                    }

                    if (friend.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(1.5.dp, HBg, CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = friend.name.split(" ").firstOrNull() ?: friend.name,
                    color = HGray2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CurrentVibeBubble(
    profileImageUrl: String,
    avatarEmoji: String,
    onLabelClick: () -> Unit,
    onAvatarClick: () -> Unit,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(min = 76.dp, max = 96.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(HCard.copy(alpha = 0.92f))
                .border(1.dp, HBorder.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                .clickable(onClick = onLabelClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    color = HWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Create note",
                    color = HGray4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(HChipBg)
                    .border(2.dp, HBlue.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Your note",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape).clickable(onClick = onAvatarClick),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = avatarEmoji.ifBlank { "👤" },
                            color = HWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(HBlue)
                    .border(2.dp, HBg, CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = HWhite,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Text(
            text = "You",
            color = HWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FriendNoteBubble(
    friend: FriendUserItem,
    note: String,
    hasStory: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .widthIn(min = 78.dp, max = 98.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF2B2F35))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = note,
                color = Color(0xFFE5E7EB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(3.dp, if (hasStory) Color(0xFFF6C177) else HGray6.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (friend.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = friend.profileImageUrl,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(HChipBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = friend.avatarEmoji.ifBlank { "👤" },
                        color = HWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                }
            }
        }

        Text(
            text = friend.name,
            color = HGray2,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoryNoteBubble(
    story: ChatStory,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .widthIn(min = 78.dp, max = 98.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF2B2F35))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = story.note.ifBlank { "New note" },
                color = Color(0xFFE5E7EB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFFF6C177), CircleShape),
        ) {
            if (story.ownerProfileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = story.ownerProfileImageUrl,
                    contentDescription = story.ownerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(story.ownerAvatarColor).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = story.ownerAvatarEmoji,
                        color = HWhite,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Text(
            text = story.ownerName,
            color = HGray2,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoryViewerOverlay(
    stories: List<ChatStory>,
    startStoryId: String,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
) {
    if (stories.isEmpty()) return

    var currentIndex by remember(stories, startStoryId) {
        mutableIntStateOf(stories.indexOfFirst { it.id == startStoryId }.coerceAtLeast(0))
    }
    val progress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentStory = stories.getOrNull(currentIndex) ?: return
    val viewersFlow = remember(currentStory.id) { viewModel.observeStoryViewers(currentStory.id) }
    val viewersResult by viewersFlow.collectAsStateWithLifecycle(initialValue = Result.success(emptyList<StoryViewer>()))
    val viewers = viewersResult.getOrNull().orEmpty()
    var showReplyComposer by remember(currentStory.id) { mutableStateOf(false) }
    var replyText by remember(currentStory.id) { mutableStateOf("") }
    var sendingReply by remember(currentStory.id) { mutableStateOf(false) }

    fun goNext() {
        if (currentIndex < stories.lastIndex) {
            currentIndex += 1
        } else {
            onDismiss()
        }
    }

    fun goPrev() {
        if (currentIndex > 0) {
            currentIndex -= 1
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(startStoryId, stories.size) {
        currentIndex = stories.indexOfFirst { it.id == startStoryId }.coerceAtLeast(0)
    }

    LaunchedEffect(currentStory.id) {
        progress.snapTo(0f)
        viewModel.markStoryViewed(currentStory)

        val durationMs = 4500L
        val tickMs = 45L
        var elapsed = 0L
        while (elapsed < durationMs) {
            if (!isHolding) {
                elapsed += tickMs
                progress.snapTo((elapsed.toFloat() / durationMs).coerceIn(0f, 1f))
            }
            delay(tickMs)
        }

        if (currentIndex < stories.lastIndex) {
            currentIndex += 1
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(currentStory.id, currentIndex) {
                        detectTapGestures(
                            onTap = { goPrev() },
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            },
                        )
                    },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(currentStory.id, currentIndex) {
                        detectTapGestures(
                            onTap = { goNext() },
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            },
                        )
                    },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stories.forEachIndexed { index, story ->
                    val fill = when {
                        index < currentIndex -> 1f
                        index > currentIndex -> 0f
                        else -> progress.value
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(if (story.isMine) HBlue else HWhite),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(currentStory.ownerAvatarColor).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (currentStory.ownerProfileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentStory.ownerProfileImageUrl,
                                contentDescription = currentStory.ownerName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(currentStory.ownerAvatarEmoji, color = HWhite)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(currentStory.ownerName, color = HWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(currentStory.createdAtLabel, color = HGray4, fontSize = 12.sp)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = HWhite)
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xAA0D1117))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                    .padding(18.dp),
            ) {
                if (currentStory.storyType == "video" && currentStory.imageUrl.startsWith("http")) {
                    StoryVideoPlayer(
                        url = currentStory.imageUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 420.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                } else if (currentStory.storyType == "image" && currentStory.imageUrl.startsWith("http")) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 420.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    ) {
                        val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                        AsyncImage(
                            model = currentStory.imageUrl,
                            contentDescription = "Story image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (currentStory.overlayText.isNotBlank()) {
                            Text(
                                text = currentStory.overlayText,
                                color = Color(currentStory.overlayColor),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        IntOffset(
                                            x = (currentStory.overlayX * canvasWidth).roundToInt(),
                                            y = (currentStory.overlayY * canvasHeight).roundToInt(),
                                        )
                                    }
                                    .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (currentStory.note.isBlank()) "No note" else currentStory.note,
                        color = HWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            StoryViewerActions(
                onReply = { showReplyComposer = true },
                onReact = {
                    showReplyComposer = true
                    if (replyText.isBlank()) {
                        replyText = "❤️"
                    }
                },
                onMore = onDismiss,
            )

            // Reply composer sheet
            if (showReplyComposer) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("❤️", "😂", "🔥", "👏", "😮").forEach { emoji ->
                            AssistChip(
                                onClick = {
                                    if (sendingReply) return@AssistChip
                                    sendingReply = true
                                    coroutineScope.launch {
                                        try {
                                            viewModel.createStoryReply(currentStory, emoji)
                                            replyText = ""
                                            showReplyComposer = false
                                        } catch (_: Exception) {
                                        } finally {
                                            sendingReply = false
                                        }
                                    }
                                },
                                label = { Text(emoji) },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reply to story") },
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showReplyComposer = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            enabled = replyText.isNotBlank() && !sendingReply,
                            onClick = {
                                if (replyText.isBlank() || sendingReply) return@TextButton
                                sendingReply = true
                                coroutineScope.launch {
                                    try {
                                        viewModel.createStoryReply(currentStory, replyText)
                                        replyText = ""
                                        showReplyComposer = false
                                    } catch (_: Exception) {
                                    } finally {
                                        sendingReply = false
                                    }
                                }
                            }
                        ) {
                            Text(if (sendingReply) "Sending..." else "Send")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (currentStory.isMine) {
                SeenBySection(viewers = viewers)
            }
        }
    }
}

@Composable
private fun StoryViewerActions(
    onReply: () -> Unit,
    onReact: () -> Unit,
    onMore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reply...",
                color = HGray4,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onReply) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Reply", tint = HBlue)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("❤️", "😂", "😮", "😢", "🔥").forEach { emoji ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onReact)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(emoji, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            TextButton(onClick = onMore) {
                Text("Done", color = HBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StoryVideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 1f
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
            }
        },
        update = { it.player = player },
    )
}

@Composable
private fun SeenBySection(viewers: List<StoryViewer>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Text(
            text = if (viewers.isEmpty()) "No views yet" else "Seen by ${viewers.size}",
            color = HWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        if (viewers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            viewers.take(4).forEach { viewer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (viewer.userProfileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = viewer.userProfileImageUrl,
                                contentDescription = viewer.userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(viewer.userAvatarEmoji, color = HWhite, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(viewer.userName, color = HGray2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(viewer.viewedAtLabel, color = HGray4, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChatFilters(
    selected: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit,
    strings: com.example.kampus.ui.localization.UiStrings
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChip(
            label = strings.all,
            selected = selected == ChatFilter.All,
            onClick = { onFilterSelected(ChatFilter.All) },
        )
        FilterChip(
            label = strings.unread,
            selected = selected == ChatFilter.Unread,
            onClick = { onFilterSelected(ChatFilter.Unread) },
        )
        FilterChip(
            label = strings.groups,
            selected = selected == ChatFilter.Groups,
            onClick = { onFilterSelected(ChatFilter.Groups) },
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) HChipActive else HChipBg)
            .border(
                width = 1.dp,
                color = if (selected) HBlue.copy(alpha = 0.8f) else HBorder.copy(alpha = 0.8f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) HWhite else HGray2,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatRow(
    chat    : ChatItem,
    onClick : () -> Unit,
    onLongPress: () -> Unit,
    onTogglePin: () -> Unit,
    strings : com.example.kampus.ui.localization.UiStrings,
    currentUserName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
                onLongClick       = onLongPress,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        AvatarWithBadge(chat)

        Spacer(Modifier.width(14.dp))

        // Name + preview
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = chat.name,
                color      = HWhite,
                fontSize   = 15.sp,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val isCall = chat.lastMessageType.equals("call", ignoreCase = true)
                if (isCall) {
                    val callIcon = when {
                        chat.lastCallStatus.equals("missed", ignoreCase = true) || chat.lastCallStatus.equals("declined", ignoreCase = true) -> Icons.Filled.Call
                        chat.lastMessageSenderId.isBlank() || chat.lastMessageSenderId == chat.otherUserId -> Icons.AutoMirrored.Filled.CallReceived
                        else -> Icons.Filled.Call
                    }
                    val callTint = when {
                        chat.lastCallStatus.equals("missed", ignoreCase = true) || chat.lastCallStatus.equals("declined", ignoreCase = true) -> Color(0xFFFF6B6B)
                        chat.lastCallStatus.equals("ended", ignoreCase = true) -> HGray4
                        else -> HBlue
                    }
                    Icon(
                        imageVector = callIcon,
                        contentDescription = null,
                        tint = callTint,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Text(
                    text     = chatPreviewText(chat, strings, currentUserName),
                    color    = if (chat.unreadCount > 0) HGray2.copy(alpha = 0.85f) else HGray4,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Timestamp + unread dot/badge
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = chat.timestamp,
                    color      = if (chat.unreadCount > 0) HBlue else HGray4,
                    fontSize   = 12.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (chat.isPinned) HBlue.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTogglePin,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (chat.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (chat.isPinned) "Unpin chat" else "Pin chat",
                        tint = if (chat.isPinned) HBlue else HGray4,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (chat.unreadCount > 0 && !chat.isLastMessageFromMe) {
                UnreadGlowDot()
            }
        }
    }
}

@Composable
private fun UnreadGlowDot() {
    val transition = rememberInfiniteTransition(label = "unreadGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unreadGlowAlpha",
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = HBlue.copy(alpha = 0.12f * glowAlpha),
                    shape = CircleShape,
                )
        )
        Box(
            modifier = Modifier
                .size(11.dp)
                .background(
                    color = HBlue.copy(alpha = 0.22f * glowAlpha),
                    shape = CircleShape,
                )
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(HBlue, CircleShape)
        )
    }
}

@Composable
private fun ChatPreviewOverlay(
    preview: ChatPreviewUiState,
    onDismiss: () -> Unit,
    onOpenChat: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onMute: (String, Boolean) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onBlock: (String) -> Unit,
) {
    val livePulse = rememberInfiniteTransition(label = "preview-live-pulse")
    val liveAlpha by livePulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "preview-live-alpha",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .systemBarsPadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF121823).copy(alpha = 0.98f),
                                Color(0xFF0B0F17).copy(alpha = 0.98f),
                            ),
                        ),
                    )
                    .border(1.dp, HBorder.copy(alpha = 0.7f), RoundedCornerShape(32.dp))
                    .padding(16.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(chatColorFromTitle(preview.chatName)).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (preview.profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = preview.profileImageUrl,
                                        contentDescription = preview.chatName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    )
                                } else {
                                    Text(
                                        text = preview.avatarEmoji,
                                        color = HWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = preview.chatName,
                                    color = HWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (preview.timestamp.isBlank()) "Preview only, unread stays intact" else "${preview.timestamp} · preview only",
                                    color = HGray4,
                                    fontSize = 12.sp,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(HBlue.copy(alpha = 0.12f))
                                        .border(1.dp, HBlue.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(HBlue.copy(alpha = liveAlpha)),
                                        )
                                        Text(
                                            text = "Live preview",
                                            color = HBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close preview", tint = HWhite)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (preview.isLoading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = HBlue)
                        }
                    } else if (preview.error != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(preview.error, color = HGray4, fontSize = 13.sp)
                        }
                    } else if (preview.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(HBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = HBlue,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Text(
                                    text = "No messages yet",
                                    color = HWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Open the chat to start the conversation",
                                    color = HGray4,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(
                                items = preview.messages,
                                key = { index, message -> "${message.id}-$index" },
                            ) { _, message ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(220)) +
                                        slideInVertically(
                                            animationSpec = tween(220),
                                            initialOffsetY = { it / 4 },
                                        ),
                                ) {
                                    MessageBubble(
                                        message = message,
                                        modifier = Modifier.padding(horizontal = 2.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PreviewActionButton(
                            label = "Open",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            tint = HBlue,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenChat(preview.chatId) },
                        )
                        PreviewActionButton(
                            label = if (preview.isPinned) "Unpin" else "Pin",
                            icon = if (preview.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            tint = if (preview.isPinned) HBlue else HGray2,
                            modifier = Modifier.weight(1f),
                            onClick = { onTogglePin(preview.chatId, !preview.isPinned) },
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    PreviewActionGrid(
                        preview = preview,
                        onMarkUnread = onMarkUnread,
                        onMute = onMute,
                        onArchive = onArchive,
                        onDelete = { chatId -> onDelete(chatId); onDismiss() },
                        onBlock = { userId -> onBlock(userId); onDismiss() },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewActionGrid(
    preview: ChatPreviewUiState,
    onMarkUnread: (String) -> Unit,
    onMute: (String, Boolean) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onBlock: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PreviewActionButton(
                label = "Mark unread",
                icon = Icons.Outlined.MarkEmailUnread,
                tint = HBlue,
                modifier = Modifier.weight(1f),
                onClick = { onMarkUnread(preview.chatId) },
            )
            PreviewActionButton(
                label = if (preview.isMuted) "Unmute" else "Mute",
                icon = Icons.Outlined.NotificationsOff,
                tint = if (preview.isMuted) HBlue else HGray2,
                modifier = Modifier.weight(1f),
                onClick = { onMute(preview.chatId, !preview.isMuted) },
            )
            PreviewActionButton(
                label = if (preview.isArchived) "Unarchive" else "Archive",
                icon = Icons.Outlined.Inventory2,
                tint = if (preview.isArchived) HBlue else HGray2,
                modifier = Modifier.weight(1f),
                onClick = { onArchive(preview.chatId, !preview.isArchived) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PreviewActionButton(
                label = "Delete",
                icon = Icons.Outlined.Delete,
                tint = HRed,
                modifier = Modifier.weight(1f),
                onClick = { onDelete(preview.chatId) },
            )
            PreviewActionButton(
                label = "Block",
                icon = Icons.Outlined.Block,
                tint = HRed,
                modifier = Modifier.weight(1f),
                onClick = { onBlock(preview.otherUserId) },
            )
            PreviewActionButton(
                label = "Close",
                icon = Icons.Outlined.Close,
                tint = HGray2,
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun PreviewActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.035f))
            .border(1.dp, if (enabled) tint.copy(alpha = 0.35f) else HBorder.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else HGray4, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                color = if (enabled) HWhite else HGray4,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

private fun chatColorFromTitle(title: String): Long {
    return title.hashCode().toLong().let { hash ->
        0xFF3B82F6 + (hash % 0x000F0F0F)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Avatar with online dot
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AvatarWithBadge(chat: ChatItem) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(chat.avatarColor).copy(alpha = 0.22f))
                .border(1.5.dp, Color(chat.avatarColor).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (chat.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = chat.profileImageUrl,
                    contentDescription = chat.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    text       = chat.avatarEmoji.ifBlank { chat.avatarInitials },
                    color      = Color(chat.avatarColor),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        if (chat.isOnline) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(HBg)
                    .border(1.5.dp, HBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
            }
        }
    }
}

private fun isGroupLikeChat(chat: ChatItem): Boolean {
    return chat.otherUserId.isBlank() || chat.name.contains("group", ignoreCase = true)
}
