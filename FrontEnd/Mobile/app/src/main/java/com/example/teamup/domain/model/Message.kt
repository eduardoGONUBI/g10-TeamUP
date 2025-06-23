package com.example.teamup.domain.model


data class Message(
    val id:        Int?    = null,
    val eventId:   Int,
    val userId:    Int,
    val author:    String,
    val text:      String,
    val timestamp: String,
    val fromMe:    Boolean = false
)