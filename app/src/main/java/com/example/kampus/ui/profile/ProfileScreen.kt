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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Dashboard
import com.example.kampus.ui.components.CampusBottomNavBar
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import com.example.kampus.ui.theme.ThemeController

private val Bg: Color get() = if (ThemeController.isDark) Color(0xFF080B11) else Color(0xFFFFFFFF)
private val Card: Color get() = if (ThemeController.isDark) Color(0xFF252A41) else Color(0xFFF7F7FA)
private val Border: Color get() = if (ThemeController.isDark) Color(0xFF2C3552) else Color(0xFFD1D5DB)
private val Blue: Color get() = ThemeController.accent.color
private val TextPrimary: Color get() = if (ThemeController.isDark) Color(0xFFFFFFFF) else Color(0xFF111827)
private val TextSecondary: Color get() = if (ThemeController.isDark) Color(0xFF99A1AF) else Color(0xFF6B7280)
private val Danger: Color get() = Color(0xFFFF3B5C)
private val NavBg: Color get() = if (ThemeController.isDark) Color(0xFF0C1018) else Color(0xFFFFFFFF)

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
	onAdminClick: () -> Unit = {},
	onCreatePost: () -> Unit,
	onOpenActivity: (ProfileActivityItem) -> Unit = {},
	onCommentClick: (ProfileActivityItem) -> Unit = {},
	onEditCoverImage: () -> Unit = {},
	viewModel: ProfileViewModel = viewModel(),
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val scroll = rememberScrollState()
	var selectedNav = remember { mutableIntStateOf(-1) } // -1 means no tab is selected (we're on profile)
	val isProfileSelected = true // Profile screen is always selected when viewing profile
	var showImagePicker by remember { mutableStateOf(false) }
	var activeMenuActivity by remember { mutableStateOf<ProfileActivityItem?>(null) }
	var confirmDeleteActivity by remember { mutableStateOf<ProfileActivityItem?>(null) }
	var editingActivity by remember { mutableStateOf<ProfileActivityItem?>(null) }
	var editPostText by remember { mutableStateOf("") }
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
				ProfileHeader(
					state = state,
					onBack = onBack,
					onOpenSettings = onOpenSettings,
					isOnline = state.isOnline,
					onEditCoverImage = {
						showImagePicker = true
					},
				)
				ProfileMeta(state = state)
				ProfileStatsSection(state = state)
				ProfileActionsSection(
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
				ProfileAboutCard(state = state)
				RecentActivitySection(
					activities = state.activities,
					displayName = state.displayName,
					avatarEmoji = state.avatarEmoji,
					profileImageUrl = state.profileImageUrl,
					viewModel = viewModel,
					onOpenActivity = onOpenActivity,
					onCommentClick = onCommentClick,
					onMenuClick = { activity -> activeMenuActivity = activity },
				)
				Spacer(modifier = Modifier.height(16.dp))
			}
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.background(
					run {
						val fadeEnd = if (ThemeController.isDark) Bg.copy(alpha = 0.98f) else Color.Transparent
						Brush.verticalGradient(listOf(Color.Transparent, fadeEnd))
					}
				)
				.padding(horizontal = 14.dp, vertical = 10.dp)
				.navigationBarsPadding(),
		) {
			val strings = com.example.kampus.ui.localization.rememberUiStrings()
			val isAdmin = state.role == "admin"

			val navItems = remember(strings, isAdmin) {
				if (isAdmin) {
					listOf(
						com.example.kampus.ui.components.NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
						com.example.kampus.ui.components.NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
						com.example.kampus.ui.components.NavItem(strings.adminPanel, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
						com.example.kampus.ui.components.NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
					)
				} else {
					listOf(
						com.example.kampus.ui.components.NavItem(strings.home, Icons.Outlined.Home, Icons.Filled.Home),
						com.example.kampus.ui.components.NavItem(strings.groups, Icons.Outlined.Group, Icons.Filled.Group),
						com.example.kampus.ui.components.NavItem(strings.events, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
						com.example.kampus.ui.components.NavItem(strings.chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
					)
				}
			}

			CampusBottomNavBar(
				navItems = navItems,
				selectedIndex = selectedNav.intValue,
				onItemSelected = { index ->
					selectedNav.intValue = index
					if (isAdmin) {
						when (index) {
							0 -> onHomeClick()
							1 -> onGroupsClick()
							2 -> onAdminClick()
							3 -> onChatClick()
						}
					} else {
						when (index) {
							0 -> onHomeClick()
							1 -> onGroupsClick()
							2 -> onEventsClick()
							3 -> onChatClick()
						}
					}
				},
				onFabClick = onCreatePost,
				onProfileClick = { },
				isProfileSelected = isProfileSelected,
			)
		}

		if (activeMenuActivity != null) {
			val activity = activeMenuActivity!!
			val bSheetState = rememberModalBottomSheetState()
			ModalBottomSheet(
				onDismissRequest = { activeMenuActivity = null },
				sheetState = bSheetState,
				containerColor = Bg,
				scrimColor = Color.Black.copy(alpha = 0.5f),
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.background(Color.Transparent)
						.navigationBarsPadding()
				) {
					// Drag handle
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 8.dp, bottom = 6.dp),
						contentAlignment = Alignment.Center
					) {
						Box(
							modifier = Modifier
								.width(36.dp)
								.height(4.dp)
								.clip(RoundedCornerShape(4.dp))
								.background(Border.copy(alpha = 0.35f))
						)
					}

					// Top group (main post actions)
					Surface(
						shape = RoundedCornerShape(16.dp),
						color = Card,
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 12.dp)
					) {
						Column(modifier = Modifier.padding(vertical = 10.dp)) {
							Text(
								text = "Post options",
								color = TextPrimary,
								fontWeight = FontWeight.Bold,
								modifier = Modifier.padding(start = 18.dp, top = 6.dp, end = 18.dp, bottom = 2.dp)
							)
							Text(
								text = "Choose what to do with this post.",
								color = TextSecondary,
								fontSize = 13.sp,
								modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp)
							)

							MenuItemLarge(
								icon = Icons.AutoMirrored.Outlined.Send,
								label = "Open post",
								tint = Blue
							) {
								onOpenActivity(activity)
								activeMenuActivity = null
							}
							MenuItemLarge(
								icon = Icons.Outlined.ContentCopy,
								label = "Copy post text",
								tint = TextSecondary
							) {
								val clipboard = context.getSystemService(ClipboardManager::class.java)
								clipboard?.setPrimaryClip(ClipData.newPlainText("post", activity.text))
								Toast.makeText(context, "Post text copied", Toast.LENGTH_SHORT).show()
								activeMenuActivity = null
							}

							HorizontalDivider(color = Border.copy(alpha = 0.5f))

							MenuItemLarge(
								icon = Icons.Default.BookmarkAdd,
								label = if (activity.isPinned) "Unpin post" else "Pin post",
								tint = Blue,
								subtitle = "Keep this post at the top"
							) {
								viewModel.pinPostActivity(activity, !activity.isPinned)
								activeMenuActivity = null
							}
							MenuItemLarge(
								icon = Icons.Default.Edit,
								label = "Edit post",
								tint = TextSecondary,
								subtitle = "Update the content or media"
							) {
								editPostText = activity.text
								editingActivity = activity
								activeMenuActivity = null
							}
							MenuItemLarge(
								icon = Icons.Outlined.Lock,
								label = "Privacy settings",
								tint = TextSecondary,
								subtitle = "Choose who can see this post"
							) {
								Toast.makeText(context, "Select a preset below to change privacy", Toast.LENGTH_SHORT).show()
								activeMenuActivity = null
							}
						}
					}

					Spacer(Modifier.height(10.dp))

					// Privacy presets
					Surface(
						shape = RoundedCornerShape(16.dp),
						color = Card,
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 12.dp)
					) {
						Column(modifier = Modifier.padding(vertical = 10.dp)) {
							MenuItemLarge(
								icon = Icons.Outlined.Public,
								label = "Public",
								tint = Color(0xFF4CAF50),
								subtitle = "Anyone can see this post"
							) {
								viewModel.updatePostVisibility(activity, com.example.kampus.ui.feed.PostItem.PostVisibility.PUBLIC)
								activeMenuActivity = null
							}
							HorizontalDivider(color = Border.copy(alpha = 0.5f))
							MenuItemLarge(
								icon = Icons.Outlined.Group,
								label = "Friends",
								tint = Blue,
								subtitle = "Only your friends can see it"
							) {
								viewModel.updatePostVisibility(activity, com.example.kampus.ui.feed.PostItem.PostVisibility.FRIENDS)
								activeMenuActivity = null
							}
							HorizontalDivider(color = Border.copy(alpha = 0.5f))
							MenuItemLarge(
								icon = Icons.Outlined.Lock,
								label = "Only me",
								tint = TextSecondary,
								subtitle = "Visible only to you"
							) {
								viewModel.updatePostVisibility(activity, com.example.kampus.ui.feed.PostItem.PostVisibility.PRIVATE)
								activeMenuActivity = null
							}
						}
					}

					Spacer(Modifier.height(10.dp))

					// Destructive actions
					Surface(
						shape = RoundedCornerShape(16.dp),
						color = Card,
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 12.dp, vertical = 8.dp)
					) {
						Column(modifier = Modifier.padding(vertical = 10.dp)) {
							MenuItemLarge(
								icon = Icons.Default.PersonOff,
								label = "Hide from profile",
								tint = TextSecondary,
								subtitle = "Remove it from your profile only"
							) {
								viewModel.hideFromProfile(activity)
								activeMenuActivity = null
							}
							HorizontalDivider(color = Border.copy(alpha = 0.5f))
							MenuItemLarge(
								icon = Icons.Default.Delete,
								label = "Delete post",
								tint = Danger,
								subtitle = "This removes the post from the feed"
							) {
								confirmDeleteActivity = activity
								activeMenuActivity = null
							}
						}
					}
					Spacer(Modifier.height(18.dp))
				}
			}
		}

		if (confirmDeleteActivity != null) {
			val activity = confirmDeleteActivity!!
			AlertDialog(
				onDismissRequest = { confirmDeleteActivity = null },
				title = { Text("Move to trash", color = TextPrimary) },
				text = { Text("Are you sure you want to delete this activity? This removes it from the feed.", color = TextSecondary) },
				confirmButton = {
					TextButton(
						onClick = {
							viewModel.deletePostFromProfile(activity)
							confirmDeleteActivity = null
						}
					) {
						Text("Delete", color = Danger)
					}
				},
				dismissButton = {
					TextButton(onClick = { confirmDeleteActivity = null }) {
						Text("Cancel", color = TextPrimary)
					}
				},
				containerColor = Card
			)
		}

		if (editingActivity != null) {
			val activity = editingActivity!!
			AlertDialog(
				onDismissRequest = { editingActivity = null },
				title = { Text("Edit Post", color = TextPrimary) },
				text = {
					androidx.compose.material3.OutlinedTextField(
						value = editPostText,
						onValueChange = { editPostText = it },
						placeholder = { Text("What's on your mind?", color = TextSecondary) },
						modifier = Modifier.fillMaxWidth(),
						textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary),
						colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
							focusedBorderColor = Blue,
							unfocusedBorderColor = Border,
							focusedLabelColor = Blue,
							cursorColor = Blue
						)
					)
				},
				confirmButton = {
					TextButton(
						onClick = {
							if (editPostText.isNotBlank()) {
								viewModel.editPostActivity(activity, editPostText)
								editingActivity = null
							}
						}
					) {
						Text("Save", color = Blue)
					}
				},
				dismissButton = {
					TextButton(onClick = { editingActivity = null }) {
						Text("Cancel", color = TextSecondary)
					}
				},
				containerColor = Card
			)
		}
	}
}

