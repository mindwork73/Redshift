package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionState
import com.example.ui.DASH
import com.example.ui.RedShiftState
import com.example.ui.components.GlassSurface
import com.example.ui.components.PlanPill
import com.example.ui.components.PowerButton
import com.example.ui.components.Screen
import com.example.ui.components.StatCell
import com.example.ui.formatDuration
import com.example.ui.formatPing
import com.example.ui.formatSpeedLabel
import com.example.ui.formatTraffic
import com.example.ui.isPremiumPlan
import com.example.ui.theme.pingColor
import com.example.ui.t
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Home tab (REDESIGN.md §8.1): timer, status, connect button, the currently selected server
 * and live metrics. Everything shown comes from [RedShiftState] — no placeholder numbers.
 *
 * Fixed to one screen ([scrollable] = false) with weighted spacers; the scene photo is
 * `R.drawable.world` via [com.example.ui.components.Screen] `imageBackground`.
 */
@Composable
fun HomeScreen(onOpenServers: () -> Unit) {
    val connectionState = RedShiftState.connectionState
    val server = RedShiftState.getSelectedServer()
    val plan = RedShiftState.subscriptionPlan

    Screen(scrollable = false, imageBackground = true) {
        // ── Header: app name + real tariff pill ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = t("app_title"),
                style = RedType.Title,
                color = VpnColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isPremiumPlan(plan)) {
                PlanPill(plan = plan)
            }
        }

        Spacer(Modifier.weight(0.45f))

        // ── Timer + status ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatDuration(RedShiftState.sessionDurationSeconds),
                style = RedType.Timer,
                color = VpnColors.TextPrimary
            )

            Spacer(Modifier.height(RedSpace.Xs))

            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> t("connected")
                    ConnectionState.CONNECTING -> t("connecting")
                    ConnectionState.DISCONNECTED -> t("disconnected")
                },
                style = RedType.Title,
                color = when (connectionState) {
                    ConnectionState.CONNECTED -> VpnColors.Success
                    ConnectionState.CONNECTING -> VpnColors.Warning
                    ConnectionState.DISCONNECTED -> VpnColors.TextSecondary
                }
            )

            Spacer(Modifier.height(RedSpace.Xxs))

            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> t("status_secured")
                    ConnectionState.CONNECTING -> t("home_vpn_starting")
                    ConnectionState.DISCONNECTED -> t("home_not_protected")
                },
                style = RedType.Caption,
                color = VpnColors.TextSecondary
            )
        }

        Spacer(Modifier.weight(0.55f))

        // ── Connect button ──
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PowerButton(
                state = connectionState,
                onClick = { RedShiftState.toggleVpn() }
            )
        }

        Spacer(Modifier.weight(0.50f))

        // ── Current server (tap → Servers tab) ──
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenServers() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VpnColors.GlassFillStrong),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = server?.flag?.takeIf { it.isNotBlank() } ?: DASH,
                        fontSize = 20.sp
                    )
                }

                Spacer(Modifier.width(RedSpace.S))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server?.name ?: t("no_server_selected"),
                        style = RedType.Body,
                        color = VpnColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(RedSpace.S))
            }
        }

        Spacer(Modifier.height(RedSpace.M))

        // ── Live metrics ──
        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCell(
                    label = t("download"),
                    value = formatSpeedLabel(RedShiftState.downloadSpeed),
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = t("upload"),
                    value = formatSpeedLabel(RedShiftState.uploadSpeed),
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = t("ping"),
                    value = formatPing(server?.latency ?: 0),
                    valueColor = pingColor(server?.latency ?: 0),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(RedSpace.M))

        // ── Protection status ──
        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (connectionState == ConnectionState.CONNECTED) {
                        Icons.Filled.VerifiedUser
                    } else {
                        Icons.Filled.Shield
                    },
                    contentDescription = null,
                    tint = if (connectionState == ConnectionState.CONNECTED) {
                        VpnColors.Success
                    } else {
                        VpnColors.TextTertiary
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(RedSpace.S))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTED) {
                            t("home_status_protected")
                        } else {
                            t("status_not_protected")
                        },
                        style = RedType.BodyMedium,
                        color = VpnColors.TextPrimary
                    )
                    Text(
                        text = buildString {
                            append(
                                when (connectionState) {
                                    ConnectionState.CONNECTED -> t("home_vpn_active")
                                    ConnectionState.CONNECTING -> t("home_vpn_starting")
                                    ConnectionState.DISCONNECTED -> t("home_vpn_idle")
                                }
                            )
                            if (connectionState == ConnectionState.CONNECTED) {
                                append(" · ")
                                append(t("session_traffic"))
                                append(": ")
                                append(formatTraffic(RedShiftState.totalDataUsedMb))
                            }
                        },
                        style = RedType.Caption,
                        color = VpnColors.TextTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.weight(0.30f))
    }
}
