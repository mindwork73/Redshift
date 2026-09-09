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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BackgroundNavy
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VpnDimensions
import com.example.ui.theme.VpnTypography

@Composable
fun HomeScreenContent(
    onPremiumClick: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit
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
                    if (
                        Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
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
                } catch (_: Exception) {
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundNavy)
    ) {
        VideoBackground(
            scrimOpacity = 0.55f,
            modifier = Modifier.fillMaxSize()
        )

        HomeScreenPremium(
            onServerSelectClick = onOpenServers,
            onToggleConnection = onToggleConnection,
            onMenuClick = onOpenSettings,
            onPremiumClick = onPremiumClick
        )
    }
}

@Composable
fun HomeScreenPremium(
    onServerSelectClick: () -> Unit,
    onToggleConnection: () -> Unit,
    onMenuClick: () -> Unit,
    onPremiumClick: () -> Unit
) {
    val server = RedShiftState.getSelectedServer()
    val state = RedShiftState.connectionState
    val isConnected = state == ConnectionState.CONNECTED
    val isConnecting = state == ConnectionState.CONNECTING

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val timerTop = maxHeight * 0.13f
        val powerTop = maxHeight * 0.22f
        val statusTop = maxHeight * 0.56f
        val popupTop = maxHeight * 0.60f

        HomeTopBar(
            onMenuClick = onMenuClick,
            onPremiumClick = onPremiumClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = timerTop),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SessionTimer(
                seconds = RedShiftState.sessionDurationSeconds,
                isConnected = isConnected
            )
            AnimatedVisibility(visible = isConnected) {
                SecurityStatusRow(modifier = Modifier.padding(top = 8.dp))
            }
        }

        HomePowerButton(
            isConnecting = isConnecting,
            isConnected = isConnected,
            onClick = onToggleConnection,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = powerTop)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusTop),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionStatusText(
                isConnected = isConnected,
                isConnecting = isConnecting
            )

            server?.let { currentServer ->
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DiamondGlyph(size = 11.dp)
                    Text(
                        text = currentServer.address,
                        style = VpnTypography.bodyRegular,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        server?.let { currentServer ->
            ConnectionPopupStack(
                server = currentServer,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = popupTop)
            )
        }

        ServerLocationBar(
            server = server,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            onClick = onServerSelectClick
        )
    }
}

@Composable
private fun HomeTopBar(
    onMenuClick: () -> Unit,
    onPremiumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = Trans.get("tab_settings"),
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        PremiumCapsuleButton(onClick = onPremiumClick)
    }
}

@Composable
private fun PremiumCapsuleButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(118.dp)
            .height(36.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = AccentRed.copy(alpha = 0.24f),
                spotColor = AccentBlue.copy(alpha = 0.24f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(AccentBlue, AccentRed)))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiamondGlyph(size = 16.dp, primary = Color(0xFFEAF6FF), secondary = Color(0xFF78BFFF))
            Text(
                text = Trans.get("premium_btn"),
                style = VpnTypography.premium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SessionTimer(seconds: Long, isConnected: Boolean) {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val timerText = if (isConnected) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        "00:00:00"
    }

    Text(
        text = timerText,
        style = VpnTypography.displayLarge,
        color = if (isConnected) TextPrimary else TextPrimary.copy(alpha = 0.26f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SecurityStatusRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = StatusGreen,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = Trans.get("status_secured"),
            style = VpnTypography.statusSecured,
            color = StatusGreen
        )
    }
}

