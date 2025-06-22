package com.example.teamup.domain.repository

import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.model.Chat
import com.example.teamup.domain.model.CreateEventRequestDomain
import com.example.teamup.domain.model.Sport

interface ActivityRepository {

    /** /events/mine – páginas do histórico do usuário */
    suspend fun getMyActivities(
        token: String,
        page:  Int = 1
    ): List<Activity>

    /** /events/search – busca filtrada com paginação */
    suspend fun searchActivities(
        token:   String,
        page:    Int               = 1,
        perPage: Int               = 15,
        name:    String?           = null,
        sport:   String?           = null,
        place:   String?           = null,
        date:    String?           = null
    ): List<Activity>

    /** /events – lista geral (paginada) */
    suspend fun getAllEvents(
        token:   String,
        page:    Int               = 1,
        perPage: Int               = 15
    ): List<Activity>

    /** cria um evento */
    suspend fun createActivity(
        token: String,
        body:  CreateEventRequestDomain
    ): Activity

    /** lista de esportes */
    suspend fun getSports(token: String): List<Sport>

    /** myChats reusa /events/mine para chat, paginado */
    suspend fun myChats(
        token: String,
        page:  Int                  = 1
    ): List<Chat>

    /** indica se ainda há próxima página após a última chamada */
    val hasMore: Boolean
}
