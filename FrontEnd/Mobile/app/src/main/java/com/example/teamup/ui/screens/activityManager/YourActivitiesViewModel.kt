// File: app/src/main/java/com/example/teamup/ui/screens/activityManager/YourActivitiesViewModel.kt
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

data class YourActivitiesUiState(
    val fullActivities: List<ActivityItem> = emptyList(),   // all items fetched from repo
    val visibleActivities: List<ActivityItem> = emptyList(),// items to show in UI right now
    val loading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,                                // 1-based page index
    val pageSize: Int = 10,                                   // how many per “page”
    val hasMore: Boolean = false                              // whether fullActivities.size > currentPage*pageSize
)

class YourActivitiesViewModel(
    private val repo: ActivityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(YourActivitiesUiState())
    val state: StateFlow<YourActivitiesUiState> = _state

    /** Load “/events/mine” and then annotate and show only the first page. */
    fun loadMyEvents(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                // 1) Fetch raw items
                val rawList = repo.getMyActivities(token)

                // 2) Decode JWT payload (“sub” claim) → currentUserId
                val currentUserId = parseUserIdFromJwt(token)

                // 3) Mark isCreator on each
                val annotated = rawList.map { item ->
                    item.copy(isCreator = (item.creatorId == currentUserId))
                }

                /// Reverse so that newest appear first
                val sorted = annotated.reversed()

                // 4) Compute initial “visible” slice (first page)
                val firstPage = sorted.take(_state.value.pageSize)
                val moreExists = sorted.size > _state.value.pageSize

                _state.value = _state.value.copy(
                    fullActivities = annotated,
                    visibleActivities = firstPage,
                    loading = false,
                    currentPage = 1,
                    hasMore = moreExists
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    /** When “Load more” is pressed, bump page and append the next slice. */
    fun loadMore() {
        val ui = _state.value
        if (!ui.hasMore) return

        val nextPage = ui.currentPage + 1
        val fromIndex = 0
        val toIndex = (nextPage * ui.pageSize).coerceAtMost(ui.fullActivities.size)
        val newSlice = ui.fullActivities.take(toIndex)

        _state.value = ui.copy(
            visibleActivities = newSlice,
            currentPage = nextPage,
            hasMore = ui.fullActivities.size > toIndex
        )
    }

    /** Simple helper: decode JWT’s payload and extract “sub” as Int. */
    private fun parseUserIdFromJwt(jwtToken: String): Int {
        return try {
            // JWT = header.payload.signature
            val parts = jwtToken.split(".")
            if (parts.size < 2) return -1
            val payloadBase64 = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                // pad to multiple of 4
                .let { s -> s + "=".repeat((4 - s.length % 4) % 4) }

            val decoded = Base64.decode(payloadBase64, Base64.DEFAULT)
            val json = JSONObject(String(decoded))
            json.optInt("sub", -1)
        } catch (_: Exception) {
            -1
        }
    }
}
