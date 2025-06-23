package com.example.teamup.ui.screens.activityManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchActivityViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    /* ── filtros ───────────────────────────── */
    private val _name  = MutableStateFlow("")
    private val _sport = MutableStateFlow("")
    private val _place = MutableStateFlow("")
    private val _date  = MutableStateFlow("")

    /* ── dados remotos ──────────────────────── */
    private val _allEvents   = MutableStateFlow<List<Activity>>(emptyList())
    private val _loading     = MutableStateFlow(false)
    private val _error       = MutableStateFlow<String?>(null)

    /* ── paginação REMOTA ───────────────────── */
    private var backendPage        = 1
    private var lastBackendPage    = false
    private var savedToken: String = ""

    /* ── paginação LOCAL ────────────────────── */
    private val pageSizeLocal       = 10
    private val _currentPageLocal   = MutableStateFlow(1)
    private val _visibleResults     = MutableStateFlow<List<Activity>>(emptyList())
    private val _hasMoreLocal       = MutableStateFlow(false)

    /* ── combina filtros  + dados remotos ───────────── */
    private val _filtered: StateFlow<List<Activity>> = combine(
        _name, _sport, _place, _date, _allEvents
    ) { name, sport, place, date, all ->
        all.filter { item ->
            val matchesName  = name.isBlank() || item.title.contains(name, ignoreCase = true)
            val matchesSport = sport.isBlank() || item.title.substringAfter(":").trim().contains(sport, ignoreCase = true)
            val matchesPlace = place.isBlank() || item.location.contains(place, ignoreCase = true)
            val matchesDate  = date.isBlank() || item.startsAt.startsWith(date)
            val notInvolved  = !item.isCreator && !item.isParticipant
            matchesName && matchesSport && matchesPlace && matchesDate && notInvolved
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /* ── actualizar UI quando a lista filtrada muda ── */
    init {
        viewModelScope.launch {
            _filtered.collect { list ->
                resetLocalPaging(list)
            }
        }
    }

    /* ─── Fluxos públicos ─────────────────────────────────── */
    val name: StateFlow<String>                    = _name
    val sport: StateFlow<String>                   = _sport
    val place: StateFlow<String>                   = _place
    val date: StateFlow<String>                    = _date
    val loading: StateFlow<Boolean>                = _loading
    val error: StateFlow<String?>                  = _error
    val filtered: StateFlow<List<Activity>>    = _filtered
    val visibleResults: StateFlow<List<Activity>> = _visibleResults
    val hasMore: StateFlow<Boolean>                = _hasMoreLocal

    // primeira chamada carrega todas as paginas antes de filtrar
    fun loadAllEvents(rawToken: String) = viewModelScope.launch {
        _loading.value = true
        _error.value   = null
        savedToken     = rawToken
        backendPage    = 1
        lastBackendPage = false
        try {
            val all = mutableListOf<Activity>()
            do {
                val pageItems = repo.getAllEvents(
                    bearer(rawToken),
                    page    = backendPage,
                    perPage = 50
                )
                all += pageItems
                backendPage++
            } while (repo.hasMore)
            _allEvents.value = all
            _loading.value   = false
        } catch (e: Exception) {
            _loading.value = false
            _error.value   = e.localizedMessage ?: "Unknown error"
        }
    }

     // atualiza filtros
    fun updateFilter(field: String, value: String) {
        when (field) {
            "name"  -> _name.value  = value
            "sport" -> _sport.value = value
            "place" -> _place.value = value
            "date"  -> _date.value  = value
        }
    }


    // load more, primeiro local depois remoto se necessario
    fun loadMore() = viewModelScope.launch {
        val filteredList = _filtered.value
        val nextLocalIdx = (_currentPageLocal.value + 1) * pageSizeLocal

        // a) se ainda há itens locais, só avança local
        if (nextLocalIdx <= filteredList.size) {
            advanceLocalPage(filteredList)
            return@launch
        }

        // b) senão, busca próxima página remota
        if (!lastBackendPage) {
            try {
                val pageItems = repo.getAllEvents(
                    bearer(savedToken),
                    page    = backendPage,
                    perPage = 50
                )
                // acumula e atualiza flag remota
                _allEvents.value   = _allEvents.value + pageItems
                lastBackendPage    = !repo.hasMore
                backendPage++
                // então avança local
                advanceLocalPage(_filtered.value)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to load more"
            }
        }
    }

    /* ─── Helpers ─────────────────────────────────────────────────── */
    private fun resetLocalPaging(fullList: List<Activity>) {
        _currentPageLocal.value = 1
        _visibleResults.value   = fullList.take(pageSizeLocal)
        _hasMoreLocal.value     = fullList.size > pageSizeLocal || !lastBackendPage
    }

    private fun advanceLocalPage(fullList: List<Activity>) {
        val nextPage = _currentPageLocal.value + 1
        val toIdx    = (nextPage * pageSizeLocal).coerceAtMost(fullList.size)
        _currentPageLocal.value = nextPage
        _visibleResults.value   = fullList.take(toIdx)
        _hasMoreLocal.value     = fullList.size > toIdx || !lastBackendPage
    }

    private fun bearer(tok: String): String =
        if (tok.trim().startsWith("Bearer ")) tok.trim()
        else "Bearer ${tok.trim()}"
}
