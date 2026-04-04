package com.example.kampus.ui.feed

import android.net.Uri

data class PostItem(
    val id: Int,
    val author: String,
    val avatar: String,
    val time: String,
    val content: String,

    // Support for multiple media items (images/videos/emojis)
    val mediaUris: List<Uri> = emptyList(),           // List of image/video URIs
    val mediaTypes: List<MediaType> = emptyList(),    // List of media types (IMAGE, VIDEO)
    val mediaEmojis: List<String> = emptyList(),      // List of emoji backgrounds

    // Backward compatibility - single media (deprecated, use mediaUris/mediaTypes instead)
    @Deprecated("Use mediaUris and mediaTypes instead", level = DeprecationLevel.WARNING)
    val imageUri: Uri? = null,
    @Deprecated("Use mediaEmojis instead", level = DeprecationLevel.WARNING)
    val imageEmoji: String? = null,
    @Deprecated("Use mediaTypes instead", level = DeprecationLevel.WARNING)
    val mediaType: MediaType? = null,

    val likes: Int,
    val comments: Int,
    val isVerified: Boolean = false,
    val feeling: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val allowComments: Boolean = true,
    val taggedPeople: List<String> = emptyList(),
    val feelingEmoji: String? = null,
) {
    enum class PostVisibility { PUBLIC, FRIENDS, PRIVATE }
    enum class MediaType { IMAGE, VIDEO }

    /**
     * Get first media URI for backward compatibility
     */
    fun getFirstMediaUri(): Uri? = mediaUris.firstOrNull() ?: imageUri

    /**
     * Get first media type for backward compatibility
     */
    fun getFirstMediaType(): MediaType? = mediaTypes.firstOrNull() ?: mediaType

    /**
     * Check if post has any media
     */
    fun hasMedia(): Boolean = mediaUris.isNotEmpty() || imageUri != null

    /**
     * Get total media count
     */
    fun getMediaCount(): Int = maxOf(
        mediaUris.size + (if (imageUri != null) 1 else 0),
        mediaTypes.size,
        mediaEmojis.size
    )
}
