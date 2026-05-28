package com.example.kampus.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.model.Group
import com.example.kampus.data.model.GroupMember
import com.example.kampus.data.model.GroupPost
import com.example.kampus.data.model.GroupPrivacy
import com.example.kampus.data.model.JoinRequest
import com.example.kampus.data.model.MemberRole
import com.example.kampus.data.model.MembershipStatus
import com.example.kampus.data.model.PostReportReason
import com.example.kampus.data.repository.GroupsRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State models ───────────────────────────────────────────────────────────

data class GroupsUiState(
    val myGroups: List<Group> = emptyList(),
    val discoverGroups: List<Group> = emptyList(),
    val pendingGroupIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class GroupDetailUiState(
    val group: Group? = null,
    val posts: List<GroupPost> = emptyList(),
    val membershipStatus: MembershipStatus = MembershipStatus.NONE,
    val currentUserRole: MemberRole = MemberRole.MEMBER,
    val isLoading: Boolean = false,
    val postSubmitted: Boolean = false,
)

data class AdminPanelUiState(
    val pendingRequests: List<JoinRequest> = emptyList(),
    val reportedPosts: List<GroupPost> = emptyList(),
    val members: List<GroupMember> = emptyList(),
    val isLoading: Boolean = false,
    val actionSuccess: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class GroupsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = GroupsRepositoryImpl(FirebaseFirestore.getInstance())

    private val currentUserId: String get() = auth.currentUser?.uid.orEmpty()

    // ── State flows ───────────────────────────────────────────────────────────

    private val _groupsUiState = MutableStateFlow(GroupsUiState(isLoading = true))
    val groupsUiState: StateFlow<GroupsUiState> = _groupsUiState.asStateFlow()

    private val _groupDetailUiState = MutableStateFlow(GroupDetailUiState(isLoading = true))
    val groupDetailUiState: StateFlow<GroupDetailUiState> = _groupDetailUiState.asStateFlow()

    private val _adminPanelUiState = MutableStateFlow(AdminPanelUiState(isLoading = true))
    val adminPanelUiState: StateFlow<AdminPanelUiState> = _adminPanelUiState.asStateFlow()

    // ── Active listeners (cancelled when navigating away) ─────────────────────

    private var groupsListJob: Job? = null
    private var groupDetailJob: Job? = null
    private var postsJob: Job? = null
    private var membersJob: Job? = null
    private var joinRequestsJob: Job? = null

    // ── Search ────────────────────────────────────────────────────────────────

    private val searchQuery = MutableStateFlow("")

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadGroups()
    }

    // ── Groups list ───────────────────────────────────────────────────────────

    fun loadGroups() {
        groupsListJob?.cancel()
        groupsListJob = viewModelScope.launch {
            _groupsUiState.update { it.copy(isLoading = true) }

            // Combine all-groups + current user memberships + pending requests (real-time)
            combine(
                repository.observeGroups(),
                repository.observeMyMemberGroupIds(),
                repository.observeMyPendingGroupIds(),
                searchQuery,
            ) { groupsResult, memberIdsResult, pendingIdsResult, query ->

                val groups = groupsResult.getOrElse {
                    Log.e("GroupsViewModel", "Failed to observe groups", it)
                    emptyList()
                }
                val memberIds = memberIdsResult.getOrElse {
                    Log.e("GroupsViewModel", "Failed to observe my member group IDs", it)
                    emptySet()
                }
                val pendingIds = pendingIdsResult.getOrElse {
                    Log.e("GroupsViewModel", "Failed to observe my pending group IDs", it)
                    emptySet()
                }

                val filtered = if (query.isBlank()) groups
                else groups.filter { g ->
                    listOf(g.name, g.category, g.description)
                        .any { it.contains(query, ignoreCase = true) }
                }

                val myGroups = filtered.filter { it.id in memberIds }
                val discoverGroups = filtered.filter { it.id !in memberIds }

                GroupsUiState(
                    myGroups = myGroups,
                    discoverGroups = discoverGroups,
                    pendingGroupIds = pendingIds,
                    searchQuery = query,
                    isLoading = false,
                    error = groupsResult.exceptionOrNull()?.message,
                )
            }.collect { state ->
                _groupsUiState.value = state
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    // ── Group detail ──────────────────────────────────────────────────────────

    fun loadGroupDetail(groupId: String) {
        // Cancel previous detail listeners
        groupDetailJob?.cancel()
        postsJob?.cancel()

        _groupDetailUiState.update { it.copy(isLoading = true, group = null, posts = emptyList()) }

        // Observe group metadata + membership + current user role
        groupDetailJob = viewModelScope.launch {
            combine(
                repository.observeGroup(groupId),
                repository.observeMyMemberGroupIds(),
                repository.observeMyPendingGroupIds(),
            ) { groupResult, memberIdsResult, pendingIdsResult ->
                val group = groupResult.getOrElse {
                    Log.e("GroupsViewModel", "Failed to observe group detail $groupId", it)
                    null
                }
                val memberIds = memberIdsResult.getOrElse {
                    Log.e("GroupsViewModel", "Failed to observe my member group IDs for detail", it)
                    emptySet()
                }
                val pendingIds = pendingIdsResult.getOrElse {
                    Log.e("GroupsViewModel", "Failed to observe my pending group IDs for detail", it)
                    emptySet()
                }

                val membershipStatus = when {
                    groupId in memberIds -> MembershipStatus.MEMBER
                    groupId in pendingIds -> MembershipStatus.PENDING
                    else -> MembershipStatus.NONE
                }

                val currentUserRole = if (group?.adminId == currentUserId) MemberRole.ADMIN
                else MemberRole.MEMBER

                Triple(group, membershipStatus, currentUserRole)
            }.collect { (group, status, role) ->
                _groupDetailUiState.update {
                    it.copy(
                        group = group,
                        membershipStatus = status,
                        currentUserRole = role,
                        isLoading = false,
                    )
                }
            }
        }

        // Observe posts separately
        postsJob = viewModelScope.launch {
            repository.observePosts(groupId).collect { result ->
                _groupDetailUiState.update {
                    it.copy(posts = result.getOrDefault(emptyList()))
                }
            }
        }
    }

    // ── Admin panel ───────────────────────────────────────────────────────────

    fun loadAdminPanel(groupId: String) {
        membersJob?.cancel()
        joinRequestsJob?.cancel()

        _adminPanelUiState.update { it.copy(isLoading = true) }

        membersJob = viewModelScope.launch {
            repository.observeMembers(groupId).collect { result ->
                val members = result.getOrDefault(emptyList())
                    .sortedWith(
                        compareByDescending<GroupMember> { it.role == MemberRole.ADMIN }
                            .thenBy { it.userName }
                    )
                _adminPanelUiState.update { it.copy(members = members, isLoading = false) }

                // Also update reported posts from current detail state
                val reportedPosts = _groupDetailUiState.value.posts
                    .filter { it.isReported || it.reportCount > 0 }
                _adminPanelUiState.update { it.copy(reportedPosts = reportedPosts) }
            }
        }

        joinRequestsJob = viewModelScope.launch {
            repository.observeJoinRequestsNew(groupId).collect { result ->
                _adminPanelUiState.update {
                    it.copy(pendingRequests = result.getOrDefault(emptyList()))
                }
            }
        }

        // Also keep reported posts in sync with detail posts
        viewModelScope.launch {
            repository.observePosts(groupId).collect { result ->
                val reportedPosts = result.getOrDefault(emptyList())
                    .filter { it.isReported || it.reportCount > 0 }
                _adminPanelUiState.update { it.copy(reportedPosts = reportedPosts) }
            }
        }
    }

    fun consumeAdminActionSuccess() {
        _adminPanelUiState.update { it.copy(actionSuccess = null) }
    }

    // ── Join / Leave ──────────────────────────────────────────────────────────

    fun joinOrRequestGroup(groupId: String) {
        viewModelScope.launch {
            val group = _groupsUiState.value.myGroups.firstOrNull { it.id == groupId }
                ?: _groupsUiState.value.discoverGroups.firstOrNull { it.id == groupId }
                ?: _groupDetailUiState.value.group
                ?: return@launch

            when (group.privacy) {
                GroupPrivacy.PUBLIC -> repository.joinPublicGroup(groupId)
                GroupPrivacy.PRIVATE -> repository.requestJoinPrivateGroup(groupId)
            }
            // State updates automatically via real-time listeners
        }
    }

    fun cancelJoinRequest(groupId: String) {
        viewModelScope.launch {
            repository.cancelJoinRequest(groupId)
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            val group = _groupDetailUiState.value.group ?: return@launch
            if (group.adminId == currentUserId) return@launch // Admin can't leave
            repository.leaveGroup(groupId)
        }
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    fun createPost(groupId: String, content: String, imageUrl: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val result = repository.createPost(groupId, content, imageUrl ?: "")
            if (result.isSuccess) {
                _groupDetailUiState.update { it.copy(postSubmitted = true) }
                // Reset flag after short delay
                kotlinx.coroutines.delay(200)
                _groupDetailUiState.update { it.copy(postSubmitted = false) }
            }
        }
    }

    fun toggleLikePost(groupId: String, postId: String) {
        viewModelScope.launch {
            repository.toggleLikePost(groupId, postId)
        }
    }

    fun reportPost(groupId: String, postId: String, reason: PostReportReason, note: String) {
        viewModelScope.launch {
            repository.reportPost(groupId, postId, reason, note)
        }
    }

    // ── Admin actions ─────────────────────────────────────────────────────────

    fun deletePost(groupId: String, postId: String) {
        viewModelScope.launch {
            val result = repository.deletePost(groupId, postId)
            if (result.isSuccess) {
                _adminPanelUiState.update { it.copy(actionSuccess = "Post deleted.") }
            }
        }
    }

    fun removeMemberFromGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            val group = _groupDetailUiState.value.group ?: return@launch
            if (group.adminId == userId) return@launch // Cannot remove admin
            val result = repository.removeMember(groupId, userId)
            if (result.isSuccess) {
                _adminPanelUiState.update { it.copy(actionSuccess = "Member removed.") }
            }
        }
    }

    fun approveJoinRequest(groupId: String, request: JoinRequest) {
        viewModelScope.launch {
            val result = repository.approveJoinRequest(groupId, request.userId)
            if (result.isSuccess) {
                _adminPanelUiState.update { it.copy(actionSuccess = "Request approved.") }
            }
        }
    }

    fun rejectJoinRequest(groupId: String, request: JoinRequest) {
        viewModelScope.launch {
            val result = repository.rejectJoinRequest(groupId, request.userId)
            if (result.isSuccess) {
                _adminPanelUiState.update { it.copy(actionSuccess = "Request rejected.") }
            }
        }
    }

    fun createGroup(
        name: String,
        category: String,
        description: String,
        privacy: GroupPrivacy,
        coverImageUrl: String? = null,
    ) {
        viewModelScope.launch {
            repository.createGroup(
                name = name,
                category = category,
                description = description,
                privacy = privacy,
                coverImageUrl = coverImageUrl ?: "",
            )
            // Groups list updates automatically via real-time listener
        }
    }

    // ── Helpers for UI ────────────────────────────────────────────────────────

    /** Returns current membership status for a group (from live state). */
    fun getMembershipStatus(groupId: String): MembershipStatus {
        return when {
            _groupsUiState.value.myGroups.any { it.id == groupId } -> MembershipStatus.MEMBER
            _groupsUiState.value.pendingGroupIds.contains(groupId) -> MembershipStatus.PENDING
            else -> MembershipStatus.NONE
        }
    }

    /** Returns current user's role in a group. */
    fun getCurrentUserRole(groupId: String): MemberRole {
        val group = _groupsUiState.value.myGroups.firstOrNull { it.id == groupId }
            ?: _groupsUiState.value.discoverGroups.firstOrNull { it.id == groupId }
        return if (group?.adminId == currentUserId) MemberRole.ADMIN else MemberRole.MEMBER
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        groupsListJob?.cancel()
        groupDetailJob?.cancel()
        postsJob?.cancel()
        membersJob?.cancel()
        joinRequestsJob?.cancel()
    }
}