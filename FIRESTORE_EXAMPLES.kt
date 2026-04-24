// Example Firestore Data Structure and Sample Code

// ============================================
// SAMPLE USER PROFILE DOCUMENT
// ============================================

// Path: users/user123

{
  "displayName": "Chanrith Reth",
  "handle": "@chanrith",
  "bio": "Blender Art 3D Designer | Computer Science Student",
  "email": "chanrith@example.com",
  "phone": "+855 12 345 678",
  "faculty": "Faculty of Engineering",
  "year": "Year 3",
  "location": "London, UK",
  "avatarEmoji": "🧑‍💻",
  "profileImageUrl": "",
  "coverImageUrl": "",
  "isOnline": true,
  "isVerified": false,
  "stats": {
    "posts": 42,
    "followers": 1280,
    "following": 314,
    "friendRequests": 3
  },
  "createdAt": Timestamp.now(),
  "updatedAt": Timestamp.now()
}

// ============================================
// FRIENDS SUBCOLLECTION
// ============================================

// Path: users/user123/friends/friend456

{
  "displayName": "Sarah Johnson",
  "handle": "@sarah_j",
  "avatarEmoji": "👩‍💼",
  "isOnline": true,
  "isMutual": true
}

// ============================================
// FOLLOWERS SUBCOLLECTION
// ============================================

// Path: users/user123/followers/follower789

{
  "displayName": "Alex Chen",
  "handle": "@alexchen",
  "avatarEmoji": "👨‍🎨",
  "isOnline": false
}

// ============================================
// FOLLOWING SUBCOLLECTION
// ============================================

// Path: users/user123/following/following456

{
  "displayName": "Emma Wilson",
  "handle": "@emmaw",
  "avatarEmoji": "👩‍💻",
  "isOnline": true
}

// ============================================
// FRIEND REQUESTS SUBCOLLECTION
// ============================================

// Path: users/user123/friendRequests/request1

{
  "fromUserId": "user789",
  "fromUserName": "Mike Davis",
  "fromUserHandle": "@miked",
  "fromUserAvatar": "👨‍💼",
  "toUserId": "user123",
  "status": "PENDING",
  "createdAt": Timestamp.now()
}

// ============================================
// SAMPLE CODE: Initialize User on Signup
// ============================================

package com.example.kampus.ui.auth

