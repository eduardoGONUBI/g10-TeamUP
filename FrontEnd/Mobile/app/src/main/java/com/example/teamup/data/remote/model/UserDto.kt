package com.example.teamup.data.remote.model

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id        : Int,
    val name      : String,
    val email     : String,
    val location  : String?,
    val latitude  : Double?,      // ← NEW
    val longitude : Double?,      // ← NEW
    val sports    : List<SportDto>?,
    @SerializedName("avatar_url")
    val avatarUrl : String?

)


data class PublicUserDto(
    val id        : Int,
    val name      : String,
    val avatar_url: String?,
    val location  : String?,
    val latitude  : Double?,      // ← NEW
    val longitude : Double?,      // ← NEW
    val sports    : List<SportDto>?
)