@Composable
fun ProfileHeader(
	state: ProfileUiState,
	onBack: () -> Unit,
	onOpenSettings: () -> Unit,
	isOnline: Boolean,
	onEditCoverImage: () -> Unit,
	showBackButton: Boolean = true,
	showSettingsButton: Boolean = true
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
			if (showBackButton) {
				ProfileCircleIconButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack)
			} else {
				Spacer(Modifier.size(40.dp))
			}
			
			if (showSettingsButton) {
				ProfileCircleIconButton(icon = Icons.Filled.Settings, onClick = onOpenSettings)
			} else {
				Spacer(Modifier.size(40.dp))
			}
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
fun ProfileMeta(state: ProfileUiState) {
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
fun ProfileStatsSection(state: ProfileUiState) {
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
fun ProfileActionsSection(
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
			ProfileActionCard(
				title = "Requests",
				icon = Icons.Outlined.PersonAdd,
				badge = if (state.stats.friendRequests > 0) state.stats.friendRequests.toString() else null,
				modifier = Modifier.weight(1f),
				onClick = onOpenFriendRequests,
			)
			ProfileActionCard(
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

		ProfileStoryRow()
	}
}

@Composable
fun ProfileActionCard(
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
fun ProfileStoryRow() {
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
fun ProfileAboutCard(state: ProfileUiState) {
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
		ProfileInfoLine(icon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = TextSecondary) }, text = state.faculty.ifEmpty { "Add faculty" })
		ProfileInfoLine(icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary) }, text = state.year.ifEmpty { "Add year" })
		ProfileInfoLine(icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary) }, text = state.location.ifEmpty { "Add location" })
	}
}

