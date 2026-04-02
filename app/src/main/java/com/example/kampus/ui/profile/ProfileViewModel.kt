package com.example.kampus.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileUiState(
	val displayName: String = "Chanrith Reth",
	val handle: String = "@chanrith",
	val bio: String = "Computer Science student. Building KAMPUS one screen at a time.",
	val email: String = "chanrith@example.com",
	val phone: String = "+855 12 345 678",
	val faculty: String = "Faculty of Engineering",
	val year: String = "Year 3",
	val avatarEmoji: String = "🧑‍💻",
	val stats: ProfileStats = ProfileStats(),
	val settings: SettingsState = SettingsState(),
)

data class ProfileStats(
	val posts: Int = 42,
	val followers: Int = 1280,
	val following: Int = 314,
)

data class SettingsState(
	val pushNotifications: Boolean = true,
	val emailUpdates: Boolean = false,
	val privateAccount: Boolean = false,
	val darkModeLocked: Boolean = true,
)

class ProfileViewModel : ViewModel() {
	private val _uiState = MutableStateFlow(ProfileUiState())
	val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

	fun togglePushNotifications() {
		_uiState.update { state ->
			state.copy(settings = state.settings.copy(pushNotifications = !state.settings.pushNotifications))
		}
	}

	fun toggleEmailUpdates() {
		_uiState.update { state ->
			state.copy(settings = state.settings.copy(emailUpdates = !state.settings.emailUpdates))
		}
	}

	fun togglePrivateAccount() {
		_uiState.update { state ->
			state.copy(settings = state.settings.copy(privateAccount = !state.settings.privateAccount))
		}
	}
}
