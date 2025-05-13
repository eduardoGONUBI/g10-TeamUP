// app/src/main/java/com/example/teamup/ui/theme/Theme.kt
package com.example.teamup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary        = Primary,
    onPrimary      = OnPrimary,
    secondary      = Secondary,
    onSecondary    = OnSecondary,
    tertiary       = Tertiary,
    onTertiary     = OnTertiary,
    background     = Background,
    onBackground   = OnBackground,
    surface        = Surface,
    onSurface      = OnSurface
)

@Composable
fun TeamUPTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
