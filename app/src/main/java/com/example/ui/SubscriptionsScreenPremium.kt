@file:JvmName("SubscriptionsScreenPremiumKt")

package com.example.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VpnColors
import com.example.ui.theme.VpnTypography

@Composable
fun SubscriptionsScreenPremium(
    onAddSubscriptionClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnColors.Background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ─── Header ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Trans.get("tab_subscriptions"),
                style = VpnTypography.statusMain.copy(fontSize = 24.sp, color = VpnColors.TextPrimary)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VpnColors.surfaceInner)
                    .border(1.dp, VpnColors.borderLight, CircleShape)
                    .clickable {
                        isRefreshing = true
                        val url = RedShiftState.subscriptionUrl
                        if (url.isNotEmpty()) {
                            RedShiftState.importSubscription(url)
                        }
                        isRefreshing = false
                        Toast.makeText(context, "Subscriptions Synchronized!", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Update all",
                    tint = VpnColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ─── Content ───
        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VpnColors.accentGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(RedShiftState.subscriptions) { sub ->
                    SubscriptionGlassCard(sub = sub)
                }

                item {
                    AddSubscriptionButton(onClick = {
                        onAddSubscriptionClick()
                        showAddDialog = true
                    })
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialogPremium(
            onDismiss = { showAddDialog = false },
            onAdd = { url ->
                RedShiftState.importSubscription(url)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SubscriptionGlassCard(sub: Subscription) {
    val context = LocalContext.current
    val isStatusOk = sub.status == "OK"
    val statusColor = if (isStatusOk) VpnColors.accentGreen else VpnColors.accentError

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VpnColors.surfaceGlass)
            .border(1.dp, VpnColors.borderLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Status Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }

                    Text(
                        text = sub.name,
                        style = VpnTypography.cardTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VpnColors.surfaceInner)
                        .clickable {
                            RedShiftState.importSubscription(sub.url)
                            Toast.makeText(context, "Synchronizing nodes...", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = VpnColors.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Sync",
                            style = VpnTypography.buttonText.copy(fontSize = 12.sp)
                        )
                    }
                }
            }

            // URL
            Text(
                text = sub.url,
                style = VpnTypography.cardSubtitle.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${sub.serverCount} servers found",
                    style = VpnTypography.cardTitle.copy(fontSize = 13.sp)
                )
                Text(
                    text = "Expires: in ${sub.expiryDays} days",
                    style = VpnTypography.cardSubtitle.copy(
                        color = if (sub.expiryDays < 7) VpnColors.accentWarning else VpnColors.accentGreen
                    )
                )
            }

            // Footer
            Text(
                text = "Last updated: " + sub.lastUpdated,
                style = VpnTypography.statsLabel
            )
        }
    }
}

@Composable
private fun AddSubscriptionButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VpnColors.surfaceInner)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(VpnColors.premiumGradient),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(VpnColors.premiumGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Add Subscription URL",
                style = VpnTypography.buttonText.copy(fontSize = 15.sp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionDialogPremium(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VpnColors.surfaceGlass,
        titleContentColor = VpnColors.TextPrimary,
        textContentColor = VpnColors.TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Add Subscription", style = VpnTypography.statusMain.copy(fontSize = 20.sp))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://...", color = VpnColors.borderLight) },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) onAdd(url.trim())
                }
            ) {
                Text("Add", style = VpnTypography.buttonText.copy(color = VpnColors.accentGreen))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = VpnTypography.buttonText.copy(color = VpnColors.TextSecondary))
            }
        }
    )
}