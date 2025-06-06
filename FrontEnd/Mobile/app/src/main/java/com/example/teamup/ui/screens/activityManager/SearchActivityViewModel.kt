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
    val results: List<ActivityItem> = emptyList()
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
     * Instead of calling `/events/search`, fetch all events via getAllEvents(),
     * then apply “name/sport/place/date” filters & exclude those you created/joined.
     */
    fun search(token: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        try {
            val s = _state.value
            // 1) Fetch every event (non-concluded) from backend
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

            // 3) Push results back into state
            _state.update { it.copy(loading = false, results = filtered) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }
}
