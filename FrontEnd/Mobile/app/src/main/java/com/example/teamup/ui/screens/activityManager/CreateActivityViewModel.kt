// app/src/main/java/com/example/teamup/ui/screens/activityManager/CreateActivityViewModel.kt
package com.example.teamup.ui.screens.activityManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.domain.model.CreateEventRequestDomain
import com.example.teamup.domain.repository.ActivityRepository
import com.example.teamup.domain.model.Sport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── UI STATE ──────────────────────────────────────────────────
sealed class CreateUiState {
    object Idle    : CreateUiState()
    object Loading : CreateUiState()
    data class Success(val createdId: String) : CreateUiState()
    data class Error(val msg: String)       : CreateUiState()
}

// ─── FORM DATA ─────────────────────────────────────────────────
data class CreateFormState(
    val name : String     = "",
    val sport: Sport?  = null,
    val place: String    = "",
    val date : String     = "",
    val time : String     = "",
    val max  : String     = "",
    val latitude : Double?   = null,
    val longitude: Double?   = null
)

// ─── VIEWMODEL ────────────────────────────────────────────────
class CreateActivityViewModel(
    private val repo: ActivityRepository
) : ViewModel() {


    private val _state = MutableStateFlow<CreateUiState>(CreateUiState.Idle)
    val state: StateFlow<CreateUiState> = _state

    private val _form = MutableStateFlow(CreateFormState())
    val form: StateFlow<CreateFormState> = _form

    private val _sports = MutableStateFlow<List<Sport>>(emptyList())
    val sports: StateFlow<List<Sport>> = _sports
    /**
     * Load the list of sports from the repository.
     */
    fun loadSports(token: String) {
        viewModelScope.launch {
            _state.value = CreateUiState.Loading
            try {
                val list = repo.getSports(token)
                _sports.value = list
                _state.value = CreateUiState.Idle
            } catch (e: Exception) {
                _state.value = CreateUiState.Error("Failed to load sports: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Update the current form state.
     */
    fun update(transform: (CreateFormState) -> CreateFormState) {
        _form.update { transform(it) }
    }

    /**
     * Called when the user taps “Create Activity.” First validate:
     *  • name must not be blank
     *  • sport must be selected
     *  • place must not be blank
     *  • date and time must not be blank
     *  • max must parse to an integer ≥ 2
     *
     * If any of these checks fail, emit CreateUiState.Error(...) immediately.
     * Otherwise, call the repository to create the event.
     */
    fun submit(token: String) {
        viewModelScope.launch {
            // 1) Validate client‐side
            val f = _form.value

            if (f.name.isBlank()) {
                _state.value = CreateUiState.Error("Please enter an activity name.")
                return@launch
            }
            val chosenSport = f.sport
            if (chosenSport == null) {
                _state.value = CreateUiState.Error("Please select a sport.")
                return@launch
            }
            if (f.place.isBlank()) {
                _state.value = CreateUiState.Error("Please enter a location.")
                return@launch
            }
            if (f.date.isBlank()) {
                _state.value = CreateUiState.Error("Please pick a date.")
                return@launch
            }
            if (f.time.isBlank()) {
                _state.value = CreateUiState.Error("Please pick a time.")
                return@launch
            }
            val maxInt = f.max.toIntOrNull()
            if (maxInt == null || maxInt < 2) {
                _state.value = CreateUiState.Error("Number of participants must be at least 2.")
                return@launch
            }

            // 2) If client‐side validation passes, proceed with API call
            _state.value = CreateUiState.Loading
            try {
                // Combine date + time into "YYYY-MM-DD HH:MM:00"
                val startsAt = "${f.date} ${f.time}:00"
                val body = CreateEventRequestDomain(
                    name            = f.name.trim(),
                    sportId         = chosenSport.id,
                    startsAt        = startsAt,
                    place           = f.place.trim(),
                    maxParticipants = maxInt,
                    latitude        = f.latitude ?: 0.0,
                    longitude       = f.longitude ?: 0.0
                )

                // The repository returns an Activity; use its id for Success
                val createdItem = repo.createActivity(token, body)

                // repo.createActivity(...) returned Activity with an Int id, so convert to String
                _state.value = CreateUiState.Success(createdItem.id.toString())
            } catch (e: Exception) {
                // If it’s an HTTP error, show code + message; otherwise just show localizedMessage
                val httpCode = (e as? retrofit2.HttpException)?.code() ?: null
                val msg = if (httpCode != null) {
                    "HTTP $httpCode — ${e.localizedMessage}"
                } else {
                    e.localizedMessage ?: "Unknown error"
                }
                _state.value = CreateUiState.Error(msg)
            }
        }
    }

    /**
     * After a successful creation, reset the state so that the “Success” only fires once.
     */
    fun clearStatus() {
        _state.value = CreateUiState.Idle
    }
}
