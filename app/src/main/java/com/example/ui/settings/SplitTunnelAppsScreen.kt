package com.example.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.RedShiftState
import com.example.ui.UiText
import com.example.ui.t
import com.example.ui.theme.RedRadius
import com.example.ui.theme.RedSpace
import com.example.ui.theme.RedType
import com.example.ui.theme.VpnColors
import com.example.ui.components.GlassIconButton
import com.example.ui.components.Screen
import com.example.ui.components.ScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

/**
 * Full-screen picker for split-tunnel apps: every installed launchable app with an
 * icon, search and quick select-all/clear actions. Selection lives in
 * [com.example.ui.RedShiftState.splitApps].
 */
@Composable
fun SplitTunnelAppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            runCatching { pm.queryIntentActivities(query, PackageManager.MATCH_ALL) }
                .getOrDefault(emptyList())
                .mapNotNull { ri ->
                    val label = ri.loadLabel(pm).toString()
                    val icon = runCatching {
                        ri.loadIcon(pm).toIcon(pm)
                    }.getOrNull()
                    AppEntry(
                        packageName = ri.activityInfo.packageName,
                        label = label,
                        icon = icon
                    )
                }
                .distinctBy { it.packageName }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        apps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }
    val selectedSet = RedShiftState.splitApps.toSet()
    val selectedCount = selectedSet.size
    val allSelected = apps.isNotEmpty() && apps.all { selectedSet.contains(it.packageName) }

    Screen(scrollable = false) {
        ScreenHeader(
            title = t("split_apps_title"),
            actions = {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = t("cd_back"),
                    onClick = onBack
                )
            }
        )
        Spacer(Modifier.height(RedSpace.L))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = UiText.format("split_selected_count", selectedCount),
                style = RedType.Caption,
                color = VpnColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val all = apps.map { it.packageName }
                    all.forEach { pkg -> if (!selectedSet.contains(pkg)) RedShiftState.toggleSplitApp(pkg) }
                },
                enabled = apps.isNotEmpty() && !allSelected
            ) {
                Text(text = t("split_apps_select_all"), color = VpnColors.Success)
            }
            TextButton(
                onClick = {
                    apps.forEach { pkg -> if (selectedSet.contains(pkg.packageName)) RedShiftState.toggleSplitApp(pkg.packageName) }
                },
                enabled = selectedCount > 0
            ) {
                Text(text = t("split_apps_clear"), color = VpnColors.Danger)
            }
        }

        Spacer(Modifier.height(RedSpace.Xs))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(text = t("search_apps"), style = RedType.Caption) },
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

        Spacer(Modifier.height(RedSpace.S))

        if (apps.isEmpty()) {
            Text(
                text = t("split_empty"),
                style = RedType.Caption,
                color = VpnColors.TextTertiary,
                modifier = Modifier.padding(vertical = RedSpace.M)
            )
        } else if (filtered.isEmpty()) {
            Text(
                text = t("split_empty"),
                style = RedType.Caption,
                color = VpnColors.TextTertiary,
                modifier = Modifier.padding(vertical = RedSpace.M)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filtered, key = { it.packageName }) { item ->
                    val checked = selectedSet.contains(item.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RedRadius.Small))
                            .clickable { RedShiftState.toggleSplitApp(item.packageName) }
                            .padding(horizontal = RedSpace.Xs, vertical = RedSpace.S),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.icon != null) {
                            Image(
                                bitmap = item.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(RedRadius.Small))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(RedRadius.Small))
                                    .background(VpnColors.GlassFillStrong),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = null,
                                    tint = VpnColors.TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(RedSpace.S))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.label,
                                style = RedType.Body,
                                color = VpnColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.packageName,
                                style = RedType.Caption,
                                color = VpnColors.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(RedSpace.S))
                        Icon(
                            imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = if (checked) VpnColors.Success else VpnColors.TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Drawable.toIcon(pm: PackageManager): ImageBitmap? {
    return runCatching {
        if (intrinsicWidth > 0 && intrinsicHeight > 0 && intrinsicWidth <= 256 && intrinsicHeight <= 256) {
            toBitmap(intrinsicWidth / 2, intrinsicHeight / 2)
        } else {
            toBitmap(72, 72)
        }.asImageBitmap()
    }.getOrNull()
}