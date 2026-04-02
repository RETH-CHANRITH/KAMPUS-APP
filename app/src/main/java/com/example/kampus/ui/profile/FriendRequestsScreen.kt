package com.example.kampus.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FrBg = Color(0xFF1A1F2E)
private val FrCard = Color(0xFF252A41)
private val FrChip = Color(0xFF3A3F54)
private val FrBlue = Color(0xFF0D7FFF)
private val FrWhite = Color(0xFFFFFFFF)
private val FrGray = Color(0xFF99A1AF)
private val FrMuted = Color(0xFF6A7282)

private data class FriendRequestItem(
    val name: String,
    val handle: String,
    val followers: String,
    val mutual: String,
)

@Composable
fun FriendRequestsScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val incoming = remember {
        listOf(
            FriendRequestItem("Jessica Lee", "@jessicalee", "2.4K Followers", "12 mutual friends"),
            FriendRequestItem("Marcus Johnson", "@marcusj", "1.8K Followers", "8 mutual friends"),
            FriendRequestItem("Sophia Chen", "@sophiachen", "3.2K Followers", "15 mutual friends"),
        )
    }
    val outgoing = remember {
        listOf(
            FriendRequestItem("Avery Cole", "@averycole", "970 Followers", "4 mutual friends"),
            FriendRequestItem("Noah Lim", "@noahlim", "1.2K Followers", "6 mutual friends"),
        )
    }

    Surface(color = FrBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderRow(onBack = onBack)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TabChip(
                    title = "Incoming (${incoming.size})",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 },
                )
                TabChip(
                    title = "Outgoing (${outgoing.size})",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 },
                )
            }

            val list = if (selectedTab == 0) incoming else outgoing
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(list) { request ->
                    RequestCard(item = request, isIncoming = selectedTab == 0)
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderRow(onBack: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = FrWhite)
        }
        Text(text = "Friend Requests", color = FrWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
private fun RequestCard(item: FriendRequestItem, isIncoming: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FrCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
                Text(
                    text = item.name.take(1),
                    color = FrWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(text = item.name, color = FrWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(text = item.handle, color = FrGray, fontSize = 14.sp)
                Text(text = item.followers, color = FrMuted, fontSize = 12.sp)
                Text(text = item.mutual, color = FrBlue, fontSize = 12.sp)
            }
        }

        if (isIncoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrBlue, contentColor = FrWhite),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(text = "Accept", fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrChip, contentColor = FrWhite),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(text = "Reject", fontWeight = FontWeight.Medium)
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
        } else {
            Button(
                onClick = {},
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FrChip, contentColor = FrWhite),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Cancel Request", fontWeight = FontWeight.Medium)
            }
        }
    }
}