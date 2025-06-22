package com.example.teamup.data.remote.api

import com.example.teamup.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ChatApi {

    private val client = OkHttpClient()

    /** POST /sendMessage/{eventId}  */
    suspend fun sendMessage(
        token: String,
        eventId: Int,
        text: String
    ) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("message", text)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("http://10.0.2.2:8082/api/sendMessage/$eventId")
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(req).execute().use { res ->
            require(res.isSuccessful) { "HTTP ${res.code}" }
        }
    }

    /** GET /fetchMessages/{eventId}  */
    // ChatApi.kt  – only the fetchHistory() function is shown here
    suspend fun fetchHistory(
        token: String,
        eventId: Int
    ): List<Message> = withContext(Dispatchers.IO) {

        val req = Request.Builder()
            .url("http://10.0.2.2:8082/api/fetchMessages/$eventId")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(req).execute().use { res ->
            require(res.isSuccessful) { "HTTP ${res.code}" }

            val root = JSONObject(res.body!!.string())
            val arr = root.optJSONArray("messages") ?: return@use emptyList<Message>()

            buildList {
                for (i in 0 until arr.length()) {
                    val j = arr.getJSONObject(i)

                    // numeric fields arrive as strings → toIntOrNull()
                    val uid = j.optString("user_id").toIntOrNull() ?: -1
                    val eid = j.optString("event_id").toIntOrNull() ?: eventId

                    add(
                        Message(
                            id = j.optInt("id"),
                            eventId = eid,
                            userId = uid,
                            author = j.optString("user_name"),
                            text = j.optString("message"),
                            timestamp = j.optString("created_at"),   // ← HERE
                            fromMe = false                        // fixed later
                        )
                    )
                }
            }
        }
    }
}