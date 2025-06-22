package com.example.teamup.data.remote.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ActivityDto(
    val id: Int,
    val name: String,
    val sport: String,
    @SerializedName("starts_at") val startsAt: String?,   // ← can be null if backend still sends “date”
    @SerializedName("date")      val date: String? = null,
    val place: String,
    val status: String,
    val max_participants: Int,
    val creator: CreatorDto,
    val weather: WeatherDto,
    val participants: List<ParticipantDto>?,
    val latitude: Double,
    val longitude: Double
)

data class CreatorDto(val id: Int, val name: String)

data class ParticipantDto(
    val id: Int,
    val name: String,
    val level: Int? = null,
    val rating: Double? = null
)
data class ParticipantUi(
    val id: Int,
    val name: String,
    val isCreator: Boolean,
    val level: Int,
    val feedbackGiven: String? = null
)

data class WeatherDto(
    val app_max_temp: String,
    val app_min_temp: String,
    val temp: String,
    val high_temp: String,
    val low_temp: String,
    val description: String
)

data class EventUpdateRequest(
    val name: String,
    val sport_id: Int,
    val date: String,
    val max_participants: Int,
    val place: String
)




data class CreateEventRawResponse(
    val message: String,
    val event: CreateEventRawDto
)

data class CreateEventRawDto(
    val id: Int,
    val name: String,
    @SerializedName("sport_id")
    val sportId: Int,
    @SerializedName("starts_at")
    val startsAt: String,
    val place: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_name")
    val userName: String,
    val status: String,
    @SerializedName("max_participants")
    val maxParticipants: Int,
    val latitude: Double,
    val longitude: Double,
    val weather: JsonElement
)

data class CreateEventRequestDto(
    val name: String,
    @SerializedName("sport_id")         val sportId: Int,
    val place: String,
    @SerializedName("max_participants") val maxParticipants: Int,
    @SerializedName("starts_at")        val startsAt: String,
    val latitude:  Double?,
    val longitude: Double?
)

data class StatusUpdateRequest(
    val status: String
)

data class SportDto(
    val id: Int,
    val name: String
)
