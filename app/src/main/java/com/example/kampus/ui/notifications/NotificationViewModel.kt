package com.example.kampus.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.domain.model.AppNotification
import com.example.kampus.utils.NotificationLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
	val notifications: List<AppNotification> = emptyList(),
	val isLoading: Boolean = true,
	val error: String? = null,
)

class NotificationViewModel : ViewModel() {

	private val _uiState = MutableStateFlow(NotificationUiState())
	val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

	private var listener: ListenerRegistration? = null

	init {
		observeNotifications()
	}

	private fun observeNotifications() {
		val userId = FirebaseAuth.getInstance().currentUser?.uid
		if (userId.isNullOrBlank()) {
			_uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
			return
		}

		listener?.remove()
		listener = NotificationLogger.observeUserNotifications(userId) { result ->
			result.onSuccess { rows ->
				val mapped = rows.map {
					AppNotification(
						id = it["id"] as? String ?: "",
						type = it["type"] as? String ?: "system",
						title = it["title"] as? String ?: "Notification",
						body = it["body"] as? String ?: "",
						toUserId = it["toUserId"] as? String ?: "",
						actorUserId = it["actorUserId"] as? String ?: "",
						actorDisplayName = it["actorDisplayName"] as? String ?: "",
						targetId = it["targetId"] as? String ?: "",
						createdAt = it["createdAt"] as? Long ?: 0L,
						isRead = it["isRead"] as? Boolean ?: false,
					)
				}
				_uiState.update { state -> state.copy(notifications = mapped, isLoading = false, error = null) }
			}
			result.onFailure { error ->
				_uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load notifications") }
			}
		}
	}

	fun markAsRead(notificationId: String) {
		val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
		viewModelScope.launch {
			runCatching {
				NotificationLogger.markNotificationRead(userId, notificationId)
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		listener?.remove()
	}
}

