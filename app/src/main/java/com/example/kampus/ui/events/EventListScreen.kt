@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.events.EventColors as C

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom nav setup — mirrors KAMPUS nav exactly
// ─────────────────────────────────────────────────────────────────────────────
private data class NavItem(val label: String, val icon: ImageVector, val iconSelected: ImageVector)

private val navItems = listOf(
    NavItem("Home",   Icons.Outlined.Home,          Icons.Filled.Home),
    NavItem("Groups", Icons.Outlined.Group,         Icons.Filled.Group),
    NavItem("Events", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    NavItem("Chat",   Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
)

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun EventListScreen(
    onEventClick   : (EventItem) -> Unit = {},
    onCreateClick  : () -> Unit          = {},
    onHomeClick    : () -> Unit          = {},
    onGroupsClick  : () -> Unit          = {},
    onChatClick    : () -> Unit          = {},
    onFabClick     : () -> Unit          = {},
    onProfileClick : () -> Unit          = {},
    notifCount     : Int                 = 3,
    viewModel      : EventViewModel      = viewModel(),
) {
    val state         by viewModel.uiState.collectAsState()
    var searchQuery   by remember { mutableStateOf("") }
    var activeFilter  by remember { mutableStateOf("All") }

    val displayed = remember(activeFilter, searchQuery, state.events) {
        viewModel.filteredEvents(activeFilter, searchQuery)
    }

    Scaffold(
        containerColor = C.Bg,
        topBar = {
            EventTopBar(
                searchQuery    = searchQuery,
                onSearchChange = { searchQuery = it; viewModel.setSearch(it) },
                activeFilter   = activeFilter,
                onFilterSelect = { activeFilter = it; viewModel.setFilter(it) },
                onCreateClick  = onCreateClick,
                filters        = viewModel.filters,
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg.copy(alpha = 0.98f))))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            ) {
                EventBottomNav(
                    selectedIndex  = 2,   // Events is always index 2
                    onItemSelected = { index ->
                        when (index) {
                            0 -> onHomeClick()
                            1 -> onGroupsClick()
                            2 -> { /* already here */ }
                            3 -> onChatClick()
                        }
                    },
                    notifCount     = notifCount,
                    onFabClick     = onFabClick,
                    onProfileClick = onProfileClick,
                )
            }
        },
    ) { innerPadding ->
        if (displayed.isEmpty()) {
            EventEmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Featured banner (first featured item)
                val featured = displayed.firstOrNull { it.isFeatured }
                if (featured != null && activeFilter == "All" && searchQuery.isEmpty()) {
                    item {
                        FeaturedEventBanner(
                            event        = featured,
                            isInterested = featured.id in state.interestedIds,
                            isLiked      = featured.id in state.likedIds,
                            isSaved      = featured.id in state.savedIds,
                            onInterested = { viewModel.toggleInterested(featured.id) },
                            onLike       = { viewModel.toggleLike(featured.id) },
                            onSave       = { viewModel.toggleSave(featured.id) },
                            onClick      = { onEventClick(featured) },
                        )
                    }
                }

                // Regular cards
                val rest = if (activeFilter == "All" && searchQuery.isEmpty() && featured != null)
                    displayed.filter { !it.isFeatured }
                else displayed

                itemsIndexed(rest, key = { _, e -> e.id }) { index, event ->
                    StaggeredEventCard(
                        event        = event,
                        isInterested = event.id in state.interestedIds,
                        isLiked      = event.id in state.likedIds,
                        isSaved      = event.id in state.savedIds,
                        index        = index,
                        onInterested = { viewModel.toggleInterested(event.id) },
                        onLike       = { viewModel.toggleLike(event.id) },
                        onSave       = { viewModel.toggleSave(event.id) },
                        onClick      = { onEventClick(event) },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top bar — title + search + filter chips
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EventTopBar(
    searchQuery    : String,
    onSearchChange : (String) -> Unit,
    activeFilter   : String,
    onFilterSelect : (String) -> Unit,
    onCreateClick  : () -> Unit,
    filters        : List<String>,
) {
    Surface(color = C.Bg, shadowElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, bottom = 10.dp),
        ) {
            // ── Title row ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Events",
                        color      = C.White,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp,
                    )
                    Text(
                        "Discover events near you",
                        color    = C.Gray3,
                        fontSize = 13.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(C.Blue, C.BlueGlow)))
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onCreateClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, "Create Event", tint = C.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Surface)
                    .border(1.dp, C.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = C.Gray4, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value         = searchQuery,
                    onValueChange = onSearchChange,
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    textStyle     = androidx.compose.ui.text.TextStyle(color = C.White, fontSize = 14.sp),
                    cursorBrush   = androidx.compose.ui.graphics.SolidColor(C.Blue),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text("Search events, locations…", color = C.Gray4, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, null, tint = C.Gray4, modifier = Modifier
                            .size(16.dp)
                            .clickable(remember { MutableInteractionSource() }, null) { onSearchChange("") }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Filter chips ─────────────────────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filters) { filter ->
                    val isActive = activeFilter == filter
                    val bgColor by animateColorAsState(
                        if (isActive) C.Blue else C.Surface,
                        tween(200), label = "chip_bg_$filter",
                    )
                    val textColor by animateColorAsState(
                        if (isActive) C.White else C.Gray3,
                        tween(200), label = "chip_text_$filter",
                    )
                    val borderColor by animateColorAsState(
                        if (isActive) C.Blue else C.Border,
                        tween(200), label = "chip_border_$filter",
                    )
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { onFilterSelect(filter) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(filter, color = textColor, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Featured banner — large hero card for top event
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FeaturedEventBanner(
    event        : EventItem,
    isInterested : Boolean,
    isLiked      : Boolean,
    isSaved      : Boolean,
    onInterested : () -> Unit,
    onLike       : () -> Unit,
    onSave       : () -> Unit,
    onClick      : () -> Unit,
) {
    val interestedColor by animateColorAsState(
        if (isInterested) C.Blue else C.Gray3, tween(200), label = "ic"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(event.coverColor1, event.coverColor2)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    ) {
        // Background emoji
        Box(
            modifier         = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(event.coverEmoji, fontSize = 90.sp)
            // Bottom fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, event.coverColor2.copy(alpha = 0.95f))))
            )
        }

        // Overlays
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category + featured badge
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CategoryBadge(event.category)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Blue.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("⭐ Featured", color = C.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(100.dp))

            // Info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(event.coverColor2.copy(alpha = 0.95f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Organizer
                OrganizerRow(event)

                // Title
                Text(
                    event.title,
                    color      = C.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    maxLines   = 2,
                )

                // Meta
                EventMetaRow(event)

                HorizontalDivider(color = C.Border.copy(0.4f), thickness = 0.5.dp)

                // Actions
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionButton(
                            icon  = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            label = "${event.likes + if (isLiked) 1 else 0}",
                            tint  = if (isLiked) C.Red else C.Gray3,
                            onClick = onLike,
                        )
                        ActionButton(Icons.Outlined.ChatBubbleOutline, "${event.comments}", C.Gray3)
                        ActionButton(Icons.Outlined.Share, "${event.shares}", C.Gray3)
                    }
                    Icon(
                        if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        "Save",
                        tint     = if (isSaved) C.Blue else C.Gray3,
                        modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onSave),
                    )
                }

                // CTA button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isInterested)
                                Brush.linearGradient(listOf(C.BlueSoft, C.BlueSoft))
                            else
                                Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                        )
                        .border(1.dp, if (isInterested) C.Blue else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onInterested),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            if (isInterested) Icons.Filled.CheckCircle else Icons.Outlined.Stars,
                            null, tint = C.White, modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (isInterested) "Interested ✓" else "I'm Interested",
                            color      = C.White,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Staggered wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StaggeredEventCard(
    event        : EventItem,
    isInterested : Boolean,
    isLiked      : Boolean,
    isSaved      : Boolean,
    index        : Int,
    onInterested : () -> Unit,
    onLike       : () -> Unit,
    onSave       : () -> Unit,
    onClick      : () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(300)) + slideInVertically(tween(300, easing = EaseOutCubic)) { it / 3 },
    ) {
        EventCard(event, isInterested, isLiked, isSaved, onInterested, onLike, onSave, onClick)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Regular event card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EventCard(
    event        : EventItem,
    isInterested : Boolean,
    isLiked      : Boolean,
    isSaved      : Boolean,
    onInterested : () -> Unit,
    onLike       : () -> Unit,
    onSave       : () -> Unit,
    onClick      : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(C.Card)
            .border(1.dp, C.Border, RoundedCornerShape(22.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
    ) {
        // ── Cover ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Brush.linearGradient(listOf(event.coverColor1, event.coverColor2))),
            contentAlignment = Alignment.Center,
        ) {
            Text(event.coverEmoji, fontSize = 64.sp)

            // Category badge top-left
            Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                CategoryBadge(event.category)
            }

            // Save icon top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    "Save",
                    tint     = if (isSaved) C.Blue else C.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Bottom fade
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, C.Card.copy(alpha = 0.9f))))
            )
        }

        // ── Content ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrganizerRow(event)

            Text(
                event.title,
                color      = C.White,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.3).sp,
            )

            EventMetaRow(event)

            HorizontalDivider(color = C.Border.copy(0.5f), thickness = 0.5.dp)

            // ── Action row ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ActionButton(
                        icon    = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        label   = "${event.likes + if (isLiked) 1 else 0}",
                        tint    = if (isLiked) C.Red else C.Gray3,
                        onClick = onLike,
                    )
                    ActionButton(Icons.Outlined.ChatBubbleOutline, "${event.comments}", C.Gray3)
                    ActionButton(Icons.Outlined.Share, "${event.shares}", C.Gray3)
                }
                // Interested count pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.BlueSoft.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.People, null, tint = C.Blue, modifier = Modifier.size(12.dp))
                    Text(
                        "${event.interested + if (isInterested) 1 else 0}",
                        color      = C.Blue,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ── CTA button ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isInterested)
                            Brush.linearGradient(listOf(C.BlueSoft, C.BlueSoft))
                        else
                            Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                    )
                    .border(1.dp, if (isInterested) C.Blue else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onInterested),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (isInterested) Icons.Filled.CheckCircle else Icons.Outlined.Stars,
                        null, tint = C.White, modifier = Modifier.size(15.dp)
                    )
                    Text(
                        if (isInterested) "Interested ✓" else "I'm Interested",
                        color      = C.White,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared sub-composables
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryBadge(category: EventCategory) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(category.color.copy(alpha = 0.18f))
            .border(1.dp, category.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(category.emoji, fontSize = 11.sp)
        Text(
            category.label.uppercase(),
            color      = category.color,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun OrganizerRow(event: EventItem) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(C.Blue.copy(0.3f), C.Gray5.copy(0.4f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(event.organizerEmoji, fontSize = 16.sp)
        }
        Column {
            Text(event.organizer, color = C.Gray2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(event.organizerTime, color = C.Gray4, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.MoreVert, null, tint = C.Gray4, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EventMetaRow(event: EventItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = C.Blue, modifier = Modifier.size(13.dp))
            Text("${event.date}  •  ${event.time}", color = C.Gray2, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.LocationOn, null, tint = C.Blue, modifier = Modifier.size(13.dp))
            Text(event.location, color = C.Gray2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.People, null, tint = C.Blue, modifier = Modifier.size(13.dp))
            Text("${event.interested} people interested", color = C.Gray3, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier              = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        if (label.isNotEmpty()) Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EventEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp).clip(CircleShape)
                    .background(C.Surface).border(1.dp, C.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🗓️", fontSize = 28.sp)
            }
            Text("No events found", color = C.Gray3, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Try a different filter or search", color = C.Gray4, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom Nav — KAMPUS style, Events (index 2) pre-selected
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EventBottomNav(
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
        Row(
            modifier = Modifier
                .weight(1f).height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(C.NavBg)
                .border(1.dp, Color(0xFF1A2333), RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            navItems.forEachIndexed { i, item ->
                val selected = selectedIndex == i
                val tabBg     by animateColorAsState(if (selected) C.Blue.copy(0.12f) else Color.Transparent, tween(240), label = "bg$i")
                val tabBorder by animateColorAsState(if (selected) C.Blue.copy(0.65f) else Color.Transparent, tween(240), label = "bd$i")
                val iconTint  by animateColorAsState(if (selected) C.Blue else C.Gray3, tween(220), label = "it$i")

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(tabBg)
                        .border(1.dp, tabBorder, RoundedCornerShape(24.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { onItemSelected(i) }
                        .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(if (selected) item.iconSelected else item.icon, item.label, tint = iconTint, modifier = Modifier.size(21.dp))
                        AnimatedVisibility(
                            visible = selected,
                            enter   = fadeIn(tween(160)) + expandHorizontally(tween(200), Alignment.Start),
                            exit    = fadeOut(tween(100)) + shrinkHorizontally(tween(150), Alignment.Start),
                        ) {
                            Text(item.label, color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .size(58.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(C.Blue, C.BlueGlow), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(80f, 80f)))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onFabClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "Create", tint = C.White, modifier = Modifier.size(26.dp))
        }

        // Profile
        Box(modifier = Modifier.size(58.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp).clip(CircleShape)
                    .background(Color(0xFF0F1520))
                    .border(1.dp, Color(0xFF1A2333), CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onProfileClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Person, "Profile", tint = C.Gray3, modifier = Modifier.size(24.dp))
            }
            if (notifCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp).align(Alignment.TopEnd).offset(2.dp, (-2).dp)
                        .clip(CircleShape).background(C.Red).border(1.5.dp, C.Bg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$notifCount", color = C.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}