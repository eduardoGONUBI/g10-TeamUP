// File: app/src/main/java/com/example/teamup/ui/screens/EditProfileScreen.kt
package com.example.teamup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.R
import com.example.teamup.data.remote.Repository.UserRepositoryImpl
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.SportDto
import com.example.teamup.presentation.profile.EditProfileViewModel
import com.example.teamup.ui.popups.DeleteAccountDialog
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    token: String,
    usernameInitial: String,
    locationInitial: String,
    sportsInitial: List<Int>,
    onFinished: (deleted: Boolean) -> Unit,
    onBack: () -> Unit,
    onChangePassword: () -> Unit
) {
    /* ──────── View-model ──────── */
    val vm: EditProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                EditProfileViewModel(UserRepositoryImpl(AuthApi.create())) as T
        }
    )
    val uiState          by vm.ui.collectAsState()
    val allSports        by vm.sports.collectAsState()
    val errorLoadingSports by vm.error.collectAsState()

    /* ──────── Local form state ──────── */
    var username  by remember { mutableStateOf(usernameInitial) }
    var location  by remember { mutableStateOf(locationInitial) }
    var latitude  by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    var chosenSportName by remember { mutableStateOf("") }
    var chosenSportId   by remember { mutableStateOf<Int?>(null) }
    var sportMenuOpen   by remember { mutableStateOf(false) }

    var didDelete       by remember { mutableStateOf(false) }
    var showDeleteDlg   by remember { mutableStateOf(false) }

    /* ──────── Google Places (inline autocomplete) ──────── */
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(ctx, ctx.getString(R.string.google_maps_key))
        }
    }

    val placesClient: PlacesClient = remember { Places.createClient(ctx) }
    val sessionToken               = remember { AutocompleteSessionToken.newInstance() }
    var suggestions                by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    /* Re-query suggestions whenever the text changes  */
    LaunchedEffect(location) {
        // user is typing → reset validation
        latitude  = null
        longitude = null

        if (location.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        val req = FindAutocompletePredictionsRequest
            .builder()
            .setSessionToken(sessionToken)
            .setQuery(location)
            .setTypeFilter(TypeFilter.CITIES)   // only cities/villages
            .build()

        placesClient.findAutocompletePredictions(req)
            .addOnSuccessListener { suggestions = it.autocompletePredictions }
            .addOnFailureListener  { suggestions = emptyList() }
    }

    /* ──────── Load sports list once ──────── */
    LaunchedEffect(token) {
        vm.loadSports("Bearer $token")
        sportsInitial.firstOrNull()?.let { initialId ->
            allSports.find { it.id == initialId }?.let {
                chosenSportName = it.name
                chosenSportId   = it.id
            }
        }
    }

    /* Return to caller when done */
    LaunchedEffect(uiState.done) { if (uiState.done) onFinished(didDelete) }

    /* Derived flag: user must have picked a *validated* place */
    val isLocationValid = if (location == locationInitial) {
        true                       // unchanged → already good
    } else {
        latitude != null && longitude != null   // user typed → must pick a place
    }
    /* ───────────────────────── UI ───────────────────────── */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Edit Profile", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            /* Avatar placeholder ------------------------------------------------ */
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
                Icon(
                    painter = painterResource(R.drawable.change_profile_pic),
                    contentDescription = null,
                    tint  = Color.White.copy(alpha = .7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp)
                        .clickable { /* TODO image picker */ }
                )
            }

            Spacer(Modifier.height(32.dp))

            /* Form --------------------------------------------------------------- */
            Column(Modifier.padding(horizontal = 16.dp)) {

                /* Username */
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                /* Favourite sport */
                Text("Favourite Sport", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = sportMenuOpen,
                    onExpandedChange = { sportMenuOpen = !sportMenuOpen }
                ) {
                    OutlinedTextField(
                        value = chosenSportName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Choose a sport") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportMenuOpen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sportMenuOpen,
                        onDismissRequest = { sportMenuOpen = false }
                    ) {
                        allSports.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(sport.name) },
                                onClick = {
                                    chosenSportName = sport.name
                                    chosenSportId   = sport.id
                                    sportMenuOpen   = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* Location (inline autocomplete) */
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("City / Town") },
                    singleLine = true,
                    trailingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    supportingText = {
                        if (!isLocationValid && location.isNotBlank())
                            Text("Pick a city from the list", color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                /* Suggestions dropdown */
                if (suggestions.isNotEmpty()) {
                    Card(
                        elevation = cardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn {
                            items(suggestions, key = { it.placeId }) { pred ->
                                DropdownMenuItem(
                                    text = { Text(pred.getFullText(null).toString()) },
                                    onClick = {
                                        /* fetch full place for lat/lng */
                                        val fields = listOf(
                                            Place.Field.NAME,
                                            Place.Field.LAT_LNG
                                        )
                                        placesClient.fetchPlace(
                                            com.google.android.libraries.places.api.net.FetchPlaceRequest
                                                .builder(pred.placeId, fields)
                                                .setSessionToken(sessionToken)
                                                .build()
                                        ).addOnSuccessListener { rsp ->
                                            val plc = rsp.place
                                            location  = plc.name ?: pred.getPrimaryText(null).toString()
                                            latitude  = plc.latLng?.latitude
                                            longitude = plc.latLng?.longitude
                                            suggestions = emptyList()
                                        }.addOnFailureListener {
                                            /* keep text but still invalid */
                                            location  = pred.getPrimaryText(null).toString()
                                            suggestions = emptyList()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(32.dp))

                /* Save ----------------------------------------------------------- */
                Button(
                    enabled = !uiState.saving && isLocationValid,
                    onClick = {
                        val sportIds = chosenSportId?.let { listOf(it) } ?: emptyList()
                        vm.save(
                            bearer   = "Bearer $token",
                            username = username,
                            location = location,
                            lat      = latitude,
                            lng      = longitude,
                            sportIds = sportIds
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.saving) "Saving…" else "Save")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onChangePassword() },    // invoke the lambda passed from NavGraph
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Change Password")
                    Spacer(Modifier.width(8.dp))
                    Text("Change Password")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { /* change-email screen */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MailOutline, null); Spacer(Modifier.width(8.dp)); Text("Change Email")
                }

                Spacer(Modifier.height(32.dp))

                /* Delete account */
                Button(
                    enabled = !uiState.saving,
                    onClick = { showDeleteDlg = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCD1606), contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete Account")
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        /* Delete confirmation dialog */
        if (showDeleteDlg) {
            Dialog(onDismissRequest = { showDeleteDlg = false }) {
                DeleteAccountDialog(
                    onCancel = { showDeleteDlg = false },
                    onDelete = {
                        didDelete = true
                        vm.deleteAccount("Bearer $token")
                        showDeleteDlg = false
                    }
                )
            }
        }

        /* Snackbars --------------------------------------------------------- */
        uiState.error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) { Text("Error: $it") }
        }
        errorLoadingSports?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) { Text("Failed to load sports: $it") }
        }
    }
}
