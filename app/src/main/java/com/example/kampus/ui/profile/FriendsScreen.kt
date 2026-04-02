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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

private val FgBg = Color(0xFF1A1F2E)
private val FgCard = Color(0xFF252A41)
private val FgBlue = Color(0xFF0D7FFF)
private val FgText = Color.White
private val FgMuted = Color(0xFF99A1AF)
private val FgDanger = Color(0xFFFF4458)

private data class FriendItem(
    val id: Int,
    val name: String,
    val handle: String,
    val mutualFriends: String,
)

@Composable
fun FriendsScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val friends = remember {
        mutableStateListOf(
            FriendItem(1, "Sarah Johnson", "@sarahjohnson", "12 mutual friends"),
            FriendItem(2, "Mike Chen", "@mikechen", "8 mutual friends"),
            FriendItem(3, "Emma Davis", "@emmadavis", "10 mutual friends"),
            FriendItem(4, "Alex Martinez", "@alexmartinez", "7 mutual friends"),
            FriendItem(5, "Luna Smith", "@lunasmith", "9 mutual friends"),
            FriendItem(6, "David Wilson", "@davidwilson", "6 mutual friends"),
        )
    }
    val followers = remember {
        mutableStateListOf(
            FriendItem(101, "Olivia Brown", "@oliviabrown", "5 mutual friends"),
            FriendItem(102, "James Taylor", "@jamestaylor", "3 mutual friends"),
        )
    }

    Surface(color = FgBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onBack = onBack)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TabButton(
                    title = "Friends (${friends.size})",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 },
                )
                TabButton(
                    title = "Followers (${followers.size})",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 },
                )
            }

            val list = if (selectedTab == 0) friends else followers
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(items = list, key = { it.id }) { item ->
                    if (selectedTab == 0) {
                        FriendRow(
                            item = item,
                            onUnfriend = { friends.removeAll { it.id == item.id } },
                            onBlock = { friends.removeAll { it.id == item.id } },
                        )
                    } else {
                        FollowerRow(
                            item = item,
                            onFollowBack = { followers.removeAll { it.id == item.id } },
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
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
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = FgText)
        }
        Text(text = "Friends", color = FgText, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FgBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = FgText)
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) FgBlue else FgCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = if (selected) FgText else FgMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FriendRow(item: FriendItem, onUnfriend: () -> Unit, onBlock: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FgCard)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2FA7FF), Color(0xFF7C3AED)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.name.take(1), color = FgText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = item.name, color = FgText, fontSize = 18.sp)
            Text(text = item.handle, color = FgMuted, fontSize = 14.sp)
            Text(text = item.mutualFriends, color = FgBlue, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FgBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = FgText, modifier = Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable { showMenu = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = FgMuted)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Color(0xFF1E2235),
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Unfriend", color = Color(0xFFD1D5DC)) },
                    onClick = {
                        showMenu = false
                        onUnfriend()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = "Block", color = FgDanger) },
                    onClick = {
                        showMenu = false
                        onBlock()
                    },
                )
            }
        }
    }
}

@Composable
private fun FollowerRow(item: FriendItem, onFollowBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FgCard)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2FA7FF), Color(0xFF7C3AED)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.name.take(1), color = FgText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = item.name, color = FgText, fontSize = 18.sp)
            Text(text = item.handle, color = FgMuted, fontSize = 14.sp)
            Text(text = item.mutualFriends, color = FgBlue, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(FgBlue)
                .clickable(onClick = onFollowBack)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Follow Back", color = FgText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}