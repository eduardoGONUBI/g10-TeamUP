package com.example.teamup.ui.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teamup.R

@Composable
fun SplashLogoScreen(onContinue: () -> Unit) {
    // Exemplo simples: após 1 s avança para a app (podes usar LaunchedEffect)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Logo"
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SplashPreview() {
    SplashLogoScreen {}
}

