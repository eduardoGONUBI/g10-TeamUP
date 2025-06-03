package com.example.teamup.data.domain.model

/** Minimal fields the chat list needs */
data class ChatItem(
    val id:        Int,
    val title:     String,
    val sport:     String,
    val status:    String      // "in progress", "concluded", etc.
)
