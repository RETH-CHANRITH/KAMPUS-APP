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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.R

private val Bg = Color(0xFF1A1D2E)
private val Card = Color(0xFF252A41)
private val Border = Color(0xFF2C3552)
private val Blue = Color(0xFF0D7FFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF99A1AF)
private val Danger = Color(0xFFFF3B5C)
private val NavBg = Color(0xFF0C1018)

private data class ProfileNavItem(
	val label: String,
	val icon: ImageVector,
	val iconSelected: ImageVector,
)

private val profileNavItems = listOf(
	ProfileNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
	ProfileNavItem("Groups", Icons.Outlined.Group, Icons.Filled.Group),
	ProfileNavItem("Events", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
	ProfileNavItem("Chat", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
)

@Composable
fun ProfileScreen(
	onBack: () -> Unit,
	onOpenSettings: () -> Unit,
	onEditProfile: () -> Unit,
	onOpenFriendRequests: () -> Unit,
	onOpenFriends: () -> Unit,
	onOpenDiscoverPeople: () -> Unit,
	onHomeClick: () -> Unit,
	onGroupsClick: () -> Unit,
	onEventsClick: () -> Unit,
	onChatClick: () -> Unit,
	onCreatePost: () -> Unit,
	onLogout: () -> Unit = {},
	viewModel: ProfileViewModel = viewModel(),
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val scroll = rememberScrollState()
	var selectedNav = remember { mutableIntStateOf(0) }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Bg),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(scroll)
				.statusBarsPadding()
				.padding(bottom = 96.dp),
		) {
			Header(state = state, onBack = onBack)
			ProfileMeta(state = state)
			StatsSection(state = state)
			ActionsSection(
				onOpenSettings = onOpenSettings,
				onEditProfile = onEditProfile,
				onOpenFriendRequests = onOpenFriendRequests,
				onOpenFriends = onOpenFriends,
				onOpenDiscoverPeople = onOpenDiscoverPeople,
			)
			AboutCard(state = state)
			GallerySection()
			DangerAction(onOpenSettings = onOpenSettings, onLogout = onLogout)
			Spacer(modifier = Modifier.height(16.dp))
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.background(Brush.verticalGradient(listOf(Color.Transparent, Bg.copy(alpha = 0.98f))))
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
				onProfileClick = { },
			)
		}
	}
}

@Composable
private fun Header(state: ProfileUiState, onBack: () -> Unit) {
	Box(modifier = Modifier.fillMaxWidth()) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(192.dp)
				.background(
					brush = Brush.linearGradient(
						colors = listOf(
							Color(0xFF59168B),
							Color(0xFF1C398E),
							Color(0xFF6E11B0),
						),
					),
				),
		)

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 12.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		) {
			CircleIconButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack)
			CircleIconButton(icon = Icons.Outlined.Notifications, onClick = {})
		}

		Box(
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.offset(y = 48.dp)
				.size(96.dp),
		) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clip(CircleShape)
					.border(4.dp, Bg, CircleShape)
					.background(
						brush = Brush.linearGradient(
							colors = listOf(Color(0xFF20A4FF), Color(0xFF7C3AED)),
						),
					),
				contentAlignment = Alignment.Center,
			) {
				Text(text = state.avatarEmoji, fontSize = 36.sp)
			}

			Box(
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.size(16.dp)
					.clip(CircleShape)
					.background(Color(0xFF00C950))
					.border(2.dp, Bg, CircleShape),
			)
		}
	}
}

@Composable
private fun ProfileMeta(state: ProfileUiState) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 60.dp, start = 24.dp, end = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(6.dp),
	) {
		Text(text = state.displayName, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
		Text(text = "Blender Art 3D Designer", color = TextSecondary, fontSize = 14.sp)
		Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
			Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
			Text(text = "London UK", color = TextSecondary, fontSize = 14.sp)
		}
	}
}

@Composable
private fun StatsSection(state: ProfileUiState) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp),
		horizontalArrangement = Arrangement.SpaceEvenly,
	) {
		StatItem(value = state.stats.posts.toString(), label = "Posts")
		StatItem(value = state.stats.followers.toString(), label = "Followers")
		StatItem(value = state.stats.following.toString(), label = "Following")
	}
}

