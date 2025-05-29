package com.example.teamup.data.domain.repository

import com.example.teamup.data.domain.model.User

interface UserRepository {
    suspend fun getCurrentUser(): User
}