package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens of the "Liquid Glass Dark" design system (REDESIGN.md §5.2).
 *
 * Every colour used by the UI is declared here once. Screens and components must not
 * declare their own colours — they read these tokens (or the [VpnColors] grouping).
 *
 * The theme is dark by design: light values do not exist for this product.
 */

// ─── Scene background ───
val Background = Color(0xFF050507)
val BackgroundGradientTop = Color(0xFF0D1117)
val BackgroundGradientBottom = Color(0xFF050507)

// ─── Glass surfaces ───
val GlassFill = Color(0x12FFFFFF) // white, alpha 0.07
val GlassFillStrong = Color(0x1CFFFFFF) // white, alpha 0.11
val GlassBorder = Color(0x29FFFFFF) // white, alpha 0.16
val GlassBorderTop = Color(0x47FFFFFF) // white, alpha 0.28

// ─── Text ───
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA2A2AF)
val TextTertiary = Color(0xFF6E6E78)

// ─── Accent ("red pill") ───
val Accent = Color(0xFFFF3B30)
val AccentSoft = Color(0x24FF3B30) // alpha 0.14
val AccentDark = Color(0xFFC42B22) // gradient partner of [Accent]

// ─── Semantic status ───
val Success = Color(0xFF30D158)
val SuccessSoft = Color(0x2430D158) // alpha 0.14
val Warning = Color(0xFFFFD60A)
val WarningSoft = Color(0x24FFD60A) // alpha 0.14
val Danger = Color(0xFFFF453A)
val DangerSoft = Color(0x24FF453A) // alpha 0.14

// ─── Chrome ───
val NavBarFill = Color(0xB30A0A0C) // #0A0A0C, alpha 0.7
val Divider = Color(0x14FFFFFF) // white, alpha 0.08

/**
 * Single grouping object for the tokens above (REDESIGN.md §5.2 asks for the tokens to be
 * grouped in `VpnColors`). Prefer these members inside components/screens.
 */
object VpnColors {
    val Background = com.example.ui.theme.Background
    val BackgroundGradientTop = com.example.ui.theme.BackgroundGradientTop
    val BackgroundGradientBottom = com.example.ui.theme.BackgroundGradientBottom

    val GlassFill = com.example.ui.theme.GlassFill
    val GlassFillStrong = com.example.ui.theme.GlassFillStrong
    val GlassBorder = com.example.ui.theme.GlassBorder
    val GlassBorderTop = com.example.ui.theme.GlassBorderTop

    val TextPrimary = com.example.ui.theme.TextPrimary
    val TextSecondary = com.example.ui.theme.TextSecondary
    val TextTertiary = com.example.ui.theme.TextTertiary

    val Accent = com.example.ui.theme.Accent
    val AccentSoft = com.example.ui.theme.AccentSoft
    val AccentDark = com.example.ui.theme.AccentDark

    val Success = com.example.ui.theme.Success
    val SuccessSoft = com.example.ui.theme.SuccessSoft
    val Warning = com.example.ui.theme.Warning
    val WarningSoft = com.example.ui.theme.WarningSoft
    val Danger = com.example.ui.theme.Danger
    val DangerSoft = com.example.ui.theme.DangerSoft

    val NavBarFill = com.example.ui.theme.NavBarFill
    val Divider = com.example.ui.theme.Divider

    /** Glass edge highlight — the thin glint that sits on the top edge of a card. */
    val GlassGlint = Color(0x38FFFFFF) // white, alpha 0.22
}

/**
 * Latency → colour mapping shared by every screen (REDESIGN.md §5.2).
 * 0 means "not measured yet" and must render as "—", never as a number.
 */
fun pingColor(latencyMs: Int): Color = when {
    latencyMs <= 0 -> VpnColors.TextTertiary
    latencyMs < 100 -> VpnColors.Success
    latencyMs <= 250 -> VpnColors.Warning
    else -> VpnColors.Danger
}
