// AppNavGraph.kt
package com.example.teamup.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamup.ui.screens.Chat.ChatDetailScreen
import com.example.teamup.ui.screens.CreatorActivityScreen
import com.example.teamup.ui.screens.EditProfileScreen
import com.example.teamup.ui.screens.SplashLogoScreen

@Composable
fun AppNavGraph() {
    // Cria o controlador de navegação
    val nav = rememberNavController()

    // Define o host de navegação, com o ecrã inicial "splash"
    NavHost(navController = nav, startDestination = "splash") {

        // Ecrã sem scaffold (Splash)
        composable("splash") {
            SplashLogoScreen(
                onContinue = {
                    // Quando continuar, navega para "main" e remove o "splash" da pilha
                    nav.navigate("main") { popUpTo("splash") { inclusive = true } }
                }
            )
        }

        // Ecrã de detalhe de chat com argumento "chatTitle"
        composable(
            "chatDetail/{chatTitle}",
            arguments = listOf(navArgument("chatTitle") { type = NavType.StringType })
        ) { backStack ->
            // Obtém o título do chat a partir dos argumentos
            val title = backStack.arguments?.getString("chatTitle") ?: ""
            ChatDetailScreen(
                chatTitle = title,
                onBack = { nav.popBackStack() } // Volta ao ecrã anterior
            )
        }

        // Ecrã para criar atividade
        composable("creator_activity") {
            CreatorActivityScreen()
        }

        // Ecrã para editar perfil
        composable("edit_profile") {
            EditProfileScreen(
                onBack = { nav.popBackStack() } // Volta ao ecrã anterior
            )
        }

        //  Ecrãs com scaffold
        composable("main") {
            RootScaffold(appNav = nav)
        }
    }
}
