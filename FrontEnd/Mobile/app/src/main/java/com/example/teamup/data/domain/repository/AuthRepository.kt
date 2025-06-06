package com.example.teamup.data.domain.repository

import com.example.teamup.data.remote.model.RegisterRequestDto

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String> // returns token or error
    suspend fun register(request: RegisterRequestDto): Result<String>
    suspend fun forgotPassword(email: String): Result<String>
    suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ): Result<String>

    suspend fun changeEmail(
        token: String,
        newEmail: String,
        password: String
    ): Result<String>
}

