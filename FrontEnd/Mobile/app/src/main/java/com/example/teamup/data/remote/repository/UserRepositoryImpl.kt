package com.example.teamup.data.remote.repository

import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.mapper.toDomain
import com.example.teamup.data.remote.mapper.toDto
import com.example.teamup.domain.model.Sport
import com.example.teamup.domain.model.UpdateUserRequestDomain
import com.example.teamup.domain.model.User
import com.example.teamup.domain.repository.UserRepository
import okhttp3.MultipartBody


class UserRepositoryImpl(
    private val authApi: AuthApi,
    private val activityApi: ActivityApi
) : UserRepository {



    private fun bearer(token: String): String =
        if (token.startsWith("Bearer ")) token else "Bearer $token"



    override suspend fun getMe(token: String): User {
        val dto = authApi.getCurrentUser()
        return dto.toDomain()
    }

    // edita o profile
    override suspend fun updateMe(
        token: String,
        body: UpdateUserRequestDomain
    ): User {
        val dto = body.toDto()
        val updated = authApi.updateMe(bearer(token), dto)
        return updated.toDomain()
    }

    // apaga a conta
    override suspend fun deleteMe(token: String) {
        authApi.deleteMe(bearer(token))
    }

    // lista desportos
    override suspend fun getAllSports(token: String): List<Sport> {
        return activityApi.getSports(bearer(token))
            .map { it.toDomain() }
    }

    // muda a foto de perfil
    override suspend fun uploadAvatar(
        token: String,
        part: MultipartBody.Part
    ): User {
        val dto = authApi.uploadAvatar(bearer(token), part)
        return dto.toDomain()
    }
}
