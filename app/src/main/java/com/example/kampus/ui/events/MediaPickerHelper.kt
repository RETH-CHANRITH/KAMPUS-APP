@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.events

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

/**
 * Data class representing a picked media file with metadata
 */
data class PickedMedia(
    val uri: Uri,
    val type: MediaType,
    val displayName: String = when (type) {
        MediaType.PHOTO -> "Photo"
        MediaType.VIDEO -> "Video"
        MediaType.DOCUMENT -> "Document"
    },
    val mimeType: String = when (type) {
        MediaType.PHOTO -> "image/*"
        MediaType.VIDEO -> "video/*"
        MediaType.DOCUMENT -> "*/*"
    }
)

/**
 * Container for media picker launcher functions
 */
data class MediaPickerFunctions(
    val pickPhoto: () -> Unit,
    val pickVideo: () -> Unit,
)

/**
 * Composable hook for handling media picking with ActivityResultContracts
 * Returns functions to trigger photo and video pickers
 */
@Composable
fun rememberMediaPickers(
    onPhotoSelected: (Uri) -> Unit = {},
    onVideoSelected: (Uri) -> Unit = {},
): MediaPickerFunctions {
    // Photo picker - using system gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { onPhotoSelected(it) }
        }
    )

    // Video picker - using system gallery
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { onVideoSelected(it) }
        }
    )

    return remember(photoPickerLauncher, videoPickerLauncher) {
        MediaPickerFunctions(
            pickPhoto = { photoPickerLauncher.launch("image/*") },
            pickVideo = { videoPickerLauncher.launch("video/*") },
        )
    }
}

/**
 * Get filename from URI safely
 */
fun Uri.getFileName(context: Context): String {
    return try {
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex("_display_name")
            if (it.moveToFirst() && nameIndex >= 0) {
                return it.getString(nameIndex)
            }
        }
        "File"
    } catch (e: Exception) {
        "File"
    }
}

/**
 * Convert URI to preview URL (for display purposes)
 * In a real app, you'd upload to Firebase Storage and get a download URL
 */
fun Uri.toPreviewUrl(): String = this.toString()
