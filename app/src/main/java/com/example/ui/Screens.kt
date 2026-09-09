package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
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
                .background(MainBackgroundBrush)
        ) {
            if (!RedShiftState.isOnboarded) {
                OnboardingScreenPremium(onFinished = { RedShiftState.isOnboarded = true })
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
                                "dashboard" -> HomeScreen(
                                    onAddServerClick = { showAddServerSheet = true },
                                    onOpenServers = { currentTab = "servers" }
                                )
                                "servers" -> ServersScreen(
                                    onOpenHome = { currentTab = "dashboard" },
                                    onOpenSettings = { currentTab = "settings" }
                                )
                                "settings" -> SettingsScreenPremium()
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

@Composable
fun HomeScreen(
    onAddServerClick: () -> Unit,
    onOpenServers: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackgroundBrush)
    ) {
        HomeScreenContent(
            onAddServerClick = onAddServerClick,
            onOpenServers = onOpenServers
        )
    }
}

@Composable
fun PingBars(latency: Int) {
    val color = when {
        latency <= 50 -> SuccessGreen
        latency <= 150 -> AmberWarning
        else -> ErrorRed
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(4.dp, 7.dp, 10.dp, 13.dp).forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color.copy(alpha = 0.8f))
            )
        }
    }
    Spacer(modifier = Modifier.width(6.dp))
}

fun speedStr(kbps: Double): String {
    return if (kbps >= 1024) {
        String.format("%.2f MB/s", kbps / 1024)
    } else {
        String.format("%.1f KB/s", kbps)
    }
}

@Composable
fun CyberBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                20.dp,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = VpnColors.accentGreen.copy(alpha = 0.12f)
            )
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = VpnColors.SurfaceNav.copy(alpha = 0.75f)
    ) {
        Column {
            HorizontalDivider(
                color = VpnColors.accentGreen.copy(alpha = 0.35f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
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
                    onClick = { onTabSelected("add_server") }
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
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) VpnColors.accentGreen else VpnColors.TextSecondary.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    VpnColors.accentGreen.copy(alpha = 0.28f),
                                    VpnColors.accentGreen.copy(alpha = 0.10f)
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .border(
                    1.dp,
                    if (isSelected) VpnColors.accentGreen.copy(alpha = 0.45f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun ServersScreen(
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val loggedIn = RedShiftState.isLoggedIn || RedShiftState.subscriptionUrl.isNotBlank()
    val context = LocalContext.current

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            RedShiftState.toggleVpn()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            RedShiftState.toggleVpn()
        }
    }

    val onConnect: () -> Unit = {
        val isCurrentlyConnected = RedShiftState.connectionState == ConnectionState.CONNECTED ||
                RedShiftState.connectionState == ConnectionState.CONNECTING
        if (isCurrentlyConnected) {
            RedShiftState.toggleVpn()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val perm = android.Manifest.permission.POST_NOTIFICATIONS
                val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    val intent = VpnService.prepare(context)
                    if (intent != null) vpnPermissionLauncher.launch(intent)
                    else RedShiftState.toggleVpn()
                } else {
                    notifPermissionLauncher.launch(perm)
                }
            } else {
                val intent = VpnService.prepare(context)
                if (intent != null) vpnPermissionLauncher.launch(intent)
                else RedShiftState.toggleVpn()
            }
        }
    }

    if (!loggedIn && RedShiftState.servers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Text("Import a subscription to see servers", color = TextSecondary, fontSize = 14.sp)
                Text("Go to Settings → Import Subscription", color = TextMuted, fontSize = 12.sp)
            }
        }
        return
    }

    ServersScreenPremium(
        servers = RedShiftState.servers,
        onBackClick = onOpenHome,
        onSettingsClick = onOpenSettings,
        onServerSelect = { server -> RedShiftState.selectedServerId = server.id },
        onToggleConnection = onConnect
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerModalSheet(
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }

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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = remarkName,
                        onValueChange = { remarkName = it },
                        label = { Text("Remark Name") },
                        placeholder = { Text("My Server") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste Subscription URL",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Supports Happ/RedPill links, vless://, hy2://, vmess://, trojan://, ss:// and Amnezia keys (vpn://)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = subscriptionUrl,
                            onValueChange = { subscriptionUrl = it },
                            placeholder = { Text("https://... or vpn://config") },
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            subscriptionUrl = clip.getItemAt(0).text.toString().trim()
                                        }
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RedPrimary.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = RedPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

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

