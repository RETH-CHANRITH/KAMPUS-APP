package com.example.kampus.data.repository

import com.example.kampus.data.model.Group
import com.example.kampus.data.model.GroupMember
import com.example.kampus.data.model.GroupPost
import com.example.kampus.data.model.GroupPrivacy
import com.example.kampus.data.model.JoinRequest
import com.example.kampus.data.model.MemberRole
import com.example.kampus.data.model.MembershipStatus
import com.example.kampus.data.model.PostReportReason
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Real-time Firestore repository for the Groups feature.
 *
 * Firestore structure:
 *   groups/{groupId}
 *     members/{userId}
 *     joinRequests/{userId}
 *     posts/{postId}
 */
class GroupsRepositoryImpl(private val firestore: FirebaseFirestore) {

    private val auth = FirebaseAuth.getInstance()

    // ── Real-time observers ───────────────────────────────────────────────────

    /** All groups, ordered newest first. */
    fun observeGroups(): Flow<Result<List<Group>>> = callbackFlow {
        val listener = firestore.collection("groups")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toGroup() }.getOrNull()
                } ?: emptyList()
                trySend(Result.success(groups))
            }
        awaitClose { listener.remove() }
    }

    /** Single group document, live updates. */
    fun observeGroup(groupId: String): Flow<Result<Group?>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val group = if (snapshot != null && snapshot.exists())
                    runCatching { snapshot.toGroup() }.getOrNull()
                else null
                trySend(Result.success(group))
            }
        awaitClose { listener.remove() }
    }

    /** Posts for a group, ordered newest first. */
    fun observePosts(groupId: String): Flow<Result<List<GroupPost>>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val listener = firestore.collection("groups").document(groupId)
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toGroupPost(currentUserId) }.getOrNull()
                } ?: emptyList()
                trySend(Result.success(posts))
            }
        awaitClose { listener.remove() }
    }

    /** Members of a group, live. */
    fun observeMembers(groupId: String): Flow<Result<List<GroupMember>>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val members = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toGroupMember(groupId) }.getOrNull()
                } ?: emptyList()
                trySend(Result.success(members))
            }
        awaitClose { listener.remove() }
    }

    /** Pending join requests for a group, live. */
    fun observeJoinRequestsNew(groupId: String): Flow<Result<List<JoinRequest>>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .collection("joinRequests")
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toJoinRequest(groupId) }.getOrNull()
                } ?: emptyList()
                trySend(Result.success(requests))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Returns all groupIds where the current user is an active member.
     * Uses a collectionGroup query on "members".
     */
    fun observeMyMemberGroupIds(): Flow<Result<Set<String>>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        if (currentUserId.isBlank()) {
            trySend(Result.success(emptySet()))
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collectionGroup("members")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.toSet() ?: emptySet()
                trySend(Result.success(ids))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Returns all groupIds where the current user has a pending join request.
     * Uses a collectionGroup query on "joinRequests".
     */
    fun observeMyPendingGroupIds(): Flow<Result<Set<String>>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        if (currentUserId.isBlank()) {
            trySend(Result.success(emptySet()))
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collectionGroup("joinRequests")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.toSet() ?: emptySet()
                trySend(Result.success(ids))
            }
        awaitClose { listener.remove() }
    }

    // ── Write operations ──────────────────────────────────────────────────────

    suspend fun createGroup(
        name: String,
        category: String,
        description: String,
        privacy: GroupPrivacy,
        coverImageUrl: String = "",
    ): Result<String> = try {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Not signed in"))
        val now = System.currentTimeMillis()
        val ref = firestore.collection("groups").document()

        ref.set(
            mapOf(
                "name" to name.trim(),
                "category" to category,
                "description" to description.trim(),
                "coverImageUrl" to coverImageUrl,
                "privacy" to privacy.name,
                "memberCount" to 1,
                "postCount" to 0,
                "adminId" to currentUser.uid,
                "createdAt" to now,
            )
        ).await()

        // Add creator as ADMIN member
        ref.collection("members").document(currentUser.uid).set(
            mapOf(
                "userId" to currentUser.uid,
                "userName" to (currentUser.displayName ?: "Unknown"),
                "userAvatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                "role" to MemberRole.ADMIN.name,
                "status" to MembershipStatus.MEMBER.name,
                "joinedAt" to now,
            )
        ).await()

        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createPost(
        groupId: String,
        content: String,
        imageUrl: String = "",
    ): Result<String> = try {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Not signed in"))
        val now = System.currentTimeMillis()
        val ref = firestore.collection("groups").document(groupId)
            .collection("posts").document()

        ref.set(
            mapOf(
                "groupId" to groupId,
                "authorId" to currentUser.uid,
                "authorName" to (currentUser.displayName ?: "Unknown"),
                "authorAvatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                "content" to content.trim(),
                "imageUrl" to imageUrl,
                "likeCount" to 0,
                "commentCount" to 0,
                "likedBy" to emptyList<String>(),
                "isReported" to false,
                "reportCount" to 0,
                "createdAt" to now,
            )
        ).await()

        // Increment post count on the group
        firestore.collection("groups").document(groupId)
            .update("postCount", FieldValue.increment(1)).await()

        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun joinPublicGroup(groupId: String): Result<Unit> = try {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Not signed in"))
        val now = System.currentTimeMillis()

        firestore.collection("groups").document(groupId)
            .collection("members").document(currentUser.uid)
            .set(
                mapOf(
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Unknown"),
                    "userAvatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "role" to MemberRole.MEMBER.name,
                    "status" to MembershipStatus.MEMBER.name,
                    "joinedAt" to now,
                )
            ).await()

        firestore.collection("groups").document(groupId)
            .update("memberCount", FieldValue.increment(1)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun requestJoinPrivateGroup(groupId: String): Result<Unit> = try {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Not signed in"))
        val now = System.currentTimeMillis()

        firestore.collection("groups").document(groupId)
            .collection("joinRequests").document(currentUser.uid)
            .set(
                mapOf(
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Unknown"),
                    "userAvatarUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "requestedAt" to now,
                )
            ).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cancelJoinRequest(groupId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not signed in"))

        firestore.collection("groups").document(groupId)
            .collection("joinRequests").document(currentUserId)
            .delete().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not signed in"))

        firestore.collection("groups").document(groupId)
            .collection("members").document(currentUserId)
            .delete().await()

        firestore.collection("groups").document(groupId)
            .update("memberCount", FieldValue.increment(-1)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleLikePost(groupId: String, postId: String): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not signed in"))
        val postRef = firestore.collection("groups").document(groupId)
            .collection("posts").document(postId)
        val snapshot = postRef.get().await()
        val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        if (currentUserId in likedBy) {
            postRef.update(
                "likedBy", FieldValue.arrayRemove(currentUserId),
                "likeCount", FieldValue.increment(-1),
            ).await()
        } else {
            postRef.update(
                "likedBy", FieldValue.arrayUnion(currentUserId),
                "likeCount", FieldValue.increment(1),
            ).await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun reportPost(
        groupId: String,
        postId: String,
        reason: PostReportReason,
        note: String,
    ): Result<Unit> = try {
        val currentUserId = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not signed in"))
        val now = System.currentTimeMillis()
        val postRef = firestore.collection("groups").document(groupId)
            .collection("posts").document(postId)

        postRef.update(
            "isReported", true,
            "reportCount", FieldValue.increment(1),
        ).await()

        postRef.collection("reports").document(currentUserId).set(
            mapOf(
                "reportedByUserId" to currentUserId,
                "reason" to reason.name,
                "additionalNote" to note.trim(),
                "createdAt" to now,
            )
        ).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deletePost(groupId: String, postId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .collection("posts").document(postId)
            .delete().await()

        firestore.collection("groups").document(groupId)
            .update("postCount", FieldValue.increment(-1)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .collection("members").document(userId)
            .delete().await()

        firestore.collection("groups").document(groupId)
            .update("memberCount", FieldValue.increment(-1)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun approveJoinRequest(groupId: String, userId: String): Result<Unit> = try {
        val requestDoc = firestore.collection("groups").document(groupId)
            .collection("joinRequests").document(userId)
            .get().await()

        val userName = requestDoc.getString("userName") ?: "Unknown"
        val userAvatarUrl = requestDoc.getString("userAvatarUrl") ?: ""
        val now = System.currentTimeMillis()

        firestore.collection("groups").document(groupId)
            .collection("members").document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "userAvatarUrl" to userAvatarUrl,
                    "role" to MemberRole.MEMBER.name,
                    "status" to MembershipStatus.MEMBER.name,
                    "joinedAt" to now,
                )
            ).await()

        firestore.collection("groups").document(groupId)
            .collection("joinRequests").document(userId)
            .delete().await()

        firestore.collection("groups").document(groupId)
            .update("memberCount", FieldValue.increment(1)).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rejectJoinRequest(groupId: String, userId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .collection("joinRequests").document(userId)
            .delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toGroup(): Group {
        val privacyStr = getString("privacy") ?: "PUBLIC"
        val privacy = runCatching {
            GroupPrivacy.valueOf(privacyStr.uppercase())
        }.getOrDefault(GroupPrivacy.PUBLIC)

        return Group(
            id = id,
            name = getString("name") ?: "Untitled",
            category = getString("category") ?: "General",
            description = getString("description") ?: "",
            coverImageUrl = getString("coverImageUrl") ?: "",
            privacy = privacy,
            memberCount = (get("memberCount") as? Number)?.toInt() ?: 0,
            postCount = (get("postCount") as? Number)?.toInt() ?: 0,
            adminId = getString("adminId") ?: "",
            createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L,
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toGroupPost(currentUserId: String): GroupPost {
        val likedBy = (get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return GroupPost(
            id = id,
            groupId = getString("groupId") ?: "",
            authorId = getString("authorId") ?: "",
            authorName = getString("authorName") ?: "Unknown",
            authorAvatarUrl = getString("authorAvatarUrl") ?: "",
            content = getString("content") ?: "",
            imageUrl = getString("imageUrl") ?: "",
            likeCount = (get("likeCount") as? Number)?.toInt() ?: 0,
            commentCount = (get("commentCount") as? Number)?.toInt() ?: 0,
            isLikedByCurrentUser = currentUserId in likedBy,
            isReported = getBoolean("isReported") ?: false,
            reportCount = (get("reportCount") as? Number)?.toInt() ?: 0,
            createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L,
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toGroupMember(groupId: String): GroupMember {
        val role = runCatching {
            MemberRole.valueOf((getString("role") ?: "MEMBER").uppercase())
        }.getOrDefault(MemberRole.MEMBER)
        val status = runCatching {
            MembershipStatus.valueOf((getString("status") ?: "MEMBER").uppercase())
        }.getOrDefault(MembershipStatus.MEMBER)

        return GroupMember(
            id = id,
            groupId = groupId,
            userId = getString("userId") ?: id,
            userName = getString("userName") ?: "Unknown",
            userAvatarUrl = getString("userAvatarUrl") ?: "",
            role = role,
            status = status,
            joinedAt = (get("joinedAt") as? Number)?.toLong() ?: 0L,
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toJoinRequest(groupId: String): JoinRequest {
        return JoinRequest(
            id = id,
            groupId = groupId,
            userId = getString("userId") ?: id,
            userName = getString("userName") ?: "Unknown",
            userAvatarUrl = getString("userAvatarUrl") ?: "",
            requestedAt = (get("requestedAt") as? Number)?.toLong() ?: 0L,
        )
    }
}
