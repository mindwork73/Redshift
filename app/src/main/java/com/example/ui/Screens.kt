package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.service.AdminStats
import com.example.service.RedPillApiClient
import kotlinx.coroutines.launch
import android.app.Activity
import android.net.VpnService

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val currentLang = LocalizationState.currentLanguage
    val layoutDirection = LocalizationState.getLayoutDirection()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
        ) {
            if (!RedShiftState.isOnboarded) {
                OnboardingScreen(onFinished = { RedShiftState.isOnboarded = true })
            } else {
                var currentTab by remember { mutableStateOf("dashboard") }
                var showAddServerSheet by remember { mutableStateOf(false) }
                val isAdmin = RedShiftState.apiAdminToken.isNotEmpty()

                Scaffold(
                    bottomBar = {
                        CyberBottomBar(
                            selectedTab = currentTab,
                            onTabSelected = { 
                                if (it == "add_server") {
                                    showAddServerSheet = true
                                } else {
                                    currentTab = it 
                                }
                            }
                        )
                    },
                    containerColor = Color.Transparent
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "tab_navigation"
                        ) { tab ->
                            when (tab) {
                                "dashboard" -> DashboardScreen(onAddServerClick = { showAddServerSheet = true })
                                "servers" -> ServersScreen(onAddServerClick = { showAddServerSheet = true })
                                "settings" -> SettingsScreen()
                            }
                        }
                    }
                }

                if (showAddServerSheet) {
                    AddServerModalSheet(
                        onDismiss = { showAddServerSheet = false },
                        onAdded = {
                            showAddServerSheet = false
                            Toast.makeText(context, Trans.get("connected") + " Node Config", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// Onboarding Screen
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var panelIndex by remember { mutableStateOf(0) }
    
    val title = when (panelIndex) {
        0 -> Trans.get("onboard_1_title")
        1 -> Trans.get("onboard_2_title")
        else -> Trans.get("onboard_3_title")
    }

    val desc = when (panelIndex) {
        0 -> Trans.get("onboard_1_desc")
        1 -> Trans.get("onboard_2_desc")
        else -> Trans.get("onboard_3_desc")
    }

    val icon = when (panelIndex) {
        0 -> Icons.Default.Visibility
        1 -> Icons.AutoMirrored.Filled.AltRoute
        else -> Icons.Default.Security
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(RedPrimary, RedGradientEnd)))
            )
            Text(
                text = "RedShift",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Mid Art Panel
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (panelIndex == 0) {
                // Display the generated hero image asset
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, RedPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(180.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cyber_hero),
                        contentDescription = "Awakening Eye Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(CyberCard)
                        .border(1.5.dp, RedPrimary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Dot Indicators + Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (index == panelIndex) 20.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index == panelIndex) RedPrimary else TextMuted)
                    )
                }
            }

            CyberButton(
                text = if (panelIndex == 2) Trans.get("get_started") else "Next",
                onClick = {
                    if (panelIndex < 2) {
                        panelIndex++
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Bottom navigation bar
@Composable
fun CyberBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(16.dp),
        color = CyberCard,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = RedPrimary.copy(alpha = 0.15f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = Trans.get("tab_dashboard"),
                    icon = Icons.Default.Dashboard,
                    isSelected = selectedTab == "dashboard",
                    onClick = { onTabSelected("dashboard") }
                )
                BottomNavItem(
                    label = Trans.get("tab_servers"),
                    icon = Icons.Default.Router,
                    isSelected = selectedTab == "servers",
                    onClick = { onTabSelected("servers") }
                )
                BottomNavItem(
                    label = Trans.get("tab_add_server"),
                    icon = Icons.Default.AddCircle,
                    isSelected = false,
                    onClick = { onTabSelected("add_server") },
                    accentColor = RedPrimary
                )
                BottomNavItem(
                    label = Trans.get("tab_settings"),
                    icon = Icons.Default.Settings,
                    isSelected = selectedTab == "settings",
                    onClick = { onTabSelected("settings") }
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = TextSecondary
) {
    val activeColor = if (accentColor == RedPrimary) RedPrimary else RedPrimary
    val tintColor = if (isSelected) activeColor else TextSecondary

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = tintColor,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(RedPrimary)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(4.dp)
            )
        }
    }
}

