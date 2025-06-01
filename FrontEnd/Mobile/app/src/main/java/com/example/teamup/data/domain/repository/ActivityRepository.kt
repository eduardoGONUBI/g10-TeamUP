package com.example.teamup.data.domain.repository

import com.example.teamup.data.remote.ActivityDto
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.model.CreateEventRequest
import com.example.teamup.data.remote.SportDto

interface ActivityRepository {

    /**  /events/mine  – user’s own + joined events  */
    suspend fun getMyActivities(token: String): List<ActivityItem>

    /**  /events/search  – filtered search  */
    suspend fun searchActivities(
        token: String,
        name:  String? = null,
        sport: String? = null,
        place: String? = null,
        date:  String? = null
    ): List<ActivityItem>

    suspend fun createActivity(token: String, body: CreateEventRequest): ActivityItem
    suspend fun getSports(token: String): List<SportDto>
}
