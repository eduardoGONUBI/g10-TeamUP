package com.example.teamup.data.remote.model


data class AchievementDto(
    val code: String,
    val title: String,
    val description: String,
    val icon: String,
)

data class ProfileResponse(
    val xp: Int,
    val level: Int
)


data class AchievementsResponse(
    val achievements: List<AchievementDto>
)



