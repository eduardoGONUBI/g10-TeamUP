package com.example.teamup.data.remote.model

import com.google.gson.annotations.SerializedName


data class MessageDto(
    val id: Int?,
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("user_id")  val userId: Int,
    val author: String,
    val text: String,
    val timestamp: String
)
