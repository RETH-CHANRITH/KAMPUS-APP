package com.example.kampus.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import com.example.kampus.ui.theme.ThemeController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top bar for Create Post screen: Close button, title, theme toggle
 */
@Composable
internal fun CreatePostTopBar(
    p: ComposerPalette,
    onClose: () -> Unit,
    isDarkMode: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(p.card)
                .border(1.dp, p.border, CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = p.text, modifier = Modifier.size(18.dp))
        }

        Text(
            text = "New post",
            color = p.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(p.card)
                .border(1.dp, p.border, CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                    ThemeController.isDark = !ThemeController.isDark
                }),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isDarkMode) "🌙" else "☀️",
                color = p.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    HorizontalDivider(color = p.border.copy(alpha = 0.7f), thickness = 0.5.dp)
}

/**
 * Header row for Create Post: Avatar, name, tag summary, visibility selector
 */
@Composable
internal fun CreatePostHeader(
    p: ComposerPalette,
    taggedPeople: List<String>,
    locationText: String,
    feelingEmoji: String?,
    eventText: String = "",
    visibility: PostItem.PostVisibility,
    onVisibilityChange: (PostItem.PostVisibility) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(p.card)
                .border(1.dp, p.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("👤", fontSize = 18.sp)
        }

        // Name + tag summary
        Column(modifier = Modifier.weight(1f)) {
            Text("You", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            // Tag summary line (like Facebook)
            val tagSummary = buildString {
                append("You")
                if (taggedPeople.isNotEmpty()) {
                    append(", ${taggedPeople.take(2).joinToString(", ")}")
                    if (taggedPeople.size > 2) append(" +${taggedPeople.size - 2}")
                }
                if (locationText.isNotEmpty()) {
                    append(" 📍 $locationText")
                }
                if (feelingEmoji != null) {
                    append(" $feelingEmoji")
                }
                if (eventText.isNotEmpty()) {
                    append(" 🎉 $eventText")
                }
            }

            Text(
                tagSummary,
                color = p.textMuted,
                fontSize = 12.sp,
                maxLines = 2,
            )
        }

        // Visibility pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(p.card)
                .border(1.dp, p.border, RoundedCornerShape(999.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onVisibilityChange(
                            when (visibility) {
                                PostItem.PostVisibility.PUBLIC -> PostItem.PostVisibility.FRIENDS
                                PostItem.PostVisibility.FRIENDS -> PostItem.PostVisibility.FOLLOWERS
                                PostItem.PostVisibility.FOLLOWERS -> PostItem.PostVisibility.UNIVERSITY
                                PostItem.PostVisibility.UNIVERSITY -> PostItem.PostVisibility.PRIVATE
                                PostItem.PostVisibility.PRIVATE -> PostItem.PostVisibility.PUBLIC
                            }
                        )
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                when (visibility) {
                    PostItem.PostVisibility.PUBLIC -> "Public"
                    PostItem.PostVisibility.FRIENDS -> "Friends"
                    PostItem.PostVisibility.FOLLOWERS -> "Followers"
                    PostItem.PostVisibility.UNIVERSITY -> "University"
                    PostItem.PostVisibility.PRIVATE -> "Private"
                },
                color = p.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
