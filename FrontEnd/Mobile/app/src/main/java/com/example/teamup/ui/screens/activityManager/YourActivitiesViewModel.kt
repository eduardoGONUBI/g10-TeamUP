package com.example.teamup.ui.screens.activityManager

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// estado
data class YourActivitiesUiState(
    val fullActivities: List<Activity> = emptyList(),
    val visibleActivities: List<Activity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val currentRemotePage: Int = 1,
    val hasMore: Boolean = false
)

class YourActivitiesViewModel(
    private val repo: ActivityRepository
) : ViewModel() {


    private val _state = MutableStateFlow(YourActivitiesUiState())
    val state: StateFlow<YourActivitiesUiState> = _state

    private var savedToken: String = ""

     //  carrega a primeira pagina de atividades
    fun loadMyEvents(token: String) = viewModelScope.launch {
        savedToken = token
        _state.value = _state.value.copy(loading = true, error = null)
        try {

            var page = 1
            val aggregated = mutableListOf<Activity>()
            do {
                val pageItems = repo.getMyActivities(token, page)
                aggregated += pageItems
                page++
            } while (repo.hasMore.not() && false) // nao buscar todas as páginas de uma vez

            val currentUserId = parseUserIdFromJwt(token)  // busca o id pelo token para ver se e o criador

            val sorted = aggregated.map { it.copy(isCreator = (it.creatorId == currentUserId)) }

            _state.value = _state.value.copy(  // atualiza o estao com a 1 pagina
                fullActivities    = sorted,
                visibleActivities = sorted,
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

     // load more
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
                visibleActivities = combined,
                currentRemotePage = nextRemotePage,
                hasMore           = repo.hasMore
            )
        } catch (e: Exception) {
            _state.value = ui.copy(error = e.localizedMessage ?: "Failed to load more")
        }
    }

    /* helper para extrair id */
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
