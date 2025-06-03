// File: app/src/main/java/com/example/teamup/presentation/profile/PublicProfileViewModel.kt
package com.example.teamup.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for loading *another* user's public profile (read‐only).
 * Fetches:
 *   - /api/users/{id}          → name, avatar_url, location
 *   - /api/profile/{id}        → xp + level
 *   - /api/rating/{id}         → rep counts + score
 *   - /api/achievements/{id}   → unlocked achievements
 */
class PublicProfileViewModel(
    private val userId: Int,
    private val bearer: String   // must be "Bearer <token>"
) : ViewModel() {

    private val authApi  = AuthApi.create()
    private val achApi   = AchievementsApi.create()

    private val _name         = MutableStateFlow("Loading…")
    val name: StateFlow<String> = _name

    private val _avatarUrl    = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl

    private val _location     = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location

    private val _level        = MutableStateFlow(0)
    val level: StateFlow<Int> = _level

    private val _behaviour    = MutableStateFlow<Int?>(null)
    val behaviour: StateFlow<Int?> = _behaviour

    private val _repLabel     = MutableStateFlow("—")
    val repLabel: StateFlow<String> = _repLabel

    private val _achievements = MutableStateFlow<List<AchievementDto>>(emptyList())
    val achievements: StateFlow<List<AchievementDto>> = _achievements

    private val _error        = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadPublicProfile()
    }

    private fun loadPublicProfile() = viewModelScope.launch {
        try {
            // ───── 1) GET /api/users/{id} ──────────────────────────────────────────
            val publicUser: PublicUserDto = authApi.getUser(userId, bearer)
            _name.value = publicUser.name
            _avatarUrl.value = publicUser.avatar_url
            _location.value = publicUser.location

            // ───── 2) GET /api/profile/{id}  (xp + level) ────────────────────────
            val profile: ProfileResponse = achApi.getProfile(userId, bearer)
            _level.value = profile.level

            // ───── 3) GET /api/rating/{id}  (score + counts) ────────────────────
            val rep: ReputationResponse = achApi.getReputation(userId, bearer)
            _behaviour.value = rep.score

            // Compute “top feedback” label
            val counts: List<Pair<String, Int>> = listOf(
                "Good teammate"   to (rep.good_teammate_count   ?: 0),
                "Friendly player" to (rep.friendly_count        ?: 0),
                "Team player"     to (rep.team_player_count     ?: 0),
                "Watchlisted"     to (rep.toxic_count           ?: 0),
                "Bad sport"       to (rep.bad_sport_count       ?: 0),
                "Frequent AFK"    to (rep.afk_count             ?: 0)
            )
            val top: Pair<String, Int>? = counts.maxByOrNull { it.second }
            _repLabel.value = if (top != null && top.second > 0) {
                "${top.first} (${top.second})"
            } else {
                "—"
            }

            // ───── 4) GET /api/achievements/{id} ─────────────────────────────────
            val achList: AchievementsResponse = achApi.listAchievements(userId, bearer)
            _achievements.value = achList.achievements

        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
