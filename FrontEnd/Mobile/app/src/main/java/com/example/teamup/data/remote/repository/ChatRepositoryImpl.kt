// File: app/src/main/java/com/example/teamup/data/remote/repository/ChatRepositoryImpl.kt
package com.example.teamup.data.remote.repository

import android.util.Base64
import com.example.teamup.data.remote.api.ChatApi
import com.example.teamup.domain.model.Message
import com.example.teamup.domain.repository.ChatRepository
import org.json.JSONObject

class ChatRepositoryImpl(
    private val api: ChatApi
) : ChatRepository {

    private fun bearer(token: String): String =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private fun extractUserId(token: String): Int {
        return try {
            val raw = token.removePrefix("Bearer ").trim()
            val parts = raw.split(".")
            if (parts.size < 2) return 0
            val payload = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let { s -> s + "=".repeat((4 - s.length % 4) % 4) }
            val decoded = Base64.decode(payload, Base64.DEFAULT)
            val json = JSONObject(String(decoded))
            json.optInt("sub", 0)
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun fetchMessages(token: String, eventId: Int): List<Message> {
        val uid = extractUserId(token)
        // ChatApi.fetchHistory returns List<Message> already
        return api.fetchHistory(bearer(token), eventId)
            .map { msg ->
                // mark fromMe based on userId
                msg.copy(fromMe = (msg.userId == uid))
            }
    }

    override suspend fun sendMessage(token: String, eventId: Int, text: String): Message {
        val uid = extractUserId(token)
        // sendMessage doesn’t return a Message DTO, so we fire-and-forget...
        api.sendMessage(bearer(token), eventId, text)
        // ...and return a minimal Message representing what we just sent
        return Message(
            id        = null,
            eventId   = eventId,
            userId    = uid,
            author    = "",        // you could fill in the user’s name if you know it
            text      = text,
            timestamp = "",        // or supply a timestamp if you have one
            fromMe    = true
        )
    }
}
