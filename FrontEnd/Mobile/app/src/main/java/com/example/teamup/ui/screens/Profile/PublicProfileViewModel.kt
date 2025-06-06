// File: app/src/main/java/com/example/teamup/presentation/profile/PublicProfileViewModel.kt
package com.example.teamup.presentation.profile

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.AchievementDto
import com.example.teamup.data.remote.model.PublicUserDto
import com.example.teamup.data.remote.model.ProfileResponse
import com.example.teamup.data.remote.model.ReputationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for loading *another* user's public profile (read‐only),
 * plus paginated “events created by” that user.
 */
class PublicProfileViewModel(
    private val userId: Int,
    private val bearer: String    // must be "Bearer <token>"
) : ViewModel() {

    private val authApi = AuthApi.create()
    private val achApi  = AchievementsApi.create()
    private val activityApi = ActivityApi.create()

    // ─── PUBLIC PROFILE STATE ────────────────────────────────────────────────
    private val _name       = MutableStateFlow("Loading…")
    val name: StateFlow<String>          = _name

    private val _avatarUrl  = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?>    = _avatarUrl

    private val _location   = MutableStateFlow<String?>(null)
    val location: StateFlow<String?>     = _location

    private val _sports     = MutableStateFlow<List<String>>(emptyList())
    val sports: StateFlow<List<String>>  = _sports

    private val _level      = MutableStateFlow(0)
    val level: StateFlow<Int>            = _level

    private val _behaviour  = MutableStateFlow<Int?>(null)
    val behaviour: StateFlow<Int?>       = _behaviour

    private val _repLabel   = MutableStateFlow("—")
    val repLabel: StateFlow<String>      = _repLabel

    private val _achievements = MutableStateFlow<List<AchievementDto>>(emptyList())
    val achievements: StateFlow<List<AchievementDto>> = _achievements

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?>        = _error

    // ─── NEW: Pagination for “events created by” ───────────────────────────
    private val _fullEvents       = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _visibleEvents    = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _currentPage      = MutableStateFlow(1)
    private val _hasMoreEvents    = MutableStateFlow(false)

    /** Expose only the currently visible “slice” */
    val visibleEvents: StateFlow<List<ActivityItem>> = _visibleEvents

    /** Indicates if there are more events beyond the current page */
    val hasMoreEvents: StateFlow<Boolean> = _hasMoreEvents

    /** Holds any error that occurs while fetching events */
    private val _eventsError  = MutableStateFlow<String?>(null)
    val eventsError: StateFlow<String?> = _eventsError

    /** How many events per “page” */
    private val pageSize = 10

    init {
        loadPublicProfile()
        loadUserEvents()  // ← Kick off initial fetch + pagination
    }

    /**
     * 1) GET /api/users/{id}        → name, avatar_url, location, sports
     * 2) GET /api/profile/{id}      → xp + level
     * 3) GET /api/rating/{id}       → rep counts + score
     * 4) GET /api/achievements/{id} → unlocked achievements
     */
    private fun loadPublicProfile() = viewModelScope.launch {
        try {
            // ─── 1) Fetch /api/users/{id} ───────────────────────────────
            val publicUser: PublicUserDto = authApi.getUser(userId, bearer)
            _name.value      = publicUser.name
            _avatarUrl.value = publicUser.avatar_url
            _location.value  = publicUser.location
            _sports.value    = publicUser.sports?.map { it.name } ?: emptyList()

            // ─── 2) Fetch /api/profile/{id} → xp + level ──────────────
            val profile: ProfileResponse = achApi.getProfile(userId, bearer)
            _level.value = profile.level

            // ─── 3) Fetch /api/rating/{id} → score + count details ────
            val rep: ReputationResponse = achApi.getReputation(userId, bearer)
            _behaviour.value = rep.score

            // Compute the “top feedback” label:
            val counts = listOf(
                "Good teammate"   to (rep.good_teammate_count ?: 0),
                "Friendly player" to (rep.friendly_count    ?: 0),
                "Team player"     to (rep.team_player_count ?: 0),
                "Watchlisted"     to (rep.toxic_count       ?: 0),
                "Bad sport"       to (rep.bad_sport_count   ?: 0),
                "Frequent AFK"    to (rep.afk_count         ?: 0)
            )
            val top = counts.maxByOrNull { it.second }
            _repLabel.value = if (top != null && top.second > 0) {
                "${top.first} (${top.second})"
            } else {
                "—"
            }

            // ─── 4) Fetch unlocked achievements ────────────────────────
            val achList = achApi.listAchievements(userId, bearer)
            _achievements.value = achList.achievements

        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /** NEW: Load *all* events created by this user, then initialize pagination */
    private fun loadUserEvents() = viewModelScope.launch {
        _eventsError.value = null
        try {
            // 1) Call the new endpoint: GET /api/users/{id}/events
            val dtos = activityApi.getEventsByUser(userId, bearer)

            // 2) Map each ActivityDto → ActivityItem (treating userId as “current”)
            val mapped = dtos.map { dto ->
                val participantIds = dto.participants?.map { it.id }?.toSet() ?: emptySet()
                ActivityItem(
                    id              = dto.id.toString(),
                    title           = "${dto.name} : ${dto.sport}",
                    location        = dto.place,
                    startsAt        = dto.startsAt ?: dto.date ?: "",
                    participants    = dto.participants?.size ?: 0,
                    maxParticipants = dto.max_participants,
                    organizer       = dto.creator.name,
                    creatorId       = dto.creator.id,
                    isParticipant   = participantIds.contains(userId),
                    latitude        = dto.latitude,
                    longitude       = dto.longitude,
                    isCreator       = (dto.creator.id == userId),
                    status          = dto.status
                )
            }

            // 3) Sort so newest appear first (optional)
            val sorted = mapped.reversed()
            _fullEvents.value = sorted

            // 4) Initialize pagination: first `pageSize` items
            val firstPage = sorted.take(pageSize)
            _visibleEvents.value = firstPage
            _currentPage.value = 1
            _hasMoreEvents.value = sorted.size > pageSize

        } catch (e: Exception) {
            _eventsError.value = e.message
        }
    }

    /** Called when “Load more” is tapped in the UI */
    fun loadMoreEvents() {
        val all = _fullEvents.value
        val current = _currentPage.value
        if (!_hasMoreEvents.value) return  // nothing more to load

        val nextPage = current + 1
        val toIndex = (nextPage * pageSize).coerceAtMost(all.size)

        // Take the slice [0..toIndex)
        val newSlice = all.take(toIndex)
        _visibleEvents.value = newSlice
        _currentPage.value = nextPage
        _hasMoreEvents.value = all.size > toIndex
    }
}
