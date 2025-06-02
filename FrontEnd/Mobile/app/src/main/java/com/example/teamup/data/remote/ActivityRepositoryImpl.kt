// app/src/main/java/com/example/teamup/data/remote/ActivityRepositoryImpl.kt
package com.example.teamup.data.remote

import android.util.Base64
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.model.CreateEventRequest
import com.example.teamup.data.domain.repository.ActivityRepository
import org.json.JSONObject


/**
 * Implementation of ActivityRepository that calls ActivityApi and maps responses into ActivityItem.
 */
class ActivityRepositoryImpl(
    private val api: ActivityApi
) : ActivityRepository {

    /* ─── Public API ───────────────────────────────────────────────────────── */

    override suspend fun getMyActivities(token: String): List<ActivityItem> {
        val dtoList = api.getMyActivities("Bearer $token")
        val userId  = extractUserId(token)
        return dtoList.map { it.toActivityItem(userId) }
    }

    override suspend fun searchActivities(
        token: String,
        name : String?,
        sport: String?,
        place: String?,
        date : String?
    ): List<ActivityItem> {
        val dtoList = api.searchEvents(
            token = "Bearer $token",
            name  = name,
            sport = sport,
            place = place,
            date  = date
        )
        val userId = extractUserId(token)
        return dtoList.map { it.toActivityItem(userId) }
    }

    override suspend fun createActivity(
        token: String,
        body : CreateEventRequest
    ): ActivityItem {
        val raw = api.createEvent("Bearer $token", body)
        val e = raw.event
        val currentUserId = extractUserId(token)
        return ActivityItem(
            id = e.id.toString(),
            title = "${e.name} : ${e.sportId}",
            location = e.place,
            date = e.date,
            participants = 1,               // only the creator is in the event right away
            maxParticipants = e.maxParticipants,
            organizer = e.userName,
            creatorId = e.userId,
            isParticipant = true,
            latitude = e.latitude,
            longitude = e.longitude,
            isCreator = true
        )
    }

    override suspend fun getSports(token: String) =
        api.getSports("Bearer $token")


    /* ─── Helpers ──────────────────────────────────────────────────────────── */

    /**
     * Lightweight JWT parser that decodes the payload and returns the “sub” claim.
     * Works completely offline (no signature verification needed).
     */
    private fun extractUserId(token: String): Int {
        return try {
            val raw = token.removePrefix("Bearer ").trim()
            val parts = raw.split(".")
            if (parts.size < 2) return 0
            val payloadPadded = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let { s -> s + "=".repeat((4 - s.length % 4) % 4) }
            val json = JSONObject(
                String(Base64.decode(payloadPadded, Base64.DEFAULT))
            )
            json.optInt("sub", 0)
        } catch (_: Exception) {
            0
        }
    }
}

/* ─── DTO → Domain mapper ───────────────────────────────────────────────── */

private fun ActivityDto.toActivityItem(currentUserId: Int): ActivityItem {
    val participantIds = participants?.map { it.id }?.toSet() ?: emptySet()

    return ActivityItem(
        id              = id.toString(),
        title           = "$name : $sport",
        location        = place,
        date            = date,
        participants    = participants?.size ?: 0,
        maxParticipants = max_participants,
        organizer       = creator.name,
        creatorId       = creator.id,
        isParticipant   = participantIds.contains(currentUserId),
        latitude        = latitude,
        longitude       = longitude,
        isCreator       = (creator.id == currentUserId)
    )
}
