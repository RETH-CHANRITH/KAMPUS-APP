package com.example.kampus.data.repository

import com.example.kampus.domain.model.User
import com.example.kampus.domain.model.UserStats
import com.example.kampus.domain.model.Friend
import com.example.kampus.domain.model.FriendRequest
import com.example.kampus.domain.model.FriendRequestStatus
import com.example.kampus.domain.repository.IUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : IUserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }

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
        var latestFollowerDocs: List<com.google.firebase.firestore.DocumentSnapshot>? = null
        var latestFollowingDocs: List<com.google.firebase.firestore.DocumentSnapshot>? = null

        fun emitMutualFriends() {
            val followerDocs = latestFollowerDocs ?: return
            val followingDocs = latestFollowingDocs ?: return

            val followerIds = followerDocs.map { doc -> doc.getString("userId") ?: doc.id }.toSet()
            val followingIds = followingDocs.map { doc -> doc.getString("userId") ?: doc.id }.toSet()
            val mutualIds = followerIds intersect followingIds

            val friends = mutualIds.mapNotNull { mutualId ->
                val source = followerDocs.firstOrNull { (it.getString("userId") ?: it.id) == mutualId }
                    ?: followingDocs.firstOrNull { (it.getString("userId") ?: it.id) == mutualId }
                source?.toFriend()?.copy(isMutual = true)
            }.distinctBy { it.userId }

            trySend(Result.success(friends))
        }

        val followersListener = firestore.collection("users").document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                latestFollowerDocs = snapshot?.documents ?: emptyList()
                emitMutualFriends()
            }

        val followingListener = firestore.collection("users").document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                latestFollowingDocs = snapshot?.documents ?: emptyList()
                emitMutualFriends()
            }
        
        awaitClose {
            followersListener.remove()
            followingListener.remove()
        }
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
        var latestOutgoingDocs: List<com.google.firebase.firestore.DocumentSnapshot>? = null
        var latestIncomingDocs: List<com.google.firebase.firestore.DocumentSnapshot>? = null
        var latestFollowerDocs: List<com.google.firebase.firestore.DocumentSnapshot>? = null

        fun emitCombined() {
            val outgoingDocs = latestOutgoingDocs ?: return
            val incomingDocs = latestIncomingDocs ?: return
            val followerDocs = latestFollowerDocs ?: return

            val outgoingRequests = outgoingDocs.mapNotNull { it.toFriendRequest() }
            val mirroredIncoming = incomingDocs.mapNotNull { it.toOutgoingMirrorFriendRequest() }
            val mirroredFollowers = followerDocs.mapNotNull { it.toOutgoingFromFollowerFriendRequest(userId) }

            val requests = (outgoingRequests + mirroredIncoming + mirroredFollowers)
                .distinctBy { it.toUserId }

            trySend(Result.success(requests))
        }

        val outgoingListener = firestore.collection("users").document(userId)
            .collection("outgoingFriendRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                latestOutgoingDocs = snapshot?.documents ?: emptyList()
                emitCombined()
            }

        // Mirror incoming requests into outgoing view so both sides can see relationship history
        // without requiring cross-user or collection-group permissions.
        val incomingListener = firestore.collection("users").document(userId)
            .collection("friendRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                latestIncomingDocs = snapshot?.documents ?: emptyList()
                emitCombined()
            }

        // Fallback for accounts that already have follower relationships but no request docs.
        val followersListener = firestore.collection("users").document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                latestFollowerDocs = snapshot?.documents ?: emptyList()
                emitCombined()
            }

        awaitClose {
            outgoingListener.remove()
            incomingListener.remove()
            followersListener.remove()
        }
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
                            "createdAt" to System.currentTimeMillis(),
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
                        batch.commit().await()

                        Result.success(Unit)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun followUser(fromUserId: String, toUserId: String): Result<Unit> {
        try {
            if (fromUserId == toUserId) return Result.failure(Exception("Cannot follow yourself"))

        val fromUser = firestore.collection("users").document(fromUserId).get().await()
        val toUser = firestore.collection("users").document(toUserId).get().await()

        val followerData = mapOf(
            "userId" to fromUserId,
            "displayName" to (fromUser.getString("displayName") ?: ""),
            "handle" to (fromUser.getString("handle") ?: ""),
            "avatarEmoji" to (fromUser.getString("avatarEmoji") ?: "👤"),
            "profileImageUrl" to (fromUser.getString("profileImageUrl") ?: ""),
            "isOnline" to false,
            "isMutual" to false,
            "createdAt" to System.currentTimeMillis(),
        )

        val followingData = mapOf(
            "userId" to toUserId,
            "displayName" to (toUser.getString("displayName") ?: ""),
            "handle" to (toUser.getString("handle") ?: ""),
            "avatarEmoji" to (toUser.getString("avatarEmoji") ?: "👤"),
            "profileImageUrl" to (toUser.getString("profileImageUrl") ?: ""),
            "isOnline" to false,
            "isMutual" to false,
            "createdAt" to System.currentTimeMillis(),
        )

        val batch = firestore.batch()
        batch.set(
            firestore.collection("users").document(toUserId)
                .collection("followers").document(fromUserId),
            followerData,
        )
        batch.set(
            firestore.collection("users").document(fromUserId)
                .collection("following").document(toUserId),
            followingData,
        )
        batch.commit().await()

        runCatching {
            firestore.collection("users").document(toUserId)
                .update("stats.followers", FieldValue.increment(1))
                .await()
        }
        runCatching {
            firestore.collection("users").document(fromUserId)
                .update("stats.following", FieldValue.increment(1))
                .await()
        }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun isUserPrivate(userId: String): Result<Boolean> {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (!doc.exists()) return Result.failure(Exception("User not found"))
            val privacy = (doc.get("privacySettings") as? Map<*, *>)?.get("privateAccount") as? Boolean
            val explicit = doc.getBoolean("privateAccount")
            val isPrivate = privacy ?: explicit ?: false
            return Result.success(isPrivate)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val incomingRef = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .document(requestId)

        val incomingDoc = incomingRef.get().await()
        val fromUserId = incomingDoc.getString("fromUserId") ?: throw Exception("Friend request not found")


        val batch = firestore.batch()

        // Mark all pending incoming requests from this sender as accepted.
        val allIncomingFromSender = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        for (doc in allIncomingFromSender.documents) {
            batch.delete(doc.reference)
        }

        // Mark any local outgoing requests to this user as accepted too.
        val allOutgoingToSender = firestore.collection("users").document(currentUserId)
            .collection("outgoingFriendRequests")
            .whereEqualTo("toUserId", fromUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        for (doc in allOutgoingToSender.documents) {
            batch.delete(doc.reference)
        }

        batch.commit().await()

        // Instagram-style accept: sender becomes a follower of the current user,
        // and the sender's following list points to the current user.
        runCatching {
            firestore.collection("users").document(currentUserId)
                .collection("followers").document(fromUserId)
                .set(
                    mapOf(
                        "userId" to fromUserId,
                        "displayName" to (incomingDoc.getString("fromUserName") ?: ""),
                        "handle" to (incomingDoc.getString("fromUserHandle") ?: ""),
                        "avatarEmoji" to (incomingDoc.getString("fromUserAvatar") ?: "👤"),
                        "profileImageUrl" to (incomingDoc.getString("fromUserProfileImageUrl") ?: ""),
                        "isOnline" to false,
                        "isMutual" to false,
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )
                .await()
        }.onFailure { e ->
            Log.w(TAG, "acceptFriendRequest: followers write skipped due rules: ${e.message}")
        }

        runCatching {
            firestore.collection("users").document(fromUserId)
                .collection("following").document(currentUserId)
                .set(
                    mapOf(
                        "userId" to currentUserId,
                        "displayName" to (incomingDoc.getString("toUserName") ?: ""),
                        "handle" to (incomingDoc.getString("toUserHandle") ?: ""),
                        "avatarEmoji" to (incomingDoc.getString("toUserAvatar") ?: "👤"),
                        "profileImageUrl" to (incomingDoc.getString("toUserProfileImageUrl") ?: ""),
                        "isOnline" to false,
                        "isMutual" to false,
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )
                .await()
        }.onFailure { e ->
            Log.w(TAG, "acceptFriendRequest: following write skipped due rules: ${e.message}")
        }

        runCatching {
            firestore.collection("users").document(currentUserId)
                .collection("outgoingFriendRequests")
                .document(requestId)
                .set(
                    mapOf(
                        "id" to requestId,
                        "fromUserId" to fromUserId,
                        "fromUserName" to (incomingDoc.getString("fromUserName") ?: ""),
                        "fromUserHandle" to (incomingDoc.getString("fromUserHandle") ?: ""),
                        "fromUserAvatar" to (incomingDoc.getString("fromUserAvatar") ?: "👤"),
                        "fromUserProfileImageUrl" to (incomingDoc.getString("fromUserProfileImageUrl") ?: ""),
                        "toUserId" to currentUserId,
                        "toUserName" to (incomingDoc.getString("toUserName") ?: ""),
                        "toUserHandle" to (incomingDoc.getString("toUserHandle") ?: ""),
                        "toUserAvatar" to (incomingDoc.getString("toUserAvatar") ?: "👤"),
                        "toUserProfileImageUrl" to (incomingDoc.getString("toUserProfileImageUrl") ?: ""),
                        "status" to "ACCEPTED",
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )
                .await()
        }.onFailure { e ->
            Log.w(TAG, "acceptFriendRequest: outgoing history write skipped due rules: ${e.message}")
        }

        // Best effort: sync sender's outgoing copy so their Outgoing tab updates in real time.
        runCatching {
            val senderOutgoing = firestore.collection("users").document(fromUserId)
                .collection("outgoingFriendRequests")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            val syncBatch = firestore.batch()
            senderOutgoing.documents.forEach { doc ->
                syncBatch.set(doc.reference, mapOf("status" to "ACCEPTED"), SetOptions.merge())
            }
            if (!senderOutgoing.isEmpty) {
                syncBatch.commit().await()
            }
        }.onFailure { e ->
            Log.w(TAG, "acceptFriendRequest: sender outgoing sync skipped due rules: ${e.message}")
        }

        runCatching {
            firestore.collection("users").document(currentUserId)
                .update("stats.followers", FieldValue.increment(1))
                .await()
        }
        runCatching {
            firestore.collection("users").document(fromUserId)
                .update("stats.following", FieldValue.increment(1))
                .await()
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val incomingRef = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .document(requestId)

        val incomingDoc = incomingRef.get().await()
        val fromUserId = incomingDoc.getString("fromUserId") ?: throw Exception("Friend request not found")

        val batch = firestore.batch()

        val incomingRequests = firestore.collection("users").document(currentUserId)
            .collection("friendRequests")
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        for (doc in incomingRequests.documents) {
            batch.delete(doc.reference)
        }

        val outgoingRequests = firestore.collection("users").document(currentUserId)
            .collection("outgoingFriendRequests")
            .whereEqualTo("toUserId", fromUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        for (doc in outgoingRequests.documents) {
            batch.delete(doc.reference)
        }

        batch.commit().await()

        runCatching {
            firestore.collection("users").document(currentUserId)
                .collection("outgoingFriendRequests")
                .document(requestId)
                .set(
                    mapOf(
                        "id" to requestId,
                        "fromUserId" to fromUserId,
                        "fromUserName" to (incomingDoc.getString("fromUserName") ?: ""),
                        "fromUserHandle" to (incomingDoc.getString("fromUserHandle") ?: ""),
                        "fromUserAvatar" to (incomingDoc.getString("fromUserAvatar") ?: "👤"),
                        "fromUserProfileImageUrl" to (incomingDoc.getString("fromUserProfileImageUrl") ?: ""),
                        "toUserId" to currentUserId,
                        "toUserName" to (incomingDoc.getString("toUserName") ?: ""),
                        "toUserHandle" to (incomingDoc.getString("toUserHandle") ?: ""),
                        "toUserAvatar" to (incomingDoc.getString("toUserAvatar") ?: "👤"),
                        "toUserProfileImageUrl" to (incomingDoc.getString("toUserProfileImageUrl") ?: ""),
                        "status" to "REJECTED",
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )
                .await()
        }.onFailure { e ->
            Log.w(TAG, "rejectFriendRequest: outgoing history write skipped due rules: ${e.message}")
        }

        // Best effort: clear sender's outgoing copy so their Outgoing tab does not keep stale pending rows.
        runCatching {
            val senderOutgoingRequests = firestore.collection("users").document(fromUserId)
                .collection("outgoingFriendRequests")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            val syncBatch = firestore.batch()
            senderOutgoingRequests.documents.forEach { doc ->
                syncBatch.delete(doc.reference)
            }
            if (!senderOutgoingRequests.isEmpty) {
                syncBatch.commit().await()
            }
        }.onFailure { e ->
            Log.w(TAG, "rejectFriendRequest: sender outgoing sync skipped due rules: ${e.message}")
        }
        
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
            .collection("following")
            .document(friendId)
            .delete()
            .await()
        
        // Also remove from the other user's followers list.
        runCatching {
            firestore.collection("users").document(friendId)
                .collection("followers")
                .document(userId)
                .delete()
                .await()
        }

        // Legacy cleanup if any old friends docs still exist.
        runCatching {
            firestore.collection("users").document(userId)
                .collection("friends")
                .document(friendId)
                .delete()
                .await()
        }
        runCatching {
            firestore.collection("users").document(friendId)
                .collection("friends")
                .document(userId)
                .delete()
                .await()
        }

        runCatching {
            firestore.collection("users").document(userId)
                .update("stats.following", FieldValue.increment(-1))
                .await()
        }
        runCatching {
            firestore.collection("users").document(friendId)
                .update("stats.followers", FieldValue.increment(-1))
                .await()
        }
        
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
            "updatedAt" to System.currentTimeMillis(),
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
            "updatedAt" to System.currentTimeMillis(),
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
            "updatedAt" to System.currentTimeMillis(),
        )
        
        firestore.collection("users").document(userId)
            .update(userData)
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncProfileStats(userId: String): Result<Unit> = try {
        val followersSnapshot = firestore.collection("users").document(userId)
            .collection("followers")
            .get()
            .await()

        val followingSnapshot = firestore.collection("users").document(userId)
            .collection("following")
            .get()
            .await()

        val friendsSnapshot = firestore.collection("users").document(userId)
            .collection("friends")
            .get()
            .await()

        val postsSnapshot = firestore.collection("posts")
            .get()
            .await()

        val followerCount = followersSnapshot.size()
        val followingCount = followingSnapshot.size()
        val friendsCount = friendsSnapshot.size()
        val postCount = postsSnapshot.documents.count { doc ->
            val authorId = doc.getString("authorId") ?: doc.getString("userId") ?: ""
            authorId == userId
        }

        if (friendsCount > 0) {
            val mirrorBatch = firestore.batch()
            friendsSnapshot.documents.forEach { friendDoc ->
                val friendId = friendDoc.getString("userId") ?: friendDoc.id
                val friendData = mapOf(
                    "userId" to friendId,
                    "displayName" to (friendDoc.getString("displayName") ?: ""),
                    "handle" to (friendDoc.getString("handle") ?: ""),
                    "avatarEmoji" to (friendDoc.getString("avatarEmoji") ?: "👤"),
                    "profileImageUrl" to (friendDoc.getString("profileImageUrl") ?: ""),
                    "isOnline" to (friendDoc.getBoolean("isOnline") ?: false),
                    "isMutual" to true,
                    "createdAt" to System.currentTimeMillis(),
                )

                mirrorBatch.set(
                    firestore.collection("users").document(userId)
                        .collection("followers")
                        .document(friendId),
                    friendData,
                    SetOptions.merge()
                )
                mirrorBatch.set(
                    firestore.collection("users").document(userId)
                        .collection("following")
                        .document(friendId),
                    friendData,
                    SetOptions.merge()
                )
            }
            mirrorBatch.commit().await()
        }

        val normalizedFollowers = if (followerCount == 0 && friendsCount > 0) friendsCount else followerCount
        val normalizedFollowing = if (followingCount == 0 && friendsCount > 0) friendsCount else followingCount

        firestore.collection("users").document(userId)
            .update(
                mapOf(
                    "stats.posts" to postCount,
                    "stats.followers" to normalizedFollowers,
                    "stats.following" to normalizedFollowing,
                    "updatedAt" to System.currentTimeMillis(),
                )
            )
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
        val listener = firestore.collection("presence").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val isOnline = snapshot?.getBoolean("isOnline")
                    ?: snapshot?.getBoolean("active")
                    ?: snapshot?.getBoolean("connected")
                    ?: snapshot?.getBoolean("present")
                    ?: false
                trySend(Result.success(isOnline))
            }
        
        awaitClose { listener.remove() }
    }

    // Helper extensions
    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User {
        // Parse nested stats map correctly
        val statsMap = this.get("stats") as? Map<*, *>
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
                userId = this.getString("userId") ?: this.id,
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toOutgoingMirrorFriendRequest(): FriendRequest? {
        return try {
            val parsedStatus = when (this.getString("status")) {
                "ACCEPTED" -> FriendRequestStatus.ACCEPTED
                "REJECTED" -> FriendRequestStatus.REJECTED
                "BLOCKED" -> FriendRequestStatus.BLOCKED
                else -> FriendRequestStatus.PENDING
            }

            FriendRequest(
                id = "mirror_${this.id}",
                fromUserId = this.getString("toUserId") ?: "",
                fromUserName = this.getString("toUserName") ?: "",
                fromUserHandle = this.getString("toUserHandle") ?: "",
                fromUserAvatar = this.getString("toUserAvatar") ?: "👤",
                fromUserProfileImageUrl = this.getString("toUserProfileImageUrl") ?: "",
                toUserId = this.getString("fromUserId") ?: "",
                toUserName = this.getString("fromUserName") ?: "",
                toUserHandle = this.getString("fromUserHandle") ?: "",
                toUserAvatar = this.getString("fromUserAvatar") ?: "👤",
                toUserProfileImageUrl = this.getString("fromUserProfileImageUrl") ?: "",
                status = parsedStatus,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toOutgoingFromFollowerFriendRequest(currentUserId: String): FriendRequest? {
        return try {
            val followerId = this.getString("userId") ?: this.id
            FriendRequest(
                id = "follower_$followerId",
                fromUserId = currentUserId,
                fromUserName = "",
                fromUserHandle = "",
                fromUserAvatar = "👤",
                fromUserProfileImageUrl = "",
                toUserId = followerId,
                toUserName = this.getString("displayName") ?: "",
                toUserHandle = this.getString("handle") ?: "",
                toUserAvatar = this.getString("avatarEmoji") ?: "👤",
                toUserProfileImageUrl = this.getString("profileImageUrl") ?: "",
                status = FriendRequestStatus.ACCEPTED,
            )
        } catch (e: Exception) {
            null
        }
    }
}
