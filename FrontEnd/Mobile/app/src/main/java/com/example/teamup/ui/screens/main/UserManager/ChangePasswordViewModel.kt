// ─── ChangePasswordViewModel.kt ───────────────────────────────────────────
package com.example.teamup.ui.screens.main.UserManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.remote.repository.AuthRepositoryImpl
import com.example.teamup.data.remote.api.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents UI state for “Change Password”:
 *  • Idle: nothing happening
 *  • Loading: in-flight request
 *  • Success(message): password changed
 *  • Error(message): failed or validation error
 */
sealed class ChangePasswordState {
    object Idle    : ChangePasswordState()
    object Loading : ChangePasswordState()
    data class Success(val message: String) : ChangePasswordState()
    data class Error(val message: String)   : ChangePasswordState()
}

/**
 * ViewModel that calls the AuthRepositoryImpl.changePassword(...) endpoint.
 */
class ChangePasswordViewModel(
    private val repository: AuthRepositoryImpl = AuthRepositoryImpl(AuthApi.create())
) : ViewModel() {

    private val _state = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val state: StateFlow<ChangePasswordState> = _state

    /**
     * Call this from the UI when user taps “Change Password” after filling all fields.
     */
    fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ) {
        // 1) Basic client‐side validation
        if (currentPassword.isBlank()) {
            _state.value = ChangePasswordState.Error("Current password is required")
            return
        }
        if (newPassword.length < 6) {
            _state.value = ChangePasswordState.Error("New password must be at least 6 characters")
            return
        }
        if (newPassword != newPasswordConfirmation) {
            _state.value = ChangePasswordState.Error("Passwords do not match")
            return
        }

        // 2) Launch network call
        _state.value = ChangePasswordState.Loading
        viewModelScope.launch {
            val result = repository.changePassword(
                token = token,
                currentPassword = currentPassword,
                newPassword = newPassword,
                newPasswordConfirmation = newPasswordConfirmation
            )

            result.fold(
                onSuccess = { msg ->
                    _state.value = ChangePasswordState.Success(msg)
                },
                onFailure = { err ->
                    _state.value = ChangePasswordState.Error(err.message ?: "Unknown error")
                }
            )
        }
    }

    /**
     * Reset the state back to Idle (e.g. if the user navigates away or wants to retry).
     */
    fun resetState() {
        _state.value = ChangePasswordState.Idle
    }
}
