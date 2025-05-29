package com.example.teamup.data.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String> // returns token or error
}
