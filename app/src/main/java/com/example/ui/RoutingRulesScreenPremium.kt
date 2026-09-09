@file:JvmName("RoutingRulesScreenPremiumKt")

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun RoutingRulesScreenPremium() {
    var rulesCollapsed by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGraphite)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Trans.get("tab_rules"),
            style = VpnTypography.statusMain.copy(fontSize = 24.sp, color = VpnColors.TextPrimary),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeSelectorCard(
                mode = RoutingMode.GLOBAL,
                icon = Icons.Default.Language,
                label = "Global",
                modifier = Modifier.weight(1f)
            )
            ModeSelectorCard(
                mode = RoutingMode.RULE,
                icon = Icons.Default.FilterAlt,
                label = "Rule",
                modifier = Modifier.weight(1f)
            )
            ModeSelectorCard(
                mode = RoutingMode.DIRECT,
                icon = Icons.Default.ElectricBolt,
                label = "Direct",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Proxy Routing Rules",
                style = VpnTypography.statusMain.copy(color = VpnColors.TextPrimary)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(VpnColors.premiumGradient))
                    .clickable { showAddRuleDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceGlass.copy(alpha = 0.85f))
                        .border(1.dp, BorderGraphite, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rulesCollapsed = !rulesCollapsed },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Predefined Rule Sets", style = VpnTypography.cardTitle)
                            Icon(
                                imageVector = if (rulesCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = null,
                                tint = VpnColors.TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = !rulesCollapsed) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                RuleSwitchRow(label = "Bypass Local Network Address", checked = RedShiftState.bypassLocal, onCheckedChange = { RedShiftState.bypassLocal = it })
                                RuleSwitchRow(label = "Bypass LAN IPs (192.168.x.x)", checked = RedShiftState.bypassLan, onCheckedChange = { RedShiftState.bypassLan = it })
                                RuleSwitchRow(label = "Bypass Russian Sites", checked = RedShiftState.bypassRussia, onCheckedChange = { RedShiftState.bypassRussia = it })
                                RuleSwitchRow(label = "Bypass China Sites", checked = RedShiftState.bypassChina, onCheckedChange = { RedShiftState.bypassChina = it })
                                RuleSwitchRow(label = "Block Ads & Trackers", checked = RedShiftState.blockAds, onCheckedChange = { RedShiftState.blockAds = it })
                            }
                        }
                    }
                }
            }

            items(RedShiftState.routingRules) { rule ->
                CustomRuleCard(rule = rule)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onRuleAdded = { newRule ->
                RedShiftState.routingRules.add(newRule)
                showAddRuleDialog = false
            }
        )
    }
}

@Composable
private fun ModeSelectorCard(
    mode: RoutingMode,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val isActive = RedShiftState.routingMode == mode
    val activeBrush = Brush.horizontalGradient(VpnColors.premiumGradient)
    val inactiveBrush = Brush.horizontalGradient(listOf(SurfaceInner, SurfaceInner))

    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) activeBrush else inactiveBrush)
            .border(
                width = 1.dp,
                color = if (isActive) Color.Transparent else BorderGraphite,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { RedShiftState.routingMode = mode },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else VpnColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = VpnTypography.buttonText.copy(
                    fontSize = 13.sp,
                    color = if (isActive) Color.White else VpnColors.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun RuleSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = VpnTypography.cardSubtitle.copy(color = VpnColors.TextPrimary, fontSize = 13.sp),
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundGraphite,
                checkedTrackColor = AccentNeonGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceInner,
                uncheckedBorderColor = BorderGraphite
            )
        )
    }
}

@Composable
private fun CustomRuleCard(rule: RoutingRule) {
    var isEnabled by remember { mutableStateOf(rule.isEnabled) }

    val actionColor = when (rule.action) {
        "Proxy" -> AccentWarning
        "Direct" -> AccentNeonGreen
        else -> AccentError
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceInner)
            .border(1.dp, BorderGraphite, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = BorderGraphite,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rule.type,
                        style = VpnTypography.statsLabel.copy(
                            color = VpnColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    NeonPill(text = rule.action, color = actionColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rule.value,
                    style = VpnTypography.cardTitle.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundGraphite,
                    checkedTrackColor = AccentNeonGreen,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = BackgroundGraphite,
                    uncheckedBorderColor = BorderGraphite
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onRuleAdded: (RoutingRule) -> Unit
) {
    var type by remember { mutableStateOf("Domain") }
    var value by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("Proxy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundGraphiteMid,
        titleContentColor = VpnColors.TextPrimary,
        textContentColor = VpnColors.TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Add Routing Rule", style = VpnTypography.statusMain.copy(fontSize = 20.sp))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Domain", "IP CIDR", "GeoIP").forEach { t ->
                        val isSel = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AccentNeonGreen.copy(alpha = 0.12f) else SurfaceInner)
                                .border(1.dp, if (isSel) AccentNeonGreen.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { type = t }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                style = VpnTypography.buttonText.copy(
                                    fontSize = 11.sp,
                                    color = if (isSel) AccentNeonGreen else VpnColors.TextSecondary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value", color = VpnColors.TextSecondary) },
                    placeholder = { Text(if (type == "Domain") "google.com" else if (type == "GeoIP") "RU" else "10.0.0.0/8", color = BorderGraphite) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentNeonGreen,
                        unfocusedBorderColor = BorderGraphite,
                        focusedTextColor = VpnColors.TextPrimary,
                        unfocusedTextColor = VpnColors.TextPrimary,
                        cursorColor = AccentNeonGreen,
                        focusedContainerColor = SurfaceInner,
                        unfocusedContainerColor = SurfaceInner
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Proxy", "Direct", "Block").forEach { act ->
                        val isSel = action == act
                        val btnColor = when (act) {
                            "Proxy" -> AccentWarning
                            "Direct" -> AccentNeonGreen
                            else -> AccentError
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) btnColor.copy(alpha = 0.12f) else SurfaceInner)
                                .border(1.dp, if (isSel) btnColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { action = act }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = act,
                                style = VpnTypography.buttonText.copy(
                                    fontSize = 11.sp,
                                    color = if (isSel) btnColor else VpnColors.TextSecondary
                                )
                            )
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
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceInner, contentColor = VpnColors.TextPrimary)
            ) {
                Text("Add", style = VpnTypography.buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = VpnTypography.buttonText.copy(color = VpnColors.TextSecondary))
            }
        }
    )
}
