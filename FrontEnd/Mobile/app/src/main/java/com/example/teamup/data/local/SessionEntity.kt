
package com.example.teamup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

//modelo da sessao  ( id,token)
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int = 0,
    val token: String
)
