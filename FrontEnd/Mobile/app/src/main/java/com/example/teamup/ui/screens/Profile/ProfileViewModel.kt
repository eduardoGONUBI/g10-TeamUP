// File: app/src/main/java/com/example/teamup/presentation/profile/ProfileViewModel.kt
package com.example.teamup.presentation.profile

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.AchievementDto
import com.example.teamup.data.remote.model.ActivityDto
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

    private val _achievements        = MutableStateFlow<List<AchievementDto>>(emptyList())
    val achievements: StateFlow<List<AchievementDto>> = _achievements

    private val _reputation          = MutableStateFlow<Int?>(null)
    val reputation: StateFlow<Int?>  = _reputation

    private val _behaviour           = MutableStateFlow<Double?>(null)
    val behaviour: StateFlow<Double?> = _behaviour

    // ─── Pagination for created activities ───────────────────────────────
    private val _fullCreatedActivities    = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _visibleCreatedActivities = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _currentPage              = MutableStateFlow(1)
    private val _hasMoreCreated           = MutableStateFlow(false)
    private val pageSize = 10

    val visibleCreatedActivities: StateFlow<List<ActivityItem>> = _visibleCreatedActivities
    val hasMoreCreated: StateFlow<Boolean>                      = _hasMoreCreated

    private val _activitiesError     = MutableStateFlow<String?>(null)
    val activitiesError: StateFlow<String?> = _activitiesError

    private val _error               = MutableStateFlow<String?>(null)
    val error: StateFlow<String?>   = _error

    private val _reputationLabel     = MutableStateFlow("—")
    val reputationLabel: StateFlow<String> = _reputationLabel

    private val _location = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location

    private val _sports = MutableStateFlow<List<String>>(emptyList())
    val sports: StateFlow<List<String>> = _sports

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
        val api = authApi(token)
        try {
            val me = api.getCurrentUser()
            _username.value = me.name
            _location.value = me.location
            _sports.value = me.sports?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /**
     * Fetches and paginates “created activities” so newest appear first.
     */
    fun loadCreatedActivities(token: String) = viewModelScope.launch {
        _activitiesError.value = null
        userId = parseUserId(token)
        if (userId == null) {
            _activitiesError.value = "Invalid token: cannot extract user ID."
            return@launch
        }

        try {
            val api  = eventsApi(token)
            val dtos = api.getMyEvents()
            val mapped = dtos.map { dto ->
                ActivityItem(
                    id               = dto.id.toString(),
                    title            = "${dto.name} : ${dto.sport}",
                    location         = dto.place,
                    startsAt         = dto.startsAt ?: "",
                    participants     = dto.participants?.size ?: 0,
                    maxParticipants  = dto.max_participants,
                    organizer        = dto.creator.name,
                    creatorId        = dto.creator.id,
                    isCreator        = dto.creator.id == userId,
                    isParticipant    = dto.creator.id == userId,
                    latitude         = dto.latitude,
                    longitude        = dto.longitude,
                    status           = dto.status
                )
            }

            // 1) Reverse so newest go first
            val sorted = mapped.reversed()

            // 2) Initialize pagination
            val firstPage   = sorted.take(pageSize)
            val moreExists  = sorted.size > pageSize

            _fullCreatedActivities.value    = sorted
            _visibleCreatedActivities.value = firstPage
            _currentPage.value              = 1
            _hasMoreCreated.value           = moreExists

        } catch (e: Exception) {
            _activitiesError.value = e.message
        }
    }

    /** Called when “Load more” is tapped in ProfileScreen */
    fun loadMoreCreated() {
        val ui = _fullCreatedActivities.value
        val current = _currentPage.value
        if (!_hasMoreCreated.value) return

        val nextPage = current + 1
        val toIndex  = (nextPage * pageSize).coerceAtMost(ui.size)
        val newSlice = ui.take(toIndex)

        _visibleCreatedActivities.value = newSlice
        _currentPage.value              = nextPage
        _hasMoreCreated.value           = ui.size > toIndex
    }

    fun loadStats(token: String) = viewModelScope.launch {
        _error.value = null
        if (userId == null) {
            userId = parseUserId(token)
        }
        if (userId == null) {
            _error.value = "Cannot load stats: user ID is missing."
            return@launch
        }

        try {
            val api    = AchievementsApi.create()
            val bearer = if (token.trim().startsWith("Bearer ")) token.trim() else "Bearer $token".trim()

            // 1) profile (xp & level)
            val profile  = api.getProfile(userId!!, bearer)
            _xp.value    = profile.xp
            _level.value = profile.level

            // 2) achievements
            val achList  = api.listAchievements(userId!!, bearer)
            _achievements.value = achList.achievements

            // 3) reputation
            val rep      = api.getReputation(userId!!, bearer)
            _reputation.value = rep.score
            _behaviour.value = rep.score.toDouble()

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

        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /* ─── RETROFIT HELPERS ───────────────────────────────────────────────── */
    private fun authApi(token: String): AuthApi {
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
            .create(AuthApi::class.java)
    }

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

// ――― Internal DTO interface for Events ―――
private interface EventsApi {
    @GET("api/events")
    suspend fun getMyEvents(): List<ActivityDto>
}
