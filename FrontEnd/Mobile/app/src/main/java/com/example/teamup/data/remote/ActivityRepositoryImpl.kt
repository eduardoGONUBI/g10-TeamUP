// app/src/main/java/com/example/teamup/data/remote/ActivityRepositoryImpl.kt
package com.example.teamup.data.remote.Repository

import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.remote.model.ActivityDto


/* ─── DTO → Domain mapper ───────────────────────────────────────────────── */

internal fun ActivityDto.toActivityItem(currentUserId: Int): ActivityItem {
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
        isCreator       = (creator.id == currentUserId),
        status          = status
    )
}
