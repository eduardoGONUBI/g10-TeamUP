package com.example.teamup.presentation.profile

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.repository.ActivityRepository
import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.api.AuthApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ───────────────────────────────────────────────────────────
// UI‐state para as actividades criadas (paginação local)
// ───────────────────────────────────────────────────────────
data class CreatedUi(
    val full: List<Activity> = emptyList(),
    val visible: List<Activity> = emptyList(),
    val currentPageLocal: Int = 1,
    val hasMore: Boolean = false
)

// ───────────────────────────────────────────────────────────
// ViewModel
// ───────────────────────────────────────────────────────────
class ProfileViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    /* ── Perfil básico ─────────────────────────────────────── */
    private val _username = MutableStateFlow("Loading…")
    val username: StateFlow<String> = _username

    private val _location = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location

    private val _sports = MutableStateFlow<List<String>>(emptyList())
    val sports: StateFlow<List<String>> = _sports

    /* ── Métricas ─────────────────────────────────────────── */
    private val _level = MutableStateFlow(0)
    val level: StateFlow<Int> = _level

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp

    private val _behaviour = MutableStateFlow<Double?>(null)
    val behaviour: StateFlow<Double?> = _behaviour

    private val _reputationLabel = MutableStateFlow("—")
    val reputationLabel: StateFlow<String> = _reputationLabel

    /* ── Achievements ─────────────────────────────────────── */
    private val _achievements = MutableStateFlow<List<com.example.teamup.data.remote.model.AchievementDto>>(emptyList())
    val achievements: StateFlow<List<com.example.teamup.data.remote.model.AchievementDto>> = _achievements

    /* ── Criados (paginação remota + local) ─────────────────── */
    private val pageSizeLocal  = 10
    private var currentRemotePage = 1
    private var remoteHasMore     = true
    private var savedToken: String = ""
    private val _createdUi = MutableStateFlow(CreatedUi())

    // Exposto à UI como StateFlow
    val visibleCreatedActivities: StateFlow<List<Activity>> =
        _createdUi
            .map { it.visible }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasMoreCreated: StateFlow<Boolean> =
        _createdUi
            .map { it.hasMore }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /* ── Erros ─────────────────────────────────────────────── */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _activitiesError = MutableStateFlow<String?>(null)
    val activitiesError: StateFlow<String?> = _activitiesError

    /* ── Cache userId do token ────────────────────────────── */
    private var userId: Int? = null

    // avatar
    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl


    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    private fun bearer(tok: String): String =
        if (tok.trim().startsWith("Bearer")) tok.trim()
        else "Bearer ${tok.trim()}"

    private fun parseUserId(token: String): Int? = try {
        val parts = token.removePrefix("Bearer ").split(".")
        if (parts.size < 2) null else {
            val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP)
            JSONObject(String(decoded)).getInt("sub")
        }
    } catch (_: Exception) { null }

    private fun authApi(token: String): AuthApi {
        val bearer = bearer(token)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", bearer)
                    .build()
                chain.proceed(req)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BaseUrlProvider.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(AuthApi::class.java)
    }

    // ─────────────────────────────────────────────────────────
    // Loaders
    // ─────────────────────────────────────────────────────────

    /** 1) Carrega perfil básico via AuthApi (interceptor já põe Bearer) */
    fun loadUser(token: String) = viewModelScope.launch {
        _error.value = null
        try {
            val me = authApi(token).getCurrentUser()
            _username.value = me.name
            _location.value = me.location
            _sports.value   = me.sports?.map { it.name } ?: emptyList()
            _avatarUrl.value = BaseUrlProvider.getBaseUrl() +
                    "api/auth/avatar/${me.id}?t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            _error.value = e.localizedMessage
        }
    }

    /** 2) Carrega 1ª página de /events/mine e filtra só criados */
    fun loadCreatedActivities(token: String) = viewModelScope.launch {
        _activitiesError.value = null
        userId = parseUserId(token)
        if (userId == null) {
            _activitiesError.value = "Invalid token"
            return@launch
        }

        try {
            savedToken        = bearer(token)
            currentRemotePage = 1
            remoteHasMore     = true

            val firstPage = repo.getMyActivities(savedToken, page = currentRemotePage)
                .filter { it.creatorId == userId }
                .map    { it.copy(isCreator = true) }
            remoteHasMore = repo.hasMore

            val reversedFirst = firstPage.reversed()
            _createdUi.value = CreatedUi(
                full             = firstPage,
                visible          = firstPage.take(pageSizeLocal),
                currentPageLocal = 1,
                hasMore          = firstPage.size > pageSizeLocal || remoteHasMore
            )
        } catch (e: Exception) {
            _activitiesError.value = e.localizedMessage
        }
    }

    /** 3) “Load more” carrega próxima página remota se a local estiver esgotada */
    fun loadMoreCreated() = viewModelScope.launch {
        val ui = _createdUi.value

        // a) Avança só slice local se ainda houver
        val nextLocalEnd = (ui.currentPageLocal + 1) * pageSizeLocal
        if (nextLocalEnd <= ui.full.size) {
            _createdUi.value = ui.copy(
                visible          = ui.full.take(nextLocalEnd),
                currentPageLocal = ui.currentPageLocal + 1,
                hasMore          = ui.full.size > nextLocalEnd || remoteHasMore
            )
            return@launch
        }

        // b) Se esgotou local mas backend ainda tem páginas, busca mais
        if (!remoteHasMore) return@launch

        try {
            currentRemotePage += 1
            val pageItems = repo.getMyActivities(savedToken, page = currentRemotePage)
                .filter { it.creatorId == userId }
                .map    { it.copy(isCreator = true) }

            remoteHasMore = repo.hasMore

            // Apenas concatena: ui.full já está em ordem desc; pageItems também
            val newFull = ui.full + pageItems

            val newLocalEnd = (ui.currentPageLocal + 1) * pageSizeLocal
            _createdUi.value = ui.copy(
                full             = newFull,
                visible          = newFull.take(newLocalEnd),
                currentPageLocal   = ui.currentPageLocal + 1,
                hasMore          = newFull.size > newLocalEnd || remoteHasMore
            )
        } catch (e: Exception) {
            _activitiesError.value = e.localizedMessage ?: "Failed to load more"
        }
    }

    /** 4) Carrega estatísticas e achievements */
    fun loadStats(token: String) = viewModelScope.launch {
        _error.value = null
        if (userId == null) userId = parseUserId(token)
        if (userId == null) {
            _error.value = "Missing user ID"
            return@launch
        }

        try {
            val api = AchievementsApi.create()
            val t   = bearer(token)

            val prof = api.getProfile(userId!!, t)
            _xp.value      = prof.xp
            _level.value   = prof.level
            _achievements.value = api.listAchievements(userId!!, t).achievements

            val rep = api.getReputation(userId!!, t)
            _behaviour.value     = rep.score.toDouble()
            val counts = listOf(
                "Good teammate" to (rep.good_teammate_count ?: 0),
                "Friendly"      to (rep.friendly_count    ?: 0),
                "Team player"   to (rep.team_player_count ?: 0),
                "Watchlisted"   to (rep.toxic_count       ?: 0),
                "Bad sport"     to (rep.bad_sport_count   ?: 0),
                "Frequent AFK"  to (rep.afk_count         ?: 0)
            )
            _reputationLabel.value = counts
                .maxByOrNull { it.second }
                ?.takeIf { it.second > 0 }
                ?.let { "${it.first} (${it.second})" }
                ?: "—"
        } catch (e: Exception) {
            _error.value = e.localizedMessage
        }
    }
}
