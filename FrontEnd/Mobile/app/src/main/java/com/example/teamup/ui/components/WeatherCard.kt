
package com.example.teamup.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.teamup.data.remote.model.WeatherDto

// mostra os detalhes da metereologia
@Composable
private fun weatherIconFor(description: String?): ImageVector {
    val desc = description?.lowercase() ?: ""
    return when {
        "thunder" in desc || "storm" in desc   -> Icons.Filled.FlashOn
        "rain" in desc || "drizzle" in desc    -> Icons.Filled.Umbrella
        "snow" in desc || "sleet" in desc      -> Icons.Filled.AcUnit
        "sun" in desc || "clear" in desc       -> Icons.Filled.WbSunny
        "cloud" in desc                        -> Icons.Filled.Cloud
        "fog" in desc || "mist" in desc        -> Icons.Filled.BlurOn
        else                                   -> Icons.Filled.Cloud
    }
}

@Composable
fun WeatherCard(
    weather: WeatherDto,
    modifier: Modifier = Modifier
) {

    val t       = weather.temp        ?: "—"
    val hi      = weather.high_temp   ?: "—"
    val lo      = weather.low_temp    ?: "—"
    val descRaw = weather.description ?: "—"
    val desc    = descRaw.replaceFirstChar { it.uppercase() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        androidx.compose.material3.Icon(
            imageVector = weatherIconFor(descRaw),
            contentDescription = descRaw,
            modifier = Modifier
                .size(48.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Temperature
        Text(
            text = "$t°C",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp
            )
        )

        Spacer(Modifier.height(4.dp))

        // High / Low
        Text(
            text = "H $hi°C   L $lo°C",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(4.dp))

        // Description
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
