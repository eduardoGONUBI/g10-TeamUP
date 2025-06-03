package com.example.teamup.data.remote.Repository

import com.example.teamup.data.domain.repository.AuthRepository
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.LoginRequestDto

class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            Result.success(response.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}