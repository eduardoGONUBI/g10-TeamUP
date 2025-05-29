package com.example.teamup.data.domain.usecase

import com.example.teamup.data.domain.model.User
import com.example.teamup.data.domain.repository.UserRepository

class GetCurrentUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): User {
        return repository.getCurrentUser()
    }
}