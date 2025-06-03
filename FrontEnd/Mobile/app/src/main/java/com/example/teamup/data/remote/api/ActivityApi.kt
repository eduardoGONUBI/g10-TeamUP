package com.example.teamup.data.remote.api

import com.example.teamup.data.domain.model.CreateEventRequest
import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.CreateEventRawResponse
import com.example.teamup.data.remote.model.EventUpdateRequest
import com.example.teamup.data.remote.model.SportDto
import com.example.teamup.data.remote.model.StatusUpdateRequest
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    // ⚙️ Leave event endpoint
    @DELETE("/api/events/{id}/leave")
    suspend fun leaveEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // ✅ Update an event
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
    ): CreateEventRawResponse

    /**
     * Join an event.
     * */
    @POST("/api/events/{id}/join")
    suspend fun joinEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

//Stashed changes
companion object {
    fun create(): ActivityApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrlProvider.getBaseUrl())  // switch between emulator and phone
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ActivityApi::class.java)
    }
}

    @PUT("/api/events/{id}/conclude")
    suspend fun concludeByCreator(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    /** generic PATCH that only carries { status = … } */
    @PUT("/api/events/{id}")
    suspend fun updateStatus(
        @Header("Authorization")  token: String,
        @Path("id")               id: Int,
        @Body body: StatusUpdateRequest
    ): Response<Unit>
}