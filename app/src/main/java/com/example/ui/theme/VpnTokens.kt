package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for the premium dark VPN home screen.
 * Reuses the existing palette values so the rest of the app stays in sync.
 */
object VpnColors {
    val Background = CyberBackground
    val BackgroundTop = Color(0xFF061426)
    val BackgroundDeep = CyberBackgroundDeep

    val Surface = CyberCard
    val SurfaceElevated = CyberElevated
    val SurfaceNav = Color(0xFF172238)
    val SurfaceButton = VpnSurfaceTertiary

    val TextPrimary = com.example.ui.theme.TextPrimary
    val TextSecondary = com.example.ui.theme.TextSecondary
    val TextMuted = com.example.ui.theme.TextMuted

    val Success = SuccessGreen
    val SuccessDark = Color(0xFF1D7D45)
    val Warning = WarningAmber
    val Error = ErrorRed

    val AccentBlue = PremiumBlue
    val AccentPurple = PremiumPurple
    val BrandCyan = Color(0xFF25B9FF)

    val Divider = VpnDivider
    val Icon = Color(0xFFF7F9FC)

    // Home redesign (from AI reference spec)
    val surfaceGlass = SurfaceGlass
    val surfaceInner = SurfaceInner
    val borderLight = BorderGraphite
    val accentGreen = AccentNeonGreen
    val accentWarning = AccentWarning
    val accentError = AccentError
    val premiumGradient = listOf(PremiumStart, PremiumEnd)

    // Servers screen (from AI reference spec)
    val surfaceCardSolid = SurfaceCardSolid
    val textTertiary = TextTertiary
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

    val PowerButtonCompact = 148.dp
    val PowerButtonDefault = 164.dp
    val PowerButtonWide = 176.dp

    val FloatingCardMinHeight = 104.dp
    val ServerCardMinHeight = 84.dp
    val ConnectTouchTarget = 48.dp
}

object VpnTypography {
    val TimerSemiCompact = 42.sp
    val TimerDefault = 48.sp

    val StatusBig = 20.sp
    val StatusConnecting = 20.sp
    val Endpoint = 15.sp
    val SecurityLabel = 16.sp

    // Home redesign styles (from AI reference spec)
    val timer = TextStyle(
        fontSize = 42.sp,
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
        VpnColors.BackgroundTop,
        VpnColors.Background,
        VpnColors.BackgroundDeep
    )
)

val AccentBrush = Brush.horizontalGradient(
    colors = listOf(VpnColors.AccentBlue, VpnColors.AccentPurple)
)

/** Ping quality color thresholds (shared with server lists when applicable). */
fun pingQualityColor(pingMs: Int): Color {
    if (pingMs <= 0) return VpnColors.TextMuted
    return when {
        pingMs < 50 -> VpnColors.Success
        pingMs < 150 -> VpnColors.Warning
        else -> VpnColors.Error
    }
}