package com.example.kampus.ui.feed

data class PostItem(
	val id: Long,
	val author: String,
	val avatar: String,
	val isVerified: Boolean,
	val time: String,
	val content: String,
	val likes: Int,
	val comments: Int,
	val imageUri: String? = null,
	val mediaType: MediaType = MediaType.NONE,
	val imageEmoji: String? = null,
	val visibility: String = "Public",
	val allowComments: Boolean = true,
	val taggedPeople: List<String> = emptyList(),
	val feelingEmoji: String? = null,
	val location: String? = null,
) {
	enum class MediaType {
		NONE,
		IMAGE,
		VIDEO,
	}
}
