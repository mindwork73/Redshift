package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSpace
import com.example.ui.theme.VpnColors
import kotlin.math.roundToInt

/**
 * The base "glass" primitive (REDESIGN.md §7.1).
 *
 * On screens with a photo ([LocalSceneHasImage]) a downscaled box-blur of the scene is drawn
 * underneath the translucent fill, pixel-aligned to the scene and clipped to [shape].
 * On gradient-only screens the blur is skipped.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RedRadius.Large),
    showGlint: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(RedSpace.M),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.then(rememberGlassChrome(shape))) {
        GlassGlint(visible = showGlint)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * Glass card used as a static container — same surface, content laid out by the caller.
 * Kept separate from [GlassSurface] for places that need a [BoxScope] (badges, overlays).
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RedRadius.Large),
    showGlint: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.then(rememberGlassChrome(shape))) {
        GlassGlint(visible = showGlint)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = contentAlignment,
            content = content
        )
    }
}

/** Small round glass chip holding an icon — list rows, headers, onboarding (§7.5, §8.0). */
@Composable
fun GlassIconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = VpnColors.TextPrimary,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(VpnColors.GlassFillStrong)
            .border(width = 0.75.dp, color = VpnColors.GlassBorder, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Shared chrome for [GlassSurface] / [GlassBox]: shadow, clip-to-shape, optional scene blur,
 * translucent fill, hairline border.
 */
@Composable
private fun rememberGlassChrome(shape: Shape): Modifier {
    val hasImage = LocalSceneHasImage.current
    val scene = LocalSceneBounds.current
    val blurred = LocalBlurredScene.current
    var origin by remember { mutableStateOf(Offset.Zero) }

    val blurLayer = if (hasImage && blurred != null && scene.size.width > 0) {
        Modifier.drawBehind {
            val dx = (scene.positionInRoot.x - origin.x).roundToInt()
            val dy = (scene.positionInRoot.y - origin.y).roundToInt()
            drawImage(
                image = blurred,
                dstOffset = IntOffset(dx, dy),
                dstSize = scene.size,
                filterQuality = FilterQuality.Low
            )
        }
    } else {
        Modifier
    }

    return Modifier
        .onGloballyPositioned { coords -> origin = coords.positionInRoot() }
        .shadow(
            elevation = 12.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.4f),
            spotColor = Color.Black.copy(alpha = 0.4f)
        )
        .clip(shape)
        .then(blurLayer)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.09f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
        .border(
            width = 0.75.dp,
            brush = Brush.verticalGradient(
                colors = listOf(VpnColors.GlassBorderTop, VpnColors.GlassBorder)
            ),
            shape = shape
        )
}

@Composable
private fun BoxScope.GlassGlint(visible: Boolean) {
    if (!visible) return
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth(0.62f)
            .height(0.6.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        VpnColors.GlassGlint,
                        VpnColors.GlassGlint,
                        Color.Transparent
                    )
                )
            )
    )
}
