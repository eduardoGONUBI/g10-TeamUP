package com.example.teamup.ui.screens

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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import kotlinx.coroutines.launch

/* --- modelo + dados de exemplo --- */
data class Message(
    val fromMe: Boolean,
    val author: String,
    val text: String,
    val time: String
)

private val dummy = listOf(
    Message(false, "Jav",    "Hi team 👋",                        "11:31 AM"),
    Message(false, "Jav",    "Anyone on for lunch?",              "11:31 AM"),
    Message(true,  "Me",     "I’m down! Any ideas??",             "11:35 AM"),
    Message(false, "Aubrey", "I was thinking the café downtown",  "11:45 AM"),
    Message(false, "Aubrey", "But limited vegan options Janet!", "11:46 AM"),
    Message(true,  "Me",     "Agreed",                            "11:52 PM")
)

/**
 * Ecrã de detalhe de chat sem conflitar com a barra de navegação do Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatTitle: String,
    onBack: () -> Unit
) {
    val messages = remember { dummy }
    var input    by remember { mutableStateOf("") }
    val scope    = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Aplica padding de cima/baixo para IME + navBar
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.ime)
            )
    ) {
        // TopAppBar sempre no topo
        TopAppBar(
            title = { Text(chatTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            }
        )

        // Lista de mensagens (ocupa todo o espaço restante)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(msg)
            }
        }

        // Row de input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier    = Modifier.weight(1f),
                placeholder = { Text("Message") },
                singleLine  = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { scope.launch { input = "" } }
                )
            )
            IconButton(
                onClick = { scope.launch { input = "" } },
                enabled = input.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message) {
    val bubbleColor = if (msg.fromMe)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val alignment = if (msg.fromMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color          = bubbleColor,
            shape          = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Column(Modifier.padding(8.dp)) {
                if (!msg.fromMe) {
                    Text(
                        text  = msg.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(text = msg.text)
            }
        }
        Text(
            text     = msg.time,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
