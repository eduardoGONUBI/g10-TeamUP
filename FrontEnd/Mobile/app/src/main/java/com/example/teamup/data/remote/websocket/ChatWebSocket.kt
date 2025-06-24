package com.example.teamup.data.remote.websocket

import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.domain.model.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// conecta websocket com o chat
class ChatWebSocket(
    private val token: String,
    private val eventId: Int,
    private val myUserId: Int
) {

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null


    private val _incoming = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incoming: SharedFlow<Message> = _incoming

    fun connect() {
        val request = Request.Builder()

            .url("${BaseUrlProvider.getWsUrl()}?token=$token")
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


    fun sendOverWebSocket(raw: String) {
        socket?.send(raw)
    }


// le o json da mensagem recebida e converte
    private fun parseAndEmit(raw: String) {
        try {
            val json = JSONObject(raw)

            if (json.getInt("event_id") != eventId) return

            val msg = Message(
                id = json.optInt("id"),
                eventId = json.getInt("event_id"),
                userId = json.getInt("user_id"),
                author = json.getString("user_name"),
                text = json.getString("message"),
                timestamp = json.getString("timestamp"),
                fromMe = json.getInt("user_id") == myUserId
            )

            _incoming.tryEmit(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}