@Composable
private fun StatItem(value: String, label: String) {
	Column(
		modifier = Modifier.padding(vertical = 4.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(2.dp),
	) {
		Text(text = value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
		Text(text = label, color = TextSecondary, fontSize = 12.sp)
	}
}

@Composable
private fun ActionsSection(
	onOpenSettings: () -> Unit,
	onEditProfile: () -> Unit,
	onOpenFriendRequests: () -> Unit,
	onOpenFriends: () -> Unit,
	onOpenDiscoverPeople: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
			ActionCard(
				title = "Requests",
				icon = Icons.Outlined.PersonAdd,
				badge = "3",
				modifier = Modifier.weight(1f),
				onClick = onOpenFriendRequests,
			)
			ActionCard(
				title = "Friends",
				icon = Icons.Outlined.Badge,
				modifier = Modifier.weight(1f),
				onClick = onOpenFriends,
			)
		}

		Button(
			onClick = onOpenDiscoverPeople,
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(14.dp),
			colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = TextPrimary),
		) {
			Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
			Spacer(Modifier.size(8.dp))
			Text(text = "Discover People", fontWeight = FontWeight.Medium)
		}

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
			Button(
				onClick = onEditProfile,
				modifier = Modifier.weight(1f),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = TextPrimary),
			) {
				Icon(Icons.Outlined.ModeEdit, contentDescription = null, modifier = Modifier.size(18.dp))
				Spacer(Modifier.size(8.dp))
				Text(text = "Edit Profile", fontWeight = FontWeight.Medium)
			}
			Button(
				onClick = onOpenSettings,
				modifier = Modifier.weight(1f),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.buttonColors(containerColor = Card, contentColor = TextPrimary),
			) {
				Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
				Spacer(Modifier.size(8.dp))
				Text(text = "Share Profile", fontWeight = FontWeight.Medium)
			}
		}

		StoryRow()
	}
}

@Composable
private fun ActionCard(
	title: String,
	icon: ImageVector,
	modifier: Modifier = Modifier,
	badge: String? = null,
	onClick: () -> Unit,
) {
	Box(
		modifier = modifier
			.clip(RoundedCornerShape(14.dp))
			.background(Card)
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
			Text(text = title, color = TextPrimary, fontWeight = FontWeight.Medium)
		}
		if (badge != null) {
			Box(
				modifier = Modifier
					.align(Alignment.CenterEnd)
					.clip(CircleShape)
					.background(Danger)
					.padding(horizontal = 8.dp, vertical = 2.dp),
			) {
				Text(text = badge, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
			}
		}
	}
}

@Composable
private fun StoryRow() {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(16.dp),
	) {
		StoryChip(title = "New", icon = Icons.Outlined.AddCircle, selected = false)
		StoryChip(title = "Lifestyle", icon = Icons.Outlined.GridView, selected = true)
		StoryChip(title = "Friends", icon = Icons.Outlined.Badge, selected = false)
	}
}

@Composable
private fun StoryChip(title: String, icon: ImageVector, selected: Boolean) {
	Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Box(
			modifier = Modifier
				.size(48.dp)
				.clip(CircleShape)
				.background(if (selected) Blue else Card),
			contentAlignment = Alignment.Center,
		) {
			Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
		}
		Text(text = title, color = if (selected) TextPrimary else TextSecondary, fontSize = 12.sp)
	}
}

@Composable
private fun AboutCard(state: ProfileUiState) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp)
			.clip(RoundedCornerShape(20.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(20.dp))
			.padding(14.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Text(text = "About", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
		InfoLine(icon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = TextSecondary) }, text = state.faculty)
		InfoLine(icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary) }, text = state.year)
		InfoLine(icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary) }, text = "Phnom Penh, Cambodia")
	}
}

@Composable

private fun GallerySection() {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		GalleryImage(drawable = R.drawable.pic, modifier = Modifier.weight(1f))
		GalleryImage(drawable = R.drawable.pic2, modifier = Modifier.weight(1f))
		GalleryImage(drawable = R.drawable.pic3, modifier = Modifier.weight(1f))
	}
}

@Composable

private fun GalleryImage(drawable: Int, modifier: Modifier = Modifier) {
	Image(
		painter = painterResource(id = drawable),
		contentDescription = null,
		contentScale = ContentScale.Crop,
		modifier = modifier
			.aspectRatio(0.72f)
			.clip(RoundedCornerShape(14.dp)),
	)
}

