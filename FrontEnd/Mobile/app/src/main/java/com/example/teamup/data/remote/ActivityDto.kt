package com.example.teamup.data.remote

data class ActivityDto(
    val id: Int,
    val name: String,
    val sport: String,
    val date: String,
    val place: String,
    val status: String,
    val max_participants: Int,
    val creator: CreatorDto,
    val weather: WeatherDto,
    val participants: List<ParticipantDto>,
    val latitude: Double,
    val longitude: Double
)

data class CreatorDto(val id: Int, val name: String)
data class ParticipantDto(val id: Int, val name: String, val rating: Double?)
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
    val sport: String,
    val date: String,
    val max_participants: Int,
    val place: String
)
<<<<<<< Updated upstream
=======

data class SportDto(val id: Int, val name: String)

>>>>>>> Stashed changes
