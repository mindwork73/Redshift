package com.example.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.ui.ConnectionState
import com.example.ui.Server
import org.json.JSONArray

private data class MapPlace(val longitude: Float, val latitude: Float)
private val russia = MapPlace(37.62f, 55.75f)

/** Coordinates describe a server's region, not the device's physical location. */
private fun placeFor(server: Server?): MapPlace? {
    if (server == null) return null
    val hint = "${server.name} ${server.address} ${server.subExtra}".lowercase()
    return when {
        Regex("(^|[^a-z])(nl|netherlands|amsterdam)([^a-z]|$)").containsMatchIn(hint) || hint.contains("нидерланд") || hint.contains("37.220.84.106") -> MapPlace(4.90f, 52.37f)
        Regex("(^|[^a-z])(eu1?|moldova|chisinau)([^a-z]|$)").containsMatchIn(hint) || hint.contains("молдов") || hint.contains("217.156.64.") -> MapPlace(28.83f, 47.01f)
        Regex("(^|[^a-z])(uk|london)([^a-z]|$)").containsMatchIn(hint) -> MapPlace(-0.13f, 51.51f)
        Regex("(^|[^a-z])(us|usa|new york)([^a-z]|$)").containsMatchIn(hint) -> MapPlace(-74.0f, 40.7f)
        Regex("(^|[^a-z])(sg|singapore)([^a-z]|$)").containsMatchIn(hint) -> MapPlace(103.82f, 1.35f)
        else -> null
    }
}

/** Natural Earth 1:110m country outlines; user can drag the map. */
@Composable
fun WorldMapBackground(state: ConnectionState, server: Server?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val paths = remember {
        val data = JSONArray(context.assets.open("world_land.json").bufferedReader().use { it.readText() })
        List(data.length()) { i ->
            val ring = data.getJSONArray(i)
            Path().apply {
                for (j in 0 until ring.length()) {
                    val point = ring.getJSONArray(j)
                    val x = (point.getDouble(0).toFloat() + 180f) / 360f
                    val y = (90f - point.getDouble(1).toFloat()) / 180f
                    if (j == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
    }
    val place = if (state == ConnectionState.CONNECTED) placeFor(server) else russia
    val focus = place ?: russia
    val lon by animateFloatAsState(focus.longitude, tween(950, easing = FastOutSlowInEasing), label = "mapLongitude")
    val lat by animateFloatAsState(focus.latitude, tween(950, easing = FastOutSlowInEasing), label = "mapLatitude")
    var drag by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(focus) { drag = Offset.Zero }
    Canvas(modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures { change, delta -> change.consume(); drag += delta }
    }) {
        val worldWidth = size.width * 2.8f
        val worldHeight = worldWidth / 2f
        val fx = (lon + 180f) / 360f
        val fy = (90f - lat) / 180f
        val originX = size.width * .5f - fx * worldWidth + drag.x
        val originY = size.height * .44f - fy * worldHeight + drag.y
        withTransform({ translate(originX, originY); scale(worldWidth, worldHeight, Offset.Zero) }) {
            paths.forEach {
                drawPath(it, Color(0xFF272E30))
                drawPath(it, Color(0xFF4A5554), style = Stroke(width = .00045f))
            }
        }
        for (i in 0..12) {
            val x = originX + worldWidth * i / 12f
            drawLine(Color.White.copy(alpha = .035f), Offset(x, originY), Offset(x, originY + worldHeight), 1f)
        }
        if (place != null) {
            val x = originX + (place.longitude + 180f) / 360f * worldWidth
            val y = originY + (90f - place.latitude) / 180f * worldHeight
            val center = Offset(x, y)
            val green = Color(0xFF60FFAE)
            drawCircle(green.copy(alpha = .06f), 44f, center)
            drawCircle(green.copy(alpha = .12f), 26f, center)
            drawCircle(green.copy(alpha = .45f), 13f, center, style = Stroke(2f))
            drawCircle(green, 5f, center)
        }
    }
}
