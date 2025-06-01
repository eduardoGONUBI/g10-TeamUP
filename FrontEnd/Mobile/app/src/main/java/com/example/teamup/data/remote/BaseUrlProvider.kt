// app/src/main/java/com/example/teamup/data/remote/BaseUrlProvider.kt
package com.example.teamup.data.remote

import android.os.Build
import com.example.teamup.BuildConfig
object BaseUrlProvider {
    /**
     * Detect “generic” fingerprint to see if running on an emulator.
     * On an emulator, Build.FINGERPRINT usually contains “generic”.
     */
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.contains("vbox")     ||
                Build.MODEL.contains("Emulator")        ||
                Build.MODEL.contains("Android SDK built for x86")
    }

    fun getBaseUrl(): String {
        return if (isEmulator()) {
            BuildConfig.BASE_URL_EMULATOR
        } else {
            BuildConfig.BASE_URL_DEVICE
        }
    }
}
