package com.example.teamup.ui.screens

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.domain.model.ActivityItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Load "my activities" from the repository, then mark `isCreator` if the
     * JWT's "sub" (user ID) matches the activity's creatorId.
     */
    fun loadActivities(token: String) {
        viewModelScope.launch {
            try {
                // Decode current user ID from the JWT (without the "Bearer " prefix)
                val currentUserId = getUserIdFromToken(token) ?: -1

                // 1) Fetch activities from repo; each is already an ActivityItem
                val items: List<ActivityItem> = repo.getMyActivities(token)

                // 2) Update `isCreator` flag based on decoded userId
                val updatedList = items.map { item ->
                    item.copy(isCreator = (item.creatorId == currentUserId))
                }

                _activities.value = updatedList
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    /**
     * Extracts the "sub" (user ID) from a JWT access token.
     * Returns null on any failure.
     */
    private fun getUserIdFromToken(token: String): Int? {
        return try {
            val jwt = token.removePrefix("Bearer ").trim()
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payloadBase64 = parts[1]
            val decodedBytes = Base64.decode(payloadBase64, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
            json.getInt("sub")
        } catch (_: Exception) {
            null
        }
    }
}