@Composable
fun RecentActivitySection(
	activities: List<ProfileActivityItem>,
	displayName: String,
	avatarEmoji: String,
	profileImageUrl: String,
	viewModel: ProfileViewModel,
	onOpenActivity: (ProfileActivityItem) -> Unit,
	onCommentClick: (ProfileActivityItem) -> Unit,
	onMenuClick: (ProfileActivityItem) -> Unit,
) {
	val displayTypes = setOf(
		"create_post",
		"create_event",
		"share_post",
		"create_group",
		"share_profile",
		"comment",
		"reply",
		"pin_post",
		"unpin_post",
		"edit_post",
		"edit_privacy",
		"archive_activity",
	)
	val itemsToShow = activities.filter { it.type in displayTypes }

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp, start = 24.dp, end = 24.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Text(text = "Recent Activity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

		if (itemsToShow.isEmpty()) {
			Text(
				text = "No recent activity yet. Your posts, events, and groups will appear here.",
				color = TextSecondary,
				fontSize = 13.sp,
			)
		} else {
			itemsToShow.forEach { activity ->
				when (activity.type) {
					"create_post"    -> ActivityPostCard(
						activity = activity,
						displayName = displayName,
						avatarEmoji = avatarEmoji,
						profileImageUrl = profileImageUrl,
						onLikeClick = { viewModel.toggleActivityLove(activity) },
						onCommentClick = { onCommentClick(activity) },
						onCardClick = { onOpenActivity(activity) },
						onMenuClick = { onMenuClick(activity) }
					)
					"create_event"   -> ActivityEventCard(
						activity = activity,
						displayName = displayName,
						avatarEmoji = avatarEmoji,
						profileImageUrl = profileImageUrl,
						onLikeClick = { viewModel.toggleActivityLove(activity) },
						onInterestClick = { viewModel.toggleActivityInterest(activity) },
						onCommentClick = { onCommentClick(activity) },
						onCardClick = { onOpenActivity(activity) },
						onMenuClick = { onMenuClick(activity) }
					)
					"share_post"     -> ActivitySharePostCard(
						activity = activity,
						displayName = displayName,
						avatarEmoji = avatarEmoji,
						profileImageUrl = profileImageUrl,
						onLikeClick = { viewModel.toggleActivityLove(activity) },
						onCommentClick = { onCommentClick(activity) },
						onCardClick = { onOpenActivity(activity) },
						onMenuClick = { onMenuClick(activity) }
					)
					"create_group"   -> ActivityGroupCard(
						activity = activity,
						displayName = displayName,
						avatarEmoji = avatarEmoji,
						profileImageUrl = profileImageUrl,
						onLikeClick = { viewModel.toggleActivityLove(activity) },
						onCommentClick = { onCommentClick(activity) },
						onCardClick = { onOpenActivity(activity) },
						onMenuClick = { onMenuClick(activity) }
					)
					"share_profile" -> ActivityShareProfileCard(activity)
					else -> ActivityGenericCard(
						activity = activity,
						onCardClick = { onOpenActivity(activity) },
						onMenuClick = { onMenuClick(activity) },
					)
				}
			}
		}
	}
}

