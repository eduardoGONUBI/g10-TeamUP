package com.example.teamup.ui.screens

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.data.remote.ActivityDto
import com.example.teamup.data.remote.ActivityItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeViewModel(
    private val api: ActivityApi
) : ViewModel() {

    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadActivities(token: String) {
        viewModelScope.launch {
            try {
                // 1️⃣ Decode current user ID from the JWT
                val currentUserId = getUserIdFromToken(token) ?: -1

                val result: List<ActivityDto> = api.getMyActivities("Bearer $token")
                _activities.value = result.map { dto ->
                    // 2️⃣ Map DTO → UI model including creatorId & isCreator
                    ActivityItem(
                        id             = dto.id.toString(),
                        title          = "${dto.name} : ${dto.sport}",
                        location       = dto.place,
                        date           = dto.date,
                        participants   = dto.participants.size,
                        maxParticipants= dto.max_participants,
                        organizer      = dto.creator.name,
                        creatorId      = dto.creator.id,
                        isCreator      = (dto.creator.id == currentUserId),
                        latitude       = dto.latitude,
                        longitude      = dto.longitude
                    )
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    /**
     * Extracts the "sub" (user ID) from a JWT access token.
     * Assumes a well-formed JWT; returns null on any failure.
     */
    private fun getUserIdFromToken(token: String): Int? {
        return try {
            val jwt = token.removePrefix("Bearer ").trim()
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            // URL‐safe Base64 decode, no padding
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            json.getInt("sub")
        } catch (_: Exception) {
            null
        }
    }
}
