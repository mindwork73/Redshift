package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.ConnectionState
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSize
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Primary action button (REDESIGN.md §7.9): 56dp tall, 16dp radius, accent gradient,
 * white bold label. The disabled state drops the gradient and goes quiet.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(RedRadius.Medium)
    val background = if (enabled) {
        Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(VpnColors.Accent, VpnColors.AccentDark)
            ),
            shape = shape
        )
    } else {
        Modifier.background(color = VpnColors.GlassFill, shape = shape)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RedSize.Button)
            .clip(shape)
            .then(background)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RedSpace.Xs)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) Color.White else VpnColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = RedType.Body.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) Color.White else VpnColors.TextTertiary
            )
        }
    }
}

/** Round glass icon button for screen headers (§7.3, §8.2). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = VpnColors.TextPrimary,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(VpnColors.GlassFillStrong)
            .border(width = 0.75.dp, color = VpnColors.GlassBorder, shape = CircleShape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else VpnColors.TextTertiary,
            modifier = Modifier.size(size / 2)
        )
    }
}

/**
 * The round connect button of the Home screen (REDESIGN.md §7.10, §8.1).
 *
 * 192dp circle with two pulse rings: the outer one scales 1 → 1.08 while fading 0.3 → 0.
 * Ring colour follows [ConnectionState]: neutral when off, warning while connecting,
 * success when connected.
 */
@Composable
fun PowerButton(
    state: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = when (state) {
        ConnectionState.CONNECTED -> VpnColors.Success
        ConnectionState.CONNECTING -> VpnColors.Warning
        ConnectionState.DISCONNECTED -> VpnColors.TextTertiary
    }
    val pulsing = state != ConnectionState.DISCONNECTED

    val transition = rememberInfiniteTransition(label = "power_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "power_pulse_scale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "power_pulse_alpha"
    )

    val diameter = RedSize.PowerButton
    val innerDiameter = diameter * 0.7f

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring (only animates while a tunnel is up or being built).
        Box(
            modifier = Modifier
                .size(diameter)
                .scale(if (pulsing) pulseScale else 1f)
                .border(
                    width = 1.dp,
                    color = ringColor.copy(alpha = if (pulsing) pulseAlpha else 0.16f),
                    shape = CircleShape
                )
        )

        // Second, static ring.
        Box(
            modifier = Modifier
                .size(diameter * 0.86f)
                .border(
                    width = 1.dp,
                    color = ringColor.copy(alpha = 0.32f),
                    shape = CircleShape
                )
        )

        // The button itself.
        GlassBox(
            modifier = Modifier.size(innerDiameter),
            shape = CircleShape,
            showGlint = false
        ) {
            if (state == ConnectionState.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = ringColor,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (state == ConnectionState.CONNECTED) {
                        VpnColors.Success
                    } else {
                        VpnColors.TextSecondary
                    },
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        // Transparent hit area on top so the ripple is not covered by the glass fill.
        Box(
            modifier = Modifier
                .size(innerDiameter)
                .clip(CircleShape)
                .clickable { onClick() }
        )
    }
}
