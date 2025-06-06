// File: app/src/main/java/com/example/teamup/ui/screens/activityManager/SearchActivityViewModel.kt
package com.example.teamup.ui.screens.activityManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * We keep each piece of UI state as its own StateFlow, instead of smashing them all into one big combine.
 */
class SearchActivityViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    // 1) Four filter fields:
    private val _name  = MutableStateFlow("")
    private val _sport = MutableStateFlow("")
    private val _place = MutableStateFlow("")
    private val _date  = MutableStateFlow("")

    // 2) “all events” loaded once from the repo:
    private val _allEvents = MutableStateFlow<List<ActivityItem>>(emptyList())

    // 3) loading / error for the initial load:
    private val _loading = MutableStateFlow(false)
    private val _error   = MutableStateFlow<String?>(null)

    // 4) pagination internals:
    private val _currentPage    = MutableStateFlow(1)
    private val _visibleResults = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _hasMore        = MutableStateFlow(false)

    // ─────────────────────────────────────────────────────────────────────────────
    // 5) Combine just the five flows (_name, _sport, _place, _date, _allEvents) into “_filtered”
    //    Whenever any of those change, re‐compute the filtered list.
    // ─────────────────────────────────────────────────────────────────────────────
    private val _filtered: StateFlow<List<ActivityItem>> = combine(
        _name,
        _sport,
        _place,
        _date,
        _allEvents
    ) { name: String,
        sport: String,
        place: String,
        date: String,
        allEvents: List<ActivityItem> ->

        allEvents.filter { item ->
            val matchesName  = name.isBlank()  || item.title.contains(name, ignoreCase = true)
            val matchesSport = sport.isBlank() || item.title.substringAfter(":", "").contains(sport, ignoreCase = true)
            val matchesPlace = place.isBlank() || item.location.contains(place, ignoreCase = true)
            val matchesDate  = date.isBlank()  || item.startsAt.startsWith(date)
            val notInvolved  = !item.isCreator && !item.isParticipant

            matchesName && matchesSport && matchesPlace && matchesDate && notInvolved
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    // ─────────────────────────────────────────────────────────────────────────────
    // 6) Whenever “_filtered” emits a new list, reset pagination to page 1.
    // ─────────────────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            _filtered.collect { newFiltered ->
                _currentPage.value = 1
                val pageSize = 10
                _visibleResults.value = newFiltered.take(pageSize)
                _hasMore.value = newFiltered.size > pageSize
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 7) Expose each flow separately so the Composable can call collectAsState() on each.
    // ─────────────────────────────────────────────────────────────────────────────
    val name: StateFlow<String>                   = _name
    val sport: StateFlow<String>                  = _sport
    val place: StateFlow<String>                  = _place
    val date: StateFlow<String>                   = _date
    val loading: StateFlow<Boolean>               = _loading
    val error: StateFlow<String?>                 = _error
    val allEvents: StateFlow<List<ActivityItem>>  = _allEvents
    val filtered: StateFlow<List<ActivityItem>>   = _filtered
    val visibleResults: StateFlow<List<ActivityItem>> = _visibleResults
    val currentPage: StateFlow<Int>               = _currentPage
    val hasMore: StateFlow<Boolean>               = _hasMore

    // ─────────────────────────────────────────────────────────────────────────────
    // 8) Called once (or whenever you want to “refresh”) to fetch all events from backend.
    // ─────────────────────────────────────────────────────────────────────────────
    fun loadAllEvents(token: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val events = repo.getAllEvents(token)
                // (If you need to set item.isCreator/isParticipant, do it here)
                _allEvents.value = events
                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                _error.value = e.localizedMessage ?: "Unknown error"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 9) Called from the Composable whenever any filter field changes.
    //    Because of our “combine” above, changing _name/_sport/_place/_date
    //    automatically re‐computes “_filtered” and resets pagination (via init { … }).
    // ─────────────────────────────────────────────────────────────────────────────
    fun updateFilter(field: String, value: String) {
        when (field) {
            "name"  -> _name.value = value
            "sport" -> _sport.value = value
            "place" -> _place.value = value
            "date"  -> _date.value = value
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 10) Called when the user taps “Load more.” We bump currentPage and take the next slice.
    // ─────────────────────────────────────────────────────────────────────────────
    fun loadMore() {
        val filteredList = _filtered.value
        val nextPage = _currentPage.value + 1
        val pageSize = 10
        val toIndex = (nextPage * pageSize).coerceAtMost(filteredList.size)
        val newSlice = filteredList.take(toIndex)

        _currentPage.value = nextPage
        _visibleResults.value = newSlice
        _hasMore.value = filteredList.size > toIndex
    }
}
