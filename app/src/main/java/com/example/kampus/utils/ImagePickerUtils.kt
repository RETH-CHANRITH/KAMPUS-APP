package com.example.kampus.utils

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Composable utility to pick image from device
 * @param onImageSelected Callback when image is selected with Uri
 */
@Composable
fun rememberImagePickerLauncher(onImageSelected: (android.net.Uri) -> Unit) = 
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }

/**
 * Open image picker intent
 */
fun pickImageFromGallery(context: Context): Intent {
    return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        .apply {
            type = "image/*"
        }
}

/**
 * Capture image from camera
 */
fun captureImageFromCamera(context: Context): Intent {
    return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
}
