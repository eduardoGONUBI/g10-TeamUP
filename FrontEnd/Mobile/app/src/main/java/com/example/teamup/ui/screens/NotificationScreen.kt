package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.teamup.R

/* -------- dados de exemplo -------- */
data class Notify(
    val icon: Int,
    val title: String,
    val subtitle: String
)

private val sample = mutableStateListOf(
    Notify(R.drawable.baseline_sports_soccer_24,   "New activity of Football near you",     "Escola Secundária Barcelos, 09/02/2025 15:00"),
    Notify(R.drawable.baseline_sports_basketball_24,"MariaPO joined your activity",         "Activity: BarcelosBasket"),
    Notify(R.drawable.baseline_sports_tennis_24,   "Joao05 edited your activity",           "Activity: TennisFofo – New Time 10:00")
)
/* ----------------------------------- */

@Composable
fun NotificationScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Cabeçalho simples (podes retirar se já usares TopAppBar externo)
        Text(
            "Notifications",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline)
        )

        /* ---------- Lista ---------- */
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(sample) { note ->
                NotificationCard(note) { sample.remove(note) }
            }
        }

        /* ---------- Botão Clear ---------- */
        Button(
            onClick = { sample.clear() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Clear")
        }
    }
}

@Composable
private fun NotificationCard(
    note: Notify,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(note.icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(note.title, fontWeight = FontWeight.Bold)
                Text(note.subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFFF8A80),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDelete() }
            )
        }
    }
}
