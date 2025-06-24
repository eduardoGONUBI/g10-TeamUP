package com.example.teamup.data.remote.api

import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient                     // ← NEW
import okhttp3.logging.HttpLoggingInterceptor  // ← NEW
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class StoreFcmTokenRequest(val fcm_token: String)

interface AuthApi {

    // registar
    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<GenericMessageResponseDto>

    // login
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    //  detalhes do user
    @GET("/api/auth/me")
    suspend fun getCurrentUser(): UserDto

    // guarda token firebase
    @POST("/api/store-fcm-token")
    suspend fun storeFcmToken(
        @Header("Authorization") auth: String,
        @Body body: StoreFcmTokenRequest
    ): Response<Unit>


    // perfil publico
    @GET("/api/users/{id}")
    suspend fun getUser(
        @Path("id") id: Int,
        @Header("Authorization") auth: String
    ): PublicUserDto

    // update perfil
    @PUT("/api/auth/update")
    suspend fun updateMe(
        @Header("Authorization") auth: String,
        @Body body: UpdateUserRequest
    ): UserDto

    // apagar conta
    @DELETE("/api/auth/delete")
    suspend fun deleteMe(
        @Header("Authorization") auth: String
    )

   // mudar email
    @POST("/api/password/email")
    suspend fun sendResetLink(@Body body: ForgotPasswordRequestDto): Response<GenericMessageResponseDto>

    // mudar password
    @POST("/api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") auth: String,
        @Body body: ChangePasswordRequestDto
    ): Response<GenericMessageResponseDto>

    // mudar email
    @POST("/api/auth/change-email")
    suspend fun changeEmail(
        @Header("Authorization") auth: String,
        @Body body: ChangeEmailRequestDto
    ): Response<GenericMessageResponseDto>

    // mudar foto de perfil
    @Multipart
    @POST("/api/auth/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") auth: String,
        @Part avatar: MultipartBody.Part
    ): UserDto

    // logout
    @POST("/api/auth/logout")
    suspend fun logout(
        @Header("Authorization") auth: String
    ): Response<GenericMessageResponseDto>



    companion object {
        fun create(): AuthApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BaseUrlProvider.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi::class.java)
        }
    }
}
