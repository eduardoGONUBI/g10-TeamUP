package com.example.teamup.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("/api/auth/me")
    suspend fun getCurrentUser(): UserDto

    companion object {
        fun create(): AuthApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:80/") // ← Android emulator alias for localhost
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(AuthApi::class.java)
        }
    }
}
