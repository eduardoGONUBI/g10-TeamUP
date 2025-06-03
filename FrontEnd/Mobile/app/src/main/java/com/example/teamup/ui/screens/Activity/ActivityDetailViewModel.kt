// File: app/src/main/java/com/example/teamup/ui/screens/ActivityDetailViewModel.kt
package com.example.teamup.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.ParticipantDto
import com.example.teamup.data.remote.model.ProfileResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for showing an event’s details (Creator/Participant screens).
 *
 * 1) Fetches ActivityDto from ActivityApi.getEventDetail(...)
 * 2) For each ParticipantDto, calls AchievementsApi.getProfile(...) to get `level`
 * 3) Emits the enriched ActivityDto (with each participant’s level filled in).
 *
 * @param eventId  The ID of the event to load.
 * @param token    The JWT (without the "Bearer " prefix).
 */
class ActivityDetailViewModel(
    private val eventId: Int,
    private val token: String
) : ViewModel() {

    private val _event = MutableStateFlow<ActivityDto?>(null)
    val event: StateFlow<ActivityDto?> = _event

    init {
        fetchEventWithLevels()
    }

    /**
     * 1) Fetch raw event from the Events API.
     * 2) For each participant, fetch ProfileResponse from Achievements API.
     * 3) Construct a new ParticipantDto with level = profile.level.
     * 4) Emit the new ActivityDto with enriched participants.
     */
    fun fetchEventWithLevels() {
        viewModelScope.launch {
            try {
                // 1) Fetch the raw event from the Events service
                val eventsApi = ActivityApi.create()
                val rawDto: ActivityDto = eventsApi.getEventDetail(eventId, "Bearer $token")

                // 2) Prepare AchievementsApi to fetch each participant’s level
                val achApi: AchievementsApi = AchievementsApi.create()

                // 3) For each ParticipantDto, run an async block to call /profile/{user_id}.
                val enrichedParticipants: List<ParticipantDto>? = rawDto.participants
                    ?.map { participant: ParticipantDto ->
                        async {
                            // Attempt to fetch that user’s profile (xp + level)
                            val profile: ProfileResponse = try {
                                achApi.getProfile(participant.id, "Bearer $token")
                            } catch (_: Exception) {
                                // Fallback to level = 0 if the call fails
                                ProfileResponse(xp = 0, level = 0)
                            }
                            ParticipantDto(
                                id = participant.id,
                                name = participant.name,
                                level = profile.level,
                                rating = participant.rating
                            )
                        }
                    }
                    ?.awaitAll()

                // 4) Copy the raw ActivityDto, replacing participants with enriched list
                val enrichedDto = rawDto.copy(participants = enrichedParticipants)

                // 5) Emit to StateFlow
                _event.value = enrichedDto

            } catch (_: Exception) {
                _event.value = null
            }
        }
    }
}
