package com.example.teamup.data.remote.model

import com.example.teamup.data.domain.model.User

data class UserDto(
    val name: String,
    val email: String
) {
    fun toUser(): User {
        return User(name = name, email = email)
    }
}