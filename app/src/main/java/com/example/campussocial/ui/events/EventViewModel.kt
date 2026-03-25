package com.example.kampus.ui.events

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─────────────────────────────────────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────────────────────────────────────
data class EventUiState(
    val events          : List<EventItem> = emptyList(),
    val interestedIds   : Set<Int>        = emptySet(),
    val likedIds        : Set<Int>        = emptySet(),
    val savedIds        : Set<Int>        = emptySet(),
    val activeFilter    : String          = "All",
    val searchQuery     : String          = "",
    val isLoading       : Boolean         = false,
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────
class EventViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    val filters = listOf("All", "Today", "This Week", "Music", "Tech", "Art", "Campus")

    init { loadEvents() }

    private fun loadEvents() {
        _uiState.update {
            it.copy(
                events = listOf(
                    EventItem(
                        id             = 1,
                        title          = "Summer Music Festival 2026",
                        category       = EventCategory.MUSIC,
                        date           = "March 28, 2026",
                        time           = "6:00 PM – 11:00 PM",
                        location       = "Central Park, New York",
                        interested     = 245,
                        likes          = 127,
                        comments       = 34,
                        shares         = 12,
                        coverEmoji     = "🎸",
                        coverColor1    = Color(0xFF1A0A2E),
                        coverColor2    = Color(0xFF4A1060),
                        description    = "The biggest outdoor music festival of the year featuring top artists, food stalls, and an unforgettable night under the stars.",
                        organizer      = "Sarah Johnson",
                        organizerEmoji = "👩‍🎤",
                        organizerTime  = "2h ago",
                        isFeatured     = true,
                    ),
                    EventItem(
                        id             = 2,
                        title          = "Global Tech Summit 2026",
                        category       = EventCategory.TECH,
                        date           = "April 5, 2026",
                        time           = "9:00 AM – 6:00 PM",
                        location       = "Convention Center",
                        interested     = 1200,
                        likes          = 342,
                        comments       = 89,
                        shares         = 45,
                        coverEmoji     = "🚀",
                        coverColor1    = Color(0xFF0D1F3C),
                        coverColor2    = Color(0xFF0F3460),
                        description    = "Join industry leaders, innovators, and entrepreneurs for a full-day summit covering AI, Web3, and the future of campus technology.",
                        organizer      = "Mike Chen",
                        organizerEmoji = "👨‍💻",
                        organizerTime  = "5h ago",
                        isFeatured     = true,
                    ),
                    EventItem(
                        id             = 3,
                        title          = "Art Gallery Opening Night",
                        category       = EventCategory.ART,
                        date           = "March 25, 2026",
                        time           = "7:00 PM – 10:00 PM",
                        location       = "Modern Art Museum",
                        interested     = 87,
                        likes          = 89,
                        comments       = 21,
                        shares         = 8,
                        coverEmoji     = "🖼️",
                        coverColor1    = Color(0xFF1A1000),
                        coverColor2    = Color(0xFF3D2800),
                        description    = "An exclusive opening night featuring works from 20 emerging campus artists. Wine, canapes, and live acoustic music included.",
                        organizer      = "Emma Davis",
                        organizerEmoji = "👩‍🎨",
                        organizerTime  = "1d ago",
                        isFeatured     = false,
                    ),
                    EventItem(
                        id             = 4,
                        title          = "Campus Hackathon 2026",
                        category       = EventCategory.CAMPUS,
                        date           = "April 12, 2026",
                        time           = "8:00 AM – 8:00 PM",
                        location       = "Engineering Building, Hall A",
                        interested     = 320,
                        likes          = 198,
                        comments       = 56,
                        shares         = 29,
                        coverEmoji     = "⚡",
                        coverColor1    = Color(0xFF0D2B18),
                        coverColor2    = Color(0xFF0F4C2A),
                        description    = "48-hour hackathon open to all students. Build something amazing, win prizes, and connect with recruiters from top tech companies.",
                        organizer      = "Campus Dev Hub",
                        organizerEmoji = "🎓",
                        organizerTime  = "3h ago",
                        isFeatured     = false,
                    ),
                    EventItem(
                        id             = 5,
                        title          = "Jazz Night at the Quad",
                        category       = EventCategory.MUSIC,
                        date           = "March 30, 2026",
                        time           = "8:00 PM – 11:30 PM",
                        location       = "Campus Quad, Open Air",
                        interested     = 156,
                        likes          = 74,
                        comments       = 18,
                        shares         = 6,
                        coverEmoji     = "🎷",
                        coverColor1    = Color(0xFF1A0D00),
                        coverColor2    = Color(0xFF3D2000),
                        description    = "An intimate evening of live jazz under the stars. Bring a blanket, your friends, and enjoy the music.",
                        organizer      = "Music Society",
                        organizerEmoji = "🎵",
                        organizerTime  = "6h ago",
                        isFeatured     = false,
                    ),
                    EventItem(
                        id             = 6,
                        title          = "Startup Pitch Competition",
                        category       = EventCategory.BUSINESS,
                        date           = "April 18, 2026",
                        time           = "2:00 PM – 7:00 PM",
                        location       = "Business School Auditorium",
                        interested     = 430,
                        likes          = 211,
                        comments       = 67,
                        shares         = 38,
                        coverEmoji     = "💡",
                        coverColor1    = Color(0xFF001A2B),
                        coverColor2    = Color(0xFF003D5C),
                        description    = "Present your startup idea to a panel of VCs and angel investors. Cash prizes and mentorship opportunities for top 3 teams.",
                        organizer      = "Startup Club",
                        organizerEmoji = "👔",
                        organizerTime  = "12h ago",
                        isFeatured     = false,
                    ),
                ),
                interestedIds = emptySet(),
                likedIds      = emptySet(),
                savedIds      = emptySet(),
            )
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun toggleInterested(id: Int) {
        _uiState.update { s ->
            s.copy(
                interestedIds = if (id in s.interestedIds) s.interestedIds - id else s.interestedIds + id
            )
        }
    }

    fun toggleLike(id: Int) {
        _uiState.update { s ->
            s.copy(
                likedIds = if (id in s.likedIds) s.likedIds - id else s.likedIds + id
            )
        }
    }

    fun toggleSave(id: Int) {
        _uiState.update { s ->
            s.copy(
                savedIds = if (id in s.savedIds) s.savedIds - id else s.savedIds + id
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // ── Add Event (from CreateEventScreen) ───────────────────────────────────
    fun addEvent(data: NewEventData) {
        val newId = (_uiState.value.events.maxOfOrNull { it.id } ?: 0) + 1
        val newEvent = EventItem(
            id             = newId,
            title          = data.title,
            category       = data.category,
            date           = data.date,
            time           = data.time,
            location       = data.location,
            interested     = 0,
            likes          = 0,
            comments       = 0,
            shares         = 0,
            coverEmoji     = data.coverEmoji,
            coverColor1    = data.category.color.copy(alpha = 0.25f)
                .compositeOver(Color(0xFF080B11)),
            coverColor2    = data.category.color.copy(alpha = 0.08f)
                .compositeOver(Color(0xFF080B11)),
            description    = data.description.ifBlank { "No description provided." },
            organizer      = "You",
            organizerEmoji = "🙋",
            organizerTime  = "Just now",
            isFeatured     = false,
        )
        _uiState.update { s ->
            s.copy(events = listOf(newEvent) + s.events)
        }
    }

    // ── Computed ──────────────────────────────────────────────────────────────
    fun filteredEvents(filter: String, query: String): List<EventItem> {
        val base = _uiState.value.events
        val byFilter = when (filter) {
            "Music"     -> base.filter { it.category == EventCategory.MUSIC }
            "Tech"      -> base.filter { it.category == EventCategory.TECH }
            "Art"       -> base.filter { it.category == EventCategory.ART }
            "Campus"    -> base.filter { it.category == EventCategory.CAMPUS }
            "Today"     -> base.filter { it.date.contains("March 25") }
            "This Week" -> base.filter { it.date.contains("March") }
            else        -> base
        }
        return if (query.isBlank()) byFilter
        else byFilter.filter {
            it.title.contains(query, true) ||
                    it.location.contains(query, true) ||
                    it.category.label.contains(query, true)
        }
    }
}