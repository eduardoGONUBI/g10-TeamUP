// File: app/src/main/java/com/example/teamup/ui/screens/HomeViewModel.kt
package com.example.teamup.ui.screens

import android.content.Context
import android.location.Geocoder
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.api.AuthApi
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.math.*

class HomeViewModel(
    private val repo: ActivityRepository
) : ViewModel() {

    /* ────── raw data from server ────── */
    private val _allActivities = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _error         = MutableStateFlow<String?>(null)

    /* ────── map centre ────── */
    private val defaultCenter  = LatLng(41.5381, -8.6151)
    private val _center        = MutableStateFlow(defaultCenter)

    /* ────── pagination state ────── */
    private val pageSize       = 10
    private val currentPage    = MutableStateFlow(1)
    private val _visibleActivities = MutableStateFlow<List<ActivityItem>>(emptyList())
    private val _hasMore           = MutableStateFlow(false)

    /** KEEP the full filtered list here so we can both paginate _and_ show all on the map **/
    private val _filteredActivities = MutableStateFlow<List<ActivityItem>>(emptyList())

    // expose public read-only flows:
    val error: StateFlow<String?>                      = _error
    val center: StateFlow<LatLng>                      = _center
    val visibleActivities: StateFlow<List<ActivityItem>> = _visibleActivities
    val hasMore: StateFlow<Boolean>                    = _hasMore

    /** This is the “full” filtered list (non-concluded within 25 km). HomeScreen will use this for the map. */
    val activities: StateFlow<List<ActivityItem>>      = _filteredActivities

    init {
        // Whenever _allActivities or _center changes, recompute filtered list,
        // then reset pagination to page 1.
        viewModelScope.launch {
            combine(_allActivities, _center) { list, c ->
                // filter out concluded + >25 km:
                list.filter { item ->
                    !item.status.equals("concluded", ignoreCase = true) &&
                            distanceMetres(item.latitude, item.longitude, c.latitude, c.longitude) <= 25_000
                }
            }.collect { filtered ->
                _filteredActivities.value = filtered
                currentPage.value = 1

                // first page slice:
                val firstSlice = filtered.take(pageSize)
                _visibleActivities.value = firstSlice
                _hasMore.value = filtered.size > pageSize
            }
        }
    }

    /** Fetch all events from repo, annotate isCreator, then push into _allActivities. */
    fun loadActivities(rawToken: String) = viewModelScope.launch {
        try {
            val userId = getUserIdFromToken(rawToken) ?: -1
            val items  = repo.getAllEvents(rawToken)  // repo should handle “Bearer” prefix internally

            // annotate isCreator:
            val annotated = items.map { it.copy(isCreator = (it.creatorId == userId)) }
            _allActivities.value = annotated
            _error.value = null
        } catch (e: Exception) {
            _error.value = e.localizedMessage
        }
    }

    /** Fetch user location or fallback‐geocode, then set _center. */
    fun loadFallbackCenter(rawToken: String, ctx: Context) = viewModelScope.launch {
        try {
            val token  = bearer(rawToken)
            val uid    = getUserIdFromToken(token) ?: return@launch
            val user   = AuthApi.create().getUser(uid, token)

            when {
                user.latitude  != null && user.longitude != null ->
                    _center.value = LatLng(user.latitude, user.longitude)

                !user.location.isNullOrBlank() ->
                    geocode(user.location!!, ctx)?.let { _center.value = it }
            }
        } catch (_: Exception) {
            // keep existing _center (default or previous)
        }
    }

    /** Called when the user taps “Load more”. */
    fun loadMore() {
        val filtered = _filteredActivities.value
        val nextPage = currentPage.value + 1
        val toIndex  = (nextPage * pageSize).coerceAtMost(filtered.size)
        val newList  = filtered.take(toIndex)

        currentPage.value = nextPage
        _visibleActivities.value = newList
        _hasMore.value = filtered.size > newList.size
    }

    /* ────── helpers ────── */

    private fun bearer(tok: String): String =
        if (tok.trim().startsWith("Bearer ")) tok.trim()
        else "Bearer ${tok.trim()}"

    private fun getUserIdFromToken(tokenWithBearer: String): Int? = try {
        val payload = tokenWithBearer.removePrefix("Bearer ").split(".")[1]
        val json = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)))
        json.getInt("sub")
    } catch (_: Exception) {
        null
    }

    private fun geocode(city: String, ctx: Context): LatLng? = try {
        Geocoder(ctx, Locale.getDefault())
            .getFromLocationName(city, 1)
            ?.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
    } catch (_: IOException) {
        null
    }

    private fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)
        val a = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }
}
