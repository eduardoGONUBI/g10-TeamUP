package com.example.teamup.ui.screens.main.UserManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

class ForgotPasswordViewModel : ViewModel() {
    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state


    private val repository = AuthRepositoryImpl(AuthApi.create())

    /* reset link */
    fun requestReset(email: String) {
        // 1) Simple client‐side validation
        if (email.isBlank()) {
            _state.value = ForgotPasswordState.Error("Email is required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = ForgotPasswordState.Error("Enter a valid email")
            return
        }

        // chama api
        _state.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            val result = repository.forgotPassword(email)
            result.fold(
                onSuccess = { msg ->
                    _state.value = ForgotPasswordState.Success(msg)
                },
                onFailure = { err ->
                    _state.value = ForgotPasswordState.Error(err.message ?: "Unknown error")
                }
            )
        }
    }


    fun resetState() {
        _state.value = ForgotPasswordState.Idle
    }
}
