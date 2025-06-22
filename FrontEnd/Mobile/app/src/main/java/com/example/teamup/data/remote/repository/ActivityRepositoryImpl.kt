package com.example.teamup.data.remote.repository

import android.util.Base64
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.mapper.toDomain
import com.example.teamup.data.remote.mapper.toDto
import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.model.Chat
import com.example.teamup.domain.model.CreateEventRequestDomain
import com.example.teamup.domain.model.Sport
import com.example.teamup.domain.repository.ActivityRepository
import org.json.JSONObject

class ActivityRepositoryImpl(
    private val api: ActivityApi
) : ActivityRepository {

    override var hasMore: Boolean = true

    private fun auth(token: String): String =
        if (token.trim().startsWith("Bearer ")) token.trim()
        else "Bearer ${'$'}{token.trim()}"

     fun extractUserId(token: String): Int {
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

    override suspend fun getMyActivities(token: String, page: Int): List<Activity> {
        val uid = extractUserId(token)
        val resp = api.getMyActivities(auth(token), page)
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        return resp.data.map { it.toDomain(currentUserId = uid) }
    }

    override suspend fun searchActivities(
        token: String,
        page: Int,
        perPage: Int,
        name: String?,
        sport: String?,
        place: String?,
        date: String?
    ): List<Activity> {
        val uid = extractUserId(token)
        val resp = api.searchEvents(
            auth(token),
            page = page,
            per = perPage,
            name = name,
            sport = sport,
            place = place,
            date = date
        )
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        return resp.data.map { it.toDomain(currentUserId = uid) }
    }

    override suspend fun createActivity(
        token: String,
        body: CreateEventRequestDomain
    ): Activity {
        // Mapping inline to preserve existing behavior
        val dto = body.toDto()                           // <-- convert
        val raw = api.createEvent(auth(token), dto)
        val e = raw.event
        val uid = extractUserId(token)
        return Activity(
            id = e.id.toString(),
            title = "${'$'}{e.name} : ${'$'}{e.sportId}",
            location = e.place,
            startsAt = e.startsAt ?: body.startsAt,
            participants = 1,
            maxParticipants = e.maxParticipants,
            organizer = e.userName,
            creatorId = e.userId,
            isParticipant = true,
            latitude = e.latitude,
            longitude = e.longitude,
            isCreator = true,
            status = e.status
        )
    }

    override suspend fun getSports(token: String): List<Sport> =
        api.getSports(auth(token))
            .map { it.toDomain() }

    override suspend fun getAllEvents(
        token: String,
        page: Int,
        perPage: Int
    ): List<Activity> {
        val uid = extractUserId(token)
        val resp = api.searchEvents(auth(token), page = page, per = perPage)
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        return resp.data
            .map { it.toDomain(currentUserId = uid) }
            .filter { it.status != "concluded" }
    }

    override suspend fun myChats(
        token: String,
        page: Int
    ): List<Chat> {
        val uid = extractUserId(token)
        val resp = api.getMyActivities(auth(token), page = page)
        hasMore = resp.meta.currentPage < resp.meta.lastPage
        // Inline mapping for Chat
        return resp.data.map {
            Chat(
                id = it.id,
                title = it.name,
                sport = it.sport,
                status = it.status,
                isCreator = (it.creator.id == uid),
                isParticipant = it.participants?.any { p -> p.id == uid } ?: false,
                startsAt = it.startsAt ?: it.date.orEmpty()
            )
        }
    }
}
