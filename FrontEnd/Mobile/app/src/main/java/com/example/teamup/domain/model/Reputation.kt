package com.example.teamup.domain.model

data class Reputation(
    val goodTeammate: Int?,
    val friendly: Int?,
    val teamPlayer: Int?,
    val toxic: Int?,
    val badSport: Int?,
    val afk: Int?
)


data class FeedbackRequest(
    val userId: Int,
    val attribute: String
)