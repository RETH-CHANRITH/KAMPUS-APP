package com.example.kampus.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kampus.navigation.Routes

@Composable
fun NotificationScreen(
	onBack: () -> Unit,
	onNavigate: (String) -> Unit,
	viewModel: NotificationViewModel = viewModel(),
) {
	val state = viewModel.uiState.collectAsStateWithLifecycle().value
	val strings = com.example.kampus.ui.localization.rememberUiStrings()
	val colors = MaterialTheme.colorScheme

	Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.statusBarsPadding()
				.navigationBarsPadding()
				.padding(horizontal = 20.dp, vertical = 12.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			Box(modifier = Modifier.fillMaxWidth()) {
				Box(
					modifier = Modifier
						.size(40.dp)
						.clip(CircleShape)
						.background(colors.surfaceVariant)
						.clickable(onClick = onBack),
					contentAlignment = Alignment.Center,
				) {
					Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.onSurface)
				}
				Column(
					modifier = Modifier.align(Alignment.Center),
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Text(
						text = strings.notificationsTitle,
						color = colors.onBackground,
						fontSize = 22.sp,
						fontWeight = FontWeight.Bold,
					)
					Text(
						text = strings.noActivityYet,
						color = colors.onSurfaceVariant,
						fontSize = 12.sp,
					)
				}
			}

			when {
				state.isLoading -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							CircularProgressIndicator(color = colors.primary)
							Spacer(Modifier.size(14.dp))
							Text(
								text = "Loading alerts...",
								color = colors.onSurfaceVariant,
								fontSize = 13.sp,
							)
						}
					}
				}

				state.error != null -> {
					Surface(
						shape = RoundedCornerShape(18.dp),
						color = MaterialTheme.colorScheme.errorContainer,
						modifier = Modifier.fillMaxWidth(),
					) {
						Text(
							text = state.error,
							color = MaterialTheme.colorScheme.onErrorContainer,
							fontSize = 14.sp,
							modifier = Modifier.padding(16.dp),
						)
					}
				}

				state.groupedNotifications.isEmpty() -> {
					Surface(
						shape = RoundedCornerShape(24.dp),
						color = colors.surface,
						modifier = Modifier.fillMaxWidth(),
					) {
						Column(
							modifier = Modifier.padding(28.dp),
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.spacedBy(10.dp),
						) {
							Box(
								modifier = Modifier
									.size(56.dp)
									.clip(CircleShape)
									.background(colors.primary.copy(alpha = 0.10f)),
								contentAlignment = Alignment.Center,
							) {
								Icon(
									imageVector = Icons.Outlined.NotificationsNone,
									contentDescription = null,
									tint = colors.primary,
								)
							}
							Text(
								text = strings.noActivityYet,
								color = colors.onSurface,
								fontSize = 15.sp,
								fontWeight = FontWeight.SemiBold,
							)
							Text(
								text = "You’ll see likes, comments, chats, and calls here.",
								color = colors.onSurfaceVariant,
								fontSize = 13.sp,
								textAlign = TextAlign.Center,
							)
						}
					}
				}

				else -> {
					Surface(
						shape = RoundedCornerShape(26.dp),
						color = colors.surface,
						modifier = Modifier.fillMaxSize(),
					) {
						LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(8.dp)) {
							items(state.groupedNotifications, key = { it.id }) { item ->
								NotificationItem(
									item = item,
									onClick = {
										viewModel.markGroupAsRead(item)
										val targetId = item.targetId
										val actorUserId = item.actorUserIds.firstOrNull().orEmpty()
										when (item.type) {
											"chat_message", "story_reply" -> {
												if (targetId.isNotBlank()) {
													onNavigate(Routes.chatScreen(targetId))
												}
											}
											"like", "love", "reaction" -> {
												val postId = targetId.toIntOrNull()
												if (postId != null) {
													onNavigate(Routes.postDetail(postId))
												}
											}
											"comment" -> {
												val postId = targetId.toIntOrNull()
												if (postId != null) {
													onNavigate(Routes.postDetail(postId, openComposer = true))
												}
											}
											"follow", "friend_request" -> {
												if (actorUserId.isNotBlank()) {
													onNavigate(Routes.profilePublic(actorUserId))
												}
											}
											"mention" -> {
												val postId = targetId.toIntOrNull()
												if (postId != null) {
													onNavigate(Routes.postDetail(postId))
												}
											}
										}
									},
								)
							}
							item { Spacer(Modifier.size(6.dp)) }
						}
					}
				}
			}
		}
	}
}
