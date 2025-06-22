package com.example.teamup.domain.usecase

import com.example.teamup.domain.repository.AuthRepository

class LoginUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        return repo.login(email, password)
    }
}
