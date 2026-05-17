@file:Suppress("SpellCheckingInspection")
package com.example.kampus.ui.activity

import android.net.Uri
import java.util.*

/**
 * Data model for user activity in KAMPUS social media
 * Represents posts, stories, and events that users create
 */
data class ActivityItem(
    // Primary identifiers
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val username: String = "",
    val userAvatar: String = "👤",
    
    // Content
    val title: String = "",
    val description: String = "",
    val mediaUri: Uri? = null,
    val mediaType: ActivityMediaType = ActivityMediaType.NONE,
    val coverEmoji: String? = null,
    
    // Metadata
    val activityType: ActivityType = ActivityType.POST,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val taggedUsers: List<String> = emptyList(),
    val feeling: String? = null,
    val coverColor: String? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val editHistory: List<EditRecord> = emptyList(),
    
    // Privacy & Visibility
    val privacy: ActivityPrivacy = ActivityPrivacy.PUBLIC,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFeatured: Boolean = false,
    val allowComments: Boolean = true,
    val hiddenFromProfile: Boolean = false,
    
    // Interactions - Realtime Counters
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: Int = 0,
    val shares: Int = 0,
    val saves: Int = 0,
    val views: Int = 0,
    
    // Reactions (like Facebook)
    val reactions: Map<String, Int> = emptyMap(), // "like" -> 5, "love" -> 2, etc.
    val userReaction: String? = null,
    
    // Engagement
    val reach: Int = 0,
    val isLikedByUser: Boolean = false,
    val isSavedByUser: Boolean = false,
    val lastInteractionTime: Long = 0,
    
    // Sync & Status
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncTime: Long = 0,
    val offlineChanges: Map<String, Any> = emptyMap(),
) {
    enum class ActivityType {
        POST,           // Regular text/image post
        STORY,          // Temporary story (24h)
        EVENT,          // Event announcement
        MILESTONE,      // Achievement/milestone
        INTRODUCTION,   // User introduction/bio
        MEDIA_GALLERY,  // Photo/video gallery
    }
    
    enum class ActivityPrivacy {
        PUBLIC,          // Everyone can see
        FRIENDS,         // Only friends
        FOLLOWERS,       // Only followers
        UNIVERSITY,      // University members only
        PRIVATE,         // Only me
    }
    
    enum class ActivityMediaType {
        NONE,
        IMAGE,
        VIDEO,
        GALLERY,
        EMOJI,
    }
    
    enum class SyncStatus {
        SYNCED,          // On server
        SYNCING,         // Currently uploading
        PENDING,         // Waiting to sync
        FAILED,          // Sync failed
        OFFLINE,         // Created offline
    }
}

/**
 * Represents an edit made to an activity
 */
data class EditRecord(
    val editedAt: Long = System.currentTimeMillis(),
    val editedBy: String = "",
    val previousContent: String = "",
    val newContent: String = "",
    val changeType: String = "text", // "text", "image", "tags", "location", "privacy"
)

/**
 * Comment on an activity - Realtime synchronized
 */
data class ActivityComment(
    val id: String = UUID.randomUUID().toString(),
    val activityId: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatar: String = "👤",
    val text: String = "",
    val mediaUri: Uri? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val userReaction: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val replies: List<ActivityComment> = emptyList(),
    val syncStatus: ActivityItem.SyncStatus = ActivityItem.SyncStatus.SYNCED,
)

/**
 * Represents a reaction to an activity or comment
 */
data class ActivityReaction(
    val id: String = UUID.randomUUID().toString(),
    val activityId: String = "",
    val userId: String = "",
    val reactionType: String = "like", // "like", "love", "haha", "wow", "sad", "angry"
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Notification for activity interactions
 */
data class ActivityNotification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val activityId: String = "",
    val actionBy: String = "", // Username of person who triggered
    val actionType: NotificationActionType = NotificationActionType.LIKE,
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionByAvatar: String = "👤",
) {
    enum class NotificationActionType {
        LIKE,
        COMMENT,
        SHARE,
        MENTION,
        FOLLOW,
        FEATURE,
        TAG,
    }
}

/**
 * Analytics data for an activity
 */
data class ActivityAnalytics(
    val activityId: String = "",
    val views: Int = 0,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val saves: Int = 0,
    val reach: Int = 0, // Unique users who saw it
    val impressions: Int = 0, // Total times shown
    val engagementRate: Float = 0f,
    val topReactions: Map<String, Int> = emptyMap(),
    val trafficSources: Map<String, Int> = emptyMap(), // "home_feed", "profile", "search", etc.
    val lastUpdated: Long = System.currentTimeMillis(),
)