@Composable
private fun HomePowerButton(
    isConnecting: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "home_pulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    val ringColor = when {
        isConnected -> StatusGreen
        isConnecting -> AccentBlue
        else -> BorderNavy
    }

    Box(
        modifier = modifier.size(VpnDimensions.PowerButtonWide),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = BorderNavy.copy(alpha = 0.26f),
                radius = 78.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = BorderNavy.copy(alpha = 0.18f),
                radius = 110.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            if (isConnected || isConnecting) {
                drawCircle(
                    color = ringColor.copy(alpha = 0.18f * (1f - pulseProgress)),
                    radius = 92.dp.toPx() + 28.dp.toPx() * pulseProgress,
                    center = center,
                    style = Stroke(width = 1.25.dp.toPx())
                )
                drawCircle(
                    color = ringColor.copy(alpha = if (isConnected) 0.10f else 0.06f),
                    radius = 72.dp.toPx(),
                    center = center
                )
            }
        }

        Box(
            modifier = Modifier
                .size(VpnDimensions.PowerButtonDefault)
                .shadow(
                    elevation = if (isConnected) 26.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = ringColor.copy(alpha = if (isConnected) 0.42f else 0.12f),
                    spotColor = ringColor.copy(alpha = if (isConnected) 0.42f else 0.12f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF454B55),
                            Color(0xFF3A404A),
                            SurfaceCard
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = when {
                        isConnected -> StatusGreen.copy(alpha = 0.62f)
                        isConnecting -> AccentBlue.copy(alpha = 0.62f)
                        else -> BorderNavy
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = if (isConnected) {
                    Trans.get("cd_power_disconnect")
                } else {
                    Trans.get("cd_power_connect")
                },
                tint = TextPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun ConnectionStatusText(
    isConnected: Boolean,
    isConnecting: Boolean
) {
    val text = when {
        isConnected -> Trans.get("connected")
        isConnecting -> Trans.get("connecting")
        else -> Trans.get("tap_to_connect")
    }
    val color = when {
        isConnected -> StatusGreen
        isConnecting -> AccentBlue
        else -> TextPrimary
    }

    Text(
        text = text,
        style = VpnTypography.statusMain,
        color = color
    )
}

@Composable
private fun ConnectionPopupStack(
    server: Server,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-6).dp, y = 24.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(StatusGreen)
                .border(4.dp, BackgroundNavy.copy(alpha = 0.28f), CircleShape)
                .shadow(14.dp, CircleShape, ambientColor = StatusGreen, spotColor = StatusGreen)
        )

        ConnectionPopupCard(
            server = server,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ConnectionPopupCard(
    server: Server,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .height(120.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            ),
        color = SurfaceGlass.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, BorderNavy.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlagCircle(flag = server.flag, size = 18.dp)
                Text(
                    text = server.name,
                    style = VpnTypography.cardTitle.copy(fontSize = 14.sp),
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = server.address,
                    style = VpnTypography.cardSubtitle,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricDirectionBlock(
                    icon = Icons.Default.NorthEast,
                    value = popupMetricValue(RedShiftState.uploadSpeed, "127.2 kb"),
                    tint = TextPrimary
                )
                MetricDirectionBlock(
                    icon = Icons.Default.SouthWest,
                    value = popupMetricValue(RedShiftState.downloadSpeed, "127.2 kb"),
                    tint = TextPrimary
                )
            }
        }
    }
}

private fun popupMetricValue(valueKbps: Double, fallback: String): String {
    if (valueKbps <= 0.0) return fallback
    return if (valueKbps >= 1024.0) {
        String.format("%.1f mb", valueKbps / 1024.0)
    } else {
        String.format("%.1f kb", valueKbps)
    }
}

@Composable
private fun MetricDirectionBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            style = VpnTypography.cardSubtitle,
            color = TextPrimary,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ServerLocationBar(
    server: Server?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.38f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceGlass.copy(alpha = 0.64f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, BorderNavy.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (server != null) {
                FlagCircle(flag = server.flag, size = 40.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderNavy, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = server?.name ?: Trans.get("select_server"),
                    style = VpnTypography.statusMain,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IP ${server?.address ?: "—"}",
                        style = VpnTypography.cardSubtitle,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (server != null) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniPingBars(color = StatusGreen)
                        Text(
                            text = if (server.latency > 0) "${server.latency} ms" else "164 ms",
                            style = VpnTypography.cardSubtitle,
                            color = StatusGreen,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MiniPingBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        MiniBar(4.dp, color)
        MiniBar(6.dp, color)
        MiniBar(8.dp, color)
        MiniBar(10.dp, color)
    }
}

@Composable
private fun MiniBar(height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(height)
            .clip(RoundedCornerShape(1.dp))
            .background(color)
    )
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
    primary: Color = AccentCyan,
    secondary: Color = AccentBlue
) {
    Box(
        modifier = Modifier
            .size(size)
            .rotate(45f)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.linearGradient(listOf(primary, secondary)))
            .border(0.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
    )
}
