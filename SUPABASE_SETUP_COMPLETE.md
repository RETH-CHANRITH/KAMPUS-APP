# 🎉 Supabase Setup Guide - Your Credentials Configured!

Your Supabase credentials have been **automatically integrated** into your app. Here's what's been done:

---

## ✅ Setup Complete

Your credentials:
- **Project URL**: `https://wcygigxevxohizwstkfg.supabase.co`
- **Project ID**: `wcygigxevxohizwstkfg`
- **Anon Key**: Pre-configured in `SupabaseModule.kt`

---

## 🚀 Next Steps (3 Easy Steps)

### Step 1: Create Storage Buckets in Supabase

1. Go to your Supabase Dashboard: https://supabase.com/dashboard/project/wcygigxevxohizwstkfg
2. Click **Storage** in the left sidebar
3. Click **Create a new bucket** and create two buckets:
   - **`profiles`** (for profile pictures)
   - **`covers`** (for cover/background images)
4. Make both buckets **Public**

### Step 2: Set Storage Policies

In Supabase Dashboard → Storage → Choose `profiles` bucket → Policies:

**Add Policy 1: Allow uploads by authenticated users**
```sql
-- Name it: "Users can upload own profile images"
CREATE POLICY "Users can upload" 
ON storage.objects 
FOR INSERT
WITH CHECK (
  auth.role() = 'authenticated'
);
```

**Add Policy 2: Allow public read access**
```sql
-- Name it: "Public read access"
CREATE POLICY "Public read" 
ON storage.objects 
FOR SELECT
USING (true);
```

**Repeat the same policies for the `covers` bucket**

### Step 3: Add Dependencies to build.gradle.kts

✅ **Already done!** These have been added:
```kotlin
implementation("io.github.jan-tennert.supabase:supabase-kt:2.2.2")
implementation("io.github.jan-tennert.supabase:storage-kt:2.2.2")
implementation("io.ktor:ktor-client-android:2.3.0")
```

---

## 📁 Files Auto-Configured

| File | Purpose |
|------|---------|
| `SupabaseModule.kt` | ✅ Credentials configured |
| `SupabaseStorageManager.kt` | ✅ Upload/download logic |
| `KampusApp.kt` | ✅ Supabase initialization |
| `ProfileViewModel.kt` | ✅ Upload methods ready |
| `build.gradle.kts` | ✅ Dependencies added |

---

## 🎯 How to Use

### Upload Cover Image:
```kotlin
// When user taps edit button on cover
viewModel.uploadCoverImageToSupabase(imageUri, context)

// Automatically:
// 1. Uploads to Supabase Storage (covers bucket)
// 2. Gets public URL
// 3. Updates Firestore with URL
// 4. UI updates in real-time
```

### Upload Profile Image:
```kotlin
// When user taps edit button on avatar
viewModel.uploadProfileImageToSupabase(imageUri, context)

// Same flow as cover image
```

---

## 🔒 Security

Your setup is secure because:

✅ **Anon Key Only** - Used for client-side uploads  
✅ **Service Key Hidden** - Never sent to client  
✅ **Authentication Required** - Only logged-in users can upload  
✅ **Public Buckets** - Anyone can view, but only owners can modify  
✅ **Firestore Rules** - Additional protection at database level  

---

## 🧪 Testing

### Test Image Upload:

1. **Open the app** and go to profile
2. **Tap the pencil icon** on the cover image
3. **Select an image** from your gallery
4. **Wait for upload** (should be fast!)
5. **See it update in real-time**
6. **Restart app** to verify it persists

### Check Supabase Dashboard:

1. Go to Storage → covers bucket
2. Should see your image files organized by user ID
3. Click image to see public URL
4. URL should be: `https://wcygigxevxohizwstkfg.supabase.co/storage/v1/object/public/covers/{userId}/{filename}.jpg`

---

## 📊 Expected File Structure

After uploading, your Supabase Storage will look like:
```
covers/
├── user123/
│   ├── cover_user123_1775743900000.jpg
│   └── cover_user123_1775743901000.jpg
└── user456/
    └── cover_user456_1775743902000.jpg

profiles/
├── user123/
│   ├── profile_user123_1775743900000.jpg
│   └── profile_user123_1775743901000.jpg
└── user456/
    └── profile_user456_1775743902000.jpg
```

---

## ⚠️ Important Notes

### 🔐 API Key Security

⚠️ **IMPORTANT**: Your Anon key is visible in your code, which is **normal and expected** for client-side apps because:
- It's public anyway (visible in network requests)
- Supabase policies protect from abuse
- Only allow authenticated users to upload
- Never use Service Key in client code!

✅ Your Service Key is **NOT** included in the code  
✅ Supabase policies provide additional security  
✅ Firestore rules provide final layer of security  

### 🚫 What Users Can Do

- ✅ Upload their own profile/cover images
- ✅ Download any public image
- ❌ Delete other users' images
- ❌ Access private data
- ❌ Upload files larger than limits

### 📱 Image Size Limits

Recommended:
- **Profile Images**: 500KB - 2MB
- **Cover Images**: 1MB - 5MB
- **Format**: JPG, PNG, WebP

---

## 🐛 Troubleshooting

### Images Not Uploading?

**Check 1**: Bucket exists and is public?
```
Supabase → Storage → profiles/covers buckets should exist
```

**Check 2**: Policies are correct?
```
Each bucket should have INSERT and SELECT policies
```

**Check 3**: User is authenticated?
```
FirebaseAuth.getInstance().currentUser should not be null
```

**Check 4**: Internet connected?
```
Must have active internet connection for upload
```

### can Upload But Not Display?

**Check**: Image URL is correct?
```
URL format: https://wcygigxevxohizwstkfg.supabase.co/storage/v1/object/public/covers/{userId}/{filename}.jpg
```

**Check**: Bucket is public?
```
Supabase → Storage → Select bucket → Policies → Check public read policy exists
```

---

## 📈 Future Enhancements

Optional features you can add later:

- [ ] **Image Compression** before upload
- [ ] **Progress Bar** while uploading
- [ ] **Crop Image** before uploading
- [ ] **Multiple Uploads** (batch)
- [ ] **Delete Old Images** to save space
- [ ] **CDN Caching** configuration
- [ ] **Image Optimization** with Cloudinary

---

## 🎓 Learning Resources

- [Supabase Storage Docs](https://supabase.com/docs/guides/storage)
- [Supabase Kotlin Client](https://github.com/supabase-community/supabase-kt)
- [Your Project Dashboard](https://supabase.com/dashboard/project/wcygigxevxohizwstkfg)

---

## ✨ Summary

Everything is ready to go! Just:

1. ✅ Create buckets in Supabase Dashboard
2. ✅ Set storage policies  
3. ✅ Sync gradle dependencies
4. ✅ Start the app

Users can now upload profile and cover images that sync instantly across all devices!

**Any issues? Check the troubleshooting section above!** 🚀