@Composable
private fun DangerAction(onOpenSettings: () -> Unit, onLogout: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp)
			.clip(RoundedCornerShape(20.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(20.dp))
			.padding(horizontal = 14.dp, vertical = 8.dp),
	) {
		ActionRow(
			title = "Settings",
			icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFFE5E7EB)) },
			onClick = onOpenSettings,
		)
		HorizontalDivider(color = Border, thickness = 0.5.dp)
		ActionRow(
			title = "Log Out",
			titleColor = Danger,
			icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = Danger) },
			onClick = onLogout,
		)
	}
}

@Composable
private fun InfoLine(icon: @Composable () -> Unit, text: String) {
	Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
		icon()
		Text(text = text, color = Color(0xFFE5E7EB), fontSize = 14.sp)
	}
}

@Composable
private fun ActionRow(
	title: String,
	titleColor: Color = Color(0xFFE5E7EB),
	icon: @Composable () -> Unit,
	onClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		icon()
		Text(text = title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
	}
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
	Box(
		modifier = Modifier
			.size(40.dp)
			.clip(CircleShape)
			.background(Color.White.copy(alpha = 0.1f))
			.clickable(onClick = onClick),
		contentAlignment = Alignment.Center,
	) {
		Icon(icon, contentDescription = null, tint = TextPrimary)
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
				.background(NavBg)
				.border(1.dp, Border, RoundedCornerShape(32.dp))
				.padding(horizontal = 6.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			profileNavItems.forEachIndexed { i, item ->
				val selected = selectedIndex == i

				val tabBg by animateColorAsState(
					if (selected) Blue.copy(alpha = 0.12f) else Color.Transparent,
					tween(240), label = "bg$i",
				)
				val tabBorder by animateColorAsState(
					if (selected) Blue.copy(alpha = 0.65f) else Color.Transparent,
					tween(240), label = "bd$i",
				)
				val iconTint by animateColorAsState(
					if (selected) Blue else TextSecondary,
					tween(220), label = "it$i",
				)

				Box(
					modifier = Modifier
						.clip(RoundedCornerShape(24.dp))
						.background(tabBg)
						.border(1.dp, tabBorder, RoundedCornerShape(24.dp))
						.clickable(remember { MutableInteractionSource() }, null) { onItemSelected(i) }
						.padding(
							horizontal = if (selected) 14.dp else 10.dp,
							vertical = 9.dp,
						),
					contentAlignment = Alignment.Center,
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(5.dp),
					) {
						Icon(
							imageVector = if (selected) item.iconSelected else item.icon,
							contentDescription = item.label,
							tint = iconTint,
							modifier = Modifier.size(21.dp),
						)
						AnimatedVisibility(
							visible = selected,
							enter = fadeIn(tween(160)) + expandHorizontally(
								animationSpec = tween(200),
								expandFrom = Alignment.Start,
							),
							exit = fadeOut(tween(100)) + shrinkHorizontally(
								animationSpec = tween(150),
								shrinkTowards = Alignment.Start,
							),
						) {
							Text(
								item.label,
								color = Blue,
								fontSize = 13.sp,
								fontWeight = FontWeight.Bold,
								maxLines = 1,
							)
						}
					}
				}
			}
		}

		Box(
			modifier = Modifier
				.size(58.dp)
				.clip(CircleShape)
				.background(
					Brush.linearGradient(
						listOf(Blue, Color(0xFF2563EB)),
					),
				)
				.clickable(remember { MutableInteractionSource() }, null, onClick = onFabClick),
			contentAlignment = Alignment.Center,
		) {
			Icon(Icons.Default.Add, "Create post", tint = TextPrimary, modifier = Modifier.size(26.dp))
		}

		Box(modifier = Modifier.size(58.dp)) {
			Box(
				modifier = Modifier
					.size(58.dp)
					.clip(CircleShape)
					.background(Card)
					.border(1.dp, Border, CircleShape)
					.clickable(remember { MutableInteractionSource() }, null, onClick = onProfileClick),
				contentAlignment = Alignment.Center,
			) {
				Icon(Icons.Outlined.Person, "Profile", tint = TextSecondary, modifier = Modifier.size(24.dp))
			}
			if (notifCount > 0) {
				Box(
					modifier = Modifier
						.size(18.dp)
						.align(Alignment.TopEnd)
						.offset(2.dp, (-2).dp)
						.clip(CircleShape)
						.background(Color(0xFFEF4444))
						.border(1.5.dp, Bg, CircleShape),
					contentAlignment = Alignment.Center,
				) {
					Text("$notifCount", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
				}
			}
		}
	}
}
