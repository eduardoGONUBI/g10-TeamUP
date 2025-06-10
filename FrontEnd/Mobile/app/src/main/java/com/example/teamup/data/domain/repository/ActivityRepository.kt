package com.example.teamup.data.domain.repository

import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.model.ChatItem
import com.example.teamup.data.domain.model.CreateEventRequest
import com.example.teamup.data.remote.model.SportDto

interface ActivityRepository {

    /** /events/mine – páginas do histórico do usuário */
    suspend fun getMyActivities(
        token: String,
        page: Int = 1
    ): List<ActivityItem>

    /** /events/search – busca filtrada com paginação */
    suspend fun searchActivities(
        token:   String,
        page:    Int    = 1,
        perPage: Int    = 15,     // ⬅️ adiciona aqui
        name:    String? = null,
        sport:   String? = null,
        place:   String? = null,
        date:    String? = null
    ): List<ActivityItem>

    /** /events – lista geral (paginada) */
    suspend fun getAllEvents(
        token:   String,
        page:    Int = 1,
        perPage: Int = 15          // ⬅️ e aqui
    ): List<ActivityItem>

    /** cria um evento */
    suspend fun createActivity(
        token: String,
        body:  CreateEventRequest
    ): ActivityItem

    /** lista de esportes */
    suspend fun getSports(token: String): List<SportDto>

    /** myChats reusa /events/mine para chat */
    suspend fun myChats(token: String): List<ChatItem>

    /** indica se ainda há próxima página após a última chamada */
    val hasMore: Boolean
}
