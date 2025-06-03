package com.example.teamup.ui.screens.Chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.ui.components.ChatCard
import androidx.compose.foundation.lazy.items
@Composable
fun ChatListScreen(
    navController: NavController,
    token: String
) {
    /* 1) VM with existing ActivityRepositoryImpl */
    val vm: ChatListScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(c: Class<T>): T {
                val repo = ActivityRepositoryImpl(ActivityApi.create())
                return ChatListScreenViewModel(repo) as T
            }
        }
    )

    /* 2) Load once */
    LaunchedEffect(token) { vm.load(token) }

    /* 3) Observe */
    val ui by vm.state.collectAsState()

    /* 4) Tabs */
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Chats", "Archive")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t ->
                Tab(
                    selected = tab == i,
                    onClick  = { tab = i },
                    text     = { Text(t) }
                )
            }
        }

        when {
            ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Error: ${ui.error}") }
            else -> {
                val list = if (tab == 0) ui.active else ui.archive
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No chats.") }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list, key = { it.id }) { chat ->
                            ChatCard(
                                chat = chat,
                                onClick = {
                                    val safe = java.net.URLEncoder.encode(chat.title, "UTF-8")
                                    navController.navigate("chatDetail/$safe")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
