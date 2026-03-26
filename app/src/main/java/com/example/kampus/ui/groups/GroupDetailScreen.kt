@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.ui.groups.GroupColors as C

// ─────────────────────────────────────────────────────────────────────────────
//  Static sample posts  (replace with repository / ViewModel in production)
// ─────────────────────────────────────────────────────────────────────────────
private val samplePosts = listOf(
    GroupPost(
        id            = 1,
        author        = "Alex Morgan",
        initials      = "AM",
        initialsColor = Color(0xFF7C3AED),
        time          = "2h ago",
        content       = "Just finished this new UI design. What do you all think? Would love some feedback!",
        hasImage      = true,
        likes         = 24,
        comments      = 8,
    ),
    GroupPost(
        id            = 2,
        author        = "Maria Garcia",
        initials      = "MG",
        initialsColor = Color(0xFF059669),
        time          = "5h ago",
        content       = "Color palette inspiration for my next project — so many directions to explore.",
        hasImage      = false,
        likes         = 42,
        comments      = 12,
    ),
    GroupPost(
        id            = 3,
        author        = "James Lee",
        initials      = "JL",
        initialsColor = Color(0xFFDC2626),
        time          = "1d ago",
        content       = "Anyone joining the design sprint this weekend? Drop a comment below!",
        hasImage      = false,
        likes         = 18,
        comments      = 23,
    ),
    GroupPost(
        id            = 4,
        author        = "Sofia Patel",
        initials      = "SP",
        initialsColor = Color(0xFFD97706),
        time          = "2d ago",
        content       = "Sharing my latest typography work — feedback welcome!",
        hasImage      = false,
        likes         = 67,
        comments      = 15,
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GroupDetailScreen(
    group     : GroupData,
    onBack    : () -> Unit     = {},
    viewModel : GroupViewModel = viewModel(),
) {
    val state    by viewModel.uiState.collectAsState()
    val isJoined  = group.id in state.joinedIds
    var postText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = C.Bg,
        bottomBar = {
            PostComposer(
                text         = postText,
                onTextChange = { postText = it },
                onPost       = { postText = "" },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {

            // ── Cover + nav bar ───────────────────────────────────────────────
            item {
                CoverSection(group = group, onBack = onBack)
            }

            // ── Group info + join button ──────────────────────────────────────
            item {
                GroupInfoSection(
                    group    = group,
                    isJoined = isJoined,
                    onToggle = { viewModel.toggleJoin(group.id) },
                )
                HorizontalDivider(
                    color     = C.Border.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier  = Modifier.padding(horizontal = 20.dp),
                )
            }

            // ── Recent posts header ───────────────────────────────────────────
            item {
                Text(
                    text          = "Recent Posts",
                    color         = C.White,
                    fontSize      = 17.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    modifier      = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            // ── Posts ─────────────────────────────────────────────────────────
            items(samplePosts, key = { it.id }) { post ->
                PostCard(
                    post    = post,
                    isLiked = post.id in state.likedPostIds,
                    onLike  = { viewModel.toggleLike(post.id) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Cover
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CoverSection(group: GroupData, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.linearGradient(listOf(group.coverColor1, group.coverColor2))
            ),
    ) {
        Text(
            text     = group.coverEmoji,
            fontSize = 88.sp,
            modifier = Modifier.align(Alignment.Center),
        )

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            C.Bg.copy(alpha = 0.65f),
                        )
                    )
                )
        )

        // Nav row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            CoverIconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = C.White,
                    modifier           = Modifier.size(20.dp),
                )
            }
            Text(
                text       = group.name,
                color      = C.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            CoverIconButton(onClick = {}) {
                Icon(
                    imageVector        = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint               = C.White,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CoverIconButton(
    onClick : () -> Unit,
    content : @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(C.Bg.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Group info + join
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GroupInfoSection(
    group    : GroupData,
    isJoined : Boolean,
    onToggle : () -> Unit,
) {
    Column(
        modifier            = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text          = group.name,
            color         = C.White,
            fontSize      = 22.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text          = group.category.uppercase(),
            color         = C.Blue,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.Group, null, tint = C.Gray3, modifier = Modifier.size(14.dp))
                Text("${group.members} members", color = C.Gray3, fontSize = 13.sp)
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = C.Gray3, modifier = Modifier.size(14.dp))
                Text("${group.posts} posts", color = C.Gray3, fontSize = 13.sp)
            }
        }

        Text(
            text       = group.description,
            color      = C.Gray1.copy(alpha = 0.85f),
            fontSize   = 14.sp,
            lineHeight = 22.sp,
        )

        // Join / Joined button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isJoined)
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    else
                        Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                )
                .then(
                    if (isJoined) Modifier.border(1.dp, C.Border, RoundedCornerShape(14.dp))
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onToggle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = if (isJoined) "Joined" else "Join Group",
                color      = if (isJoined) C.Gray3 else C.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Post card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PostCard(
    post    : GroupPost,
    isLiked : Boolean,
    onLike  : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .border(1.dp, C.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Author row ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Initials avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(post.initialsColor.copy(alpha = 0.15f))
                        .border(1.5.dp, post.initialsColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = post.initials,
                        color      = post.initialsColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(post.author, color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(post.time,   color = C.Gray4, fontSize = 11.sp)
                }
            }
            Icon(Icons.Default.MoreVert, null, tint = C.Gray5, modifier = Modifier.size(18.dp))
        }

        // ── Content ───────────────────────────────────────────────────────────
        Text(
            text       = post.content,
            color      = C.Gray1,
            fontSize   = 14.sp,
            lineHeight = 22.sp,
        )

        // ── Optional image placeholder ────────────────────────────────────────
        if (post.hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1E1040), Color(0xFF2D1B6E)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎨", fontSize = 40.sp)
            }
        }

        // ── Actions ───────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Like
            val likeColor by animateColorAsState(
                targetValue   = if (isLiked) C.Red else C.Gray4,
                animationSpec = tween(200),
                label         = "like_color",
            )
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onLike,
                ),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector        = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint               = likeColor,
                    modifier           = Modifier.size(17.dp),
                )
                Text(
                    text     = "${post.likes + if (isLiked) 1 else 0}",
                    color    = likeColor,
                    fontSize = 13.sp,
                )
            }

            // Comments
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = C.Gray4, modifier = Modifier.size(17.dp))
                Text("${post.comments}", color = C.Gray4, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom post composer
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PostComposer(
    text         : String,
    onTextChange : (String) -> Unit,
    onPost       : () -> Unit,
) {
    Surface(
        color          = C.NavBg,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = C.NavBorder,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Input field
            BasicTextField(
                value         = text,
                onValueChange = onTextChange,
                modifier      = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Surface)
                    .border(1.dp, C.Border, RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .defaultMinSize(minHeight = 52.dp),
                textStyle     = TextStyle(color = C.White, fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush   = SolidColor(C.Blue),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("Share something with the group…", color = C.Gray5, fontSize = 14.sp)
                    }
                    inner()
                },
            )

            // Action row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Image attach button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.Surface)
                        .border(1.dp, C.Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Image, null, tint = C.Gray4, modifier = Modifier.size(18.dp))
                }

                // Post button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onPost,
                        )
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Post",
                            tint               = C.White,
                            modifier           = Modifier.size(16.dp),
                        )
                        Text(
                            text       = "Post",
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