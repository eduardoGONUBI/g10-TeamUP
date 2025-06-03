package com.example.teamup.data.remote.api

import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.model.AchievementsResponse
import com.example.teamup.data.remote.model.FeedbackRequestDto
import com.example.teamup.data.remote.model.ProfileResponse
import com.example.teamup.data.remote.model.ReputationResponse
import com.example.teamup.data.remote.model.UserAverageResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the Achievements/XP service.
 * GET /profile/{user_id} → { xp: Int, level: Int }
 */
interface AchievementsApi {

    @GET("/api/profile/{user_id}")
    suspend fun getProfile(
        @Path("user_id") userId: Int,
        @Header("Authorization") authHeader: String
    ): ProfileResponse

    // NEW – unlocked achievements
    @GET("/api/achievements/{user_id}")
    suspend fun listAchievements(
        @Path("user_id") userId: Int,
        @Header("Authorization") auth: String
    ): AchievementsResponse

    // NEW – reputation score & badges
    @GET("/api/rating/{user_id}")
    suspend fun getReputation(
        @Path("user_id") userId: Int,
        @Header("Authorization") auth: String
    ): ReputationResponse

    // NEW – behaviour index (global average)
    @GET("/api/userAverage/{user_id}")
    suspend fun getUserAverage(
        @Path("user_id") userId: Int,
        @Header("Authorization") auth: String
    ): UserAverageResponse

    @POST("api/events/{event_id}/feedback")
    suspend fun giveFeedback(
        @Header("Authorization") bearer: String,
        @Path("event_id") eventId: Int,
        @Body body: FeedbackRequestDto
    ): Response<Void>




    companion object {
        fun create(): AchievementsApi {
            return Retrofit.Builder()
                .baseUrl(BaseUrlProvider.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AchievementsApi::class.java)
        }
    }
}