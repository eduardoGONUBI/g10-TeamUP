/* ─── File: app/src/main/java/com/example/teamup/ui/screens/Chat/ChatListScreen.kt ── */
package com.example.teamup.ui.screens.Chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ChatListScreen(
    navController: NavController,
    token: String,
    myUserId: Int            // ← NEW: needs to be forwarded from RootScaffold
) {
    /* ── 1) View-model ────────────────────────────────────────────────────── */
    val vm: ChatListScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(c: Class<T>): T {
                val repo = ActivityRepositoryImpl(ActivityApi.create())
                return ChatListScreenViewModel(repo) as T
            }
        }
    )

    /* ── 2) Load once ─────────────────────────────────────────────────────── */
    LaunchedEffect(token) { vm.load(token) }

    /* ── 3) Observe state ─────────────────────────────────────────────────── */
    val ui by vm.state.collectAsState()

    /* ── 4) Tabs (“Chats” / “Archive”) ────────────────────────────────────── */
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
            ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Error: ${ui.error}")
            }
            else -> {
                val visibleList = if (tab == 0) ui.visibleActive else ui.visibleArchive
                val hasMore     = if (tab == 0) ui.hasMoreActive  else ui.hasMoreArchive

                if (visibleList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No chats.") }
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleList, key = { it.id }) { chat ->
                            ChatCard(
                                chat = chat,
                                onClick = {
                                    // Build route: chatDetail/{chatTitle}/{eventId}/{token}/{myUserId}
                                    val route = buildString {
                                        append("chatDetail/")
                                        append(
                                            URLEncoder.encode(
                                                chat.title,
                                                StandardCharsets.UTF_8.toString()
                                            )
                                        )
                                        append("/")
                                        append(chat.id)          // <-- eventId
                                        append("/")
                                        append(
                                            URLEncoder.encode(
                                                token,
                                                StandardCharsets.UTF_8.toString()
                                            )
                                        )
                                        append("/")
                                        append(myUserId)
                                    }
                                    navController.navigate(route)
                                }
                            )
                        }

                        if (hasMore) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    Alignment.Center
                                ) {
                                    Button(
                                        onClick = {
                                            if (tab == 0) vm.loadMoreActive()
                                            else           vm.loadMoreArchive()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                            .padding(8.dp)
                                    ) { Text("Load more") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
