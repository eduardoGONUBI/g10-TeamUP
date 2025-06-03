package com.example.teamup.data.domain.model

data class ActivityItem(
    val id: String,
    val title: String,
    val location: String,
    val date: String,
    val participants: Int,
    val maxParticipants: Int,
    val organizer: String,
    val creatorId: Int,
    val isParticipant: Boolean,
    val latitude: Double,
    val longitude: Double,
    val isCreator: Boolean  
)

data class CreateEventRequest(
    val name: String,
    val sport_id: Int,
    val date: String,
    val place: String,
    val max_participants: Int
)

