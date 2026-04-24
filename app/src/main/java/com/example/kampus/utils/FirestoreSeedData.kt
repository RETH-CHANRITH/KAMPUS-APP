package com.example.kampus.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Utility to seed Firestore with sample data for testing real-time functionality
 */
object FirestoreSeedData {

    suspend fun clearSeededSocialDataForCurrentUser(firestore: FirebaseFirestore): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val userDoc = firestore.collection("users").document(currentUser.uid)

        // Remove legacy social docs so Friends/Followers starts from real account data only.
        deleteSubcollection(userDoc, "friends")
        deleteSubcollection(userDoc, "followers")
        deleteSubcollection(userDoc, "following")
        deleteSubcollection(userDoc, "friendRequests")
        deleteSubcollection(userDoc, "outgoingFriendRequests")

        // Discover often contains legacy suggested people used during initial setup.
        deleteNestedSubcollection(userDoc, "discover", "suggested", "people")
        deleteNestedSubcollection(userDoc, "discover", "new", "people")
        deleteNestedSubcollection(userDoc, "discover", "all", "people")

        return true
    }

    suspend fun seedAllData(firestore: FirebaseFirestore) {
        try {
            println("\n═══════════════════════════════════════════════════════════════")
            println("🚀 KAMPUS FIRESTORE INITIALIZATION")
            println("═══════════════════════════════════════════════════════════════")
            
            seedChatData(firestore)
            
            // Seed current user profile if authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                println("✅ Authenticated: ${currentUser.email}")
                println("👤 User ID: ${currentUser.uid}")
                println("📛 Display Name: ${currentUser.displayName}")
                seedUserProfile(firestore, currentUser.uid)
            } else {
                println("⚠️  No authenticated user found")
            }
            
            println("\n✅ Firestore initialization complete!")
            println("═══════════════════════════════════════════════════════════════\n")
        } catch (e: Exception) {
            println("❌ Error seeding data: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun seedUserProfile(firestore: FirebaseFirestore, userId: String) {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            // Get actual Gmail name and email
            val email = currentUser?.email ?: "user@example.com"
            
            // Extract displayName from email if not set
            val displayName = if (!currentUser?.displayName.isNullOrEmpty()) {
                currentUser?.displayName ?: "User"
            } else {
                // Extract from email: reth.chanrith.2823@rupp.edu.kh → Reth Chanrith
                email.substringBefore("@")
                    .replace(".", " ")
                    .replace(Regex("\\d+"), "")  // Remove numbers
                    .trim()
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    .ifEmpty { "User" }
            }
            
            println("\n📦 PROFILE SETUP")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("👤 Display Name: $displayName")
            println("📧 Email: $email")
            println("🔗 Handle: @${displayName.lowercase().replace(" ", "")}")

            // Check if profile already exists
            val existingProfile = firestore.collection("users").document(userId).get().await()
            
            if (existingProfile.exists()) {
                // Profile exists - update only core auth identity fields, keep user-entered profile data intact
                println("📝 Profile exists - syncing auth identity fields (non-destructive)")
                val currentName = existingProfile.getString("displayName").orEmpty()
                val currentEmail = existingProfile.getString("email").orEmpty()
                val currentHandle = existingProfile.getString("handle").orEmpty()
                val computedHandle = "@${displayName.lowercase().replace(" ", "")}"

                val updates = mutableMapOf<String, Any>()
                if (currentName.isBlank()) updates["displayName"] = displayName
                if (currentEmail.isBlank()) updates["email"] = email
                if (currentHandle.isBlank()) updates["handle"] = computedHandle

                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = System.currentTimeMillis()
                    firestore.collection("users").document(userId).update(updates).await()
                    println("✅ Missing identity fields synced")
                } else {
                    println("✅ Profile already complete - no overwrite performed")
                }
            } else {
                // Profile doesn't exist - create new one
                println("🆕 Creating new profile...")
                
                val statsMap = mapOf(
                    "posts" to 0L,
                    "followers" to 0L,
                    "following" to 0L,
                    "friendRequests" to 0L,
                )
                
                println("\n📊 INITIAL STATS")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("📝 Posts: 0")
                println("👥 Followers: 0")
                println("🔗 Following: 0")
                println("🤝 Friend Requests: 0")

                val userProfile = mapOf(
                    "id" to userId,
                    "displayName" to displayName,  // ← From Gmail or extracted from email
                    "handle" to "@${displayName.lowercase().replace(" ", "")}",  // Auto-generate
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
                
                println("\n✅ New profile created in Firestore")
            }
        } catch (e: Exception) {
            println("❌ Error seeding user profile: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun seedChatData(firestore: FirebaseFirestore) {
        try {
            // Sample chats
            val chats = listOf(
                mapOf(
                    "id" to "chat_1",
                    "name" to "Joanna Evan",
                    "lastMessage" to "See you on the next meeting! 😊",
                    "lastMessageTime" to System.currentTimeMillis() - 2040000,
                    "timestamp" to "34 min",
                    "unreadCount" to 3,
                    "isOnline" to true,
                    "avatarInitials" to "JE",
                    "avatarColor" to 0xFF8B5CF6
                ),
                mapOf(
                    "id" to "chat_2",
                    "name" to "Lana Smith",
                    "lastMessage" to "I'm doing my homework, but need to take...",
                    "lastMessageTime" to System.currentTimeMillis() - 3600000,
                    "timestamp" to "1h",
                    "unreadCount" to 0,
                    "isOnline" to false,
                    "avatarInitials" to "LS",
                    "avatarColor" to 0xFFEC4899
                ),
                mapOf(
                    "id" to "chat_3",
                    "name" to "Marina Martinez",
                    "lastMessage" to "I'm watching Friends, what are u doin? 😊",
                    "lastMessageTime" to System.currentTimeMillis() - 3600000,
                    "timestamp" to "1 hour",
                    "unreadCount" to 1,
                    "isOnline" to true,
                    "avatarInitials" to "MM",
                    "avatarColor" to 0xFF10B981
                ),
                mapOf(
                    "id" to "chat_4",
                    "name" to "Alex Johnson",
                    "lastMessage" to "See you on the next meeting! 😊",
                    "lastMessageTime" to System.currentTimeMillis() - 7200000,
                    "timestamp" to "2 hour",
                    "unreadCount" to 12,
                    "isOnline" to false,
                    "avatarInitials" to "AJ",
                    "avatarColor" to 0xFFF59E0B
                ),
            )

            // Add chats to Firestore
            for (chat in chats) {
                firestore.collection("chats")
                    .document(chat["id"].toString())
                    .set(chat)
                    .await()
            }

            // Sample messages for chat_1
            val messages1 = listOf(
                mapOf("id" to "msg_1", "senderId" to "user_joanna", "text" to "Hi! How are you doing? 😊", "timestamp" to System.currentTimeMillis() - 5400000, "isRead" to true),
                mapOf("id" to "msg_2", "senderId" to "current_user", "text" to "I'm doing great, thanks for asking!", "timestamp" to System.currentTimeMillis() - 5100000, "isRead" to true),
                mapOf("id" to "msg_3", "senderId" to "user_joanna", "text" to "Are you coming to the meeting tomorrow?", "timestamp" to System.currentTimeMillis() - 4800000, "isRead" to true),
                mapOf("id" to "msg_4", "senderId" to "current_user", "text" to "Yes of course! See you on the next meeting! 😊", "timestamp" to System.currentTimeMillis() - 4500000, "isRead" to true),
            )

            for (msg in messages1) {
                firestore.collection("chats").document("chat_1").collection("messages").document(msg["id"].toString()).set(msg).await()
            }

            // Sample messages for chat_3
            val messages3 = listOf(
                mapOf("id" to "msg_1", "senderId" to "user_marina", "text" to "Hey! What's up?", "timestamp" to System.currentTimeMillis() - 3600000, "isRead" to true),
                mapOf("id" to "msg_2", "senderId" to "user_marina", "text" to "I'm watching Friends, what are u doin? 😊", "timestamp" to System.currentTimeMillis() - 3300000, "isRead" to true),
                mapOf("id" to "msg_3", "senderId" to "current_user", "text" to "Nothing much, just relaxing at home", "timestamp" to System.currentTimeMillis() - 3000000, "isRead" to true),
            )

            for (msg in messages3) {
                firestore.collection("chats").document("chat_3").collection("messages").document(msg["id"].toString()).set(msg).await()
            }

            println("✅ Chat data seeded successfully!")
        } catch (e: Exception) {
            println("❌ Error seeding chat data: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun deleteSubcollection(
        userDoc: com.google.firebase.firestore.DocumentReference,
        subcollection: String,
    ) {
        try {
            val docs = userDoc.collection(subcollection).get().await().documents
            for (doc in docs) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            println("⚠️ Skipping cleanup for $subcollection: ${e.message}")
        }
    }

    private suspend fun deleteNestedSubcollection(
        userDoc: com.google.firebase.firestore.DocumentReference,
        parentCollection: String,
        parentDoc: String,
        childCollection: String,
    ) {
        try {
            val docs = userDoc.collection(parentCollection)
                .document(parentDoc)
                .collection(childCollection)
                .get()
                .await()
                .documents

            for (doc in docs) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            println("⚠️ Skipping cleanup for $parentCollection/$parentDoc/$childCollection: ${e.message}")
        }
    }
}
