package com.example.teamup.model

/**
 * Chat message coming from the backend.
 *
 * @param fromMe indicates if it was authored by the current user
 *        (calculated on the client – not returned by the server).
 */
data class Message(
    val id:        Int?    = null,
    val eventId:   Int,
    val userId:    Int,
    val author:    String,
    val text:      String,
    val timestamp: String,
    val fromMe:    Boolean = false
)
