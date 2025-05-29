package com.example.teamup.data.remote

import com.example.teamup.data.domain.model.User
import com.example.teamup.data.domain.repository.UserRepository

class UserRepositoryImpl(
    private val api: AuthApi
) : UserRepository {
    override suspend fun getCurrentUser(): User {
        return api.getCurrentUser().toUser()
    }
}