package com.example.kampus.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

/**
 * Image picker dialog for uploading profile or cover images to Supabase
 * Usage:
 * 
 * val imageLauncher = rememberImagePickerDialog(
 *     context = context,
 *     onImageSelected = { uri ->
 *         // Upload to Supabase
 *         viewModel.uploadCoverImageToSupabase(uri, context)
 *     }
 * )
 * 
 * // Then call:
 * imageLauncher.launch(null)
 */
@Composable
fun ImagePickerDialog(
    onImageSelected: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            onImageSelected(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Text(
            "Select Image Source",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Gallery option - MAIN CLICKABLE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1a1f3a))
                .clickable(enabled = true) { 
                    imageLauncher.launch("image/*")
                }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = "Gallery",
                    tint = Color(0xFF0D7FFF),
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Choose from Gallery",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Select from your device photos",
                        color = Color(0xFF99A1AF),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Camera option (Coming Soon)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1a1f3a).copy(alpha = 0.5f))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Take Photo",
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Coming soon",
                        color = Color(0xFF4B5563),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * How to use in your ProfileScreen:
 * 
 * 1. Add state for image picker
 * var showImagePicker by remember { mutableStateOf(false) }
 * var imagePickerMode by remember { mutableStateOf<ImagePickerMode?>(null) }
 * 
 * 2. In your header, when edit button is clicked:
 * onEditCoverImage = {
 *     showImagePicker = true
 *     imagePickerMode = ImagePickerMode.COVER
 * }
 * 
 * 3. Add image picker dialog:
 * if (showImagePicker && imagePickerMode != null) {
 *     ImagePickerDialog(
 *         onImageSelected = { uri ->
 *             when (imagePickerMode) {
 *                 ImagePickerMode.COVER -> 
 *                     viewModel.uploadCoverImageToSupabase(uri, context)
 *                 ImagePickerMode.PROFILE -> 
 *                     viewModel.uploadProfileImageToSupabase(uri, context)
 *                 null -> {}
 *             }
 *             showImagePicker = false
 *         },
 *         onDismiss = { showImagePicker = false }
 *     )
 * }
 */
enum class ImagePickerMode {
    PROFILE, COVER
}
