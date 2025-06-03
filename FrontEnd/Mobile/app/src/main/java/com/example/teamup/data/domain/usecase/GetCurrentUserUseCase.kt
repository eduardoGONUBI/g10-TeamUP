// app/src/main/java/com/example/teamup/data/domain/usecase/GetCurrentUserUseCase.kt
package com.example.teamup.data.domain.usecase

import com.example.teamup.data.domain.model.User
import com.example.teamup.data.domain.repository.UserRepository
import com.example.teamup.data.remote.model.UserDto

/**
 * Use‐case that fetches the “/api/auth/me” endpoint via UserRepository#getMe(token),
 * then maps the JSON→UserDto into the domain model User.
 */
class GetCurrentUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(token: String): User {
        // 1) Fetch the raw UserDto from your repository
        val dto: UserDto = repository.getMe(token)

        // 2) Map it into your domain model
        return User(
            name = dto.name,
            email = dto.email
            // …add other fields if you want to expose them in User
        )
    }
}
