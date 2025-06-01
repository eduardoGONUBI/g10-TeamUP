// app/src/main/java/com/example/teamup/ui/screens/activityManager/YourActivitiesViewModel.kt
package com.example.teamup.ui.screens.activityManager

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.domain.model.ActivityItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class YourActivitiesUiState(
    val activities: List<ActivityItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class YourActivitiesViewModel(
    private val repo: ActivityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(YourActivitiesUiState())
    val state: StateFlow<YourActivitiesUiState> = _state

    /** Load “/events/mine” and then mark each item’s [isCreator] field. */
    fun loadMyEvents(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                // 1) Fetch raw items
                val rawList = repo.getMyActivities(token)
                // 2) Decode JWT payload (“sub” claim) → currentUserId
                val currentUserId = parseUserIdFromJwt(token)
                // 3) Mark isCreator = true if creatorId == currentUserId
                val annotated = rawList.map { item ->
                    item.copy(isCreator = (item.creatorId == currentUserId))
                }
                _state.value = _state.value.copy(loading = false, activities = annotated)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
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
