package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = AccentBlue,
    tertiary = StatusAmber,
    background = BackgroundNavy,
    surface = SurfaceGlass,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = BackgroundNavy,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
