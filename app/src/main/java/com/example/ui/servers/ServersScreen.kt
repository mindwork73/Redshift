package com.example.ui.servers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionState
import com.example.ui.DASH
import com.example.ui.RedShiftState
import com.example.ui.Server
import com.example.ui.components.GlassSurface
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GradientButton
import com.example.ui.components.ImportSubscriptionSheet
import com.example.ui.components.PingPill
import com.example.ui.components.ProtocolPill
import com.example.ui.components.Screen
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionTitle
import com.example.ui.t
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSize
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/**
 * Servers tab (REDESIGN.md §8.2): the real `RedShiftState.servers` list — no limits,
 * no free/premium split, no invented counts.
 */
@Composable
fun ServersScreen() {
    var showImportSheet by remember { mutableStateOf(false) }
    var serverToDelete by remember { mutableStateOf<Server?>(null) }

    val servers = RedShiftState.servers.toList()
    val selectedId = RedShiftState.selectedServerId
    val sortByPing = RedShiftState.sortByPing

    // Active server first; latency ordering only when the user opted into sortByPing (§8.2).
    val ordered = servers.sortedWith(
        compareByDescending<Server> { it.id == selectedId }
            .thenBy { if (sortByPing && it.latency > 0) it.latency else Int.MAX_VALUE }
            .thenBy { it.name }
    )

    Screen(scrollable = true) {
        ScreenHeader(
            title = t("nav_servers"),
            actions = {
                GlassIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = t("cd_ping_all"),
                    onClick = { RedShiftState.pingAllServers() }
                )
                GlassIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = t("add_server_btn"),
                    onClick = { showImportSheet = true }
                )
            }
        )

        Spacer(Modifier.height(RedSpace.L))

        if (servers.isEmpty()) {
            EmptyServers(onAddSubscription = { showImportSheet = true })
        } else {
            SectionTitle(text = "${t("all_servers")} (${servers.size})")
            Spacer(Modifier.height(RedSpace.Xs))

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
            ) {
                ordered.forEachIndexed { index, server ->
                    ServerRow(
                        server = server,
                        isActive = server.id == selectedId,
                        showDivider = index != ordered.lastIndex,
                        onClick = {
                            RedShiftState.selectedServerId = server.id
                            if (RedShiftState.connectionState != ConnectionState.CONNECTED) {
                                RedShiftState.toggleVpn()
                            }
                        },
                        onLongClick = {
                            if (server.isCustom) serverToDelete = server
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(RedSpace.M))
    }

    if (showImportSheet) {
        ImportSubscriptionSheet(onDismissRequest = { showImportSheet = false })
    }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text(text = t("delete_server")) },
            text = { Text(text = t("delete_server_confirm")) },
            containerColor = VpnColors.Background,
            titleContentColor = VpnColors.TextPrimary,
            textContentColor = VpnColors.TextSecondary,
            confirmButton = {
                TextButton(onClick = {
                    RedShiftState.removeServer(server.id)
                    serverToDelete = null
                }) {
                    Text(text = t("delete"), color = VpnColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text(text = t("cancel"), color = VpnColors.TextSecondary)
                }
            }
        )
    }
}

/** Placeholder shown when there is not a single server yet (§8.2). */
@Composable
private fun EmptyServers(onAddSubscription: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(RedSpace.Xl)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VpnColors.GlassFillStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = null,
                tint = VpnColors.TextTertiary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(RedSpace.M))

        Text(
            text = t("empty_servers"),
            style = RedType.Title,
            color = VpnColors.TextPrimary
        )

        Spacer(Modifier.height(RedSpace.Xs))

        Text(
            text = t("empty_servers_desc"),
            style = RedType.Caption,
            color = VpnColors.TextSecondary
        )

        Spacer(Modifier.height(RedSpace.L))

        GradientButton(
            text = t("add_subscription"),
            onClick = onAddSubscription
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerRow(
    server: Server,
    isActive: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RedSize.ServerRow)
                .clip(RoundedCornerShape(RedRadius.Small))
                .background(if (isActive) VpnColors.AccentSoft else Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = RedSpace.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VpnColors.GlassFillStrong),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = server.flag.takeIf { it.isNotBlank() } ?: DASH,
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.width(RedSpace.S))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = RedType.Body,
                    color = VpnColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = server.address,
                    style = RedType.Caption,
                    color = VpnColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(RedSpace.Xs))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(RedSpace.Xxs)
            ) {
                ProtocolPill(protocol = server.protocol)
                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RedSpace.Xxs)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = t("server_active"),
                            tint = VpnColors.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = t("server_active"),
                            style = RedType.CaptionMedium,
                            color = VpnColors.Success
                        )
                    }
                } else {
                    PingPill(latencyMs = server.latency)
                }
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp)
                    .height(0.5.dp)
                    .background(VpnColors.Divider)
            )
        }
    }
}
