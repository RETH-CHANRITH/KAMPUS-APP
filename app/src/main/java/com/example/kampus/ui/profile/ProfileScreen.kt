package com.example.kampus.ui.profile

import android.content.Intent
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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

private val Bg = Color(0xFF080B11)
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

@OptIn(ExperimentalMaterial3Api::class)
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
	onOpenActivity: (ProfileActivityItem) -> Unit = {},
	onEditCoverImage: () -> Unit = {},
	viewModel: ProfileViewModel = viewModel(),
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val scroll = rememberScrollState()
	var selectedNav = remember { mutableIntStateOf(-1) } // -1 means no tab is selected (we're on profile)
	val isProfileSelected = true // Profile screen is always selected when viewing profile
	var showImagePicker by remember { mutableStateOf(false) }
	val context = LocalContext.current

	// Show error as snackbar
	if (state.error != null) {
		ErrorSnackBar(
			message = state.error!!,
			onDismiss = { viewModel.clearError() }
		)
	}

	// Image Picker Dialog
	if (showImagePicker) {
		val sheetState = rememberModalBottomSheetState()
		ModalBottomSheet(
			onDismissRequest = { showImagePicker = false },
			sheetState = sheetState,
			containerColor = Card,
			scrimColor = Color.Black.copy(alpha = 0.5f),
		) {
			ImagePickerDialog(
				onImageSelected = { uri ->
					viewModel.uploadCoverImageToSupabase(uri, context)
					showImagePicker = false
				},
				onDismiss = { showImagePicker = false },
			)
		}
	}

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
			if (state.isLoading) {
				LoadingState()
			} else {
				Header(
					state = state,
					onBack = onBack,
					onOpenSettings = onOpenSettings,
					isOnline = state.isOnline,
					onEditCoverImage = {
						showImagePicker = true
					},
				)
				ProfileMeta(state = state)
				StatsSection(state = state)
				ActionsSection(
					state = state,
					onOpenSettings = onOpenSettings,
					onShareProfile = {
						val link = viewModel.getShareProfileLink()
						if (link != null) {
							viewModel.logShareProfileActivity()
							val shareIntent = Intent(Intent.ACTION_SEND).apply {
								type = "text/plain"
								putExtra(Intent.EXTRA_SUBJECT, "KAMPUS Profile")
								putExtra(
									Intent.EXTRA_TEXT,
									"Check out ${state.displayName}'s profile on KAMPUS: $link",
								)
							}
							context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
						}
					},
					onEditProfile = onEditProfile,
					onOpenFriendRequests = onOpenFriendRequests,
					onOpenFriends = onOpenFriends,
					onOpenDiscoverPeople = onOpenDiscoverPeople,
				)
				AboutCard(state = state)
				RecentActivitySection(
					activities = state.activities,
					onActivityClick = onOpenActivity,
				)
				Spacer(modifier = Modifier.height(16.dp))
			}
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.background(Brush.verticalGradient(listOf(Color.Transparent, Bg.copy(alpha = 0.98f))))
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
				onProfileClick = { },
				isProfileSelected = isProfileSelected,
			)
		}
	}
}

