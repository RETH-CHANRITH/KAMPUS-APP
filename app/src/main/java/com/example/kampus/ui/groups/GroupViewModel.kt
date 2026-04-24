package com.example.kampus.ui.groups

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.GroupRepositoryImpl
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