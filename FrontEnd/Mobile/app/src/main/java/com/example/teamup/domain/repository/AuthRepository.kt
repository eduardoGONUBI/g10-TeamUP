package com.example.teamup.domain.repository

import com.example.teamup.domain.model.RegisterRequestDomain

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
    suspend fun register(request: RegisterRequestDomain): Result<String>
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

