# 🖼️ Image Upload to Supabase - Complete Guide

## Problem Fixed ✅
1. **Slow Loading** - Removed unnecessary loading state, profile now loads instantly from real-time listeners
2. **Edit Button Not Working** - Created complete image picker and upload system
3. **Store in Supabase** - Integrated Supabase Storage for image uploads

---

## Files Created

### 1. `SupabaseStorageManager.kt`
Handles all image uploads to Supabase Storage:
- Upload profile picture
- Upload cover image  
- Delete old images
- Generate public URLs

### 2. `ImagePickerDialog.kt`
Beautiful image picker UI:
- Gallery option
- Camera option
- Dark theme matching your app

### 3. `ImagePickerUtils.kt`
Utility functions for image picking

---

## Step 1: Set Up Supabase in build.gradle.kts

Add these dependencies:

```kotlin
dependencies {
    // Supabase
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.2.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.2.2")
    implementation("io.ktor:ktor-client-android:2.3.0")
}
```

---

## Step 2: Initialize Supabase Client

Update `SupabaseModule.kt`:

```kotlin
package com.example.kampus.di

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import com.example.kampus.data.remote.SupabaseStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): io.github.jan.supabase.SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://YOUR_PROJECT.supabase.co",
            supabaseKey = "YOUR_ANON_KEY"
        ) {
            install(Storage)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseStorageManager(
        client: io.github.jan.supabase.SupabaseClient,
        context: Context
    ): SupabaseStorageManager {
        return SupabaseStorageManager(client, context)
    }
}
```

Replace with your actual Supabase credentials from your dashboard.

---

## Step 3: Set Up Supabase Storage Buckets

1. Go to Supabase Dashboard
2. Navigate to Storage
3. Create two public buckets:
   - **`profiles`** - for profile pictures
   - **`covers`** - for cover images

### Set Bucket Permissions:
```sql
-- Allow authenticated users to upload and read
create policy "Users can upload own images"
on storage.objects
for insert
with check (
  auth.role() = 'authenticated' 
  and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "Images are publicly readable"  
on storage.objects
for select
using (true);
```

---

## Step 4: Update ProfileScreen to Use Image Picker

Add this to your `ProfileScreen.kt`:

```kotlin
@Composable
fun ProfileScreen(
    // ... existing parameters ...
    onEditCoverImage: () -> Unit = {},
    context: Context = LocalContext.current,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Image picker state
    var showImagePicker by remember { mutableStateOf(false) }
    var imagePickerMode by remember { mutableStateOf<ImagePickerMode?>(null) }
    
    // Image launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            when (imagePickerMode) {
                ImagePickerMode.COVER -> viewModel.uploadCoverImageToSupabase(it, context)
                ImagePickerMode.PROFILE -> viewModel.uploadProfileImageToSupabase(it, context)
                null -> {}
            }
            showImagePicker = false
        }
    }
    
    // ... rest of your code ...
    
    Header(
        state = state,
        onBack = onBack,
        isOnline = state.isOnline,
        onEditCoverImage = {
            imagePickerMode = ImagePickerMode.COVER
            imageLauncher.launch("image/*")
        }
    )
}
```

---

## Step 5: Update ProfileViewModel

The ViewModel already has these methods ready:

```kotlin
// Upload cover image
viewModel.uploadCoverImageToSupabase(imageUri, context)

// Upload profile image  
viewModel.uploadProfileImageToSupabase(imageUri, context)

// They automatically:
// 1. Upload to Supabase Storage
// 2. Get public URL
// 3. Update Firestore with new URL
// 4. Show real-time update in UI
```

---

## Step 6: Firestore Structure Update

Your user document now includes:

```firestore
users/{userId}
├── profileImageUrl: string (from Supabase)
├── coverImageUrl: string (from Supabase)
└── ... other fields
```

---

## Complete Flow

```
User taps edit button on cover
        ↓
Image picker opens
        ↓
User selects image from gallery
        ↓
ViewModel.uploadCoverImageToSupabase(uri, context)
        ↓
SupabaseStorageManager uploads to Supabase Storage
        ↓
Gets public URL: https://project.supabase.co/storage/v1/object/public/covers/...
        ↓
Updates Firestore: coverImageUrl = <new-url>
        ↓
Real-time listener detects change
        ↓
ViewModel receives new coverImageUrl
        ↓
ProfileScreen recomposes with new image
        ↓
User sees update instantly! ✨
```

---

## Performance Improvements

### ✅ No More Slow Loading
- Removed `isLoading = true` on init
- Uses real-time listeners (faster than initial load)
- Data appears as soon as it's available

### ✅ Image Uploads to Supabase (Not Firebase)
- Lighter weight than Firebase Storage
- Better for just images
- Integrates seamlessly with Firestore

### ✅ Caching
Images are cached automatically:
- Supabase CDN caches all public images
- Your app downloads once, cached locally

---

## Testing

### Test Cover Image Upload:
1. Open profile
2. Tap pencil icon on cover
3. Select image from gallery
4. Wait for upload (should be fast!)
5. See cover image update in real-time
6. Refresh app to confirm Supabase saved it

### Test Profile Image Upload:
1. Tap on avatar
2. Select new image
3. Profile picture updates instantly
4. See it synced to Firestore

---

## Troubleshooting

### Upload Not Working?
```
✓ Check Supabase credentials are correct
✓ Verify buckets exist (profiles, covers)
✓ Check bucket permissions are set
✓ Ensure user is authenticated
✓ Check internet connection
```

### Images Not Displaying?
```
✓ Verify public bucket permissions
✓ Check image URLs in Firestore are correct
✓ Test URL in browser to see if it works
✓ Check Supabase Storage quota
```

### Slow Uploads?
```
✓ Compress images before upload
✓ Check image file size (keep under 5MB)
✓ Use image optimization library (Coil)
✓ Test with smaller images first
```

---

## Code Examples

### Upload with Progress (Optional):
```kotlin
suspend fun uploadImageWithProgress(
    userId: String, 
    imageUri: Uri,
    onProgress: (Float) -> Unit
): Result<String> {
    // Upload and track progress
    // onProgress(0.5f) = 50% done
}
```

### Delete Old Image Before Upload:
```kotlin
// In ViewModel before uploading new image
val oldImageUrl = state.coverImageUrl
if (oldImageUrl.isNotEmpty()) {
    storageManager.deleteImage(oldImageUrl)
}
// Then upload new image
```

### Compress Image Before Upload:
```kotlin
// Use these libraries in build.gradle.kts
implementation("io.coil-kt:coil-compose:2.7.0")

// Compress before sending to Supabase
val compressedUri = compressImage(context, imageUri)
```

---

## Summary

✅ **Problem**: Slow loading + edit button doesn't work
✅ **Solution**: 
   - Real-time listeners (instant loading)
   - Complete image picker + Supabase upload
   - Full integration with ProfileScreen

✅ **Files Added**:
   - SupabaseStorageManager.kt
   - ImagePickerDialog.kt
   - ImagePickerUtils.kt

✅ **What You Get**:
   - Upload to Supabase (not Firebase)
   - Fast image upload & display
   - Real-time synchronization
   - Professional image picker UI

**Ready to use! Just add Supabase credentials and create storage buckets.** 🎉
