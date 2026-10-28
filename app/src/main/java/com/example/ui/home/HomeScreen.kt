package com.example.ui.home

import com.example.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionState
import com.example.ui.RedShiftState
import com.example.ui.formatDuration
import com.example.ui.formatPing
import com.example.ui.formatSpeedLabel
import com.example.ui.t

private val mint = Color(0xFF72F7B5)
private val muted = Color(0xFF9EA9A4)

@Composable
fun HomeScreen(onOpenServers: () -> Unit) {
    val state = RedShiftState.connectionState
    val server = RedShiftState.getSelectedServer()
    Box(Modifier.fillMaxSize().background(Color(0xFF090D0C))) {
        WorldMapBackground(state, server, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xE6090D0C), Color.Transparent, Color(0xDD090D0C), Color(0xFF090D0C)))))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("REDSHIFT", color = Color.White, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp, fontSize = 16.sp)
                    Text("PRIVATE NETWORK", color = muted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Text("●", color = if (state == ConnectionState.CONNECTED) mint else muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(34.dp))
            Text(when(state) { ConnectionState.CONNECTED -> t("connected"); ConnectionState.CONNECTING -> t("connecting"); ConnectionState.DISCONNECTED -> t("disconnected") }, color = if (state == ConnectionState.CONNECTED) mint else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            Text(if (state == ConnectionState.CONNECTED) server?.name ?: "—" else t("home_not_protected"), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (state == ConnectionState.CONNECTED) formatDuration(RedShiftState.sessionDurationSeconds) else "REDPILL CLOUD", color = muted, fontSize = 12.sp, letterSpacing = 1.sp)
            if (state == ConnectionState.DISCONNECTED && RedShiftState.vpnError != null) {
                Spacer(Modifier.height(10.dp))
                Text(t("vpn_connect_failed") + " · " + t("vpn_error_${RedShiftState.vpnError}"), color = Color(0xFFFFB4A9), fontSize = 12.sp, maxLines = 2)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(106.dp).border(1.dp, mint.copy(alpha = .30f), CircleShape).padding(9.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF354C40), Color(0xFF171E1B)))).border(1.dp, Color.White.copy(alpha=.18f), CircleShape).clickable { RedShiftState.toggleVpn() }, contentAlignment = Alignment.Center) {
                    Icon(AppIcons.PowerSettingsNew, contentDescription = t("app_title"), tint = if (state == ConnectionState.CONNECTED) mint else Color.White, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xE51A201D)).border(1.dp, Color.White.copy(alpha=.08f), RoundedCornerShape(18.dp)).clickable { onOpenServers() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.ServerIcon(active = state == ConnectionState.CONNECTED, flag = RedShiftState.serverFlag(server))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("nav_servers").uppercase(), color = muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Text(server?.name ?: t("no_server_selected"), color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(AppIcons.ArrowForward, contentDescription = null, tint = mint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric(t("download"), formatSpeedLabel(RedShiftState.downloadSpeed))
                Metric(t("upload"), formatSpeedLabel(RedShiftState.uploadSpeed))
                Metric(t("ping"), formatPing(server?.latency ?: 0))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label.uppercase(), color = muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Start)
    }
}
