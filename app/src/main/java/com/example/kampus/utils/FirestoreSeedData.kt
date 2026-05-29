package com.example.kampus.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Utility to seed Firestore with sample data for testing real-time functionality
 */
object FirestoreSeedData {

    private const val TAG = "FirestoreSeedData"
    private const val DELETE_BATCH_SIZE = 400
    private const val MAX_DELETE_RETRIES = 2
    private const val RETRY_DELAY_MS = 250L

    suspend fun clearSeededSocialDataForCurrentUser(firestore: FirebaseFirestore): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val userDoc = firestore.collection("users").document(currentUser.uid)
        var allSuccessful = true

        // Remove legacy social docs so Friends/Followers starts from real account data only.
        allSuccessful = deleteSubcollection(userDoc, "friends") && allSuccessful
        allSuccessful = deleteSubcollection(userDoc, "followers") && allSuccessful
        allSuccessful = deleteSubcollection(userDoc, "following") && allSuccessful
        allSuccessful = deleteSubcollection(userDoc, "friendRequests") && allSuccessful
        allSuccessful = deleteSubcollection(userDoc, "outgoingFriendRequests") && allSuccessful

        // Discover often contains legacy suggested people used during initial setup.
        allSuccessful = deleteNestedSubcollection(userDoc, "discover", "suggested", "people") && allSuccessful
        allSuccessful = deleteNestedSubcollection(userDoc, "discover", "new", "people") && allSuccessful
        allSuccessful = deleteNestedSubcollection(userDoc, "discover", "all", "people") && allSuccessful

