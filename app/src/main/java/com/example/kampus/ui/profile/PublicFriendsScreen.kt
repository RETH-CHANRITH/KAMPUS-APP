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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import coil.compose.AsyncImage
import com.example.kampus.data.repository.UserRepositoryImpl
import com.example.kampus.domain.model.Friend
import com.example.kampus.ui.localization.rememberUiStrings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class PublicFriendItem(
    val userId: String,
    val name: String,
    val handle: String,
    val profileImageUrl: String,
    val isOnline: Boolean,
)

@Composable
fun PublicFriendsScreen(
    userId: String,
    initialTab: Int = 1, // 0=friends,1=followers,2=following
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val strings = rememberUiStrings()
    val repo = remember { UserRepositoryImpl(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()) }

    val followersRes by repo.getFollowers(userId).collectAsStateWithLifecycle(initialValue = Result.success(emptyList()))
    val followingRes by repo.getFollowing(userId).collectAsStateWithLifecycle(initialValue = Result.success(emptyList()))
    val friendsRes by repo.getFriends(userId).collectAsStateWithLifecycle(initialValue = Result.success(emptyList()))

    val followers = followersRes.getOrNull().orEmpty().map { it.toPublicFriendItem() }
    val following = followingRes.getOrNull().orEmpty().map { it.toPublicFriendItem() }
    val friends = friendsRes.getOrNull().orEmpty().map { it.toPublicFriendItem() }

    var selectedTab by remember { mutableStateOf(initialTab) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF3F4F8)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = strings.friendsLabel, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = when (selectedTab) {
                    0 -> "${friends.size}"
                    1 -> "${followers.size}"
                    else -> "${following.size}"
                }, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TabPill(title = "${strings.friendsLabel} (${friends.size})", selected = selectedTab == 0) { selectedTab = 0 }
                TabPill(title = "${strings.followersLabel} (${followers.size})", selected = selectedTab == 1) { selectedTab = 1 }
                TabPill(title = "${strings.followingLabel} (${following.size})", selected = selectedTab == 2) { selectedTab = 2 }
            }

            val list = when (selectedTab) { 0 -> friends; 1 -> followers; else -> following }

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.userId }) { item ->
                    PublicFriendRow(item = item, onOpenProfile = { onOpenProfile(item.userId) })
                }
            }
        }
    }
}

@Composable
private fun TabPill(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
        .background(if (selected) Color(0xFF0D7FFF) else Color(0xFFFFFFFF))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text = title, color = if (selected) Color.White else Color.Black)
    }
}

@Composable
private fun PublicFriendRow(item: PublicFriendItem, onOpenProfile: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenProfile)
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp)) {
            Box(modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2FA7FF), Color(0xFF7C3AED)))), contentAlignment = Alignment.Center) {
                if (item.profileImageUrl.isNotBlank()) {
                    AsyncImage(model = item.profileImageUrl, contentDescription = item.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Text(text = item.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Box(modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (item.isOnline) Color(0xFF39D98A) else Color(0xFF6B7280)))
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = item.name, fontWeight = FontWeight.SemiBold)
            Text(text = item.handle, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

private fun Friend.toPublicFriendItem(): PublicFriendItem {
    return PublicFriendItem(userId = userId, name = displayName.ifEmpty { handle }, handle = "@${handle}", profileImageUrl = profileImageUrl ?: "", isOnline = isOnline)
}
