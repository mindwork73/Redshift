package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.example.ui.DASH
import com.example.ui.RedShiftState
import com.example.ui.UiText
import com.example.ui.components.GlassSurface
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GradientButton
import com.example.ui.components.ListItem
import com.example.ui.components.PlanPill
import com.example.ui.components.Screen
import com.example.ui.components.ScreenHeader
import com.example.ui.isPremiumPlan
import com.example.ui.t
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors

/** Support bot handle — the one the user reaches for real (typo for @redpillcloudbot handle is 100% broken). */
private const val SupportBot = "@redpillcloud_bot"

/**
 * Profile tab (REDESIGN.md §8.4): account identity, the real subscription and logout.
 * Anything the API has not returned renders as a dash, never as a guess.
 */
@Composable
fun ProfileScreen() {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val userInfo = RedShiftState.userInfo
    val telegramId = RedShiftState.telegramToken
    val isLoadingUser = RedShiftState.isLoadingUser
    val plan = RedShiftState.subscriptionPlan
    val expiry = RedShiftState.subscriptionExpiry

    val username = userInfo?.username?.takeIf { it.isNotBlank() }
    val deviceLimit = userInfo?.subscription?.devicesLimit ?: 0
    val devicesUsed = userInfo?.let {
        if (it.deviceCount > 0) it.deviceCount else it.subscription?.devices?.size ?: 0
    } ?: 0

    Screen(scrollable = true) {
        ScreenHeader(
            title = t("profile_title"),
            actions = {
                if (isLoadingUser) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VpnColors.Accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    GlassIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = t("cd_refresh"),
                        enabled = RedShiftState.isLoggedIn,
                        onClick = {
                            telegramId.toIntOrNull()?.let { id ->
                                RedShiftState.refreshUserData(id)
                            }
                        }
                    )
                }
            }
        )

        Spacer(Modifier.height(RedSpace.L))

        // ── Account ──
        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(VpnColors.GlassFillStrong),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = VpnColors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(RedSpace.M))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (RedShiftState.isLoggedIn) {
                            username ?: t("signed_in")
                        } else {
                            t("not_signed_in")
                        },
                        style = RedType.Title,
                        color = VpnColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(t("telegram_id"))
                            append(": ")
                            append(telegramId.ifBlank { DASH })
                        },
                        style = RedType.Caption,
                        color = VpnColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isPremiumPlan(plan)) {
                    Spacer(Modifier.width(RedSpace.S))
                    PlanPill(plan = plan)
                }
            }
        }

        Spacer(Modifier.height(RedSpace.M))

        // ── Subscription ──
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPremiumPlan(plan)) {
                        Icons.Filled.VerifiedUser
                    } else {
                        Icons.Filled.Shield
                    },
                    contentDescription = null,
                    tint = if (isPremiumPlan(plan)) {
                        VpnColors.Accent
                    } else {
                        VpnColors.TextTertiary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(RedSpace.S))
                Text(
                    text = t("subscription"),
                    style = RedType.BodyMedium,
                    color = VpnColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(RedSpace.Xs))

            ListItem(
                title = t("subscription_plan"),
                trailingText = plan.ifBlank { DASH },
                showDivider = true
            )
            ListItem(
                title = t("subscription_expiry"),
                trailingText = expiry.ifBlank { DASH },
                showDivider = deviceLimit > 0
            )
            if (deviceLimit > 0) {
                ListItem(
                    title = t("devices"),
                    trailingText = UiText.format("device_count", devicesUsed, deviceLimit)
                )
            }
        }

        Spacer(Modifier.height(RedSpace.M))

        // ── Support ──
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = RedSpace.M, vertical = RedSpace.Xs)
        ) {
            val context = LocalContext.current
            ListItem(
                title = t("support_bot"),
                subtitle = SupportBot,
                leadingIcon = Icons.Filled.Shield,
                showChevron = true,
                onClick = {
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
            )
        }

        Spacer(Modifier.height(RedSpace.Xl))

        GradientButton(
            text = t("logout"),
            icon = Icons.Filled.Logout,
            enabled = RedShiftState.isLoggedIn,
            onClick = { showLogoutDialog = true }
        )

        Spacer(Modifier.height(RedSpace.M))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = t("logout")) },
            text = { Text(text = t("logout_confirm")) },
            containerColor = VpnColors.Background,
            titleContentColor = VpnColors.TextPrimary,
            textContentColor = VpnColors.TextSecondary,
            confirmButton = {
                TextButton(onClick = {
                    RedShiftState.logout()
                    showLogoutDialog = false
                }) {
                    Text(text = t("logout"), color = VpnColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = t("cancel"), color = VpnColors.TextSecondary)
                }
            }
        )
    }
}
