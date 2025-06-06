package com.example.teamup.data.remote.api

import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.model.LoginRequestDto
import com.example.teamup.data.remote.model.LoginResponseDto
import com.example.teamup.data.remote.model.PublicUserDto
import com.example.teamup.data.remote.model.RegisterRequestDto
import com.example.teamup.data.remote.model.RegisterResponseDto
import com.example.teamup.data.remote.model.UpdateUserRequest
import com.example.teamup.data.remote.model.UserDto
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

interface AuthApi {

    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<RegisterResponseDto>

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

    /** PUT /api/auth/update  – partial updates */
    @PUT("/api/auth/update")
    suspend fun updateMe(
        @Header("Authorization") auth: String,
        @Body body: UpdateUserRequest
    ): UserDto                                              // ← returns the fresh user

    /** DELETE /api/auth/delete – deletes the logged-in user */
    @DELETE("/api/auth/delete")
    suspend fun deleteMe(
        @Header("Authorization") auth: String
    )


    // ------------------------------------------------------------------

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