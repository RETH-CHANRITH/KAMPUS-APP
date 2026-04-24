package com.example.kampus.ui.events

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kampus.data.repository.EventRepositoryImpl
import com.example.kampus.utils.ActivityLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val eventRepository = EventRepositoryImpl(FirebaseFirestore.getInstance())

    init { loadEvents() }

    private fun loadEvents() {
        viewModelScope.launch {
            eventRepository.getEvents().collect { result ->
                result.onSuccess { events ->
                    _uiState.update { it.copy(events = events, isLoading = false) }
                }
                result.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun toggleInterested(id: Int) {
        val becameInterested = id !in _uiState.value.interestedIds
        _uiState.update { s ->
            s.copy(
                interestedIds = if (id in s.interestedIds) s.interestedIds - id else s.interestedIds + id
            )
        }

        if (becameInterested) {
            ActivityLogger.logAction(
                type = "interested_event",
                text = "Interested in an event",
                metadata = mapOf("eventId" to id),
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

        ActivityLogger.logAction(
            type = "create_event",
            text = "Created event: ${data.title.ifBlank { "Untitled" }}",
            metadata = mapOf("eventId" to newId),
        )
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