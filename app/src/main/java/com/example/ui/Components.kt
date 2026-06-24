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
    glowColor: Color = RedPrimary,
    glowWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                BorderStroke(
                    glowWidth, 
                    Brush.verticalGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.6f),
                            glowColor.copy(alpha = 0.08f)
                        )
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        color = CyberCard.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp),
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
    glowColor: Color = RedPrimary,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(RedPrimary, RedGradientEnd)
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(TextMuted, TextMuted)
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .border(1.dp, glowColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
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
fun MiniStatPill(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    glowColor: Color = PurpleSecondary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberElevated)
            .border(0.5.dp, glowColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Column {
                Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Normal)
                Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProtocolBadge(
    protocol: String,
    modifier: Modifier = Modifier
) {
    val color = when {
        protocol.contains("VLESS") -> RedPrimary
        protocol.contains("VMess") -> Color(0xFFFF9100)
        protocol.contains("Trojan") -> PurpleSecondary
        protocol.contains("Shadowsocks") -> Color(0xFF2979FF)
        protocol.contains("Socks") -> Color(0xFF00B0FF)
        protocol.contains("Hysteria") -> Color(0xFFFF4081)
        protocol.contains("Amnezia") -> Color(0xFF00E676)
        else -> TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = protocol,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LatencyBadge(
    ping: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        ping <= 50 -> SuccessGreen
        ping <= 150 -> WarningAmber
        else -> ErrorRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "${ping}ms",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
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
        targetValue = 1.25f,
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
        ConnectionState.DISCONNECTED -> RedPrimary
        ConnectionState.CONNECTING -> AmberWarning
        ConnectionState.CONNECTED -> SuccessGreen
    }

    val glowColor = when (connectionState) {
        ConnectionState.DISCONNECTED -> GlowRed
        ConnectionState.CONNECTING -> Color(0x22FFD740)
        ConnectionState.CONNECTED -> GlowGreen
    }

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring (only when connected or disconnected)
        if (connectionState != ConnectionState.CONNECTING) {
            Box(
                modifier = Modifier
                    .size(120.dp * pulseScale)
                    .border(
                        BorderStroke(1.5.dp, color.copy(alpha = pulseAlpha)),
                        shape = CircleShape
                    )
            )
        }

        // Inner glowing and spinning dash border when connecting
        Box(
            modifier = Modifier
                .size(116.dp)
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

        // Core central button
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberElevated, CyberBackground)
                    )
                )
                .border(2.dp, color, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Internal neon visual effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                                radius = size.minDimension / 2
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (connectionState) {
                    ConnectionState.DISCONNECTED -> {
                        // Power icon
                        PowerIcon(color = color)
                    }
                    ConnectionState.CONNECTING -> {
                        // Tiny spin loader
                        CircularProgressIndicator(
                            color = color,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    ConnectionState.CONNECTED -> {
                        // Shield checkmark
                        ShieldCheckIcon(color = color)
                    }
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
        val center = Offset(w / 2, h / 2)
        
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
        
        // Checkmark
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
