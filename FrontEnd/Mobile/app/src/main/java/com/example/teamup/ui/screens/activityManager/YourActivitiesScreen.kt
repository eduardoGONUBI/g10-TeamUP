package com.example.teamup.ui.screens.activityManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.repository.ActivityRepository
import com.example.teamup.data.remote.repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.ui.components.ActivityCard

@Composable
fun YourActivitiesScreen(
    token: String,
    onActivityClick: (Activity) -> Unit
) {
    val repo: ActivityRepository = remember {
        ActivityRepositoryImpl(ActivityApi.create())
    }

    val vm: YourActivitiesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return YourActivitiesViewModel(repo) as T
            }
        }
    )



    val uiState by vm.state.collectAsState()

      // carega a primeira pagina
    LaunchedEffect(token) {
        vm.loadMyEvents("Bearer $token")
    }

    // lista atividades e load more
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // mostra cada ativbity card
        if (uiState.visibleActivities.isNotEmpty()) {
            items(uiState.visibleActivities, key = { it.id }) { activity ->
                // define as cores de cada atividade
                val bgColor: Color
                var bgBrush: Brush? = null
                var labelCreator: String? = null
                var labelConcluded: String? = null

                when {
                    // criador e concluido -> mistura solid orange e pale blue
                    activity.isCreator && activity.status == "concluded" -> {
                        bgBrush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFE3F2FD),
                                Color(0xFFFFA500)
                            )
                        )
                        bgColor = Color.Transparent
                        labelCreator = "You are the creator"
                        labelConcluded = "Event concluded"
                    }

                    // criador → pale blue
                    activity.isCreator -> {
                        bgColor = Color(0xFFE3F2FD)
                        labelCreator = "You are the creator"
                        labelConcluded = null
                    }

                    // apenas concluido → solid orange
                    activity.status == "concluded" -> {
                        bgColor = Color(0xFFFFA500)
                        labelCreator = null
                        labelConcluded = "Event concluded"
                    }

                    // apenas participante → pale gray
                    activity.isParticipant -> {
                        bgColor = Color(0xFFF5F5F5)
                        labelCreator = null
                        labelConcluded = null
                    }

                    // default
                    else -> {
                        bgColor = Color(0xFFF5F5F5)
                        labelCreator = null
                        labelConcluded = null
                    }
                }

                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity) },
                    bgColor = bgColor,
                    bgBrush = bgBrush,
                    labelCreator = labelCreator,
                    labelConcluded = labelConcluded
                )
            }
        }
        // nao ha atividades polaceholder
        else if (!uiState.loading && uiState.error == null && uiState.fullActivities.isEmpty()) {
            item {
                Text(
                    text = "You have no activities.",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }

        // erro
        uiState.error?.let { err ->
            item {
                Text(
                    text = "Failed to load your activities: $err",
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        // load more
        if (uiState.hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Button(
                        onClick = { vm.loadMore() },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(8.dp)
                    ) {
                        Text("Load more")
                    }
                }
            }
        }
    }
}
