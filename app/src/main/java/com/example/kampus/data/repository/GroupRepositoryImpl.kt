package com.example.kampus.data.repository

import androidx.compose.ui.graphics.Color
import com.example.kampus.ui.groups.GroupData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepositoryImpl(private val firestore: FirebaseFirestore) {

    fun getGroups(): Flow<Result<List<GroupData>>> = callbackFlow {
        val listener = firestore.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        GroupData(
                            id = (doc.get("id") as? Number)?.toInt() ?: doc.id.hashCode(),
                            name = doc.getString("name") ?: "Untitled Group",
                            category = doc.getString("category") ?: "General",
                            coverColor1 = Color(0xFF1E1040),
                            coverColor2 = Color(0xFF2D1B6E),
                            coverEmoji = doc.getString("coverEmoji") ?: "👥",
                            members = doc.getString("members") ?: "0",
                            posts = doc.getString("posts") ?: "0",
                            isJoined = doc.getBoolean("isJoined") ?: false,
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(groups))
            }

        awaitClose { listener.remove() }
    }

    fun getMyGroups(): Flow<Result<List<GroupData>>> = callbackFlow {
        val listener = firestore.collection("groups")
            .whereEqualTo("isJoined", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        GroupData(
                            id = (doc.get("id") as? Number)?.toInt() ?: doc.id.hashCode(),
                            name = doc.getString("name") ?: "Untitled Group",
                            category = doc.getString("category") ?: "General",
                            coverColor1 = Color(0xFF1E1040),
                            coverColor2 = Color(0xFF2D1B6E),
                            coverEmoji = doc.getString("coverEmoji") ?: "👥",
                            members = doc.getString("members") ?: "0",
                            posts = doc.getString("posts") ?: "0",
                            isJoined = doc.getBoolean("isJoined") ?: false,
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(groups))
            }

        awaitClose { listener.remove() }
    }

    fun getDiscoverGroups(): Flow<Result<List<GroupData>>> = callbackFlow {
        val listener = firestore.collection("groups")
            .whereEqualTo("isJoined", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        GroupData(
                            id = (doc.get("id") as? Number)?.toInt() ?: doc.id.hashCode(),
                            name = doc.getString("name") ?: "Untitled Group",
                            category = doc.getString("category") ?: "General",
                            coverColor1 = Color(0xFF1E1040),
                            coverColor2 = Color(0xFF2D1B6E),
                            coverEmoji = doc.getString("coverEmoji") ?: "👥",
                            members = doc.getString("members") ?: "0",
                            posts = doc.getString("posts") ?: "0",
                            isJoined = doc.getBoolean("isJoined") ?: false,
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
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
