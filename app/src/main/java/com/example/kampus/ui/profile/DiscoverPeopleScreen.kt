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
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private enum class DiscoverTab { Suggested, New, All }

private data class DiscoverPerson(
    val userId: String,
    val name: String,
    val handle: String,
    val city: String,
    val followers: String,
    val mutualCount: Int,
    val isNew: Boolean,
    val imageUrl: String,
    val coverImageUrl: String,
    val isRequested: Boolean,
    val isFriend: Boolean,
)

private val DpBg = Color(0xFF1A1F2E)
private val DpCard = Color(0xFF252A41)
private val DpBlue = Color(0xFF0D7FFF)
private val DpMuted = Color(0xFF99A1AF)
private val DpWhite = Color.White
private val DpActionMuted = Color(0xFF3A3F54)

@Composable
fun DiscoverPeopleScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    var selected by remember { mutableStateOf(DiscoverTab.Suggested) }
    var allUsers by remember { mutableStateOf<List<DiscoverPerson>>(emptyList()) }
    var mutualCountByUser by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val profileState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentFriendIds = remember(profileState.friends) {
        profileState.friends.map { it.userId }.toSet()
    }
    val outgoingRequestIds = remember(profileState.outgoingFriendRequests) {
        profileState.outgoingFriendRequests.map { it.toUserId }.toSet()
    }

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect

        firestore.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                allUsers = snapshot.documents
                    .filter { it.id != userId }
                    .map { doc ->
                        val statsMap = doc.get("stats") as? Map<*, *>
                        val followersCount = (statsMap?.get("followers") as? Number)?.toLong() ?: 0L
                        val createdAtRaw = doc.get("createdAt")
                        val createdAtMs = when (createdAtRaw) {
                            is Number -> createdAtRaw.toLong()
                            is Timestamp -> createdAtRaw.toDate().time
                            else -> 0L
                        }
                        val isNewUser = createdAtMs > 0L && (System.currentTimeMillis() - createdAtMs) <= 7L * 24L * 60L * 60L * 1000L
                        val personId = doc.id

                        DiscoverPerson(
                            userId = personId,
                            name = doc.getString("displayName") ?: "",
                            handle = doc.getString("handle") ?: "",
                            city = doc.getString("location") ?: "",
                            followers = if (followersCount >= 1000L) "${followersCount / 1000L}K" else "$followersCount",
                            mutualCount = 0,
                            isNew = isNewUser,
                            imageUrl = doc.getString("profileImageUrl") ?: "",
                            coverImageUrl = doc.getString("coverImageUrl") ?: "",
                            isRequested = false,
                            isFriend = false,
                        )
                    }
                    .sortedByDescending { it.isNew }
            }
    }

    val decoratedUsers = remember(allUsers, currentFriendIds, outgoingRequestIds) {
        allUsers.map { person ->
            person.copy(
                isRequested = person.userId in outgoingRequestIds,
                isFriend = person.userId in currentFriendIds,
                mutualCount = mutualCountByUser[person.userId] ?: 0,
            )
        }
    }

    LaunchedEffect(allUsers, currentFriendIds) {
        val firestore = FirebaseFirestore.getInstance()
        val counts = mutableMapOf<String, Int>()

        allUsers.forEach { person ->
            val count = try {
                val docs = firestore.collection("users")
                    .document(person.userId)
                    .collection("friends")
                    .get()
                    .await()

                val targetFriendIds = docs.documents.map { it.id }.toSet()
                (targetFriendIds intersect currentFriendIds).size
            } catch (_: Exception) {
                0
            }

            counts[person.userId] = count
        }

        mutualCountByUser = counts
    }

    val visible = when (selected) {
        DiscoverTab.All -> decoratedUsers
        DiscoverTab.New -> decoratedUsers.filter { it.isNew }
        DiscoverTab.Suggested -> decoratedUsers.filter { !it.isFriend && !it.isRequested }
    }

    Surface(color = DpBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onBack = onBack)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DiscoverTabChip(
                    title = "Suggested",
                    icon = Icons.Outlined.StarOutline,
                    selected = selected == DiscoverTab.Suggested,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.Suggested },
                )
                DiscoverTabChip(
                    title = "New",
                    icon = Icons.Outlined.PersonAdd,
                    selected = selected == DiscoverTab.New,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.New },
                )
                DiscoverTabChip(
                    title = "All",
                    icon = Icons.Outlined.GridView,
                    selected = selected == DiscoverTab.All,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.All },
                )
            }

            Text(
                text = when (selected) {
                    DiscoverTab.Suggested -> "People you may know based on mutual friends and interests"
                    DiscoverTab.New -> "New members who recently joined the community"
                    DiscoverTab.All -> "Browse all users on the platform"
                },
                color = DpMuted,
                fontSize = 14.sp,
            )

            // 2-column grid layout for all tabs
            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No users to discover yet",
                        color = DpMuted,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                    items(visible.chunked(2)) { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            DiscoverCard(
                                person = rowItems[0],
                                modifier = Modifier.weight(1f),
                                onFollow = { viewModel.sendFriendRequest(rowItems[0].userId) },
                            )
                            if (rowItems.size > 1) {
                                DiscoverCard(
                                    person = rowItems[1],
                                    modifier = Modifier.weight(1f),
                                    onFollow = { viewModel.sendFriendRequest(rowItems[1].userId) },
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = DpWhite)
        }
        Text(text = "Discover People", color = DpWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiscoverTabChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DpBlue else DpCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) DpWhite else DpMuted, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            color = if (selected) DpWhite else DpMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun DiscoverCard(
    person: DiscoverPerson,
    modifier: Modifier = Modifier,
    onFollow: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DpCard),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Color(0xFF1E2740)),
        ) {
            if (person.coverImageUrl.isNotBlank()) {
                AsyncImage(
                    model = person.coverImageUrl,
                    contentDescription = "${person.name} cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.25f)),
                            ),
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFAD46FF), Color(0xFF2B7FFF), Color(0xFF9810FA)),
                            ),
                        )
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 56.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, DpCard, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (person.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = person.imageUrl,
                        contentDescription = person.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = person.name.trim().take(1).uppercase().ifBlank { "?" },
                        color = DpWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = person.name,
                    color = DpWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (person.isNew) {
                    Box(
                        modifier = Modifier
                            .height(19.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4ADE80))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "NEW",
                            color = DpBg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(text = person.handle, color = DpMuted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DpMuted, modifier = Modifier.size(14.dp))
                Text(text = person.city.ifBlank { "Unknown" }, color = DpMuted, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(text = person.followers, color = DpWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Followers", color = DpMuted, fontSize = 10.sp)
                }
                Column {
                    Text(text = "${person.mutualCount}", color = DpBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "friends", color = DpMuted, fontSize = 10.sp)
                }
            }

            Button(
                onClick = onFollow,
                enabled = !person.isRequested && !person.isFriend,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (person.isFriend || person.isRequested) DpActionMuted else DpBlue,
                    contentColor = DpWhite,
                    disabledContainerColor = DpActionMuted,
                    disabledContentColor = DpWhite,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val actionText = when {
                    person.isFriend -> "Friend"
                    person.isRequested -> "Requested"
                    else -> "Follow"
                }
                Text(text = actionText, fontWeight = FontWeight.Medium)
            }
        }
    }
}