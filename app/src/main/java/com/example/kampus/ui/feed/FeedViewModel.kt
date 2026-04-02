package com.example.kampus.ui.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FeedViewModel : ViewModel() {

	private val _posts = MutableStateFlow(
		listOf(
			PostItem(
				id = 1L,
				author = "Chandrith",
				avatar = "🎓",
				isVerified = true,
				time = "Just now",
				content = "Welcome to KAMPUS!",
				likes = 12,
				comments = 4,
			),
			PostItem(
				id = 2L,
				author = "Campus Club",
				avatar = "📚",
				isVerified = false,
				time = "2h ago",
				content = "New event this Friday. Everyone is invited!",
				likes = 22,
				comments = 7,
				imageEmoji = "🎉",
			),
		)
	)
	val posts: StateFlow<List<PostItem>> = _posts.asStateFlow()

	private val _likedIds = MutableStateFlow<Set<Long>>(emptySet())
	val likedIds: StateFlow<Set<Long>> = _likedIds.asStateFlow()

	fun toggleLike(postId: Long) {
		_likedIds.update { liked ->
			if (postId in liked) liked - postId else liked + postId
		}
	}

	fun addPost(
		text: String,
		imageUri: String?,
		mediaType: PostItem.MediaType,
		feeling: String? = null,
		location: String?,
		visibility: String,
		allowComments: Boolean,
		tags: List<String> = emptyList(),
		taggedPeople: List<String>,
		feelingEmoji: String?,
	) {
		val trimmed = text.trim()
		if (trimmed.isEmpty() && imageUri == null && feelingEmoji == null) return

		val nextId = ((_posts.value.maxOfOrNull { it.id } ?: 0L) + 1L)
		val newPost = PostItem(
			id = nextId,
			author = "You",
			avatar = "🧑‍💻",
			isVerified = false,
			time = "Just now",
			content = trimmed,
			likes = 0,
			comments = 0,
			imageUri = imageUri,
			mediaType = mediaType,
			imageEmoji = feelingEmoji,
			visibility = visibility,
			allowComments = allowComments,
			taggedPeople = taggedPeople + tags,
			feelingEmoji = feelingEmoji,
			location = location,
		)

		_posts.update { current -> listOf(newPost) + current }
	}
}
