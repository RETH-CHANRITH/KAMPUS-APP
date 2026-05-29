package com.example.kampus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kampus.ui.theme.ThemeController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val UiIsDark get() = ThemeController.isDark
private val Card get() = if (UiIsDark) Color(0xFF252A41) else Color(0xFFFFFFFF)
private val Border get() = if (UiIsDark) Color(0xFF2C3552) else Color(0xFFD1D5DB)
private val Blue get() = ThemeController.accent.color
private val TextPrimary get() = if (UiIsDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val TextSecondary get() = if (UiIsDark) Color(0xFF99A1AF) else Color(0xFF6B7280)
private val NavBgDefault get() = if (UiIsDark) Color(0x99080B11) else Color(0xCCFFFFFF)

data class NavItem(
	val label: String,
	val icon: ImageVector,
	val iconSelected: ImageVector,
)

@Composable
fun rememberCurrentUserRole(defaultRole: String = "student"): String {
	val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
	var roleState by remember { mutableStateOf(defaultRole) }

	DisposableEffect(currentUserId) {
		if (currentUserId.isNullOrBlank()) {
			roleState = defaultRole
			onDispose { }
		} else {
			val registration = FirebaseFirestore.getInstance()
				.collection("users")
				.document(currentUserId)
				.addSnapshotListener { snapshot, _ ->
					roleState = snapshot?.getString("role") ?: defaultRole
				}

			onDispose { registration.remove() }
		}
	}

	return roleState
}

@Composable
fun rememberCampusNavItems(isAdmin: Boolean): List<NavItem> {
	val strings = com.example.kampus.ui.localization.rememberUiStrings()

	return remember(strings, isAdmin) {
		if (isAdmin) {
			listOf(
				NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
				NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
				NavItem(strings.adminPanel, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
				NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
			)
		} else {
			listOf(
				NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
				NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
				NavItem(strings.events, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
				NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
			)
		}
	}
}

@Composable
fun CampusBottomNavBar(
	selectedIndex: Int,
	onItemSelected: (Int) -> Unit,
	onFabClick: () -> Unit,
	onProfileClick: () -> Unit,
	isProfileSelected: Boolean = false,
	navItems: List<NavItem>? = null,
	fabIcon: ImageVector = Icons.Default.Add,
	backgroundColor: Color? = null
) {
	val strings = com.example.kampus.ui.localization.rememberUiStrings()
	
	val actualNavItems = navItems ?: remember(strings) {
		listOf(
			NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
			NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
			NavItem(strings.events, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
			NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
		)
	}

	val navBg = backgroundColor ?: NavBgDefault

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
				.background(navBg)
				.border(1.dp, Border, RoundedCornerShape(32.dp))
				.padding(horizontal = 6.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			actualNavItems.forEachIndexed { i, item ->
				val selected = selectedIndex == i && !isProfileSelected

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
						.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onItemSelected(i) }
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
				.height(58.dp)
				.size(58.dp)
				.clip(CircleShape)
				.background(
					Brush.linearGradient(
						listOf(Blue, Blue.copy(alpha = 0.78f)),
					),
				)
				.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFabClick),
			contentAlignment = Alignment.Center,
		) {
			Icon(fabIcon, "Action", tint = TextPrimary, modifier = Modifier.size(26.dp))
		}

		Box(modifier = Modifier.size(58.dp)) {
			Box(
				modifier = Modifier
					.size(58.dp)
					.clip(CircleShape)
					.background(
						if (isProfileSelected) Blue.copy(alpha = if (UiIsDark) 0.22f else 0.14f)
						else Card.copy(alpha = if (UiIsDark) 0.5f else 0.82f)
					)
					.border(
						1.dp,
						if (isProfileSelected) Blue.copy(alpha = 0.75f) else Border,
						CircleShape
					)
					.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onProfileClick),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					Icons.Outlined.Person,
					"Profile",
					tint = if (isProfileSelected) Blue else TextSecondary,
					modifier = Modifier.size(24.dp)
				)
			}
		}
	}
}
