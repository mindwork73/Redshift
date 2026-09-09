@file:JvmName("SettingsScreenPremiumKt")

package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnTypography

private enum class SettingsRoute { SUBSCRIPTIONS, ROUTING, ADMIN }

@Composable
fun SettingsScreenPremium() {
    var route by remember { mutableStateOf<SettingsRoute?>(null) }

    if (route == null) {
        SettingsMainScreen { route = it }
    } else {
        val title = when (route) {
            SettingsRoute.SUBSCRIPTIONS -> Trans.get("tab_subscriptions")
            SettingsRoute.ROUTING -> Trans.get("tab_rules")
            SettingsRoute.ADMIN -> Trans.get("admin_panel")
            null -> Trans.get("tab_settings")
        }
        SettingsPushedScreen(
            title = title,
            onBack = { route = null }
        ) {
            when (route) {
                SettingsRoute.SUBSCRIPTIONS -> SubscriptionsScreenPremium()
                SettingsRoute.ROUTING -> RoutingRulesScreenPremium()
                SettingsRoute.ADMIN -> AdminDashboardScreenPremium()
                null -> {}
            }
        }
    }
}

@Composable
private fun SettingsPushedScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VpnColors.surfaceInner)
                    .border(1.dp, VpnColors.borderLight, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = Trans.get("back"),
                    tint = VpnColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = VpnTypography.statusMain.copy(fontSize = 20.sp, color = VpnColors.TextPrimary)
            )
        }
        content()
    }
}

