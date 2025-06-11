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
    val participantIds = participants?.map { it.id }?.toSet() ?: emptySet()
    return ChatItem(
        id            = id,
        title         = name,
        sport         = sport,
        status        = status,
        isCreator     = (creator.id == currentUserId),
        isParticipant = participantIds.contains(currentUserId)
    )
}

/* ─── Repository implementation ───────────────────────────────────────────────── */

class ActivityRepositoryImpl(
    private val api: ActivityApi
) : ActivityRepository {

    override var hasMore: Boolean = true

    private fun auth(token: String): String =
        if (token.trim().startsWith("Bearer ")) token.trim()
        else "Bearer ${token.trim()}"

    override suspend fun getMyActivities(
        token: String,
        page:  Int
    ): List<ActivityItem> {
        val uid   = extractUserId(token)
        val resp  = api.getMyActivities(auth(token), page = page)
        hasMore   = resp.meta.currentPage < resp.meta.lastPage
        return resp.data.map { it.toActivityItem(uid) }
    }

    override suspend fun searchActivities(
        token:    String,
        page:     Int,
        perPage:  Int,
        name:     String?,
        sport:    String?,
        place:    String?,
        date:     String?
    ): List<ActivityItem> {
        val uid  = extractUserId(token)
        val resp = api.searchEvents(
            auth(token),
            page    = page,
            per     = perPage,
            name    = name,
            sport   = sport,
            place   = place,
            date    = date
        )
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        return resp.data.map { it.toActivityItem(uid) }
    }

    override suspend fun createActivity(
        token: String,
        body: CreateEventRequest
    ): ActivityItem {
        val raw = api.createEvent("Bearer $token", body)
        val e   = raw.event
        val currentUserId = extractUserId(token)
        return ActivityItem(
            id              = e.id.toString(),
            title           = "${e.name} : ${e.sportId}",
            location        = e.place,
            startsAt        = e.startsAt ?: body.startsAt,
            participants    = 1,
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

    override suspend fun getSports(token: String): List<SportDto> =
        api.getSports("Bearer $token")

    override suspend fun getAllEvents(
        token:   String,
        page:    Int,
        perPage: Int
    ): List<ActivityItem> {
        val uid  = extractUserId(token)
        val resp = api.searchEvents(auth(token), page = page, per = perPage)
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        return resp.data
            .map { it.toActivityItem(uid) }
            .filter { it.status != "concluded" }
    }

    override suspend fun myChats(
        token: String,
        page:  Int
    ): List<ChatItem> {
        val uid  = extractUserId(token)
        val resp = api.getMyActivities(auth(token), page = page)
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        return resp.data.map { it.toChatItem(uid) }
    }

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
            val json    = JSONObject(String(decoded))
            json.optInt("sub", 0)
        } catch (_: Exception) {
            0
        }
    }
}