/* ───────────────────────────── helpers ───────────────────────────── */

@Composable
fun ActivityCardHeader(
	displayName: String,
	avatarEmoji: String,
	profileImageUrl: String,
	activity: ProfileActivityItem,
	onMenuClick: () -> Unit = {}
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically,
	) {
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
			if (profileImageUrl.isNotBlank()) {
				AsyncImage(
					model = profileImageUrl,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier
						.size(36.dp)
						.clip(CircleShape)
				)
			} else {
				Box(
					modifier = Modifier
						.size(36.dp)
						.clip(CircleShape)
						.background(Color.White.copy(alpha = 0.12f)),
					contentAlignment = Alignment.Center,
				) {
					Text(text = avatarEmoji.ifBlank { "👤" }, fontSize = 18.sp)
				}
			}
			Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
				Text(text = displayName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
				Text(text = formatActivityTime(activity.createdAt), color = TextSecondary, fontSize = 12.sp)
			}
		}
		Icon(
			Icons.Outlined.MoreVert,
			contentDescription = null,
			tint = TextSecondary,
			modifier = Modifier
				.size(18.dp)
				.clickable { onMenuClick() }
		)
	}
}

@Composable
fun ActivityActionItem(
	icon: ImageVector,
	tint: Color,
	count: Int,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.clip(RoundedCornerShape(8.dp))
			.clickable(onClick = onClick)
			.padding(horizontal = 8.dp, vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
		Text(text = count.toString(), color = TextSecondary, fontSize = 13.sp)
	}
}

