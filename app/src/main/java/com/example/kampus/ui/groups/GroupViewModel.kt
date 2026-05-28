package com.example.kampus.ui.groups

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.GroupRepositoryImpl
import com.example.kampus.utils.ActivityLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────────────────────────────────────
data class GroupUiState(
    val myGroups       : List<GroupData> = emptyList(),
    val discoverGroups : List<GroupData> = emptyList(),
    val joinedIds      : Set<Int>        = emptySet(),
    val requestedIds   : Set<Int>        = emptySet(),
    val likedPostIds   : Set<Int>        = emptySet(),
    val selectedTab    : Int             = 0,
    val searchQuery    : String          = "",
    val isLoading      : Boolean         = false,
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────
class GroupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private val groupRepository = GroupRepositoryImpl(FirebaseFirestore.getInstance())

    init { loadGroups() }

    // ── Loaders ───────────────────────────────────────────────────────────────
    private fun loadGroups() {
        viewModelScope.launch {
            // Load my groups
            groupRepository.getMyGroups().collect { result ->
                result.onSuccess { myGroups ->
                    _uiState.update { it.copy(myGroups = myGroups) }
                }
                result.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            // Load discover groups
            groupRepository.getDiscoverGroups().collect { result ->
                result.onSuccess { discoverGroups ->
                    _uiState.update { it.copy(discoverGroups = discoverGroups) }
                }
                result.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    sealed class JoinActionResult {
        data object Joined : JoinActionResult()
        data object Requested : JoinActionResult()
        data object AlreadyRequested : JoinActionResult()
        data object NotAuthenticated : JoinActionResult()
        data class Failed(val message: String) : JoinActionResult()
    }

    fun toggleJoin(groupId: Int) {
        _uiState.update { state ->
            state.copy(
                joinedIds = if (groupId in state.joinedIds)
                    state.joinedIds - groupId
                else
                    state.joinedIds + groupId,
            )
        }
    }

    suspend fun handleJoinAction(group: GroupData): JoinActionResult {
        if (group.privacy.equals("private", ignoreCase = true)) {
            if (group.id in _uiState.value.requestedIds || group.id in _uiState.value.joinedIds) {
                return JoinActionResult.AlreadyRequested
            }

            val currentUser = FirebaseAuth.getInstance().currentUser ?: return JoinActionResult.NotAuthenticated
            val requesterId = currentUser.uid
            if (requesterId.isBlank()) return JoinActionResult.NotAuthenticated

            val requesterName = currentUser.displayName
                ?: currentUser.email
                ?: "Student"

            val result = groupRepository.requestJoinGroup(
                groupId = group.id.toString(),
                request = GroupJoinRequest(
                    requesterId = requesterId,
                    requesterName = requesterName,
                    requestedAt = System.currentTimeMillis(),
                )
            )

            return result
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(requestedIds = state.requestedIds + group.id)
                    }
                }
                .fold(
                    onSuccess = { JoinActionResult.Requested },
                    onFailure = { JoinActionResult.Failed(it.message ?: "Could not send join request") }
                )
        }

        toggleJoin(group.id)
        return JoinActionResult.Joined
    }

    fun toggleLike(postId: Int) {
        _uiState.update { state ->
            state.copy(
                likedPostIds = if (postId in state.likedPostIds)
                    state.likedPostIds - postId
                else
                    state.likedPostIds + postId,
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    suspend fun createGroup(groupData: GroupData): Result<String> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val groupPayload = mapOf(
            "id" to groupData.id,
            "name" to groupData.name,
            "category" to groupData.category,
            "coverColor1" to groupData.coverColor1.value.toLong(),
            "coverColor2" to groupData.coverColor2.value.toLong(),
            "coverEmoji" to groupData.coverEmoji,
            "description" to groupData.description,
            "members" to groupData.members,
            "posts" to groupData.posts,
            "isJoined" to false,
            "privacy" to groupData.privacy.lowercase(),
            "createdAt" to System.currentTimeMillis(),
            "ownerId" to currentUserId,
        )

        val result = groupRepository.createGroup(groupPayload)
        result.onSuccess { groupId ->
            ActivityLogger.logAction(
                type = "create_group",
                text = "Created group: ${groupData.name}",
                metadata = mapOf("groupId" to groupId),
            )
            _uiState.update { state ->
                state.copy(myGroups = listOf(groupData) + state.myGroups)
            }
        }
        return result
    }

    // ── Computed helpers ──────────────────────────────────────────────────────
    fun filteredGroups(tab: Int, query: String): List<GroupData> {
        val base = if (tab == 0) _uiState.value.myGroups else _uiState.value.discoverGroups
        return if (query.isBlank()) base
        else base.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
        }
    }
}