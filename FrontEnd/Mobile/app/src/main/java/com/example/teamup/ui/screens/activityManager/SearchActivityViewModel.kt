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
            "name" -> it.copy(name = value)
            "sport" -> it.copy(sport = value)
            "place" -> it.copy(place = value)
            "date" -> it.copy(date = value)
            else -> it
        }
    }

    fun search(token: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val s = _state.value
            // Call the repository; it will add "Bearer $token" internally.
            val allResults: List<ActivityItem> = repo.searchActivities(token,
                name  = s.name.takeIf { it.isNotBlank() },
                sport = s.sport.takeIf { it.isNotBlank() },
                place = s.place.takeIf { it.isNotBlank() },
                date  = s.date.takeIf { it.isNotBlank() }
            )
            // Filter out anything you created or already joined
            val filtered = allResults.filter { !it.isCreator && !it.isParticipant }
            _state.update { it.copy(loading = false, results = filtered) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }
}
