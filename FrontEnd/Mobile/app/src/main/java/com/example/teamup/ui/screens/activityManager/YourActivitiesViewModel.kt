// app/src/main/java/com/example/teamup/ui/screens/activityManager/YourActivitiesViewModel.kt
package com.example.teamup.ui.screens.activityManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.domain.model.ActivityItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that loads “my” activities (i.e. events the current user created).
 */
class YourActivitiesViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    // ─── State ───────────────────────────────────────────────────────
    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Load the list of events created by *this* user.
     */
    fun loadMyActivities(token: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                val list = repo.getMyActivities(token)
                // Every item here was created by the user, so isCreator = true
                // (If your repository-to-ActivityItem mapper already sets isCreator, you can omit this.)
                val marked = list.map { it.copy(isCreator = true) }
                _activities.value = marked
            } catch (e: Exception) {
                _error.value = "Failed to load your activities:\n${e.localizedMessage}"
            }
        }
    }
}
