// src/main/java/com/example/teamup/data/local/SessionDao.kt
package com.example.teamup.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    /** Emits the current token or null (table empty) and keeps observing. */
    @Query("SELECT token FROM session WHERE id = 0")
    fun observeToken(): Flow<String?>

    /** Insert or overwrite the single row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(session: SessionEntity)

    @Query("DELETE FROM session WHERE id = 0")
    suspend fun clear()
}
