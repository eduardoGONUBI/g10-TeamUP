package com.example.teamup.ui.screens.Chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import kotlinx.coroutines.launch

// Representa uma mensagem na conversa
data class Message(
    val fromMe: Boolean,  // Se a mensagem foi enviada por mim
    val author: String,   // Nome do autor da mensagem
    val text: String,     // Conteúdo da mensagem
    val time: String      // Hora a que foi enviada
)

// Lista fixa de mensagens para demonstração
private val dummy = listOf(
    Message(false, "Jav",    "Hi team 👋",                        "11:31 AM"),
    Message(false, "Jav",    "Anyone on for lunch?",              "11:31 AM"),
    Message(true,  "Me",     "I’m down! Any ideas??",             "11:35 AM"),
    Message(false, "Aubrey", "I was thinking the café downtown",  "11:45 AM"),
    Message(false, "Aubrey", "But limited vegan options Janet!", "11:46 AM"),
    Message(true,  "Me",     "Agreed",                            "11:52 PM")
)

/**
 * Ecrã de detalhe de chat que ajusta espaço para teclado e barra de navegação
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatTitle: String,   // Título do chat a exibir no topo
    onBack: () -> Unit   // Callback para ação de voltar atrás
) {
    val messages = remember { dummy }                     // Mensagens a mostrar
    var input    by remember { mutableStateOf("") }       // Texto atual do campo de input
    val scope    = rememberCoroutineScope()               // Coroutine scope para ações assíncronas

    Column(
        modifier = Modifier
            .fillMaxSize()  // Ocupa todo o ecrã
            // Aplica padding para não sobrepor teclado (IME) nem barra de navegação
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.ime)
            )
    ) {
        // Barra de topo com título e botão de voltar
        TopAppBar(
            title = { Text(chatTitle) },   // Exibe o nome do chat
            navigationIcon = {
                IconButton(onClick = onBack) {  // Volta ao ecrã anterior
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            }
        )

        // Lista rolável de bolhas de mensagem
        LazyColumn(
            modifier = Modifier
                .weight(1f)              // Ocupa todo o espaço restante
                .fillMaxWidth()          // Largura total
                .padding(horizontal = 8.dp, vertical = 4.dp),
            reverseLayout = true          // Inverte a ordem: mensagens recentes em baixo
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(msg)       // Componente que desenha cada bolha
            }
        }

        // Linha de input para enviar nova mensagem
        Row(
            modifier = Modifier
                .fillMaxWidth()          // Largura total
                .padding(8.dp),          // Espaço à volta
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Campo de texto para escrever a mensagem
            OutlinedTextField(
                value = input,                       // Texto atual
                onValueChange = { input = it },      // Atualiza o estado quando muda
                modifier    = Modifier.weight(1f),   // Ocupa espaço disponível
                placeholder = { Text("Message") },   // Texto sugerido
                singleLine  = true,                  // Apenas uma linha
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {                      // Ação ao pressionar “Enviar” no teclado
                        scope.launch { input = "" } // Limpa o campo
                    }
                )
            )
            // Botão de enviar mensagem
            IconButton(
                onClick = { scope.launch { input = "" } },  // Limpa o campo ao enviar
                enabled = input.isNotBlank()                // Só ativo se houver texto
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}

/**
 * Bolha de mensagem individual
 */
@Composable
private fun MessageBubble(msg: Message) {
    // Define cor de fundo consoante quem enviou
    val bubbleColor = if (msg.fromMe)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    // Alinhamento da bolha (à direita se for minha, à esquerda caso contrário)
    val alignment = if (msg.fromMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()         // Largura total
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment  // Alinha horizontalmente
    ) {
        // Superfície que envolve o conteúdo da mensagem (bolha)
        Surface(
            color          = bubbleColor,                 // Cor de fundo
            shape          = MaterialTheme.shapes.medium, // Bordas arredondadas
            tonalElevation = 2.dp                         // Elevation para sombra
        ) {
            Column(Modifier.padding(8.dp)) {
                if (!msg.fromMe) {
                    // Exibe nome do autor acima da mensagem se não for auto
                    Text(
                        text  = msg.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Texto da mensagem
                Text(text = msg.text)
            }
        }
        // Hora da mensagem abaixo da bolha
        Text(
            text     = msg.time,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}