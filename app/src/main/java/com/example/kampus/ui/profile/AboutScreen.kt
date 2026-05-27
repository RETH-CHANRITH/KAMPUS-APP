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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kampus.ui.components.CampusBottomNavBar
import com.example.kampus.ui.localization.rememberUiStrings

// Note: This screen now uses ProfileColors for dynamic theming


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
    supportViewModel: SupportContentViewModel = viewModel(),
) {
    val strings = rememberUiStrings()
    val selectedNav = remember { mutableIntStateOf(0) }
    val supportContent by supportViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileColors.Bg),
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
                            .background(ProfileColors.Card)
                            .border(1.dp, ProfileColors.Border, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back, tint = ProfileColors.White)
                    }
                    Text(strings.aboutTitle, color = ProfileColors.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                            .background(Brush.linearGradient(listOf(ProfileColors.Blue, ProfileColors.Purple))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (supportContent.appLogoUrl.isNotBlank()) {
                            AsyncImage(
                                model = supportContent.appLogoUrl,
                                contentDescription = supportContent.appName,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = supportContent.appLogoFallbackText.ifBlank {
                                    supportContent.appName.firstOrNull()?.uppercaseChar()?.toString() ?: "K"
                                },
                                color = ProfileColors.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(supportContent.appName, color = ProfileColors.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${strings.version} ${supportContent.aboutVersion}", color = ProfileColors.Subtle, fontSize = 15.sp)
                }
            }

            item {
                AboutActionRow(
                    title = supportContent.aboutActions.getOrNull(0)?.title ?: "Privacy Policy",
                    icon = SupportContentViewModel.iconForAbout(supportContent.aboutActions.getOrNull(0)?.iconKey ?: "privacy"),
                    onClick = {
                        openWebPage(context, supportContent.aboutActions.getOrNull(0)?.actionUrl?.ifBlank { "https://kampus.app/privacy" } ?: "https://kampus.app/privacy")
                    },
                )
            }
            item {
                AboutActionRow(
                    title = supportContent.aboutActions.getOrNull(1)?.title ?: "Open Source Licenses",
                    icon = SupportContentViewModel.iconForAbout(supportContent.aboutActions.getOrNull(1)?.iconKey ?: "licenses"),
                    onClick = {
                        openWebPage(context, supportContent.aboutActions.getOrNull(1)?.actionUrl?.ifBlank { "https://kampus.app/licenses" } ?: "https://kampus.app/licenses")
                    },
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ProfileColors.Card)
                        .border(1.dp, ProfileColors.Border, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(strings.credits, color = ProfileColors.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(strings.iconsBy, color = ProfileColors.Subtle, fontSize = 14.sp)
                    Text(strings.imagesBy, color = ProfileColors.Subtle, fontSize = 14.sp)
                    Text(strings.builtWith, color = ProfileColors.Subtle, fontSize = 14.sp)
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(strings.copyright, color = ProfileColors.Subtle, fontSize = 14.sp)
                    Text(strings.madeWithLove, color = ProfileColors.Subtle, fontSize = 14.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, ProfileColors.Bg.copy(alpha = 0.98f))))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
        ) {
            CampusBottomNavBar(
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
                onFabClick = onCreatePost,
                onProfileClick = onProfileClick,
                isProfileSelected = false,
            )
        }
    }
}

@Composable
private fun AboutActionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ProfileColors.Card)
            .border(1.dp, ProfileColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ProfileColors.Blue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ProfileColors.Blue)
            }
            Text(title, color = ProfileColors.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = ProfileColors.Subtle)
    }
}