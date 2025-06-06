// File: app/src/main/java/com/example/teamup/ui/screens/activityManager/SearchActivityViewModel.kt
package com.example.teamup.ui.screens.activityManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val name: String = "",
    val sport: String = "",
    val place: String = "",
    val date: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    // Holds the full filtered list after “Search” is pressed:
    val fullResults: List<ActivityItem> = emptyList(),
    // Holds just the current page (initially empty until first search):
    val visibleResults: List<ActivityItem> = emptyList(),
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val hasMore: Boolean = false
)

class SearchActivityViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun updateFilter(field: String, value: String) = _state.update {
        when (field) {
            "name"  -> it.copy(name = value)
            "sport" -> it.copy(sport = value)
            "place" -> it.copy(place = value)
            "date"  -> it.copy(date = value)
            else    -> it
        }
    }

    /**
     * 1) Fetch all events via getAllEvents()
     * 2) Apply “name/sport/place/date” filters & exclude those you created/joined.
     * 3) Initialize pagination (fullResults, visibleResults = first page, hasMore, currentPage = 1).
     */
    fun search(token: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        try {
            val s = _state.value

            // 1) Fetch every event from backend
            val allEvents: List<ActivityItem> = repo.getAllEvents(token)

            // 2) Apply each filter locally (case-insensitive contains)
            val filtered = allEvents.filter { item ->
                // a) “name” filter checks item.title (“<eventName> : <sport>”)
                val matchesName = s.name.isBlank() ||
                        item.title.contains(s.name, ignoreCase = true)

                // b) “sport” filter checks substring after “:”
                val matchesSport = s.sport.isBlank() ||
                        item.title.substringAfter(":", "")
                            .contains(s.sport, ignoreCase = true)

                // c) “place” filter checks item.location
                val matchesPlace = s.place.isBlank() ||
                        item.location.contains(s.place, ignoreCase = true)

                // d) “date” filter checks item.startsAt prefix
                val matchesDate = s.date.isBlank() ||
                        item.startsAt.startsWith(s.date)

                // e) Exclude any event where you’re creator or a participant
                val notAlreadyInvolved = !item.isCreator && !item.isParticipant

                matchesName && matchesSport && matchesPlace && matchesDate && notAlreadyInvolved
            }

            // 3) Initialize pagination:
            val firstPage = filtered.take(_state.value.pageSize)
            val moreExists = filtered.size > _state.value.pageSize

            _state.update {
                it.copy(
                    loading = false,
                    fullResults = filtered,
                    visibleResults = firstPage,
                    currentPage = 1,
                    hasMore = moreExists,
                    error = null
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.localizedMessage) }
        }
    }

    /**
     * Called when “Load more” is pressed.
     * Increments currentPage, takes the next slice, updates visibleResults & hasMore.
     */
    fun loadMore() {
        val ui = _state.value
        if (!ui.hasMore) return

        val nextPage = ui.currentPage + 1
        val toIndex  = (nextPage * ui.pageSize).coerceAtMost(ui.fullResults.size)
        val newSlice = ui.fullResults.take(toIndex)

        _state.update {
            it.copy(
                visibleResults = newSlice,
                currentPage = nextPage,
                hasMore = ui.fullResults.size > toIndex
            )
        }
    }
}
