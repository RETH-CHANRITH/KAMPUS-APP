@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import com.example.kampus.data.repository.EventComment
import com.example.kampus.ui.events.EventColors as C
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: EventItem,
    isInterested: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    onInterested: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: EventViewModel,
    openComposer: Boolean = false,
) {
    val strings = com.example.kampus.ui.localization.rememberUiStrings()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var organizerName by remember { mutableStateOf(event.organizer) }
    var organizerAvatar by remember { mutableStateOf(event.organizerEmoji) }
    var commentText by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var showComposer by remember { mutableStateOf(openComposer) }
    var commentImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val commentMediaPickers = rememberMediaPickers(onPhotoSelected = { commentImageUri = it })

    var commentsState by remember { mutableStateOf<List<EventComment>>(emptyList()) }
    // Collect comments from repository and keep a local copy so we can perform
    // optimistic updates (e.g., toggling likes) for a smooth UX.
    LaunchedEffect(event.remoteId) {
        if (event.remoteId.isBlank()) return@LaunchedEffect
        viewModel.observeComments(event.remoteId).collect { result ->
            commentsState = result.getOrDefault(emptyList())
        }
    }

    val loveScale by animateFloatAsState(
        targetValue = if (isLiked) 1.06f else 1f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "love_scale",
    )
    val saveScale by animateFloatAsState(
        targetValue = if (isSaved) 1.06f else 1f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "save_scale",
    )
    val interestScale by animateFloatAsState(
        targetValue = if (isInterested) 1.03f else 1f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "interest_scale",
    )
    val interestBackground: Color = if (isInterested) C.BlueSoft else C.Blue

    LaunchedEffect(event.organizer) {
        if (event.organizer.isNotBlank()) {
            val doc = FirebaseFirestore.getInstance().collection("users").document(event.organizer).get().await()
            if (doc.exists()) {
                organizerName = doc.getString("displayName") ?: "User"
                organizerAvatar = doc.getString("avatarEmoji") ?: "🙋"
            }
        }
    }

    val coverHeight by animateDpAsState(
        targetValue = if (scrollState.value > 200) 180.dp else 280.dp,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "cover_height",
    )

    Box(modifier = Modifier.fillMaxSize().background(C.Bg)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .background(Brush.linearGradient(listOf(event.coverColor1, event.coverColor2))),
                contentAlignment = Alignment.Center,
            ) {
                if (event.coverImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = event.coverImageUrl,
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(event.coverEmoji, fontSize = 96.sp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg)))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                ) {
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
                            color = event.category.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        event.title,
                        color = C.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(C.Surface)
                            .border(1.dp, C.Border, CircleShape)
                            .graphicsLayer(scaleX = saveScale, scaleY = saveScale)
                            .clickable(onClick = onSave),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) C.Blue else C.Gray3,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("👥", "${event.interested} ${strings.interestedLabel}")
                    StatPill("❤️", "${event.likes} ${strings.likesLabel}")
                    StatPill("💬", "${event.comments} ${strings.commentsLabel}")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TabPill(label = "Details", active = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
                    TabPill(label = "Comments", active = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
                }

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(C.Card)
                            .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        InfoRow(icon = Icons.Outlined.CalendarMonth, title = strings.dateLabel, value = event.date)
                        HorizontalDivider(color = C.Border.copy(0.5f), thickness = 0.5.dp)
                        InfoRow(icon = Icons.Outlined.Schedule, title = strings.timeLabel, value = event.time)
                        HorizontalDivider(color = C.Border.copy(0.5f), thickness = 0.5.dp)
                        InfoRow(icon = Icons.Outlined.LocationOn, title = strings.locationLabel, value = event.location)
                    }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(strings.organizer, color = C.Gray3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(C.Blue.copy(0.3f), C.Gray5.copy(0.4f)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(organizerAvatar, fontSize = 22.sp)
                        }
                        Column {
                            Text(organizerName, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${strings.posted} ${event.organizerTime}", color = C.Gray4, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.BlueSoft.copy(alpha = 0.4f))
                                .border(1.dp, C.Blue.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(strings.follow, color = C.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    C.Card,
                                    C.Surface.copy(alpha = 0.62f)
                                )
                            )
                        )
                        .border(1.dp, C.Border.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.BlueSoft.copy(alpha = 0.28f))
                                .border(1.dp, C.Blue.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("📝", fontSize = 14.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(strings.aboutThisEvent.uppercase(), color = C.Gray4, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Text("Event overview", color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.Bg.copy(alpha = 0.55f))
                            .border(1.dp, C.Border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            event.description,
                            color = C.Gray2,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                // Extended Event Details - Professional Card Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    C.Card,
                                    C.Surface.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .border(1.dp, C.Blue.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.BlueSoft.copy(alpha = 0.4f))
                                .border(1.dp, C.Blue.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = C.Blue, modifier = Modifier.size(16.dp))
                        }
                        Text("Event Details", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp)
                    }

                    // 2-Column Grid for key details
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Row 1: Type & Capacity
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailItem(
                                icon = Icons.Outlined.Category,
                                label = "Type",
                                value = event.eventType.ifBlank { "Not specified" },
                                modifier = Modifier.weight(1f)
                            )
                            DetailItem(
                                icon = Icons.Outlined.People,
                                label = "Capacity",
                                value = if (event.capacity > 0) "${event.capacity} people" else "Unlimited",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2: Deadline & Format
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailItem(
                                icon = Icons.AutoMirrored.Outlined.EventNote,
                                label = "Deadline",
                                value = event.registrationDeadline.ifBlank { "Open" },
                                modifier = Modifier.weight(1f)
                            )
                            DetailItem(
                                icon = if (event.onlineEvent) Icons.Outlined.Public else Icons.Outlined.LocationOn,
                                label = "Format",
                                value = if (event.onlineEvent) "Online" else "In-person",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = C.Border.copy(alpha = 0.3f), thickness = 1.dp)

                    // Status Badges Row
                    if (event.certificateAvailable || event.paidEvent || event.allowGuest) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Highlights", color = C.Gray3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (event.certificateAvailable) {
                                    BadgeChip(emoji = "🏆", label = "Certificate", backgroundColor = Color(0xFFAB7C1F).copy(alpha = 0.14f), textColor = Color(0xFFCD9F26))
                                }
                                if (event.paidEvent) {
                                    BadgeChip(emoji = "💰", label = "Paid", backgroundColor = Color(0xFF22C55E).copy(alpha = 0.14f), textColor = Color(0xFF22C55E))
                                }
                                if (event.allowGuest) {
                                    BadgeChip(emoji = "➕", label = "Guests OK", backgroundColor = Color(0xFFEC4899).copy(alpha = 0.14f), textColor = Color(0xFFEC4899))
                                }
                            }
                        }
                    }

                    // Speaker
                    if (event.speaker.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(C.BlueSoft.copy(alpha = 0.25f))
                                    .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = C.Blue, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Featured Speaker", color = C.Gray3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(event.speaker, color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Website/Registration Link
                    if (event.website.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(C.Surface.copy(alpha = 0.8f), C.Surface.copy(alpha = 0.4f))))
                                .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { }
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(C.BlueSoft.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Link, contentDescription = null, tint = C.Blue, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Registration Link", color = C.Gray3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(event.website, color = C.Blue, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = C.Blue, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Tags
                    if (event.tags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Topics", color = C.Gray3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                event.tags.forEach { tag ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(C.BlueSoft.copy(alpha = 0.15f))
                                            .border(1.dp, C.Blue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("#", color = C.Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(tag, color = C.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                }

                if (selectedTab == 1) {
                // Inline comments section (Facebook-style thread)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(C.Card)
                        .border(1.dp, C.Border, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val replyingToName = commentsState
                        .firstOrNull { it.id == replyingToCommentId }
                        ?.authorName

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Comments", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Realtime replies for this event", color = C.Gray3, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(C.BlueSoft.copy(alpha = 0.16f))
                                .border(1.dp, C.Blue.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = C.Blue, modifier = Modifier.size(14.dp))
                            Text("${commentsState.size}", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (replyingToName != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(C.BlueSoft.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(replyingToName.firstOrNull()?.toString() ?: "U", color = C.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Replying to $replyingToName", color = C.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "Cancel",
                                color = C.Gray3,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { replyingToCommentId = null },
                            )
                        }
                    }

                                    val keyboardController = LocalSoftwareKeyboardController.current
                                    LaunchedEffect(showComposer) {
                                        if (showComposer) {
                                            try { focusRequester.requestFocus() } catch (_: Exception) {}
                                            keyboardController?.show()
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = showComposer,
                                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                                    ) {
                                        Surface(
                                            color = C.Surface,
                                            shape = RoundedCornerShape(20.dp),
                                            tonalElevation = 8.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                                .border(1.dp, if (replyingToCommentId != null) C.Blue.copy(alpha = 0.35f) else C.Border, RoundedCornerShape(20.dp)),
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterHorizontally)
                                                        .size(width = 42.dp, height = 4.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(C.Gray4.copy(alpha = 0.55f)),
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            if (replyingToCommentId == null) "Start a comment" else "Write a reply",
                                                            color = C.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                        Text(
                                                            "Clean, fast and photo-ready",
                                                            color = C.Gray3,
                                                            fontSize = 11.sp,
                                                        )
                                                    }
                                                    Text(
                                                        if (commentImageUri == null) "Text or photo" else "1 photo ready",
                                                        color = if (commentImageUri == null) C.Gray4 else C.Blue,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(if (commentImageUri == null) C.Bg else C.BlueSoft.copy(alpha = 0.14f))
                                                            .border(1.dp, if (commentImageUri == null) C.Border else C.Blue.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    )
                                                }

                                                if (replyingToCommentId != null) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .clip(CircleShape)
                                                                .background(C.BlueSoft.copy(alpha = 0.3f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text(replyingToName?.firstOrNull()?.toString() ?: "U", fontSize = 14.sp)
                                                        }

                                                        Spacer(Modifier.width(8.dp))
                                                        Text("Replying to ${replyingToName}", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                        Spacer(Modifier.weight(1f))
                                                        Text(
                                                            "Cancel",
                                                            color = C.Gray3,
                                                            modifier = Modifier.clickable {
                                                                replyingToCommentId = null
                                                                showComposer = false
                                                            },
                                                        )
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = commentText,
                                                    onValueChange = { commentText = it },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(focusRequester),
                                                    placeholder = {
                                                        Text(
                                                            if (replyingToName != null) "Write a reply..." else "Write a comment...",
                                                            color = C.Gray4,
                                                        )
                                                    },
                                                    minLines = 2,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = C.Blue,
                                                        unfocusedBorderColor = C.Border,
                                                        focusedContainerColor = C.Bg,
                                                        unfocusedContainerColor = C.Bg,
                                                        cursorColor = C.Blue,
                                                        focusedTextColor = C.White,
                                                        unfocusedTextColor = C.White,
                                                    ),
                                                )

                                                if (commentImageUri != null) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(C.Bg)
                                                            .border(1.dp, C.Blue.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    ) {
                                                        AsyncImage(
                                                            model = commentImageUri,
                                                            contentDescription = "Selected comment image",
                                                            modifier = Modifier
                                                                .size(84.dp)
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .shadow(6.dp, RoundedCornerShape(14.dp)),
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("Photo attached", color = C.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                            Text("Ready to post with your comment", color = C.Gray3, fontSize = 11.sp)
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove photo",
                                                            tint = C.Gray3,
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .clickable { commentImageUri = null },
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .height(44.dp)
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(C.Bg)
                                                            .border(1.dp, C.Blue.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                                                            .clickable { commentMediaPickers.pickPhoto() }
                                                            .padding(horizontal = 12.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Icon(Icons.Outlined.Image, contentDescription = null, tint = C.Blue, modifier = Modifier.size(16.dp))
                                                            Text("Photo", color = C.Gray2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }

                                                    Button(
                                                        onClick = {
                                                            val text = commentText.trim()
                                                            if (text.isEmpty()) return@Button

                                                            scope.launch {
                                                                val result = if (replyingToCommentId == null) {
                                                                    viewModel.addComment(event, text, commentImageUri)
                                                                } else {
                                                                    viewModel.addReply(event, replyingToCommentId.orEmpty(), text, commentImageUri)
                                                                }

                                                                if (result.isSuccess) {
                                                                    commentText = ""
                                                                    replyingToCommentId = null
                                                                    commentImageUri = null
                                                                    showComposer = false
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = C.White),
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Text(if (replyingToCommentId == null) "Post Comment" else "Post Reply", fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    Button(
                                                        onClick = {
                                                            replyingToCommentId = null
                                                            commentImageUri = null
                                                            showComposer = false
                                                        },
                                                        modifier = Modifier.padding(top = 4.dp).height(44.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = C.Surface, contentColor = C.Gray3),
                                                        shape = RoundedCornerShape(12.dp),
                                                    ) {
                                                        Text("Close")
                                                    }
                                                }
                                            }
                                        }
                                    }

                    // If opened with composer flag, scroll to comments and focus the text field
                    LaunchedEffect(openComposer) {
                        if (openComposer) {
                            replyingToCommentId = null
                            scope.launch {
                                try {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                } catch (_: Exception) {}
                                focusRequester.requestFocus()
                            }
                        }
                    }

                    if (commentsState.isEmpty()) {
                        Text("Be the first to comment.", color = C.Gray3, fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(commentsState) { comment ->
                                    CommentRow(
                                        comment = comment,
                                        isOrganizer = comment.authorId == event.organizer,
                                        onReply = {
                                            replyingToCommentId = comment.id
                                            // open composer when user taps Reply
                                            showComposer = true
                                            scope.launch {
                                                try { scrollState.animateScrollTo(scrollState.maxValue) } catch (_: Exception) {}
                                                focusRequester.requestFocus()
                                            }
                                        },
                                        onToggleLike = {
                                            // Optimistic UI update: toggle locally first
                                            val previous = commentsState
                                            commentsState = commentsState.map { c ->
                                                if (c.id == comment.id) c.copy(
                                                    likedByCurrentUser = !c.likedByCurrentUser,
                                                    likesCount = if (c.likedByCurrentUser) c.likesCount - 1 else c.likesCount + 1,
                                                ) else c
                                            }

                                            // Fire the network update; revert on failure
                                            scope.launch {
                                                val res = viewModel.toggleCommentLike(event, comment.id)
                                                if (res.isFailure) {
                                                    commentsState = previous
                                                }
                                            }
                                        },
                                    )

                                comment.replies.forEach { reply ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp),
                                    ) {
                                        CommentRow(
                                            comment = reply,
                                            isOrganizer = reply.authorId == event.organizer,
                                            isReply = true,
                                            onReply = {
                                                replyingToCommentId = comment.id
                                                showComposer = true
                                                scope.launch {
                                                    try { scrollState.animateScrollTo(scrollState.maxValue) } catch (_: Exception) {}
                                                    focusRequester.requestFocus()
                                                }
                                            },
                                            onToggleLike = {
                                                // Optimistic update for replies
                                                val previous = commentsState
                                                commentsState = commentsState.map { parent ->
                                                    if (parent.id == comment.id) parent else parent.copy(
                                                        replies = parent.replies.map {
                                                            if (it.id == reply.id) it.copy(
                                                                likedByCurrentUser = !it.likedByCurrentUser,
                                                                likesCount = if (it.likedByCurrentUser) it.likesCount - 1 else it.likesCount + 1,
                                                            ) else it
                                                        }
                                                    )
                                                }

                                                scope.launch {
                                                    val res = viewModel.toggleCommentLike(event, reply.id)
                                                    if (res.isFailure) commentsState = previous
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLiked) C.Red.copy(0.15f) else C.Surface)
                            .border(1.dp, if (isLiked) C.Red.copy(0.4f) else C.Border, RoundedCornerShape(12.dp))
                            .graphicsLayer(scaleX = loveScale, scaleY = loveScale)
                            .clickable(onClick = onLike),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (isLiked) C.Red else C.Gray3, modifier = Modifier.size(16.dp))
                            Text("Love", color = if (isLiked) C.Red else C.Gray3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Surface)
                            .border(1.dp, C.Border, RoundedCornerShape(12.dp))
                            .clickable {
                                replyingToCommentId = null
                                showComposer = true
                                scope.launch {
                                    try { scrollState.animateScrollTo(scrollState.maxValue) } catch (_: Exception) {}
                                    focusRequester.requestFocus()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = C.Gray3, modifier = Modifier.size(16.dp))
                            Text("${event.comments}", color = C.Gray3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Surface)
                            .border(1.dp, C.Border, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.shareEvent(event)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, event.title)
                                    putExtra(Intent.EXTRA_TEXT, "Check out this event on Kampus: ${event.title}\n${event.description}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share event"))
                            },
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

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.White, modifier = Modifier.size(20.dp))
        }

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
                    .background(interestBackground)
                    .border(1.dp, if (isInterested) C.Blue else Color.Transparent, RoundedCornerShape(16.dp))
                    .graphicsLayer(scaleX = interestScale, scaleY = interestScale)
                    .clickable { onInterested() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (isInterested) Icons.Filled.CheckCircle else Icons.Outlined.Stars, null, tint = C.White, modifier = Modifier.size(18.dp))
                    Text(
                        if (isInterested) "${strings.youreInterested} ✓" else strings.imInterested,
                        color = C.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

    }
}

@Composable
private fun StatPill(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(C.Surface)
            .border(1.dp, C.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(label, color = C.Gray2, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
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

@Composable
private fun CommentRow(
    comment: EventComment,
    isOrganizer: Boolean = false,
    isReply: Boolean = false,
    onReply: () -> Unit,
    onToggleLike: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(if (isReply) C.Bg else C.Surface)
            .border(1.dp, if (isReply) C.Blue.copy(alpha = 0.22f) else C.Border.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isReply) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.verticalGradient(listOf(C.BlueSoft.copy(alpha = 0.9f), C.Blue.copy(alpha = 0.45f)))),
            )
        }

        Box(
            modifier = Modifier
                .size(if (isReply) 32.dp else 40.dp)
                .clip(CircleShape)
                .background(if (isReply) C.BlueSoft.copy(alpha = 0.24f) else C.BlueSoft.copy(alpha = 0.36f))
                .border(1.dp, if (isReply) C.Blue.copy(alpha = 0.18f) else C.Blue.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (comment.authorProfileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = comment.authorProfileImageUrl,
                    contentDescription = comment.authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(comment.authorEmoji, fontSize = if (isReply) 14.sp else 16.sp)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(comment.authorName, color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (isOrganizer) {
                    Text(
                        "ORGANIZER",
                        color = Color(0xFF8B7BFF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF8B7BFF).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFF8B7BFF).copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                if (isReply) {
                    Text(
                        "Reply",
                        color = C.Blue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(C.BlueSoft.copy(alpha = 0.14f))
                            .border(1.dp, C.Blue.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(relativeTime(comment.createdAt), color = C.Gray4, fontSize = 11.sp)
            Text(comment.text, color = C.Gray2, fontSize = 13.sp, lineHeight = 18.sp)

            if (!comment.imageUrl.isNullOrBlank()) {
                var showImageDialog by remember { mutableStateOf(false) }

                AsyncImage(
                    model = comment.imageUrl,
                    contentDescription = "Comment image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, C.Blue.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                        .clickable { showImageDialog = true },
                    contentScale = ContentScale.Crop,
                )

                if (showImageDialog) {
                    Dialog(onDismissRequest = { showImageDialog = false }) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)) {
                            AsyncImage(
                                model = comment.imageUrl,
                                contentDescription = "Full comment image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (comment.likedByCurrentUser) C.Red.copy(alpha = 0.12f) else C.Bg)
                        .border(1.dp, if (comment.likedByCurrentUser) C.Red.copy(alpha = 0.35f) else C.Border.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onToggleLike)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(if (comment.likedByCurrentUser) "❤️" else "🤍", fontSize = 11.sp)
                    Text(
                        "${comment.likesCount}",
                        color = if (comment.likedByCurrentUser) C.Red else C.Gray3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!isReply) {
                    Text(
                        text = "Reply",
                        color = C.Blue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(C.BlueSoft.copy(alpha = 0.10f))
                            .border(1.dp, C.Blue.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                            .clickable(onClick = onReply)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private fun relativeTime(timestamp: Long?): String {
    if (timestamp == null) return "now"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        diff < 60_000 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d"
    }
}

@Composable
private fun DetailBadge(icon: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .border(1.dp, C.Border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 14.sp)
        Text(label, color = C.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BadgeChip(emoji: String, label: String, backgroundColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TabPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (active) {
                    Modifier.background(Brush.linearGradient(listOf(C.Blue.copy(alpha = 0.95f), Color(0xFF8B5CF6))))
                } else {
                    Modifier.background(C.Bg)
                }
            )
            .border(1.dp, if (active) Color.Transparent else C.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) C.White else C.Gray3,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(C.Surface.copy(alpha = 0.7f), C.Surface.copy(alpha = 0.4f))))
            .border(1.dp, C.Border.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = C.Blue, modifier = Modifier.size(18.dp))
            Text(label, color = C.Gray3, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
        Text(value, color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
