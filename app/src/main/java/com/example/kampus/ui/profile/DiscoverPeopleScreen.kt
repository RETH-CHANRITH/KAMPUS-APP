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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.kampus.ui.localization.UiStrings
import com.example.kampus.ui.localization.rememberUiStrings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.kampus.ui.theme.ThemeController
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class DiscoverTab { Suggested, New, All }

private data class DiscoverPerson(
    val userId: String,
    val name: String,
    val handle: String,
    val city: String,
    val followers: Long,
    val following: Long,
    val mutualCount: Int,
    val isNew: Boolean,
    val imageUrl: String,
    val coverImageUrl: String,
    val isRequested: Boolean,
    val isFriend: Boolean,
)

private data class SocialMetrics(
    val followersCount: Long,
    val followingCount: Long,
    val mutualCount: Int,
)

private val DpIsDark get() = ThemeController.isDark
private val DpBg get() = if (DpIsDark) Color(0xFF1A1D2E) else Color(0xFFF3F4F8)
private val DpCard get() = if (DpIsDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
private val DpBlue get() = Color(0xFF0D7FFF)
private val DpMuted get() = if (DpIsDark) Color(0xFF99A1AF) else Color(0xFF6B7280)
private val DpTextPrimary get() = if (DpIsDark) Color.White else Color(0xFF111827)
private val DpTextSecondary get() = if (DpIsDark) Color(0xFFD1D5DC) else Color(0xFF374151)
private val DpWhite = Color.White
private val DpActionMuted get() = if (DpIsDark) Color(0xFF3A3F54) else Color(0xFFE5E7EB)

@Composable
fun DiscoverPeopleScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    var selected by remember { mutableStateOf(DiscoverTab.Suggested) }
    var allUsers by remember { mutableStateOf<List<DiscoverPerson>>(emptyList()) }
    var mutualCountByUser by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var followerCountByUser by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var followingCountByUser by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val profileState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val strings = rememberUiStrings()

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
                        val followingCount = (statsMap?.get("following") as? Number)?.toLong() ?: 0L
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
                            followers = followersCount,
                            following = followingCount,
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
            val liveFollowerCount = followerCountByUser[person.userId] ?: person.followers
            val liveFollowingCount = followingCountByUser[person.userId] ?: person.following
            person.copy(
                isRequested = person.userId in outgoingRequestIds,
                isFriend = person.userId in currentFriendIds,
                mutualCount = mutualCountByUser[person.userId] ?: 0,
                followers = liveFollowerCount,
                following = liveFollowingCount,
            )
        }
    }

    LaunchedEffect(allUsers, currentFriendIds) {
        val firestore = FirebaseFirestore.getInstance()
        val counts = mutableMapOf<String, Int>()
        val followerCounts = mutableMapOf<String, Long>()
        val followingCounts = mutableMapOf<String, Long>()

        allUsers.forEach { person ->
            val metrics = loadSocialMetrics(
                firestore = firestore,
                targetUserId = person.userId,
                currentFriendIds = currentFriendIds,
            )

            if (metrics != null) {
                counts[person.userId] = metrics.mutualCount
                followerCounts[person.userId] = metrics.followersCount
                followingCounts[person.userId] = metrics.followingCount
            }
        }

        mutualCountByUser = counts
        followerCountByUser = followerCounts
        followingCountByUser = followingCounts
    }

    DisposableEffect(allUsers, currentFriendIds) {
        val firestore = FirebaseFirestore.getInstance()
        val registrations = mutableListOf<ListenerRegistration>()

        allUsers.forEach { person ->
            val followersListener = firestore.collection("users")
                .document(person.userId)
                .collection("followers")
                .addSnapshotListener { _, _ ->
                    scope.launch {
                        val metrics = loadSocialMetrics(
                            firestore = firestore,
                            targetUserId = person.userId,
                            currentFriendIds = currentFriendIds,
                        )
                        if (metrics != null) {
                            followerCountByUser = followerCountByUser + (person.userId to metrics.followersCount)
                            followingCountByUser = followingCountByUser + (person.userId to metrics.followingCount)
                            mutualCountByUser = mutualCountByUser + (person.userId to metrics.mutualCount)
                        }
                    }
                }

            val followingListener = firestore.collection("users")
                .document(person.userId)
                .collection("following")
                .addSnapshotListener { _, _ ->
                    scope.launch {
                        val metrics = loadSocialMetrics(
                            firestore = firestore,
                            targetUserId = person.userId,
                            currentFriendIds = currentFriendIds,
                        )
                        if (metrics != null) {
                            followingCountByUser = followingCountByUser + (person.userId to metrics.followingCount)
                            mutualCountByUser = mutualCountByUser + (person.userId to metrics.mutualCount)
                        }
                    }
                }

            registrations += followersListener
            registrations += followingListener
        }

        onDispose {
            registrations.forEach { it.remove() }
        }
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
            Header(onBack = onBack, strings = strings)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DiscoverTabChip(
                    title = strings.suggested,
                    icon = Icons.Outlined.StarOutline,
                    selected = selected == DiscoverTab.Suggested,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.Suggested },
                )
                DiscoverTabChip(
                    title = strings.newest,
                    icon = Icons.Outlined.PersonAdd,
                    selected = selected == DiscoverTab.New,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.New },
                )
                DiscoverTabChip(
                    title = strings.all,
                    icon = Icons.Outlined.GridView,
                    selected = selected == DiscoverTab.All,
                    modifier = Modifier.weight(1f),
                    onClick = { selected = DiscoverTab.All },
                )
            }

            Text(
                text = when (selected) {
                    DiscoverTab.Suggested -> strings.discoverSuggestedDesc
                    DiscoverTab.New -> strings.discoverNewDesc
                    DiscoverTab.All -> strings.discoverAllDesc
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
                        text = strings.noUsersToDiscoverYet,
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
                                strings = strings,
                                modifier = Modifier.weight(1f),
                                onOpenProfile = { onOpenProfile(rowItems[0].userId) },
                                onFollow = { viewModel.sendFriendRequest(rowItems[0].userId) },
                            )
                            if (rowItems.size > 1) {
                                DiscoverCard(
                                    person = rowItems[1],
                                    strings = strings,
                                    modifier = Modifier.weight(1f),
                                    onOpenProfile = { onOpenProfile(rowItems[1].userId) },
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
private fun Header(onBack: () -> Unit, strings: UiStrings) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (DpIsDark) Color.White.copy(alpha = 0.1f) else DpCard)
                .border(1.dp, if (DpIsDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD1D5DB), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = DpTextPrimary)
        }
        Text(text = strings.discoverPeople, color = DpTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
    strings: UiStrings,
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit,
    onFollow: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DpCard)
            .clickable(onClick = onOpenProfile),
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
                        color = DpTextPrimary,
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
                    color = DpTextPrimary,
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
                            text = strings.newest.uppercase(),
                            color = DpBg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(text = person.handle, color = DpTextSecondary, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DpTextSecondary, modifier = Modifier.size(14.dp))
                Text(text = person.city.ifBlank { strings.unknownLocation }, color = DpTextSecondary, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(text = formatFollowerCount(person.followers), color = DpTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = strings.followersLabel, color = DpTextSecondary, fontSize = 10.sp)
                }
                Column {
                    Text(text = formatFollowerCount(person.following), color = DpBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = strings.followingLabel, color = DpTextSecondary, fontSize = 10.sp)
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
                    person.isFriend -> strings.friend
                    person.isRequested -> strings.requested
                    else -> strings.follow
                }
                Text(text = actionText, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formatFollowerCount(count: Long): String {
    return if (count >= 1000L) "${count / 1000L}K" else "$count"
}

private suspend fun loadSocialMetrics(
    firestore: FirebaseFirestore,
    targetUserId: String,
    currentFriendIds: Set<String>,
) : SocialMetrics? {
    return try {
        val followersSnapshot = firestore.collection("users")
            .document(targetUserId)
            .collection("followers")
            .get()
            .await()

        val followingSnapshot = firestore.collection("users")
            .document(targetUserId)
            .collection("following")
            .get()
            .await()

        val targetFollowerIds = followersSnapshot.documents.map { doc ->
            doc.getString("userId") ?: doc.id
        }.toSet()

        val targetFollowingIds = followingSnapshot.documents.map { doc ->
            doc.getString("userId") ?: doc.id
        }.toSet()

        val targetMutualFollowIds = targetFollowerIds intersect targetFollowingIds

        // Backward compatibility for legacy users/{id}/friends documents.
        val legacyFriendsSnapshot = firestore.collection("users")
            .document(targetUserId)
            .collection("friends")
            .get()
            .await()
        val legacyFriendIds = legacyFriendsSnapshot.documents.map { doc ->
            doc.getString("userId") ?: doc.id
        }.toSet()

        val targetFriendGraph = targetMutualFollowIds + legacyFriendIds
        val mutualCount = (targetFriendGraph intersect currentFriendIds).size
        SocialMetrics(
            followersCount = followersSnapshot.size().toLong(),
            followingCount = followingSnapshot.size().toLong(),
            mutualCount = mutualCount,
        )
    } catch (_: Exception) {
        null
    }
}