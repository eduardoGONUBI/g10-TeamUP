// File: app/src/main/java/com/example/teamup/TeamUpApp.kt
package com.example.teamup

import android.app.Application
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class TeamUpApp : Application(), OnMapsSdkInitializedCallback {

    override fun onCreate() {
        super.onCreate()
        // Dispara o download/descompactação dos binários do renderer
        MapsInitializer.initialize(
            applicationContext,
            MapsInitializer.Renderer.LATEST,  // ou LEGACY para opt-out
            this                             // callback → onMapsSdkInitialized()
        )
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        Log.d("TeamUpApp", "Maps SDK ready with $renderer renderer ✅")
        // A partir daqui qualquer GoogleMap desenha tiles instantaneamente
    }
}