import com.example.kampus.domain.model.User, UserStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SignupViewModel {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun signupWithEmail(
        email: String,
        password: String,
        displayName: String,
        faculty: String,
        year: String
    ): Result<Unit> = try {
        // 1. Create user in Firebase Auth
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

        // 2. Create user profile in Firestore
        val userData = mapOf(
            "displayName" to displayName,
            "handle" to "@${displayName.lowercase().replace(" ", "")}",
            "bio" to "",
            "email" to email,
            "phone" to "",
            "faculty" to faculty,
            "year" to year,
            "location" to "",
            "avatarEmoji" to "👤",
            "profileImageUrl" to "",
            "coverImageUrl" to "",
            "isOnline" to false,
            "isVerified" to false,
            "stats" to mapOf(
                "posts" to 0,
                "followers" to 0,
                "following" to 0,
                "friendRequests" to 0
            ),
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users").document(userId).set(userData).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ============================================
// SAMPLE CODE: Update User Online Status
// ============================================

package com.example.kampus.service

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PresenceManager : DefaultLifecycleObserver {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        updateOnlineStatus(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        scope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                firestore.collection("users").document(userId)
                    .update("isOnline", isOnline)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// ============================================
// SAMPLE CODE: Send Friend Request
// ============================================

suspend fun sendFriendRequest(toUserId: String): Result<Unit> = try {
    val currentUser = FirebaseAuth.getInstance().currentUser 
        ?: throw Exception("Not authenticated")
    val firestore = FirebaseFirestore.getInstance()

    // Get current user's data
    val currentUserDoc = firestore.collection("users")
        .document(currentUser.uid)
        .get()
        .await()

    val requestData = mapOf(
        "fromUserId" to currentUser.uid,
        "fromUserName" to (currentUserDoc.getString("displayName") ?: ""),
        "fromUserHandle" to (currentUserDoc.getString("handle") ?: ""),
        "fromUserAvatar" to (currentUserDoc.getString("avatarEmoji") ?: "👤"),
        "toUserId" to toUserId,
        "status" to "PENDING",
        "createdAt" to com.google.firebase.Timestamp.now()
    )

    // Add to recipient's friend requests
    firestore.collection("users").document(toUserId)
        .collection("friendRequests")
        .add(requestData)
        .await()

    // Increment friend request count
    firestore.collection("users").document(toUserId)
        .update("stats.friendRequests", com.google.firebase.firestore.FieldValue.increment(1))
        .await()

    Result.success(Unit)
} catch (e: Exception) {
    Result.failure(e)
}

// ============================================
// SAMPLE CODE: Accept Friend Request
// ============================================

suspend fun acceptFriendRequest(requestId: String, fromUserId: String): Result<Unit> = try {
    val currentUser = FirebaseAuth.getInstance().currentUser 
        ?: throw Exception("Not authenticated")
    val firestore = FirebaseFirestore.getInstance()
    val toUserId = currentUser.uid

    // Get request data
    val requestDoc = firestore.collection("users").document(toUserId)
        .collection("friendRequests").document(requestId).get().await()

    val fromUserData = mapOf(
        "displayName" to (requestDoc.getString("fromUserName") ?: ""),
        "handle" to (requestDoc.getString("fromUserHandle") ?: ""),
        "avatarEmoji" to (requestDoc.getString("fromUserAvatar") ?: "👤"),
        "isOnline" to false,
        "isMutual" to true
    )

    // Add to both friends lists
    firestore.collection("users").document(toUserId)
        .collection("friends").document(fromUserId)
        .set(fromUserData)
        .await()

    // Get to user's data for reciprocal friend addition
    val toUserDoc = firestore.collection("users").document(toUserId).get().await()
    val toUserData = mapOf(
        "displayName" to (toUserDoc.getString("displayName") ?: ""),
        "handle" to (toUserDoc.getString("handle") ?: ""),
        "avatarEmoji" to (toUserDoc.getString("avatarEmoji") ?: "👤"),
        "isOnline" to false,
        "isMutual" to true
    )

    firestore.collection("users").document(fromUserId)
        .collection("friends").document(toUserId)
        .set(toUserData)
        .await()

    // Update request status
    firestore.collection("users").document(toUserId)
        .collection("friendRequests").document(requestId)
        .update("status", "ACCEPTED")
        .await()

    // Decrement friend requests count
    firestore.collection("users").document(toUserId)
        .update("stats.friendRequests", com.google.firebase.firestore.FieldValue.increment(-1))
        .await()

    // Increment friends count for both users
    firestore.collection("users").document(toUserId)
        .update("stats.following", com.google.firebase.firestore.FieldValue.increment(1))
        .await()

    firestore.collection("users").document(fromUserId)
        .update("stats.followers", com.google.firebase.firestore.FieldValue.increment(1))
        .await()

    Result.success(Unit)
} catch (e: Exception) {
    Result.failure(e)
}

// ============================================
// FIRESTORE SECURITY RULES
// ============================================

/*
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // User profiles - only user can write their own
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
      
      // Subcollections - only user can write
      match /{document=**} {
        allow read: if request.auth != null;
        allow write: if request.auth.uid == userId;
      }
    }
  }
}
*/

// ============================================
// KEY PATTERNS IMPLEMENTED
// ============================================

/*
1. REAL-TIME SUBSCRIPTIONS
   - Use Flow<Result<T>> to subscribe to changes
   - Repository creates listeners with callbackFlow
   - ViewModel collects flows and updates state
   - UI recomposes automatically with new data

2. PRESENCE TRACKING
   - Update isOnline when app foreground/background
   - Use ProcessLifecycleOwner to detect lifecycle
   - Automatically update in Firestore

3. FRIEND CONNECTIONS
   - Accept/reject only creates documents on recipient side
   - Add reciprocal entry when accepting
   - Update stats counters incrementally

4. ERROR HANDLING
   - Result<T> type for success/failure
   - Try-catch blocks around Firebase operations
   - User-friendly error messages in UI

5. FLOW-BASED ARCHITECTURE
   - All data flows from Firestore → Repository → ViewModel → UI
   - Single source of truth in Firestore
   - No manual refreshing needed
*/
