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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnTypography

@Composable
fun RoutingRulesScreenPremium() {
    var rulesCollapsed by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnColors.Background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ─── Header ───
        Text(
            text = Trans.get("tab_rules"),
            style = VpnTypography.statusMain.copy(fontSize = 24.sp, color = VpnColors.TextPrimary),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ─── Mode Selectors ───
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

        // ─── Custom Rules Header ───
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

        // ─── Rules List ───
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Predefined Rule Sets
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(VpnColors.surfaceGlass)
                        .border(1.dp, VpnColors.borderLight, RoundedCornerShape(20.dp))
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
                            Text(
                                text = "Predefined Rule Sets",
                                style = VpnTypography.cardTitle
                            )
                            Icon(
                                imageVector = if (rulesCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = null,
                                tint = VpnColors.TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = !rulesCollapsed) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                RuleSwitchRowPremium(label = "Bypass Local Network Address", checked = RedShiftState.bypassLocal, onCheckedChange = { RedShiftState.bypassLocal = it })
                                RuleSwitchRowPremium(label = "Bypass LAN IPs (192.168.x.x)", checked = RedShiftState.bypassLan, onCheckedChange = { RedShiftState.bypassLan = it })
                                RuleSwitchRowPremium(label = "Bypass Russian Sites", checked = RedShiftState.bypassRussia, onCheckedChange = { RedShiftState.bypassRussia = it })
                                RuleSwitchRowPremium(label = "Bypass China Sites", checked = RedShiftState.bypassChina, onCheckedChange = { RedShiftState.bypassChina = it })
                                RuleSwitchRowPremium(label = "Block Ads & Trackers", checked = RedShiftState.blockAds, onCheckedChange = { RedShiftState.blockAds = it })
                            }
                        }
                    }
                }
            }

            // Custom Rules
            items(RedShiftState.routingRules) { rule ->
                CustomRuleCardPremium(rule = rule)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialogPremium(
            onDismiss = { showAddRuleDialog = false },
            onRuleAdded = { newRule ->
                RedShiftState.routingRules.add(newRule)
                showAddRuleDialog = false
            }
        )
    }
}

// ─── Component Helpers ───

@Composable
private fun ModeSelectorCard(
    mode: RoutingMode,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val isActive = RedShiftState.routingMode == mode
    val activeBrush = Brush.horizontalGradient(VpnColors.premiumGradient)
    val inactiveBrush = Brush.horizontalGradient(listOf(VpnColors.surfaceInner, VpnColors.surfaceInner))

    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) activeBrush else inactiveBrush)
            .border(
                width = 1.dp,
                color = if (isActive) Color.Transparent else VpnColors.borderLight,
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
private fun RuleSwitchRowPremium(
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
private fun CustomRuleCardPremium(rule: RoutingRule) {
    var isEnabled by remember { mutableStateOf(rule.isEnabled) }

    val actionColor = when (rule.action) {
        "Proxy" -> VpnColors.accentWarning
        "Direct" -> VpnColors.accentGreen
        else -> VpnColors.accentError
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VpnColors.surfaceInner)
            .border(1.dp, VpnColors.borderLight, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = VpnColors.borderLight,
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(actionColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rule.action,
                            style = VpnTypography.buttonText.copy(fontSize = 10.sp, color = actionColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rule.value,
                    style = VpnTypography.cardTitle.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VpnColors.Background,
                    checkedTrackColor = VpnColors.accentGreen,
                    uncheckedThumbColor = VpnColors.TextSecondary,
                    uncheckedTrackColor = VpnColors.Background,
                    uncheckedBorderColor = VpnColors.borderLight
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialogPremium(
    onDismiss: () -> Unit,
    onRuleAdded: (RoutingRule) -> Unit
) {
    var type by remember { mutableStateOf("Domain") }
    var value by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("Proxy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VpnColors.surfaceGlass,
        titleContentColor = VpnColors.TextPrimary,
        textContentColor = VpnColors.TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Add Routing Rule", style = VpnTypography.statusMain.copy(fontSize = 20.sp))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Type Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Domain", "IP CIDR", "GeoIP").forEach { t ->
                        val isSel = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) VpnColors.surfaceCardSolid else VpnColors.surfaceInner)
                                .border(1.dp, if (isSel) VpnColors.TextPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { type = t }
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

                // Value Input
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value", color = VpnColors.TextSecondary) },
                    placeholder = { Text(if (type == "Domain") "google.com" else if (type == "GeoIP") "RU" else "10.0.0.0/8", color = VpnColors.borderLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VpnColors.TextPrimary,
                        unfocusedBorderColor = VpnColors.borderLight,
                        focusedTextColor = VpnColors.TextPrimary,
                        unfocusedTextColor = VpnColors.TextPrimary,
                        cursorColor = VpnColors.accentGreen,
                        focusedContainerColor = VpnColors.surfaceInner,
                        unfocusedContainerColor = VpnColors.surfaceInner
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Action Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Proxy", "Direct", "Block").forEach { act ->
                        val isSel = action == act
                        val btnColor = when (act) {
                            "Proxy" -> VpnColors.accentWarning
                            "Direct" -> VpnColors.accentGreen
                            else -> VpnColors.accentError
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) btnColor.copy(alpha = 0.15f) else VpnColors.surfaceInner)
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
                colors = ButtonDefaults.buttonColors(containerColor = VpnColors.surfaceInner, contentColor = VpnColors.TextPrimary)
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