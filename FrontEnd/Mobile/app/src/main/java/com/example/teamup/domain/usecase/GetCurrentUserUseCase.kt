// File: app/src/main/java/com/example/teamup/domain/usecase/GetCurrentUserUseCase.kt
package com.example.teamup.domain.usecase

import com.example.teamup.domain.model.User
import com.example.teamup.domain.repository.UserRepository

/**
 * Use-case that fetches the current user from the repository,
 * which already returns a domain User.
 */
class GetCurrentUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(token: String): User {
        // Now repository.getMe() returns User directly—no DTO here.
        return repository.getMe(token)
    }
}
