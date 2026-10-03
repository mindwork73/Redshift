package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography of the "Liquid Glass Dark" system (REDESIGN.md §5.3).
 *
 * System font only ([FontFamily.Default]) — an iOS-like stack, no bundled faces.
 * Numeric readouts use tabular figures ("tnum") so the session timer does not jitter.
 */
object RedType {
    /** Session timer on Home: 56sp Light, tabular figures. */
    val Timer = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        fontFeatureSettings = "tnum"
    )

    /** Main screen title: 32sp Bold. */
    val Display = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    )

    /** Card titles: 20sp SemiBold. */
    val Title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    /** Main text: 16sp Regular. */
    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    /** Active values / metrics: 15sp Medium. */
    val BodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    /** Captions, secondary text: 13sp Regular. */
    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    /** Badges and small caps: 12sp Medium. */
    val CaptionMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )

    /** IP addresses, ports: 14sp monospace. */
    val Mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    )
}

/** Material typography wired to the same tokens, for anything using [androidx.compose.material3.Text] defaults. */
val Typography =
    Typography(
        headlineLarge = RedType.Display,
        headlineMedium = RedType.Display,
        titleLarge = RedType.Title,
        titleMedium = RedType.Title,
        bodyLarge = RedType.Body,
        bodyMedium = RedType.BodyMedium,
        bodySmall = RedType.Caption,
        labelLarge = RedType.BodyMedium,
        labelMedium = RedType.CaptionMedium,
        labelSmall = RedType.CaptionMedium
    )
