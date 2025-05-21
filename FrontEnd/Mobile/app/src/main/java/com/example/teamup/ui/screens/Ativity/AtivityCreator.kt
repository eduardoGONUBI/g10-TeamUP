// CreatorActivityScreen.kt (ficheiro completo)

package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.teamup.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
/* ---------- modelos dummy ---------- */
// Data class para representar cada participante
data class Participant(val avatar: Int, val name: String, val rating: Double)

// Lista mutável de participantes de exemplo
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
    onBack: () -> Unit = {}  // Callback para ação de voltar atrás
) {
    // Define localização inicial do mapa (Braga)
    val braga = remember { LatLng(41.5535, -8.4170) }
    // Estado da câmara do GoogleMap com posição e zoom
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(braga, 16f)
    }

    // Container principal com fundo e padding para não sobrepor barras do sistema
    Column(
        modifier = Modifier
            .fillMaxSize()  // Ocupa todo o ecrã
            .windowInsetsPadding(WindowInsets.navigationBars)  // Evita barra de navegação
            .background(MaterialTheme.colorScheme.background)  // Cor de fundo do tema
    ) {
        /* ---------- Header fixo ---------- */
        TopAppBar(
            title = { Text("Creator See Activity") },  // Título no AppBar
            navigationIcon = {
                // Ícone de voltar atrás
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Voltar"
                    )
                }
            },
            actions = {
                // Ícone para partilhar atividade
                IconButton(onClick = { /* partilhar */ }) {
                    Icon(
                        painter = painterResource(R.drawable.join_activity),
                        contentDescription = "Partilhar"
                    )
                }
                // Ícone para editar atividade
                IconButton(onClick = { /* editar */ }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
            },
            modifier = Modifier.statusBarsPadding()  // Afasta da status bar
        )

        /* ---------- Conteúdo rolável ---------- */
        LazyColumn(
            modifier = Modifier.fillMaxSize(),  // Ocupa todo o espaço disponível
            contentPadding = PaddingValues(bottom = 24.dp)  // Padding inferior
        ) {

            /* Foto de capa */
            item {
                Image(
                    painter = painterResource(R.drawable.sample_activity_photo),
                    contentDescription = null,  // Imagem decorativa
                    modifier = Modifier
                        .fillMaxWidth()  // Largura total
                        .height(180.dp), // Altura fixa
                    contentScale = ContentScale.Crop  // Ajusta corte da imagem
                )
            }

            /* Card com info da atividade */
            item {
                Card(
                    modifier = Modifier
                        .padding(16.dp)   // Espaço à volta do card
                        .fillMaxWidth(),  // Largura total
                    elevation = CardDefaults.cardElevation(4.dp)  // Sombra leve
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Futebolada – Sergio01", fontWeight = FontWeight.Bold)  // Título
                        Spacer(Modifier.height(4.dp))  // Espaço vertical
                        Text("05/02/2025, 17:00")  // Data e hora
                        Text("Complexo Desportivo da Rodovia")  // Localização
                    }
                }
            }

            /* Mapa da localização */
            item {
                Text(
                    "Location",
                    style    = MaterialTheme.typography.titleMedium,  // Estilo de título
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(200.dp),  // Altura para o mapa
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState
                    ) {
                        // Marcador no local definido
                        Marker(
                            state = MarkerState(position = braga),
                            title = "Complexo Desportivo da Rodovia"
                        )
                    }
                }
            }

            /* Participants + badge */
            item {
                Spacer(Modifier.height(16.dp))  // Espaço antes do título
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Participants",
                        style    = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)  // Preenche espaço disponível
                    )
                    AssistChip(
                        onClick = {},  // Ação ao clicar no badge
                        label   = { Text("${participants.size}") },  // Número de participantes
                        colors  = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF7065FF),
                            labelColor     = Color.White
                        )
                    )
                }
            }

            /* Cabeçalho da tabela de participantes */
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("Participant Name", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Rating", fontWeight = FontWeight.Bold)
                    }
                }
            }

            /* Lista de participantes (cada item é uma row) */
            items(participants) { p ->
                ParticipantRow(p) { participants.remove(p) }  // Remove participante ao clicar no delete
            }
        }
    }
}

@Composable
private fun ParticipantRow(p: Participant, onDelete: () -> Unit) {
    // Card que envolve cada linha de participante
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),  // Padding interno
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar do participante
            Image(
                painter = painterResource(p.avatar),
                contentDescription = null,
                modifier = Modifier.size(32.dp)  // Tamanho fixo
            )
            Spacer(Modifier.width(8.dp))  // Espaço horizontal
            Text(p.name, Modifier.weight(1f))  // Nome com peso para ocupar espaço
            // Botão de eliminar participante
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint               = Color(0xFFFF8A80)  // Cor de alerta
                )
            }
            Spacer(Modifier.width(4.dp))
            // Badge com rating do participante
            AssistChip(
                onClick = {},  // Ação ao clicar no chip
                label   = { Text("%.1f".format(p.rating)) },  // Formata rating
                leadingIcon = {
                    Icon(
                        painter            = painterResource(R.drawable.frame),
                        contentDescription = null,
                        tint               = Color(0xFFFFC107)  // Cor dourada
                    )
                }
            )
        }
    }
}