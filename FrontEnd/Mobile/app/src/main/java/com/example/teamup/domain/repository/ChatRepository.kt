package com.example.teamup.domain.repository

import com.example.teamup.domain.model.Message

interface ChatRepository {
    suspend fun fetchMessages(token: String, eventId: Int): List<Message>
    suspend fun sendMessage(token: String, eventId: Int, text: String): Message
}
