@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.feed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// ─────────────────────────────────────────────────────────────────────────────
// Palette
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
// Models
// ─────────────────────────────────────────────────────────────────────────────
data class StoryItem(
    val name   : String,
    val emoji  : String,
    val isMe   : Boolean = false,
    val hasNew : Boolean = true,
)

data class PostItem(
    val id         : Int,
    val author     : String,
    val avatar     : String,
    val time       : String,
    val content    : String,
    val imageEmoji : String?  = null,
    val likes      : Int,
    val comments   : Int,
    val isVerified : Boolean  = false,
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

private val posts = listOf(
    PostItem(1, "Blenderr Art",  "🎨", "8h",     "Just finished my latest 3D render for the campus art exhibition 🔥", "🟫", 325, 98),
    PostItem(2, "Sanita R.",     "👩‍💼", "2h ago", "Hello Everyone, How are you today? Hope you all fine and blessed everyday 😊", null, 142, 34, isVerified = true),
    PostItem(3, "Campus Events", "🎓", "1h ago", "📢 Tech Talk with Senior Engineers this Friday at 5 PM in Hall B. Register before seats fill up!", null, 89, 21, isVerified = true),
    PostItem(4, "Caroline Tan",  "👩‍🔬", "30m ago","Study group for Data Structures finals — Library Room 204, tomorrow 3 PM 📚", null, 57, 18),
    PostItem(5, "Soccer Club",   "⚽", "45m ago","Great match today! Final score 3–1 🏆  Next game Saturday — don't miss it!", "🏟️", 210, 47),
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
    var selectedNav by remember { mutableIntStateOf(0) }
    var notifCount  by remember { mutableIntStateOf(3) }
    var likedPosts  by remember { mutableStateOf(setOf<Int>()) }
    val listState   = rememberLazyListState()

    Scaffold(
        containerColor = HBg,
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
                CampusBottomNav(
                    selectedIndex  = selectedNav,
                    // ── FIX: wire navigation callbacks per tab index ──────────
                    onItemSelected = { index ->
                        selectedNav = index
                        when (index) {
                            0 -> { /* already on Home, do nothing */ }
                            1 -> onGroupsClick()
                            2 -> onEventsClick()
                            3 -> onChatClick()
                        }
                    },
                    notifCount     = notifCount,
                    onFabClick     = onCreatePost,
                    onProfileClick = onProfileClick,
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
                        likedPosts = if (post.id in likedPosts)
                            likedPosts - post.id else likedPosts + post.id
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
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
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onSearchClick),
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
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onNotifClick),
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
        modifier            = Modifier.clickable(remember { MutableInteractionSource() }, null) {},
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
        Text(
            story.name,
            color      = if (story.hasNew) HGray2 else HGray4,
            fontSize   = 11.sp,
            fontWeight = if (story.hasNew) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 1,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Post Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PostCard(post: PostItem, isLiked: Boolean, onLike: () -> Unit) {
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
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null) {},
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MoreVert, "More", tint = HGray4, modifier = Modifier.size(18.dp))
            }
        }

        if (post.imageEmoji != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(HGray6.copy(0.28f), Color(0xFF050810)))),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(post.imageEmoji, fontSize = 80.sp)
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
                    ImageAction(Icons.AutoMirrored.Outlined.Send, "", HWhite.copy(0.8f))
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
                TextAction(Icons.AutoMirrored.Outlined.Send, "Share", HGray4)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ImageAction(icon: ImageVector, label: String, tint: Color, scale: Float = 1f, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
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
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
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
private fun CampusBottomNav(
    selectedIndex  : Int,
    onItemSelected : (Int) -> Unit,   // ← receives index, caller decides navigation
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
                        .clickable(
                            remember { MutableInteractionSource() }, null
                        ) { onItemSelected(i) }   // ← calls parent with index
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
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end   = androidx.compose.ui.geometry.Offset(80f, 80f),
                    )
                )
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