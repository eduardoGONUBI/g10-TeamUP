package com.example.teamup.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("/api/auth/me")
    suspend fun getCurrentUser(): UserDto

    /** GET /api/users/{id} – public profile (minimal fields). */
    @GET("/api/users/{id}")
    suspend fun getUser(
        @Path("id") id: Int,
        @Header("Authorization") auth: String
    ): PublicUserDto

    companion object {
        fun create(): AuthApi {
            return Retrofit.Builder()
                .baseUrl(BaseUrlProvider.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi::class.java)
        }
    }
}