@Composable
fun ActivityMediaGallery(
	mediaUrls: List<String>,
	mediaTypes: List<String>,
	fallbackImageUrl: String,
	height: Dp = 200.dp,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	if (mediaUrls.isNotEmpty()) {
		LazyRow(
			modifier = modifier
				.padding(horizontal = 14.dp, vertical = 6.dp)
				.fillMaxWidth()
				.height(height)
				.clip(RoundedCornerShape(12.dp))
				.background(Color.Black.copy(alpha = 0.2f))
				.border(1.dp, Border, RoundedCornerShape(12.dp)),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			items(mediaUrls.size) { index ->
				val mediaUrl = mediaUrls[index]
				val isVideo = mediaTypes.getOrNull(index)?.equals("VIDEO", ignoreCase = true) == true

				if (isVideo) {
					Box(
						modifier = Modifier
							.width(280.dp)
							.height(height)
							.fillParentMaxHeight()
					) {
						val player = remember(mediaUrl) {
							ExoPlayer.Builder(context).build().apply {
								setMediaItem(MediaItem.fromUri(mediaUrl))
								prepare()
								playWhenReady = false
							}
						}
						DisposableEffect(player) {
							onDispose { player.release() }
						}
						AndroidView(
							modifier = Modifier.fillMaxSize(),
							factory = { ctx ->
								PlayerView(ctx).apply {
									useController = true
									this.player = player
								}
							},
							update = { it.player = player },
						)
					}
				} else {
					Box(
						modifier = Modifier
							.width(280.dp)
							.height(height)
							.fillParentMaxHeight()
					) {
						AsyncImage(
							model = mediaUrl,
							contentDescription = "Activity media",
							modifier = Modifier.fillMaxSize(),
							contentScale = ContentScale.Crop,
						)
					}
				}
			}
		}
	} else if (fallbackImageUrl.isNotBlank()) {
		AsyncImage(
			model = fallbackImageUrl,
			contentDescription = null,
			contentScale = ContentScale.Crop,
			modifier = modifier
				.fillMaxWidth()
				.height(height)
				.padding(horizontal = 14.dp, vertical = 6.dp)
				.clip(RoundedCornerShape(12.dp)),
		)
	}
}

/* ─────────────────── create_post card ─────────────────── */

@Composable
fun ActivityPostCard(
	activity: ProfileActivityItem,
	displayName: String,
	avatarEmoji: String,
	profileImageUrl: String,
	onLikeClick: () -> Unit,
	onCommentClick: () -> Unit = {},
	onCardClick: () -> Unit = {},
	onMenuClick: () -> Unit = {}
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(18.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(18.dp))
			.clickable(onClick = onCardClick),
		verticalArrangement = Arrangement.spacedBy(0.dp),
	) {
		ActivityCardHeader(
			displayName = displayName,
			avatarEmoji = avatarEmoji,
			profileImageUrl = profileImageUrl,
			activity = activity,
			onMenuClick = onMenuClick
		)

		val displayContent = if (activity.sharedOriginalPostId != null) {
			val sharedPrefix = "Shared from ${activity.sharedOriginalAuthor}"
			val stripped = activity.text
				.substringBefore("\n\n$sharedPrefix")
				.substringBefore("\n$sharedPrefix")
				.trim()
			stripped
		} else {
			activity.text
		}

		if (displayContent.isNotBlank()) {
			Text(
				text = displayContent,
				color = TextPrimary,
				fontSize = 15.sp,
				fontWeight = FontWeight.Medium,
				lineHeight = 21.sp,
				maxLines = 4,
				modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
			)
		}

		if (activity.sharedOriginalPostId != null) {
			Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
				com.example.kampus.ui.post.SharedOriginalCard(
					author = activity.sharedOriginalAuthor ?: "Unknown",
					authorId = activity.sharedOriginalAuthorId.orEmpty(),
					avatar = activity.sharedOriginalAvatar ?: "👤",
					profileImageUrl = activity.sharedOriginalProfileImageUrl.orEmpty(),
					time = activity.sharedOriginalTime ?: "now",
					content = activity.sharedOriginalContent.orEmpty(),
					mediaUris = activity.sharedOriginalMediaUrls.map { android.net.Uri.parse(it) },
					mediaTypes = activity.sharedOriginalMediaTypes.map {
						if (it.lowercase() == "video") com.example.kampus.ui.feed.PostItem.MediaType.VIDEO else com.example.kampus.ui.feed.PostItem.MediaType.IMAGE
					},
					mediaEmojis = activity.sharedOriginalMediaEmojis,
					likes = activity.sharedOriginalLikes ?: 0,
					comments = activity.sharedOriginalComments ?: 0,
					shares = activity.sharedOriginalShares ?: 0,
					isVerified = activity.sharedOriginalIsVerified ?: false,
					onClick = { onCardClick() }
				)
			}
			Spacer(modifier = Modifier.height(6.dp))
		} else {
			ActivityMediaGallery(
				mediaUrls = activity.mediaUrls,
				mediaTypes = activity.mediaTypes,
				fallbackImageUrl = activity.previewImageUrl,
				height = 200.dp
			)
		}

		Spacer(modifier = Modifier.height(4.dp))

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 14.dp, vertical = 8.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			ActivityActionItem(
				icon = if (activity.currentUserLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				tint = if (activity.currentUserLoved) Danger else TextSecondary,
				count = activity.likeCount,
				onClick = onLikeClick
			)
			ActivityActionItem(
				icon = Icons.Outlined.ChatBubbleOutline,
				tint = TextSecondary,
				count = activity.commentCount,
				onClick = onCommentClick
			)
			ActivityActionItem(
				icon = Icons.AutoMirrored.Outlined.Send,
				tint = TextSecondary,
				count = activity.shareCount,
				onClick = {}
			)
		}
		Spacer(modifier = Modifier.height(6.dp))
	}
}

