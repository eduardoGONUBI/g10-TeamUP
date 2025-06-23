
package com.example.teamup.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject   // ignore if you don’t use DI yet

//Faz a ponte entre o DAO e o resto da app
class SessionRepository @Inject constructor(
    private val dao: SessionDao
) {
    val token: Flow<String?> = dao.observeToken()

    suspend fun save(token: String) = dao.save(SessionEntity(token = token))
    suspend fun clear()            = dao.clear()
}
