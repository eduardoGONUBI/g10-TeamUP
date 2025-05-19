package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teamup.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/* ---------- modelos dummy ---------- */
data class Participant(val avatar: Int, val name: String, val rating: Double)

private val participants = mutableStateListOf(
    Participant(R.drawable.avatar_1,  "Sergio01",         4.9),
    Participant(R.drawable.avatar_2,  "SavannahNguyen",   4.7),
    Participant(R.drawable.avatar_3,  "DarleneRobertson", 0.0),
    Participant(R.drawable.avatar_4,  "LeslieAlexander",  3.1),
    Participant(R.drawable.avatar_5,  "AlbertFlores",     4.0),
    Participant(R.drawable.avatar_6,  "DianneRussell",    2.8),
    Participant(R.drawable.avatar_7,  "RobertFox",        3.9),
    Participant(R.drawable.avatar_4,  "LeslieAlexander",  4.6),
    Participant(R.drawable.avatar_8,  "DarrellSteward",   4.5),
    Participant(R.drawable.avatar_9,  "ArleneMcCoy",      3.0),
    Participant(R.drawable.avatar_10, "JohnLenon",        1.0)
)
/* ----------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorActivityScreen(
    onBack: () -> Unit = {}
) {
    // Posição aproximada do Complexo Desportivo da Rodovia (Braga)
    val braga = remember { LatLng(41.5535, -8.4170) }
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(braga, 16f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {

        /* ---------- Header ---------- */
        item {
            TopAppBar(
                title = { Text("Creator See Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(R.drawable.arrow_back, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* share */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "Voltar"
                        )
                    }
                    IconButton(onClick = { /* edit */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }

        /* ---------- Foto de capa ---------- */
        item {
            Image(
                painter = painterResource(R.drawable.sample_activity_photo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }

        /* ---------- Card com info da atividade ---------- */
        item {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Futebolada – Sergio01", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("05/02/2025, 17:00")
                    Text("Complexo Desportivo da Rodovia")
                }
            }
        }

        /* ---------- Mapa ---------- */
        item {
            Text(
                "Location",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState
                ) {
                    Marker(
                        state = MarkerState(position = braga),
                        title = "Complexo Desportivo da Rodovia"
                    )
                }
            }
        }

        /* ---------- Título + badge ---------- */
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "Participants",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${participants.size}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF7065FF),
                        labelColor = Color.White
                    )
                )
            }
        }

        /* ---------- Cabeçalho da tabela ---------- */
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Participant Name",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rating",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        /* ---------- Lista de participantes ---------- */
        items(participants) { p ->
            ParticipantRow(
                participant = p,
                onDelete = { participants.remove(p) }
            )
        }
    }
}

@Composable
private fun ParticipantRow(participant: Participant, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(participant.avatar),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                participant.name,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint = Color(0xFFFF8A80)
                )
            }
            Spacer(Modifier.width(4.dp))
            AssistChip(
                onClick = {},
                label = { Text("%.1f".format(participant.rating)) },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.frame),
                        contentDescription = null,
                        tint = Color(0xFFFFC107)
                    )
                }
            )
        }
    }
}
