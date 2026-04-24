package com.example.kampus.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

private val Bg = Color(0xFF080B11)
private val Card = Color(0xFF252A41)
private val Border = Color(0xFF2C3552)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF99A1AF)
private val Blue = Color(0xFF0D7FFF)

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenActivity: (ProfileActivityItem) -> Unit,
    viewModel: PublicProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.observeUser(userId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1a1f3a), Color(0xFF080B11))
                            )
                        ),
                ) {
                    if (state.coverImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = state.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 48.dp)
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(4.dp, Bg, CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF20A4FF), Color(0xFF7C3AED)))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = state.profileImageUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Text(text = state.avatarEmoji, fontSize = 36.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = state.displayName.ifBlank { "Unknown user" }, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text(text = state.location.ifBlank { "Location not set" }, color = TextSecondary, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(value = state.posts.toString(), label = "Posts")
                    StatItem(value = state.followers.toString(), label = "Followers")
                    StatItem(value = state.following.toString(), label = "Following")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "About", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card)
                        .border(1.dp, Border, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        text = state.bio.ifBlank { "No bio yet" },
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Recent Activity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                val filteredActivities = state.activities.filter { it.type != "share_profile" }
                if (filteredActivities.isEmpty()) {
                    Text(text = "No public activity yet.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    filteredActivities.forEach { activity ->
                        PublicActivityRow(activity = activity, onClick = { onOpenActivity(activity) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun PublicActivityRow(activity: ProfileActivityItem, onClick: () -> Unit) {
    val icon = when (activity.type) {
        "share_profile" -> Icons.Outlined.Share
        "interested_event" -> Icons.Outlined.CalendarMonth
        "create_post" -> Icons.Outlined.ModeEdit
        "create_event" -> Icons.Outlined.AddCircle
        else -> Icons.Outlined.Stars
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = activity.text, color = TextPrimary, fontSize = 14.sp)
            Text(text = formatActivityTime(activity.createdAt), color = TextSecondary, fontSize = 11.sp)
        }
    }
}

private fun formatActivityTime(timestamp: Long): String {
    if (timestamp <= 0L) return "just now"
    val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
