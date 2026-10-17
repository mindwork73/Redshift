package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shape scale (REDESIGN.md §5.4). No sharp corners anywhere in the app.
 */
object RedRadius {
    val Small: Dp = 12.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 20.dp
    val XLarge: Dp = 28.dp
    val Pill: Dp = 999.dp
}

/** 4dp spacing grid (REDESIGN.md §5.4). */
object RedSpace {
    val Xxs: Dp = 4.dp
    val Xs: Dp = 8.dp
    val S: Dp = 12.dp
    val M: Dp = 16.dp
    val L: Dp = 20.dp
    val Xl: Dp = 24.dp
    val Xxl: Dp = 32.dp
}

/** Fixed component metrics (REDESIGN.md §5.4 / §7). */
object RedSize {
    /** Connect button on Home. */
    val PowerButton: Dp = 192.dp

    /** Server list row. */
    val ServerRow: Dp = 72.dp

    /** Generic list row. */
    val ListRow: Dp = 64.dp

    /** Distance between two sections. */
    val SectionGap: Dp = 32.dp

    /** Bottom navigation bar (without insets). */
    val NavBar: Dp = 80.dp

    /** Primary button. */
    val Button: Dp = 56.dp
}

private val DarkColorScheme =
    darkColorScheme(
        primary = Accent,
        onPrimary = TextPrimary,
        primaryContainer = AccentSoft,
        onPrimaryContainer = TextPrimary,
        secondary = TextSecondary,
        onSecondary = Background,
        tertiary = Success,
        onTertiary = Background,
        background = Background,
        onBackground = TextPrimary,
        surface = GlassFill,
        onSurface = TextPrimary,
        surfaceVariant = GlassFillStrong,
        onSurfaceVariant = TextSecondary,
        outline = GlassBorder,
        outlineVariant = Divider,
        error = Danger,
        onError = TextPrimary,
        scrim = Background
    )

/**
 * Entry point required by `MainActivity` — signature is frozen (REDESIGN.md §2).
 *
 * The design is dark-only: [darkTheme] and [dynamicColor] are accepted for call-site
 * compatibility but never switch the palette (REDESIGN.md §7).
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Dark-only by design: `darkTheme` / `dynamicColor` are kept for call-site
    // compatibility but never switch the palette.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
