package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.LocalizationState
import com.example.ui.RedShiftState
import com.example.ui.RoutingMode
import com.example.ui.UiText
import com.example.ui.components.GlassSurface
import com.example.ui.components.GradientButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.ImportSubscriptionSheet
import com.example.ui.components.LanguageSheet
import com.example.ui.components.ListItem
import com.example.ui.components.Screen
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionTitle
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToggleRow
import com.example.ui.DASH
import com.example.ui.shortenUrl
import com.example.ui.t
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/** Support bot handle — the same one the onboarding mentions (REDESIGN.md §8.0). */
private const val SupportBot = "@redpillcloud_bot"

/** Opens the support bot in Telegram, falling back to the web client. */
private fun openSupportBot(context: android.content.Context) {
    val tg = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=redpillcloud_bot"))
    try {
        context.startActivity(tg)
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/redpillcloud_bot"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }
}

/**
 * Settings tab (REDESIGN.md §8.3). Every row is bound to a real [RedShiftState] field;
 * nothing here is decorative or invented.
 */
@Composable
fun SettingsScreen(onOpenSplitApps: () -> Unit = {}) {
    var showSubscriptionSheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDomainSheet by remember { mutableStateOf(false) }
    val screenContext = LocalContext.current

    Screen(scrollable = true) {
        ScreenHeader(title = t("nav_settings"))
        Spacer(Modifier.height(RedSpace.L))

        // ── 1. Subscription ──
        SectionTitle(text = t("subscription"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            ListItem(
                title = t("subscription"),
                subtitle = if (RedShiftState.subscriptionPlan.isBlank()) {
                    t("no_subscription")
                } else {
                    RedShiftState.subscriptionPlan
                },
                leadingIcon = Icons.Filled.Cloud,
                showChevron = true,
                showDivider = true,
                onClick = { showSubscriptionSheet = true }
            )
            ListItem(
                title = t("import_title"),
                subtitle = t("import_hint"),
                leadingIcon = Icons.Filled.SdStorage,
                showChevron = true,
                onClick = { showImportSheet = true }
            )
        }

        SectionSpacer()

        // ── 2. Connection ──
        SectionTitle(text = t("section_connection"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            ToggleRow(
                title = t("start_on_boot"),
                checked = RedShiftState.startOnBoot,
                onCheckedChange = { RedShiftState.startOnBoot = it },
                leadingIcon = Icons.Filled.PowerSettingsNew,
                showDivider = true
            )
            ToggleRow(
                title = t("auto_reconnect"),
                checked = RedShiftState.autoReconnect,
                onCheckedChange = { RedShiftState.autoReconnect = it },
                leadingIcon = Icons.Filled.Refresh,
                showDivider = true
            )
            ToggleRow(
                title = t("notifications"),
                checked = RedShiftState.vpnNotification,
                onCheckedChange = { RedShiftState.vpnNotification = it },
                leadingIcon = Icons.Filled.Notifications
            )
        }

        SectionSpacer()

        // ── 3. Routing ──
        SectionTitle(text = t("section_routing"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.S)
        ) {
            Text(
                text = t("routing_hint"),
                style = RedType.Caption,
                color = VpnColors.TextTertiary
            )
            Spacer(Modifier.height(RedSpace.S))
            SegmentedControl(
                options = listOf(RoutingMode.GLOBAL, RoutingMode.RULE, RoutingMode.DIRECT),
                selected = RedShiftState.routingMode,
                onSelect = { RedShiftState.applyRoutingMode(it) },
                label = { mode ->
                    when (mode) {
                        RoutingMode.GLOBAL -> t("routing_global")
                        RoutingMode.RULE -> t("routing_rule")
                        RoutingMode.DIRECT -> t("routing_direct")
                    }
                }
            )
            Spacer(Modifier.height(RedSpace.S))
            Text(
                text = when (RedShiftState.routingMode) {
                    RoutingMode.GLOBAL -> t("routing_global_desc")
                    RoutingMode.RULE -> t("routing_rule_desc")
                    RoutingMode.DIRECT -> t("routing_direct_desc")
                },
                style = RedType.Caption,
                color = VpnColors.TextSecondary
            )
            Spacer(Modifier.height(RedSpace.S))
            ToggleRow(
                title = t("block_ads"),
                subtitle = t("block_ads_desc"),
                checked = RedShiftState.blockAds,
                onCheckedChange = { RedShiftState.applyBlockAds(it) },
                leadingIcon = Icons.Filled.Shield
            )
            ToggleRow(
                title = t("auto_select_best"),
                subtitle = t("auto_select_best_desc"),
                checked = RedShiftState.autoSelectBestServer,
                onCheckedChange = { RedShiftState.setAutoSelectBest(it) },
                leadingIcon = Icons.Filled.NetworkPing,
                showDivider = false
            )
        }

        SectionSpacer()

        // ── 3.5. Split tunneling ──
        SectionTitle(text = t("section_split_tunnel"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.S)
        ) {
            ToggleRow(
                title = t("split_tunnel"),
                subtitle = t("split_tunnel_desc"),
                checked = RedShiftState.splitTunnelEnabled,
                onCheckedChange = { RedShiftState.setSplitEnabled(it) },
                leadingIcon = Icons.Filled.Lock,
                checkedTrackColor = VpnColors.Success,
                showDivider = RedShiftState.splitTunnelEnabled
            )
            if (RedShiftState.splitTunnelEnabled) {
                SegmentedControl(
                    options = listOf(true, false),
                    selected = RedShiftState.splitTunnelByApps,
                    onSelect = { RedShiftState.setSplitByApps(it) },
                    label = { byApps -> if (byApps) t("split_by_apps") else t("split_by_domains") }
                )
                Spacer(Modifier.height(RedSpace.S))
                val byApps = RedShiftState.splitTunnelByApps
                val count = if (byApps) RedShiftState.splitApps.size else RedShiftState.splitDomains.size
                ListItem(
                    title = if (byApps) t("split_apps_title") else t("split_domains_title"),
                    trailingText = UiText.format("split_selected_count", count),
                    showChevron = true,
                    showDivider = true,
                    onClick = {
                        if (byApps) onOpenSplitApps() else showDomainSheet = true
                    }
                )
                ToggleRow(
                    title = if (RedShiftState.splitTunnelOnly) {
                        t("split_only_selected")
                    } else {
                        t("split_exclude_selected")
                    },
                    checked = RedShiftState.splitTunnelOnly,
                    onCheckedChange = { RedShiftState.setSplitOnly(it) },
                    checkedTrackColor = VpnColors.Success
                )
            }
        }

        SectionSpacer()

        // ── 4. Language ──
        SectionTitle(text = t("language"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            ListItem(
                title = t("language"),
                subtitle = LocalizationState.currentLanguage.nativeName,
                leadingIcon = Icons.Filled.Public,
                showChevron = true,
                onClick = { showLanguageSheet = true }
            )
        }

        SectionSpacer()

        // ── 5. Auto refresh ──
        SectionTitle(text = t("section_auto_refresh"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.S)
        ) {
            ToggleRow(
                title = t("auto_refresh"),
                checked = RedShiftState.autoRefresh,
                onCheckedChange = { enabled ->
                    RedShiftState.setAutoRefreshEnabled(enabled, RedShiftState.autoRefreshInterval)
                }
            )
            Spacer(Modifier.height(RedSpace.S))
            Text(
                text = t("refresh_interval"),
                style = RedType.Caption,
                color = VpnColors.TextTertiary
            )
            Spacer(Modifier.height(RedSpace.Xs))
            SegmentedControl(
                options = listOf(6, 12, 24),
                selected = RedShiftState.autoRefreshInterval,
                onSelect = { hours ->
                    RedShiftState.setAutoRefreshEnabled(RedShiftState.autoRefresh, hours)
                },
                label = { hours -> UiText.format("every_n_hours", hours) }
            )
            Spacer(Modifier.height(RedSpace.S))
            ListItem(
                title = t("last_refresh"),
                trailingText = if (RedShiftState.lastRefreshTime > 0L) {
                    LocalizationState.formatDate(RedShiftState.lastRefreshTime)
                } else {
                    t("never")
                },
                leadingIcon = Icons.Filled.Refresh,
                showDivider = true
            )
            ListItem(
                title = t("cached_servers"),
                trailingText = RedShiftState.cachedServerCount.toString(),
                leadingIcon = Icons.Filled.SdStorage
            )
        }

        SectionSpacer()

        // ── 6. Data ──
        SectionTitle(text = t("section_data"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            ListItem(
                title = t("reset_cache"),
                leadingIcon = Icons.Filled.DataUsage,
                titleColor = VpnColors.Danger,
                onClick = { showResetDialog = true }
            )
        }

        SectionSpacer()

        // ── 7. About ──
        SectionTitle(text = t("section_about"))
        Spacer(Modifier.height(RedSpace.Xs))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            ListItem(
                title = t("version"),
                trailingText = BuildConfig.VERSION_NAME,
                leadingIcon = Icons.Filled.Info,
                showDivider = true
            )
            ListItem(
                title = t("local_port"),
                trailingText = RedShiftState.localPort.toString(),
                leadingIcon = Icons.Filled.Public,
                showDivider = true
            )
            ListItem(
                title = t("support"),
                subtitle = SupportBot,
                leadingIcon = Icons.Filled.VerifiedUser,
                showChevron = true,
                onClick = { openSupportBot(screenContext) }
            )
        }

        Spacer(Modifier.height(RedSpace.M))
    }

    if (showSubscriptionSheet) {
        SubscriptionSheet(onDismissRequest = { showSubscriptionSheet = false })
    }
    if (showImportSheet) {
        ImportSubscriptionSheet(onDismissRequest = { showImportSheet = false })
    }
    if (showLanguageSheet) {
        LanguageSheet(onDismissRequest = { showLanguageSheet = false })
    }
    if (showDomainSheet) {
        SplitDomainsSheet(onDismissRequest = { showDomainSheet = false })
    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = t("reset_cache")) },
            text = { Text(text = t("reset_cache_confirm")) },
            containerColor = VpnColors.Background,
            titleContentColor = VpnColors.TextPrimary,
            textContentColor = VpnColors.TextSecondary,
            confirmButton = {
                TextButton(onClick = {
                    RedShiftState.resetDefaultData()
                    showResetDialog = false
                }) {
                    Text(text = t("reset_cache"), color = VpnColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = t("cancel"), color = VpnColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionSpacer() {
    Spacer(Modifier.height(RedSpace.Xxl))
}

/**
 * Subscription details sheet (REDESIGN.md §8.3.1): tariff, expiry, link, refresh and remove.
 * Values come straight from [RedShiftState]; missing values render as a dash.
 */
@Composable
private fun SubscriptionSheet(onDismissRequest: () -> Unit) {
    val plan = RedShiftState.subscriptionPlan
    val expiry = RedShiftState.subscriptionExpiry
    val url = RedShiftState.subscriptionUrl
    val importing = RedShiftState.isImporting

    GlassSheet(onDismissRequest = onDismissRequest, title = t("subscription")) {
        DetailRow(label = t("subscription_plan"), value = plan.ifBlank { DASH })
        DetailRow(label = t("subscription_expiry"), value = expiry.ifBlank { DASH })
        DetailRow(label = t("subscription_url"), value = shortenUrl(url))

        Spacer(Modifier.height(RedSpace.L))

        if (importing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = VpnColors.Accent,
                    strokeWidth = 2.dp
                )
            }
        } else {
            GradientButton(
                text = t("subscription_refresh"),
                enabled = url.isNotBlank(),
                onClick = { RedShiftState.importSubscription(url) }
            )
            Spacer(Modifier.height(RedSpace.Xs))
            GradientButton(
                text = t("subscription_remove"),
                enabled = url.isNotBlank(),
                onClick = { RedShiftState.subscriptionUrl = "" }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RedSpace.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = RedType.Caption,
            color = VpnColors.TextTertiary,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = RedType.BodyMedium,
            color = VpnColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private data class SplitAppItem(val packageName: String, val label: String)

/**
 * Domain list editor for split tunneling by sites: add a domain, remove with a tap.
 */
@Composable
private fun SplitDomainsSheet(onDismissRequest: () -> Unit) {
    var input by remember { mutableStateOf("") }

    GlassSheet(onDismissRequest = onDismissRequest, title = t("split_domains_title")) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(text = t("split_domain_hint"), style = RedType.Caption) },
                    textStyle = RedType.BodyMedium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VpnColors.Accent,
                        unfocusedBorderColor = VpnColors.GlassBorder,
                        focusedTextColor = VpnColors.TextPrimary,
                        unfocusedTextColor = VpnColors.TextPrimary,
                        cursorColor = VpnColors.Accent
                    )
                )
            }
            Spacer(Modifier.width(RedSpace.S))
            GradientButton(
                text = t("split_add_domain"),
                onClick = {
                    if (input.isNotBlank()) {
                        RedShiftState.addSplitDomain(input)
                        input = ""
                    }
                }
            )
        }

        Spacer(Modifier.height(RedSpace.M))

        if (RedShiftState.splitDomains.isEmpty()) {
            Text(
                text = t("split_empty"),
                style = RedType.Caption,
                color = VpnColors.TextTertiary
            )
        } else {
            RedShiftState.splitDomains.forEach { domain ->
                ListItem(
                    title = domain,
                    trailingText = "✕",
                    trailingColor = VpnColors.TextTertiary,
                    onClick = { RedShiftState.removeSplitDomain(domain) }
                )
            }
        }
    }
}
