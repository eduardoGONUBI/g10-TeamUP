package com.example.teamup.data.remote

import com.example.teamup.data.domain.model.CreateEventRequest
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
interface ActivityApi {

    @GET("/api/events/mine")
    suspend fun getMyActivities(
        @Header("Authorization") token: String
    ): List<ActivityDto>

    @GET("/api/events/{id}")
    suspend fun getEventDetail(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): ActivityDto

    @DELETE("/api/events/{id}")
    suspend fun deleteActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    @GET("/api/events")
    suspend fun getAllEvents(
        @Header("Authorization") token: String
    ): List<ActivityDto>

    @DELETE("/api/events/{eventId}/participants/{participantId}")
    suspend fun kickParticipant(
        @Header("Authorization") token: String,
        @Path("eventId") eventId: Int,
        @Path("participantId") participantId: Int
    ): Response<Unit>

    @DELETE("/api/events/{id}/leave")
    suspend fun leaveEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    @PUT("/api/events/{id}")
    suspend fun updateActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body updatedEvent: EventUpdateRequest
    ): Response<Unit>

    @GET("/api/events/search")
    suspend fun searchEvents(
        @Header("Authorization") token: String,
        @Query("name")  name:  String? = null,
        @Query("sport") sport: String? = null,
        @Query("place") place: String? = null,
        @Query("date")  date:  String? = null
    ): List<ActivityDto>

    @GET("/api/sports")
    suspend fun getSports(
        @Header("Authorization") token: String
    ): List<SportDto>

    /**
     * The server’s response to POST /api/events
     */
    @POST("/api/events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Body body: CreateEventRequest
    ): ActivityDto

    /**
     * Join an event.
     * */
    @POST("/api/events/{id}/join")
    suspend fun joinEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    companion object {
        fun create(): ActivityApi {
            // ─── 1) (Optional) Logging interceptor ────────────────────────────
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            // ─── 2) Build a custom OkHttpClient with 30-second timeouts ───────
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)                                // optional: logs request/response
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            // ─── 3) Create Retrofit using that OkHttpClient ─────────────────
            return Retrofit.Builder()
                .baseUrl(BaseUrlProvider.getBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ActivityApi::class.java)
        }
    }
}



