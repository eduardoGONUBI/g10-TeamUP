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


class ActivityDetailViewModel(
    private val eventId: Int,
    private val token: String
) : ViewModel() {

    private val _event = MutableStateFlow<ActivityDto?>(null)
    val event: StateFlow<ActivityDto?> = _event

    init { // quando a view model e criado faz o fetch
        fetchEventWithLevels()
    }

    // load event e os participantes e o seus lvls
    fun fetchEventWithLevels() {
        viewModelScope.launch {
            try {
                // chama ativida pela API
                val eventsApi = ActivityApi.create()
                val rawDto: ActivityDto = eventsApi.getEventDetail(eventId, "Bearer $token")

                // API achievemetns
                val achApi: AchievementsApi = AchievementsApi.create()

                //  para cada participante vai buscar xp / level
                val enrichedParticipants: List<ParticipantDto>? = rawDto.participants
                    ?.map { participant: ParticipantDto ->
                        async {
                            val profile: ProfileResponse = try {
                                achApi.getProfile(participant.id, "Bearer $token")
                            } catch (_: Exception) {
                                ProfileResponse(xp = 0, level = 0)
                            }
                            ParticipantDto(   // reconstroi o participante com lvl e rating
                                id = participant.id,
                                name = participant.name,
                                level = profile.level,
                                rating = participant.rating
                            )
                        }
                    }
                    ?.awaitAll()

                // cria um novo DTO com a lista atualizada de participantes
                val enrichedDto = rawDto.copy(participants = enrichedParticipants)

                // atualiza o estado
                _event.value = enrichedDto
            } catch (_: Exception) {
                _event.value = null
            }
        }
    }

    enum class ActivityRole {   // define o papel do utilizador
        CREATOR,
        PARTICIPANT,
        VIEWER
    }
}
