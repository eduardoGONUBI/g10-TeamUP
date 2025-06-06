package com.example.teamup.data.remote.Repository

import android.net.http.HttpException
import android.os.Build
import androidx.annotation.RequiresExtension
import com.example.teamup.data.domain.repository.AuthRepository
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.LoginRequestDto
import com.example.teamup.data.remote.model.RegisterRequestDto

class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            Result.success(response.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override suspend fun register(request: RegisterRequestDto): Result<String> {
        return try {
            val response = api.register(request)
            if (response.isSuccessful) {
                // We only need the “message” or just a success indicator
                val body = response.body()
                Result.success(body?.message ?: "Registered successfully")
            } else {
                // Try to extract Laravel’s validation error (first message)
                val errorMsg = try {
                    val errorJson = response.errorBody()?.string() ?: ""
                    val jsonObj = org.json.JSONObject(errorJson)
                    when {
                        jsonObj.has("errors") -> {
                            val errors = jsonObj.getJSONObject("errors")
                            val firstKey = errors.keys().next()
                            val arr = errors.getJSONArray(firstKey)
                            arr.getString(0)
                        }
                        jsonObj.has("message") -> jsonObj.getString("message")
                        else -> "Registration failed"
                    }
                } catch (_: Exception) {
                    "Registration failed"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }



}

