package com.example.teamup.domain.model

data class Activity(
    val id: String,
    val title: String,
    val location: String,
    val startsAt: String,
    val participants: Int,
    val maxParticipants: Int,
    val organizer: String,
    val creatorId: Int,
    val isParticipant: Boolean,
    val latitude: Double,
    val longitude: Double,
    val isCreator: Boolean,
    val status: String
)

data class CreateEventRequestDomain(
    val name: String,
    val sportId: Int,
    val place: String,
    val maxParticipants: Int,
    val startsAt: String,
    val latitude: Double,
    val longitude: Double
)

data class EventUpdate(
    val name: String,
    val sportId: Int,
    val date: String,
    val maxParticipants: Int,
    val place: String
)
data class EventStatus(val status: String)

data class Weather(
    val temp: String,
    val highTemp: String,
    val lowTemp: String,
    val description: String
)

