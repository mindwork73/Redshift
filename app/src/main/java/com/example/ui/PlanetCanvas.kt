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
 * Interactive planet Earth rendered with Compose Canvas.
 * - Idle: slow rotation when disconnected
 * - Connecting: faster rotation towards target
 * - Connected: faces server location, green glow pulse
 */
@Composable
fun PlanetCanvas(
    connectionState: ConnectionState,
    serverCoords: Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planet")

    // Idle rotation angle (always running)
    val idleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_rotation"
    )

    // Atmosphere pulse
    val atmospherePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atmosphere"
    )

    // Server marker pulse
    val markerPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marker_pulse"
    )

    val markerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marker_alpha"
    )

    // Calculate rotation offset based on connection state
    val targetRotation = remember(connectionState, serverCoords) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                if (serverCoords != null) {
                    // Convert longitude to rotation angle
                    (serverCoords.second.toFloat() * -1f)
                } else 0f
            }
            ConnectionState.CONNECTING -> {
                if (serverCoords != null) {
                    (serverCoords.second.toFloat() * -1f) + 30f
                } else 0f
            }
            ConnectionState.DISCONNECTED -> 0f
        }
    }

    // Animated rotation towards target when connecting/connected
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

        // ─── Space background with stars ───
        drawStars(size)

        // ─── Atmosphere outer glow ───
        val atmosphereColor = if (isActive) AccentNeonGreen else AccentBlue
        drawCircle(
            color = atmosphereColor.copy(alpha = 0.04f * atmospherePulse),
            radius = planetRadius + 60f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = atmosphereColor.copy(alpha = 0.06f * atmospherePulse),
            radius = planetRadius + 40f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = atmosphereColor.copy(alpha = 0.1f * atmospherePulse),
            radius = planetRadius + 20f,
            center = Offset(cx, cy)
        )

        // ─── Planet sphere (base) ───
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF122040),
                    Color(0xFF0A1628),
                    Color(0xFF060D1A)
                ),
                center = Offset(cx - planetRadius * 0.3f, cy - planetRadius * 0.3f),
                radius = planetRadius * 1.4f
            ),
            radius = planetRadius,
            center = Offset(cx, cy)
        )

        // ─── Continents (simplified, rotating) ───
        drawContinents(
            cx = cx,
            cy = cy,
            radius = planetRadius,
            rotation = currentRotation,
            isActive = isActive
        )

        // ─── Atmosphere rim ───
        drawCircle(
            color = atmosphereColor.copy(alpha = 0.15f),
            radius = planetRadius + 2f,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = atmosphereColor.copy(alpha = 0.08f),
            radius = planetRadius + 6f,
            center = Offset(cx, cy),
            style = Stroke(width = 8f)
        )

        // ─── Specular highlight (top-left) ───
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx - planetRadius * 0.4f, cy - planetRadius * 0.4f),
                radius = planetRadius * 0.8f
            ),
            radius = planetRadius,
            center = Offset(cx, cy)
        )

        // ─── Server location marker ───
        if (isActive && serverCoords != null) {
            drawServerMarker(
                cx = cx,
                cy = cy,
                planetRadius = planetRadius,
                coords = serverCoords,
                rotation = currentRotation,
                pulseScale = markerPulse,
                pulseAlpha = markerAlpha
            )
        }

        // ─── Grid lines (longitude) ───
        drawGridLines(cx, cy, planetRadius, currentRotation)
    }
}

private fun DrawScope.drawStars(size: Size) {
    // Deterministic star positions based on canvas size
    val starSeed = listOf(
        0.1f to 0.15f, 0.85f to 0.1f, 0.15f to 0.85f, 0.9f to 0.9f,
        0.5f to 0.05f, 0.05f to 0.5f, 0.95f to 0.5f, 0.5f to 0.95f,
        0.3f to 0.2f, 0.7f to 0.8f, 0.2f to 0.7f, 0.8f to 0.3f,
        0.4f to 0.12f, 0.6f to 0.88f, 0.12f to 0.4f, 0.88f to 0.6f
    )
    for ((fx, fy) in starSeed) {
        drawCircle(
            color = Color.White.copy(alpha = 0.3f + (fx * 0.4f)),
            radius = 1f + fx * 1.5f,
            center = Offset(size.width * fx, size.height * fy)
        )
    }
}

