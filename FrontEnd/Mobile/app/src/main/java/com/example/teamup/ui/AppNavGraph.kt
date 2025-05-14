package com.example.teamup.ui
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
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

        // Conjunto de ecrãs COM scaffold
        composable("main") { RootScaffold() }
    }
}
