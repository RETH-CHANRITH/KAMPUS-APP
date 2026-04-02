package com.example.kampus.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditProfileScreen(
	onBack: () -> Unit,
	onSaved: () -> Unit,
	viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
	val state = viewModel.uiState.value
	var username by remember { mutableStateOf(state.displayName) }
	var bio by remember { mutableStateOf(state.bio) }
	var email by remember { mutableStateOf(state.email) }
	var phone by remember { mutableStateOf(state.phone) }

	Surface(
		modifier = Modifier.fillMaxSize(),
		color = Color(0xFF1A1D2E),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.statusBarsPadding()
				.navigationBarsPadding()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 20.dp, vertical = 12.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Box(
					modifier = Modifier
						.size(40.dp)
						.clip(CircleShape)
						.background(Color.White.copy(alpha = 0.1f))
						.clickable(onClick = onBack),
					contentAlignment = Alignment.Center,
				) {
					Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
				}
				Text("Edit Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
				Spacer(Modifier.size(40.dp))
			}

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 8.dp, bottom = 6.dp),
				contentAlignment = Alignment.Center,
			) {
				Box(contentAlignment = Alignment.BottomEnd) {
					Box(
						modifier = Modifier
							.size(96.dp)
							.clip(CircleShape)
							.background(Color.White.copy(alpha = 0.12f))
							.clickable { },
						contentAlignment = Alignment.Center,
					) {
						Icon(
							imageVector = Icons.Outlined.Person,
							contentDescription = "Profile picture",
							tint = Color.White.copy(alpha = 0.9f),
							modifier = Modifier.size(44.dp),
						)
					}

					Box(
						modifier = Modifier
							.size(30.dp)
							.clip(CircleShape)
							.background(Color(0xFF0D7FFF))
							.clickable { },
						contentAlignment = Alignment.Center,
					) {
						Icon(
							imageVector = Icons.Outlined.Edit,
							contentDescription = "Edit profile picture",
							tint = Color.White,
							modifier = Modifier.size(16.dp),
						)
					}
				}
			}

			Column(
				modifier = Modifier
					.fillMaxWidth()
					.clip(RoundedCornerShape(16.dp))
					.background(Color(0xFF252A41))
					.padding(14.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				OutlinedTextField(
					value = username,
					onValueChange = { username = it },
					label = { Text("Username") },
					modifier = Modifier.fillMaxWidth(),
				)
				OutlinedTextField(
					value = email,
					onValueChange = { email = it },
					label = { Text("Email") },
					modifier = Modifier.fillMaxWidth(),
				)
				OutlinedTextField(
					value = phone,
					onValueChange = { phone = it },
					label = { Text("Phone") },
					modifier = Modifier.fillMaxWidth(),
				)
				OutlinedTextField(
					value = bio,
					onValueChange = { bio = it },
					label = { Text("Bio") },
					modifier = Modifier.fillMaxWidth(),
					minLines = 3,
				)
			}

			Button(
				onClick = onSaved,
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = Color(0xFF0D7FFF),
					contentColor = Color.White,
				),
			) {
				Text("Save Changes", fontWeight = FontWeight.Medium)
			}
		}
	}
}
