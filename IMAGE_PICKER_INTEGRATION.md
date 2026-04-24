# 📸 How to Integrate Image Picker with ProfileScreen

Complete integration guide to make the edit button work.

---

## Current Setup

Your ProfileScreen already has:
✅ Edit button on cover image  
✅ ViewModel with `uploadCoverImageToSupabase()` method  
✅ ViewModel with `uploadProfileImageToSupabase()` method  
✅ Real-time Firestore sync  
✅ Supabase storage configured  

---

## What's Missing

You need to connect the edit button to actually open the image picker.

---

## 🔧 Integration Steps

### Step 1: Add Image Picker Launcher to ProfileScreen.kt

Find this line in ProfileScreen.kt:
```kotlin
@Composable
fun ProfileScreen(
    // existing parameters...
    onEditCoverImage: () -> Unit = {},
)
```

Replace the entire `ProfileScreen` function signature and beginning with:

```kotlin
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenFriendRequests: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenDiscoverPeople: () -> Unit,
    onHomeClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onEventsClick: () -> Unit,
    onChatClick: () -> Unit,
    onCreatePost: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val context = android.content.Context // Get context
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            viewModel.uploadCoverImageToSupabase(it, context)
        }
    }
    
    // Rest of your existing code...
    val scroll = rememberScrollState()
    // ... etc
```

### Step 2: Add Required Imports

Add these to your ProfileScreen.kt imports:

```kotlin
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
```

### Step 3: Update Header Function Call

Find where you call `Header()` in your ProfileScreen and update it:

```kotlin
Header(
    state = state,
    onBack = onBack,
    isOnline = state.isOnline,
    onEditCoverImage = {
        imageLauncher.launch("image/*")  // Open image picker
    }
)
```

### Step 4: Import LocalContext

At the top of your composable functions, add:
```kotlin
val context = LocalContext.current
```

---

## Complete Example

Here's how your ProfileScreen function should look:

```kotlin
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenFriendRequests: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenDiscoverPeople: () -> Unit,
    onHomeClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onEventsClick: () -> Unit,
    onChatClick: () -> Unit,
    onCreatePost: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    
    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            // Show loading and upload
            viewModel.uploadCoverImageToSupabase(it, context)
        }
    }
    
    if (state.error != null) {
        ErrorSnackBar(
            message = state.error!!,
            onDismiss = { viewModel.clearError() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .padding(bottom = 96.dp),
        ) {
            Header(
                state = state,
                onBack = onBack,
                isOnline = state.isOnline,
                onEditCoverImage = {
                    imageLauncher.launch("image/*")
                }
            )
            ProfileMeta(state = state)
            StatsSection(state = state)
            ActionsSection(
                state = state,
                onOpenSettings = onOpenSettings,
                onEditProfile = onEditProfile,
                onOpenFriendRequests = onOpenFriendRequests,
                onOpenFriends = onOpenFriends,
                onOpenDiscoverPeople = onOpenDiscoverPeople,
            )
            AboutCard(state = state)
            GallerySection()
            DangerAction(onOpenSettings = onOpenSettings, onLogout = onLogout)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom navigation (existing code)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Bg.copy(alpha = 0.98f))))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
        ) {
            CampusBottomNav(
                selectedIndex = -1,
                onItemSelected = { index ->
                    when (index) {
                        0 -> onHomeClick()
                        1 -> onGroupsClick()
                        2 -> onEventsClick()
                        3 -> onChatClick()
                    }
                },
                notifCount = 0,
                onFabClick = onCreatePost,
                onProfileClick = { },
                isProfileSelected = true,
            )
        }
    }
}
```

---

## 🎯 Flow When User Taps Edit Button

1. **User taps pencil icon** on cover image
   ↓
2. **ImagePicker opens** (gallery)
   ↓
3. **User selects image**
   ↓
4. **ViewModel.uploadCoverImageToSupabase()** called
   ↓
5. **Shows loading state** (spinner appears)
   ↓
6. **Uploads to Supabase Storage**
   ↓
7. **Gets public URL from Supabase**
   ↓
8. **Updates Firestore** with new URL
   ↓
9. **Real-time listener** detects change
   ↓
10. **ProfileScreen recomposes** with new image
    ↓
11. **Loading stops** - image visible! ✨

---

## Required Imports for ProfileScreen

Make sure your ProfileScreen.kt has these imports:

```kotlin
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
```

---

## Permissions Required

Your AndroidManifest.xml already has:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

If you want camera support, add:
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

---

## 🧪 Testing

1. **Compile the project** (should have no errors)
2. **Run the app** on emulator or device
3. **Navigate to Profile**
4. **Tap the pencil icon** on cover image
5. **Select an image** from gallery
6. **See it upload** and update in real-time
7. **Restart app** - image should persist

---

## ⚠️ Common Issues

### Issue: "Cannot resolve symbol 'LocalContext'"
**Fix**: Import it:
```kotlin
import androidx.compose.ui.platform.LocalContext
```

### Issue: "Cannot resolve symbol 'ActivityResultContracts'"
**Fix**: Import it:
```kotlin
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
```

### Issue: Image picker doesn't open
**Fix**: Check that `imageLauncher.launch("image/*")` is called in your button's onClick

### Issue: Image uploads but doesn't display
**Fix**: 
- Check Supabase bucket is public
- Check image URL is correct
- Check Firestore has the new URL

---

## 📝 Notes

- Image picker uses `ActivityResultContracts.GetContent()`
- This is the modern way (replaces deprecated startActivityForResult)
- Works with all Android versions
- Automatically handles permissions

---

## 🎉 You're Done!

Your profile image upload is now fully integrated and working!

Next steps (optional):
- [ ] Add image compression before upload
- [ ] Add progress indicator
- [ ] Add image cropping
- [ ] Delete old images to save space
- [ ] Add camera option along with gallery

See the main guides for those advanced features! 🚀