/* ─────────────────── create_event card ─────────────────── */

@Composable
fun ActivityEventCard(
	activity: ProfileActivityItem,
	displayName: String,
	avatarEmoji: String,
	profileImageUrl: String,
	onLikeClick: () -> Unit,
	onInterestClick: () -> Unit,
	onCommentClick: () -> Unit = {},
	onCardClick: () -> Unit = {},
	onMenuClick: () -> Unit = {}
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(18.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(18.dp))
			.clickable(onClick = onCardClick),
	) {
		// Cover banner
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(150.dp),
		) {
			if (activity.previewImageUrl.isNotBlank()) {
				AsyncImage(
					model = activity.previewImageUrl,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(
							Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF0D7FFF))),
						),
				)
			}
			// dim overlay
			Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
			
			// Category badge
			Box(
				modifier = Modifier
					.align(Alignment.TopStart)
					.padding(10.dp)
					.clip(RoundedCornerShape(6.dp))
					.background(Blue.copy(alpha = 0.85f))
					.border(1.dp, Border.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
					.padding(horizontal = 8.dp, vertical = 4.dp),
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(4.dp)
				) {
					Text("🎓", fontSize = 11.sp)
					Text("CAMPUS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
				}
			}

			// Bookmark badge
			Box(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(10.dp)
					.size(32.dp)
					.clip(CircleShape)
					.background(Color.Black.copy(alpha = 0.4f)),
				contentAlignment = Alignment.Center
			) {
				Icon(
					Icons.Outlined.BookmarkBorder,
					contentDescription = "Save",
					tint = Color.White,
					modifier = Modifier.size(16.dp)
				)
			}
		}

		// Body Header
		ActivityCardHeader(
			displayName = displayName,
			avatarEmoji = avatarEmoji,
			profileImageUrl = profileImageUrl,
			activity = activity,
			onMenuClick = onMenuClick
		)

		// Event Title & Details
		Column(
			modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Text(
				text = activity.previewTitle.ifBlank { activity.text },
				color = TextPrimary,
				fontSize = 18.sp,
				fontWeight = FontWeight.Bold,
				lineHeight = 24.sp,
				maxLines = 2,
			)

			// Details list
			Column(
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Blue, modifier = Modifier.size(16.dp))
					val dateStr = buildString {
						append(activity.eventDate.ifBlank { "TBA" })
						if (activity.eventTime.isNotBlank()) append(" · ${activity.eventTime}")
					}
					Text(text = dateStr, color = TextSecondary, fontSize = 13.sp)
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Blue, modifier = Modifier.size(16.dp))
					Text(text = activity.eventLocation.ifBlank { "TBA" }, color = TextSecondary, fontSize = 13.sp)
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Icon(Icons.Outlined.Group, contentDescription = null, tint = Blue, modifier = Modifier.size(16.dp))
					Text(text = "${activity.eventInterestedCount} people interested", color = TextSecondary, fontSize = 13.sp)
				}
			}

			Spacer(modifier = Modifier.height(4.dp))

			// Reaction Row - compact left-aligned
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 4.dp),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				ActivityActionItem(
					icon = if (activity.currentUserLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
					tint = if (activity.currentUserLoved) Danger else TextSecondary,
					count = activity.likeCount,
					onClick = onLikeClick
				)
				ActivityActionItem(
					icon = Icons.Outlined.ChatBubbleOutline,
					tint = TextSecondary,
					count = activity.commentCount,
					onClick = onCommentClick
				)
				ActivityActionItem(
					icon = Icons.AutoMirrored.Outlined.Send,
					tint = TextSecondary,
					count = activity.shareCount,
					onClick = {}
				)
			}

			Spacer(modifier = Modifier.height(6.dp))

			// "I'm Interested" Button
			Button(
				onClick = onInterestClick,
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = if (activity.currentUserInterested) Card else Blue,
					contentColor = if (activity.currentUserInterested) TextPrimary else Color.White
				),
				border = if (activity.currentUserInterested) BorderStroke(1.dp, Border) else null
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					Icon(
						if (activity.currentUserInterested) Icons.Filled.Stars else Icons.Outlined.Stars,
						contentDescription = null,
						modifier = Modifier.size(18.dp)
					)
					Text(
						text = if (activity.currentUserInterested) "Interested" else "I'm Interested",
						fontWeight = FontWeight.Medium,
						fontSize = 14.sp
					)
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
		}
	}
}

/* ─────────────────── share_post card ─────────────────── */

