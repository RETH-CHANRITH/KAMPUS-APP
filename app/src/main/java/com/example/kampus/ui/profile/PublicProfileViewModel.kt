package com.example.kampus.ui.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PublicProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val bio: String = "",
    val location: String = "",
    val avatarEmoji: String = "👤",
    val profileImageUrl: String = "",
    val coverImageUrl: String = "",
    val posts: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val activities: List<ProfileActivityItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class PublicProfileViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var activityListener: ListenerRegistration? = null

    fun observeUser(userId: String) {
        userListener?.remove()
        activityListener?.remove()

        _uiState.update { it.copy(userId = userId, isLoading = true, error = null) }

        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    _uiState.update { it.copy(isLoading = false, error = "Profile not found") }
                    return@addSnapshotListener
                }

                val statsMap = snapshot.get("stats") as? Map<*, *>
                _uiState.update {
                    it.copy(
                        userId = userId,
                        displayName = snapshot.getString("displayName").orEmpty(),
                        bio = snapshot.getString("bio").orEmpty(),
                        location = snapshot.getString("location").orEmpty(),
                        avatarEmoji = snapshot.getString("avatarEmoji") ?: "👤",
                        profileImageUrl = snapshot.getString("profileImageUrl").orEmpty(),
                        coverImageUrl = snapshot.getString("coverImageUrl").orEmpty(),
                        posts = (statsMap?.get("posts") as? Number)?.toInt() ?: 0,
                        followers = (statsMap?.get("followers") as? Number)?.toInt() ?: 0,
                        following = (statsMap?.get("following") as? Number)?.toInt() ?: 0,
                        isLoading = false,
                        error = null,
                    )
                }
            }

        activityListener = firestore.collection("users").document(userId)
            .collection("activities")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error.message) }
                    return@addSnapshotListener
                }

                val activities = snapshot?.documents?.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: (doc.getLong("createdAt") ?: 0L)

                    ProfileActivityItem(
                        type = doc.getString("type") ?: "activity",
                        text = doc.getString("text") ?: "Did an activity",
                        createdAt = createdAt,
                        eventId = doc.getLong("eventId")?.toInt(),
                        postId = doc.getLong("postId")?.toInt(),
                    )
                } ?: emptyList()

                _uiState.update { it.copy(activities = activities) }
            }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        activityListener?.remove()
    }
}