private fun DrawScope.drawContinents(
    cx: Float,
    cy: Float,
    radius: Float,
    rotation: Float,
    isActive: Boolean
) {
    val landColor = if (isActive) Color(0xFF1B4A2A) else Color(0xFF1B3A2A)
    val landHighlight = if (isActive) Color(0xFF2A6B3A) else Color(0xFF254D35)

    // Simplified continent shapes as arcs/ellipses positioned on the sphere
    // Each "continent" is defined by center offset angle and size
    val continents = listOf(
        // Europe (approx 10°E, 50°N)
        ContinentDef(10f, 50f, 0.18f, 0.12f),
        // Africa (approx 20°E, 0°)
        ContinentDef(20f, 0f, 0.15f, 0.25f),
        // Asia (approx 80°E, 40°N)
        ContinentDef(80f, 40f, 0.3f, 0.2f),
        // North America (approx -100°, 45°N)
        ContinentDef(-100f, 45f, 0.22f, 0.18f),
        // South America (approx -60°, -15°)
        ContinentDef(-60f, -15f, 0.12f, 0.22f),
        // Australia (approx 135°E, -25°)
        ContinentDef(135f, -25f, 0.1f, 0.08f)
    )

    for (cont in continents) {
        val adjustedLon = cont.lon + rotation
        val normalizedLon = ((adjustedLon % 360f) + 360f) % 360f

        // Only draw if on the visible hemisphere (0-180 in our projection)
        val lonRad = Math.toRadians(normalizedLon.toDouble())
        val latRad = Math.toRadians(cont.lat.toDouble())

        // Project to 2D (orthographic projection)
        val x3d = cos(latRad) * sin(lonRad)
        val y3d = -sin(latRad)
        val z3d = cos(latRad) * cos(lonRad)

        if (z3d < -0.1) continue // Behind the planet

        val screenX = cx + (x3d * radius).toFloat()
        val screenY = cy + (y3d * radius).toFloat()

        // Scale based on z-depth (further = smaller, edge distortion)
        val scale = maxOf(0.3f, z3d.toFloat())
        val w = radius * cont.widthFactor * scale
        val h = radius * cont.heightFactor * scale

        // Draw continent blob
        drawOval(
            color = landColor.copy(alpha = 0.7f * scale),
            topLeft = Offset(screenX - w / 2, screenY - h / 2),
            size = Size(w, h)
        )
        // Highlight edge
        drawOval(
            color = landHighlight.copy(alpha = 0.3f * scale),
            topLeft = Offset(screenX - w / 2 + w * 0.1f, screenY - h / 2 + h * 0.1f),
            size = Size(w * 0.6f, h * 0.6f)
        )
    }
}

private data class ContinentDef(
    val lon: Float,
    val lat: Float,
    val widthFactor: Float,
    val heightFactor: Float
)

private fun DrawScope.drawServerMarker(
    cx: Float,
    cy: Float,
    planetRadius: Float,
    coords: Pair<Double, Double>,
    rotation: Float,
    pulseScale: Float,
    pulseAlpha: Float
) {
    val adjustedLon = coords.second.toFloat() + rotation
    val normalizedLon = ((adjustedLon % 360f) + 360f) % 360f

    val lonRad = Math.toRadians(normalizedLon.toDouble())
    val latRad = Math.toRadians(coords.first.toDouble())

    val x3d = cos(latRad) * sin(lonRad)
    val y3d = -sin(latRad)
    val z3d = cos(latRad) * cos(lonRad)

    if (z3d < 0) return // Behind planet

    val screenX = cx + (x3d * planetRadius).toFloat()
    val screenY = cy + (y3d * planetRadius).toFloat()

    val markerRadius = 5f + z3d.toFloat() * 3f

    // Pulse ring
    drawCircle(
        color = AccentNeonGreen.copy(alpha = pulseAlpha * z3d.toFloat()),
        radius = markerRadius * pulseScale,
        center = Offset(screenX, screenY),
        style = Stroke(width = 2f)
    )

    // Second pulse ring (delayed)
    drawCircle(
        color = AccentNeonGreen.copy(alpha = pulseAlpha * 0.5f * z3d.toFloat()),
        radius = markerRadius * pulseScale * 1.5f,
        center = Offset(screenX, screenY),
        style = Stroke(width = 1.5f)
    )

    // Core dot
    drawCircle(
        color = AccentNeonGreen,
        radius = markerRadius,
        center = Offset(screenX, screenY)
    )

    // Inner bright dot
    drawCircle(
        color = AccentGreenBright,
        radius = markerRadius * 0.5f,
        center = Offset(screenX, screenY)
    )

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentNeonGreen.copy(alpha = 0.4f * z3d.toFloat()),
                Color.Transparent
            ),
            center = Offset(screenX, screenY),
            radius = markerRadius * 4f
        ),
        radius = markerRadius * 4f,
        center = Offset(screenX, screenY)
    )
}

private fun DrawScope.drawGridLines(
    cx: Float,
    cy: Float,
    radius: Float,
    rotation: Float
) {
    val gridColor = Color.White.copy(alpha = 0.03f)

    // Longitude lines
    for (lon in -180..180 step 30) {
        val adjustedLon = lon + rotation
        val normalizedLon = ((adjustedLon % 360f) + 360f) % 360f
        val lonRad = Math.toRadians(normalizedLon.toDouble())

        val x3d = sin(lonRad)
        val z3d = cos(lonRad)

        if (z3d < 0) continue

        val screenX = cx + (x3d * radius).toFloat()

        drawLine(
            color = gridColor,
            start = Offset(screenX, cy - radius * z3d.toFloat()),
            end = Offset(screenX, cy + radius * z3d.toFloat()),
            strokeWidth = 0.5f
        )
    }

    // Latitude lines (as ellipses)
    for (lat in -60..60 step 30) {
        if (lat == 0) continue
        val latRad = Math.toRadians(lat.toDouble())
        val y3d = -sin(latRad)
        val r = (cos(latRad) * radius).toFloat()

        drawOval(
            color = gridColor,
            topLeft = Offset(cx - r, cy + (y3d * radius).toFloat() - 1f),
            size = Size(r * 2, 2f),
            style = Stroke(width = 0.5f)
        )
    }
}
