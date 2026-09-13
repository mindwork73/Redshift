package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.ConnectionState
import com.example.ui.DASH
import com.example.ui.formatPing
import com.example.ui.theme.pingColor
import com.example.ui.t
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Rounded badge (REDESIGN.md §7.8): soft coloured background, [RedType.CaptionMedium] label.
 * Used for protocol, latency and status chips.
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = VpnColors.GlassFillStrong,
    contentColor: Color = VpnColors.TextSecondary,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(RedRadius.Pill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(width = 0.5.dp, color = VpnColors.GlassBorder, shape = shape)
            .padding(horizontal = RedSpace.Xs, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            style = RedType.CaptionMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Protocol chip — the protocol string comes straight from `Server.protocol` (§8.2). */
@Composable
fun ProtocolPill(
    protocol: String,
    modifier: Modifier = Modifier
) {
    if (protocol.isBlank()) return
    Pill(
        text = protocol.uppercase(),
        modifier = modifier,
        containerColor = VpnColors.GlassFillStrong,
        contentColor = VpnColors.TextSecondary
    )
}

/** Latency chip. 0 means "not measured" → dash with a neutral colour, never a fake number (§4). */
@Composable
fun PingPill(
    latencyMs: Int,
    modifier: Modifier = Modifier
) {
    val color = pingColor(latencyMs)
    Pill(
        text = formatPing(latencyMs),
        modifier = modifier,
        containerColor = if (latencyMs <= 0) VpnColors.GlassFill else color.copy(alpha = 0.14f),
        contentColor = color
    )
}

/** Connection status chip driven by [ConnectionState] (§5.2, §8.1). */
@Composable
fun StatusPill(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val text = when (state) {
        ConnectionState.CONNECTED -> t("connected")
        ConnectionState.CONNECTING -> t("connecting")
        ConnectionState.DISCONNECTED -> t("disconnected")
    }
    val color = when (state) {
        ConnectionState.CONNECTED -> VpnColors.Success
        ConnectionState.CONNECTING -> VpnColors.Warning
        ConnectionState.DISCONNECTED -> VpnColors.TextSecondary
    }
    Pill(
        text = text,
        modifier = modifier,
        containerColor = color.copy(alpha = 0.14f),
        contentColor = color
    )
}

/**
 * Tariff chip for the Home header (§8.1). Rendered only for a real premium tariff —
 * there is no "premium by guesswork" heuristic (§4).
 */
@Composable
fun PlanPill(
    plan: String,
    modifier: Modifier = Modifier
) {
    if (plan.isBlank()) return
    Pill(
        text = plan,
        modifier = modifier,
        containerColor = VpnColors.AccentSoft,
        contentColor = VpnColors.Accent
    )
}

/** Neutral placeholder chip for "no value yet". */
@Composable
fun EmptyPill(
    modifier: Modifier = Modifier
) {
    Pill(
        text = DASH,
        modifier = modifier,
        containerColor = VpnColors.GlassFill,
        contentColor = VpnColors.TextTertiary
    )
}
