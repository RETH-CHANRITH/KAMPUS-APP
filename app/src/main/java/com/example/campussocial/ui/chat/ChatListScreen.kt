@file:Suppress("SpellCheckingInspection")
package com.example.campussocial.ui.chat
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.chat.ChatViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Palette — identical to HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
private val HBg     = Color(0xFF080B11)
private val HCard   = Color(0xFF0F1520)
private val HBorder = Color(0xFF1A2333)
private val HBlue   = Color(0xFF3B82F6)
private val HGlow   = Color(0xFF2563EB)
private val HWhite  = Color(0xFFFFFFFF)
private val HGray2  = Color(0xFFE5E7EB)
private val HGray4  = Color(0xFF9CA3AF)
private val HGray6  = Color(0xFF374151)
private val HRed    = Color(0xFFEF4444)
private val HNavBg  = Color(0xFF0C1018)

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

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ChatListScreen(
    onChatClick    : (Int) -> Unit,
    onHomeClick    : () -> Unit,
    onGroupsClick  : () -> Unit,
    onEventsClick  : () -> Unit,
    onProfileClick : () -> Unit,
    viewModel      : ChatViewModel = viewModel(),
) {
    val state      by viewModel.chatListState.collectAsStateWithLifecycle()
    var notifCount by remember { mutableIntStateOf(3) }

    Scaffold(
        containerColor = HBg,
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
                CampusBottomNav(
                    selectedIndex  = 3,          // Chat tab is index 3
                    onItemSelected = { index ->
                        when (index) {
                            0 -> onHomeClick()
                            1 -> onGroupsClick()
                            2 -> onEventsClick()
                            3 -> { /* already here */ }
                        }
                    },
                    notifCount     = notifCount,
                    onFabClick     = { },
                    onProfileClick = onProfileClick,
                )
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
            // ── Header ──────────────────────────────────────────────────────
            ChatListHeader()

            // ── Search ──────────────────────────────────────────────────────
            ChatSearchBar(
                query         = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            Spacer(Modifier.height(6.dp))

            // ── List ────────────────────────────────────────────────────────
            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(state.chats, key = { it.id }) { chat ->
                    ChatRow(
                        chat    = chat,
                        onClick = { onChatClick(chat.id) },
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 78.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color     = HBorder.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text          = "Messages",
                color         = HWhite,
                fontSize      = 28.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text     = "Your conversations",
                color    = HGray4,
                fontSize = 13.sp,
            )
        }
        // "4 new" pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(HBlue, HGlow),
                        start = Offset(0f, 0f),
                        end   = Offset(120f, 0f),
                    )
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text       = "4 new",
                color      = HWhite,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatSearchBar(
    query         : String,
    onQueryChange : (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HCard)
            .border(1.dp, HBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 2.dp),
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
                Text("Search messages…", color = HGray4, fontSize = 14.sp)
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

// ─────────────────────────────────────────────────────────────────────────────
// Chat row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatRow(
    chat    : ChatItem,
    onClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .background(
                if (chat.unreadCount > 0) HCard.copy(alpha = 0.6f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
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
            Text(
                text     = chat.lastMessage,
                color    = if (chat.unreadCount > 0) HGray2.copy(alpha = 0.85f) else HGray4,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Timestamp + unread badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = chat.timestamp,
                color      = if (chat.unreadCount > 0) HBlue else HGray4,
                fontSize   = 12.sp,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(4.dp))
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(HBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = if (chat.unreadCount > 9) "9+" else "${chat.unreadCount}",
                        color      = HWhite,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
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
            Text(
                text       = chat.avatarInitials,
                color      = Color(chat.avatarColor),
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
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

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Nav — EXACT copy of CampusBottomNav from HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CampusBottomNav(
    selectedIndex  : Int,
    onItemSelected : (Int) -> Unit,
    notifCount     : Int,
    onFabClick     : () -> Unit,
    onProfileClick : () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── LEFT PILL — nav tabs ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(HNavBg)
                .border(1.dp, HBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            navItems.forEachIndexed { i, item ->
                val selected = selectedIndex == i

                val tabBg by animateColorAsState(
                    if (selected) HBlue.copy(alpha = 0.12f) else Color.Transparent,
                    tween(240), label = "bg$i",
                )
                val tabBorder by animateColorAsState(
                    if (selected) HBlue.copy(alpha = 0.65f) else Color.Transparent,
                    tween(240), label = "bd$i",
                )
                val iconTint by animateColorAsState(
                    if (selected) HBlue else HGray4,
                    tween(220), label = "it$i",
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(tabBg)
                        .border(1.dp, tabBorder, RoundedCornerShape(24.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { onItemSelected(i) }
                        .padding(
                            horizontal = if (selected) 14.dp else 10.dp,
                            vertical   = 9.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector        = if (selected) item.iconSelected else item.icon,
                            contentDescription = item.label,
                            tint               = iconTint,
                            modifier           = Modifier.size(21.dp),
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter   = fadeIn(tween(160)) + expandHorizontally(
                                animationSpec = tween(200),
                                expandFrom    = Alignment.Start,
                            ),
                            exit    = fadeOut(tween(100)) + shrinkHorizontally(
                                animationSpec = tween(150),
                                shrinkTowards = Alignment.Start,
                            ),
                        ) {
                            Text(
                                item.label,
                                color      = HBlue,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines   = 1,
                            )
                        }
                    }
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(HBlue, HGlow),
                        start = Offset(0f, 0f),
                        end   = Offset(80f, 80f),
                    )
                )
                .clickable(remember { MutableInteractionSource() }, null, onClick = onFabClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "New", tint = HWhite, modifier = Modifier.size(26.dp))
        }

        // ── Profile ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.size(58.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(HCard)
                    .border(1.dp, HBorder, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onProfileClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Person, "Profile", tint = HGray4, modifier = Modifier.size(24.dp))
            }
            if (notifCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
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