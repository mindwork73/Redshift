package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnDimensions
import com.example.ui.theme.VpnTypography
import com.example.ui.theme.pingQualityColor

@Composable
fun HomeScreenContent(
    onAddServerClick: () -> Unit,
    onOpenServers: () -> Unit
) {
    val context = LocalContext.current
    val state = RedShiftState.connectionState

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            RedShiftState.toggleVpn()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            RedShiftState.toggleVpn()
        }
    }

    val onToggleConnection: () -> Unit = {
        when (state) {
            ConnectionState.CONNECTED,
            ConnectionState.CONNECTING -> RedShiftState.toggleVpn()

            ConnectionState.DISCONNECTED -> {
                try {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnPermissionLauncher.launch(intent)
                        } else {
                            RedShiftState.toggleVpn()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnColors.Background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_cyber_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to VpnColors.Background.copy(alpha = 0.85f),
                        1f to VpnColors.Background
                    )
                )
        )
        HomeScreenPremium(
            onServerSelectClick = onOpenServers,
            onToggleConnection = onToggleConnection
        )
    }
}

@Composable
fun HomeScreenPremium(
    onServerSelectClick: () -> Unit,
    onToggleConnection: () -> Unit
) {
    val server = RedShiftState.getSelectedServer()
    val state = RedShiftState.connectionState
    val isConnected = state == ConnectionState.CONNECTED
    val isConnecting = state == ConnectionState.CONNECTING

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isConnected) {
                SessionTimer(seconds = RedShiftState.sessionDurationSeconds, visible = true)
            } else {
                Text(
                    text = "00:00:00",
                    style = VpnTypography.timer,
                    color = VpnColors.TextPrimary.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = isConnected) {
                ShieldRow()
            }

            Spacer(modifier = Modifier.height(48.dp))

            PowerButtonHero(
                isConnecting = isConnecting,
                isConnected = isConnected,
                onClick = onToggleConnection
            )

            Spacer(modifier = Modifier.height(32.dp))

            ConnectionStatusText(
                isConnected = isConnected,
                isConnecting = isConnecting
            )

            if (server != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = server.address,
                    style = VpnTypography.cardSubtitle,
                    color = VpnColors.TextPrimary.copy(alpha = 0.8f)
                )
            }
        }

        AnimatedVisibility(
            visible = isConnected && server != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            if (server != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    TooltipStatsCard(
                        serverName = server.name,
                        serverIp = server.address,
                        downloadSpeed = RedShiftState.downloadSpeed,
                        uploadSpeed = RedShiftState.uploadSpeed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            ServerSelectorCard(server = server, onClick = onServerSelectClick)
        }
    }
}

@Composable
private fun HomeTopBar() {
    val plan = RedShiftState.subscriptionPlan
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VpnDimensions.TopBarHeight)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(VpnColors.premiumGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RS",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "RedShift",
                color = VpnColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(Brush.horizontalGradient(VpnColors.premiumGradient))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = plan.ifBlank { Trans.get("premium_btn") },
                style = VpnTypography.premium
            )
        }
    }
}

@Composable
private fun ShieldRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(VpnColors.accentGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(VpnColors.accentGreen)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = Trans.get("status_secured"),
            style = VpnTypography.statusSecured
        )
    }
}

@Composable
fun SessionTimer(seconds: Long, visible: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val fontSize = if (maxWidth < 380.dp) VpnTypography.TimerSemiCompact else VpnTypography.TimerDefault
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            Text(
                text = String.format("%02d:%02d:%02d", hrs, mins, secs),
                color = VpnColors.TextPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun PowerButtonHero(
    isConnecting: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val isActive = isConnecting || isConnected
    val baseGlowColor = if (isConnecting) VpnColors.accentWarning else VpnColors.accentGreen

    Box(
        modifier = Modifier
            .size(200.dp)
            .drawBehind {
                if (isActive) {
                    drawCircle(
                        color = baseGlowColor.copy(alpha = pulseAlpha),
                        radius = size.minDimension / 2 * pulseScale
                    )
                    drawCircle(
                        color = baseGlowColor.copy(alpha = 0.1f),
                        radius = size.minDimension / 2.2f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(VpnColors.surfaceInner)
                .border(2.dp, VpnColors.borderLight, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = Trans.get("toggle_vpn"),
                tint = if (isConnected) VpnColors.accentGreen else if (isConnecting) VpnColors.accentWarning else VpnColors.TextPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun ConnectionStatusText(isConnected: Boolean, isConnecting: Boolean) {
    val text = when {
        isConnected -> Trans.get("connected")
        isConnecting -> Trans.get("connecting")
        else -> Trans.get("status_disconnected")
    }
    val color = when {
        isConnected -> VpnColors.accentGreen
        isConnecting -> VpnColors.accentWarning
        else -> VpnColors.TextPrimary
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isConnected || isConnecting) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = VpnTypography.statusMain.copy(color = color)
        )
    }
}

@Composable
private fun TooltipStatsCard(
    serverName: String,
    serverIp: String,
    downloadSpeed: Double,
    uploadSpeed: Double
) {
    Row(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VpnColors.surfaceGlass)
            .border(1.dp, VpnColors.borderLight, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName,
                style = VpnTypography.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = serverIp,
                style = VpnTypography.cardSubtitle
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "↓ ${speedStr(downloadSpeed)}",
                    style = VpnTypography.cardSubtitle,
                    color = VpnColors.TextPrimary
                )
                Text(
                    text = "↑ ${speedStr(uploadSpeed)}",
                    style = VpnTypography.cardSubtitle,
                    color = VpnColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ServerSelectorCard(server: Server?, onClick: () -> Unit) {
    val pingColor = pingQualityColor(server?.latency ?: 0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .shadow(
                14.dp,
                RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = VpnColors.accentGreen.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(VpnColors.surfaceGlass.copy(alpha = 0.45f))
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
            }
            .border(1.5.dp, Brush.linearGradient(VpnColors.premiumGradient), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(VpnColors.surfaceInner)
                .border(1.dp, VpnColors.borderLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (server != null) {
                Text(text = server.flag, fontSize = 22.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = VpnColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = server?.name ?: Trans.get("select_server"),
                    style = VpnTypography.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (server != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VpnColors.accentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = server.protocol,
                            style = VpnTypography.cardSubtitle.copy(fontSize = 10.sp, color = VpnColors.accentGreen),
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (server != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IP ${server.address}",
                        style = VpnTypography.cardSubtitle,
                        color = VpnColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PingBars(latency = server.latency)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (server.latency > 0) "${server.latency} ms" else "— ms",
                        style = VpnTypography.cardSubtitle,
                        color = if (server.latency > 0) pingColor else VpnColors.TextSecondary
                    )
                }
            } else {
                Text(
                    text = Trans.get("select_server"),
                    style = VpnTypography.cardSubtitle,
                    color = VpnColors.TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VpnColors.accentGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = VpnColors.accentGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}