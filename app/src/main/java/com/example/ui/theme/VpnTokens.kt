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
    val SurfaceNav = Color(0xDD0A111B)
    val SurfaceButton = SurfaceInner

    val TextPrimary = com.example.ui.theme.TextPrimary
    val TextSecondary = com.example.ui.theme.TextSecondary
    val TextMuted = com.example.ui.theme.TextMuted

    val Success = StatusGreen
    val SuccessDark = Color(0xFF1D7D45)
    val Warning = StatusAmber
    val Error = StatusRed

    val AccentBlue = com.example.ui.theme.AccentBlue
    val AccentPurple = com.example.ui.theme.AccentBlue
    val BrandCyan = AccentCyan

    val Divider = BorderNavy
    val Icon = Color(0xFFE8E8F0)

    // Glass system
    val surfaceGlass = SurfaceGlass
    val surfaceGlassLight = SurfaceGlassLight
    val surfaceGlassBright = SurfaceGlassBright
    val surfaceInner = SurfaceInner
    val surfaceCard = SurfaceCard
    val borderLight = BorderNavy
    val borderGlass = BorderGlass

    // Accents (Red Pill primary, Blue secondary)
    val accentPrimary = AccentRed           // Red Pill
    val accentPrimarySoft = AccentRedSoft
    val accentPrimaryGlow = AccentRedGlow
    val accentSecondary = AccentBlue        // Blue
    val accentSecondarySoft = AccentBlueSoft
    val accentSecondaryGlow = AccentBlueGlow

    // Legacy compat aliases
    val accentGreen = AccentRed
    val accentGreenSoft = AccentRedSoft
    val accentGreenGlow = AccentRedGlow
    val accentWarning = StatusAmber
    val accentError = StatusRed
    val premiumGradient = listOf(AccentRed, AccentBlue)

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
    val ScreenPaddingDefault = 22.dp

    val RadiusSmall = 14.dp
    val RadiusMedium = 20.dp
    val RadiusLarge = 28.dp
    val RadiusXLarge = 36.dp

    val TopBarHeight = 56.dp
    val BottomNavHeight = 72.dp

    val PowerButtonCompact = 120.dp
    val PowerButtonDefault = 140.dp
    val PowerButtonWide = 160.dp

    val FloatingCardMinHeight = 104.dp
    val ServerCardMinHeight = 84.dp
    val ConnectTouchTarget = 48.dp

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
        BackgroundNavy,
        BackgroundNavyMid,
        BackgroundNavyDeep
    )
)

val AccentBrush = Brush.horizontalGradient(
    colors = listOf(AccentRed, AccentBlue)
)

val RedAccentBrush = Brush.horizontalGradient(
    colors = listOf(AccentRed, AccentRedBright)
)

fun pingQualityColor(pingMs: Int): Color {
    if (pingMs <= 0) return VpnColors.TextMuted
    return when {
        pingMs < 50 -> StatusGreen
        pingMs < 150 -> StatusAmber
        else -> StatusRed
    }
}
