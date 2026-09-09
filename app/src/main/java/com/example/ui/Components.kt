package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    glowColor: Color = AccentNeonGreen,
    glowWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = glowColor.copy(alpha = 0.1f)
            )
            .border(
                BorderStroke(
                    glowWidth,
                    Brush.verticalGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.4f),
                            BorderGraphite.copy(alpha = 0.6f)
                        )
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        color = SurfaceGlass.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = AccentNeonGreen,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(PremiumStart, PremiumEnd)
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(TextMuted.copy(alpha = 0.5f), TextMuted.copy(alpha = 0.5f))
                    )
                }
            )
            .then(
                if (enabled) Modifier.drawBehind {
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderGraphite,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceGlass.copy(alpha = 0.85f))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun NeonPill(
    text: String,
    color: Color = AccentNeonGreen,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProtocolBadge(
    protocol: String,
    modifier: Modifier = Modifier
) {
    val color = when {
        protocol.contains("VLESS") -> AccentNeonGreen
        protocol.contains("VMess") -> AccentWarning
        protocol.contains("Trojan") -> PremiumStart
        protocol.contains("Shadowsocks") -> AccentBlue
        protocol.contains("Socks") -> PremiumCyan
        protocol.contains("Hysteria") -> AccentError
        protocol.contains("Amnezia") -> AccentNeonGreen
        else -> TextSecondary
    }
    NeonPill(text = protocol, color = color, modifier = modifier)
}

@Composable
fun LatencyBadge(
    ping: Int,
    modifier: Modifier = Modifier
) {
    if (ping <= 0) {
        NeonPill(text = "—", color = TextMuted, modifier = modifier)
        return
    }

    val color = when {
        ping <= 50 -> AccentNeonGreen
        ping <= 150 -> AccentWarning
        else -> AccentError
    }
    NeonPill(text = "${ping}ms", color = color, modifier = modifier)
}

@Composable
fun MiniStatPill(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    glowColor: Color = AccentNeonGreen
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGlass.copy(alpha = 0.7f))
            .border(0.5.dp, glowColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Column {
                Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Normal)
                Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PulsingConnectionRing(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteSpec(1500),
        label = "scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteSpec(1500),
        label = "alpha"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val color = when (connectionState) {
        ConnectionState.DISCONNECTED -> TextPrimary
        ConnectionState.CONNECTING -> AccentWarning
        ConnectionState.CONNECTED -> AccentNeonGreen
    }

    Box(
        modifier = modifier.size(160.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (connectionState != ConnectionState.CONNECTING) {
            Box(
                modifier = Modifier
                    .size(130.dp * pulseScale)
                    .border(
                        BorderStroke(1.5.dp, color.copy(alpha = pulseAlpha)),
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .drawBehind {
                    if (connectionState == ConnectionState.CONNECTING) {
                        drawArc(
                            color = color,
                            startAngle = spinAngle,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                            )
                        )
                    } else {
                        drawCircle(
                            color = color.copy(alpha = 0.15f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SurfaceInner, BackgroundGraphite)
                    )
                )
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
                                radius = size.minDimension / 2
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (connectionState) {
                    ConnectionState.DISCONNECTED -> PowerIcon(color = color)
                    ConnectionState.CONNECTING -> CircularProgressIndicator(
                        color = color,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    ConnectionState.CONNECTED -> ShieldCheckIcon(color = color)
                }
            }
        }
    }
}

private fun <T> infiniteSpec(duration: Int): InfiniteRepeatableSpec<T> {
    return infiniteRepeatable(
        animation = tween(duration, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Restart
    )
}

@Composable
fun PowerIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width
        val h = size.height
        drawArc(
            color = color,
            startAngle = -220f,
            sweepAngle = 260f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(w / 2, h / 5),
            end = Offset(w / 2, h / 2),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun ShieldCheckIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width
        val h = size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.15f, h * 0.25f)
            lineTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.25f)
            cubicTo(w * 0.85f, h * 0.55f, w * 0.5f, h * 0.88f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.5f, h * 0.88f, w * 0.15f, h * 0.55f, w * 0.15f, h * 0.25f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.48f),
            end = Offset(w * 0.47f, h * 0.6f),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.47f, h * 0.6f),
            end = Offset(w * 0.68f, h * 0.38f),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun TrafficActivityBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "activity_bars")

    val height1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(height1).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(height2).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.width(3.dp).fillMaxHeight(height3).clip(RoundedCornerShape(1.dp)).background(color))
    }
}
