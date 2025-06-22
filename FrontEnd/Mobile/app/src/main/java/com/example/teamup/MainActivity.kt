package com.example.teamup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.teamup.data.local.AppDatabase
import com.example.teamup.data.local.SessionRepository
import com.example.teamup.ui.navigation.AppNavGraph
import com.example.teamup.ui.theme.TeamUPTheme
import com.google.android.libraries.places.api.Places
import java.net.URLEncoder

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // google places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        setContent {
            TeamUPTheme {

                // controlador de navegaçao
                val navController = rememberNavController()

                 //sessao local
                val context = LocalContext.current
                val sessionRepo = remember {
                    SessionRepository(AppDatabase.get(context).sessionDao())
                }

                //Define as rotas da app com base no NavController.
                AppNavGraph(navController = navController)

               // Pede permissão de notificações
                NotificationPermissionRequester()

               // Se existir token guardado, faz login automático
                LaunchedEffect(Unit) {
                    sessionRepo.token.collect { token ->
                        if (token != null) {
                            val encoded = URLEncoder.encode(token, "UTF-8")
                            navController.navigate("main/$encoded") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }
}

// pede permissao para notificaçoes
@Composable
private fun NotificationPermissionRequester() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {  }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!alreadyGranted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
