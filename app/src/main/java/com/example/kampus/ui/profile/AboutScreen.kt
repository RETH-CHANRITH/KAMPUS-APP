package com.example.kampus.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ABg = Color(0xFF1A1D2E)
private val ACard = Color(0xFF252A41)
private val ABorder = Color(0xFF364153)
private val ABlue = Color(0xFF0D7FFF)
private val APurple = Color(0xFF7C3AED)
private val AWhite = Color(0xFFFFFFFF)
private val ASubtle = Color(0xFF99A1AF)
private val ARed = Color(0xFFEF4444)
private val ANavBg = Color(0xFF0C1018)

private data class AboutNavItem(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

private val aboutNavItems = listOf(
    AboutNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
    AboutNavItem("Groups", Icons.Outlined.Group, Icons.Filled.Group),
    AboutNavItem("Events", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    AboutNavItem("Chat", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
)

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onEventsClick: () -> Unit,
    onChatClick: () -> Unit,
    onCreatePost: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val selectedNav = remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ABg),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ACard)
                            .border(1.dp, ABorder, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AWhite)
                    }
                    Text("About", color = AWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp))
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(ABlue, APurple))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("K", color = AWhite, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("KAMPUS", color = AWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.0", color = ASubtle, fontSize = 15.sp)
                }
            }

            item {
                AboutActionRow("Privacy Policy", Icons.Outlined.Description)
            }
            item {
                AboutActionRow("Open Source Licenses", Icons.Outlined.Gavel)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ACard)
                        .border(1.dp, ABorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Credits", color = AWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("- Icons by Lucide Icons", color = ASubtle, fontSize = 14.sp)
                    Text("- Images by Unsplash", color = ASubtle, fontSize = 14.sp)
                    Text("- Built with Android Compose", color = ASubtle, fontSize = 14.sp)
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("© 2026 KAMPUS Inc.", color = ASubtle, fontSize = 14.sp)
                    Text("Made with love for connecting people", color = ASubtle, fontSize = 14.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, ABg.copy(alpha = 0.98f))))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
        ) {
            CampusBottomNav(
                selectedIndex = selectedNav.intValue,
                onItemSelected = { index ->
                    selectedNav.intValue = index
                    when (index) {
                        0 -> onHomeClick()
                        1 -> onGroupsClick()
                        2 -> onEventsClick()
                        3 -> onChatClick()
                    }
                },
                notifCount = 0,
                onFabClick = onCreatePost,
                onProfileClick = onProfileClick,
            )
        }
    }
}

@Composable
private fun AboutActionRow(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ACard)
            .border(1.dp, ABorder, RoundedCornerShape(14.dp))
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ABlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ABlue)
            }
            Text(title, color = AWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = ASubtle)
    }
}

@Composable
private fun CampusBottomNav(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    notifCount: Int,
    onFabClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(ANavBg)
                .border(1.dp, ABorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            aboutNavItems.forEachIndexed { i, item ->
                val selected = selectedIndex == i
                val tabBg by animateColorAsState(if (selected) ABlue.copy(alpha = 0.12f) else Color.Transparent, tween(240), label = "bg$i")
                val tabBorder by animateColorAsState(if (selected) ABlue.copy(alpha = 0.65f) else Color.Transparent, tween(240), label = "bd$i")
                val iconTint by animateColorAsState(if (selected) ABlue else ASubtle, tween(220), label = "it$i")

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(tabBg)
                        .border(1.dp, tabBorder, RoundedCornerShape(24.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { onItemSelected(i) }
                        .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            imageVector = if (selected) item.iconSelected else item.icon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp),
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(160)) + expandHorizontally(tween(200), expandFrom = Alignment.Start),
                            exit = fadeOut(tween(100)) + shrinkHorizontally(tween(150), shrinkTowards = Alignment.Start),
                        ) {
                            Text(item.label, color = ABlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ABlue, Color(0xFF2563EB))))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onFabClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "Create post", tint = AWhite, modifier = Modifier.size(26.dp))
        }

        Box(modifier = Modifier.size(58.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(ACard)
                    .border(1.dp, ABorder, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onProfileClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Person, "Profile", tint = ASubtle, modifier = Modifier.size(24.dp))
            }
            if (notifCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(2.dp, (-2).dp)
                        .clip(CircleShape)
                        .background(ARed)
                        .border(1.5.dp, ABg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$notifCount", color = AWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}