package com.example.teamup.data.remote.model

data class UserDto(
    val name: String,
    val email: String,
    val location: String?,
    val sports:   List<SportDto>?
)


data class PublicUserDto(
    val id: Int,
    val name: String,
    val avatar_url: String?,
    val location: String?,
    val sports: List<SportDto>?
)
