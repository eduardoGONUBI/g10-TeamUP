// File: app/src/main/java/com/example/teamup/data/remote/Repository/UserRepositoryImpl.kt
package com.example.teamup.data.remote.Repository

import com.example.teamup.data.domain.repository.UserRepository
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.UpdateUserRequest
import com.example.teamup.data.remote.model.UserDto
import com.example.teamup.data.remote.model.SportDto
import okhttp3.MultipartBody

class UserRepositoryImpl(
    private val authApi: AuthApi
) : UserRepository {

    // We also need an ActivityApi in order to fetch “/api/sports”
    private val activityApi: ActivityApi = ActivityApi.create()

    // Helper to normalize “Bearer ” prefix
    private fun bearer(token: String): String =
        if (token.startsWith("Bearer")) token
        else "Bearer $token"

    override suspend fun getMe(token: String): UserDto {
        // The AuthApi.getCurrentUser() call will use an interceptor
        // (inside AuthApi.create()) to add the “Authorization” header.
        return authApi.getCurrentUser()
    }

    override suspend fun updateMe(token: String, body: UpdateUserRequest): UserDto {
        return authApi.updateMe(bearer(token), body)
    }

    override suspend fun deleteMe(token: String) {
        authApi.deleteMe(bearer(token))
    }

    override suspend fun getAllSports(token: String): List<SportDto> {
        // Now that activityApi is defined, we can call getSports(...)
        return activityApi.getSports(bearer(token))
    }

    override suspend fun uploadAvatar(token: String, part: MultipartBody.Part): UserDto {
        return authApi.uploadAvatar(bearer(token), part)
    }
}
