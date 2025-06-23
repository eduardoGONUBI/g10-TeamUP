// app/src/main/java/com/example/teamup/ui/screens/SplashLogoScreen.kt
package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teamup.R
import kotlinx.coroutines.delay

@Composable
fun SplashLogoScreen(onContinue: () -> Unit) {
    // After a short delay, call onContinue() to navigate away
    LaunchedEffect(Unit) {
        delay(1000)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .width(200.dp)
                .height(200.dp)
        )
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SplashPreview() {
    SplashLogoScreen {}
}
