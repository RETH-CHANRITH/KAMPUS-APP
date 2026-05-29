package com.example.kampus.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.example.kampus.data.repository.EventRepositoryImpl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await

data class AdminUiState(
    val totalUsers: Int = 0,
    val activeGroups: Int = 0,
    val postsToday: Int = 0,
    val openReports: Int = 0,
    val totalEvents: Int = 0,
    val bannedUsers: Int = 0,
    val usersToday: Int = 0,
    val groupsThisWeek: Int = 0,
    val upcomingEvents: Int = 0,
    val userList: List<AdminUser> = emptyList(),
    val reportedItems: List<ReportedItem> = emptyList(),
    val recentActivities: List<Pair<String, Long>> = emptyList(),
    val pastAnnouncements: List<Triple<String, String, Long>> = emptyList(),
    val manageablePosts: List<ManageableContent> = emptyList(),
    val manageableGroups: List<ManageableContent> = emptyList(),
    val manageableEvents: List<ManageableContent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventRepository = EventRepositoryImpl()
    private var recentPostActivities = emptyList<Pair<String, Long>>()
    private var recentReportActivities = emptyList<Pair<String, Long>>()
    private var recentEventActivities = emptyList<Pair<String, Long>>()

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        startRealtimeDashboard()
        fetchReports()
        fetchAnnouncements()
        fetchManageableContent()
        observeSupabaseEvents()
    }

    private fun observeSupabaseEvents() {
        viewModelScope.launch {
            eventRepository.getEvents().collectLatest { result ->
                result.onSuccess { events ->
                    val recentEvents = events
                        .sortedByDescending { it.createdAt ?: 0L }
                        .take(5)
                        .map { event ->
                            val createdAt = event.createdAt ?: 0L
                            val normalizedCreatedAt = if (createdAt > 100_000_000_000L) createdAt else createdAt * 1000L
                            "New event: ${event.title.ifBlank { "Untitled event" }}" to normalizedCreatedAt
                        }

                    val manageable = events.map { event ->
                        ManageableContent(
                            id = event.id.toString(),
                            title = event.title,
                            subtitle = "${event.location ?: "Campus"} • ${event.createdAt ?: 0L}",
                            type = "EVENT"
                        )
                    }
                    recentEventActivities = recentEvents
                    _uiState.value = _uiState.value.copy(
                        manageableEvents = manageable,
                        totalEvents = events.size,
                        upcomingEvents = events.count { (it.startDate ?: 0L) > System.currentTimeMillis() / 1000 }
                    )
                    publishRecentActivities()
                }
            }
        }
    }

    private fun fetchManageableContent() {
        // Fetch Posts
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val posts = snapshot?.documents?.map { doc ->
                    ManageableContent(
                        id = doc.id,
                        title = doc.getString("text") ?: "No content",
                        subtitle = "by ${doc.getString("authorName") ?: "Unknown"} • ${doc.getLong("createdAt") ?: 0L}",
                        type = "POST"
                    )
                } ?: emptyList()
                _uiState.value = _uiState.value.copy(manageablePosts = posts)
            }

        // Fetch Groups
        firestore.collection("groups")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val groups = snapshot?.documents?.map { doc ->
                    ManageableContent(
                        id = doc.id,
                        title = doc.getString("name") ?: "No name",
                        subtitle = "${doc.get("membersCount") ?: 0} members • ${doc.getString("category") ?: "Active"}",
                        type = "GROUP"
                    )
                } ?: emptyList()
                _uiState.value = _uiState.value.copy(manageableGroups = groups)
            }
    }

    private fun publishRecentActivities() {
        val merged = (recentPostActivities + recentReportActivities + recentEventActivities)
            .distinctBy { it.first }
            .sortedByDescending { it.second }
            .take(10)

        _uiState.value = _uiState.value.copy(recentActivities = merged)
    }

    private fun startRealtimeDashboard() {
        Log.d("AdminViewModel", "Starting realtime dashboard listeners...")
        
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        val startOfWeek = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis

        // Listen to Users
        firestore.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AdminViewModel", "Users listener failed", e)
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                return@addSnapshotListener
            }
            snapshot?.let {
                Log.d("AdminViewModel", "Users updated: ${it.size()} docs")
                val total = it.size()
                val banned = it.documents.count { doc -> doc.getBoolean("isBanned") == true }
                val today = it.documents.count { doc -> (doc.getLong("createdAt") ?: 0L) > startOfToday }
                
                val users = it.documents.map { doc ->
                    AdminUser(
                        uid = doc.id,
                        name = doc.getString("displayName") ?: "Unknown",
                        major = doc.getString("faculty") ?: "Unspecified",
                        role = doc.getString("role") ?: "student",
                        isBanned = doc.getBoolean("isBanned") ?: false,
                        avatarInitial = (doc.getString("displayName") ?: "U").take(1).uppercase()
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    totalUsers = total,
                    bannedUsers = banned,
                    usersToday = today,
                    userList = users,
                    isLoading = false
                )
            }
        }

        // Listen to Groups
        firestore.collection("groups").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AdminViewModel", "Groups listener failed", e)
                return@addSnapshotListener
            }
            snapshot?.let {
                Log.d("AdminViewModel", "Groups updated: ${it.size()} docs")
                val total = it.size()
                val thisWeek = it.documents.count { doc -> (doc.getLong("createdAt") ?: 0L) > startOfWeek }
                _uiState.value = _uiState.value.copy(
                    activeGroups = total,
                    groupsThisWeek = thisWeek
                )
            }
        }

        // Listen to Posts
        firestore.collection("posts").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AdminViewModel", "Posts listener failed", e)
                return@addSnapshotListener
            }
            snapshot?.let {
                Log.d("AdminViewModel", "Posts updated: ${it.size()} docs")
                val today = it.documents.count { doc -> (doc.getLong("createdAt") ?: 0L) > startOfToday }
                recentPostActivities = it.documents.take(5).map { doc ->
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val author = doc.getString("authorName") ?: doc.getString("author") ?: "Unknown"
                    val text = (doc.getString("text") ?: doc.getString("content") ?: "New post").take(40)
                    "Post by $author: $text" to createdAt
                }
                _uiState.value = _uiState.value.copy(postsToday = today)
                publishRecentActivities()
            }
        }

        // Listen to Reports
        firestore.collection("reports").whereEqualTo("status", "open").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AdminViewModel", "Reports listener failed", e)
                return@addSnapshotListener
            }
            snapshot?.let {
                Log.d("AdminViewModel", "Reports updated: ${it.size()} docs")
                _uiState.value = _uiState.value.copy(openReports = it.size())
                recentReportActivities = it.documents.take(5).map { doc ->
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val contentType = doc.getString("contentType") ?: doc.getString("type") ?: "content"
                    val reason = doc.getString("reason") ?: "reported"
                    "Open report: $contentType • $reason" to createdAt
                }
                publishRecentActivities()
            }
        }

        // Listen to Global Activities (using collectionGroup requires an index)
        // Fallback: use newest posts and users as activity for now to avoid index dependency crash
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val activities = snapshot?.documents?.map { doc ->
                    val author = (doc.get("authorName") ?: doc.get("author") ?: "User").toString()
                    val text = (doc.get("text") ?: "New post").toString()
                    "New post by $author: ${text.take(20)}..." to (doc.getLong("createdAt") ?: 0L)
                } ?: emptyList()
                recentPostActivities = activities
                publishRecentActivities()
            }
    }

    private fun fetchReports() {
        firestore.collection("reports")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                viewModelScope.launch {
                    val reports = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        runCatching { loadReportedItem(doc) }.getOrNull()
                    }.filter { it.status.equals("open", ignoreCase = true) }
                        .sortedByDescending { it.reportCreatedAt }

                    _uiState.value = _uiState.value.copy(reportedItems = reports, openReports = reports.size)
                }
            }
    }

    private suspend fun loadReportedItem(doc: DocumentSnapshot): ReportedItem {
        val type = doc.getString("contentType") ?: doc.getString("type") ?: "POST"
        val contentId = (doc.get("contentId") ?: doc.get("postId") ?: doc.id).toString()
        val base = ReportedItem(
            id = doc.id,
            type = type,
            contentId = contentId,
            groupId = doc.getString("groupId") ?: "",
            reportCreatedAt = doc.getLong("createdAt") ?: 0L,
            reason = doc.getString("reason") ?: "No reason",
            reportedBy = doc.getString("reportedByName") ?: doc.getString("reporterName") ?: "Anonymous",
            content = doc.getString("contentPreview") ?: doc.getString("content") ?: "No preview available",
            status = doc.getString("status") ?: "open"
        )

        return when (type.uppercase()) {
            "POST" -> enrichPostReport(base)
            "GROUP_POST", "GROUPPOST" -> enrichGroupPostReport(base)
            "EVENT" -> enrichEventReport(base)
            else -> base
        }
    }

    private suspend fun enrichPostReport(report: ReportedItem): ReportedItem {
        val snapshot = firestore.collection("posts").document(report.contentId).get().await()
        if (!snapshot.exists()) return report
        return report.copy(
            content = snapshot.getString("content") ?: snapshot.getString("text") ?: report.content,
            contentAuthor = snapshot.getString("authorName") ?: snapshot.getString("author") ?: "",
            contentCreatedAt = snapshot.getLong("createdAt") ?: 0L,
            contentUpdatedAt = snapshot.getLong("updatedAt") ?: 0L,
            contentVisibility = snapshot.getString("visibility") ?: "",
            contentLikeCount = (snapshot.getLong("likes") ?: snapshot.getLong("likeCount") ?: 0L).toInt(),
            contentCommentCount = (snapshot.getLong("comments") ?: snapshot.getLong("commentCount") ?: 0L).toInt(),
            contentShareCount = (snapshot.getLong("shares") ?: snapshot.getLong("shareCount") ?: 0L).toInt(),
            contentImageUrl = snapshot.getString("imageUrl") ?: snapshot.getString("mediaUrl") ?: "",
            contentBody = snapshot.getString("body") ?: snapshot.getString("content") ?: snapshot.getString("text") ?: report.content,
        )
    }

    private suspend fun enrichGroupPostReport(report: ReportedItem): ReportedItem {
        val groupId = report.groupId
        if (groupId.isBlank()) return report

        val snapshot = firestore.collection("groups").document(groupId)
            .collection("posts")
            .document(report.contentId)
            .get()
            .await()

        if (!snapshot.exists()) return report

        return report.copy(
            content = snapshot.getString("content") ?: snapshot.getString("text") ?: report.content,
            contentAuthor = snapshot.getString("authorName") ?: snapshot.getString("author") ?: "",
            contentCreatedAt = snapshot.getLong("createdAt") ?: 0L,
            contentUpdatedAt = snapshot.getLong("updatedAt") ?: 0L,
            contentVisibility = snapshot.getString("visibility") ?: "",
            contentLikeCount = (snapshot.getLong("likes") ?: snapshot.getLong("likeCount") ?: 0L).toInt(),
            contentCommentCount = (snapshot.getLong("comments") ?: snapshot.getLong("commentCount") ?: 0L).toInt(),
            contentShareCount = (snapshot.getLong("shares") ?: snapshot.getLong("shareCount") ?: 0L).toInt(),
            contentImageUrl = snapshot.getString("imageUrl") ?: snapshot.getString("mediaUrl") ?: "",
            contentBody = snapshot.getString("body") ?: snapshot.getString("content") ?: snapshot.getString("text") ?: report.content,
        )
    }

    private suspend fun enrichEventReport(report: ReportedItem): ReportedItem {
        val event = eventRepository.getEventById(report.contentId).first().getOrNull() ?: return report
        val createdAt = event.createdAt ?: 0L
        val normalizedCreatedAt = if (createdAt > 100_000_000_000L) createdAt else createdAt * 1000L
        return report.copy(
            content = event.description?.takeIf { it.isNotBlank() } ?: report.content,
            contentAuthor = event.ownerId,
            contentCreatedAt = normalizedCreatedAt,
            contentVisibility = if (event.allowGuest) "Public" else "Restricted",
            contentImageUrl = event.imageUrl.orEmpty(),
            contentBody = event.description.orEmpty(),
        )
    }

    private fun fetchAnnouncements() {
        firestore.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val announcements = snapshot?.documents?.map { doc ->
                    Triple(
                        doc.getString("title") ?: "",
                        doc.getString("message") ?: "",
                        doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                
                _uiState.value = _uiState.value.copy(pastAnnouncements = announcements)
            }
    }

    fun sendAnnouncement(title: String, message: String, audience: String) {
        viewModelScope.launch {
            val announcement = mapOf(
                "title" to title,
                "message" to message,
                "audience" to audience,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("announcements").add(announcement)
        }
    }

    fun banUser(uid: String, isBanned: Boolean) {
        firestore.collection("users").document(uid).update("isBanned", isBanned)
    }

    fun changeUserRole(uid: String, newRole: String) {
        firestore.collection("users").document(uid).update("role", newRole)
    }

    fun dismissReport(reportId: String) {
        viewModelScope.launch {
            val previousState = _uiState.value
            val updatedReports = previousState.reportedItems.filterNot { it.id == reportId }
            _uiState.value = previousState.copy(reportedItems = updatedReports, openReports = updatedReports.size)

            try {
                firestore.collection("reports").document(reportId).update("status", "dismissed").await()
            } catch (e: Exception) {
                _uiState.value = previousState.copy(error = e.message)
            }
        }
    }

    fun deleteReportedContent(reportId: String, contentType: String, contentId: String, groupId: String = "") {
        viewModelScope.launch {
            val previousState = _uiState.value
            val normalizedType = contentType.uppercase()

            if ((normalizedType == "GROUP_POST" || normalizedType == "GROUPPOST") && groupId.isBlank()) {
                _uiState.value = previousState.copy(error = "Missing group id for the reported group post")
                return@launch
            }

            val updatedReports = previousState.reportedItems.filterNot { it.id == reportId }
            _uiState.value = previousState.copy(
                reportedItems = updatedReports,
                openReports = updatedReports.size,
                manageablePosts = if (normalizedType == "POST") previousState.manageablePosts.filterNot { it.id == contentId } else previousState.manageablePosts,
                manageableGroups = if (normalizedType == "GROUP" || normalizedType == "GROUP_POST" || normalizedType == "GROUPPOST") previousState.manageableGroups.filterNot { it.id == contentId } else previousState.manageableGroups,
                manageableEvents = if (normalizedType == "EVENT") previousState.manageableEvents.filterNot { it.id == contentId } else previousState.manageableEvents,
            )

            when (normalizedType) {
                "POST" -> firestore.collection("posts").document(contentId).delete()
                "EVENT" -> eventRepository.deleteEvent(contentId)
                "GROUP" -> firestore.collection("groups").document(contentId).delete()
                "GROUP_POST", "GROUPPOST" -> {
                    firestore.collection("groups").document(groupId)
                        .collection("posts").document(contentId).delete()
                }
                else -> return@launch
            }

            try {
                firestore.collection("reports").document(reportId).update("status", "resolved_deleted").await()
            } catch (e: Exception) {
                _uiState.value = previousState.copy(error = e.message)
            }
        }
    }

    fun restrictContent(reportId: String, contentType: String, contentId: String) {
        viewModelScope.launch {
            val previousState = _uiState.value
            val updatedReports = previousState.reportedItems.filterNot { it.id == reportId }
            _uiState.value = previousState.copy(reportedItems = updatedReports, openReports = updatedReports.size)

            when (contentType.uppercase()) {
                "POST" -> firestore.collection("posts").document(contentId).update("visibility", "private")
                "EVENT" -> {
                    // Supabase events don't have a simple visibility field yet, but we could add one
                    // or just delete for now as per "restrict" intent
                }
                else -> return@launch
            }

            try {
                firestore.collection("reports").document(reportId).update("status", "resolved_restricted").await()
            } catch (e: Exception) {
                _uiState.value = previousState.copy(error = e.message)
            }
        }
    }

    fun deleteContent(type: String, id: String) {
        viewModelScope.launch {
            val previousState = _uiState.value
            val normalizedType = type.uppercase()
            _uiState.value = previousState.copy(
                manageablePosts = if (normalizedType == "POST") previousState.manageablePosts.filterNot { it.id == id } else previousState.manageablePosts,
                manageableGroups = if (normalizedType == "GROUP") previousState.manageableGroups.filterNot { it.id == id } else previousState.manageableGroups,
                manageableEvents = if (normalizedType == "EVENT") previousState.manageableEvents.filterNot { it.id == id } else previousState.manageableEvents,
            )

            val result = when(normalizedType) {
                "POST" -> runCatching { firestore.collection("posts").document(id).delete().await() }
                "GROUP" -> runCatching { firestore.collection("groups").document(id).delete().await() }
                "EVENT" -> eventRepository.deleteEvent(id)
                else -> Result.success(Unit)
            }

            if (result.isFailure) {
                _uiState.value = previousState.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete content")
            }
        }
    }
}

data class ManageableContent(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: String
)
