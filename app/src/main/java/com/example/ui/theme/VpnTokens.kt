package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for the RedShift VPN premium redesign.
 * Dark graphite + glassmorphism + neon-green accents.
 */
object VpnColors {
    val Background = BackgroundGraphite
    val BackgroundTop = BackgroundGraphite
    val BackgroundMid = BackgroundGraphiteMid
    val BackgroundDeep = BackgroundGraphiteDeep

    val Surface = SurfaceGlass
    val SurfaceElevated = com.example.ui.theme.SurfaceElevated
    val SurfaceNav = Color(0xFF111420)
    val SurfaceButton = SurfaceInner

    val TextPrimary = com.example.ui.theme.TextPrimary
    val TextSecondary = com.example.ui.theme.TextSecondary
    val TextMuted = com.example.ui.theme.TextMuted

    val Success = AccentNeonGreen
    val SuccessDark = Color(0xFF1D7D45)
    val Warning = AccentWarning
    val Error = AccentError

    val AccentBlue = com.example.ui.theme.AccentBlue
    val AccentPurple = PremiumStart
    val BrandCyan = PremiumCyan

    val Divider = BorderGraphite
    val Icon = Color(0xFFF7F9FC)

    // Glass system
    val surfaceGlass = SurfaceGlass
    val surfaceGlassLight = SurfaceGlassLight
    val surfaceInner = SurfaceInner
    val borderLight = BorderGraphite
    val borderGlass = BorderGlass
    val accentGreen = AccentNeonGreen
    val accentGreenSoft = AccentGreenSoft
    val accentGreenGlow = AccentGreenGlow
    val accentWarning = AccentWarning
    val accentError = AccentError
    val premiumGradient = listOf(PremiumStart, PremiumEnd)

    // Servers screen
    val surfaceCardSolid = SurfaceCardSolid
    val textTertiary = TextTertiary

    // Planet
    val planetOcean = PlanetOcean
    val planetLand = PlanetLand
    val planetAtmosphere = PlanetAtmosphere
    val planetGlow = PlanetGlow
    val spaceDark = SpaceDark
}

object VpnDimensions {
    val ScreenPaddingCompact = 16.dp
    val ScreenPaddingDefault = 22.dp

    val RadiusSmall = 16.dp
    val RadiusMedium = 24.dp
    val RadiusLarge = 30.dp
    val RadiusXLarge = 36.dp

    val TopBarHeight = 56.dp
    val BottomNavHeight = 76.dp

    val PowerButtonCompact = 120.dp
    val PowerButtonDefault = 132.dp
    val PowerButtonWide = 148.dp

    val FloatingCardMinHeight = 104.dp
    val ServerCardMinHeight = 84.dp
    val ConnectTouchTarget = 48.dp

    // Planet
    val PlanetSize = 280.dp
    val PlanetGlowSpread = 40.dp
}

object VpnTypography {
    val TimerSemiCompact = 42.sp
    val TimerDefault = 48.sp

    val StatusBig = 20.sp
    val StatusConnecting = 20.sp
    val Endpoint = 15.sp
    val SecurityLabel = 16.sp

    val timer = TextStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        fontFeatureSettings = "tnum"
    )
    val premium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    val statusMain = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    val statusSecured = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = AccentNeonGreen
    )
    val cardTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    val cardSubtitle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = TextMuted
    )
    val headerSection = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    )
    val statsValue = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    val statsLabel = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = TextMuted
    )
    val buttonText = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    )
}

val MainBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        BackgroundGraphite,
        BackgroundGraphiteMid,
        BackgroundGraphiteDeep
    )
)

val AccentBrush = Brush.horizontalGradient(
    colors = listOf(PremiumStart, PremiumEnd)
)

val GreenAccentBrush = Brush.horizontalGradient(
    colors = listOf(AccentNeonGreen, AccentGreenBright)
)

/** Ping quality color thresholds. */
fun pingQualityColor(pingMs: Int): Color {
    if (pingMs <= 0) return VpnColors.TextMuted
    return when {
        pingMs < 50 -> VpnColors.Success
        pingMs < 150 -> VpnColors.Warning
        else -> VpnColors.Error
    }
}
