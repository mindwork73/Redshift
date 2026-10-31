package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import com.example.R
import androidx.compose.ui.unit.sp

/**
 * Typography of the "Liquid Glass Dark" system (REDESIGN.md §5.3).
 *
 * Inter is bundled so the same typography is used offline on every screen.
 * Numeric readouts use tabular figures ("tnum") so the session timer does not jitter.
 */
val Inter = FontFamily(Font(R.font.inter_variable, FontWeight.Normal))

object RedType {
    /** Session timer on Home: 56sp Light, tabular figures. */
    val Timer = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        fontFeatureSettings = "tnum"
    )

    /** Main screen title: 32sp Bold. */
    val Display = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    )

    /** Card titles: 20sp SemiBold. */
    val Title = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    /** Main text: 16sp Regular. */
    val Body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    /** Active values / metrics: 15sp Medium. */
    val BodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp
    )

    /** Captions, secondary text: 13sp Regular. */
    val Caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )

    /** Badges and small caps: 12sp Medium. */
    val CaptionMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp
    )

    /** IP addresses, ports: 14sp monospace. */
    val Mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
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
