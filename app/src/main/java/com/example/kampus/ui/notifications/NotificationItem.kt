package com.example.kampus.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.domain.model.AppNotification

@Composable
fun NotificationItem(
	item: AppNotification,
	onClick: () -> Unit,
) {
	val colors = MaterialTheme.colorScheme
	val accent = accentColorForType(item.type)
	val bg = if (item.isRead) colors.surface else colors.primaryContainer.copy(alpha = 0.55f)

	Surface(
		modifier = Modifier
			.fillMaxWidth()
		.clip(RoundedCornerShape(20.dp))
		.clickable(onClick = onClick),
		color = bg,
		shape = RoundedCornerShape(20.dp),
		border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.10f)),
		tonalElevation = if (item.isRead) 0.dp else 1.dp,
		shadowElevation = if (item.isRead) 0.dp else 1.dp,
	) {
		Row(
			modifier = Modifier.padding(14.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.size(48.dp)
					.clip(CircleShape)
					.background(accent.copy(alpha = 0.12f)),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					imageVector = iconForType(item.type),
					contentDescription = null,
					tint = accent,
					modifier = Modifier.size(22.dp),
				)
				if (!item.isRead) {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(5.dp)
							.size(10.dp)
							.clip(CircleShape)
							.background(colors.primary),
					)
				}
			}

			Column(modifier = Modifier.weight(1f)) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						item.title,
						color = colors.onSurface,
						fontWeight = FontWeight.SemiBold,
						fontSize = 15.sp,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
					Text(
						formatTimeAgo(item.createdAt),
						color = colors.onSurfaceVariant,
						fontSize = 11.sp,
						maxLines = 1,
					)
				}
				Spacer(Modifier.size(4.dp))
				Text(
					item.body,
					color = colors.onSurfaceVariant,
					fontSize = 13.sp,
					lineHeight = 18.sp,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)
			}
		}
	}
}

private fun accentColorForType(type: String): Color {
	return when (type) {
		"like" -> Color(0xFFE91E63)
		"comment" -> Color(0xFF7C4DFF)
		"share" -> Color(0xFF0288D1)
		"follow", "friend_request" -> Color(0xFF2E7D32)
		"call", "call_invite" -> Color(0xFF1565C0)
		else -> Color(0xFF546E7A)
	}
}

private fun iconForType(type: String): ImageVector {
	return when (type) {
		"like" -> Icons.Outlined.FavoriteBorder
		"comment" -> Icons.Outlined.ChatBubbleOutline
		"share" -> Icons.Outlined.Share
		"follow", "friend_request" -> Icons.Outlined.PersonAdd
		"call", "call_invite" -> Icons.Outlined.ChatBubbleOutline
		else -> Icons.Outlined.ChatBubbleOutline
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