@Composable
fun ActivitySharePostCard(
	activity: ProfileActivityItem,
	displayName: String,
	avatarEmoji: String,
	profileImageUrl: String,
	onLikeClick: () -> Unit,
	onCommentClick: () -> Unit = {},
	onCardClick: () -> Unit = {},
	onMenuClick: () -> Unit = {}
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(18.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(18.dp))
			.clickable(onClick = onCardClick),
	) {
		ActivityCardHeader(
			displayName = displayName,
			avatarEmoji = avatarEmoji,
			profileImageUrl = profileImageUrl,
			activity = activity,
			onMenuClick = onMenuClick
		)

		if (activity.previewTitle.isNotBlank()) {
			Text(
				text = activity.previewTitle,
				color = TextPrimary,
				fontSize = 15.sp,
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.padding(horizontal = 14.dp),
			)
		}

		val displayContent = if (activity.sharedOriginalPostId != null) {
			val sharedPrefix = "Shared from ${activity.sharedOriginalAuthor}"
			val stripped = activity.text
				.substringBefore("\n\n$sharedPrefix")
				.substringBefore("\n$sharedPrefix")
				.trim()
			stripped
		} else {
			activity.text
		}

		if (displayContent.isNotBlank()) {
			Text(
				text = displayContent,
				color = TextSecondary,
				fontSize = 13.sp,
				maxLines = 3,
				modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
			)
		}

		if (activity.sharedOriginalPostId != null) {
			Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
				com.example.kampus.ui.post.SharedOriginalCard(
					author = activity.sharedOriginalAuthor ?: "Unknown",
					authorId = activity.sharedOriginalAuthorId.orEmpty(),
					avatar = activity.sharedOriginalAvatar ?: "👤",
					profileImageUrl = activity.sharedOriginalProfileImageUrl.orEmpty(),
					time = activity.sharedOriginalTime ?: "now",
					content = activity.sharedOriginalContent.orEmpty(),
					mediaUris = activity.sharedOriginalMediaUrls.map { android.net.Uri.parse(it) },
					mediaTypes = activity.sharedOriginalMediaTypes.map {
						if (it.lowercase() == "video") com.example.kampus.ui.feed.PostItem.MediaType.VIDEO else com.example.kampus.ui.feed.PostItem.MediaType.IMAGE
					},
					mediaEmojis = activity.sharedOriginalMediaEmojis,
					likes = activity.sharedOriginalLikes ?: 0,
					comments = activity.sharedOriginalComments ?: 0,
					shares = activity.sharedOriginalShares ?: 0,
					isVerified = activity.sharedOriginalIsVerified ?: false,
					onClick = { onCardClick() }
				)
			}
			Spacer(modifier = Modifier.height(6.dp))
		} else {
			ActivityMediaGallery(
				mediaUrls = activity.mediaUrls,
				mediaTypes = activity.mediaTypes,
				fallbackImageUrl = activity.previewImageUrl,
				height = 180.dp
			)
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 14.dp, vertical = 8.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			ActivityActionItem(
				icon = if (activity.currentUserLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				tint = if (activity.currentUserLoved) Danger else TextSecondary,
				count = activity.likeCount,
				onClick = onLikeClick
			)
			ActivityActionItem(
				icon = Icons.Outlined.ChatBubbleOutline,
				tint = TextSecondary,
				count = activity.commentCount,
				onClick = onCommentClick
			)
			ActivityActionItem(
				icon = Icons.AutoMirrored.Outlined.Send,
				tint = TextSecondary,
				count = activity.shareCount,
				onClick = {}
			)
		}
		Spacer(modifier = Modifier.height(12.dp))
	}
}

/* ─────────────────── create_group card ─────────────────── */

@Composable
fun ActivityGroupCard(
	activity: ProfileActivityItem,
	displayName: String,
	avatarEmoji: String,
	profileImageUrl: String,
	onLikeClick: () -> Unit,
	onCommentClick: () -> Unit = {},
	onCardClick: () -> Unit = {},
	onMenuClick: () -> Unit = {}
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(18.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(18.dp))
			.clickable(onClick = onCardClick),
	) {
		ActivityCardHeader(
			displayName = displayName,
			avatarEmoji = avatarEmoji,
			profileImageUrl = profileImageUrl,
			activity = activity,
			onMenuClick = onMenuClick
		)

		// Group details panel
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 14.dp, vertical = 6.dp)
				.clip(RoundedCornerShape(12.dp))
				.background(Blue.copy(alpha = 0.08f))
				.border(1.dp, Blue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
				.padding(12.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Box(
				modifier = Modifier
					.size(44.dp)
					.clip(RoundedCornerShape(10.dp))
					.background(Blue.copy(alpha = 0.2f)),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					Icons.Outlined.Group,
					contentDescription = null,
					tint = Blue,
					modifier = Modifier.size(22.dp)
				)
			}

			Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
				Text(
					text = activity.previewTitle.ifBlank { "Untitled Group" },
					color = TextPrimary,
					fontSize = 14.sp,
					fontWeight = FontWeight.Bold,
					maxLines = 1,
				)
				Text(
					text = activity.previewSubtitle.ifBlank { "Shared from groups" },
					color = TextSecondary,
					fontSize = 12.sp,
					maxLines = 1,
				)
			}

			Text(
				text = "View",
				color = Blue,
				fontSize = 12.sp,
				fontWeight = FontWeight.SemiBold,
			)
		}

		if (activity.text.isNotBlank() && activity.text != "Created a group" && !activity.text.startsWith("Created group:")) {
			Text(
				text = activity.text,
				color = TextPrimary,
				fontSize = 14.sp,
				fontWeight = FontWeight.Normal,
				modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
			)
		}

		Spacer(modifier = Modifier.height(4.dp))

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 14.dp, vertical = 8.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			ActivityActionItem(
				icon = if (activity.currentUserLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				tint = if (activity.currentUserLoved) Danger else TextSecondary,
				count = activity.likeCount,
				onClick = onLikeClick
			)
			ActivityActionItem(
				icon = Icons.Outlined.ChatBubbleOutline,
				tint = TextSecondary,
				count = activity.commentCount,
				onClick = onCommentClick
			)
			ActivityActionItem(
				icon = Icons.AutoMirrored.Outlined.Send,
				tint = TextSecondary,
				count = activity.shareCount,
				onClick = {}
			)
		}
		Spacer(modifier = Modifier.height(6.dp))
	}
}

