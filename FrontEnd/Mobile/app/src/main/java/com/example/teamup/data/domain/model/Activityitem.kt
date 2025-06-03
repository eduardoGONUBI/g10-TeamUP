package com.example.teamup.data.domain.model

import com.google.gson.annotations.SerializedName

data class ActivityItem(
    val id: String,
    val title: String,
    val location: String,
    @SerializedName("starts_at") val startsAt: String,
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

data class CreateEventRequest(
    val name: String,
    val sport_id: Int,
    val place: String,
    val max_participants: Int,
    @SerializedName("starts_at") val startsAt: String
)

