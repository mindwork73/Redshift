package com.example.ui.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import com.example.ui.theme.VpnColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Size and root-space origin of the current scene (the [SceneBackdrop] box).
 * Glass cards use this to pixel-align a blurred copy of the photo behind them.
 */
data class SceneBounds(
    val size: IntSize = IntSize.Zero,
    val positionInRoot: Offset = Offset.Zero
)

/** Size + position of the scene, updated via [onGloballyPositioned] / [positionInRoot]. */
val LocalSceneBounds = compositionLocalOf { SceneBounds() }

/** True only on screens that paint [com.example.R.drawable.world] as a photo (Home). */
val LocalSceneHasImage = compositionLocalOf { false }

/**
 * Downscaled, box-blurred, ContentScale.Crop-matched copy of the scene photo.
 * Null on screens without a photo — glass must not blur over a gradient.
 */
internal val LocalBlurredScene = compositionLocalOf<ImageBitmap?> { null }

/** Dark scrim on top of the Home photo so type and glass stay readable. */
internal const val SceneImageOverlayAlpha = 0.48f

data class WorldPalette(
    val top: Color,
    val bottom: Color
)

/**
 * Scene wrapper used by [Screen].
 *
 * * [imageBackground] = true → `R.drawable.world` with [ContentScale.Crop] + a dark overlay.
 * * otherwise → a vertical gradient sampled from the same photo (top/bottom thirds, darkened).
 *
 * Palette and the blur bitmap are decoded once, off the main thread, and cached.
 */
@Composable
fun SceneBackdrop(
    imageBackground: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var bounds by remember { mutableStateOf(SceneBounds()) }
    var palette by remember { mutableStateOf(WorldSceneCache.palette) }
    var blurred by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        palette = withContext(Dispatchers.Default) {
            WorldSceneCache.ensurePalette(context.resources)
        }
    }

    LaunchedEffect(imageBackground, bounds.size) {
        if (!imageBackground || bounds.size.width <= 0 || bounds.size.height <= 0) {
            blurred = null
            return@LaunchedEffect
        }
        val sceneSize = bounds.size
        blurred = withContext(Dispatchers.Default) {
            WorldSceneCache.ensureBlur(context.resources, sceneSize)
        }
    }

    val top = palette?.top ?: VpnColors.BackgroundGradientTop
    val bottom = palette?.bottom ?: VpnColors.BackgroundGradientBottom

    CompositionLocalProvider(
        LocalSceneBounds provides bounds,
        LocalSceneHasImage provides imageBackground,
        LocalBlurredScene provides if (imageBackground) blurred else null
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    bounds = SceneBounds(
                        size = coords.size,
                        positionInRoot = coords.positionInRoot()
                    )
                }
        ) {
            if (imageBackground) {
                Image(
                    painter = painterResource(com.example.R.drawable.world),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = SceneImageOverlayAlpha))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(top, bottom)))
                )
            }
            content()
        }
    }
}

/**
 * Off-main cache for the world photo derivatives. Everything here must be called from
 * [Dispatchers.Default] (or any worker thread) — Bitmap decode/blur is not cheap.
 */
internal object WorldSceneCache {
    private val lock = Any()

    @Volatile
    var palette: WorldPalette? = null
        private set

    @Volatile
    private var source: Bitmap? = null

    @Volatile
    private var blurKey: Int = 0

    @Volatile
    private var blurred: ImageBitmap? = null

    fun ensurePalette(resources: Resources): WorldPalette {
        synchronized(lock) {
            palette?.let { return it }
            val bmp = sourceBitmap(resources)
            val sampled = WorldPalette(
                top = darken(averageThird(bmp, topThird = true)),
                bottom = darken(averageThird(bmp, topThird = false))
            )
            palette = sampled
            return sampled
        }
    }

    fun ensureBlur(resources: Resources, sceneSize: IntSize): ImageBitmap {
        val key = sceneSize.width * 31 + sceneSize.height
        synchronized(lock) {
            val existing = blurred
            if (existing != null && blurKey == key) return existing
            val bmp = sourceBitmap(resources)
            val cropped = cropToFill(bmp, sceneSize.width, sceneSize.height)
            val small = downscale(cropped, maxSide = 96)
            if (cropped !== bmp && cropped !== small) cropped.recycle()
            val frosted = boxBlur(small, radius = 3)
            if (small !== frosted && small !== bmp) small.recycle()
            darkenInPlace(frosted, SceneImageOverlayAlpha)
            val image = frosted.asImageBitmap()
            blurred = image
            blurKey = key
            return image
        }
    }

    private fun sourceBitmap(resources: Resources): Bitmap {
        source?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, com.example.R.drawable.world, bounds)
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        while (longest / sample > 1024) sample *= 2
        val decoded = BitmapFactory.decodeResource(
            resources,
            com.example.R.drawable.world,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        source = decoded
        return decoded
    }
}

private fun averageThird(bmp: Bitmap, topThird: Boolean): Color {
    val w = bmp.width
    val h = bmp.height
    if (w <= 0 || h <= 0) return Color.Black
    val y0 = if (topThird) 0 else (h * 2) / 3
    val y1 = if (topThird) h / 3 else h
    val regionH = (y1 - y0).coerceAtLeast(1)
    val pixels = IntArray(w * regionH)
    bmp.getPixels(pixels, 0, w, 0, y0, w, regionH)
    var r = 0L
    var g = 0L
    var b = 0L
    var n = 0
    var i = 0
    while (i < pixels.size) {
        val c = pixels[i]
        r += (c ushr 16) and 0xFF
        g += (c ushr 8) and 0xFF
        b += c and 0xFF
        n++
        i += 4
    }
    if (n == 0) return Color.Black
    return Color(
        red = (r / n).toInt() / 255f,
        green = (g / n).toInt() / 255f,
        blue = (b / n).toInt() / 255f,
        alpha = 1f
    )
}

