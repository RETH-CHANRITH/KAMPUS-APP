package com.example.kampus.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kampus.ui.theme.ThemeController

@Composable
fun NotificationItem(
	item: GroupedNotification,
	onClick: () -> Unit,
) {
	val isDark = ThemeController.isDark
	val colors = MaterialTheme.colorScheme
	val accentColor = ThemeController.accent.color

	// Premium backgrounds matching home screen
	val bg = if (item.isRead) {
		if (isDark) Color(0xFF0F1520) else Color(0xFFFFFFFF)
	} else {
		// Unread notifications get a premium glowing accent background
		if (isDark) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.08f)
	}
	val borderColor = if (isDark) Color(0xFF1A2333) else Color(0xFFD1D5DB)

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(16.dp))
			.clickable(onClick = onClick),
		color = bg,
		shape = RoundedCornerShape(16.dp),
		border = BorderStroke(1.dp, if (!item.isRead) accentColor.copy(alpha = 0.25f) else borderColor.copy(alpha = 0.35f)),
		tonalElevation = if (item.isRead) 0.dp else 2.dp,
	) {
		Row(
			modifier = Modifier.padding(12.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// Avatar Stack
			Box(modifier = Modifier.size(52.dp)) {
				if (item.actors.size > 1) {
					val first = item.actors[0]
					val second = item.actors[1]
					// Bottom layer (second actor)
					AvatarImage(
						actor = second,
						size = 34.dp,
						modifier = Modifier
							.align(Alignment.BottomEnd)
							.border(2.dp, bg, CircleShape)
					)
					// Top layer (first actor)
					AvatarImage(
						actor = first,
						size = 34.dp,
						modifier = Modifier
							.align(Alignment.TopStart)
							.border(2.dp, bg, CircleShape)
					)
				} else {
					val actor = item.actors.firstOrNull() ?: ActorProfile(displayName = "Someone")
					AvatarImage(
						actor = actor,
						size = 48.dp,
						modifier = Modifier.align(Alignment.Center)
					)
				}

				// Small type badge on bottom right
				Box(
					modifier = Modifier
						.size(18.dp)
						.align(Alignment.BottomEnd)
						.clip(CircleShape)
						.background(badgeColorForType(item.type))
						.border(1.5.dp, bg, CircleShape),
					contentAlignment = Alignment.Center
				) {
					Icon(
						imageVector = badgeIconForType(item.type),
						contentDescription = null,
						tint = Color.White,
						modifier = Modifier.size(10.dp)
					)
				}
			}

			// Text details
			Column(modifier = Modifier.weight(1f)) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = item.title,
						color = if (isDark) Color.White else Color(0xFF111827),
						fontWeight = FontWeight.Bold,
						fontSize = 14.sp,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.weight(1f)
					)
					Text(
						text = formatTimeAgo(item.latestCreatedAt),
						color = if (isDark) Color.White.copy(alpha = 0.52f) else Color(0xFF111827).copy(alpha = 0.60f),
						fontSize = 11.sp,
						modifier = Modifier.padding(start = 6.dp)
					)
				}
				Spacer(modifier = Modifier.height(2.dp))
				Text(
					text = item.body,
					color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF111827).copy(alpha = 0.8f),
					fontSize = 13.sp,
					lineHeight = 17.sp,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis
				)
			}

			// Thumbnail on far right
			if (item.postImageUrl.isNotBlank()) {
				AsyncImage(
					model = item.postImageUrl,
					contentDescription = "Post preview",
					modifier = Modifier
						.size(40.dp)
						.clip(RoundedCornerShape(8.dp))
						.background(colors.surfaceVariant),
					contentScale = ContentScale.Crop
				)
			}

			// Unread dot
			if (!item.isRead) {
				Box(
					modifier = Modifier
						.size(8.dp)
						.clip(CircleShape)
						.background(accentColor)
				)
			}
		}
	}
}

@Composable
private fun AvatarImage(
	actor: ActorProfile,
	size: Dp,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
			.size(size)
			.clip(CircleShape)
			.background(MaterialTheme.colorScheme.surfaceVariant),
		contentAlignment = Alignment.Center
	) {
		if (actor.profileImageUrl.isNotBlank()) {
			AsyncImage(
				model = actor.profileImageUrl,
				contentDescription = actor.displayName,
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop
			)
		} else {
			Text(
				text = actor.avatarEmoji.ifBlank { "👤" },
				fontSize = (size.value * 0.45f).sp
			)
		}
	}
}

private fun badgeIconForType(type: String): ImageVector {
	return when (type) {
		"like", "love", "reaction" -> Icons.Default.Favorite
		"comment" -> Icons.AutoMirrored.Filled.Comment
		"chat_message", "direct_message", "story_reply" -> Icons.AutoMirrored.Filled.Send
		"follow", "friend_request" -> Icons.Default.Person
		"mention" -> Icons.Default.Tag
		"share" -> Icons.Default.Share
		"story" -> Icons.Default.CameraAlt
		else -> Icons.Default.Notifications
	}
}

private fun badgeColorForType(type: String): Color {
	return when (type) {
		"like", "love", "reaction" -> Color(0xFFEF4444) // Red
		"comment" -> Color(0xFF8B5CF6) // Purple
		"chat_message", "direct_message", "story_reply" -> Color(0xFF3B82F6) // Blue
		"follow", "friend_request" -> Color(0xFF10B981) // Green
		"mention" -> Color(0xFFF59E0B) // Amber
		"share" -> Color(0xFF673AB7) // Indigo/Purple
		"story" -> Color(0xFFFF9800) // Deep Amber
		else -> Color(0xFF6B7280) // Gray
	}
}

private fun formatTimeAgo(ts: Long): String {
	if (ts <= 0L) return "just now"
	val mins = ((System.currentTimeMillis() - ts) / 60000L).coerceAtLeast(0L)
	return when {
		mins < 1 -> "just now"
		mins < 60 -> "${mins}m ago"
		mins < 1440 -> "${mins / 60}h ago"
		else -> "${mins / 1440}d ago"
	}
}
