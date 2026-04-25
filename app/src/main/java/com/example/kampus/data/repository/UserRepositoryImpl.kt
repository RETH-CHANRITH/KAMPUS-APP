package com.example.kampus.data.repository

import com.example.kampus.domain.model.User
import com.example.kampus.domain.model.UserStats
import com.example.kampus.domain.model.Friend
import com.example.kampus.domain.model.FriendRequest
import com.example.kampus.domain.model.FriendRequestStatus
import com.example.kampus.domain.repository.IUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : IUserRepository {

    override fun getCurrentUserProfile(): Flow<Result<User>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toUser()
                    trySend(Result.success(user))
                } else {
                    trySend(Result.failure(Exception("User profile not found")))
                }
            }
        
        awaitClose { listener.remove() }
    }

    override fun getUserProfile(userId: String): Flow<Result<User>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toUser()
                    trySend(Result.success(user))
                } else {
                    trySend(Result.failure(Exception("User not found")))
                }
            }
        
        awaitClose { listener.remove() }
    }

    override fun getUserStats(userId: String): Flow<Result<User>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toUser()
                    trySend(Result.success(user))
                }
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFriends(userId: String): Flow<Result<List<Friend>>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val friends = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFriend()
                }?.distinctBy { it.userId } ?: emptyList()
                
                trySend(Result.success(friends))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFriendsCount(userId: String): Flow<Result<Int>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val count = snapshot?.size() ?: 0
                trySend(Result.success(count))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFollowers(userId: String): Flow<Result<List<Friend>>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val followers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFriend()
                }?.distinctBy { it.userId } ?: emptyList()
                
                trySend(Result.success(followers))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFollowersCount(userId: String): Flow<Result<Int>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val count = snapshot?.size() ?: 0
                trySend(Result.success(count))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFollowing(userId: String): Flow<Result<List<Friend>>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val following = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFriend()
                }?.distinctBy { it.userId } ?: emptyList()
                
                trySend(Result.success(following))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFollowingCount(userId: String): Flow<Result<Int>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val count = snapshot?.size() ?: 0
                trySend(Result.success(count))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getFriendRequests(userId: String): Flow<Result<List<FriendRequest>>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("friendRequests")
            .whereEqualTo("status", "PENDING")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFriendRequest()
                }?.distinctBy { it.fromUserId } ?: emptyList()
                
                trySend(Result.success(requests))
            }
        
        awaitClose { listener.remove() }
    }

    override fun getOutgoingFriendRequests(userId: String): Flow<Result<List<FriendRequest>>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("outgoingFriendRequests")
            .whereEqualTo("status", "PENDING")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFriendRequest()
                }?.distinctBy { it.toUserId } ?: emptyList()

                trySend(Result.success(requests))
            }

        awaitClose { listener.remove() }
    }

    override fun getFriendRequestsCount(userId: String): Flow<Result<Int>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("friendRequests")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val count = snapshot?.size() ?: 0
                trySend(Result.success(count))
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
        if (fromUserId == toUserId) {
            Result.failure(Exception("Cannot send friend request to yourself"))
        } else {

        val existingOutgoing = firestore.collection("users").document(fromUserId)
            .collection("outgoingFriendRequests")
            .whereEqualTo("toUserId", toUserId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .get()
            .await()

        if (!existingOutgoing.isEmpty) {
            Result.success(Unit)
        } else {

        val existingIncoming = firestore.collection("users").document(fromUserId)
            .collection("friendRequests")
            .whereEqualTo("fromUserId", toUserId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .get()
            .await()

        if (!existingIncoming.isEmpty) {
            Result.success(Unit)
        } else {

        val alreadyFriends = firestore.collection("users").document(fromUserId)
            .collection("friends")
            .document(toUserId)
            .get()
            .await()

        if (alreadyFriends.exists()) {
            Result.success(Unit)
        } else {

        val requestId = firestore.collection("users").document(toUserId)
            .collection("friendRequests")
            .document().id

        val fromUser = firestore.collection("users").document(fromUserId).get().await()
        val toUser = firestore.collection("users").document(toUserId).get().await()

        val requestData = mapOf(
            "id" to requestId,
            "fromUserId" to fromUserId,
            "fromUserName" to (fromUser.getString("displayName") ?: ""),
            "fromUserHandle" to (fromUser.getString("handle") ?: ""),
            "fromUserAvatar" to (fromUser.getString("avatarEmoji") ?: "👤"),
            "fromUserProfileImageUrl" to (fromUser.getString("profileImageUrl") ?: ""),
            "toUserId" to toUserId,
            "toUserName" to (toUser.getString("displayName") ?: ""),
            "toUserHandle" to (toUser.getString("handle") ?: ""),
            "toUserAvatar" to (toUser.getString("avatarEmoji") ?: "👤"),
            "toUserProfileImageUrl" to (toUser.getString("profileImageUrl") ?: ""),
            "status" to "PENDING",
            "createdAt" to com.google.firebase.Timestamp.now(),
        )

        // Follow behavior: immediately reflect relationship in followers/following collections.
        val followerDataForTarget = mapOf(
            "userId" to fromUserId,
            "displayName" to (fromUser.getString("displayName") ?: ""),
            "handle" to (fromUser.getString("handle") ?: ""),
            "avatarEmoji" to (fromUser.getString("avatarEmoji") ?: "👤"),
            "profileImageUrl" to (fromUser.getString("profileImageUrl") ?: ""),
            "isOnline" to (fromUser.getBoolean("isOnline") ?: false),
            "isMutual" to false,
            "addedAt" to com.google.firebase.Timestamp.now(),
        )

        val followingDataForSource = mapOf(
            "userId" to toUserId,
            "displayName" to (toUser.getString("displayName") ?: ""),
            "handle" to (toUser.getString("handle") ?: ""),
            "avatarEmoji" to (toUser.getString("avatarEmoji") ?: "👤"),
            "profileImageUrl" to (toUser.getString("profileImageUrl") ?: ""),
            "isOnline" to (toUser.getBoolean("isOnline") ?: false),
            "isMutual" to false,
            "addedAt" to com.google.firebase.Timestamp.now(),
        )

        val batch = firestore.batch()
        batch.set(
            firestore.collection("users").document(toUserId)
                .collection("friendRequests").document(requestId),
            requestData,
        )
        batch.set(
            firestore.collection("users").document(fromUserId)
                .collection("outgoingFriendRequests").document(requestId),
            requestData,
        )
        batch.set(
            firestore.collection("users").document(toUserId)
                .collection("followers").document(fromUserId),
            followerDataForTarget,
        )
        batch.set(
            firestore.collection("users").document(fromUserId)
                .collection("following").document(toUserId),
            followingDataForSource,
        )
        batch.commit().await()
        
        Result.success(Unit)
        }
        }
        }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val incomingRef = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .document(requestId)

        val incomingDoc = incomingRef.get().await()
        val fromUserId = incomingDoc.getString("fromUserId") ?: throw Exception("Friend request not found")

        val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
        val fromUserDoc = firestore.collection("users").document(fromUserId).get().await()

        val currentUserFriendData = mapOf(
            "userId" to currentUserId,
            "displayName" to (currentUserDoc.getString("displayName") ?: ""),
            "handle" to (currentUserDoc.getString("handle") ?: ""),
            "avatarEmoji" to (currentUserDoc.getString("avatarEmoji") ?: "👤"),
            "profileImageUrl" to (currentUserDoc.getString("profileImageUrl") ?: ""),
            "isOnline" to (currentUserDoc.getBoolean("isOnline") ?: false),
            "isMutual" to true,
            "addedAt" to com.google.firebase.Timestamp.now(),
        )

        val fromUserFriendData = mapOf(
            "userId" to fromUserId,
            "displayName" to (fromUserDoc.getString("displayName") ?: ""),
            "handle" to (fromUserDoc.getString("handle") ?: ""),
            "avatarEmoji" to (fromUserDoc.getString("avatarEmoji") ?: "👤"),
            "profileImageUrl" to (fromUserDoc.getString("profileImageUrl") ?: ""),
            "isOnline" to (fromUserDoc.getBoolean("isOnline") ?: false),
            "isMutual" to true,
            "addedAt" to com.google.firebase.Timestamp.now(),
        )

        val batch = firestore.batch()

        // Delete all incoming requests from this user (respects ownership rules).
        val allIncomingFromSender = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        for (doc in allIncomingFromSender.documents) {
            batch.delete(doc.reference)
        }

        // Delete any local outgoing requests to this user.
        val allOutgoingToSender = firestore.collection("users").document(currentUserId)
            .collection("outgoingFriendRequests")
            .whereEqualTo("toUserId", fromUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        for (doc in allOutgoingToSender.documents) {
            batch.delete(doc.reference)
        }

        batch.set(
            firestore.collection("users").document(currentUserId)
                .collection("friends").document(fromUserId),
            fromUserFriendData,
        )
        batch.set(
            firestore.collection("users").document(fromUserId)
                .collection("friends").document(currentUserId),
            currentUserFriendData,
        )

        batch.commit().await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        
        val incomingRef = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .document(requestId)

        // Delete the incoming request (respects ownership rules).
        incomingRef.delete().await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun cancelFriendRequest(requestId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val outgoingRef = firestore.collection("users").document(currentUserId)
            .collection("outgoingFriendRequests")
            .document(requestId)

        // Delete only your own outgoing request (respects ownership rules).
        outgoingRef.delete().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeFriend(userId: String, friendId: String): Result<Unit> = try {
        firestore.collection("users").document(userId)
            .collection("friends")
            .document(friendId)
            .delete()
            .await()
        
        // Also remove from friend's list
        firestore.collection("users").document(friendId)
            .collection("friends")
            .document(userId)
            .delete()
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProfile(user: User): Result<Unit> = try {
        val userData = mapOf(
            "displayName" to user.displayName,
            "handle" to user.handle,
            "bio" to user.bio,
            "email" to user.email,
            "phone" to user.phone,
            "faculty" to user.faculty,
            "year" to user.year,
            "location" to user.location,
            "avatarEmoji" to user.avatarEmoji,
            "updatedAt" to com.google.firebase.Timestamp.now(),
        )
        
        firestore.collection("users").document(user.id)
            .update(userData)
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit> = try {
        val userData = mapOf(
            "profileImageUrl" to imageUrl,
            "updatedAt" to com.google.firebase.Timestamp.now(),
        )
        
        firestore.collection("users").document(userId)
            .update(userData)
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateCoverImage(userId: String, imageUrl: String): Result<Unit> = try {
        val userData = mapOf(
            "coverImageUrl" to imageUrl,
            "updatedAt" to com.google.firebase.Timestamp.now(),
        )
        
        firestore.collection("users").document(userId)
            .update(userData)
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun searchUsers(query: String): Flow<Result<List<User>>> = callbackFlow {
        if (query.isEmpty()) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("users")
            .orderBy("displayName")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toUser()
                } ?: emptyList()
                
                trySend(Result.success(users))
            }
        
        awaitClose { listener.remove() }
    }

    override fun observeUserOnlineStatus(userId: String): Flow<Result<Boolean>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val isOnline = snapshot?.getBoolean("isOnline") ?: false
                trySend(Result.success(isOnline))
            }
        
        awaitClose { listener.remove() }
    }

    // Helper extensions
    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User {
        // Parse nested stats map correctly
        val statsMap = this.get("stats") as? Map<String, Any>
        val stats = UserStats(
            posts = (statsMap?.get("posts") as? Number)?.toInt() ?: 0,
            followers = (statsMap?.get("followers") as? Number)?.toInt() ?: 0,
            following = (statsMap?.get("following") as? Number)?.toInt() ?: 0,
            friendRequests = (statsMap?.get("friendRequests") as? Number)?.toInt() ?: 0,
        )
        
        return User(
            id = this.id,
            displayName = this.getString("displayName") ?: "",
            handle = this.getString("handle") ?: "",
            bio = this.getString("bio") ?: "",
            email = this.getString("email") ?: "",
            phone = this.getString("phone") ?: "",
            faculty = this.getString("faculty") ?: "",
            year = this.getString("year") ?: "",
            location = this.getString("location") ?: "",
            avatarEmoji = this.getString("avatarEmoji") ?: "👤",
            profileImageUrl = this.getString("profileImageUrl") ?: "",
            coverImageUrl = this.getString("coverImageUrl") ?: "",
            stats = stats,
            isVerified = this.getBoolean("isVerified") ?: false,
            isOnline = this.getBoolean("isOnline") ?: false,
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFriend(): Friend? {
        return try {
            Friend(
                userId = this.id,
                displayName = this.getString("displayName") ?: "",
                handle = this.getString("handle") ?: "",
                avatarEmoji = this.getString("avatarEmoji") ?: "👤",
                profileImageUrl = this.getString("profileImageUrl") ?: "",
                isOnline = this.getBoolean("isOnline") ?: false,
                isMutual = this.getBoolean("isMutual") ?: false,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFriendRequest(): FriendRequest? {
        return try {
            FriendRequest(
                id = this.id,
                fromUserId = this.getString("fromUserId") ?: "",
                fromUserName = this.getString("fromUserName") ?: "",
                fromUserHandle = this.getString("fromUserHandle") ?: "",
                fromUserAvatar = this.getString("fromUserAvatar") ?: "",
                fromUserProfileImageUrl = this.getString("fromUserProfileImageUrl") ?: "",
                toUserId = this.getString("toUserId") ?: "",
                toUserName = this.getString("toUserName") ?: "",
                toUserHandle = this.getString("toUserHandle") ?: "",
                toUserAvatar = this.getString("toUserAvatar") ?: "👤",
                toUserProfileImageUrl = this.getString("toUserProfileImageUrl") ?: "",
                status = when (this.getString("status")) {
                    "ACCEPTED" -> FriendRequestStatus.ACCEPTED
                    "REJECTED" -> FriendRequestStatus.REJECTED
                    "BLOCKED" -> FriendRequestStatus.BLOCKED
                    else -> FriendRequestStatus.PENDING
                },
            )
        } catch (e: Exception) {
            null
        }
    }
}
