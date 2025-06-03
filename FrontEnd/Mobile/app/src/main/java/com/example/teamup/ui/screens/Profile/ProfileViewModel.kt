// File: app/src/main/java/com/example/teamup/presentation/profile/ProfileViewModel.kt
package com.example.teamup.presentation.profile

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.remote.AchievementsApi
import com.example.teamup.data.remote.AuthApi
import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.ActivityDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class ProfileViewModel : ViewModel() {

    /* ─── PUBLIC read‐only state ───────────────────────────────────────── */
    private val _username            = MutableStateFlow("Loading…")
    val username: StateFlow<String>   = _username

    private val _xp                  = MutableStateFlow(0)
    val xp: StateFlow<Int>           = _xp

    private val _level               = MutableStateFlow(0)
    val level: StateFlow<Int>        = _level

    private val _achievements        = MutableStateFlow<List<com.example.teamup.data.remote.AchievementDto>>(emptyList())
    val achievements: StateFlow<List<com.example.teamup.data.remote.AchievementDto>> = _achievements

    private val _reputation          = MutableStateFlow<Int?>(null)
    val reputation: StateFlow<Int?>  = _reputation

    private val _behaviour           = MutableStateFlow<Double?>(null)
    val behaviour: StateFlow<Double?> = _behaviour

    private val _createdActivities   = MutableStateFlow<List<ActivityItem>>(emptyList())
    val createdActivities: StateFlow<List<ActivityItem>> = _createdActivities

    private val _error               = MutableStateFlow<String?>(null)
    val error: StateFlow<String?>   = _error

    private val _activitiesError     = MutableStateFlow<String?>(null)
    val activitiesError: StateFlow<String?> = _activitiesError

    /**
     * A single‐string label for “Reputation → top feedback count”
     * e.g. `"Good teammate (3)"` or `"—"` if no feedback.
     */
    private val _reputationLabel     = MutableStateFlow("—")
    val reputationLabel: StateFlow<String> = _reputationLabel

    /* ─── Derived / cached ───────────────────────────────────────────────── */
    var userId: Int? = null
        private set

    /**
     * Extracts the `sub` claim from a JWT, or returns null if invalid.
     */
    private fun parseUserId(token: String): Int? {
        return try {
            val rawJwt = token.removePrefix("Bearer ").trim()
            val parts  = rawJwt.split(".")
            if (parts.size < 2) return null
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP)
            val payloadJson  = JSONObject(String(payloadBytes, Charsets.UTF_8))
            payloadJson.getInt("sub")
        } catch (_: Exception) {
            null
        }
    }

    /* ─── LOADERS ───────────────────────────────────────────────────────── */
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

        // Parse and cache userId once
        userId = parseUserId(token)
        if (userId == null) {
            _activitiesError.value = "Invalid token: cannot extract user ID."
            return@launch
        }

        try {
            // Use EventsApi (which calls GET /api/events)
            val api  = eventsApi(token)
            val dtos = api.getMyEvents()
            _createdActivities.value = dtos.map { dto ->
                ActivityItem(
                    id               = dto.id.toString(),
                    title            = "${dto.name} : ${dto.sport}",
                    location         = dto.place,
                    date             = dto.date,
                    participants     = dto.participants?.size ?: 0,
                    maxParticipants  = dto.max_participants,
                    organizer        = dto.creator.name,
                    creatorId        = dto.creator.id,
                    isCreator        = dto.creator.id == userId,
                    isParticipant    = dto.creator.id == userId,
                    latitude         = dto.latitude,
                    longitude        = dto.longitude
                )
            }
        } catch (e: Exception) {
            _activitiesError.value = e.message
        }
    }

    /**
     * Loads XP, Level, Achievements, Reputation score (to Behaviour),
     * and top‐feedback label (to Reputation), and ignores average‐rating entirely.
     */
    fun loadStats(token: String) = viewModelScope.launch {
        _error.value = null

        // Ensure userId is known
        if (userId == null) {
            userId = parseUserId(token)
        }
        if (userId == null) {
            _error.value = "Cannot load stats: user ID is missing."
            return@launch
        }

        try {
            // Build Retrofit for the Achievements API
            val api    = AchievementsApi.create()
            val bearer = if (token.trim().startsWith("Bearer ")) token.trim() else "Bearer $token".trim()

            // 1) fetch profile (xp & level)
            val profile  = api.getProfile(userId!!, bearer)
            _xp.value    = profile.xp
            _level.value = profile.level

            // 2) fetch achievements list
            val achList  = api.listAchievements(userId!!, bearer)
            _achievements.value = achList.achievements

            // 3) fetch raw reputation details (score + six feedback counts)
            val rep      = api.getReputation(userId!!, bearer)
            _reputation.value = rep.score

            // Use the **score**‐field as “Behaviour”:
            _behaviour.value = rep.score.toDouble()

            // Build a single “top feedback” label for “Reputation”:
            val counts: List<Pair<String, Int>> = listOf(
                "Good teammate"   to (rep.good_teammate_count ?: 0),
                "Friendly player" to (rep.friendly_count    ?: 0),
                "Team player"     to (rep.team_player_count ?: 0),
                "Watchlisted"     to (rep.toxic_count       ?: 0),
                "Bad sport"       to (rep.bad_sport_count   ?: 0),
                "Frequent AFK"    to (rep.afk_count         ?: 0)
            )
            val topFeedback = counts.maxByOrNull { it.second }
            _reputationLabel.value = if (topFeedback != null && topFeedback.second > 0) {
                "${topFeedback.first} (${topFeedback.second})"
            } else {
                "—"
            }

            // 4) We no longer need `getUserAverage(...)` at all, so skip it.

        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /* ─── RETROFIT HELPERS ───────────────────────────────────────────────── */

    /**
     * Builds an `AuthApi` that injects a single
     * “Authorization: Bearer <jwt>” header into each request.
     */
    private fun authApi(token: String): com.example.teamup.data.remote.AuthApi {
        val rawBearer = if (token.trim().startsWith("Bearer ")) token.trim() else "Bearer $token".trim()
        val client = OkHttpClient.Builder()
            .addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val authorized = chain.request().newBuilder()
                        .addHeader("Authorization", rawBearer)
                        .build()
                    return chain.proceed(authorized)
                }
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BaseUrlProvider.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(com.example.teamup.data.remote.AuthApi::class.java)
    }

    /**
     * Builds an `EventsApi` that injects a single
     * “Authorization: Bearer <jwt>” header into each request.
     */
    private fun eventsApi(token: String): EventsApi {
        val rawBearer = if (token.trim().startsWith("Bearer ")) token.trim() else "Bearer $token".trim()
        val client = OkHttpClient.Builder()
            .addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val authorized = chain.request().newBuilder()
                        .addHeader("Authorization", rawBearer)
                        .build()
                    return chain.proceed(authorized)
                }
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BaseUrlProvider.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(EventsApi::class.java)
    }
}

// ――― Internal DTO interface for Events (used by eventsApi above) ―――
private interface EventsApi {
    @GET("api/events")
    suspend fun getMyEvents(): List<ActivityDto>
}
