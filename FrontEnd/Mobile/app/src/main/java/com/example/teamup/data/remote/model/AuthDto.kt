package com.example.teamup.data.remote.model

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    @SerializedName("access_token")
    val token: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_in")
    val expiresIn: Int,

    val payload: Payload
)

data class Payload(
    val id: Int,
    val name: String
)

data class PublicUserDto(
    val id: Int,
    val name: String,
    val avatar_url: String?,
    val location: String?,
    val sports: List<SportDto>?
)

data class SportDto(
    val id: Int,
    val name: String
)
