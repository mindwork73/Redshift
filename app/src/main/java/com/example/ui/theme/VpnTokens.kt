package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VpnColors {
    val Background = BackgroundNavy
    val BackgroundTop = BackgroundNavy
    val BackgroundMid = BackgroundNavyMid
    val BackgroundDeep = BackgroundNavyDeep

    val Surface = SurfaceGlass
    val SurfaceElevated = SurfaceElevated
    val SurfaceNav = Color(0xE60A0E17)
    val SurfaceButton = SurfaceInner

    val TextPrimary = com.example.ui.theme.TextPrimary
    val TextSecondary = com.example.ui.theme.TextSecondary
    val TextMuted = com.example.ui.theme.TextMuted

    val Success = StatusGreen
    val SuccessDark = Color(0xFF0E8E52)
    val Warning = StatusAmber
    val Error = StatusRed

    val AccentBlue = com.example.ui.theme.AccentBlue
    val AccentPurple = com.example.ui.theme.AccentRed
    val BrandCyan = AccentCyan

    val Divider = BorderNavy
    val Icon = TextPrimary

    // Glass system
    val surfaceGlass = SurfaceGlass
    val surfaceGlassLight = SurfaceGlassLight
    val surfaceGlassBright = SurfaceGlassBright
    val surfaceInner = SurfaceInner
    val surfaceCard = SurfaceCard
    val borderLight = BorderNavy
    val borderGlass = BorderGlass

    // Accents
    val accentPrimary = AccentRed
    val accentPrimarySoft = AccentRedSoft
    val accentPrimaryGlow = AccentRedGlow
    val accentSecondary = AccentBlue
    val accentSecondarySoft = AccentBlueSoft
    val accentSecondaryGlow = AccentBlueGlow
    val accentGreen = StatusGreen
    val accentGreenSoft = AccentGreenSoft
    val accentGreenGlow = AccentGreenGlow
    val accentWarning = StatusAmber
    val accentError = StatusRed
    val premiumGradient = listOf(AccentBlue, AccentRed)

    val surfaceCardSolid = SurfaceCard
    val textTertiary = TextMuted

    // Planet
    val planetOcean = BackgroundNavy
    val planetLand = SurfaceCard
    val planetAtmosphere = AccentBlueGlow
    val planetGlow = AccentBlueGlow
    val spaceDark = BackgroundNavyDeep
}

object VpnDimensions {
    val ScreenPaddingCompact = 16.dp
    val ScreenPaddingDefault = 16.dp

    val RadiusSmall = 16.dp
    val RadiusMedium = 20.dp
    val RadiusLarge = 24.dp
    val RadiusXLarge = 32.dp

    val TopBarHeight = 56.dp
    val BottomNavHeight = 72.dp

    val PowerButtonCompact = 120.dp
    val PowerButtonDefault = 120.dp
    val PowerButtonWide = 240.dp

    val FloatingCardMinHeight = 104.dp
    val ServerCardMinHeight = 72.dp
    val ConnectTouchTarget = 48.dp

    val PlanetSize = 280.dp
    val PlanetGlowSpread = 40.dp
}

object VpnTypography {
    val TimerSemiCompact = 36.sp
    val TimerDefault = 36.sp
    val StatusBig = 20.sp
    val StatusConnecting = 20.sp
    val Endpoint = 14.sp
    val SecurityLabel = 12.sp

    val displayLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        letterSpacing = 0.1.sp
    )
    val countryTitle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
    val timer = displayLarge
    val premium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        letterSpacing = 0.1.sp
    )
    val statusMain = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    val statusSecured = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = StatusGreen
    )
    val cardTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    val cardSubtitle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = TextSecondary
    )
    val bodyRegular = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = TextPrimary
    )
    val headerSection = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
    )
    val statsValue = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
    val statsLabel = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = TextSecondary
    )
    val buttonText = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

val MainBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        BackgroundNavy,
        BackgroundNavyMid,
        BackgroundNavyDeep
    )
)

val AccentBrush = Brush.horizontalGradient(
    colors = listOf(AccentBlue, AccentRed)
)

val RedAccentBrush = Brush.horizontalGradient(
    colors = listOf(AccentBlue, AccentRed)
)

fun pingQualityColor(pingMs: Int): Color {
    if (pingMs <= 0) return VpnColors.TextMuted
    return when {
        pingMs < 50 -> StatusGreen
        pingMs < 150 -> StatusAmber
        else -> StatusRed
    }
}
