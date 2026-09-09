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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CyberCard(modifier: Modifier = Modifier, glowColor: Color = AccentRed, glowWidth: Dp = 1.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = glowColor.copy(alpha = 0.08f))
            .border(BorderStroke(glowWidth, Brush.verticalGradient(listOf(glowColor.copy(alpha = 0.3f), BorderNavy.copy(alpha = 0.5f)))), shape = RoundedCornerShape(20.dp)),
        color = SurfaceGlass.copy(alpha = 0.85f), shape = RoundedCornerShape(20.dp)
    ) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

@Composable
fun CyberButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(modifier = modifier.clip(RoundedCornerShape(100.dp))
        .background(if (enabled) Brush.horizontalGradient(listOf(AccentRed, AccentBlue)) else Brush.horizontalGradient(listOf(TextMuted.copy(alpha = 0.5f), TextMuted.copy(alpha = 0.5f))))
        .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp, horizontal = 24.dp), contentAlignment = Alignment.Center
    ) { Text(text, color = if (enabled) Color.White else TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SurfaceGlass.copy(alpha = 0.85f)).border(1.dp, BorderNavy, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Column(content = content)
    }
}

@Composable
fun NeonPill(text: String, color: Color = AccentRed, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(100.dp)).background(color.copy(alpha = 0.1f))
        .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(100.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun ProtocolBadge(protocol: String, modifier: Modifier = Modifier) {
    val color = when {
        protocol.contains("VLESS") -> AccentRed
        protocol.contains("VMess") -> StatusAmber
        protocol.contains("Trojan") -> AccentBlue
        protocol.contains("Shadowsocks") -> AccentBlue
        protocol.contains("Hysteria") -> StatusRed
        else -> TextSecondary
    }
    NeonPill(text = protocol, color = color, modifier = modifier)
}

@Composable
fun LatencyBadge(ping: Int, modifier: Modifier = Modifier) {
    if (ping <= 0) { NeonPill("—", TextMuted, modifier); return }
    val color = when { ping <= 50 -> StatusGreen; ping <= 150 -> StatusAmber; else -> StatusRed }
    NeonPill("${ping}ms", color, modifier)
}

@Composable
fun PulsingConnectionRing(connectionState: ConnectionState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "scale")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "alpha")
    val color = when (connectionState) { ConnectionState.DISCONNECTED -> TextPrimary; ConnectionState.CONNECTING -> StatusAmber; ConnectionState.CONNECTED -> AccentRed }

    Box(modifier = modifier.size(160.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(130.dp * pulseScale).border(BorderStroke(1.5.dp, color.copy(alpha = pulseAlpha)), CircleShape))
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Brush.radialGradient(listOf(SurfaceInner, BackgroundNavy))).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            when (connectionState) {
                ConnectionState.DISCONNECTED -> PowerIcon(color)
                ConnectionState.CONNECTING -> CircularProgressIndicator(color = color, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                ConnectionState.CONNECTED -> ShieldCheckIcon(color)
            }
        }
    }
}

@Composable
fun PowerIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width; val h = size.height
        drawArc(color = color, startAngle = -220f, sweepAngle = 260f, useCenter = false, style = Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawLine(color, Offset(w / 2, h / 5), Offset(w / 2, h / 2), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
fun ShieldCheckIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply { moveTo(w*0.15f,h*0.25f); lineTo(w*0.5f,h*0.12f); lineTo(w*0.85f,h*0.25f); cubicTo(w*0.85f,h*0.55f,w*0.5f,h*0.88f,w*0.5f,h*0.88f); cubicTo(w*0.5f,h*0.88f,w*0.15f,h*0.55f,w*0.15f,h*0.25f); close() }
        drawPath(path, color, style = Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawLine(color, Offset(w*0.35f,h*0.48f), Offset(w*0.47f,h*0.6f), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(w*0.47f,h*0.6f), Offset(w*0.68f,h*0.38f), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
fun MiniStatPill(icon: String, label: String, value: String, modifier: Modifier = Modifier, glowColor: Color = AccentRed) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceGlass.copy(alpha = 0.7f)).border(0.5.dp, glowColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, fontSize = 14.sp)
            Column { Text(label, color = TextMuted, fontSize = 10.sp); Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun TrafficActivityBars(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bars")
    val h1 by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "h1")
    val h2 by t.animateFloat(0.8f, 0.1f, infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse), label = "h2")
    val h3 by t.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "h3")
    Row(modifier = modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        Box(Modifier.width(3.dp).fillMaxHeight(h1).clip(RoundedCornerShape(1.dp)).background(color))
        Box(Modifier.width(3.dp).fillMaxHeight(h2).clip(RoundedCornerShape(1.dp)).background(color))
        Box(Modifier.width(3.dp).fillMaxHeight(h3).clip(RoundedCornerShape(1.dp)).background(color))
    }
}
