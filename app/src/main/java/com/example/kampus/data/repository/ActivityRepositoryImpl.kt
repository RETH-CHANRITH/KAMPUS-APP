@file:Suppress("SpellCheckingInspection")
package com.example.kampus.data.repository

import android.net.Uri
import com.example.kampus.ui.activity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.kampus.utils.NotificationLogger

/**
 * Repository for managing user activities with realtime Firestore sync
 * Handles all CRUD operations and realtime listeners
 */
class ActivityRepositoryImpl(private val firestore: FirebaseFirestore) {

    private companion object {
        const val ACTIVITIES_COLLECTION = "activities"
        const val COMMENTS_COLLECTION = "comments"
        const val REACTIONS_COLLECTION = "reactions"
        const val ANALYTICS_COLLECTION = "analytics"
        const val ARCHIVED_COLLECTION = "archived_activities"
        const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity Operations - Realtime Listeners
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get user's activities with realtime updates
     */
    fun getUserActivities(userId: String): Flow<Result<List<ActivityItem>>> = callbackFlow {
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isArchived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val activities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toActivityItem()
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(Result.success(activities))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get pinned activities (sorted to top)
     */
    fun getPinnedActivities(userId: String): Flow<Result<List<ActivityItem>>> = callbackFlow {
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isPinned", true)
            .whereEqualTo("isArchived", false)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val activities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toActivityItem()
                } ?: emptyList()
                
                trySend(Result.success(activities))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get activity by ID with realtime updates
     */
    fun getActivityById(activityId: String): Flow<Result<ActivityItem?>> = callbackFlow {
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val activity = if (snapshot?.exists() == true) {
                    snapshot.toActivityItem()
                } else {
                    null
                }
                
                trySend(Result.success(activity))
            }

        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity Management - CRUD Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create new activity
     */
    suspend fun createActivity(activity: ActivityItem): Result<String> = try {
        val activityMap = activity.toFirestoreMap()
        val ref = firestore.collection(ACTIVITIES_COLLECTION).add(activityMap).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Update activity - realtime sync
     */
    suspend fun updateActivity(activityId: String, updates: Map<String, Any>): Result<Unit> = try {
        val updateMap = updates.toMutableMap()
        updateMap["updatedAt"] = System.currentTimeMillis()
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(updateMap)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Delete activity permanently
     */
    suspend fun deleteActivity(activityId: String): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pin/Unpin - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pin activity to top - realtime update
     */
    suspend fun pinActivity(activityId: String, isPinned: Boolean): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf(
                "isPinned" to isPinned,
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privacy & Visibility - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update activity privacy - realtime change
     */
    suspend fun updatePrivacy(activityId: String, privacy: ActivityItem.ActivityPrivacy): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>(
                "privacy" to privacy.name,
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Hide activity from profile
     */
    suspend fun hideFromProfile(activityId: String, isHidden: Boolean): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>(
                "hiddenFromProfile" to isHidden,
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Archive - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Archive activity (move to hidden collection)
     */
    suspend fun archiveActivity(activityId: String): Result<Unit> = try {
        val activity = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .get()
            .await()

        activity.data?.let { data ->
            // Copy to archived collection
            firestore.collection(ARCHIVED_COLLECTION)
                .document(activityId)
                .set(data + ("archivedAt" to System.currentTimeMillis()))
                .await()
        }

        // Delete from active collection
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>("isArchived" to true))
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Restore archived activity
     */
    suspend fun restoreActivity(activityId: String): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>("isArchived" to false))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit Activity - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Edit activity content with history tracking
     */
    suspend fun editActivity(
        activityId: String,
        newTitle: String,
        newDescription: String,
        newTags: List<String> = emptyList(),
        newLocation: String? = null,
        newMediaUri: Uri? = null
    ): Result<Unit> = try {
        // Create edit record
        val editRecord = EditRecord(
            editedAt = System.currentTimeMillis(),
            editedBy = "", // Should be passed in
            previousContent = "",
            newContent = newDescription,
            changeType = "text"
        )

        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>(
                "title" to newTitle,
                "description" to newDescription,
                "tags" to newTags,
                "location" to (newLocation ?: ""),
                "editHistory" to com.google.firebase.firestore.FieldValue.arrayUnion(editRecord.toMap()),
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reactions & Likes - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Add or update reaction on activity
     */
    suspend fun addReaction(
        activityId: String,
        userId: String,
        reactionType: String = "like"
    ): Result<Unit> = try {
        val activitySnapshot = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .get()
            .await()

        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>(
                "reactions.$reactionType" to com.google.firebase.firestore.FieldValue.increment(1),
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()

        // Store individual reaction for tracking
        firestore.collection(REACTIONS_COLLECTION)
            .document("${activityId}_${userId}_${reactionType}")
            .set(mapOf(
                "activityId" to activityId,
                "userId" to userId,
                "type" to reactionType,
                "createdAt" to System.currentTimeMillis()
            ))
            .await()

        val ownerId = activitySnapshot.getString("userId").orEmpty()
        val activityTitle = activitySnapshot.getString("title").orEmpty().ifBlank { "your post" }
        runCatching {
            NotificationLogger.notifyUser(
                toUserId = ownerId,
                type = reactionType.ifBlank { "reaction" },
                title = "New ${reactionType.ifBlank { "reaction" }}",
                body = "Someone reacted to $activityTitle",
                targetId = activityId,
            )
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Remove reaction from activity
     */
    suspend fun removeReaction(
        activityId: String,
        userId: String,
        reactionType: String = "like"
    ): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf(
                "reactions.$reactionType" to com.google.firebase.firestore.FieldValue.increment(-1),
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()

        // Remove individual reaction record
        firestore.collection(REACTIONS_COLLECTION)
            .document("${activityId}_${userId}_${reactionType}")
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get reactions on activity with realtime updates
     */
    fun getActivityReactions(activityId: String): Flow<Result<Map<String, Int>>> = callbackFlow {
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                @Suppress("UNCHECKED_CAST")
                val reactions = snapshot?.get("reactions") as? Map<String, Int> ?: emptyMap()
                trySend(Result.success(reactions))
            }

        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comments - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get comments on activity with realtime updates
     */
    fun getActivityComments(activityId: String): Flow<Result<List<ActivityComment>>> = callbackFlow {
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .collection(COMMENTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toActivityComment()
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Result.success(comments))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add comment to activity
     */
    suspend fun addComment(
        activityId: String,
        comment: ActivityComment
    ): Result<String> = try {
        val activitySnapshot = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .get()
            .await()

        val ref = firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .collection(COMMENTS_COLLECTION)
            .add(comment.toMap())
            .await()

        // Increment comment count
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>("comments" to com.google.firebase.firestore.FieldValue.increment(1)))
            .await()

        val ownerId = activitySnapshot.getString("userId").orEmpty()
        val activityTitle = activitySnapshot.getString("title").orEmpty().ifBlank { "your post" }
        runCatching {
            NotificationLogger.notifyUser(
                toUserId = ownerId,
                type = "comment",
                title = "New comment",
                body = "${comment.username.ifBlank { "Someone" }} commented on $activityTitle",
                targetId = ref.id,
            )
        }

        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Delete comment from activity
     */
    suspend fun deleteComment(activityId: String, commentId: String): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .collection(COMMENTS_COLLECTION)
            .document(commentId)
            .delete()
            .await()

        // Decrement comment count
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>("comments" to com.google.firebase.firestore.FieldValue.increment(-1)))
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Share & Analytics - Realtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Increment share counter
     */
    suspend fun incrementShareCount(activityId: String): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>(
                "shares" to com.google.firebase.firestore.FieldValue.increment(1),
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Increment view counter
     */
    suspend fun incrementViewCount(activityId: String): Result<Unit> = try {
        firestore.collection(ACTIVITIES_COLLECTION)
            .document(activityId)
            .update(mapOf<String, Any>(
                "views" to com.google.firebase.firestore.FieldValue.increment(1),
                "lastInteractionTime" to System.currentTimeMillis()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Extension Functions
    // ─────────────────────────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toActivityItem(): ActivityItem {
        @Suppress("UNCHECKED_CAST")
        return ActivityItem(
            id = id,
            userId = getString("userId") ?: "",
            username = getString("username") ?: "",
            userAvatar = getString("userAvatar") ?: "👤",
            title = getString("title") ?: "",
            description = getString("description") ?: "",
            activityType = ActivityItem.ActivityType.valueOf(getString("activityType") ?: "POST"),
            location = getString("location"),
            tags = get("tags") as? List<String> ?: emptyList(),
            taggedUsers = get("taggedUsers") as? List<String> ?: emptyList(),
            feeling = getString("feeling"),
            coverColor = getString("coverColor"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            privacy = ActivityItem.ActivityPrivacy.valueOf(getString("privacy") ?: "PUBLIC"),
            isPinned = getBoolean("isPinned") ?: false,
            isArchived = getBoolean("isArchived") ?: false,
            isFeatured = getBoolean("isFeatured") ?: false,
            allowComments = getBoolean("allowComments") ?: true,
            hiddenFromProfile = getBoolean("hiddenFromProfile") ?: false,
            likes = (get("likes") as? Number)?.toInt() ?: 0,
            comments = (get("comments") as? Number)?.toInt() ?: 0,
            shares = (get("shares") as? Number)?.toInt() ?: 0,
            saves = (get("saves") as? Number)?.toInt() ?: 0,
            views = (get("views") as? Number)?.toInt() ?: 0,
            reactions = (get("reactions") as? Map<String, Int>) ?: emptyMap(),
            reach = (get("reach") as? Number)?.toInt() ?: 0,
            isLikedByUser = getBoolean("isLikedByUser") ?: false,
            isSavedByUser = getBoolean("isSavedByUser") ?: false,
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toActivityComment(): ActivityComment {
        @Suppress("UNCHECKED_CAST")
        return ActivityComment(
            id = id,
            activityId = getString("activityId") ?: "",
            userId = getString("userId") ?: "",
            username = getString("username") ?: "",
            userAvatar = getString("userAvatar") ?: "👤",
            text = getString("text") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            isEdited = getBoolean("isEdited") ?: false,
            reactions = (get("reactions") as? Map<String, Int>) ?: emptyMap(),
        )
    }

    private fun ActivityItem.toFirestoreMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username,
        "userAvatar" to userAvatar,
        "title" to title,
        "description" to description,
        "activityType" to activityType.name,
        "location" to (location ?: ""),
        "tags" to tags,
        "taggedUsers" to taggedUsers,
        "feeling" to (feeling ?: ""),
        "privacy" to privacy.name,
        "isPinned" to isPinned,
        "isArchived" to isArchived,
        "isFeatured" to isFeatured,
        "allowComments" to allowComments,
        "hiddenFromProfile" to hiddenFromProfile,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "likes" to likes,
        "comments" to comments,
        "shares" to shares,
        "saves" to saves,
        "views" to views,
        "reactions" to reactions,
        "reach" to reach,
        "isLikedByUser" to isLikedByUser,
        "isSavedByUser" to isSavedByUser,
        "syncStatus" to syncStatus.name,
    )

    private fun ActivityComment.toMap(): Map<String, Any> = mapOf(
        "activityId" to activityId,
        "userId" to userId,
        "username" to username,
        "userAvatar" to userAvatar,
        "text" to text,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isEdited" to isEdited,
        "reactions" to reactions,
    )

    private fun EditRecord.toMap(): Map<String, Any> = mapOf(
        "editedAt" to editedAt,
        "editedBy" to editedBy,
        "previousContent" to previousContent,
        "newContent" to newContent,
        "changeType" to changeType,
    )
}
