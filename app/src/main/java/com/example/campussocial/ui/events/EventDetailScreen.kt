@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.events.EventColors as C

@Composable
fun EventDetailScreen(
    event        : EventItem,
    isInterested : Boolean,
    isLiked      : Boolean,
    isSaved      : Boolean,
    onInterested : () -> Unit,
    onLike       : () -> Unit,
    onSave       : () -> Unit,
    onBack       : () -> Unit,
) {
    val scrollState = rememberScrollState()

    // Hero parallax: shrink cover as user scrolls
    val coverHeight by animateDpAsState(
        targetValue   = if (scrollState.value > 200) 180.dp else 280.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label         = "cover_height",
    )

    Box(modifier = Modifier.fillMaxSize().background(C.Bg)) {

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

            // ── Hero cover ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .background(Brush.linearGradient(listOf(event.coverColor1, event.coverColor2))),
                contentAlignment = Alignment.Center,
            ) {
                Text(event.coverEmoji, fontSize = 96.sp)

                // bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg)))
                )

                // Category badge bottom-left
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(event.category.color.copy(alpha = 0.2f))
                            .border(1.dp, event.category.color.copy(0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(event.category.emoji, fontSize = 12.sp)
                        Text(
                            event.category.label.uppercase(),
                            color      = event.category.color,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Title + save
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top,
                ) {
                    Text(
                        event.title,
                        color      = C.White,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        modifier   = Modifier.weight(1f).padding(end = 12.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(C.Surface)
                            .border(1.dp, C.Border, CircleShape)
                            .clickable(remember { MutableInteractionSource() }, null, onClick = onSave),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            "Save",
                            tint     = if (isSaved) C.Blue else C.Gray3,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // ── Stat pills row ────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("👥", "${event.interested + if (isInterested) 1 else 0} interested")
                    StatPill("❤️", "${event.likes + if (isLiked) 1 else 0} likes")
                    StatPill("💬", "${event.comments} comments")
                }

                // ── Info card ─────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InfoRow(
                        icon  = Icons.Outlined.CalendarMonth,
                        title = "Date",
                        value = event.date,
                    )
                    HorizontalDivider(color = C.Border.copy(0.5f), thickness = 0.5.dp)
                    InfoRow(
                        icon  = Icons.Outlined.AccessTime,
                        title = "Time",
                        value = event.time,
                    )
                    HorizontalDivider(color = C.Border.copy(0.5f), thickness = 0.5.dp)
                    InfoRow(
                        icon  = Icons.Outlined.LocationOn,
                        title = "Location",
                        value = event.location,
                    )
                }

                // ── Organizer card ────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Organizer", color = C.Gray3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(C.Blue.copy(0.3f), C.Gray5.copy(0.4f)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(event.organizerEmoji, fontSize = 22.sp)
                        }
                        Column {
                            Text(event.organizer, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Posted ${event.organizerTime}", color = C.Gray4, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.BlueSoft.copy(alpha = 0.4f))
                                .border(1.dp, C.Blue.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Follow", color = C.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Description ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("About this Event", color = C.Gray3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        event.description,
                        color      = C.Gray2,
                        fontSize   = 14.sp,
                        lineHeight = 22.sp,
                    )
                }

                // ── Social actions ────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Like
                    Box(
                        modifier = Modifier
                            .weight(1f).height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLiked) C.Red.copy(0.15f) else C.Surface)
                            .border(1.dp, if (isLiked) C.Red.copy(0.4f) else C.Border, RoundedCornerShape(12.dp))
                            .clickable(remember { MutableInteractionSource() }, null, onClick = onLike),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                                tint = if (isLiked) C.Red else C.Gray3, modifier = Modifier.size(16.dp))
                            Text(
                                "${event.likes + if (isLiked) 1 else 0}",
                                color = if (isLiked) C.Red else C.Gray3,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    // Comment
                    Box(
                        modifier = Modifier
                            .weight(1f).height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Surface)
                            .border(1.dp, C.Border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = C.Gray3, modifier = Modifier.size(16.dp))
                            Text("${event.comments}", color = C.Gray3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // Share
                    Box(
                        modifier = Modifier
                            .weight(1f).height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Surface)
                            .border(1.dp, C.Border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Share, null, tint = C.Gray3, modifier = Modifier.size(16.dp))
                            Text("${event.shares}", color = C.Gray3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Fixed top-left back button ────────────────────────────────────────
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = C.White, modifier = Modifier.size(20.dp))
        }

        // ── Fixed bottom CTA ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg.copy(0.98f))))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isInterested)
                            Brush.linearGradient(listOf(C.BlueSoft, C.BlueSoft))
                        else
                            Brush.linearGradient(listOf(C.Blue, C.BlueGlow))
                    )
                    .border(1.dp, if (isInterested) C.Blue else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onInterested),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (isInterested) Icons.Filled.CheckCircle else Icons.Outlined.Stars,
                        null, tint = C.White, modifier = Modifier.size(18.dp)
                    )
                    Text(
                        if (isInterested) "You're Interested ✓" else "I'm Interested",
                        color      = C.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sub-composables
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatPill(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(C.Surface)
            .border(1.dp, C.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(label, color = C.Gray2, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.BlueSoft.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = C.Blue, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, color = C.Gray4, fontSize = 11.sp)
            Text(value, color = C.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}