package com.example.teamup.data.remote.repository

import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.mapper.toDto
import com.example.teamup.data.remote.model.ChangeEmailRequestDto
import com.example.teamup.data.remote.model.ChangePasswordRequestDto
import com.example.teamup.data.remote.model.ForgotPasswordRequestDto
import com.example.teamup.data.remote.model.LoginRequestDto
import com.example.teamup.domain.model.RegisterRequestDomain
import com.example.teamup.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val api: AuthApi
) : AuthRepository {

    // login
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            Result.success(response.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // registar
    override suspend fun register(request: RegisterRequestDomain): Result<String> {
        return try {
            val dto      = request.toDto()
            val response = api.register(dto)
            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.message ?: "Registered successfully")
            } else {
                val errorMsg = try {
                    val errorJson = response.errorBody()?.string() ?: ""
                    val jsonObj   = JSONObject(errorJson)
                    when {
                        jsonObj.has("errors") -> {
                            val errors   = jsonObj.getJSONObject("errors")
                            val firstKey = errors.keys().next()
                            val arr      = errors.getJSONArray(firstKey)
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
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unknown"}"))
        }
    }

    // forgot password
    override suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val dto      = ForgotPasswordRequestDto(email = email)
            val response = api.sendResetLink(dto)

            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.message ?: "Reset link sent")
            } else {
                val errorJson = response.errorBody()?.string() ?: ""
                val parsed    = try {
                    val obj = JSONObject(errorJson)
                    when {
                        obj.has("errors") -> {
                            val errs     = obj.getJSONObject("errors")
                            val firstKey = errs.keys().next()
                            errs.getJSONArray(firstKey).getString(0)
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
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unknown"}"))
        }
    }

    // change password
    override suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dto = ChangePasswordRequestDto(
                currentPassword       = currentPassword,
                newPassword           = newPassword,
                newPasswordConfirmation = newPasswordConfirmation
            )
            val bearer   = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.changePassword(bearer, dto)

            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Password changed successfully")
            } else {
                val errorJson = response.errorBody()?.string() ?: ""
                val msg       = try {
                    val obj = JSONObject(errorJson)
                    when {
                        obj.has("errors") -> obj.getJSONObject("errors").let { errs ->
                            val firstKey = errs.keys().next()
                            errs.getJSONArray(firstKey).getString(0)
                        }
                        obj.has("message") -> obj.getString("message")
                        else -> "Failed to change password"
                    }
                } catch (_: Exception) {
                    "Failed to change password"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unknown"}"))
        }
    }

    // change email
    override suspend fun changeEmail(
        token: String,
        newEmail: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dto = ChangeEmailRequestDto(newEmail = newEmail, password = password)
            val bearer   = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.changeEmail(bearer, dto)
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Email changed successfully")
            } else {
                val errorJson = response.errorBody()?.string() ?: ""
                val errorMsg  = try {
                    val obj = JSONObject(errorJson)
                    when {
                        obj.has("errors") -> obj.getJSONObject("errors").let { errs ->
                            val firstKey = errs.keys().next()
                            errs.getJSONArray(firstKey).getString(0)
                        }
                        obj.has("message") -> obj.getString("message")
                        else -> "Failed to change email"
                    }
                } catch (_: Exception) {
                    "Failed to change email"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unknown"}"))
        }
    }
}
