package com.example.teamup.network

import com.example.teamup.model.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around OkHttp’s WebSocket that exposes an idiomatic Kotlin Flow
 * for new messages.  Connect once, collect forever.
 */
class ChatWebSocket(
    private val token: String,
    private val eventId: Int,
    private val myUserId: Int
) {

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null

    // Buffer up to 64 uncollected messages without suspending the sender
    private val _incoming = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incoming: SharedFlow<Message> = _incoming

    fun connect() {
        val request = Request.Builder()
            // NOTE: 10.0.2.2 = “localhost” from the Android emulator
            .url("ws://10.0.2.2:55333/?token=$token")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) =
                parseAndEmit(text)

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) =
                parseAndEmit(bytes.utf8())

            override fun onFailure(ws: WebSocket, t: Throwable, res: Response?) {
                t.printStackTrace()
            }
        })
    }

    fun disconnect() {
        socket?.close(1000, null)
    }

    /** If you ever want to push directly over WS instead of POST’ing. */
    fun sendOverWebSocket(raw: String) {
        socket?.send(raw)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses a raw JSON string from the server and, if it belongs to `eventId`,
     * emits it into [_incoming].  Any exceptions are caught and logged.
     */
    private fun parseAndEmit(raw: String) {
        try {
            val json = JSONObject(raw)

            // Ignore messages for other events
            if (json.getInt("event_id") != eventId) return

            val msg = Message(
                id        = json.optInt("id"),
                eventId   = json.getInt("event_id"),
                userId    = json.getInt("user_id"),
                author    = json.getString("user_name"),
                text      = json.getString("message"),
                timestamp = json.getString("timestamp"),
                fromMe    = json.getInt("user_id") == myUserId
            )

            _incoming.tryEmit(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
