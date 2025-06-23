package com.example.teamup.ui.screens.main.UserManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.repository.AuthRepositoryImpl
import com.example.teamup.data.remote.api.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed class ChangeEmailState {
    object Idle    : ChangeEmailState()
    object Loading : ChangeEmailState()
    data class Success(val message: String) : ChangeEmailState()
    data class Error(val message: String)   : ChangeEmailState()
}


class ChangeEmailViewModel(

    private val repository: AuthRepositoryImpl = AuthRepositoryImpl(AuthApi.create())
) : ViewModel() {

    private val _state = MutableStateFlow<ChangeEmailState>(ChangeEmailState.Idle)
    val state: StateFlow<ChangeEmailState> = _state

// change email
    fun changeEmail(token: String, newEmail: String, password: String) {
        // client‐side validation
        if (newEmail.isBlank()) {
            _state.value = ChangeEmailState.Error("New email is required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _state.value = ChangeEmailState.Error("Enter a valid email address")
            return
        }
        if (password.isBlank()) {
            _state.value = ChangeEmailState.Error("Current password is required")
            return
        }

        //  call backend
        _state.value = ChangeEmailState.Loading
        viewModelScope.launch {
            val result = repository.changeEmail(token, newEmail, password)
            result.fold(
                onSuccess = { msg ->
                    _state.value = ChangeEmailState.Success(msg)
                },
                onFailure = { err ->
                    _state.value = ChangeEmailState.Error(err.message ?: "Unknown error")
                }
            )
        }
    }

    fun resetState() {
        _state.value = ChangeEmailState.Idle
    }
}
