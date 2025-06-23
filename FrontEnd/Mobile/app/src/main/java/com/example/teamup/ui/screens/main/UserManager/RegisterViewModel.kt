package com.example.teamup.ui.screens.main.UserManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.RegisterRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject

// estado da ui
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val message: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

// estado formulario
data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val location: String = ""
)

class RegisterViewModel : ViewModel() {

    // form state
    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState

    //  api state
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    // instancia api
    private val api: AuthApi = AuthApi.create()

    // atualiza os campos quando o user digita
    fun updateForm(updater: (RegisterFormState) -> RegisterFormState) {
        _formState.value = updater(_formState.value)
    }

  // quando clica registar
    fun register() {
        val form = _formState.value

        // validaçao
        val name     = form.name.trim()
        val email    = form.email.trim()
        val password = form.password
        val confirm  = form.confirmPassword
        val location = form.location.trim()

        if (name.isEmpty()) {
            _registerState.value = RegisterState.Error("Username is required")
            return
        }
        if (!name.matches(Regex("^\\w{1,255}\$"))) {
            _registerState.value =
                RegisterState.Error("Username can only contain letters, numbers, or underscores")
            return
        }
        if (email.isEmpty()) {
            _registerState.value = RegisterState.Error("Email is required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerState.value = RegisterState.Error("Enter a valid email address")
            return
        }
        if (password.length < 6) {
            _registerState.value = RegisterState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirm) {
            _registerState.value = RegisterState.Error("Passwords do not match")
            return
        }
        if (location.isEmpty()) {
            _registerState.value = RegisterState.Error("City is required")
            return
        }

        // chama o backend
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                //  build DTO
                val body = RegisterRequestDto(
                    name = name,
                    email = email,
                    password = password,
                    passwordConfirmation = confirm,
                    location = location
                )
                val response = api.register(body)

                if (response.isSuccessful) { // sucesso

                    val respBody = response.body()
                    val msg = respBody?.message ?: "Registered! Check your email."
                    _registerState.value = RegisterState.Success(msg)
                } else {
                    // erros de validaçao do backend
                    val errorJson = response.errorBody()?.string() ?: ""
                    val parsedMsg = try {
                        val obj = JSONObject(errorJson)
                        if (obj.has("errors")) {
                            val errs = obj.getJSONObject("errors")
                            val firstKey = errs.keys().next()
                            val arr = errs.getJSONArray(firstKey)
                            arr.getString(0)
                        } else if (obj.has("message")) {
                            obj.getString("message")
                        } else {
                            "Registration failed"
                        }
                    } catch (_: Exception) {
                        "Registration failed"
                    }
                    _registerState.value = RegisterState.Error(parsedMsg)
                }
            } catch (e: HttpException) {
                _registerState.value = RegisterState.Error("Server error: ${e.message()}")
            } catch (e: Exception) {
                _registerState.value =
                    RegisterState.Error("Network error: ${e.localizedMessage ?: "Unknown"}")
            }
        }
    }


    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}