// SCREEN 1: HOME / DASHBOARD
@Composable
fun DashboardScreen(onAddServerClick: () -> Unit) {
    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // BENTO CELL 1: Header / Title Card
            HeaderBentoCard()

            // BENTO CELL 2: Power & Connection Ring Card (Big Centerpiece)
            ConnectionBentoCard()

            // BENTO CELL 3 & 4: Speed Indicators (Two adjacent columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpeedBentoCard(
                    isDownload = true,
                    speedValue = RedShiftState.downloadSpeed,
                    modifier = Modifier.weight(1f)
                )
                SpeedBentoCard(
                    isDownload = false,
                    speedValue = RedShiftState.uploadSpeed,
                    modifier = Modifier.weight(1f)
                )
            }

            // BENTO CELL 5: Status Slider / Quick Details
            QuickDetailsBentoRow()

            // BENTO CELL 6: Recent Nodes Grid/Slider
            RecentServersBentoCard()

            // Powered by
            Text(
                text = Trans.get("powered_by") + " • redpillcloud.ru",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }

        // FAB floating
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onAddServerClick,
                containerColor = RedPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add node")
            }
        }
    }
}

@Composable
fun HeaderBentoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberCard.copy(alpha = 0.8f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(RedPrimary, RedGradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "REDSHIFT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = Trans.get("app_tagline"),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberElevated)
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "V1.0.0",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (RedShiftState.isLoggedIn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleSecondary.copy(alpha = 0.15f))
                        .border(0.5.dp, PurpleSecondary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRO",
                        color = PurpleSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionBentoCard() {
    val context = LocalContext.current
    val connectionState = RedShiftState.connectionState
    val statusText = when (connectionState) {
        ConnectionState.DISCONNECTED -> Trans.get("disconnected")
        ConnectionState.CONNECTING -> Trans.get("connecting")
        ConnectionState.CONNECTED -> Trans.get("connected")
    }
    val statusColor = when (connectionState) {
        ConnectionState.DISCONNECTED -> RedPrimary
        ConnectionState.CONNECTING -> AmberWarning
        ConnectionState.CONNECTED -> SuccessGreen
    }
    val subText = when (connectionState) {
        ConnectionState.DISCONNECTED -> "Tap to wake up"
        ConnectionState.CONNECTING -> "Securing tunnel..."
        ConnectionState.CONNECTED -> "Tap to secure"
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            RedShiftState.toggleVpn()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            RedShiftState.toggleVpn()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberCard.copy(alpha = 0.8f))
            .border(
                BorderStroke(
                    1.dp,
                    if (connectionState == ConnectionState.CONNECTED) SuccessGreen.copy(alpha = 0.25f)
                    else if (connectionState == ConnectionState.CONNECTING) AmberWarning.copy(alpha = 0.25f)
                    else RedPrimary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        // Subtle ambient neon blur glow in background
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PulsingConnectionRing(
                connectionState = connectionState,
                onClick = {
                    Toast.makeText(context, "Ring clicked!", Toast.LENGTH_SHORT).show()
                    try {
                        Log.e("RedShiftVPN", "onClick fired")
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(context, "Requesting notification perm", Toast.LENGTH_SHORT).show()
                            Log.e("RedShiftVPN", "Requesting POST_NOTIFICATIONS")
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnPermissionLauncher.launch(intent)
                            } else {
                                RedShiftState.toggleVpn()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("RedShiftVPN", "onClick error: ${e.message}", e)
                    }
                }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pulsing neon dot matching connection state
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Text(
                    text = subText.uppercase(),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                AnimatedVisibility(visible = connectionState == ConnectionState.CONNECTED) {
                    val server = RedShiftState.getSelectedServer()
                    if (server != null) {
                        Text(
                            text = "${server.flag} ${server.name}",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedBentoCard(
    isDownload: Boolean,
    speedValue: Double,
    modifier: Modifier = Modifier
) {
    val title = if (isDownload) Trans.get("download") else Trans.get("upload")
    val color = if (isDownload) RedPrimary else PurpleSecondary
    val icon = if (isDownload) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    
    // Simulate speed progress: map speed speedValue (0..100) to progress (0f..1f)
    val progress = (speedValue / 100.0).coerceIn(0.1, 1.0).toFloat()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberCard.copy(alpha = 0.8f))
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = title.uppercase(),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = LocalizationState.formatSpeed(speedValue),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Mbps",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // High-polish progress track with glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CyberElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun QuickDetailsBentoRow() {
    val seconds = RedShiftState.sessionDurationSeconds
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeStr = String.format("%02d:%02d:%02d", hrs, mins, secs)

    val selectedServer = RedShiftState.getSelectedServer()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick info capsules with thin borders and icons
        QuickCapsule(icon = "⏱", label = timeStr)
        QuickCapsule(icon = "💾", label = String.format("%.2f MB", RedShiftState.totalDataUsedMb))
        QuickCapsule(badge = selectedServer?.protocol ?: "VLESS", label = selectedServer?.name?.split(" • ")?.lastOrNull() ?: "Frankfurt")
    }
}

@Composable
fun QuickCapsule(
    icon: String? = null,
    badge: String? = null,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(CyberElevated)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 12.sp)
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RedPrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(text = badge, color = RedPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = label,
                color = TextPrimary.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentServersBentoCard() {
    val loggedIn = RedShiftState.isLoggedIn || RedShiftState.subscriptionUrl.isNotBlank()
    val hasServers = RedShiftState.servers.isNotEmpty()
    val hasRecent = RedShiftState.recentServers.isNotEmpty()

    if (!loggedIn && !hasServers) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberCard.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔗", fontSize = 28.sp)
                Text("Import your subscription to get started", color = TextSecondary, fontSize = 13.sp)
                Text("Go to Settings → Import Subscription", color = TextMuted, fontSize = 11.sp)
            }
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var pinging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasRecent) "Recent" else "Servers",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            if (hasServers) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RedPrimary.copy(alpha = 0.15f))
                            .clickable(enabled = !pinging) {
                                pinging = true
                                scope.launch {
                                    try {
                                        val client = RedPillApiClient()
                                        RedShiftState.servers.forEachIndexed { index, server ->
                                            val start = System.currentTimeMillis()
                                            try {
                                                val result = client.ping()
                                                if (result) {
                                                    val elapsed = (System.currentTimeMillis() - start).toInt()
                                                    RedShiftState.servers[index] = server.copy(latency = elapsed)
                                                }
                                            } catch (_: Exception) {
                                                RedShiftState.servers[index] = server.copy(latency = -1)
                                            }
                                        }
                                    } catch (_: Exception) {}
                                    pinging = false
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (pinging) "•••" else "PING",
                            color = RedPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (hasServers) {
                        Text(
                            text = (if (expanded) "▲" else "▼"),
                            color = RedPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { expanded = !expanded }
                        )
                    }
                }
            }
        }

        val displayServers = if (hasRecent && !expanded) RedShiftState.recentServers.take(4) else RedShiftState.servers

        if (displayServers.isEmpty()) {
            Text("No servers yet", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else if (!expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayServers.take(4).forEach { server ->
                    ServerMiniCard(server = server)
                }
            }
        } else {
            RedShiftState.servers.forEach { server ->
                val isSelected = RedShiftState.selectedServerId == server.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) RedPrimary.copy(alpha = 0.1f) else CyberCard)
                        .border(
                            1.dp,
                            if (isSelected) RedPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { RedShiftState.selectedServerId = server.id }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = server.flag, fontSize = 20.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = server.name.split(" • ").lastOrNull() ?: server.name,
                                    color = if (isSelected) RedPrimary else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = server.protocol,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val statusColor = when {
                                server.latency <= 0 -> TextMuted
                                server.latency < 50 -> SuccessGreen
                                server.latency < 150 -> WarningAmber
                                else -> ErrorRed
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            if (server.latency > 0) {
                                Text(
                                    text = "${server.latency}ms",
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServerMiniCard(server: Server) {
    val isSelected = RedShiftState.selectedServerId == server.id
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberCard)
            .border(
                1.dp,
                if (isSelected) RedPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .clickable { RedShiftState.selectedServerId = server.id }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = server.flag, fontSize = 20.sp)

                val statusColor = when {
                    server.latency <= 0 -> TextMuted
                    server.latency < 50 -> SuccessGreen
                    server.latency < 150 -> WarningAmber
                    else -> ErrorRed
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Text(
                text = server.name.split(" • ").lastOrNull() ?: server.name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (server.latency > 0) {
                Text(
                    text = "${server.latency}ms",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// SCREEN 2: SERVERS LIST
@Composable
fun ServersScreen(onAddServerClick: () -> Unit) {
    val loggedIn = RedShiftState.isLoggedIn || RedShiftState.subscriptionUrl.isNotBlank()

    if (!loggedIn && RedShiftState.servers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Link, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Text("Import a subscription to see servers", color = TextSecondary, fontSize = 14.sp)
                Text("Go to Settings → Import Subscription", color = TextMuted, fontSize = 12.sp)
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedProtocolFilter by remember { mutableStateOf("All") }

    val filterChips = listOf("All", "VLESS", "VMess", "Trojan", "Shadowsocks", "Hysteria 2", "Favorites")

    val filteredServers = RedShiftState.servers.filter { server ->
        val matchesSearch = server.name.contains(searchQuery, ignoreCase = true) ||
                            server.address.contains(searchQuery, ignoreCase = true) ||
                            server.protocol.contains(searchQuery, ignoreCase = true)
        
        val matchesFilter = when (selectedProtocolFilter) {
            "All" -> true
            "Favorites" -> server.latency < 25
            else -> server.protocol.contains(selectedProtocolFilter, ignoreCase = true)
        }

        matchesSearch && matchesFilter
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = Trans.get("tab_servers"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = Trans.get("search_placeholder"), fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = RedPrimary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = RedPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Filter Chips (Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterChips.forEach { chip ->
                    val isActive = selectedProtocolFilter == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) RedPrimary else CyberCard)
                            .border(
                                0.5.dp,
                                if (isActive) RedPrimary else TextMuted,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedProtocolFilter = chip }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            color = if (isActive) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Server Cards List
            if (filteredServers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                        Text(text = Trans.get("empty_servers"), color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredServers) { server ->
                        val isSelected = RedShiftState.selectedServerId == server.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { RedShiftState.selectedServerId = server.id }
                                .border(
                                    BorderStroke(
                                        if (isSelected) 1.5.dp else 0.5.dp,
                                        if (isSelected) RedPrimary else TextMuted.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = CyberCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        if (isSelected) {
                                            // Draw left highlight bar
                                            drawRect(
                                                color = RedPrimary,
                                                topLeft = Offset(0f, 0f),
                                                size = Size(4.dp.toPx(), this@drawBehind.size.height)
                                            )
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = server.flag, fontSize = 28.sp)

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = server.name,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ProtocolBadge(protocol = server.protocol)
                                        LatencyBadge(ping = server.latency)
                                    }
                                }

                                // Right speed / traffic indicator
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = String.format("%.1f/%.1f GB", server.usedTraffic, server.totalTraffic),
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Custom visual micro-bar
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(TextMuted)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((server.usedTraffic / server.totalTraffic).toFloat())
                                                .background(RedPrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB floating
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onAddServerClick,
                containerColor = RedPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add node")
            }
        }
    }
}

// SCREEN 3: ADD SERVER MODAL SHEET (Manual / Subscription Tabs)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerModalSheet(
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Manual Config, 1 = Subscription

    // Manual Fields
    var remarkName by remember { mutableStateOf("") }
    var selectedProtocol by remember { mutableStateOf("VLESS") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var uuidPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var encryption by remember { mutableStateOf("auto") }
    var transport by remember { mutableStateOf("tcp") }
    var tlsEnabled by remember { mutableStateOf(true) }
    var sni by remember { mutableStateOf("") }
    var allowInsecure by remember { mutableStateOf(false) }
    var networkType by remember { mutableStateOf("ipv4") }

    // Subscription Fields
    var subscriptionUrl by remember { mutableStateOf("") }
    var autoDetect by remember { mutableStateOf(true) }
    var updateInterval by remember { mutableStateOf("24h") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CyberElevated,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = 0 }
                        .drawBehind {
                            if (activeTab == 0) {
                                drawLine(
                                    color = RedPrimary,
                                    start = Offset(0f, this@drawBehind.size.height),
                                    end = Offset(this@drawBehind.size.width, this@drawBehind.size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Trans.get("manual_config"),
                        color = if (activeTab == 0) RedPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = 1 }
                        .drawBehind {
                            if (activeTab == 1) {
                                drawLine(
                                    color = RedPrimary,
                                    start = Offset(0f, this@drawBehind.size.height),
                                    end = Offset(this@drawBehind.size.width, this@drawBehind.size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Trans.get("subscription"),
                        color = if (activeTab == 1) RedPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (activeTab == 0) {
                // MANUAL CONFIG FORM
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = remarkName,
                        onValueChange = { remarkName = it },
                        label = { Text("Remark Name") },
                        placeholder = { Text("My Server") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Protocol Dropdown selector simulation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCard)
                            .border(1.dp, TextMuted, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Protocol", color = TextSecondary, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("VLESS", "VMess", "Trojan", "Shadowsocks").forEach { proto ->
                                val isSel = selectedProtocol == proto
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) RedPrimary else Color.Transparent)
                                        .clickable { selectedProtocol = proto }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(proto, color = if (isSel) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        placeholder = { Text("example.com or 192.168.1.1") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { Text("443") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uuidPassword,
                        onValueChange = { uuidPassword = it },
                        label = { Text("UUID / Password") },
                        placeholder = { Text("Enter UUID or password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // TLS Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TLS / Encryption", color = TextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = tlsEnabled,
                            onCheckedChange = { tlsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary)
                        )
                    }

                    if (tlsEnabled) {
                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text("SNI") },
                            placeholder = { Text("sni.redpillcloud.ru") },
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Allow Insecure Certificates", color = TextSecondary, fontSize = 13.sp)
                            Switch(
                                checked = allowInsecure,
                                onCheckedChange = { allowInsecure = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CyberButton(
                        text = Trans.get("add_server_btn"),
                        onClick = {
                            if (remarkName.isEmpty()) remarkName = "Manual Node"
                            RedShiftState.servers.add(
                                Server(
                                    id = "manual_" + System.currentTimeMillis(),
                                    flag = "⚙",
                                    name = remarkName,
                                    protocol = selectedProtocol,
                                    address = address.ifEmpty { "127.0.0.1" },
                                    port = port.toIntOrNull() ?: 443,
                                    latency = 35,
                                    usedTraffic = 0.0,
                                    totalTraffic = 10.0,
                                    isCustom = true
                                )
                            )
                            onAdded()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // SUBSCRIPTION FORM
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste RedPill Subscription URL",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = subscriptionUrl,
                        onValueChange = { subscriptionUrl = it },
                        placeholder = { Text("https://redpillcloud.ru/sub/...") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-detect protocol configurations", color = TextSecondary, fontSize = 13.sp)
                        Switch(
                            checked = autoDetect,
                            onCheckedChange = { autoDetect = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary)
                        )
                    }

                    // Update intervals chips
                    Text("Auto-update Interval", color = TextPrimary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("6h", "12h", "24h", "Manual").forEach { interval ->
                            val isSel = updateInterval == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) RedPrimary else CyberCard)
                                    .border(0.5.dp, if (isSel) RedPrimary else TextMuted, RoundedCornerShape(8.dp))
                                    .clickable { updateInterval = interval }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(interval, color = if (isSel) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CyberButton(
                        text = if (RedShiftState.isImporting) "Importing..." else "Import Subscription",
                        onClick = {
                            val url = subscriptionUrl.ifEmpty { "https://redpillcloud.ru/sub/rp_custom" }
                            RedShiftState.importSubscription(url)
                            onAdded()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !RedShiftState.isImporting
                    )

                    if (RedShiftState.importError != null) {
                        Text(
                            text = "Error: ${RedShiftState.importError}",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RedPrimary,
    unfocusedBorderColor = TextMuted,
    focusedLabelColor = RedPrimary,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = RedPrimary
)

// SCREEN 4: SUBSCRIPTIONS
@Composable
fun SubscriptionsScreen() {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Trans.get("tab_subscriptions"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = {
                        isRefreshing = true
                        val url = RedShiftState.subscriptionUrl
                        if (url.isNotEmpty()) {
                            RedShiftState.importSubscription(url)
                        }
                        isRefreshing = false
                        Toast.makeText(context, "Subscriptions Synchronized!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Update all", tint = RedPrimary)
                }
            }

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RedPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(RedShiftState.subscriptions) { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberCard),
                            border = BorderStroke(0.5.dp, TextMuted.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (sub.status == "OK") SuccessGreen else ErrorRed)
                                        )
                                        Text(
                                            text = sub.name,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            RedShiftState.importSubscription(sub.url)
                                            Toast.makeText(context, "Synchronizing nodes...", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Text(
                                    text = sub.url,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sub.serverCount} servers found",
                                        color = PurpleSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Expires: in ${sub.expiryDays} days",
                                        color = if (sub.expiryDays < 7) WarningAmber else SuccessGreen,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = "Last updated: " + sub.lastUpdated,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    item {
                        // Add Subscription outlined card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Transparent)
                                .border(
                                    BorderStroke(1.dp, Brush.sweepGradient(listOf(RedPrimary, PurpleSecondary))),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = RedPrimary)
                                Text("Add Subscription URL", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 5: ROUTING RULES
@Composable
fun RoutingRulesScreen(onAddRuleClick: () -> Unit) {
    var selectedMode by remember { mutableStateOf(RoutingMode.RULE) }
    var rulesCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Trans.get("tab_rules"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Mode Toggles Row (Global, Rule, Direct)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val modes = listOf<Triple<RoutingMode, ImageVector, String>>(
                Triple(RoutingMode.GLOBAL, Icons.Default.Language, "Global"),
                Triple(RoutingMode.RULE, Icons.Default.FilterAlt, "Rule"),
                Triple(RoutingMode.DIRECT, Icons.Default.ElectricBolt, "Direct")
            )
            modes.forEach { item ->
                val mode = item.first
                val icon = item.second
                val label = item.third
                val isActive = selectedMode == mode
                val accentColor = when (mode) {
                    RoutingMode.GLOBAL -> RedPrimary
                    RoutingMode.RULE -> PurpleSecondary
                    RoutingMode.DIRECT -> TextSecondary
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = mode }
                        .border(
                            1.5.dp,
                            if (isActive) accentColor else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) CyberElevated else CyberCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isActive) accentColor else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = if (isActive) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Rules List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Proxy Routing Rules", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onAddRuleClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = RedPrimary)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Predefined rulesets toggle card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rulesCollapsed = !rulesCollapsed },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Predefined Rule Sets", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = if (rulesCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = !rulesCollapsed) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                RuleSwitchRow(label = "Bypass Local Network Address", checked = RedShiftState.bypassLocal, onCheckedChange = { RedShiftState.bypassLocal = it })
                                RuleSwitchRow(label = "Bypass LAN IPs (192.168.0.0/16, etc.)", checked = RedShiftState.bypassLan, onCheckedChange = { RedShiftState.bypassLan = it })
                                RuleSwitchRow(label = "Bypass Russian Sites", checked = RedShiftState.bypassRussia, onCheckedChange = { RedShiftState.bypassRussia = it })
                                RuleSwitchRow(label = "Bypass China Sites", checked = RedShiftState.bypassChina, onCheckedChange = { RedShiftState.bypassChina = it })
                                RuleSwitchRow(label = "Block Ads & Tracker domains", checked = RedShiftState.blockAds, onCheckedChange = { RedShiftState.blockAds = it })
                            }
                        }
                    }
                }
            }

            items(RedShiftState.routingRules) { rule ->
                var isEnabled by remember { mutableStateOf(rule.isEnabled) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    border = BorderStroke(0.5.dp, TextMuted.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = TextMuted)
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rule.type,
                                    color = PurpleSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (rule.action == "Proxy") RedPrimary.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(rule.action, color = if (rule.action == "Proxy") RedPrimary else SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = rule.value, color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary)
        )
    }
}

// SCREEN 6: SETTINGS
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showLangSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Trans.get("tab_settings"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // General Section
        SettingsHeader(title = Trans.get("general_settings"))
        CyberCard {
            SettingsSwitchRow(
                icon = Icons.Outlined.PowerSettingsNew,
                title = "Start on Boot",
                description = "Automatically launch RedShift client on system start.",
                checked = RedShiftState.startOnBoot,
                onCheckedChange = { RedShiftState.startOnBoot = it }
            )
            HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), thickness = 0.5.dp)
            SettingsSwitchRow(
                icon = Icons.Outlined.Notifications,
                title = "VPN Notification",
                description = "Show dynamic status & speed controls in notification shade.",
                checked = RedShiftState.vpnNotification,
                onCheckedChange = { RedShiftState.vpnNotification = it }
            )
            HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), thickness = 0.5.dp)
            SettingsSwitchRow(
                icon = Icons.Outlined.Dangerous,
                title = Trans.get("kill_switch"),
                description = "Block all unproxied traffic in case connection drops unexpectedly.",
                checked = RedShiftState.killSwitch,
                onCheckedChange = { RedShiftState.killSwitch = it },
                accentColor = RedPrimary
            )
        }

        // Account / RedPill Login Section
        SettingsHeader(title = "RedPill Cloud Account")
        CyberCard {
            if (!RedShiftState.isLoggedIn) {
                var email by remember { mutableStateOf("") }
                var tgId by remember { mutableStateOf("") }
                var showManual by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sign in with Telegram",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open RedPill Cloud bot in Telegram and tap Start to auto-link your account.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    CyberButton(
                        text = "Open Telegram Bot",
                        onClick = {
                            try {
                                val tgIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/RedPillCloudBot"))
                                tgIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(tgIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Telegram not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = { showManual = !showManual },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Or enter Telegram ID manually", color = TextSecondary)
                    }

                    if (showManual) {
                        OutlinedTextField(
                            value = tgId,
                            onValueChange = { tgId = it },
                            placeholder = { Text("Telegram User ID") },
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        CyberButton(
                            text = if (RedShiftState.isLoadingUser) "Loading..." else "Connect",
                            onClick = {
                                val id = tgId.toIntOrNull()
                                if (id != null) {
                                    RedShiftState.login(id)
                                } else {
                                    Toast.makeText(context, "Enter a valid numeric Telegram ID", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !RedShiftState.isLoadingUser
                        )
                    }

                    if (RedShiftState.loginError != null) {
                        Text(
                            text = "Error: ${RedShiftState.loginError}",
                            color = ErrorRed,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                val user = RedShiftState.userInfo
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "User: @${user?.username ?: RedShiftState.telegramToken}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Plan: ${RedShiftState.subscriptionPlan}", color = TextPrimary, fontSize = 13.sp)
                            Text(text = "Expires: ${RedShiftState.subscriptionExpiry.take(10)}", color = SuccessGreen, fontSize = 12.sp)
                            if (user != null) {
                                Text(text = "Devices: ${user.deviceCount}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = {
                                RedShiftState.refreshUserData(RedShiftState.telegramToken.toIntOrNull() ?: 0)
                                Toast.makeText(context, "Refreshed!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary.copy(alpha = 0.15f), contentColor = PurpleSecondary),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("↻")
                        }
                        Button(
                            onClick = { RedShiftState.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary.copy(alpha = 0.15f), contentColor = RedPrimary)
                        ) {
                            Text("Log Out")
                        }
                    }
                }
            }
        }

        // Auto-refresh settings
        SettingsHeader(title = "Subscription Auto-Refresh")
        CyberCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSwitchRow(
                    icon = Icons.Default.Sync,
                    title = "Auto-refresh Subscription",
                    description = "Periodically fetch latest server config in background.",
                    checked = RedShiftState.autoRefresh,
                    onCheckedChange = { enabled ->
                        RedShiftState.setAutoRefreshEnabled(enabled, RedShiftState.autoRefreshInterval)
                    }
                )
                if (RedShiftState.autoRefresh) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interval", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 3, 6, 12, 24).forEach { h ->
                                val isSel = RedShiftState.autoRefreshInterval == h
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) RedPrimary else CyberElevated)
                                        .clickable { RedShiftState.setAutoRefreshEnabled(true, h) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${h}h", color = if (isSel) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (RedShiftState.lastRefreshTime > 0) {
                        val lastRefresh = java.text.SimpleDateFormat("MMM dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(RedShiftState.lastRefreshTime))
                        Text("Last refresh: $lastRefresh", color = TextMuted, fontSize = 11.sp)
                    }
                    if (RedShiftState.cachedServerCount > 0) {
                        Text("Cached servers: ${RedShiftState.cachedServerCount}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Import Subscription Section
        SettingsHeader(title = "Import Subscription")
        CyberCard {
            var subUrl by remember { mutableStateOf(RedShiftState.subscriptionUrl) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subUrl,
                    onValueChange = { subUrl = it },
                    placeholder = { Text("https://...") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                CyberButton(
                    text = if (RedShiftState.isImporting) "Importing..." else "Import",
                    onClick = { RedShiftState.importSubscription(subUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = subUrl.isNotBlank() && !RedShiftState.isImporting
                )
                if (RedShiftState.importError != null) {
                    Text(text = "Error: ${RedShiftState.importError}", color = ErrorRed, fontSize = 12.sp)
                }
                if (RedShiftState.servers.isNotEmpty()) {
                    Text("${RedShiftState.servers.size} servers loaded", color = SuccessGreen, fontSize = 12.sp)
                }
            }
        }

        var showAdvanced by remember { mutableStateOf(false) }
        TextButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (showAdvanced) "▲ Hide Advanced Settings" else "▼ Advanced Settings",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        AnimatedVisibility(visible = showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // DNS & Port configs
                SettingsHeader(title = "Connection Engine Settings")
                CyberCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Local Socks Port", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Current port used for browser proxification.", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(
                            text = RedShiftState.localPort.toString(),
                            color = RedPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), thickness = 0.5.dp)
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Lan,
                        title = "Allow LAN Connections",
                        description = "Share proxy with other devices in local wifi.",
                        checked = RedShiftState.allowLan,
                        onCheckedChange = { RedShiftState.allowLan = it }
                    )
                }

                // Language settings
                SettingsHeader(title = "Appearance & Language")
                CyberCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLangSelector = !showLangSelector }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Translate, contentDescription = null, tint = RedPrimary)
                            Column {
                                Text(text = Trans.get("language"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "App language overrides system locale.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = LocalizationState.currentLanguage.nativeName,
                            color = RedPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(visible = showLangSelector) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                val isSelected = LocalizationState.currentLanguage == lang
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LocalizationState.currentLanguage = lang
                                            showLangSelector = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = lang.nativeName, color = if (isSelected) RedPrimary else TextPrimary, fontSize = 13.sp)
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = RedPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.1f), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                // Admin API Config
                SettingsHeader(title = "Admin API Configuration")
                CyberCard {
                    var tempAdminToken by remember { mutableStateOf(RedShiftState.apiAdminToken) }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = tempAdminToken,
                            onValueChange = { tempAdminToken = it },
                            placeholder = { Text("Enter admin API token...") },
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        CyberButton(
                            text = "Set Admin Token",
                            onClick = {
                                RedShiftState.apiAdminToken = tempAdminToken
                                Toast.makeText(context, "Admin token updated", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // About section
                SettingsHeader(title = "System Info & Licenses")
                CyberCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(RedPrimary, RedGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("RS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("RedShift VPN Client", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Version 1.0.0 • Powered by RedPill Cloud", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = RedPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor)
            Column {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = description, color = TextSecondary, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary)
        )
    }
}

// ADMIN DASHBOARD SCREEN
@Composable
fun AdminDashboardScreen() {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var users by remember { mutableStateOf<org.json.JSONArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var grantUserId by remember { mutableStateOf("") }
    var grantTariff by remember { mutableStateOf("pro_1m") }
    var grantDays by remember { mutableStateOf("30") }
    var actionResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val client = RedPillApiClient(
            baseUrl = RedShiftState.apiBaseUrl,
            adminToken = RedShiftState.apiAdminToken
        )
        stats = client.adminStats()
        users = client.adminListUsers()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ADMIN PANEL", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarningAmber,
            fontFamily = FontFamily.Monospace)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }
        } else {
            // Stats cards
            stats?.let { s ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Users", s.totalUsers.toString(), RedPrimary, Modifier.weight(1f))
                    StatCard("Active", s.activeSubscriptions.toString(), SuccessGreen, Modifier.weight(1f))
                    StatCard("Devices", s.totalDevices.toString(), PurpleSecondary, Modifier.weight(1f))
                }
            }

            // Grant access
            CyberCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Grant Access", color = TextPrimary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = grantUserId, onValueChange = { grantUserId = it },
                        placeholder = { Text("User ID") }, colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("pro_1m", "pro_12m", "ru_1m").forEach { t ->
                            val isSel = grantTariff == t
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) RedPrimary else CyberElevated)
                                .clickable { grantTariff = t }.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center) {
                                Text(t, color = if (isSel) Color.White else TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                    OutlinedTextField(value = grantDays, onValueChange = { grantDays = it },
                        placeholder = { Text("Days") }, colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth())
                    CyberButton(text = "Grant Access", onClick = {
                        coroutineScope.launch {
                            val client = RedPillApiClient(adminToken = RedShiftState.apiAdminToken)
                            val uid = grantUserId.toIntOrNull() ?: return@launch
                            val res = client.adminGrant(uid, grantTariff, grantDays.toIntOrNull() ?: 30)
                            actionResult = if (res != null) "OK: ${res.optJSONObject("result")?.optInt("subscription_id")}"
                            else "Failed"
                        }
                    }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Users list
            if (users != null) {
                Text("Recent Users (max 100)", color = TextPrimary, fontWeight = FontWeight.Bold)
                for (i in 0 until users!!.length()) {
                    val u = users!!.getJSONObject(i)
                    Card(colors = CardDefaults.cardColors(containerColor = CyberCard),
                        border = BorderStroke(0.5.dp, TextMuted.copy(alpha = 0.3f))) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("@${u.optString("username", "?")}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ID: ${u.getInt("user_id")}", color = TextSecondary, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(u.optString("tariff", "none"), color = if (u.has("tariff")) SuccessGreen else TextMuted, fontSize = 11.sp)
                                Text("Dev: ${u.optInt("device_count", 0)}", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            actionResult?.let {
                Text(it, color = if (it.startsWith("OK")) SuccessGreen else ErrorRed, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
        modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(label.uppercase(), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Custom Dialog to Add Rule
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onRuleAdded: (RoutingRule) -> Unit
) {
    var type by remember { mutableStateOf("Domain") }
    var value by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("Proxy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Routing Rule", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rule type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Domain", "IP CIDR", "GeoIP").forEach { t ->
                        val isSel = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) RedPrimary else CyberCard)
                                .clickable { type = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, color = if (isSel) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    placeholder = { Text(if (type == "Domain") "google.com" else if (type == "GeoIP") "RU" else "10.0.0.0/8") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Action selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Proxy", "Direct", "Block").forEach { act ->
                        val isSel = action == act
                        val btnColor = if (act == "Proxy") RedPrimary else if (act == "Direct") SuccessGreen else TextMuted
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) btnColor else CyberCard)
                                .clickable { action = act }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(act, color = if (isSel) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotEmpty()) {
                        onRuleAdded(RoutingRule("custom_" + System.currentTimeMillis(), type, value, action))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = Color.White)
            ) {
                Text("Add Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CyberElevated
    )
}
