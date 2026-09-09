package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ServersScreenPremium(
    servers: List<Server>,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onServerSelect: (Server) -> Unit,
    onToggleConnection: () -> Unit
) {
    val activeServer = RedShiftState.getSelectedServer()
    val isConnected = RedShiftState.connectionState == ConnectionState.CONNECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGraphite)
    ) {
        ServersTopBar(onBackClick, onSettingsClick)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (activeServer != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ActiveServerHeroCard(
                        server = activeServer,
                        isConnected = isConnected,
                        downloadSpeed = RedShiftState.downloadSpeed,
                        uploadSpeed = RedShiftState.uploadSpeed,
                        onToggle = onToggleConnection
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                Text(
                    text = Trans.get("all_servers"),
                    style = VpnTypography.headerSection,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (servers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = VpnColors.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = Trans.get("empty_servers"),
                                style = VpnTypography.cardSubtitle
                            )
                        }
                    }
                }
            }

            items(servers) { server ->
                ServerListItem(
                    server = server,
                    isSelected = server.id == RedShiftState.selectedServerId,
                    onSelected = { onServerSelect(server) },
                    onConnect = {
                        onServerSelect(server)
                        onToggleConnection()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ServersTopBar(onBackClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceInner)
                .border(1.dp, BorderGraphite, CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = Trans.get("back"),
                tint = VpnColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = Trans.get("tab_servers"),
            style = VpnTypography.statusMain.copy(color = VpnColors.TextPrimary)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceInner)
                .border(1.dp, BorderGraphite, CircleShape)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = Trans.get("tab_settings"),
                tint = VpnColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActiveServerHeroCard(
    server: Server,
    isConnected: Boolean,
    downloadSpeed: Double,
    uploadSpeed: Double,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass.copy(alpha = 0.85f))
            .border(
                1.dp,
                if (isConnected) AccentNeonGreen.copy(alpha = glowAlpha) else BorderGraphite,
                RoundedCornerShape(24.dp)
            )
            .drawBehind {
                if (isConnected) {
                    drawRoundRect(
                        color = AccentNeonGreen.copy(alpha = 0.03f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                    )
                }
            }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceInner)
                    .border(
                        1.dp,
                        if (isConnected) AccentNeonGreen.copy(alpha = 0.5f) else BorderGraphite,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = server.flag, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = server.name,
                    style = VpnTypography.headerSection,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = VpnColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = server.address,
                        style = VpnTypography.cardSubtitle
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProtocolBadge(protocol = server.protocol)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ServersStatItem(
                icon = Icons.Default.ArrowDownward,
                label = Trans.get("download"),
                value = if (isConnected) speedStr(downloadSpeed) else "0 KB/s",
                iconTint = if (isConnected) AccentNeonGreen else VpnColors.TextPrimary
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(BorderGraphite)
            )
            ServersStatItem(
                icon = Icons.Default.SignalCellularAlt,
                label = Trans.get("ping"),
                value = "${server.latency}ms",
                iconTint = pingQualityColor(server.latency)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(BorderGraphite)
            )
            ServersStatItem(
                icon = Icons.Default.ArrowUpward,
                label = Trans.get("upload"),
                value = if (isConnected) speedStr(uploadSpeed) else "0 KB/s",
                iconTint = if (isConnected) AccentNeonGreen else VpnColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(
                    if (isConnected) AccentNeonGreen.copy(alpha = 0.12f)
                    else Brush.horizontalGradient(VpnColors.premiumGradient)
                )
                .border(
                    1.dp,
                    if (isConnected) AccentNeonGreen.copy(alpha = 0.4f) else Color.Transparent,
                    RoundedCornerShape(100.dp)
                )
                .clickable { onToggle() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) AccentNeonGreen
                        else Brush.horizontalGradient(VpnColors.premiumGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = if (isConnected) BackgroundGraphite else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (isConnected) Trans.get("disconnect") else Trans.get("connect"),
                style = VpnTypography.buttonText.copy(
                    color = if (isConnected) AccentNeonGreen else Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = ">>>",
                style = VpnTypography.buttonText.copy(
                    color = VpnColors.textTertiary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

@Composable
private fun ServersStatItem(icon: ImageVector, label: String, value: String, iconTint: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = VpnTypography.statsLabel)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = VpnTypography.statsValue)
    }
}

@Composable
private fun ServerListItem(
    server: Server,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) SurfaceGlass.copy(alpha = 0.9f)
                else SurfaceInner.copy(alpha = 0.6f)
            )
            .border(
                1.dp,
                if (isSelected) AccentNeonGreen.copy(alpha = 0.4f) else BorderGraphite.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onSelected() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceInner)
                .border(
                    1.dp,
                    if (isSelected) AccentNeonGreen.copy(alpha = 0.3f) else BorderGraphite,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = server.flag, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = server.name,
                    style = VpnTypography.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentNeonGreen)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = server.protocol,
                    style = VpnTypography.cardSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    style = VpnTypography.cardSubtitle,
                    color = VpnColors.TextMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                LatencyBadge(ping = server.latency)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(
                    if (isSelected) AccentNeonGreen.copy(alpha = 0.15f)
                    else SurfaceCardSolid
                )
                .border(
                    0.5.dp,
                    if (isSelected) AccentNeonGreen.copy(alpha = 0.4f) else Color.Transparent,
                    RoundedCornerShape(100.dp)
                )
                .clickable { onConnect() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = Trans.get("connect"),
                style = VpnTypography.buttonText.copy(
                    color = if (isSelected) AccentNeonGreen else VpnColors.TextPrimary
                )
            )
        }
    }
}
