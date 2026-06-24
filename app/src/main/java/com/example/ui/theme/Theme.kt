package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = RedPrimary,
    secondary = PurpleSecondary,
    tertiary = AmberWarning,
    background = CyberBackground,
    surface = CyberCard,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = CyberBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for cyberpunk aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
