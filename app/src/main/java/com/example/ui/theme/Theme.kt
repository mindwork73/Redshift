package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentNeonGreen,
    secondary = PremiumStart,
    tertiary = AccentWarning,
    background = BackgroundGraphite,
    surface = SurfaceGlass,
    onPrimary = BackgroundGraphite,
    onSecondary = TextPrimary,
    onTertiary = BackgroundGraphite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
