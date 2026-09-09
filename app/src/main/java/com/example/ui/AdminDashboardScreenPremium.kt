@file:JvmName("AdminDashboardScreenPremiumKt")

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AdminStats
import com.example.service.RedPillApiClient
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnTypography
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreenPremium() {
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
            .background(VpnColors.Background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ─── Header ───
        Text(
            text = Trans.get("admin_panel"),
            style = VpnTypography.statusMain.copy(
                fontSize = 24.sp,
                color = VpnColors.accentWarning,
                fontFamily = FontFamily.Monospace
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VpnColors.accentWarning)
            }
        } else {
            // ─── Stats Row ───
            stats?.let { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumStatCard(
                        label = Trans.get("users"),
                        value = s.totalUsers.toString(),
                        icon = Icons.Default.Group,
                        accentColor = VpnColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumStatCard(
                        label = Trans.get("active"),
                        value = s.activeSubscriptions.toString(),
                        icon = Icons.Default.Security,
                        accentColor = VpnColors.accentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumStatCard(
                        label = Trans.get("devices"),
                        value = s.totalDevices.toString(),
                        icon = Icons.Default.Devices,
                        accentColor = VpnColors.accentWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─── Grant Access Card ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VpnColors.surfaceGlass)
                    .border(1.dp, VpnColors.borderLight, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = Trans.get("grant_access"),
                        style = VpnTypography.cardTitle
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumOutlinedField(
                            value = grantUserId,
                            onValueChange = { grantUserId = it },
                            placeholder = Trans.get("user_id"),
                            modifier = Modifier.weight(1f)
                        )
                        PremiumOutlinedField(
                            value = grantDays,
                            onValueChange = { grantDays = it },
                            placeholder = Trans.get("days"),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("pro_1m", "pro_12m", "ru_1m").forEach { t ->
                            val isSel = grantTariff == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) VpnColors.surfaceCardSolid else VpnColors.surfaceInner)
                                    .border(1.dp, if (isSel) VpnColors.TextPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { grantTariff = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    style = VpnTypography.buttonText.copy(
                                        fontSize = 11.sp,
                                        color = if (isSel) VpnColors.TextPrimary else VpnColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(Brush.horizontalGradient(VpnColors.premiumGradient))
                            .clickable {
                                coroutineScope.launch {
                                    val client = RedPillApiClient(adminToken = RedShiftState.apiAdminToken)
                                    val uid = grantUserId.toIntOrNull() ?: return@launch
                                    val res = client.adminGrant(uid, grantTariff, grantDays.toIntOrNull() ?: 30)
                                    actionResult = if (res != null) "OK: ${res.optJSONObject("result")?.optInt("subscription_id")}"
                                    else "Failed"
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = Trans.get("grant_access"), style = VpnTypography.buttonText)
                    }

                    actionResult?.let {
                        Text(
                            text = it,
                            style = VpnTypography.cardSubtitle.copy(
                                color = if (it.startsWith("OK")) VpnColors.accentGreen else VpnColors.accentError
                            )
                        )
                    }
                }
            }

            // ─── Recent Users List ───
            if (users != null) {
                Text(
                    text = Trans.get("recent_users"),
                    style = VpnTypography.cardTitle,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in 0 until users!!.length()) {
                        val u = users!!.getJSONObject(i)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(VpnColors.surfaceInner)
                                .border(1.dp, VpnColors.borderLight, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "@${u.optString("username", "?")}",
                                        style = VpnTypography.cardTitle.copy(fontSize = 15.sp)
                                    )
                                    Text(
                                        text = "ID: ${u.getInt("user_id")}",
                                        style = VpnTypography.cardSubtitle
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val hasTariff = u.has("tariff")
                                    Text(
                                        text = u.optString("tariff", Trans.get("none")),
                                        style = VpnTypography.buttonText.copy(
                                            fontSize = 12.sp,
                                            color = if (hasTariff) VpnColors.accentGreen else VpnColors.TextSecondary
                                        )
                                    )
                                    Text(
                                        text = "${Trans.get("dev")}: ${u.optInt("device_count", 0)}",
                                        style = VpnTypography.cardSubtitle
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── UI Helpers ───

@Composable
private fun PremiumStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VpnColors.surfaceGlass)
            .border(1.dp, VpnColors.borderLight, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Text(
                text = value,
                style = VpnTypography.statusMain.copy(fontSize = 20.sp, color = accentColor, fontFamily = FontFamily.Monospace)
            )
            Text(
                text = label.uppercase(),
                style = VpnTypography.statsLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = VpnColors.borderLight) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VpnColors.TextPrimary,
            unfocusedBorderColor = VpnColors.borderLight,
            focusedTextColor = VpnColors.TextPrimary,
            unfocusedTextColor = VpnColors.TextPrimary,
            cursorColor = VpnColors.accentGreen,
            focusedContainerColor = VpnColors.surfaceInner,
            unfocusedContainerColor = VpnColors.surfaceInner
        ),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}