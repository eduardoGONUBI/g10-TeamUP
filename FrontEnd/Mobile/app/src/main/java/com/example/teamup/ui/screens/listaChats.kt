package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teamup.R   // ícones vector na pasta res/drawable

/* ---------- 1. Dados fictícios ---------- */
data class ChatSummary(
    val id: Int,
    val title: String,
    val subtitle: String,
    val iconRes: Int          // drawable do desporto
)

private val sampleChats = listOf(
    ChatSummary(1, "Futebolada", "Football", R.drawable.baseline_sports_soccer_24),
    ChatSummary(2, "BarcelosBasket", "Basketball", R.drawable.baseline_sports_basketball_24),
    ChatSummary(3, "Semanas Ténis 2", "Tennis", R.drawable.baseline_sports_tennis_24)
)

/* ---------- 2. Ecrã principal ---------- */
@Composable
fun ChatScreen(
    onTabChange: (Int) -> Unit = {},
    onBottomNavClick: (BottomDest) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = { ChatTopBar() },
        bottomBar = {
            ChatBottomBar(
                current = BottomDest.CHAT,
                onClick = onBottomNavClick
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {

            /* ---- Tabs ---- */
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Chats", "Archive").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx; onTabChange(idx) },
                        text = { Text(label) }
                    )
                }
            }

            /* ---- Lista ---- */
            if (selectedTab == 0) {      // Chats
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleChats) { chat ->
                        ChatCard(chat)
                    }
                }
            } else {
                // Conteúdo do separador Archive
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem conversas arquivadas.")
                }
            }
        }
    }
}

/* ---------- 3. Sub‑componentes ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar() {
    TopAppBar(
        title = { },
        navigationIcon = {
            Image(
                painter = painterResource(id = R.drawable.baseline_cruelty_free_24),
                contentDescription = "Logo TeamUp",
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        actions = {
            IconButton(onClick = { /* TODO: open notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notificações")
            }
        }
    )
}

@Composable
private fun ChatCard(chat: ChatSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(chat.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 12.dp)
            )
            Column {
                Text(chat.title, style = MaterialTheme.typography.titleMedium)
                Text(chat.subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

enum class BottomDest(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    CALENDAR(Icons.Default.DateRange),
    CHAT(Icons.Default.Email),
    PROFILE(Icons.Default.Person)
}

@Composable
private fun ChatBottomBar(
    current: BottomDest,
    onClick: (BottomDest) -> Unit
) {
    NavigationBar {
        BottomDest.values().forEach { dest ->
            NavigationBarItem(
                selected = dest == current,
                onClick = { onClick(dest) },
                icon = { Icon(dest.icon, contentDescription = dest.name) },
                label = { Text(dest.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

/* ---------- 4. Preview ---------- */
@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    ChatScreen()
}
