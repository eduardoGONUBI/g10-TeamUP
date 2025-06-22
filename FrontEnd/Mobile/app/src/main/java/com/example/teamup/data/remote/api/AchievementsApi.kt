package com.example.teamup.data.remote.api

import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.model.AchievementsResponse
import com.example.teamup.data.remote.model.FeedbackRequestDto
import com.example.teamup.data.remote.model.ProfileResponse
import com.example.teamup.data.remote.model.ReputationResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

 // achievements e rating
interface AchievementsApi {

     // detalhes para o perfil
    @GET("/api/profile/{user_id}")
    suspend fun getProfile(
        @Path("user_id") userId: Int,
        @Header("Authorization") authHeader: String
    ): ProfileResponse


     // achievements desbloquados
    @GET("/api/achievements/{user_id}")
    suspend fun listAchievements(
        @Path("user_id") userId: Int,
        @Header("Authorization") auth: String
    ): AchievementsResponse


     // reputaçao
    @GET("/api/rating/{user_id}")
    suspend fun getReputation(
        @Path("user_id") userId: Int,
        @Header("Authorization") auth: String
    ): ReputationResponse

     // enviar feedback
     @POST("/api/events/{event_id}/feedback")
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