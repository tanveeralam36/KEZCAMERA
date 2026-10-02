package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color.Black,
    primaryContainer = StudioCyan.copy(alpha = 0.2f),
    onPrimaryContainer = StudioCyan,
    secondary = StudioYellow,
    onSecondary = Color.Black,
    secondaryContainer = StudioYellow.copy(alpha = 0.2f),
    onSecondaryContainer = StudioYellow,
    tertiary = StudioRed,
    onTertiary = Color.White,
    background = StudioDarkBg,
    onBackground = StudioTextPrimary,
    surface = StudioTopBarBg,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioCardBg,
    onSurfaceVariant = StudioTextSecondary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
