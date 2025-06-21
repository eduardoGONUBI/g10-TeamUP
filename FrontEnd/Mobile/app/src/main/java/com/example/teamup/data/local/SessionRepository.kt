// src/main/java/com/example/teamup/data/local/SessionRepository.kt
package com.example.teamup.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject   // ignore if you don’t use DI yet

class SessionRepository @Inject constructor(
    private val dao: SessionDao
) {
    val token: Flow<String?> = dao.observeToken()

    suspend fun save(token: String) = dao.save(SessionEntity(token = token))
    suspend fun clear()            = dao.clear()
}
