// app/src/main/java/com/example/teamup/data/remote/ActivityRepositoryImpl.kt
package com.example.teamup.data.remote

import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.model.CreateEventRequest
import com.example.teamup.data.domain.repository.ActivityRepository

/**
 * Implementation of ActivityRepository that calls ActivityApi and maps responses into ActivityItem.
 */
class ActivityRepositoryImpl(
    private val api: ActivityApi
) : ActivityRepository {

    override suspend fun getMyActivities(token: String): List<ActivityItem> {
        val dtoList: List<ActivityDto> = api.getMyActivities("Bearer $token")
        return dtoList.map { it.toActivityItem() }
    }

    override suspend fun searchActivities(
        token: String,
        name: String?,
        sport: String?,
        place: String?,
        date: String?
    ): List<ActivityItem> {
        val dtoList = api.searchEvents(
            token = "Bearer $token",
            name  = name,
            sport = sport,
            place = place,
            date  = date
        )
        return dtoList.map { it.toActivityItem() }
    }

    override suspend fun createActivity(
        token: String,
        body: CreateEventRequest
    ): ActivityItem {
        // We assume ActivityApi.createEvent(...) returns an ActivityDto directly
        val dto: ActivityDto = api.createEvent("Bearer $token", body)
        return dto.toActivityItem()
    }

    override suspend fun getSports(token: String): List<SportDto> =
        api.getSports("Bearer $token")
}

/**
 * Extension function to convert ActivityDto → ActivityItem, guarding against any null fields.
 */
private fun ActivityDto.toActivityItem(): ActivityItem {
    return ActivityItem(
        // ID is non‐null, so just toString()
        id              = this.id.toString(),
        // title should never be null – supply empty strings if server somehow left them null
        title           = "${ this.name ?: "" } : ${ this.sport ?: "" }",
        // location must not be null (ActivityItem expects a non‐null String)
        location        = this.place ?: "",
        // date must not be null
        date            = this.date ?: "",
        // participants list might be null, so default to zero
        participants    = this.participants?.size ?: 0,
        // max_participants is Int, assume server always sends it
        maxParticipants = this.max_participants,
        // creator may be null, so guard‐rail with empty string / zero
        organizer       = this.creator?.name ?: "",
        creatorId       = this.creator?.id   ?: 0,
        // isCreator you may compute later; leave as false for now
        isCreator       = false,
        // latitude/longitude might be nullable in DTO; default to 0.0 if so
        latitude        = this.latitude  ?: 0.0,
        longitude       = this.longitude ?: 0.0
    )
}
