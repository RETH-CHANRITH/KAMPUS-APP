package com.example.kampus.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.example.kampus.domain.model.User

private val yearOptions = listOf("Year 1", "Year 2", "Year 3", "Year 4", "Year 5")

private val cambodiaLocations = listOf(
	"Phnom Penh",
	"Banteay Meanchey",
	"Battambang",
	"Kampong Cham",
	"Kampong Chhnang",
	"Kampong Speu",
	"Kampong Thom",
	"Kampot",
	"Kandal",
	"Kep",
	"Koh Kong",
	"Kratie",
	"Mondulkiri",
	"Oddar Meanchey",
	"Pailin",
	"Preah Sihanouk",
	"Preah Vihear",
	"Prey Veng",
	"Pursat",
	"Ratanakiri",
	"Siem Reap",
	"Stung Treng",
	"Svay Rieng",
	"Takeo",
	"Tboung Khmum",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
	onBack: () -> Unit,
	onSaved: () -> Unit,
	viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	var yearExpanded by remember { mutableStateOf(false) }
	var locationExpanded by remember { mutableStateOf(false) }
	
	// Get real email from Firebase Auth (actual RUPP email)
	val realEmail = FirebaseAuth.getInstance().currentUser?.email ?: state.email
	val normalizedYear = yearOptions.firstOrNull { it.equals(state.year, ignoreCase = true) }.orEmpty()
	val normalizedLocation = cambodiaLocations.firstOrNull { it.equals(state.location, ignoreCase = true) }.orEmpty()
	
	var username by remember { mutableStateOf(state.displayName) }
	var bio by remember { mutableStateOf(state.bio) }
	var email by remember { mutableStateOf(realEmail) }
	var phone by remember { mutableStateOf(state.phone) }
	var faculty by remember { mutableStateOf(state.faculty) }
	var year by remember { mutableStateOf(normalizedYear) }
	var location by remember { mutableStateOf(normalizedLocation) }
	var emailError by remember { mutableStateOf("") }
	var isSaving by remember { mutableStateOf(false) }

	val photoPickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.GetContent(),
	) { uri ->
		if (uri != null) {
			viewModel.uploadProfileImageToSupabase(uri, context)
		}
	}

	LaunchedEffect(state.displayName, state.email, state.phone, state.faculty, state.year, state.location) {
		username = state.displayName
		bio = state.bio
		email = realEmail
		phone = state.phone
		faculty = state.faculty
		year = yearOptions.firstOrNull { it.equals(state.year, ignoreCase = true) }.orEmpty()
		location = cambodiaLocations.firstOrNull { it.equals(state.location, ignoreCase = true) }.orEmpty()
	}

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
							.clickable { photoPickerLauncher.launch("image/*") },
						contentAlignment = Alignment.Center,
					) {
						if (state.profileImageUrl.isNotEmpty()) {
							AsyncImage(
								model = state.profileImageUrl,
								contentDescription = "Profile picture",
								modifier = Modifier
									.fillMaxSize()
									.clip(CircleShape),
							)
						} else {
							Icon(
								imageVector = Icons.Outlined.Person,
								contentDescription = "Profile picture",
								tint = Color.White.copy(alpha = 0.9f),
								modifier = Modifier.size(44.dp),
							)
						}
					}

					Box(
						modifier = Modifier
							.size(30.dp)
							.clip(CircleShape)
							.background(Color(0xFF0D7FFF))
							.clickable { photoPickerLauncher.launch("image/*") },
						contentAlignment = Alignment.Center,
					) {
						Icon(
							imageVector = Icons.Outlined.CameraAlt,
							contentDescription = "Change profile picture",
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
				Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
					OutlinedTextField(
						value = email,
						onValueChange = { 
							email = it
							emailError = if (!it.endsWith("@rupp.edu.kh") && it.isNotEmpty()) {
								"Only RUPP emails (@rupp.edu.kh) allowed"
							} else {
								""
							}
						},
						label = { Text("Email") },
						modifier = Modifier.fillMaxWidth(),
						isError = emailError.isNotEmpty(),
					)
					if (emailError.isNotEmpty()) {
						Text(
							text = emailError,
							color = Color(0xFFFF4D6A),
							fontSize = 12.sp,
							modifier = Modifier.padding(horizontal = 4.dp)
						)
					}
				}
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
				OutlinedTextField(
					value = faculty,
					onValueChange = { faculty = it },
					label = { Text("Faculty") },
					modifier = Modifier.fillMaxWidth(),
				)
				ExposedDropdownMenuBox(
					expanded = yearExpanded,
					onExpandedChange = { yearExpanded = !yearExpanded },
				) {
					OutlinedTextField(
						value = year,
						onValueChange = {},
						readOnly = true,
						label = { Text("Year") },
						placeholder = { Text("Select year") },
						trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
						modifier = Modifier
							.menuAnchor()
							.fillMaxWidth(),
					)
					ExposedDropdownMenu(
						expanded = yearExpanded,
						onDismissRequest = { yearExpanded = false },
					) {
						yearOptions.forEach { option ->
							DropdownMenuItem(
								text = { Text(option) },
								onClick = {
									year = option
									yearExpanded = false
								},
							)
						}
					}
				}

				ExposedDropdownMenuBox(
					expanded = locationExpanded,
					onExpandedChange = { locationExpanded = !locationExpanded },
				) {
					OutlinedTextField(
						value = location,
						onValueChange = {},
						readOnly = true,
						label = { Text("Location") },
						placeholder = { Text("Select Cambodia province") },
						trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
						modifier = Modifier
							.menuAnchor()
							.fillMaxWidth(),
					)
					ExposedDropdownMenu(
						expanded = locationExpanded,
						onDismissRequest = { locationExpanded = false },
					) {
						cambodiaLocations.forEach { option ->
							DropdownMenuItem(
								text = { Text(option) },
								onClick = {
									location = option
									locationExpanded = false
								},
							)
						}
					}
				}
			}

			Button(
				enabled = !isSaving,
				onClick = {
					// Validate RUPP email
					if (!email.endsWith("@rupp.edu.kh")) {
						emailError = "Only RUPP emails (@rupp.edu.kh) allowed"
						return@Button
					}
					isSaving = true
					
					// Get current user ID
					val currentUser = FirebaseAuth.getInstance().currentUser
					currentUser?.let { user ->
						// Create updated user object with new values
						val updatedUser = User(
							id = user.uid,
							displayName = username,
							handle = state.handle,
							bio = bio,
							email = email,
							phone = phone,
							faculty = faculty,
							year = year,
							location = location,
							avatarEmoji = state.avatarEmoji,
							profileImageUrl = state.profileImageUrl,
							coverImageUrl = state.coverImageUrl,
							isOnline = state.isOnline,
						)
						// Update profile in Firestore
						viewModel.updateProfile(updatedUser) { result ->
							isSaving = false
							if (result.isSuccess) {
								onSaved()
							}
						}
					}
					if (currentUser == null) {
						isSaving = false
					}
				},
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = Color(0xFF0D7FFF),
					contentColor = Color.White,
				),
			) {
				Text(if (isSaving) "Saving..." else "Save Changes", fontWeight = FontWeight.Medium)
			}
		}
	}
}
