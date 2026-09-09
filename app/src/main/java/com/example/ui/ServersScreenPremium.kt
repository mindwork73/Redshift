package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BackgroundNavy
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceInner
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
    val connectionState = RedShiftState.connectionState
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING

    val availableServers = servers.filter { it.id != activeServer?.id }
    val freeServers = availableServers.take(2)
    val premiumServers = availableServers.drop(2)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        BackgroundNavy,
                        BackgroundNavy,
                        Color(0xFF102238)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ServersTopBar(
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
            ) {
                activeServer?.let { server ->
                    item {
                        ActiveServerCard(
                            server = server,
                            connectionState = connectionState,
                            onToggle = onToggleConnection
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (freeServers.isNotEmpty()) {
                    item { SectionHeader(title = "Free server") }
                    itemsIndexed(freeServers, key = { _, item -> item.id }) { index, server ->
                        ServerListItem(
                            server = server,
                            isPremium = false,
                            isSelected = server.id == RedShiftState.selectedServerId,
                            showDivider = index != freeServers.lastIndex,
                            subtitle = "${sampleServerCount(server.name, false)} servers",
                            onSelected = { onServerSelect(server) },
                            onConnect = {
                                onServerSelect(server)
                                if (!isConnected && !isConnecting) onToggleConnection()
                            }
                        )
                    }
                }

                if (premiumServers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "Premium server")
                    }
                    itemsIndexed(premiumServers, key = { _, item -> item.id }) { index, server ->
                        ServerListItem(
                            server = server,
                            isPremium = true,
                            isSelected = server.id == RedShiftState.selectedServerId,
                            showDivider = index != premiumServers.lastIndex,
                            subtitle = "${sampleServerCount(server.name, true)} servers",
                            onSelected = { onServerSelect(server) },
                            onConnect = {
                                onServerSelect(server)
                                if (!isConnected && !isConnecting) onToggleConnection()
                            }
                        )
                    }
                }

                if (activeServer == null && servers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Trans.get("empty_servers"),
                                style = VpnTypography.bodyRegular,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServersTopBar(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = Trans.get("back")
        )
        Text(
            text = "Server",
            style = VpnTypography.titleLarge,
            color = TextPrimary
        )
        HeaderIconButton(
            onClick = onSettingsClick,
            icon = Icons.Default.Settings,
            contentDescription = Trans.get("tab_settings")
        )
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ActiveServerCard(
    server: Server,
    connectionState: ConnectionState,
    onToggle: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val buttonText = when (connectionState) {
        ConnectionState.CONNECTED -> Trans.get("disconnect")
        ConnectionState.CONNECTING -> Trans.get("connecting")
        ConnectionState.DISCONNECTED -> Trans.get("connect")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.34f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E2530),
                        Color(0xFF232C38),
                        Color(0xFF273240)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagCircle(flag = server.flag, size = 48.dp)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = server.name,
                    style = VpnTypography.countryTitle,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = server.address,
                        style = VpnTypography.bodyRegular,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ArrowDownward,
                label = Trans.get("download"),
                value = if (isConnected) speedLabel(RedShiftState.downloadSpeed, "20mbps") else "20mbps",
                iconTint = TextSecondary
            )
            VerticalMetricDivider()
            MetricColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.SignalCellularAlt,
                label = Trans.get("ping"),
                value = if (server.latency > 0) "${server.latency}ms" else "164ms",
                iconTint = StatusGreen
            )
            VerticalMetricDivider()
            MetricColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ArrowUpward,
                label = Trans.get("upload"),
                value = if (isConnected) speedLabel(RedShiftState.uploadSpeed, "16mbps") else "16mbps",
                iconTint = TextSecondary
            )
        }

        ConnectSliderButton(
            text = buttonText,
            onClick = onToggle
        )
    }
}

private fun speedLabel(valueKbps: Double, fallback: String): String {
    if (valueKbps <= 0.0) return fallback
    return if (valueKbps >= 1024.0) {
        String.format("%.1fmbps", valueKbps / 1024.0)
    } else {
        String.format("%.0fkbps", valueKbps)
    }
}

@Composable
private fun MetricColumn(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = VpnTypography.statsLabel,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Text(
            text = value,
            style = VpnTypography.statsValue,
            color = TextPrimary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun VerticalMetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(BorderNavy.copy(alpha = 0.8f))
    )
}

@Composable
private fun ConnectSliderButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF353D49))
            .border(1.dp, BorderNavy, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(AccentBlue, AccentRed))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = VpnTypography.statusMain,
                color = TextPrimary
            )
            ChevronTrail(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                color = TextMuted
            )
        }
    }
}

@Composable
private fun ChevronTrail(
    modifier: Modifier = Modifier,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "chevrons")
    val alphaA by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaA"
    )
    val alphaB by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaB"
    )
    val alphaC by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaC"
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = ">",
            style = VpnTypography.buttonText.copy(fontWeight = FontWeight.Bold),
            color = color.copy(alpha = alphaA)
        )
        Text(
            text = ">",
            style = VpnTypography.buttonText.copy(fontWeight = FontWeight.Bold),
            color = color.copy(alpha = alphaB)
        )
        Text(
            text = ">",
            style = VpnTypography.buttonText.copy(fontWeight = FontWeight.Bold),
            color = color.copy(alpha = alphaC)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = VpnTypography.headerSection,
        color = TextPrimary.copy(alpha = 0.92f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ServerListItem(
    server: Server,
    isPremium: Boolean,
    isSelected: Boolean,
    showDivider: Boolean,
    subtitle: String,
    onSelected: () -> Unit,
    onConnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) SurfaceCard.copy(alpha = 0.35f) else Color.Transparent)
                .clickable(onClick = onSelected)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagCircle(flag = server.flag, size = 32.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
            ) {
                Text(
                    text = server.name,
                    style = VpnTypography.cardTitle,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = VpnTypography.cardSubtitle,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ConnectChip(
                isPremium = isPremium,
                onClick = onConnect
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderNavy.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun ConnectChip(
    isPremium: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(if (isPremium) 110.dp else 92.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceInner)
            .border(1.dp, BorderNavy, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPremium) {
            DiamondGlyph(size = 12.dp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = Trans.get("connect"),
            style = VpnTypography.buttonText,
            color = TextPrimary
        )
    }
}

private fun sampleServerCount(name: String, premium: Boolean): Int {
    val normalized = name.lowercase()
    return when {
        "singapore" in normalized -> 18
        "russia" in normalized -> 12
        "united state" in normalized || "usa" in normalized || "united states" in normalized -> 26
        "canada" in normalized -> 21
        "australia" in normalized -> 14
        premium -> 20 + (normalized.length % 9)
        else -> 10 + (normalized.length % 9)
    }
}

@Composable
private fun FlagCircle(flag: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceCard)
            .border(1.dp, BorderNavy.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = flag, fontSize = (size.value * 0.46f).sp)
    }
}

@Composable
private fun DiamondGlyph(
    size: Dp,
    primary: Color = Color(0xFFEAF6FF),
    secondary: Color = AccentBlue
) {
    Box(
        modifier = Modifier
            .size(size)
            .rotate(45f)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
    )
}
