// ─── ChangeEmailViewModel.kt ─────────────────────────────────────────────
package com.example.teamup.ui.screens.main.UserManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.Repository.AuthRepositoryImpl
import com.example.teamup.data.remote.api.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for Change Email flow:
 *   • Idle: no request
 *   • Loading: network call in progress
 *   • Success(message): email changed successfully
 *   • Error(message): validation or server error
 */
sealed class ChangeEmailState {
    object Idle    : ChangeEmailState()
    object Loading : ChangeEmailState()
    data class Success(val message: String) : ChangeEmailState()
    data class Error(val message: String)   : ChangeEmailState()
}

/**
 * ViewModel coordinating the “Change Email” endpoint.
 */
class ChangeEmailViewModel(
    // You can inject AuthRepositoryImpl or let ViewModel create it directly:
    private val repository: AuthRepositoryImpl = AuthRepositoryImpl(AuthApi.create())
) : ViewModel() {

    private val _state = MutableStateFlow<ChangeEmailState>(ChangeEmailState.Idle)
    val state: StateFlow<ChangeEmailState> = _state

    /**
     * Called when the user taps “Change Email” in the UI.
     * Performs client‐side checks and then calls the repository.
     */
    fun changeEmail(token: String, newEmail: String, password: String) {
        // 1) client‐side validation
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

        // 2) call backend
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

    /** Reset back to Idle (e.g. user navigates away). */
    fun resetState() {
        _state.value = ChangeEmailState.Idle
    }
}
