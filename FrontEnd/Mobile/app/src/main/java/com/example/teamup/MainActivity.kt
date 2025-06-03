// app/src/main/java/com/example/teamup/MainActivity.kt
package com.example.teamup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.teamup.ui.AppNavGraph
import com.example.teamup.ui.theme.TeamUPTheme
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        setContent {
            TeamUPTheme {
                AppNavGraph()
            }
        }
    }
}
