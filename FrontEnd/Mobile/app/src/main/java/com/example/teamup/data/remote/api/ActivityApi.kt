package com.example.teamup.data.remote.api


import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.CreateEventRawResponse
import com.example.teamup.data.remote.model.CreateEventRequestDto
import com.example.teamup.data.remote.model.EventUpdateRequest
import com.example.teamup.data.remote.model.PaginatedResponse
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

    // minhas ativities
    @GET("/api/events/mine")
    suspend fun getMyActivities(
        @Header("Authorization") token: String,
        @Query("page")           page:  Int = 1,
        @Query("per_page")       per:   Int = 7
    ): PaginatedResponse<ActivityDto>

    // atividades criados do utilizador
    @GET("/api/events/{id}")
    suspend fun getEventDetail(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): ActivityDto

    // apagar atividade
    @DELETE("/api/events/{id}")
    suspend fun deleteActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>


    // remover participante
    @DELETE("/api/events/{eventId}/participants/{participantId}")
    suspend fun kickParticipant(
        @Header("Authorization") token: String,
        @Path("eventId") eventId: Int,
        @Path("participantId") participantId: Int
    ): Response<Unit>

    // sair da atividade
    @DELETE("/api/events/{id}/leave")
    suspend fun leaveEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // update atividade
    @PUT("/api/events/{id}")
    suspend fun updateActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body updatedEvent: EventUpdateRequest
    ): Response<Unit>


    // procurar atividade
    @GET("/api/events/search")
    suspend fun searchEvents(
        @Header("Authorization") token: String,
        @Query("page")           page:  Int = 1,
        @Query("per_page")       per:   Int = 15,
        @Query("name")  name:  String? = null,
        @Query("sport") sport: String? = null,
        @Query("place") place: String? = null,
        @Query("date")  date:  String? = null
    ): PaginatedResponse<ActivityDto>

    // lista de desportos
    @GET("/api/sports")
    suspend fun getSports(
        @Header("Authorization") token: String
    ): List<SportDto>

// criar atividade
    @POST("/api/events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Body body: CreateEventRequestDto
    ): CreateEventRawResponse

 // join atividade
    @POST("/api/events/{id}/join")
    suspend fun joinEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>


companion object {
    fun create(): ActivityApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrlProvider.getBaseUrl())
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

    @GET("/api/users/{id}/events")
    suspend fun getEventsByUser(
        @Path("id") userId: Int,
        @Header("Authorization") token: String
    ): List<ActivityDto>

}