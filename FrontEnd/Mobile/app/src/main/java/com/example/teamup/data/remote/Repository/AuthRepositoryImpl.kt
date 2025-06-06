// src/main/java/com/example/teamup/data/remote/Repository/AuthRepositoryImpl.kt
package com.example.teamup.data.remote.Repository

import com.example.teamup.data.domain.repository.AuthRepository
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.ChangePasswordRequestDto
import com.example.teamup.data.remote.model.ForgotPasswordRequestDto
import com.example.teamup.data.remote.model.LoginRequestDto
import com.example.teamup.data.remote.model.RegisterRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            Result.success(response.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequestDto): Result<String> {
        return try {
            val response = api.register(request)
            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.message ?: "Registered successfully")
            } else {
                // Try to extract Laravel’s validation error (first message)
                val errorMsg = try {
                    val errorJson = response.errorBody()?.string() ?: ""
                    val jsonObj = JSONObject(errorJson)
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
            // Use e.message (property), not e.message()
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unknown"}"))
        }
    }

    override suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val dto = ForgotPasswordRequestDto(email = email)
            val response = api.sendResetLink(dto)

            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.message ?: "Reset link sent")
            } else {
                // Extract Laravel validation errors or fallback
                val errorJson = response.errorBody()?.string() ?: ""
                val parsed = try {
                    val obj = JSONObject(errorJson)
                    when {
                        obj.has("errors") -> {
                            val errs = obj.getJSONObject("errors")
                            val firstKey = errs.keys().next()
                            val arr = errs.getJSONArray(firstKey)
                            arr.getString(0)
                        }
                        obj.has("message") -> obj.getString("message")
                        else -> "Failed to send reset link"
                    }
                } catch (_: Exception) {
                    "Failed to send reset link"
                }
                Result.failure(Exception(parsed))
            }
        } catch (e: HttpException) {
            // Use e.message (property), not e.message()
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unknown"}"))
        }
    }

    override suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Build request body
                val dto = ChangePasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    newPasswordConfirmation = newPasswordConfirmation
                )

                val bearerHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = api.changePassword(bearerHeader, dto)

                if (response.isSuccessful) {
                    val body = response.body()
                    Result.success(body?.message ?: "Password changed successfully")
                } else {
                    val errorJson = response.errorBody()?.string() ?: ""
                    val msg = try {
                        val obj = JSONObject(errorJson)
                        if (obj.has("errors")) {
                            val errs = obj.getJSONObject("errors")
                            val firstKey = errs.keys().next()
                            errs.getJSONArray(firstKey).getString(0)
                        } else if (obj.has("message")) {
                            obj.getString("message")
                        } else {
                            "Failed to change password"
                        }
                    } catch (_: Exception) {
                        "Failed to change password"
                    }
                    Result.failure(Exception(msg))
                }
            } catch (e: HttpException) {
                Result.failure(Exception("Server error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }
}
