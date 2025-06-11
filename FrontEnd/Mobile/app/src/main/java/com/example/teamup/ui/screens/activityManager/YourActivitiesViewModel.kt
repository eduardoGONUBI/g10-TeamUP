package com.example.teamup.ui.screens.activityManager

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// UI state permanece praticamente igual, mas "pageSize" já não é usado para slices locais
// porque cada página remota será appendada por inteiro.
data class YourActivitiesUiState(
    val fullActivities: List<ActivityItem> = emptyList(),
    val visibleActivities: List<ActivityItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val currentRemotePage: Int = 1,
    val hasMore: Boolean = false                // vem do repo.hasMore
)

class YourActivitiesViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(YourActivitiesUiState())
    val state: StateFlow<YourActivitiesUiState> = _state

    private var savedToken: String = ""

    /** Primeiro carregamento / refresh total */
    fun loadMyEvents(token: String) = viewModelScope.launch {
        savedToken = token
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            // reset paginação remota
            var page = 1
            val aggregated = mutableListOf<ActivityItem>()
            do {
                val pageItems = repo.getMyActivities(token, page)
                aggregated += pageItems
                page++
            } while (repo.hasMore.not() && false) // nao buscar todas as páginas de uma vez
            // O requisito actual: só a 1ª página, restantes via "Load more"

            val currentUserId = parseUserIdFromJwt(token)

            val sorted = aggregated.map { it.copy(isCreator = (it.creatorId == currentUserId)) }

            _state.value = _state.value.copy(
                fullActivities    = sorted,
                visibleActivities = sorted,           // mostra tudo o que veio (1ª página)
                loading           = false,
                currentRemotePage = 1,
                hasMore           = repo.hasMore
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                error   = e.localizedMessage ?: "Unknown error"
            )
        }
    }

    /** Load more: busca próxima página remota se existir e faz append */
    fun loadMore() = viewModelScope.launch {
        val ui = _state.value
        if (!ui.hasMore) return@launch
        val nextRemotePage = ui.currentRemotePage + 1
        try {
            val newPageItems = repo.getMyActivities(savedToken, nextRemotePage)
            val currentUserId = parseUserIdFromJwt(savedToken)
            val annotated = newPageItems.map { it.copy(isCreator = (it.creatorId == currentUserId)) }
            val combined = ui.fullActivities + annotated
            _state.value = ui.copy(
                fullActivities    = combined,
                visibleActivities = combined,   // mostra tudo acumulado
                currentRemotePage = nextRemotePage,
                hasMore           = repo.hasMore
            )
        } catch (e: Exception) {
            _state.value = ui.copy(error = e.localizedMessage ?: "Failed to load more")
        }
    }

    /* helper para extrair sub claim */
    private fun parseUserIdFromJwt(jwtToken: String): Int {
        return try {
            val parts = jwtToken.removePrefix("Bearer ").split(".")
            if (parts.size < 2) return -1
            val payloadBase64 = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let { s -> s + "=".repeat((4 - s.length % 4) % 4) }
            val decoded = Base64.decode(payloadBase64, Base64.DEFAULT)
            val json = JSONObject(String(decoded))
            json.optInt("sub", -1)
        } catch (_: Exception) { -1 }
    }
}
