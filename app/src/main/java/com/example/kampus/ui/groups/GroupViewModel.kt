package com.example.kampus.ui.groups

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    init { loadGroups() }

    // ── Loaders ───────────────────────────────────────────────────────────────
    private fun loadGroups() {
        _uiState.update {
            it.copy(
                myGroups = listOf(
                    GroupData(
                        id          = 1,
                        name        = "Design Inspiration",
                        category    = "Art & Design",
                        coverColor1 = Color(0xFF1E1040),
                        coverColor2 = Color(0xFF2D1B6E),
                        coverEmoji  = "🎨",
                        members     = "1.2K",
                        posts       = "234",
                        isJoined    = true,
                        description = "A community for designers to share inspiration, get feedback, and connect with fellow creatives from around the world.",
                    ),
                    GroupData(
                        id          = 2,
                        name        = "3D Artists Community",
                        category    = "3D & Graphics",
                        coverColor1 = Color(0xFF0D1F3C),
                        coverColor2 = Color(0xFF0F3460),
                        coverEmoji  = "🌀",
                        members     = "854",
                        posts       = "156",
                        isJoined    = true,
                        description = "For 3D artists who love to create, share, and collaborate on stunning visual projects.",
                    ),
                    GroupData(
                        id          = 3,
                        name        = "Campus Dev Hub",
                        category    = "Technology",
                        coverColor1 = Color(0xFF0D2B18),
                        coverColor2 = Color(0xFF0F4C2A),
                        coverEmoji  = "💻",
                        members     = "2.1K",
                        posts       = "412",
                        isJoined    = true,
                        description = "Where campus developers meet, code, and build amazing things together.",
                    ),
                ),
                discoverGroups = listOf(
                    GroupData(
                        id          = 4,
                        name        = "Photography Lovers",
                        category    = "Photography",
                        coverColor1 = Color(0xFF1A0D2B),
                        coverColor2 = Color(0xFF3D1A5C),
                        coverEmoji  = "📷",
                        members     = "2.5K",
                        posts       = "489",
                        isJoined    = false,
                        description = "Share your best shots and learn photography techniques from the community.",
                    ),
                    GroupData(
                        id          = 5,
                        name        = "UI/UX Designers",
                        category    = "Design",
                        coverColor1 = Color(0xFF0D1A2B),
                        coverColor2 = Color(0xFF0F2D50),
                        coverEmoji  = "✏️",
                        members     = "3.1K",
                        posts       = "672",
                        isJoined    = false,
                        description = "A space for UI/UX professionals to share work, resources, and tips.",
                    ),
                    GroupData(
                        id          = 6,
                        name        = "Music & Arts",
                        category    = "Entertainment",
                        coverColor1 = Color(0xFF2B1400),
                        coverColor2 = Color(0xFF5C2E00),
                        coverEmoji  = "🎵",
                        members     = "1.8K",
                        posts       = "321",
                        isJoined    = false,
                        description = "Music lovers sharing melodies, art, and creative expression.",
                    ),
                    GroupData(
                        id          = 7,
                        name        = "Startup Founders",
                        category    = "Business",
                        coverColor1 = Color(0xFF1A2B0D),
                        coverColor2 = Color(0xFF2E4A14),
                        coverEmoji  = "🚀",
                        members     = "4.3K",
                        posts       = "891",
                        isJoined    = false,
                        description = "Connect with entrepreneurs, share ideas, and grow your startup together.",
                    ),
                    GroupData(
                        id          = 8,
                        name        = "Anime & Manga",
                        category    = "Entertainment",
                        coverColor1 = Color(0xFF2B0D1A),
                        coverColor2 = Color(0xFF5C1A3D),
                        coverEmoji  = "🌸",
                        members     = "5.7K",
                        posts       = "1.2K",
                        isJoined    = false,
                        description = "The go-to community for anime lovers and manga readers on campus.",
                    ),
                ),
                joinedIds = setOf(1, 2, 3),
            )
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