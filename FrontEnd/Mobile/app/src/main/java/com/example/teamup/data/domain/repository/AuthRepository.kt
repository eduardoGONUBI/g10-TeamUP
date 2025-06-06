package com.example.teamup.data.domain.repository

import com.example.teamup.data.remote.model.RegisterRequestDto

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String> // returns token or error
    suspend fun register(request: RegisterRequestDto): Result<String>
    suspend fun forgotPassword(email: String): Result<String>
}

