package com.example.teamup.domain.repository

import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.model.Chat
import com.example.teamup.domain.model.CreateEventRequestDomain
import com.example.teamup.domain.model.Sport

interface ActivityRepository {


    suspend fun getMyActivities(
        token: String,
        page:  Int = 1
    ): List<Activity>


    suspend fun searchActivities(
        token:   String,
        page:    Int               = 1,
        perPage: Int               = 15,
        name:    String?           = null,
        sport:   String?           = null,
        place:   String?           = null,
        date:    String?           = null
    ): List<Activity>


    suspend fun getAllEvents(
        token:   String,
        page:    Int               = 1,
        perPage: Int               = 15
    ): List<Activity>


    suspend fun createActivity(
        token: String,
        body:  CreateEventRequestDomain
    ): Activity


    suspend fun getSports(token: String): List<Sport>


    suspend fun myChats(
        token: String,
        page:  Int                  = 1
    ): List<Chat>

    /** indica se ainda há próxima página após a última chamada */
    val hasMore: Boolean
}
