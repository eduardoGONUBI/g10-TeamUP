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
 * ViewModel para ecrã de perfil público de outro utilizador,
 * incluindo paginação local de “events created by” esse utilizador.
 */
class PublicProfileViewModel(
    private val userId: Int,
    private val bearer: String        // deve ser "Bearer <token>"
) : ViewModel() {

    private val authApi     = AuthApi.create()
    private val achApi      = AchievementsApi.create()
    private val activityApi = ActivityApi.create()

    /* ─── Perfil público ──────────────────────────────────────────────── */
    private val _name      = MutableStateFlow("Loading…")
    val name: StateFlow<String>           = _name

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?>     = _avatarUrl

    private val _location  = MutableStateFlow<String?>(null)
    val location: StateFlow<String?>      = _location

    private val _sports    = MutableStateFlow<List<String>>(emptyList())
    val sports: StateFlow<List<String>>   = _sports

    /* ─── Métricas ───────────────────────────────────────────────────── */
    private val _level     = MutableStateFlow(0)
    val level: StateFlow<Int>            = _level

    private val _behaviour = MutableStateFlow<Int?>(null)
    val behaviour: StateFlow<Int?>       = _behaviour

    private val _repLabel  = MutableStateFlow("—")
    val repLabel: StateFlow<String>      = _repLabel

    private val _achievements = MutableStateFlow<List<AchievementDto>>(emptyList())
    val achievements: StateFlow<List<AchievementDto>> = _achievements

    private val _error     = MutableStateFlow<String?>(null)
    val error: StateFlow<String?>         = _error

    /* ─── Eventos criados (paginação local) ───────────────────────────── */
    private val _fullEvents    = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _visibleEvents = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _currentPage   = MutableStateFlow(1)
    private val _hasMoreEvents = MutableStateFlow(false)
    private val _eventsError   = MutableStateFlow<String?>(null)

    /** Exposto à UI: fatia corrente de eventos */
    val visibleEvents: StateFlow<List<ActivityItem>> = _visibleEvents

    /** Exposto à UI: se há mais páginas locais restantes */
    val hasMoreEvents: StateFlow<Boolean> = _hasMoreEvents

    /** Error ao carregar eventos */
    val eventsError: StateFlow<String?> = _eventsError

    /** Quantos eventos por “página” local */
    private val pageSize = 10

    init {
        loadPublicProfile()
        loadUserEvents()
    }

    /**
     * 1) GET /api/users/{id}
     * 2) GET /api/profile/{id}
     * 3) GET /api/rating/{id}
     * 4) GET /api/achievements/{id}
     */
    private fun loadPublicProfile() = viewModelScope.launch {
        try {
            // 1) dados básicos do user
            val pu: PublicUserDto = authApi.getUser(userId, bearer)
            _name.value      = pu.name
            _avatarUrl.value = pu.avatar_url
            _location.value  = pu.location
            _sports.value    = pu.sports?.map { it.name } ?: emptyList()

            // 2) xp + level
            val pr: ProfileResponse = achApi.getProfile(userId, bearer)
            _level.value = pr.level

            // 3) reputação
            val rr: ReputationResponse = achApi.getReputation(userId, bearer)
            _behaviour.value = rr.score
            val counts = listOf(
                "Good teammate"   to (rr.good_teammate_count ?: 0),
                "Friendly player" to (rr.friendly_count    ?: 0),
                "Team player"     to (rr.team_player_count ?: 0),
                "Watchlisted"     to (rr.toxic_count       ?: 0),
                "Bad sport"       to (rr.bad_sport_count   ?: 0),
                "Frequent AFK"    to (rr.afk_count         ?: 0)
            )
            _repLabel.value = counts.maxByOrNull { it.second }
                ?.takeIf { it.second > 0 }
                ?.let { "${it.first} (${it.second})" }
                ?: "—"

            // 4) achievements
            _achievements.value = achApi.listAchievements(userId, bearer).achievements

        } catch (e: Exception) {
            _error.value = e.localizedMessage
        }
    }

    /** Carrega todos os eventos criados por este user e inicializa paginação local */
    private fun loadUserEvents() = viewModelScope.launch {
        _eventsError.value = null
        try {
            // fetch raw list (assume endpoint retornar todos de uma vez)
            val dtoList = activityApi.getEventsByUser(userId, bearer)

            // mapear DTO → ActivityItem
            val mapped = dtoList.map { dto ->
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

            // ordenar (mais recentes primeiro)
            val sorted = mapped.reversed()
            _fullEvents.value = sorted

            // slice inicial
            _visibleEvents.value = sorted.take(pageSize)
            _currentPage.value   = 1
            _hasMoreEvents.value = sorted.size > pageSize

        } catch (e: Exception) {
            _eventsError.value = e.localizedMessage
        }
    }

    /** Chamado pelo botão “Load more” para expandir a lista local */
    fun loadMoreEvents() {
        val all     = _fullEvents.value
        val current = _currentPage.value
        if (!_hasMoreEvents.value) return

        val nextPage = current + 1
        val toIndex  = (nextPage * pageSize).coerceAtMost(all.size)
        _visibleEvents.value = all.take(toIndex)
        _currentPage.value   = nextPage
        _hasMoreEvents.value = all.size > toIndex
    }
}
