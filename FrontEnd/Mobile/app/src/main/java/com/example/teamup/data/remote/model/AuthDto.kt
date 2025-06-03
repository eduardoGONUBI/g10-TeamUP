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



data class UpdateUserRequest(
    val name: String?        = null,
    val location: String?    = null,
    val sports: List<Int>?   = null      // back-end expects an array of sport IDs
)
