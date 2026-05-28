package com.example.kampus.ui.profile

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.kampus.ui.localization.UiStrings
import com.example.kampus.ui.localization.rememberUiStrings
import com.example.kampus.ui.theme.ThemeController

private val FrIsDark get() = ThemeController.isDark
private val FrBg get() = if (FrIsDark) Color(0xFF1A1D2E) else Color(0xFFF3F4F8)
private val FrCard get() = if (FrIsDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
private val FrChip get() = if (FrIsDark) Color(0xFF3A3F54) else Color(0xFFE5E7EB)
private val FrBlue get() = ThemeController.accent.color
private val FrWhite get() = if (FrIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val FrGray get() = if (FrIsDark) Color(0xFF99A1AF) else Color(0xFF6B7280)
private val FrMuted get() = if (FrIsDark) Color(0xFF6A7282) else Color(0xFF9CA3AF)

@Composable
fun FriendRequestsScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = rememberUiStrings()
    var selectedTab by remember { mutableStateOf(0) }

    val incoming = state.friendRequests
    val outgoing = state.outgoingFriendRequests

    Surface(color = FrBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderRow(onBack = onBack, strings = strings)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TabChip(
                    title = "${strings.incoming} (${incoming.size})",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 },
                )
                TabChip(
                    title = "${strings.outgoing} (${outgoing.size})",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 },
                )
            }

            val list = if (selectedTab == 0) incoming else outgoing
            if (list.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTab == 0) strings.noIncomingRequests else strings.noOutgoingRequests,
                        color = FrGray,
                        fontSize = 16.sp,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { request ->
                        RequestCard(
                            item = request,
                            strings = strings,
                            isIncoming = selectedTab == 0,
                            onOpenProfile = onOpenProfile,
                            onAccept = { viewModel.acceptFriendRequest(request.id) },
                            onReject = { viewModel.rejectFriendRequest(request.id) },
                            onCancel = { viewModel.cancelFriendRequest(request.id) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(onBack: () -> Unit, strings: UiStrings) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (FrIsDark) Color.White.copy(alpha = 0.1f) else FrCard)
                .border(1.dp, if (FrIsDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD1D5DB), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = FrWhite)
        }
        Text(text = strings.friendRequestsTitle, color = FrWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TabChip(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) FrBlue else FrCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = if (selected) FrWhite else FrGray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RequestCard(
    item: com.example.kampus.domain.model.FriendRequest,
    strings: UiStrings,
    isIncoming: Boolean,
    onOpenProfile: (String) -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
) {
    val name = if (isIncoming) item.fromUserName else item.toUserName
    val handle = if (isIncoming) item.fromUserHandle else item.toUserHandle
    val mutualLabel = "${if (isIncoming) item.fromUserAvatar else item.toUserAvatar} ${strings.mutualFriendsLabel}"
    val avatarUrl = if (isIncoming) item.fromUserProfileImageUrl else item.toUserProfileImageUrl
    val requestLabel = if (isIncoming) strings.incomingRequest else strings.sentRequest
    val targetUserId = if (isIncoming) item.fromUserId else item.toUserId
    val isPending = item.status == com.example.kampus.domain.model.FriendRequestStatus.PENDING
    val statusLabel = when (item.status) {
        com.example.kampus.domain.model.FriendRequestStatus.PENDING -> strings.pending
        com.example.kampus.domain.model.FriendRequestStatus.ACCEPTED -> strings.accepted
        com.example.kampus.domain.model.FriendRequestStatus.REJECTED -> strings.rejected
        com.example.kampus.domain.model.FriendRequestStatus.BLOCKED -> strings.blocked
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FrCard)
            .border(1.dp, if (FrIsDark) Color.Transparent else Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = targetUserId.isNotBlank()) { onOpenProfile(targetUserId) },
        ) {
            RequestAvatar(
                name = name,
                avatarUrl = avatarUrl,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(text = name.ifBlank { strings.unknownUser }, color = FrWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(text = handle.ifBlank { "@unknown" }, color = FrGray, fontSize = 14.sp)
                Text(text = requestLabel, color = FrMuted, fontSize = 12.sp)
                Text(text = mutualLabel, color = FrBlue, fontSize = 12.sp)
                if (!isPending) {
                    Text(text = statusLabel, color = FrGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (isIncoming && isPending) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrBlue, contentColor = FrWhite),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(text = strings.accept, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onReject,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrChip, contentColor = FrWhite),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(text = strings.reject, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FrChip),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = FrWhite)
                }
            }
        } else if (!isIncoming && isPending) {
            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FrChip, contentColor = FrWhite),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = strings.cancelRequest, fontWeight = FontWeight.Medium)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FrChip)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = statusLabel, color = FrWhite, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun RequestAvatar(
    name: String,
    avatarUrl: String,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2FA7FF), Color(0xFF7C3AED)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase().ifBlank { "?" },
                color = FrWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}