package com.example.kampus.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PostDetailScreen(
	postId: Int,
	onBack: () -> Unit,
	viewModel: PostViewModel = viewModel(),
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val colors = MaterialTheme.colorScheme

	LaunchedEffect(postId) {
		viewModel.observePost(postId)
	}

	Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.statusBarsPadding()
				.padding(horizontal = 16.dp, vertical = 10.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Box(
					modifier = Modifier
						.size(40.dp)
						.clip(CircleShape)
						.background(colors.surfaceVariant)
						.clickable(onClick = onBack),
					contentAlignment = Alignment.Center,
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
						contentDescription = "Back",
						tint = colors.onSurface,
						modifier = Modifier.size(20.dp),
					)
				}
				Text(text = "Post Detail", color = colors.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
			}

			Spacer(modifier = Modifier.height(16.dp))

			when {
				state.isLoading -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						CircularProgressIndicator(color = colors.primary)
					}
				}

				state.post == null -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						Text(
							text = state.error ?: "Post not found",
							color = colors.onSurfaceVariant,
							fontSize = 14.sp,
						)
					}
				}

				else -> {
					val post = state.post!!
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(16.dp))
							.background(colors.surface)
							.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp),
					) {
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(10.dp),
						) {
							Text(text = post.avatar, fontSize = 22.sp)
							Column {
								Text(text = post.author, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
								Text(text = post.time, color = colors.onSurfaceVariant, fontSize = 12.sp)
							}
						}
						Text(text = post.content, color = colors.onSurface, fontSize = 15.sp, lineHeight = 22.sp)
						Text(
							text = "${post.likes} likes • ${post.comments} comments",
							color = colors.onSurfaceVariant,
							fontSize = 12.sp,
						)
					}
				}
			}
		}
	}
}

