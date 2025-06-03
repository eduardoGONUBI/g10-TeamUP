// File: app/src/main/java/com/example/teamup/data/remote/AchievementsApi.kt
package com.example.teamup.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
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

