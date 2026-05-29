@file:Suppress("DEPRECATION")

package com.example.kampus.ui.feed

import android.net.Uri

data class PostItem(
    val id: Int,
    val author: String,
    val authorId: String = "",
    val avatar: String,
    val profileImageUrl: String = "",
    val time: String,
    val content: String,
    val timestamp: Long = 0L,

    // Support for multiple media items (images/videos/emojis)
    val mediaUris: List<Uri> = emptyList(),           // List of image/video URIs
    val mediaTypes: List<MediaType> = emptyList(),    // List of media types (IMAGE, VIDEO)
    val mediaEmojis: List<String> = emptyList(),      // List of emoji backgrounds

    // Backward compatibility - single media (deprecated, use mediaUris/mediaTypes instead)
    val imageUri: Uri? = null,
    val imageEmoji: String? = null,
    val mediaType: MediaType? = null,

    val likes: Int,
    val comments: Int,
    val shares: Int = 0,
    val isVerified: Boolean = false,
    val feeling: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val likedBy: List<String> = emptyList(),
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val allowComments: Boolean = true,
    val taggedPeople: List<String> = emptyList(),
    val feelingEmoji: String? = null,
    val isPinned: Boolean = false,
    val sharedOriginalPostId: Int? = null,
    val sharedOriginalAuthor: String? = null,
    val sharedOriginalAuthorId: String? = null,
    val sharedOriginalAvatar: String? = null,
    val sharedOriginalProfileImageUrl: String? = null,
    val sharedOriginalTime: String? = null,
    val sharedOriginalTimestamp: Long? = null,
    val sharedOriginalContent: String? = null,
    val sharedOriginalMediaUris: List<Uri> = emptyList(),
    val sharedOriginalMediaTypes: List<MediaType> = emptyList(),
    val sharedOriginalMediaEmojis: List<String> = emptyList(),
    val sharedOriginalLikes: Int? = null,
    val sharedOriginalComments: Int? = null,
    val sharedOriginalShares: Int? = null,
    val sharedOriginalVisibility: PostVisibility? = null,
    val sharedOriginalIsVerified: Boolean? = null,
) {
    enum class PostVisibility { PUBLIC, FRIENDS, FOLLOWERS, UNIVERSITY, PRIVATE }
    enum class MediaType { IMAGE, VIDEO }

    /**
     * Get first media URI for backward compatibility
     */
    fun getFirstMediaUri(): Uri? = mediaUris.firstOrNull()

    /**
     * Get first media type for backward compatibility
     */
    fun getFirstMediaType(): MediaType? = mediaTypes.firstOrNull()

    /**
     * Check if post has any media
     */
    fun hasMedia(): Boolean = mediaUris.isNotEmpty() || mediaTypes.isNotEmpty() || mediaEmojis.isNotEmpty()

    /**
     * Get total media count
     */
    fun getMediaCount(): Int = maxOf(
        mediaUris.size,
        mediaTypes.size,
        mediaEmojis.size
    )
}
