package com.example.teamup.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// define as operações CRUD da entidade SessionEntity
@Dao
interface SessionDao {

   //retorna o token atual
    @Query("SELECT token FROM session WHERE id = 0")
    fun observeToken(): Flow<String?>

    // insere ou substitui a linha existente
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(session: SessionEntity)

    //apaga o token
    @Query("DELETE FROM session WHERE id = 0")
    suspend fun clear()
}
