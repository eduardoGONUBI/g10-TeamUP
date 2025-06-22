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
    val name      : String?  = null,
    val email     : String?  = null,
    val location  : String?  = null,
    val latitude  : Double?  = null,
    val longitude : Double?  = null,
    val sports    : List<Int>? = null
)


data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,


    @SerializedName("password_confirmation")
    val passwordConfirmation: String,


    val location: String
)

data class GenericMessageResponseDto(
    val message: String
)
data class ForgotPasswordRequestDto(
    val email: String
)

data class ChangePasswordRequestDto(
    @SerializedName("current_password")
    val currentPassword: String,

    @SerializedName("new_password")
    val newPassword: String,


    @SerializedName("new_password_confirmation")
    val newPasswordConfirmation: String
)
data class ChangeEmailRequestDto(
    @SerializedName("new_email")
    val newEmail: String,

    val password: String
)
