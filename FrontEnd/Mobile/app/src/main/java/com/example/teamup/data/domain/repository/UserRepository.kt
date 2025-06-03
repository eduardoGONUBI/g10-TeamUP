package com.example.teamup.data.domain.repository

import com.example.teamup.data.domain.model.User
import com.example.teamup.data.remote.model.SportDto
import com.example.teamup.data.remote.model.UpdateUserRequest
import com.example.teamup.data.remote.model.UserDto

interface UserRepository {
    suspend fun getMe(token: String): UserDto
    suspend fun updateMe(token: String, body: UpdateUserRequest): UserDto
    suspend fun deleteMe(token: String)
    suspend fun getAllSports(token: String): List<SportDto>
}
