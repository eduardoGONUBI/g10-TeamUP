package com.example.teamup.domain.model


data class Chat(
    val id:        Int,
    val title:     String,
    val sport:     String,
    val status:    String ,     // "in progress", "concluded", etc.
    val isCreator: Boolean = false,
    val isParticipant: Boolean = false,
    val startsAt: String,
)
