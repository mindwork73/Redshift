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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(BackgroundGraphite)
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
        latency <= 50 -> AccentNeonGreen
        latency <= 150 -> AccentWarning
        else -> AccentError
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
                24.dp,
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = AccentNeonGreen.copy(alpha = 0.06f)
            )
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xDD0F1117)
    ) {
        Column {
            HorizontalDivider(
                color = BorderGraphite.copy(alpha = 0.6f),
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
    val tint = if (isSelected) AccentNeonGreen else TextMuted.copy(alpha = 0.7f)

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
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) AccentNeonGreen.copy(alpha = 0.1f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
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
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundGraphite),
            contentAlignment = Alignment.Center
        ) {
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
    var isManualMode by remember { mutableStateOf(false) }
    var remarkName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var selectedProtocol by remember { mutableStateOf("VLESS") }
    var credential by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var tlsEnabled by remember { mutableStateOf(true) }
    var sni by remember { mutableStateOf("") }
    var allowInsecure by remember { mutableStateOf(false) }
    var subscriptionUrl by remember { mutableStateOf("") }
    var autoDetect by remember { mutableStateOf(true) }
    var updateInterval by remember { mutableStateOf("6h") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundGraphiteMid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(BorderGraphite)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Trans.get("tab_add_server"),
                    style = VpnTypography.statusMain.copy(fontSize = 22.sp, color = TextPrimary)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceInner)
                        .border(1.dp, BorderGraphite, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceInner)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Subscription" to false, "Manual" to true).forEach { (label, mode) ->
                    val isSel = isManualMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSel) Brush.horizontalGradient(VpnColors.premiumGradient)
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { isManualMode = mode }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isManualMode) {
                // Manual server entry
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = remarkName,
                        onValueChange = { remarkName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("My Private Node") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("VLESS", "Trojan", "VMess", "SS").forEach { p ->
                            val isSel = selectedProtocol == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) AccentNeonGreen.copy(alpha = 0.15f) else SurfaceInner)
                                    .border(1.dp, if (isSel) AccentNeonGreen else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedProtocol = p }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p, color = if (isSel) AccentNeonGreen else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        placeholder = { Text("example.com or 1.2.3.4") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = credential,
                        onValueChange = { credential = it },
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
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundGraphite,
                                checkedTrackColor = AccentNeonGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceInner,
                                uncheckedBorderColor = BorderGraphite
                            )
                        )
                    }

                    if (tlsEnabled) {
                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text("SNI") },
                            placeholder = { Text("sni.example.com") },
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BackgroundGraphite,
                                    checkedTrackColor = AccentWarning,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceInner,
                                    uncheckedBorderColor = BorderGraphite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(Brush.horizontalGradient(VpnColors.premiumGradient))
                            .clickable {
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
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Trans.get("add_server_btn"),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Subscription import
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentNeonGreen.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = AccentNeonGreen, modifier = Modifier.size(20.dp))
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
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundGraphite,
                                checkedTrackColor = AccentNeonGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceInner,
                                uncheckedBorderColor = BorderGraphite
                            )
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
                                    .background(if (isSel) AccentNeonGreen.copy(alpha = 0.15f) else SurfaceInner)
                                    .border(0.5.dp, if (isSel) AccentNeonGreen.copy(alpha = 0.4f) else BorderGraphite, RoundedCornerShape(8.dp))
                                    .clickable { updateInterval = interval }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(interval, color = if (isSel) AccentNeonGreen else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (!RedShiftState.isImporting) Brush.horizontalGradient(VpnColors.premiumGradient)
                                else Brush.horizontalGradient(listOf(TextMuted.copy(alpha = 0.5f), TextMuted.copy(alpha = 0.5f)))
                            )
                            .clickable(enabled = !RedShiftState.isImporting) {
                                val url = subscriptionUrl.ifEmpty { "https://redpillcloud.ru/sub/rp_custom" }
                                RedShiftState.importSubscription(url)
                                onAdded()
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (RedShiftState.isImporting) "Importing..." else "Import Subscription",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (RedShiftState.importError != null) {
                        Text(
                            text = "Error: ${RedShiftState.importError}",
                            color = AccentError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentNeonGreen,
    unfocusedBorderColor = BorderGraphite,
    focusedLabelColor = AccentNeonGreen,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentNeonGreen,
    focusedContainerColor = SurfaceInner,
    unfocusedContainerColor = SurfaceInner
)
