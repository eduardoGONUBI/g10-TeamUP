package com.example.teamup.domain.repository

import com.example.teamup.domain.model.Sport
import com.example.teamup.domain.model.UpdateUserRequestDomain
import com.example.teamup.domain.model.User
import okhttp3.MultipartBody


interface UserRepository {
    suspend fun getMe(token: String): User
    suspend fun updateMe(token: String, body: UpdateUserRequestDomain): User
    suspend fun deleteMe(token: String)
    suspend fun getAllSports(token: String): List<Sport>
    suspend fun uploadAvatar(token: String, part: MultipartBody.Part): User
}
