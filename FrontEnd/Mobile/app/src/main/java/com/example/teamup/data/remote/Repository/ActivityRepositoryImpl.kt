package com.example.teamup.data.remote.Repository

import android.util.Base64
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.model.ChatItem
import com.example.teamup.data.domain.model.CreateEventRequest
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.CreateEventRawDto
import com.example.teamup.data.remote.model.SportDto
import org.json.JSONObject

/* ─── DTO → Domain mapper ───────────────────────────────────────────────── */

internal fun ActivityDto.toActivityItem(currentUserId: Int): ActivityItem {
    // Collect all participant IDs so we can tell if “currentUserId” is a participant
    val participantIds = participants?.map { it.id }?.toSet() ?: emptySet()

    return ActivityItem(
        id              = id.toString(),
        title           = "$name : $sport",
        location        = place,
        startsAt        = startsAt ?: date ?: "",
        participants    = participants?.size ?: 0,
        maxParticipants = max_participants,
        organizer       = creator.name,
        creatorId       = creator.id,
        isParticipant   = participantIds.contains(currentUserId),
        latitude        = latitude,
        longitude       = longitude,
        isCreator       = (creator.id == currentUserId),
        status          = status
    )
}

private fun ActivityDto.toChatItem(currentUserId: Int): ChatItem {
    // Determine if the current user is the creator:
    val creatorId = creator.id

    // Determine if the current user appears in the participant list:
    val participantIds = participants?.map { it.id }?.toSet() ?: emptySet()

    return ChatItem(
        id            = id,
        title         = name,
        sport         = sport,
        status        = status,
        isCreator     = (creatorId == currentUserId),
        isParticipant = participantIds.contains(currentUserId)
    )
}

/* ─── Repository implementation ───────────────────────────────────────────────── */

class ActivityRepositoryImpl(
    private val api: ActivityApi
) : ActivityRepository {

    /* ------------ small helper --------------- */
    private fun auth(token: String): String =
        if (token.trim().startsWith("Bearer ")) token.trim()
        else "Bearer ${token.trim()}"

    /** /api/events/mine – returns ActivityDto for each event I created or joined */
    override suspend fun getMyActivities(token: String): List<ActivityItem> {
        val currentUserId = extractUserId(token)
        return api.getMyActivities("Bearer $token")
            .map { it.toActivityItem(currentUserId) }
    }

    /** /api/events/search – returns ActivityDto matching filters */
    override suspend fun searchActivities(
        token: String,
        name:  String?,
        sport: String?,
        place: String?,
        date:  String?
    ): List<ActivityItem> {
        val currentUserId = extractUserId(token)
        return api.searchEvents(
            token = "Bearer $token",
            name  = name,
            sport = sport,
            place = place,
            date  = date
        ).map { it.toActivityItem(currentUserId) }
    }

    /** POST /api/events – create a new event, then map CreateEventRawDto → ActivityItem */
    override suspend fun createActivity(
        token: String,
        body: CreateEventRequest
    ): ActivityItem {
        // 1) call backend
        val raw = api.createEvent("Bearer $token", body)
        val e: CreateEventRawDto = raw.event

        // 2) build ActivityItem directly, since CreateEventRawDto is not an ActivityDto
        val currentUserId = extractUserId(token)
        return ActivityItem(
            id              = e.id.toString(),
            title           = "${e.name} : ${e.sportId}",
            location        = e.place,
            startsAt        = e.startsAt ?: body.startsAt,
            participants    = 1,               // creator auto‐joined
            maxParticipants = e.maxParticipants,
            organizer       = e.userName,
            creatorId       = e.userId,
            isParticipant   = true,
            latitude        = e.latitude,
            longitude       = e.longitude,
            isCreator       = true,
            status          = e.status
        )
    }

    /** GET /api/sports – returns list of SportDto */
    override suspend fun getSports(token: String): List<SportDto> =
        api.getSports("Bearer $token")

    /**
     *  myChats: reuses GET /api/events/mine
     *  → maps each ActivityDto to ChatItem
     */
    override suspend fun myChats(token: String): List<ChatItem> {
        val currentUserId = extractUserId(token)
        return api.getMyActivities("Bearer $token")
            .map { it.toChatItem(currentUserId) }
    }
    /* ─── Helpers ──────────────────────────────────────────────────────────── */

    /**
     * Offline‐only JWT parser that extracts the “sub” claim (user ID) from a Bearer token.
     * Does NOT verify the signature.
     * Returns 0 on any failure.
     */
    private fun extractUserId(token: String): Int {
        return try {
            // Remove "Bearer " prefix if present
            val raw = token.removePrefix("Bearer ").trim()

            // JWT = header.payload.signature
            val parts = raw.split(".")
            if (parts.size < 2) return 0

            // Base64‐decode the payload (second part)
            val payloadPart = parts[1]
            val padded = payloadPart
                .replace('-', '+')
                .replace('_', '/')
                .let { s -> s + "=".repeat((4 - s.length % 4) % 4) }
            val decoded = Base64.decode(padded, Base64.DEFAULT)
            val json = JSONObject(String(decoded))

            json.optInt("sub", 0)
        } catch (_: Exception) {
            0
        }
    }

    /** /api/events –HOMEPAGE -  returns every event visible to the auth user excep the concluded*/
    override suspend fun getAllEvents(token: String): List<ActivityItem> {
        val uid = extractUserId(token)

        // 1. pedir todos os eventos (sem filtros)
        val dtoList = api.searchEvents(
            token = auth(token),
            name  = null,
            sport = null,
            place = null,
            date  = null
        )

        // 2. mapear ➜ ActivityItem e descartar logo os “concluded”
        return dtoList
            .map { it.toActivityItem(uid) }
            .filter { !it.status.equals("concluded", ignoreCase = true) }
    }
}