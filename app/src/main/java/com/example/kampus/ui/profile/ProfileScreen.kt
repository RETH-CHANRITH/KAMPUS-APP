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
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Divider
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
import com.example.kampus.ui.feed.PostItem

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
					posts = state.timelinePosts,
					activities = state.activities,
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
	posts: List<PostItem>,
	activities: List<ProfileActivityItem>,
) {
	val eventFallbacks = activities.filter { it.type == "create_event" }
	val itemsToShow = if (posts.isNotEmpty()) posts else emptyList()
	val hasAnything = itemsToShow.isNotEmpty() || eventFallbacks.isNotEmpty()

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Text(text = "Recent Activity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

		if (!hasAnything) {
			Text(
				text = "No recent posts yet. New posts will appear here in real time.",
				color = TextSecondary,
				fontSize = 13.sp,
			)
		} else {
			itemsToShow.forEach { post ->
				ProfileTimelinePostCard(post = post)
			}
			if (itemsToShow.isEmpty()) {
				eventFallbacks.forEach { activity ->
					ProfileTimelineActivityCard(activity = activity)
				}
			}
		}
	}
}

@Composable
private fun ProfileTimelineActivityCard(
	activity: ProfileActivityItem,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(20.dp))
			.padding(14.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			Box(
				modifier = Modifier
					.size(36.dp)
					.clip(CircleShape)
					.background(Color.White.copy(alpha = 0.12f)),
				contentAlignment = Alignment.Center,
			) {
				Text(text = "🎓", fontSize = 18.sp)
			}
			Column(modifier = Modifier.weight(1f)) {
				Text(text = "Created event", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
				Text(text = formatActivityTime(activity.createdAt), color = TextSecondary, fontSize = 12.sp)
			}
			Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
		}

		Text(
			text = activity.previewTitle.ifBlank { activity.text },
			color = TextPrimary,
			fontSize = 22.sp,
			fontWeight = FontWeight.Bold,
			lineHeight = 26.sp,
			maxLines = 3,
		)

		if (activity.previewImageUrl.isNotBlank()) {
			AsyncImage(
				model = activity.previewImageUrl,
				contentDescription = activity.previewTitle,
				contentScale = ContentScale.Crop,
				modifier = Modifier
					.fillMaxWidth()
					.height(220.dp)
					.clip(RoundedCornerShape(18.dp)),
			)
		}

		Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
			InfoLine(icon = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = Danger, modifier = Modifier.size(15.dp)) }, text = activity.likeCount.toString())
			InfoLine(icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = activity.commentCount.toString())
			InfoLine(icon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = activity.shareCount.toString())
		}

		Button(
			onClick = { },
			modifier = Modifier.fillMaxWidth().height(48.dp),
			shape = RoundedCornerShape(14.dp),
			colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White),
		) {
			Text(text = "I'm Interested", fontWeight = FontWeight.SemiBold)
		}
	}
}

@Composable
private fun ProfileTimelinePostCard(
	post: PostItem,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
		.clip(RoundedCornerShape(20.dp))
		.background(Card)
		.border(1.dp, Border, RoundedCornerShape(20.dp))
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(190.dp),
		) {
			if (post.getFirstMediaUri() != null) {
				AsyncImage(
					model = post.getFirstMediaUri(),
					contentDescription = post.content,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(
							Brush.linearGradient(
								listOf(Color(0xFF1F3C88), Color(0xFF0D7FFF)),
							),
						),
				)
			}
			Box(
				modifier = Modifier
					.align(Alignment.TopStart)
					.padding(12.dp)
					.clip(RoundedCornerShape(8.dp))
					.background(Blue.copy(alpha = 0.9f))
					.padding(horizontal = 10.dp, vertical = 4.dp),
			) {
				Text("KAMPUS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
			}
			Box(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(12.dp)
					.size(28.dp)
					.clip(CircleShape)
					.background(Color.Black.copy(alpha = 0.35f)),
				contentAlignment = Alignment.Center,
			) {
				Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
			}
		}

		Column(
			modifier = Modifier.padding(14.dp),
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Box(
					modifier = Modifier
						.size(36.dp)
						.clip(CircleShape)
						.background(Color.White.copy(alpha = 0.12f)),
					contentAlignment = Alignment.Center,
				) {
					Text(text = post.avatar, fontSize = 18.sp)
				}
				Spacer(modifier = Modifier.size(10.dp))
				Column(modifier = Modifier.weight(1f)) {
					Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
						Text(text = post.author, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
						if (post.isVerified) {
							Icon(Icons.Filled.Verified, contentDescription = null, tint = Blue, modifier = Modifier.size(14.dp))
						}
					}
					Text(text = post.time, color = TextSecondary, fontSize = 12.sp)
				}
				Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
			}

			Text(
				text = post.content,
				color = TextPrimary,
				fontSize = 22.sp,
				fontWeight = FontWeight.Bold,
				lineHeight = 26.sp,
				maxLines = 3,
			)

			Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
				val visibilityIcon = when (post.visibility) {
					PostItem.PostVisibility.PUBLIC -> Icons.Outlined.Public
					PostItem.PostVisibility.FRIENDS -> Icons.Outlined.Group
					else -> Icons.Outlined.Lock
				}
				val visibilityLabel = when (post.visibility) {
					PostItem.PostVisibility.PUBLIC -> "Public"
					PostItem.PostVisibility.FRIENDS -> "Friends"
					PostItem.PostVisibility.FOLLOWERS -> "Followers"
					PostItem.PostVisibility.UNIVERSITY -> "University"
					PostItem.PostVisibility.PRIVATE -> "Only me"
				}
				InfoLine(icon = { Icon(visibilityIcon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = visibilityLabel)
				if (post.location != null) {
					InfoLine(icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = post.location)
				}
			}

			if (post.hasMedia()) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(220.dp)
						.clip(RoundedCornerShape(18.dp))
						.background(Color(0xFF111827)),
				) {
					AsyncImage(
						model = post.getFirstMediaUri(),
						contentDescription = post.content,
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxSize(),
					)
				}
			}

			Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
				InfoLine(icon = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = Danger, modifier = Modifier.size(15.dp)) }, text = post.likes.toString())
				InfoLine(icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = post.comments.toString())
				InfoLine(icon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = "Share")
			}

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
			) {
				TimelineActionChip(icon = Icons.Outlined.FavoriteBorder, label = "Like", tint = TextSecondary, modifier = Modifier.weight(1f))
				TimelineActionChip(icon = Icons.Outlined.ChatBubbleOutline, label = "Comment", tint = TextSecondary, modifier = Modifier.weight(1f))
				TimelineActionChip(icon = Icons.Outlined.Share, label = "Share", tint = TextSecondary, modifier = Modifier.weight(1f))
			}

			Button(
				onClick = { },
				modifier = Modifier.fillMaxWidth().height(48.dp),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White),
			) {
				Text(text = "I'm Interested", fontWeight = FontWeight.SemiBold)
			}
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
private fun TimelineActionChip(
	icon: ImageVector,
	label: String,
	tint: Color,
	modifier: Modifier = Modifier,
) {
	Row(
		modifier = modifier
			.clip(RoundedCornerShape(12.dp))
			.background(Color.White.copy(alpha = 0.05f))
			.clickable { }
			.padding(vertical = 10.dp),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
		Spacer(modifier = Modifier.size(6.dp))
		Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

