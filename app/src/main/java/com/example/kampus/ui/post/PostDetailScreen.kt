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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Bg = Color(0xFF080B11)
private val Card = Color(0xFF252A41)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF99A1AF)
private val Blue = Color(0xFF0D7FFF)

@Composable
fun PostDetailScreen(
	postId: Int,
	onBack: () -> Unit,
	viewModel: PostViewModel = viewModel(),
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(postId) {
		viewModel.observePost(postId)
	}

	Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
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
						.background(Color.White.copy(alpha = 0.1f))
						.clickable(onClick = onBack),
					contentAlignment = Alignment.Center,
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
						contentDescription = "Back",
						tint = TextPrimary,
						modifier = Modifier.size(20.dp),
					)
				}
				Text(text = "Post Detail", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
			}

			Spacer(modifier = Modifier.height(16.dp))

			when {
				state.isLoading -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						CircularProgressIndicator(color = Blue)
					}
				}

				state.post == null -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						Text(
							text = state.error ?: "Post not found",
							color = TextSecondary,
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
							.background(Card)
							.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp),
					) {
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(10.dp),
						) {
							Text(text = post.avatar, fontSize = 22.sp)
							Column {
								Text(text = post.author, color = TextPrimary, fontWeight = FontWeight.SemiBold)
								Text(text = post.time, color = TextSecondary, fontSize = 12.sp)
							}
						}
						Text(text = post.content, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
						Text(
							text = "${post.likes} likes • ${post.comments} comments",
							color = TextSecondary,
							fontSize = 12.sp,
						)
					}
				}
			}
		}
	}
}

