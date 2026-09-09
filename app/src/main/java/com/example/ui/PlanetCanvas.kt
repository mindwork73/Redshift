@file:JvmName("PlanetCanvasKt")

package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Interactive planet Earth — now uses blue atmosphere glow
 * (matching the navy + red pill design language).
 */
@Composable
fun PlanetCanvas(
    connectionState: ConnectionState,
    serverCoords: Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planet")

    val idleRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "idle_rotation"
    )

    val atmospherePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "atmosphere"
    )

    val markerPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "marker_pulse"
    )

    val markerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "marker_alpha"
    )

    val targetRotation = remember(connectionState, serverCoords) {
        when (connectionState) {
            ConnectionState.CONNECTED -> serverCoords?.second?.toFloat()?.unaryPlus() ?: 0f
            ConnectionState.CONNECTING -> (serverCoords?.second?.toFloat() ?: 0f) + 30f
            ConnectionState.DISCONNECTED -> 0f
        }
    }

    val animatedTargetRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = if (connectionState == ConnectionState.CONNECTING) 1500 else 2000,
            easing = FastOutSlowInEasing
        ),
        label = "target_rotation"
    )

    val currentRotation = when (connectionState) {
        ConnectionState.DISCONNECTED -> idleRotation
        else -> animatedTargetRotation
    }

    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING
    val isActive = isConnected || isConnecting

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val planetRadius = min(size.width, size.height) * 0.38f

        drawStars(size)

        // Atmosphere glow — BLUE for navy theme
        val atmosphereColor = if (isActive) AccentRed else AccentBlue
        drawCircle(color = atmosphereColor.copy(alpha = 0.04f * atmospherePulse), radius = planetRadius + 60f, center = Offset(cx, cy))
        drawCircle(color = atmosphereColor.copy(alpha = 0.06f * atmospherePulse), radius = planetRadius + 40f, center = Offset(cx, cy))
        drawCircle(color = atmosphereColor.copy(alpha = 0.1f * atmospherePulse), radius = planetRadius + 20f, center = Offset(cx, cy))

        // Planet sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF122040), Color(0xFF0A1628), Color(0xFF060D1A)),
                center = Offset(cx - planetRadius * 0.3f, cy - planetRadius * 0.3f),
                radius = planetRadius * 1.4f
            ),
            radius = planetRadius, center = Offset(cx, cy)
        )

        drawContinents(cx, cy, planetRadius, currentRotation, isActive)

        // Atmosphere rim
        drawCircle(color = atmosphereColor.copy(alpha = 0.15f), radius = planetRadius + 2f, center = Offset(cx, cy), style = Stroke(width = 4f))
        drawCircle(color = atmosphereColor.copy(alpha = 0.08f), radius = planetRadius + 6f, center = Offset(cx, cy), style = Stroke(width = 8f))

        // Specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx - planetRadius * 0.4f, cy - planetRadius * 0.4f),
                radius = planetRadius * 0.8f
            ),
            radius = planetRadius, center = Offset(cx, cy)
        )

        // Server marker
        if (isActive && serverCoords != null) {
            drawServerMarker(cx, cy, planetRadius, serverCoords, currentRotation, markerPulse, markerAlpha)
        }

        drawGridLines(cx, cy, planetRadius, currentRotation)
    }
}

private fun DrawScope.drawStars(size: Size) {
    val starSeed = listOf(
        0.1f to 0.15f, 0.85f to 0.1f, 0.15f to 0.85f, 0.9f to 0.9f,
        0.5f to 0.05f, 0.05f to 0.5f, 0.95f to 0.5f, 0.5f to 0.95f,
        0.3f to 0.2f, 0.7f to 0.8f, 0.2f to 0.7f, 0.8f to 0.3f
    )
    for ((fx, fy) in starSeed) {
        drawCircle(color = Color.White.copy(alpha = 0.3f + fx * 0.4f), radius = 1f + fx * 1.5f, center = Offset(size.width * fx, size.height * fy))
    }
}

