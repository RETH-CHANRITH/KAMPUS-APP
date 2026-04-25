package com.example.kampus.ui.post

import androidx.lifecycle.ViewModel
import com.example.kampus.ui.feed.PostItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PostDetailUiState(
	val post: PostItem? = null,
	val isLoading: Boolean = true,
	val error: String? = null,
)

class PostViewModel : ViewModel() {

	private val firestore = FirebaseFirestore.getInstance()
	private val _uiState = MutableStateFlow(PostDetailUiState())
	val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

	private var listener: ListenerRegistration? = null

	fun observePost(postId: Int) {
		listener?.remove()
		_uiState.update { it.copy(isLoading = true, error = null) }

		listener = firestore.collection("posts")
			.whereEqualTo("id", postId)
			.limit(1)
			.addSnapshotListener { snapshot, error ->
				if (error != null) {
					_uiState.update { it.copy(isLoading = false, error = error.message) }
					return@addSnapshotListener
				}

				val doc = snapshot?.documents?.firstOrNull()
				if (doc == null) {
					_uiState.update { it.copy(isLoading = false, post = null) }
					return@addSnapshotListener
				}

				val post = PostItem(
					id = (doc.getLong("id")?.toInt()) ?: doc.id.hashCode(),
					author = doc.getString("author") ?: "Unknown",
					avatar = doc.getString("avatar") ?: "👤",
					time = doc.getString("time") ?: "now",
					content = doc.getString("content") ?: "",
					mediaUris = emptyList(),
					mediaTypes = emptyList(),
					mediaEmojis = emptyList(),
					likes = (doc.getLong("likes") ?: 0L).toInt(),
					comments = (doc.getLong("comments") ?: 0L).toInt(),
				)

				_uiState.update { it.copy(post = post, isLoading = false, error = null) }
			}
	}

	override fun onCleared() {
		super.onCleared()
		listener?.remove()
	}
}

