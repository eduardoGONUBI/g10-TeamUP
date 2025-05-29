package com.example.teamup.presentation.profile

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class ProfileViewModel : ViewModel() {

    // ─── STATE ────────────────────────────────────────────────
    private val _username = MutableStateFlow("Loading…")
    val username: StateFlow<String> = _username

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createdActivities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val createdActivities: StateFlow<List<ActivityItem>> = _createdActivities

    private val _activitiesError = MutableStateFlow<String?>(null)
    val activitiesError: StateFlow<String?> = _activitiesError

    // ─── PUBLIC ────────────────────────────────────────────────
    fun loadUser(token: String) = viewModelScope.launch {
        _error.value = null
        try {
            val api = authApi(token)
            _username.value = api.getCurrentUser().name
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun loadCreatedActivities(token: String) = viewModelScope.launch {
        _activitiesError.value = null
        try {
            val api = eventsApi(token)
            val userId = getUserIdFromToken(token)
            val dtos = api.getMyEvents()

            _createdActivities.value = dtos.map { dto ->
                ActivityItem(
                    id               = dto.id.toString(),
                    title            = "${dto.name} : ${dto.sport}",
                    location         = dto.place,
                    date             = dto.date,
                    participants     = dto.participants.size,
                    maxParticipants  = dto.max_participants,
                    organizer        = dto.creator.name,
                    creatorId        = dto.creator.id,
                    isCreator        = dto.creator.id == userId,
                    latitude         = dto.latitude,
                    longitude        = dto.longitude
                )
            }
        } catch (e: Exception) {
            _activitiesError.value = e.message
        }
    }

    // ─── HELPERS ───────────────────────────────────────────────
    private fun bearerClient(token: String) =
        OkHttpClient.Builder()
            .addInterceptor { c ->
                val req = c.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                c.proceed(req)
            }.build()

    private fun authApi(token: String): AuthApi =
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:80/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(bearerClient(token))
            .build()
            .create(AuthApi::class.java)

    private fun eventsApi(token: String): EventsApi =
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8081/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(bearerClient(token))
            .build()
            .create(EventsApi::class.java)

    private fun getUserIdFromToken(token: String): Int? {
        return try {
            val jwt = token.removePrefix("Bearer ").trim()
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            json.getInt("sub")
        } catch (_: Exception) {
            null
        }
    }
}

// ─── DTO interface ─────────────────────────────────────────────
private interface EventsApi {
    @GET("api/events")
    suspend fun getMyEvents(): List<ActivityDto>
}