        return allSuccessful
    }

    suspend fun seedAllData(firestore: FirebaseFirestore) {
        try {
            Log.i(TAG, "Starting Firestore initialization")
            
            // Seed current user profile if authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Log.i(TAG, "Authenticated user=${currentUser.uid} email=${currentUser.email}")
                seedUserProfile(firestore, currentUser.uid)
                // NOTE: Social relationship seeding is intentionally disabled.
                // New accounts should start with zero followers/following (Instagram-style).
                // seedSocialRelationshipsForCurrentUser(firestore, currentUser.uid)
            } else {
                Log.w(TAG, "No authenticated user found; skipping profile seed")
            }

            // Seed default groups globally so all users see them in Discover
            seedDefaultGroups(firestore)
            
            Log.i(TAG, "Firestore initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding data", e)
        }
    }

    /** Seeds a set of default campus groups into Firestore if none exist yet. */
    private suspend fun seedDefaultGroups(firestore: FirebaseFirestore) {
        try {
            val existing = firestore.collection("groups").limit(1).get().await()
            if (!existing.isEmpty) {
                Log.i(TAG, "Groups already exist; skipping group seed")
                return
            }

            Log.i(TAG, "Seeding default campus groups")

            val defaultGroups = listOf(
                mapOf(
                    "id" to 1001,
                    "name" to "Computer Science Hub",
                    "category" to "Technology",
                    "coverColor1" to 0xFF1E3A5F.toLong(),
                    "coverColor2" to 0xFF2D6A9F.toLong(),
                    "coverEmoji" to "💻",
                    "description" to "A place for CS students to share knowledge, projects, and opportunities.",
                    "members" to "128",
                    "posts" to "47",
                    "privacy" to "public",
                    "isJoined" to false,
                    "ownerId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                ),
                mapOf(
                    "id" to 1002,
                    "name" to "Business & Finance Club",
                    "category" to "Business",
                    "coverColor1" to 0xFF1A4731.toLong(),
                    "coverColor2" to 0xFF2E7D52.toLong(),
                    "coverEmoji" to "📊",
                    "description" to "Discuss entrepreneurship, investments, and career opportunities.",
                    "members" to "94",
                    "posts" to "31",
                    "privacy" to "public",
                    "isJoined" to false,
                    "ownerId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                ),
                mapOf(
                    "id" to 1003,
                    "name" to "Photography & Art",
                    "category" to "Creative",
                    "coverColor1" to 0xFF4A1942.toLong(),
                    "coverColor2" to 0xFF8B3A8A.toLong(),
                    "coverEmoji" to "📷",
                    "description" to "Share your photos, artwork, and creative projects with fellow artists.",
                    "members" to "76",
                    "posts" to "89",
                    "privacy" to "public",
                    "isJoined" to false,
                    "ownerId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                ),
                mapOf(
                    "id" to 1004,
                    "name" to "Study Together",
                    "category" to "Academic",
                    "coverColor1" to 0xFF1A2E4A.toLong(),
                    "coverColor2" to 0xFF3A5F8A.toLong(),
                    "coverEmoji" to "📚",
                    "description" to "Find study partners, share notes, and prepare for exams together.",
                    "members" to "212",
                    "posts" to "156",
                    "privacy" to "public",
                    "isJoined" to false,
                    "ownerId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                ),
                mapOf(
                    "id" to 1005,
                    "name" to "Sports & Fitness",
                    "category" to "Sports",
                    "coverColor1" to 0xFF4A2000.toLong(),
                    "coverColor2" to 0xFF8A4A00.toLong(),
                    "coverEmoji" to "⚽",
                    "description" to "Connect with athletes, organize matches, and share fitness tips.",
                    "members" to "63",
                    "posts" to "28",
                    "privacy" to "public",
                    "isJoined" to false,
                    "ownerId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                ),
                mapOf(
                    "id" to 1006,
                    "name" to "Engineering Society",
                    "category" to "Engineering",
                    "coverColor1" to 0xFF2A1F00.toLong(),
                    "coverColor2" to 0xFF5A4400.toLong(),
                    "coverEmoji" to "⚙️",
                    "description" to "Discuss engineering projects, internships, and technical challenges.",
                    "members" to "87",
                    "posts" to "42",
                    "privacy" to "public",
                    "isJoined" to false,
                    "ownerId" to "system",
                    "createdAt" to System.currentTimeMillis(),
                ),
            )

            val batch = firestore.batch()
            defaultGroups.forEach { group ->
                val ref = firestore.collection("groups").document()
                batch.set(ref, group)
            }
            batch.commit().await()

            Log.i(TAG, "Default groups seeded (${defaultGroups.size} groups)")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding default groups", e)
        }
    }


    private suspend fun seedSocialRelationshipsForCurrentUser(firestore: FirebaseFirestore, userId: String) {
        try {
            Log.i(TAG, "Seeding social relationships for user $userId")

            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.getBoolean("demoSocialDataSeeded") == true) {
                Log.i(TAG, "Social relationships already seeded for user $userId; skipping")
                return
            }

            val hasExistingSocialData = listOf("friends", "followers", "following", "friendRequests", "outgoingFriendRequests")
                .any { subcollection ->
                    firestore.collection("users").document(userId)
                        .collection(subcollection)
                        .limit(1)
                        .get()
                        .await()
                        .isEmpty
                        .not()
                }

            if (hasExistingSocialData) {
                Log.i(TAG, "Existing social data found for user $userId; skipping seed")
                firestore.collection("users").document(userId)
                    .set(mapOf("demoSocialDataSeeded" to true), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                return
            }
            
            // Get all other users to create sample relationships
            val allUsers = firestore.collection("users").get().await().documents
                .filter { it.id != userId }
                .take(5)  // Use first 5 other users
            
            if (allUsers.isEmpty()) {
                Log.w(TAG, "No other users found to seed relationships")
                return
            }

            // Create sample followers and following relationships
            allUsers.forEachIndexed { index, userDoc ->
                val otherUserId = userDoc.id
                val otherUserData = mapOf(
                    "userId" to otherUserId,
                    "displayName" to (userDoc.getString("displayName") ?: "User"),
                    "handle" to (userDoc.getString("handle") ?: "@user"),
                    "avatarEmoji" to (userDoc.getString("avatarEmoji") ?: "👤"),
                    "profileImageUrl" to (userDoc.getString("profileImageUrl") ?: ""),
                    "isOnline" to (userDoc.getBoolean("isOnline") ?: false),
                    "isMutual" to false,
                    "createdAt" to System.currentTimeMillis(),
                )

                val currentUserData = mapOf(
                    "userId" to userId,
                    "displayName" to "You",
                    "handle" to "@you",
                    "avatarEmoji" to "👤",
                    "profileImageUrl" to "",
                    "isOnline" to true,
                    "isMutual" to false,
                    "createdAt" to System.currentTimeMillis(),
                )

                // Add as follower
                firestore.collection("users").document(userId)
                    .collection("followers").document(otherUserId)
                    .set(otherUserData)
                    .await()

                // Add as following (alternate pattern for variety)
                if (index % 2 == 0) {
                    firestore.collection("users").document(userId)
                        .collection("following").document(otherUserId)
                        .set(otherUserData)
                        .await()
                }

                // Create sample friend request from other user
                if (index < 2) {
                    val requestId = firestore.collection("users").document(userId)
                        .collection("friendRequests").document().id

                    val requestData = mapOf(
                        "id" to requestId,
                        "fromUserId" to otherUserId,
                        "fromUserName" to (userDoc.getString("displayName") ?: "User"),
                        "fromUserHandle" to (userDoc.getString("handle") ?: "@user"),
                        "fromUserAvatar" to (userDoc.getString("avatarEmoji") ?: "👤"),
                        "fromUserProfileImageUrl" to (userDoc.getString("profileImageUrl") ?: ""),
                        "toUserId" to userId,
                        "toUserName" to "You",
                        "toUserHandle" to "@you",
                        "toUserAvatar" to "👤",
                        "toUserProfileImageUrl" to "",
                        "status" to "PENDING",
                        "createdAt" to System.currentTimeMillis(),
                    )

                    firestore.collection("users").document(userId)
                        .collection("friendRequests").document(requestId)
                        .set(requestData)
                        .await()
                }
            }

            firestore.collection("users").document(userId)
                .set(
                    mapOf(
                        "demoSocialDataSeeded" to true,
                        "demoSocialDataSeededAt" to System.currentTimeMillis(),
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
                .await()

            Log.i(TAG, "Social relationships seeded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding social relationships", e)
        }
    }

    suspend fun seedUserProfile(firestore: FirebaseFirestore, userId: String) {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            // Get actual Gmail name and email
            val email = currentUser?.email ?: "user@example.com"
            
            // Extract displayName from email if not set
            val displayName = deriveDisplayName(currentUser?.displayName, email)
            val computedHandle = createHandle(displayName)
            Log.i(TAG, "Profile setup displayName=$displayName email=$email handle=$computedHandle")

            // Check if profile already exists
            val existingProfile = firestore.collection("users").document(userId).get().await()
            
            if (existingProfile.exists()) {
                // Profile exists - update only core auth identity fields, keep user-entered profile data intact
                Log.i(TAG, "Profile exists, syncing identity fields non-destructively")
                val currentName = existingProfile.getString("displayName").orEmpty()
                val currentEmail = existingProfile.getString("email").orEmpty()
                val currentHandle = existingProfile.getString("handle").orEmpty()

                val updates = mutableMapOf<String, Any>()
                if (currentName.isBlank()) updates["displayName"] = displayName
                if (currentEmail.isBlank()) updates["email"] = email
                if (currentHandle.isBlank()) updates["handle"] = computedHandle

                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = System.currentTimeMillis()
                    firestore.collection("users").document(userId).update(updates).await()
                    Log.i(TAG, "Missing identity fields synced")
                } else {
                    Log.i(TAG, "Profile already complete, no overwrite performed")
                }
            } else {
                // Profile doesn't exist - create new one
                Log.i(TAG, "Creating new profile")
                
                val statsMap = mapOf(
                    "posts" to 0L,
                    "followers" to 0L,
                    "following" to 0L,
                    "friendRequests" to 0L,
                )

                val userProfile = mapOf(
                    "id" to userId,
                    "displayName" to displayName,  // ← From Gmail or extracted from email
                    "handle" to computedHandle,  // Auto-generate
                    "bio" to "",  // ← EMPTY - user fills in Edit Profile
                    "email" to email,  // ← From Gmail
                    "phone" to "",  // ← EMPTY - user fills in Edit Profile
                    "faculty" to "",  // ← Empty - optional
                    "year" to "",  // ← Empty - optional
                    "location" to "",  // ← Empty - optional
                    "avatarEmoji" to "🎓",
                    "profileImageUrl" to "",
                    "coverImageUrl" to "",
                    "isOnline" to true,
                    "isVerified" to false,
                    "stats" to statsMap,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                )

                firestore.collection("users")
                    .document(userId)
                    .set(userProfile)
                    .await()
                
                Log.i(TAG, "New profile created in Firestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding user profile", e)
            throw e
        }
    }

    internal fun deriveDisplayName(authDisplayName: String?, email: String): String {
        if (!authDisplayName.isNullOrBlank()) return authDisplayName

        // Extract from email: reth.chanrith.2823@rupp.edu.kh -> Reth Chanrith
        return email.substringBefore("@")
            .replace('.', ' ')
            .replace(Regex("\\d+"), "")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { c -> c.uppercaseChar() }
            }
            .ifEmpty { "User" }
    }

    internal fun createHandle(displayName: String): String {
        val normalized = displayName.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
            .replace(Regex("\\s+"), "")

        return "@${normalized.ifEmpty { "user" }}"
    }

    private suspend fun deleteSubcollection(
        userDoc: DocumentReference,
        subcollection: String,
    ): Boolean {
        try {
            val docs = userDoc.collection(subcollection).get().await().documents
            return deleteDocumentsInBatches(
                docs = docs.map { it.reference },
                context = subcollection,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Skipping cleanup for $subcollection", e)
            return false
        }
    }

    private suspend fun deleteNestedSubcollection(
        userDoc: DocumentReference,
        parentCollection: String,
        parentDoc: String,
        childCollection: String,
    ): Boolean {
        try {
            val docs = userDoc.collection(parentCollection)
                .document(parentDoc)
                .collection(childCollection)
                .get()
                .await()
                .documents

            return deleteDocumentsInBatches(
                docs = docs.map { it.reference },
                context = "$parentCollection/$parentDoc/$childCollection",
            )
        } catch (e: Exception) {
            Log.w(TAG, "Skipping cleanup for $parentCollection/$parentDoc/$childCollection", e)
            return false
        }
    }
    private suspend fun deleteDocumentsInBatches(
        docs: List<DocumentReference>,
        context: String,
    ): Boolean {
        if (docs.isEmpty()) return true

        docs.chunked(DELETE_BATCH_SIZE).forEachIndexed { index, chunk ->
            var attempts = 0
            var deleted = false

            while (!deleted && attempts <= MAX_DELETE_RETRIES) {
                attempts++

                try {
                    val batch = docs.first().firestore.batch()
                    chunk.forEach { batch.delete(it) }
                    batch.commit().await()
                    deleted = true
                } catch (e: Exception) {
                    if (attempts > MAX_DELETE_RETRIES) {
                        Log.w(
                            TAG,
                            "Failed deleting batch ${index + 1} for $context after $attempts attempts",
                            e,
                        )
                        return false
                    }

                    Log.w(
                        TAG,
                        "Retrying delete batch ${index + 1} for $context (attempt $attempts)",
                        e,
                    )
                    delay(RETRY_DELAY_MS * attempts)
                }
            }
        }

        return true
    }
}
