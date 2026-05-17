package com.example.kampus.ui.notifications

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.domain.model.AppNotification

@Composable
fun NotificationItem(
	item: AppNotification,
	onClick: () -> Unit,
) {
	val colors = MaterialTheme.colorScheme
	val bg = if (item.isRead) colors.surface else colors.primary.copy(alpha = 0.16f)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(14.dp))
			.background(bg)
			.clickable(onClick = onClick)
			.padding(12.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.size(36.dp)
				.clip(CircleShape)
				.background(colors.surfaceVariant),
			contentAlignment = Alignment.Center,
		) {
			Icon(
				imageVector = iconForType(item.type),
				contentDescription = null,
				tint = colors.onSurface,
				modifier = Modifier.size(18.dp),
			)
		}

		Column(modifier = Modifier.weight(1f)) {
			Text(item.title, color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
			Spacer(Modifier.size(2.dp))
			Text(item.body, color = colors.onSurfaceVariant, fontSize = 13.sp)
			Spacer(Modifier.size(4.dp))
			Text(formatTimeAgo(item.createdAt), color = colors.onSurfaceVariant, fontSize = 11.sp)
		}
	}
}

private fun iconForType(type: String): ImageVector {
	return when (type) {
		"like" -> Icons.Outlined.FavoriteBorder
		"comment" -> Icons.Outlined.ChatBubbleOutline
		"share" -> Icons.Outlined.Share
		"follow", "friend_request" -> Icons.Outlined.PersonAdd
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

