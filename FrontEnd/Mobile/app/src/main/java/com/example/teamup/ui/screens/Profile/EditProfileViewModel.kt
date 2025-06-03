// File: app/src/main/java/com/example/teamup/presentation/profile/EditProfileViewModel.kt
package com.example.teamup.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.repository.UserRepository
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.UpdateUserRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.teamup.data.remote.model.SportDto

data class EditProfileUiState(
    val saving: Boolean = false,
    val error : String? = null,
    val done  : Boolean = false          // flips to true when update succeeds / account deleted
)

class EditProfileViewModel(
    private val repo : UserRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(EditProfileUiState())
    val ui: StateFlow<EditProfileUiState> = _ui

    private val _sports = MutableStateFlow<List<SportDto>>(emptyList())
    val sports: StateFlow<List<SportDto>> = _sports

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    fun save(token: String, name: String, location: String, sportIds: List<Int>) {
        viewModelScope.launch {
            _ui.value = EditProfileUiState(saving = true)
            try {
                repo.updateMe(
                    token,
                    UpdateUserRequest(
                        name     = name.trim().takeIf { it.isNotBlank() },
                        location = location.trim().takeIf { it.isNotBlank() },
                        sports   = sportIds.ifEmpty { null }
                    )
                )
                _ui.value = EditProfileUiState(done = true)
            } catch (e: Exception) {
                _ui.value = EditProfileUiState(error = e.message)
            }
        }
    }

    fun deleteAccount(token: String) {
        viewModelScope.launch {
            _ui.value = EditProfileUiState(saving = true)
            try {
                repo.deleteMe(token)
                _ui.value = EditProfileUiState(done = true)
            } catch (e: Exception) {
                _ui.value = EditProfileUiState(error = e.message)
            }
        }
    }

    fun loadSports(bearer: String) = viewModelScope.launch {
       val activityApi: ActivityApi = ActivityApi.create()

        _error.value = null
        try {
            _sports.value = activityApi.getSports(bearer)
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
