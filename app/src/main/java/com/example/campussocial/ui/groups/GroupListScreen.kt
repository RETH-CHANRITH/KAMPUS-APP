@file:Suppress("SpellCheckingInspection", "unused")
package com.example.kampus.ui.groups

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.groups.GroupColors as C

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom nav items — must match HomeScreen order exactly
//  0=Home, 1=Groups, 2=Events, 3=Chat
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
//  Screen
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun GroupListScreen(
    onGroupClick       : (GroupData) -> Unit = {},
    onCreateGroupClick : () -> Unit          = {},
    onHomeClick        : () -> Unit          = {},   // ← navigate back to Home
    onEventsClick      : () -> Unit          = {},   // ← future
    onChatClick        : () -> Unit          = {},   // ← future
    onFabClick         : () -> Unit          = {},   // ← create post FAB
    onProfileClick     : () -> Unit          = {},   // ← profile
    viewModel          : GroupViewModel      = viewModel(),
) {
    val state       by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Groups tab is always index 1 in the global nav
    val selectedNavIndex = 1

    val displayed = remember(selectedTab, searchQuery, state.myGroups, state.discoverGroups) {
        viewModel.filteredGroups(selectedTab, searchQuery)
    }

    Scaffold(
        containerColor = C.Bg,

        // ── FIXED TOP BAR ────────────────────────────────────────────────────
        topBar = {
            GroupTopBar(
                searchQuery        = searchQuery,
                onSearchChange     = { searchQuery = it; viewModel.setSearch(it) },
                selectedTab        = selectedTab,
                onTabSelected      = { selectedTab = it; viewModel.selectTab(it) },
                onCreateGroupClick = onCreateGroupClick,
            )
        },

        // ── FIXED BOTTOM NAV ─────────────────────────────────────────────────
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, C.Bg.copy(alpha = 0.98f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            ) {
                GroupBottomNav(
                    selectedIndex  = selectedNavIndex,
                    onItemSelected = { index ->
                        when (index) {
                            0 -> onHomeClick()
                            1 -> { /* already here */ }
                            2 -> onEventsClick()
                            3 -> onChatClick()
                        }
                    },
                    onFabClick     = onFabClick,
                    onProfileClick = onProfileClick,
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState  = selectedTab,
            transitionSpec = {
                if (targetState > initialState)
                    slideInHorizontally { it / 3 } + fadeIn(tween(220)) togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut(tween(180))
                else
                    slideInHorizontally { -it / 3 } + fadeIn(tween(220)) togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut(tween(180))
            },
            label    = "tab_anim",
            modifier = Modifier.padding(innerPadding),
        ) { _ ->
            if (displayed.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(displayed, key = { _, g -> g.id }) { index, group ->
                        val isJoined = group.id in state.joinedIds
                        StaggeredGroupCard(
                            group    = group,
                            isJoined = isJoined,
                            index    = index,
                            onJoin   = { viewModel.toggleJoin(group.id) },
                            onClick  = { onGroupClick(group) },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom Nav — same style as HomeScreen, Groups tab pre-selected
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupBottomNav(
    selectedIndex  : Int,
    onItemSelected : (Int) -> Unit,
    onFabClick     : () -> Unit,
    onProfileClick : () -> Unit,
) {
    val NavBg     = Color(0xFF0C1018)
    val HBlue     = Color(0xFF3B82F6)
    val HGlow     = Color(0xFF2563EB)
    val HBorder   = Color(0xFF1A2333)
    val HCard     = Color(0xFF0F1520)
    val HGray4    = Color(0xFF9CA3AF)
    val HWhite    = Color(0xFFFFFFFF)
    val HBg       = Color(0xFF080B11)

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
                .background(NavBg)
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
                        .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 9.dp),
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
                            enter   = fadeIn(tween(160)) + expandHorizontally(tween(200), Alignment.Start),
                            exit    = fadeOut(tween(100)) + shrinkHorizontally(tween(150), Alignment.Start),
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
                .background(Brush.linearGradient(
                    listOf(HBlue, HGlow),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(80f, 80f),
                ))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onFabClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "Create post", tint = HWhite, modifier = Modifier.size(26.dp))
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
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top bar  (title + search + tabs)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupTopBar(
    searchQuery        : String,
    onSearchChange     : (String) -> Unit,
    selectedTab        : Int,
    onTabSelected      : (Int) -> Unit,
    onCreateGroupClick : () -> Unit,
) {
    Surface(color = C.Bg, shadowElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text          = "Groups",
                    color         = C.White,
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(C.Blue)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onCreateGroupClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, "Create Group", tint = C.White, modifier = Modifier.size(20.dp))
                }
            }

            val source = remember { MutableInteractionSource() }
            val focused by source.collectIsFocusedAsState()
            val borderColor by animateColorAsState(
                targetValue   = if (focused) C.Blue else C.Border,
                animationSpec = tween(200),
                label         = "search_border",
            )

            BasicTextField(
                value             = searchQuery,
                onValueChange     = onSearchChange,
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(C.Surface)
                    .border(1.dp, borderColor, RoundedCornerShape(13.dp)),
                singleLine        = true,
                textStyle         = TextStyle(color = C.White, fontSize = 14.sp),
                cursorBrush       = SolidColor(C.Blue),
                interactionSource = source,
                decorationBox     = { inner ->
                    Row(
                        modifier          = Modifier.fillMaxSize().padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = C.Gray3, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(9.dp))
                        Box(Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) Text("Search groups…", color = C.Gray5, fontSize = 14.sp)
                            inner()
                        }
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .border(1.dp, C.Border, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf("My Groups", "Discover").forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    val bgBrush = if (selected)
                        Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                    else
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

                    val textColor by animateColorAsState(
                        targetValue   = if (selected) C.White else C.Gray4,
                        animationSpec = tween(220),
                        label         = "tab_text_$index",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgBrush)
                            .clickable(remember { MutableInteractionSource() }, null) { onTabSelected(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = label,
                            color      = textColor,
                            fontSize   = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Staggered entrance wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StaggeredGroupCard(
    group    : GroupData,
    isJoined : Boolean,
    index    : Int,
    onJoin   : () -> Unit,
    onClick  : () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 55L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(280)) + slideInVertically(tween(280, easing = EaseOutCubic)) { it / 3 },
    ) {
        GroupCard(group = group, isJoined = isJoined, onJoin = onJoin, onClick = onClick)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Group card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupCard(
    group    : GroupData,
    isJoined : Boolean,
    onJoin   : () -> Unit,
    onClick  : () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = C.Card),
        border    = BorderStroke(1.dp, C.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Brush.linearGradient(listOf(group.coverColor1, group.coverColor2))),
                contentAlignment = Alignment.Center,
            ) {
                Text(group.coverEmoji, fontSize = 56.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, C.Card.copy(alpha = 0.9f))))
                )
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(group.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                Text(group.category.uppercase(), color = C.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatChip(
                        icon  = { Icon(Icons.Outlined.Group, null, tint = C.Gray3, modifier = Modifier.size(13.dp)) },
                        label = "${group.members} members",
                    )
                    StatChip(
                        icon  = { Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = C.Gray3, modifier = Modifier.size(13.dp)) },
                        label = "${group.posts} posts",
                    )
                }

                Spacer(Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isJoined) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            else Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                        )
                        .border(1.dp, if (isJoined) C.Border else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onJoin),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = if (isJoined) "Joined" else "Join Group",
                        color      = if (isJoined) C.Gray3 else C.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatChip(icon: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon()
        Text(text = label, color = C.Gray3, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(C.Surface).border(1.dp, C.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Search, null, tint = C.Gray5, modifier = Modifier.size(24.dp))
            }
            Text("No groups found", color = C.Gray5, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Try a different keyword", color = C.Border, fontSize = 13.sp)
        }
    }
}