package com.example.teamup.data.remote.model

import com.google.gson.annotations.SerializedName

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

data class FeedbackRequestDto(
    @SerializedName("user_id")  val userId:   Int,
    @SerializedName("attribute")val attribute:String
)

