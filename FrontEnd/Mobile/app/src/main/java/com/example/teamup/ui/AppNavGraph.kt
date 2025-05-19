package com.example.teamup.ui
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamup.ui.screens.ChatDetailScreen
import com.example.teamup.ui.screens.CreatorActivityScreen
import com.example.teamup.ui.screens.SplashLogoScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "splash") {

        // Ecrã SEM scaffold
        composable("splash") {
            SplashLogoScreen(
                onContinue = {
                    nav.navigate("main") { popUpTo("splash") { inclusive = true } }
                }
            )
        }

        composable(
            "chatDetail/{chatTitle}",
            arguments = listOf(navArgument("chatTitle") { type = NavType.StringType })
        ) { backStack ->
            val title = backStack.arguments?.getString("chatTitle") ?: ""
            ChatDetailScreen(chatTitle = title, onBack = { nav.popBackStack() })
        }
        composable("creator_activity") { CreatorActivityScreen() }


        // Conjunto de ecrãs COM scaffold
        composable("main") { RootScaffold(appNav = nav) }
    }
}