@Composable
private fun SettingsMainScreen(onOpen: (SettingsRoute) -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showAdvanced by remember { mutableStateOf(false) }
    val isAdmin = RedShiftState.apiAdminToken.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnColors.Background)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Trans.get("tab_settings"),
            style = VpnTypography.statusMain.copy(fontSize = 24.sp, color = VpnColors.TextPrimary)
        )

        // ─── General ───
        SettingsSectionHeader(title = Trans.get("general_settings"))
        SettingsGlassCard {
            SettingsToggleRow(
                icon = Icons.Outlined.PowerSettingsNew,
                title = "Start on Boot",
                description = "Automatically launch RedShift client on system start.",
                checked = RedShiftState.startOnBoot,
                onCheckedChange = { RedShiftState.startOnBoot = it }
            )
            SettingsSectionDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.Notifications,
                title = "VPN Notification",
                description = "Show dynamic status & speed controls in notification shade.",
                checked = RedShiftState.vpnNotification,
                onCheckedChange = { RedShiftState.vpnNotification = it }
            )
            SettingsSectionDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.Dangerous,
                title = Trans.get("kill_switch"),
                description = "Block all unproxied traffic in case connection drops unexpectedly.",
                checked = RedShiftState.killSwitch,
                onCheckedChange = { RedShiftState.killSwitch = it },
                accentColor = VpnColors.accentError
            )
            SettingsSectionDivider()
            SettingsLanguageRow()
        }

        // ─── Account ───
        SettingsSectionHeader(title = "RedPill Cloud Account")
        SettingsGlassCard {
            if (!RedShiftState.isLoggedIn) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "No account linked", style = VpnTypography.cardTitle)
                    Text(
                        text = "Import your RedPill Cloud subscription link below to link your account automatically.",
                        style = VpnTypography.cardSubtitle
                    )
                }
            } else {
                val user = RedShiftState.userInfo
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "User ID: ${if (RedShiftState.telegramToken.toIntOrNull() != null) RedShiftState.telegramToken else "—"}", style = VpnTypography.cardTitle)
                        Text(text = "Plan: ${RedShiftState.subscriptionPlan}", style = VpnTypography.cardSubtitle.copy(color = VpnColors.TextPrimary))
                        Text(text = "Expires: ${premiumExpiryDisplay(RedShiftState.subscriptionExpiry)}", style = VpnTypography.cardSubtitle.copy(color = VpnColors.accentGreen))
                        if (user != null) {
                            Text(text = "Devices: ${user.deviceCount}", style = VpnTypography.cardSubtitle)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                RedShiftState.refreshUserData(RedShiftState.telegramToken.toIntOrNull() ?: 0)
                                Toast.makeText(context, "Refreshed!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VpnColors.surfaceInner, contentColor = VpnColors.TextPrimary),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("↻", fontSize = 16.sp)
                        }
                        Button(
                            onClick = { RedShiftState.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = VpnColors.accentError.copy(alpha = 0.15f), contentColor = VpnColors.accentError),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Log Out", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ─── Import Subscription ───
        SettingsSectionHeader(title = "Import Subscription")
        SettingsGlassCard {
            var subUrl by remember { mutableStateOf(RedShiftState.subscriptionUrl) }
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subUrl,
                    onValueChange = { subUrl = it },
                    placeholder = { Text("https://redpillcloud.ru/sub/...", color = VpnColors.TextSecondary) },
                    colors = premiumTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (subUrl.isNotBlank() && !RedShiftState.isImporting) Brush.horizontalGradient(VpnColors.premiumGradient)
                            else Brush.horizontalGradient(listOf(VpnColors.surfaceInner, VpnColors.surfaceInner))
                        )
                        .clickable(enabled = subUrl.isNotBlank() && !RedShiftState.isImporting) {
                            RedShiftState.importSubscription(subUrl)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (RedShiftState.isImporting) "Importing..." else "Import",
                        style = VpnTypography.buttonText,
                        color = if (subUrl.isNotBlank() && !RedShiftState.isImporting) VpnColors.TextPrimary else VpnColors.TextSecondary
                    )
                }

                if (RedShiftState.importError != null) {
                    Text(text = "Error: ${RedShiftState.importError}", style = VpnTypography.cardSubtitle.copy(color = VpnColors.accentError))
                }
                if (RedShiftState.servers.isNotEmpty()) {
                    Text("${RedShiftState.servers.size} servers loaded", style = VpnTypography.cardSubtitle.copy(color = VpnColors.accentGreen))
                }
            }
        }

        // ─── Subscription Auto-Refresh ───
        SettingsSectionHeader(title = "Subscription Auto-Refresh")
        SettingsGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsToggleRow(
                    icon = Icons.Default.Sync,
                    title = "Auto-refresh",
                    description = "Periodically fetch latest server config in background.",
                    checked = RedShiftState.autoRefresh,
                    onCheckedChange = { enabled ->
                        RedShiftState.setAutoRefreshEnabled(enabled, RedShiftState.autoRefreshInterval)
                    }
                )
                if (RedShiftState.autoRefresh) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interval", style = VpnTypography.cardTitle)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 6, 12, 24).forEach { h ->
                                val isSel = RedShiftState.autoRefreshInterval == h
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) VpnColors.surfaceInner else Color.Transparent)
                                        .border(1.dp, if (isSel) VpnColors.borderLight else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { RedShiftState.setAutoRefreshEnabled(true, h) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${h}h", color = if (isSel) VpnColors.TextPrimary else VpnColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── Management (Subscriptions / Routing / Admin) ───
        SettingsSectionHeader(title = Trans.get("management"))
        SettingsGlassCard {
            SettingsNavRow(
                icon = Icons.Outlined.Cloud,
                title = Trans.get("tab_subscriptions"),
                subtitle = "Manage imported subscription links.",
                onClick = { onOpen(SettingsRoute.SUBSCRIPTIONS) }
            )
            SettingsSectionDivider()
            SettingsNavRow(
                icon = Icons.Outlined.Rule,
                title = Trans.get("tab_rules"),
                subtitle = "Split tunneling, bypass & custom rules.",
                onClick = { onOpen(SettingsRoute.ROUTING) }
            )
            if (isAdmin) {
                SettingsSectionDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = Trans.get("admin_panel"),
                    subtitle = "Manage users, grants & plans.",
                    onClick = { onOpen(SettingsRoute.ADMIN) }
                )
            }
        }

        // ─── Advanced ───
        TextButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (showAdvanced) "▲ Hide Advanced Settings" else "▼ Advanced Settings",
                color = VpnColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        AnimatedVisibility(visible = showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsSectionHeader(title = "Connection Engine Settings")
                SettingsGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local Socks Port", style = VpnTypography.cardTitle)
                            Text("Current port used for browser proxification.", style = VpnTypography.cardSubtitle)
                        }
                        Text(
                            text = RedShiftState.localPort.toString(),
                            style = VpnTypography.statusMain.copy(color = VpnColors.accentWarning)
                        )
                    }
                    SettingsSectionDivider()
                    SettingsToggleRow(
                        icon = Icons.Outlined.Lan,
                        title = "Allow LAN Connections",
                        description = "Share proxy with other devices in local wifi.",
                        checked = RedShiftState.allowLan,
                        onCheckedChange = { RedShiftState.allowLan = it }
                    )
                }

                SettingsSectionHeader(title = "System Info")
                SettingsGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(VpnColors.premiumGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("RS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("RedShift VPN Client", style = VpnTypography.cardTitle)
                            Text("Version 1.0.0 • Powered by RedPill Cloud", style = VpnTypography.cardSubtitle)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Вспомогательные компоненты для нового дизайна ───

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = VpnTypography.statsLabel.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VpnColors.surfaceGlass)
            .border(1.dp, VpnColors.borderLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSectionDivider() {
    HorizontalDivider(
        color = VpnColors.borderLight,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = VpnColors.TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VpnColors.surfaceInner)
                    .border(1.dp, VpnColors.borderLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = title, style = VpnTypography.cardTitle)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = VpnTypography.cardSubtitle)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VpnColors.Background,
                checkedTrackColor = VpnColors.accentGreen,
                uncheckedThumbColor = VpnColors.TextSecondary,
                uncheckedTrackColor = VpnColors.surfaceInner,
                uncheckedBorderColor = VpnColors.borderLight
            )
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VpnColors.surfaceInner)
                    .border(1.dp, VpnColors.borderLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = VpnColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = title, style = VpnTypography.cardTitle)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = VpnTypography.cardSubtitle)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = VpnColors.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsLanguageRow() {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VpnColors.surfaceInner)
                        .border(1.dp, VpnColors.borderLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Translate, contentDescription = null, tint = VpnColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
                Text(text = Trans.get("language"), style = VpnTypography.cardTitle)
            }
            Text(
                text = LocalizationState.currentLanguage.nativeName,
                style = VpnTypography.cardTitle.copy(color = VpnColors.accentGreen)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().height(240.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppLanguage.values().forEach { lang ->
                    val isSelected = LocalizationState.currentLanguage == lang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) VpnColors.surfaceInner else Color.Transparent)
                            .clickable {
                                RedShiftState.setLanguage(lang)
                                expanded = false
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = lang.nativeName, style = VpnTypography.cardTitle.copy(color = if (isSelected) VpnColors.accentGreen else VpnColors.TextPrimary))
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = VpnColors.accentGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VpnColors.TextPrimary,
    unfocusedBorderColor = VpnColors.borderLight,
    focusedTextColor = VpnColors.TextPrimary,
    unfocusedTextColor = VpnColors.TextPrimary,
    cursorColor = VpnColors.accentGreen,
    focusedContainerColor = VpnColors.surfaceInner,
    unfocusedContainerColor = VpnColors.surfaceInner
)

private fun premiumExpiryDisplay(raw: String): String {
    if (raw.isBlank() || raw == "N/A") return "—"
    return try {
        val date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).parse(raw) ?: return raw
        val locale = if (LocalizationState.currentLanguage == AppLanguage.RU) java.util.Locale.forLanguageTag("ru-RU") else java.util.Locale.US
        java.text.SimpleDateFormat("dd MMM yyyy", locale).format(date)
    } catch (_: Exception) {
        raw
    }
}