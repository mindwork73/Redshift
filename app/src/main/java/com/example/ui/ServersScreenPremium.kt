package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .background(BackgroundNavy)
    ) {
        // ─── Top Bar ───
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceInner).border(1.dp, BorderNavy, CircleShape).clickable { onBackClick() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBackIosNew, Trans.get("back"), tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
            Text(Trans.get("tab_servers"), style = VpnTypography.statusMain.copy(color = TextPrimary))
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceInner).border(1.dp, BorderNavy, CircleShape).clickable { onSettingsClick() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, Trans.get("tab_settings"), tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ─── Active Server Hero Card ───
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
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            item {
                Text(Trans.get("all_servers"), style = VpnTypography.headerSection, modifier = Modifier.padding(bottom = 16.dp))
            }

            if (servers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Public, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Text(Trans.get("empty_servers"), style = VpnTypography.cardSubtitle)
                        }
                    }
                }
            }

            items(servers) { server ->
                ServerListItem(
                    server = server,
                    isSelected = server.id == RedShiftState.selectedServerId,
                    onSelected = { onServerSelect(server) },
                    onConnect = { onServerSelect(server); onToggleConnection() }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
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
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlassBright.copy(alpha = 0.7f))
            .border(1.dp, if (isConnected) AccentRed.copy(alpha = glowAlpha) else BorderNavy, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceInner)
                .border(1.dp, if (isConnected) AccentRed.copy(alpha = 0.5f) else BorderNavy, CircleShape), contentAlignment = Alignment.Center
            ) { Text(server.flag, fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(server.name, style = VpnTypography.headerSection, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(server.address, style = VpnTypography.cardSubtitle)
                    Spacer(modifier = Modifier.width(8.dp))
                    ProtocolBadge(server.protocol)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ServersStatItem(Icons.Default.ArrowDownward, Trans.get("download"), if (isConnected) speedStr(downloadSpeed) else "0 KB/s", if (isConnected) StatusGreen else TextPrimary)
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderNavy))
            ServersStatItem(Icons.Default.SignalCellularAlt, Trans.get("ping"), "${server.latency}ms", pingQualityColor(server.latency))
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderNavy))
            ServersStatItem(Icons.Default.ArrowUpward, Trans.get("upload"), if (isConnected) speedStr(uploadSpeed) else "0 KB/s", if (isConnected) StatusGreen else TextPrimary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Connect/Disconnect button
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (isConnected) AccentRed.copy(alpha = 0.12f) else Brush.horizontalGradient(listOf(AccentRed, AccentBlue)))
                .border(1.dp, if (isConnected) AccentRed.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(100.dp))
                .clickable { onToggle() }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isConnected) AccentRed else Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowForward, null, tint = if (isConnected) Color.White else Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                if (isConnected) Trans.get("disconnect") else Trans.get("connect"),
                style = VpnTypography.buttonText.copy(color = if (isConnected) AccentRed else Color.White),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ServersStatItem(icon: ImageVector, label: String, value: String, iconTint: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = VpnTypography.statsLabel)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = VpnTypography.statsValue)
    }
}

@Composable
private fun ServerListItem(server: Server, isSelected: Boolean, onSelected: () -> Unit, onConnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) SurfaceGlassBright.copy(alpha = 0.6f) else SurfaceCard.copy(alpha = 0.7f))
            .border(1.dp, if (isSelected) AccentRed.copy(alpha = 0.4f) else BorderNavy.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onSelected() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SurfaceInner)
            .border(1.dp, if (isSelected) AccentRed.copy(alpha = 0.3f) else BorderNavy, CircleShape), contentAlignment = Alignment.Center
        ) { Text(server.flag, fontSize = 20.sp) }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.name, style = VpnTypography.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isSelected) { Spacer(Modifier.width(8.dp)); Box(Modifier.size(6.dp).clip(CircleShape).background(AccentRed)) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.protocol, style = VpnTypography.cardSubtitle)
                Spacer(Modifier.width(8.dp))
                Text("•", style = VpnTypography.cardSubtitle, color = TextMuted)
                Spacer(Modifier.width(8.dp))
                LatencyBadge(server.latency)
            }
        }

        Box(modifier = Modifier.clip(RoundedCornerShape(100.dp))
            .background(if (isSelected) AccentRed.copy(alpha = 0.15f) else SurfaceInner)
            .border(0.5.dp, if (isSelected) AccentRed.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(100.dp))
            .clickable { onConnect() }.padding(horizontal = 14.dp, vertical = 8.dp)
        ) { Text(Trans.get("connect"), style = VpnTypography.buttonText.copy(color = if (isSelected) AccentRed else TextPrimary)) }
    }
}
