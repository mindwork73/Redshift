package com.example.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnTypography

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
            .background(VpnColors.Background)
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
                        Text(
                            text = Trans.get("empty_servers"),
                            style = VpnTypography.cardSubtitle
                        )
                    }
                }
            }

            items(servers) { server ->
                ServerListItem(
                    server = server,
                    onSelected = { onServerSelect(server) },
                    onConnect = {
                        onServerSelect(server)
                        onToggleConnection()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
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
        Icon(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = Trans.get("back"),
            tint = VpnColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBackClick() }
        )
        Text(
            text = Trans.get("tab_servers"),
            style = VpnTypography.statusMain.copy(color = VpnColors.TextPrimary)
        )
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = Trans.get("tab_settings"),
            tint = VpnColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onSettingsClick() }
        )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(VpnColors.surfaceGlass)
            .border(1.dp, VpnColors.borderLight, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VpnColors.surfaceInner),
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
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                icon = Icons.Default.ArrowDownward,
                label = Trans.get("download"),
                value = if (isConnected) speedStr(downloadSpeed) else "0 KB/s",
                iconTint = VpnColors.TextPrimary
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(VpnColors.borderLight)
            )
            StatItem(
                icon = Icons.Default.SignalCellularAlt,
                label = Trans.get("ping"),
                value = "${server.latency}ms",
                iconTint = VpnColors.accentGreen
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(VpnColors.borderLight)
            )
            StatItem(
                icon = Icons.Default.ArrowUpward,
                label = Trans.get("upload"),
                value = if (isConnected) speedStr(uploadSpeed) else "0 KB/s",
                iconTint = VpnColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(VpnColors.surfaceInner)
                .clickable { onToggle() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(VpnColors.premiumGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = VpnColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (isConnected) Trans.get("disconnect") else Trans.get("connect"),
                style = VpnTypography.buttonText,
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
private fun StatItem(icon: ImageVector, label: String, value: String, iconTint: Color) {
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
    onSelected: () -> Unit,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onSelected() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(VpnColors.surfaceInner)
                .border(1.dp, VpnColors.borderLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = server.flag, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = VpnTypography.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${Trans.get("protocol")}: ${server.protocol}",
                style = VpnTypography.cardSubtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(VpnColors.surfaceCardSolid)
                .clickable { onConnect() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = Trans.get("connect"),
                style = VpnTypography.buttonText
            )
        }
    }
}