package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
// ═══  REDSHIFT — Dark Navy + Red Pill + Blue Glass  ═══
// ═══════════════════════════════════════════════════════════

// ─── Backgrounds (deep navy, NOT pure black) ───
val BackgroundNavy = Color(0xFF0A111B)
val BackgroundNavyDeep = Color(0xFF070D16)
val BackgroundNavyMid = Color(0xFF0F1A27)

// ─── Surfaces (glass with blue tint) ───
val SurfaceGlass = Color(0xB3162030)        // glass with blue tint
val SurfaceGlassLight = Color(0x99203040)
val SurfaceGlassBright = Color(0xCC283C56)   // brighter cards (bottom area)
val SurfaceCard = Color(0xFF1A2636)
val SurfaceCardBright = Color(0xFF283C56)
val SurfaceInner = Color(0xFF1E2A3A)
val SurfaceElevated = Color(0xFF2A3E58)

// ─── Borders ───
val BorderNavy = Color(0xFF2A3A50)
val BorderGlass = Color(0x2A4060A0)
val BorderSubtle = Color(0x1A4060A0)

// ─── PRIMARY ACCENT: Red Pill ───
val AccentRed = Color(0xFFD42030)
val AccentRedBright = Color(0xFFE83040)
val AccentRedSoft = Color(0x33D42030)
val AccentRedGlow = Color(0x44D42030)

// ─── SECONDARY ACCENT: Blue / Violet ───
val AccentBlue = Color(0xFF4050B0)
val AccentBlueBright = Color(0xFF5060D0)
val AccentBlueSoft = Color(0x334050B0)
val AccentBlueGlow = Color(0x444050B0)
val AccentCyan = Color(0xFF30A0C0)

// ─── Status ───
val StatusGreen = Color(0xFF30C060)
val StatusAmber = Color(0xFFE0A030)
val StatusRed = Color(0xFFE03040)

// ─── Text ───
val TextPrimary = Color(0xFFE8E8F0)
val TextSecondary = Color(0xFF8898B0)
val TextMuted = Color(0xFF506070)
val TextDim = Color(0xFF354050)

// ─── Legacy compat (keep for service references) ───
val RedPrimary = AccentRed
val RedGradientEnd = Color(0xFF8B12D4)
val PurpleSecondary = AccentBlue
val AmberWarning = StatusAmber
val WarningAmber = StatusAmber
val ErrorRed = StatusRed
val CyberBackground = BackgroundNavy
val CyberBackgroundDeep = BackgroundNavyDeep
val CyberCard = SurfaceCard
val CyberElevated = SurfaceElevated
val VpnSurfaceTertiary = SurfaceInner
val TextTertiary = TextMuted
val SuccessGreen = StatusGreen
val SuccessGreenSoft = Color(0x3330C060)
val VpnDivider = BorderNavy
val PremiumBlue = AccentBlue
val PremiumPurple = AccentBlue
val PremiumViolet = AccentBlue
val PowerButtonSurface = SurfaceInner
val GlowRed = AccentRedGlow
val GlowPurple = AccentBlueGlow
val GlowGreen = Color(0x3330C060)
val GlassCard = SurfaceGlass
val GlassBorder = BorderGlass
val BackgroundGraphite = BackgroundNavy
val BackgroundGraphiteMid = BackgroundNavyMid
val BackgroundGraphiteDeep = BackgroundNavyDeep
val SurfaceGlassLight0 = SurfaceGlassLight
val SurfaceCardSolid = SurfaceCard
val BorderGraphite = BorderNavy
val AccentNeonGreen = AccentRed         // remap! Red Pill accent
val AccentGreenSoft = AccentRedSoft
val AccentGreenGlow = AccentRedGlow
val AccentGreenBright = AccentRedBright
val AccentWarning = StatusAmber
val AccentError = StatusRed
val PremiumStart = AccentRed
val PremiumEnd = AccentBlue
val PremiumCyan = AccentCyan
val PlanetOcean = BackgroundNavy
val PlanetLand = SurfaceCard
val PlanetAtmosphere = AccentBlueGlow
val PlanetGlow = AccentBlueGlow
val SpaceDark = BackgroundNavyDeep
