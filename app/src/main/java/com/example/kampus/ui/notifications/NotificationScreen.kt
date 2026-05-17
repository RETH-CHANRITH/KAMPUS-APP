package com.example.kampus.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NotificationScreen(
	onBack: () -> Unit,
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
				Text(
					text = strings.notificationsTitle,
					color = colors.onBackground,
					fontSize = 22.sp,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.align(Alignment.Center),
				)
			}

			when {
				state.isLoading -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						CircularProgressIndicator(color = colors.primary)
					}
				}

				state.error != null -> {
					Text(
						text = state.error,
						color = MaterialTheme.colorScheme.error,
						fontSize = 14.sp,
					)
				}

				state.notifications.isEmpty() -> {
					Text(
						text = strings.noActivityYet,
						color = colors.onSurfaceVariant,
						fontSize = 14.sp,
					)
				}

				else -> {
					LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
						items(state.notifications, key = { it.id }) { item ->
							NotificationItem(
								item = item,
								onClick = { viewModel.markAsRead(item.id) },
							)
						}
						item { Spacer(Modifier.size(6.dp)) }
					}
				}
			}
		}
	}
}

