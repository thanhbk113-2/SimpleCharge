package com.example.simplecharge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color.Black,
    primaryContainer = Slate800,
    onPrimaryContainer = Emerald400,
    secondary = Cyan500,
    onSecondary = Color.Black,
    secondaryContainer = Slate800,
    onSecondaryContainer = Cyan400,
    tertiary = Amber500,
    onTertiary = Color.Black,
    background = Slate900,
    onBackground = Slate200,
    surface = Slate800,
    onSurface = Slate200,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate600
)

@Composable
fun SimpleChargeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
