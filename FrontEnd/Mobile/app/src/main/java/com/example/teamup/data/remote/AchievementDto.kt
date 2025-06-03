package com.example.teamup.data.remote


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


data class ReputationResponse(
    val user_id: Int,
    val score: Int,
    val good_teammate_count: Int?,
    val friendly_count: Int?,
    val team_player_count: Int?,
    val toxic_count: Int?,
    val bad_sport_count: Int?,
    val afk_count: Int?
)


data class UserAverageResponse(
    val user_id: Int,
    val average_rating: Double?,
    )

