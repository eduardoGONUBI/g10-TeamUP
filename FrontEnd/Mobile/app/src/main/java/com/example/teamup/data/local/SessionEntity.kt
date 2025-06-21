// src/main/java/com/example/teamup/data/local/SessionEntity.kt
package com.example.teamup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class SessionEntity(
    /** Always a single row ⇒ constant PK keeps things simple */
    @PrimaryKey val id: Int = 0,
    val token: String
)
