package com.example.teamup.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class CreateEventRawResponse(
    val message: String,
    val event: CreateEventRawDto
)

data class CreateEventRawDto(
    val id: Int,
    val name: String,
    @SerializedName("sport_id")
    val sportId: Int,
    val date: String,
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