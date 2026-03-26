package com.example.kampus.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.groups.GroupColors as C

/**
 * Compact single-row group item — used in horizontally scrolling
 * suggestion strips or search results where vertical space is tight.
 */
@Composable
fun GroupItem(
    group    : GroupData,
    isJoined : Boolean,
    onJoin   : () -> Unit,
    onClick  : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.Card)
            .border(1.dp, C.Border, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Cover circle ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(group.coverColor1, group.coverColor2))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(group.coverEmoji, fontSize = 24.sp)
        }

        // ── Info ──────────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text       = group.name,
                color      = C.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = group.category,
                color    = C.Gray3,
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Group,
                        contentDescription = null,
                        tint               = C.Gray3,
                        modifier           = Modifier.size(11.dp),
                    )
                    Text(group.members, color = C.Gray3, fontSize = 11.sp)
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        tint               = C.Gray3,
                        modifier           = Modifier.size(11.dp),
                    )
                    Text("${group.posts} posts", color = C.Gray3, fontSize = 11.sp)
                }
            }
        }

        // ── Join / Joined pill ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isJoined)
                        Brush.linearGradient(listOf(C.Card, C.Card))
                    else
                        Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                )
                .border(
                    width = 1.dp,
                    color = if (isJoined) C.Border else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onJoin,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = if (isJoined) "Joined" else "Join",
                color      = if (isJoined) C.Gray3 else C.White,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}