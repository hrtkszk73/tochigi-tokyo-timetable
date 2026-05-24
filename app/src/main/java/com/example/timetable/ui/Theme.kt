package com.example.timetable.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    secondary = Color(0xFFB8C8DB),
    tertiary = Color(0xFFD6BEE4),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F5DAA),
    secondary = Color(0xFF55617A),
    tertiary = Color(0xFF7C5184),
)

@Composable
fun TimetableAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        supportsDynamic && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        supportsDynamic && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