@Composable
private fun Header(
	state: ProfileUiState,
	onBack: () -> Unit,
	onOpenSettings: () -> Unit,
	isOnline: Boolean,
	onEditCoverImage: () -> Unit,
) {
	Box(modifier = Modifier.fillMaxWidth()) {
		// Background/Cover Image Container
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(192.dp),
		) {
			// Background Image
			if (state.coverImageUrl.isNotEmpty()) {
			AsyncImage(
				model = state.coverImageUrl,
				contentDescription = "Cover Image",
				contentScale = ContentScale.Crop,
				modifier = Modifier
					.fillMaxWidth()
					.height(192.dp),
			)
			} else {
				// Default gradient background
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(192.dp)
						.background(
							Brush.linearGradient(
								colors = listOf(
									Color(0xFF1a1f3a),
									Color(0xFF080B11)
								),
								start = androidx.compose.ui.geometry.Offset(0f, 0f),
								end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
							)
						),
				)
			}

			// Dark Overlay
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(192.dp)
					.background(
						Brush.verticalGradient(
							colors = listOf(
								Color.Black.copy(alpha = 0.3f),
								Color.Black.copy(alpha = 0.5f)
							)
						)
					),
			)

			// Edit Cover Button (Bottom Right)
			Box(
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(12.dp)
					.size(40.dp)
					.clip(CircleShape)
					.background(Blue.copy(alpha = 0.9f))
					.clickable(onClick = onEditCoverImage),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					Icons.Outlined.CameraAlt,
					contentDescription = "Edit Cover",
					tint = TextPrimary,
					modifier = Modifier.size(20.dp),
				)
			}
		}

		// Header Actions (Top)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 12.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		) {
			CircleIconButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack)
			CircleIconButton(icon = Icons.Filled.Settings, onClick = onOpenSettings)
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
					.background(brush = Brush.linearGradient(colors = listOf(Color(0xFF20A4FF), Color(0xFF7C3AED)))),
				contentAlignment = Alignment.Center,
			) {
				if (state.profileImageUrl.isNotEmpty()) {
					AsyncImage(
						model = state.profileImageUrl,
						contentDescription = "Profile image",
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.fillMaxSize()
							.clip(CircleShape),
					)
				} else {
					Text(text = state.avatarEmoji, fontSize = 36.sp)
				}
			}

			Box(
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.size(16.dp)
					.clip(CircleShape)
					.background(if (isOnline) Color(0xFF00C950) else Color(0xFF6B7280))
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
		Text(text = state.bio.ifEmpty { "Add your bio" }, color = TextSecondary, fontSize = 14.sp)
		Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
			Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
			Text(text = state.location.ifEmpty { "Add your location" }, color = TextSecondary, fontSize = 14.sp)
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
	state: ProfileUiState,
	onOpenSettings: () -> Unit,
	onShareProfile: () -> Unit,
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
				badge = if (state.stats.friendRequests > 0) state.stats.friendRequests.toString() else null,
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
				onClick = onShareProfile,
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
		InfoLine(icon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = TextSecondary) }, text = state.faculty.ifEmpty { "Add faculty" })
		InfoLine(icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary) }, text = state.year.ifEmpty { "Add year" })
		InfoLine(icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary) }, text = state.location.ifEmpty { "Add location" })
	}
}

@Composable
private fun RecentActivitySection(
	activities: List<ProfileActivityItem>,
	onActivityClick: (ProfileActivityItem) -> Unit,
) {
	// Filter out "share_profile" activities - only show real content activities
	val filteredActivities = activities.filter { it.type != "share_profile" }
	
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Text(text = "Recent Activity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

		if (filteredActivities.isEmpty()) {
			Text(
				text = "No activity yet. Your posts, event actions, and shares will appear here.",
				color = TextSecondary,
				fontSize = 13.sp,
			)
		} else {
			filteredActivities.forEach { activity ->
				ActivityRow(activity = activity, onClick = { onActivityClick(activity) })
			}
		}
	}
}

@Composable
private fun ActivityRow(activity: ProfileActivityItem, onClick: () -> Unit) {
	val icon = when (activity.type) {
		"share_profile" -> Icons.Outlined.Share
		"interested_event" -> Icons.Outlined.CalendarMonth
		"create_post" -> Icons.Outlined.ModeEdit
		"create_event" -> Icons.Outlined.AddCircle
		else -> Icons.Outlined.Stars
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(14.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(14.dp))
			.clickable(onClick = onClick)
			.padding(horizontal = 12.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
			Text(text = activity.text, color = Color(0xFFE5E7EB), fontSize = 14.sp)
			Text(text = formatActivityTime(activity.createdAt), color = TextSecondary, fontSize = 11.sp)
		}
	}
}

private fun formatActivityTime(timestamp: Long): String {
	if (timestamp <= 0L) return "just now"
	val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0L)
	return when {
		minutes < 1 -> "just now"
		minutes < 60 -> "${minutes}m ago"
		minutes < 1440 -> "${minutes / 60}h ago"
		else -> "${minutes / 1440}d ago"
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
private fun LoadingState() {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			androidx.compose.material3.CircularProgressIndicator(color = Blue)
			Text(text = "Loading profile...", color = TextSecondary, fontSize = 14.sp)
		}
	}
}

@Composable
private fun ErrorSnackBar(message: String, onDismiss: () -> Unit) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp),
		color = Danger.copy(alpha = 0.9f),
		shape = RoundedCornerShape(8.dp),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(text = message, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
			Icon(
				imageVector = Icons.Default.Add,
				contentDescription = null,
				tint = TextPrimary,
				modifier = Modifier
					.size(20.dp)
					.clickable { onDismiss() }
					.rotate(45f),
			)
		}
	}
}

