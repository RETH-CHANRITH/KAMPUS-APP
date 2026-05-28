package com.example.kampus.data.repository

import androidx.compose.ui.graphics.Color
import com.example.kampus.ui.groups.GroupJoinRequest
import com.example.kampus.ui.groups.GroupData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepositoryImpl(private val firestore: FirebaseFirestore) {

    private fun parseColor(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, fallback: Color): Color {
        val raw = doc.get(field)
        val value = (raw as? Number)?.toLong()
            ?: doc.getLong(field)
            ?: return fallback
        return Color(value.toULong())
    }

    private fun toGroupData(doc: com.google.firebase.firestore.DocumentSnapshot): GroupData {
        return GroupData(
            id = (doc.get("id") as? Number)?.toInt() ?: doc.id.hashCode(),
            name = doc.getString("name") ?: "Untitled Group",
            category = doc.getString("category") ?: "General",
            coverColor1 = parseColor(doc, "coverColor1", Color(0xFF1E1040)),
            coverColor2 = parseColor(doc, "coverColor2", Color(0xFF2D1B6E)),
            coverEmoji = doc.getString("coverEmoji") ?: "👥",
            members = doc.getString("members") ?: "0",
            posts = doc.getString("posts") ?: "0",
            isJoined = doc.getBoolean("isJoined") ?: false,
            privacy = doc.getString("privacy") ?: "public",
            ownerId = doc.getString("ownerId") ?: "",
            description = doc.getString("description") ?: ""
        )
    }

    fun getGroups(): Flow<Result<List<GroupData>>> = callbackFlow {
        val listener = firestore.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { toGroupData(doc) }.getOrNull()
                } ?: emptyList()

                trySend(Result.success(groups))
            }

        awaitClose { listener.remove() }
    }

    fun getMyGroups(): Flow<Result<List<GroupData>>> = callbackFlow {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val listener = firestore.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { toGroupData(doc) }
                        .getOrNull()
                        ?.takeIf { it.ownerId == currentUserId || it.isJoined }
                } ?: emptyList()

                trySend(Result.success(groups))
            }

        awaitClose { listener.remove() }
    }

    fun getDiscoverGroups(): Flow<Result<List<GroupData>>> = callbackFlow {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val listener = firestore.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { toGroupData(doc) }
                        .getOrNull()
                        ?.takeIf { it.ownerId != currentUserId }
                } ?: emptyList()

                trySend(Result.success(groups))
            }

        awaitClose { listener.remove() }
    }

    suspend fun createGroup(group: Map<String, Any>): Result<String> = try {
        val ref = firestore.collection("groups").add(group).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeJoinRequests(groupId: String): Flow<Result<List<GroupJoinRequest>>> = callbackFlow {
        val listener = firestore.collection("groups")
            .document(groupId)
            .collection("joinRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        GroupJoinRequest(
                            requesterId = doc.getString("requesterId") ?: doc.id,
                            requesterName = doc.getString("requesterName") ?: "Unknown",
                            requestedAt = doc.getLong("requestedAt") ?: 0L,
                            status = doc.getString("status") ?: "pending",
                        )
                    }.getOrNull()
                }?.sortedByDescending { it.requestedAt } ?: emptyList()

                trySend(Result.success(requests))
            }

        awaitClose { listener.remove() }
    }

    suspend fun requestJoinGroup(groupId: String, request: GroupJoinRequest): Result<Unit> = try {
        firestore.collection("groups")
            .document(groupId)
            .collection("joinRequests")
            .document(request.requesterId)
            .set(
                mapOf(
                    "requesterId" to request.requesterId,
                    "requesterName" to request.requesterName,
                    "requestedAt" to request.requestedAt,
                    "status" to request.status,
                )
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun approveJoinRequest(groupId: String, requesterId: String): Result<Unit> = try {
        firestore.collection("groups")
            .document(groupId)
            .collection("joinRequests")
            .document(requesterId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rejectJoinRequest(groupId: String, requesterId: String): Result<Unit> = try {
        firestore.collection("groups")
            .document(groupId)
            .collection("joinRequests")
            .document(requesterId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun joinGroup(groupId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId).update("isJoined", true).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId).update("isJoined", false).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