private fun darken(color: Color, amount: Float = 0.52f): Color {
    val keep = 1f - amount
    return Color(
        red = color.red * keep,
        green = color.green * keep,
        blue = color.blue * keep,
        alpha = 1f
    )
}

/** Multiply RGB by (1 - overlayAlpha) so the blur matches the photo + scrim. */
private fun darkenInPlace(bmp: Bitmap, overlayAlpha: Float) {
    val keep = (255f * (1f - overlayAlpha)).toInt().coerceIn(0, 255)
    val w = bmp.width
    val h = bmp.height
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        val c = pixels[i]
        val a = (c ushr 24) and 0xFF
        val r = (((c ushr 16) and 0xFF) * keep) / 255
        val g = (((c ushr 8) and 0xFF) * keep) / 255
        val b = ((c and 0xFF) * keep) / 255
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    bmp.setPixels(pixels, 0, w, 0, 0, w, h)
}

/** Same crop math as [ContentScale.Crop]: fill [dstW]×[dstH], discard overflow, keep centre. */
private fun cropToFill(src: Bitmap, dstW: Int, dstH: Int): Bitmap {
    if (dstW <= 0 || dstH <= 0) return src
    val srcW = src.width
    val srcH = src.height
    if (srcW <= 0 || srcH <= 0) return src
    val srcAspect = srcW.toFloat() / srcH.toFloat()
    val dstAspect = dstW.toFloat() / dstH.toFloat()
    val cropW: Int
    val cropH: Int
    val left: Int
    val top: Int
    if (srcAspect > dstAspect) {
        cropH = srcH
        cropW = (srcH * dstAspect).toInt().coerceIn(1, srcW)
        left = (srcW - cropW) / 2
        top = 0
    } else {
        cropW = srcW
        cropH = (srcW / dstAspect).toInt().coerceIn(1, srcH)
        left = 0
        top = (srcH - cropH) / 2
    }
    if (cropW == srcW && cropH == srcH) return src
    return Bitmap.createBitmap(src, left, top, cropW, cropH)
}

private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
    val longest = maxOf(src.width, src.height)
    if (longest <= maxSide) return src
    val scale = maxSide.toFloat() / longest.toFloat()
    val w = (src.width * scale).toInt().coerceAtLeast(1)
    val h = (src.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, w, h, true)
}

/** Separable box blur, two passes ≈ a small gaussian. Operates in-place on a copy. */
private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
    val w = src.width
    val h = src.height
    val a = IntArray(w * h)
    src.getPixels(a, 0, w, 0, 0, w, h)
    val b = IntArray(w * h)
    blurHorizontal(a, b, w, h, radius)
    blurVertical(b, a, w, h, radius)
    blurHorizontal(a, b, w, h, radius)
    blurVertical(b, a, w, h, radius)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(a, 0, w, 0, 0, w, h)
    return out
}

private fun blurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
    val div = radius * 2 + 1
    for (y in 0 until h) {
        val row = y * w
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -radius..radius) {
            val c = src[row + i.coerceIn(0, w - 1)]
            sumA += (c ushr 24) and 0xFF
            sumR += (c ushr 16) and 0xFF
            sumG += (c ushr 8) and 0xFF
            sumB += c and 0xFF
        }
        for (x in 0 until w) {
            dst[row + x] =
                ((sumA / div) shl 24) or
                    ((sumR / div) shl 16) or
                    ((sumG / div) shl 8) or
                    (sumB / div)
            val drop = src[row + (x - radius).coerceIn(0, w - 1)]
            val add = src[row + (x + radius + 1).coerceIn(0, w - 1)]
            sumA += ((add ushr 24) and 0xFF) - ((drop ushr 24) and 0xFF)
            sumR += ((add ushr 16) and 0xFF) - ((drop ushr 16) and 0xFF)
            sumG += ((add ushr 8) and 0xFF) - ((drop ushr 8) and 0xFF)
            sumB += (add and 0xFF) - (drop and 0xFF)
        }
    }
}

private fun blurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
    val div = radius * 2 + 1
    for (x in 0 until w) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -radius..radius) {
            val c = src[i.coerceIn(0, h - 1) * w + x]
            sumA += (c ushr 24) and 0xFF
            sumR += (c ushr 16) and 0xFF
            sumG += (c ushr 8) and 0xFF
            sumB += c and 0xFF
        }
        for (y in 0 until h) {
            dst[y * w + x] =
                ((sumA / div) shl 24) or
                    ((sumR / div) shl 16) or
                    ((sumG / div) shl 8) or
                    (sumB / div)
            val drop = src[(y - radius).coerceIn(0, h - 1) * w + x]
            val add = src[(y + radius + 1).coerceIn(0, h - 1) * w + x]
            sumA += ((add ushr 24) and 0xFF) - ((drop ushr 24) and 0xFF)
            sumR += ((add ushr 16) and 0xFF) - ((drop ushr 16) and 0xFF)
            sumG += ((add ushr 8) and 0xFF) - ((drop ushr 8) and 0xFF)
            sumB += (add and 0xFF) - (drop and 0xFF)
        }
    }
}