private fun DrawScope.drawContinents(cx: Float, cy: Float, radius: Float, rotation: Float, isActive: Boolean) {
    val landColor = if (isActive) Color(0xFF1B4A2A) else Color(0xFF1B3A2A)
    val landHighlight = if (isActive) Color(0xFF2A6B3A) else Color(0xFF254D35)
    val continents = listOf(
        ContinentDef(10f, 50f, 0.18f, 0.12f), ContinentDef(20f, 0f, 0.15f, 0.25f),
        ContinentDef(80f, 40f, 0.3f, 0.2f), ContinentDef(-100f, 45f, 0.22f, 0.18f),
        ContinentDef(-60f, -15f, 0.12f, 0.22f), ContinentDef(135f, -25f, 0.1f, 0.08f)
    )
    for (cont in continents) {
        val adjustedLon = ((cont.lon + rotation) % 360f + 360f) % 360f
        val lonRad = Math.toRadians(adjustedLon.toDouble())
        val latRad = Math.toRadians(cont.lat.toDouble())
        val x3d = cos(latRad) * sin(lonRad); val y3d = -sin(latRad); val z3d = cos(latRad) * cos(lonRad)
        if (z3d < -0.1) continue
        val screenX = cx + (x3d * radius).toFloat(); val screenY = cy + (y3d * radius).toFloat()
        val scale = maxOf(0.3f, z3d.toFloat()); val w = radius * cont.widthFactor * scale; val h = radius * cont.heightFactor * scale
        drawOval(color = landColor.copy(alpha = 0.7f * scale), topLeft = Offset(screenX - w / 2, screenY - h / 2), size = Size(w, h))
        drawOval(color = landHighlight.copy(alpha = 0.3f * scale), topLeft = Offset(screenX - w / 2 + w * 0.1f, screenY - h / 2 + h * 0.1f), size = Size(w * 0.6f, h * 0.6f))
    }
}

private data class ContinentDef(val lon: Float, val lat: Float, val widthFactor: Float, val heightFactor: Float)

private fun DrawScope.drawServerMarker(cx: Float, cy: Float, planetRadius: Float, coords: Pair<Double, Double>, rotation: Float, pulseScale: Float, pulseAlpha: Float) {
    val adjustedLon = ((coords.second.toFloat() + rotation) % 360f + 360f) % 360f
    val lonRad = Math.toRadians(adjustedLon.toDouble()); val latRad = Math.toRadians(coords.first.toDouble())
    val x3d = cos(latRad) * sin(lonRad); val y3d = -sin(latRad); val z3d = cos(latRad) * cos(lonRad)
    if (z3d < 0) return
    val screenX = cx + (x3d * planetRadius).toFloat(); val screenY = cy + (y3d * planetRadius).toFloat()
    val markerRadius = 5f + z3d.toFloat() * 3f
    drawCircle(color = AccentRed.copy(alpha = pulseAlpha * z3d.toFloat()), radius = markerRadius * pulseScale, center = Offset(screenX, screenY), style = Stroke(width = 2f))
    drawCircle(color = AccentRed, radius = markerRadius, center = Offset(screenX, screenY))
    drawCircle(color = AccentRedBright, radius = markerRadius * 0.5f, center = Offset(screenX, screenY))
    drawCircle(brush = Brush.radialGradient(colors = listOf(AccentRed.copy(alpha = 0.4f * z3d.toFloat()), Color.Transparent), center = Offset(screenX, screenY), radius = markerRadius * 4f), radius = markerRadius * 4f, center = Offset(screenX, screenY))
}

private fun DrawScope.drawGridLines(cx: Float, cy: Float, radius: Float, rotation: Float) {
    val gridColor = Color.White.copy(alpha = 0.03f)
    for (lon in -180..180 step 30) {
        val adjustedLon = ((lon + rotation) % 360f + 360f) % 360f
        val lonRad = Math.toRadians(adjustedLon.toDouble()); val x3d = sin(lonRad); val z3d = cos(lonRad)
        if (z3d < 0) continue
        val screenX = cx + (x3d * radius).toFloat()
        drawLine(color = gridColor, start = Offset(screenX, cy - radius * z3d.toFloat()), end = Offset(screenX, cy + radius * z3d.toFloat()), strokeWidth = 0.5f)
    }
}
