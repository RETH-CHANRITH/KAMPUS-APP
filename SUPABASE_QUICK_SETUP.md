# 🔧 Quick Setup - Supabase Storage

Copy-paste this guide to quickly set up Supabase for image uploads.

---

## 1️⃣ Login to Supabase Dashboard

Go to: https://supabase.com/dashboard/project/wcygigxevxohizwstkfg

---

## 2️⃣ Create Two Storage Buckets

### Create "profiles" Bucket:
1. Click **Storage** in left sidebar
2. Click **Create a new bucket**
3. Name: `profiles`
4. **Check ✓** "Make it public"
5. Click **Create bucket**

### Create "covers" Bucket:
1. Click **Create a new bucket** again
2. Name: `covers`
3. **Check ✓** "Make it public"
4. Click **Create bucket**

---

## 3️⃣ Set Storage Policies

### For "profiles" Bucket:

1. Click **profiles** bucket
2. Go to **Policies** tab
3. Click **New Policy**
4. Select **For INSERT**
5. **Read the template** or write custom
6. Paste this in the policy editor:

```sql
(auth.role() = 'authenticated')
```

Click **Save**

7. Click **New Policy** again
8. Select **For SELECT**
9. Paste this (allows public read):

```sql
(true)
```

Click **Save**

### For "covers" Bucket:

Repeat the exact same steps for the "covers" bucket:
1. Click **covers** bucket
2. Add INSERT policy: `(auth.role() = 'authenticated')`
3. Add SELECT policy: `(true)`

---

## 4️⃣ Verify Setup

Check your Supabase Dashboard:
- ✅ **Storage** section visible
- ✅ **profiles** bucket exists and is public
- ✅ **covers** bucket exists and is public
- ✅ Both have INSERT and SELECT policies

---

## 5️⃣ Sync Android Project

1. Open Android Studio
2. Click **File → Sync Now**
3. Wait for Gradle to sync
4. Build project (should compile without errors)

---

## 6️⃣ Done! 🎉

Your app is now ready to:
- Upload profile images to Supabase
- Upload cover images to Supabase
- Display them in real-time
- Sync across all devices

---

## 🧪 Test It Out

1. Open the app
2. Go to Profile screen
3. Tap the **pencil icon** on cover image
4. Select an image from gallery
5. Wait for upload to complete
6. See the image update instantly!
7. Restart the app - image should persist

---

## 📞 Need Help?

If image not uploading:
- [ ] Check bucket exists in Supabase
- [ ] Check bucket is public
- [ ] Check policies are set
- [ ] Check user is authenticated (logged in)
- [ ] Check you have internet connection
- [ ] Check image file size (< 10MB)

If image not displaying:
- [ ] Check image URL is correct
- [ ] Check bucket is public
- [ ] Check SELECT policy exists
- [ ] Try opening image URL in browser

---

That's it! Your Supabase storage is ready! 🚀