/* ─────────────────── share_profile card ─────────────────── */


@Composable
private fun ActivityShareProfileCard(activity: ProfileActivityItem) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(18.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(18.dp))
			.padding(14.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.size(44.dp)
				.clip(CircleShape)
				.background(Blue.copy(alpha = 0.2f)),
			contentAlignment = Alignment.Center,
		) {
			Icon(Icons.Outlined.Share, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
		}

		Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
			Text(
				text = activity.previewTitle.ifBlank { "Shared a profile" },
				color = TextPrimary,
				fontSize = 14.sp,
				fontWeight = FontWeight.SemiBold,
			)
			Text(
				text = formatActivityTime(activity.createdAt),
				color = TextSecondary,
				fontSize = 12.sp,
			)
		}
	}
}

@Composable
private fun ActivityGenericCard(
	activity: ProfileActivityItem,
	onCardClick: () -> Unit,
	onMenuClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(18.dp))
			.background(Card)
			.border(1.dp, Border, RoundedCornerShape(18.dp))
			.clickable(onClick = onCardClick)
			.padding(14.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.size(44.dp)
				.clip(CircleShape)
				.background(Blue.copy(alpha = 0.18f)),
			contentAlignment = Alignment.Center,
		) {
			Icon(Icons.Outlined.Public, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
		}

		Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
			Text(
				text = activity.previewTitle.ifBlank { activity.text.ifBlank { "Recent activity" } },
				color = TextPrimary,
				fontSize = 14.sp,
				fontWeight = FontWeight.SemiBold,
				maxLines = 2,
			)
			Text(
				text = activity.previewSubtitle.ifBlank { formatActivityTime(activity.createdAt) },
				color = TextSecondary,
				fontSize = 12.sp,
				maxLines = 2,
			)
		}

		Icon(
			Icons.Outlined.MoreVert,
			contentDescription = null,
			tint = TextSecondary,
			modifier = Modifier
				.size(18.dp)
				.clickable { onMenuClick() }
		)
	}
}

/* ─────────────────── utilities ─────────────────── */

private fun formatActivityTime(timestamp: Long): String {
	if (timestamp <= 0L) return "just now"
	val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0L)
	return when {
		minutes < 1    -> "just now"
		minutes < 60   -> "${minutes}m ago"
		minutes < 1440 -> "${minutes / 60}h ago"
		else           -> "${minutes / 1440}d ago"
	}
}

@Composable
fun ProfileInfoLine(icon: @Composable () -> Unit, text: String) {
	Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
		icon()
		val textColor = com.example.kampus.ui.theme.KampusColors.TextMuted
		Text(text = text, color = textColor, fontSize = 13.sp)
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
fun ProfileCircleIconButton(icon: ImageVector, onClick: () -> Unit) {
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

@Composable
private fun MenuItemLarge(
	icon: ImageVector,
	label: String,
	tint: Color,
	subtitle: String? = null,
	onClick: () -> Unit = {}
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(64.dp)
			.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
			.padding(horizontal = 16.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(
			modifier = Modifier
				.size(44.dp)
				.clip(RoundedCornerShape(10.dp))
				.background(NavBg)
				.border(1.dp, Border, RoundedCornerShape(10.dp)),
			contentAlignment = Alignment.Center,
		) {
			Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
		}

		Column(modifier = Modifier.padding(start = 14.dp)) {
			Text(label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
			if (!subtitle.isNullOrEmpty()) {
				Text(subtitle, color = TextSecondary, fontSize = 13.sp)
			}
		}
	}
}

