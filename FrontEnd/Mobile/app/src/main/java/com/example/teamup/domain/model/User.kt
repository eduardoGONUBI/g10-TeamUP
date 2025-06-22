package com.example.teamup.domain.model

data class User(
    val id: Int,
    val name: String,
    val email: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val sports: List<Sport>?,
    val avatarUrl: String?
)

data class ProfileStats(
    val xp: Int,
    val level: Int,
    val reputation: Reputation
)

data class RegisterRequestDomain(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirmation: String,
    val location: String
)
data class UpdateUserRequestDomain(
    val name: String? = null,
    val email: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sports: List<Int>? = null
)