package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBrush
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnDimensions
import com.example.ui.theme.VpnTypography
import com.example.ui.theme.pingQualityColor

/** UI-level connection states. Maps onto [ConnectionState]; Disconnecting/Error
 *  are reserved for future engine states and are unreachable with current engine. */
enum class VpnConnectionState {
    Disconnected, Connecting, Connected, Disconnecting, Error
}

private fun ConnectionState.toVpnUiState(): VpnConnectionState = when (this) {
    ConnectionState.DISCONNECTED -> VpnConnectionState.Disconnected
    ConnectionState.CONNECTING -> VpnConnectionState.Connecting
    ConnectionState.CONNECTED -> VpnConnectionState.Connected
}

@Composable
fun HomeScreenContent(
    onAddServerClick: () -> Unit,
    onOpenServers: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val server = RedShiftState.getSelectedServer()
    val uiState = RedShiftState.connectionState.toVpnUiState()

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

    val onPowerClick: () -> Unit = {
        when (uiState) {
            VpnConnectionState.Connected,
            VpnConnectionState.Connecting,
            VpnConnectionState.Disconnecting -> RedShiftState.toggleVpn()

            VpnConnectionState.Disconnected,
            VpnConnectionState.Error -> {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        HomeTopBar()

        Spacer(modifier = Modifier.height(4.dp))

        ConnectionHero(
            uiState = uiState,
            durationSeconds = RedShiftState.sessionDurationSeconds,
            serverName = server?.name,
            endpoint = server?.address,
            onPowerClick = onPowerClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        if (server != null) {
            Spacer(modifier = Modifier.height(12.dp))

            ConnectionInfoCard(
                serverName = server.name,
                endpoint = server.address,
                downloadSpeed = speedStr(RedShiftState.downloadSpeed),
                uploadSpeed = speedStr(RedShiftState.uploadSpeed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VpnDimensions.ScreenPaddingDefault)
            )

            Spacer(modifier = Modifier.height(10.dp))

            SelectedServerCard(
                serverName = server.name,
                endpoint = server.address,
                pingMs = server.latency,
                onClick = onOpenServers,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VpnDimensions.ScreenPaddingDefault)
            )

            if (RedShiftState.recentServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                RecentServersRow(
                    recent = RedShiftState.recentServers.take(5),
                    selectedId = RedShiftState.selectedServerId,
                    onSelect = { next -> RedShiftState.selectedServerId = next.id },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HomeTopBar(modifier: Modifier = Modifier) {
    val plan = RedShiftState.subscriptionPlan
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val horizontalPad =
            if (maxWidth < 360.dp) VpnDimensions.ScreenPaddingCompact else VpnDimensions.ScreenPaddingDefault
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(VpnDimensions.TopBarHeight)
                .padding(horizontal = horizontalPad),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AccentBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "RedShift",
                    color = VpnColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (plan.isNotBlank()) {
                PlanPill(plan = plan)
            }
        }
    }
}

@Composable
fun PlanPill(plan: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(VpnColors.AccentPurple.copy(alpha = 0.16f))
            .border(0.5.dp, VpnColors.AccentPurple.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = plan.take(16),
            color = VpnColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ConnectionHero(
    uiState: VpnConnectionState,
    durationSeconds: Long,
    serverName: String?,
    endpoint: String?,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsEnabled = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }

    Box(
        modifier = modifier.drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(VpnColors.AccentBlue.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.38f),
                    radius = size.maxDimension
                )
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SessionTimer(
                seconds = durationSeconds,
                visible = uiState == VpnConnectionState.Connected
            )

            Spacer(modifier = Modifier.height(if (uiState == VpnConnectionState.Connected) 12.dp else 2.dp))

            SecurityLabel(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            VpnPowerButton(
                uiState = uiState,
                onClick = onPowerClick,
                pulseAnimations = animationsEnabled
            )

            Spacer(modifier = Modifier.height(22.dp))

            ConnectionStatusText(
                uiState = uiState,
                serverName = serverName,
                endpoint = endpoint
            )
        }
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
private fun SecurityLabel(uiState: VpnConnectionState) {
    val (text, color) = when (uiState) {
        VpnConnectionState.Connected ->
            Trans.get("status_secured") to VpnColors.Success
        VpnConnectionState.Connecting, VpnConnectionState.Disconnecting ->
            Trans.get("status_establishing") to VpnColors.AccentPurple
        VpnConnectionState.Error ->
            Trans.get("status_failed") to VpnColors.Error
        VpnConnectionState.Disconnected ->
            Trans.get("status_not_protected") to VpnColors.Warning
    }

    val animatedColor by animateColorAsState(color, label = "sec_label_color")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (uiState == VpnConnectionState.Connected) {
                Icons.Default.VerifiedUser
            } else {
                Icons.Default.Public
            },
            contentDescription = null,
            tint = animatedColor,
            modifier = Modifier.size(18.dp)
        )
        AnimatedContent(targetState = text, label = "sec_label") { t ->
            Text(
                text = t,
                color = animatedColor,
                fontSize = VpnTypography.SecurityLabel,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun VpnPowerButton(
    uiState: VpnConnectionState,
    onClick: () -> Unit,
    pulseAnimations: Boolean = true,
    modifier: Modifier = Modifier
) {
    val enabled = uiState == VpnConnectionState.Connected ||
            uiState == VpnConnectionState.Disconnected ||
            uiState == VpnConnectionState.Error

    val coreColor = when (uiState) {
        VpnConnectionState.Connected, VpnConnectionState.Disconnecting -> VpnColors.SuccessDark
        VpnConnectionState.Error -> VpnColors.Error
        else -> VpnColors.SurfaceElevated
    }
    val accent = when (uiState) {
        VpnConnectionState.Connected -> VpnColors.Success
        VpnConnectionState.Connecting -> VpnColors.AccentPurple
        VpnConnectionState.Disconnecting -> VpnColors.AccentBlue
        VpnConnectionState.Error -> VpnColors.Error
        else -> VpnColors.SurfaceElevated
    }

    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "power_press"
    )

    val showDisconnectLabel = uiState == VpnConnectionState.Connected ||
            uiState == VpnConnectionState.Connecting ||
            uiState == VpnConnectionState.Disconnecting
    val contentDescr =
        if (showDisconnectLabel) Trans.get("cd_power_disconnect") else Trans.get("cd_power_connect")

    BoxWithConstraints(modifier = modifier) {
        val canvasSize: Dp = when {
            maxWidth < 380.dp -> VpnDimensions.PowerButtonCompact
            maxWidth < 430.dp -> VpnDimensions.PowerButtonDefault
            else -> VpnDimensions.PowerButtonWide
        }
        val buttonSize = canvasSize * 0.68f
        val iconSize = canvasSize * 0.34f
        val ringTint = VpnColors.Success.copy(
            alpha = if (uiState == VpnConnectionState.Connected) 0.07f else 0.045f
        )

        Box(
            modifier = Modifier.size(canvasSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        for (i in 1..3) {
                            val r = size.minDimension / 2 * (0.45f + i * 0.16f)
                            drawCircle(
                                color = ringTint,
                                radius = r,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
            )

            if (uiState == VpnConnectionState.Connecting && pulseAnimations) {
                PulsingRing(color = accent, modifier = Modifier.matchParentSize())
            }

            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .scale(pressScale)
                    .clip(CircleShape)
                    .background(coreColor)
                    .border(1.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .drawBehind {
                        if (uiState == VpnConnectionState.Connected) {
                            drawCircle(
                                color = VpnColors.Success.copy(alpha = 0.28f),
                                radius = size.minDimension / 2
                            )
                        } else if (uiState == VpnConnectionState.Error) {
                            drawCircle(
                                color = VpnColors.Error.copy(alpha = 0.16f),
                                radius = size.minDimension / 2
                            )
                        }
                    }
                    .semantics { contentDescription = contentDescr }
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactions,
                        indication = LocalIndication.current,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState == VpnConnectionState.Connecting) {
                    CircularProgressIndicator(
                        color = VpnColors.AccentPurple,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(iconSize * 0.9f)
                    )
                } else if (uiState == VpnConnectionState.Disconnecting) {
                    CircularProgressIndicator(
                        color = VpnColors.AccentBlue,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(iconSize * 0.9f)
                    )
                } else {
                    val iconTint = when {
                        uiState == VpnConnectionState.Connected -> Color.White
                        uiState == VpnConnectionState.Error -> VpnColors.Error
                        else -> Color.White.copy(alpha = 0.88f)
                    }
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingRing(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse_ring")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = modifier.drawBehind {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    )
}

@Composable
fun ConnectionStatusText(
    uiState: VpnConnectionState,
    serverName: String?,
    endpoint: String?,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "status_main",
        modifier = modifier
    ) { state ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VpnDimensions.ScreenPaddingDefault)
        ) {
            when (state) {
                VpnConnectionState.Connected -> {
                    Text(
                        text = Trans.get("connected"),
                        color = VpnColors.Success,
                        fontSize = VpnTypography.StatusBig,
                        fontWeight = FontWeight.Bold
                    )
                    if (endpoint != null) {
                        Text(
                            text = endpoint,
                            color = VpnColors.TextSecondary,
                            fontSize = VpnTypography.Endpoint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                VpnConnectionState.Connecting -> {
                    Text(
                        text = Trans.get("connecting"),
                        color = VpnColors.AccentPurple,
                        fontSize = VpnTypography.StatusConnecting,
                        fontWeight = FontWeight.Bold
                    )
                    if (serverName != null) {
                        Text(
                            text = serverName,
                            color = VpnColors.TextSecondary.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                VpnConnectionState.Disconnecting -> {
                    Text(
                        text = Trans.get("status_disconnecting"),
                        color = VpnColors.AccentBlue,
                        fontSize = VpnTypography.StatusConnecting,
                        fontWeight = FontWeight.Bold
                    )
                    if (serverName != null) {
                        Text(
                            text = serverName,
                            color = VpnColors.TextSecondary.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                VpnConnectionState.Error -> {
                    Text(
                        text = Trans.get("status_failed"),
                        color = VpnColors.Error,
                        fontSize = VpnTypography.StatusBig,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Trans.get("tap_to_connect"),
                        color = VpnColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
                VpnConnectionState.Disconnected -> {
                    Text(
                        text = Trans.get("tap_to_connect"),
                        color = VpnColors.TextPrimary,
                        fontSize = VpnTypography.StatusBig,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = serverName ?: "—",
                        color = VpnColors.TextSecondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionInfoCard(
    serverName: String,
    endpoint: String?,
    downloadSpeed: String,
    uploadSpeed: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = VpnDimensions.FloatingCardMinHeight)
            .clip(RoundedCornerShape(VpnDimensions.RadiusMedium))
            .background(VpnColors.Surface.copy(alpha = 0.82f))
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(VpnDimensions.RadiusMedium))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(VpnColors.AccentBlue.copy(alpha = 0.14f))
                .border(0.5.dp, VpnColors.AccentBlue.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = VpnColors.BrandCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName,
                color = VpnColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            if (endpoint != null) {
                Text(
                    text = endpoint,
                    color = VpnColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedItem(label = Trans.get("download"), value = downloadSpeed, icon = Icons.Default.Download)
                SpeedItem(label = Trans.get("upload"), value = uploadSpeed, icon = Icons.Default.Upload)
            }
        }
    }
}

@Composable
private fun SpeedItem(label: String, value: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VpnColors.TextMuted,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = VpnColors.TextMuted,
            fontSize = 10.sp
        )
        Text(
            text = if (value.isBlank()) "—" else value,
            color = VpnColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
fun SelectedServerCard(
    serverName: String,
    endpoint: String?,
    pingMs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qualityColor = pingQualityColor(pingMs)

    Row(
        modifier = modifier
            .heightIn(min = VpnDimensions.ServerCardMinHeight)
            .clip(RoundedCornerShape(VpnDimensions.RadiusLarge))
            .background(VpnColors.SurfaceElevated.copy(alpha = 0.92f))
            .border(0.5.dp, VpnColors.AccentPurple.copy(alpha = 0.18f), RoundedCornerShape(VpnDimensions.RadiusLarge))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(VpnColors.AccentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = VpnColors.BrandCyan,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName,
                color = VpnColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = endpoint ?: "—",
                color = VpnColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        SignalBars(color = if (pingMs > 0) qualityColor else VpnColors.TextMuted.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (pingMs > 0) "$pingMs ms" else "— ms",
            color = if (pingMs > 0) qualityColor else VpnColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = VpnColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SignalBars(color: Color) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(4.dp, 7.dp, 10.dp).forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun RecentServersRow(
    recent: List<Server>,
    selectedId: String,
    onSelect: (Server) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VpnDimensions.ScreenPaddingDefault),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Trans.get("recent_short"),
                color = VpnColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Trans.get("see_all"),
                color = VpnColors.AccentPurple.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = VpnDimensions.ScreenPaddingDefault),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recent, key = { it.id }) { server ->
                RecentServerCard(
                    server = server,
                    isSelected = server.id == selectedId,
                    onClick = { onSelect(server) }
                )
            }
        }
    }
}

@Composable
private fun RecentServerCard(
    server: Server,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pingColor = pingQualityColor(server.latency)
    Column(
        modifier = Modifier
            .widthIn(min = 150.dp, max = 170.dp)
            .clip(RoundedCornerShape(VpnDimensions.RadiusMedium))
            .background(VpnColors.Surface.copy(alpha = 0.72f))
            .border(
                1.dp,
                if (isSelected) VpnColors.AccentPurple.copy(alpha = 0.7f)
                else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(VpnDimensions.RadiusMedium)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) VpnColors.AccentPurple.copy(alpha = 0.18f) else VpnColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = if (isSelected) VpnColors.AccentPurple else VpnColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = server.name,
            color = VpnColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (server.latency > 0) "${server.latency} ms" else "— ms",
            color = if (server.latency > 0) pingColor else VpnColors.TextMuted,
            fontSize = 11.sp,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
        )
    }
}

@Preview(name = "Home · Connected", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
private fun HomeConnectedPreview() {
    MaterialTheme {
        Column(modifier = Modifier.background(CyberBackground)) {
            ConnectionHero(
                uiState = VpnConnectionState.Connected,
                durationSeconds = 276,
                serverName = "RedPill Cloud",
                endpoint = "redpillcloud.ru",
                onPowerClick = {}
            )
            ConnectionInfoCard(
                serverName = "RedPill Cloud",
                endpoint = "redpillcloud.ru",
                downloadSpeed = "0.2 KB/s",
                uploadSpeed = "0.1 KB/s"
            )
        }
    }
}

@Preview(name = "Home · Disconnected", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
private fun HomeDisconnectedPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionHero(
                uiState = VpnConnectionState.Disconnected,
                durationSeconds = 0,
                serverName = "RedPill Cloud",
                endpoint = null,
                onPowerClick = {}
            )
        }
    }
}

@Preview(name = "Home · Connecting · Narrow 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeConnectingPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionHero(
                uiState = VpnConnectionState.Connecting,
                durationSeconds = 0,
                serverName = "RedPill Cloud",
                endpoint = null,
                onPowerClick = {}
            )
        }
    }
}

@Preview(name = "Home · Error", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
private fun HomeErrorPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionHero(
                uiState = VpnConnectionState.Error,
                durationSeconds = 0,
                serverName = "RedPill Cloud",
                endpoint = null,
                onPowerClick = {}
            )
        }
    }
}