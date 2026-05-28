package com.example.kampus.ui.screens.groups

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kampus.data.model.MembershipStatus
import com.example.kampus.ui.components.groups.GroupCard
import com.example.kampus.ui.components.groups.KampusSearchBar
import com.example.kampus.ui.theme.KampusColors as C
import com.example.kampus.ui.theme.KampusType as T
import com.example.kampus.viewmodel.GroupsViewModel

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Brush
import com.example.kampus.ui.components.CampusBottomNavBar

@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onHomeClick: () -> Unit = {},
    onEventsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCreatePost: () -> Unit = {},
) {
    val state by viewModel.groupsUiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.loadGroups() }

    val visibleGroups = if (selectedTab == 0) state.myGroups else state.discoverGroups

    Scaffold(
        containerColor = C.Background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, C.Background.copy(alpha = 0.98f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            ) {
                CampusBottomNavBar(
                    selectedIndex = 1,          // Groups tab is index 1
                    onItemSelected = { index ->
                        when (index) {
                            0 -> onHomeClick()
                            1 -> { /* already here */ }
                            2 -> onEventsClick()
                            3 -> onChatClick()
                        }
                    },
                    onFabClick = onCreatePost,
                    onProfileClick = onProfileClick,
                    isProfileSelected = false,
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(C.Background)
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Groups", color = C.TextPrimary, style = T.HeadingLarge)
                IconButton(onClick = onCreateGroupClick) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(C.Primary)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            KampusSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search groups",
            )

            GroupTabs(
                selectedTab = selectedTab,
                myCount = state.myGroups.size,
                discoverCount = state.discoverGroups.size,
                onSelect = { selectedTab = it },
            )

            AnimatedContent(targetState = visibleGroups, label = "groups_list") { groups ->
                if (groups.isEmpty()) {
                    EmptyGroupsState(
                        isMyGroups = selectedTab == 0,
                        onCreateGroupClick = onCreateGroupClick,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(groups, key = { it.id }) { group ->
                            val membershipStatus = viewModel.getMembershipStatus(group.id)
                            val currentRole = viewModel.getCurrentUserRole(group.id)
                            val onActionClick = {
                                when (membershipStatus) {
                                    MembershipStatus.PENDING -> viewModel.cancelJoinRequest(group.id)
                                    MembershipStatus.MEMBER -> { }
                                    MembershipStatus.NONE -> viewModel.joinOrRequestGroup(group.id)
                                }
                            }
                            GroupCard(
                                group = group,
                                membershipStatus = membershipStatus,
                                currentUserRole = currentRole,
                                onJoinClick = onActionClick,
                                onCardClick = { onGroupClick(group.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTabs(
    selectedTab: Int,
    myCount: Int,
    discoverCount: Int,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TabPill(text = "My Groups ($myCount)", selected = selectedTab == 0, onClick = { onSelect(0) }, modifier = Modifier.weight(1f))
        TabPill(text = "Discover ($discoverCount)", selected = selectedTab == 1, onClick = { onSelect(1) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TabPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(if (selected) C.Primary else C.Surface, tween(220), label = "tab_bg")
    val content by animateColorAsState(if (selected) Color.White else C.TextSecondary, tween(220), label = "tab_content")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(vertical = 12.dp)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = content, style = T.LabelMedium, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun EmptyGroupsState(isMyGroups: Boolean, onCreateGroupClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (isMyGroups) "No groups yet" else "No discoverable groups", color = C.TextSecondary, style = T.HeadingSmall)
        Text(if (isMyGroups) "Create your first group or join one from Discover." else "Try a different search or create a new group.", color = C.TextMuted, style = T.BodyMedium)
        TextButton(onClick = onCreateGroupClick) { Text("Create a Group", color = C.Primary) }
    }
}
