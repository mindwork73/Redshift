package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collects the app's own diagnostics (verbose debug log, sing-box error output, saved
 * crash reports and a short device/build header) into one text file and hands it to the
 * system share sheet.
 *
 * Why this exists: the interesting failures happen on the user's device (the olcRTC room
 * join, native sing-box start, process death), and the app's filesDir is unreadable
 * without adb. Everything is written by the app itself, so nothing here needs extra
 * permissions.
 */
object DiagnosticExporter {

    private const val FILE_NAME = "redshift_diag.txt"
    private const val MAX_LOG_CHARS = 120_000
    private const val MAX_CRASHES = 5
    private fun fingerprint(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .take(8).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    /** Writes the bundle and returns the file to share, or null when nothing could be read. */
    fun build(context: Context): File? {
        val dir = File(context.cacheDir, "diag").apply { mkdirs() }
        val out = File(dir, FILE_NAME)
        return try {
            out.bufferedWriter(Charsets.UTF_8).use { w ->
                w.write("RedShift diagnostics\n")
                w.write("generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
                w.write("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) debug=${BuildConfig.DEBUG}\n")
                w.write("android: ${Build.VERSION.RELEASE} (api ${Build.VERSION.SDK_INT}) ${Build.MANUFACTURER} ${Build.MODEL}\n")
                w.write("abi: ${Build.SUPPORTED_ABIS.joinToString()}\n")
                w.write("vpn state: ${com.example.ui.RedShiftState.connectionState}")
                w.write("\n")
                w.write("connectivity_probe=${com.example.ui.RedShiftState.connectivityStatus}\n")
                w.write("connectivity_details=${com.example.ui.RedShiftState.lastConnectivityDetails}\n")
                for (event in com.example.ui.RedShiftState.connectivityHistorySnapshot()) w.write("probe_history=$event\n")
                w.write("automatic_recovery_attempts=${com.example.ui.RedShiftState.recoveryAttempts}\n")
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val networks = cm.allNetworks.mapNotNull { cm.getNetworkCapabilities(it) }
                    .filter { it.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN) }
                val types = networks.mapNotNull {
                    when {
                        it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                        it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                        it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                        else -> null
                    }
                }.distinct().sorted()
                w.write("underlying_networks=${types.joinToString(",").ifEmpty { "none" }}\n")
                w.write("underlying_validated=${networks.any { it.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) }}\n")
                w.write("auto_reconnect=${com.example.ui.RedShiftState.autoReconnect}\n")
                w.write("tunReady=${RedShiftVpnService.tunReady} tunFd=${RedShiftVpnService.tunFdRaw}")
                w.write("\n")
                // Raw logs, URLs and tunnel configuration may contain credentials.
                // Export only a small allowlisted summary.
                val state = com.example.ui.RedShiftState
                w.write("servers=${state.servers.size}\n")
                val protocol = state.getSelectedServer()?.protocol.orEmpty().uppercase(Locale.ROOT)
                val safeProtocol = when {
                    "OLCRTC" in protocol -> "OLCRTC"
                    "AMNEZIA" in protocol || "AWG" in protocol || "WIREGUARD" in protocol -> "AMNEZIAWG"
                    "VLESS" in protocol -> "VLESS"
                    "VMESS" in protocol -> "VMESS"
                    "TROJAN" in protocol -> "TROJAN"
                    "HYSTERIA" in protocol || "HY2" in protocol -> "HYSTERIA2"
                    "SHADOWSOCKS" in protocol || protocol == "SS" -> "SHADOWSOCKS"
                    else -> "OTHER"
                }
                w.write("selected_protocol=$safeProtocol routing=${state.routingMode.name}\n")
                state.getSelectedServer()?.let { server ->
                    val endpoint = "${server.address.trim().lowercase(Locale.ROOT)}:${server.port}"
                    w.write("endpoint_fingerprint=${fingerprint(endpoint)} port=${server.port}\n")
                    if (safeProtocol == "AMNEZIAWG") {
                        val params = try { org.json.JSONObject(server.subAwgParams) } catch (_: Exception) { org.json.JSONObject() }
                        val names = listOf("jc", "jmin", "jmax", "s1", "s2", "s3", "s4", "h1", "h2", "h3", "h4", "i1", "i2", "i3", "i4", "i5")
                        val present = names.filter { params.has(it) && params.optString(it).isNotBlank() }
                        w.write("awg_private_key_present=${server.subPrivateKey.isNotBlank()} server_key_present=${server.subServerPublicKey.isNotBlank()} preshared_key_present=${server.subPresharedKey.isNotBlank()} address_present=${server.subLocalAddress.isNotBlank()} mtu=${server.subMtu}\n")
                        w.write("awg_parameters_present=${present.joinToString(",").ifEmpty { "none" }}\n")
                        if (server.subServerPublicKey.isNotBlank()) w.write("awg_server_key_fingerprint=${fingerprint(server.subServerPublicKey.trim())}\n")
                        if (server.subLocalAddress.isNotBlank()) w.write("awg_client_address_fingerprint=${fingerprint(server.subLocalAddress.trim().substringBefore('/'))}\n")
                        val normalized = present.joinToString(";") { "$it=${params.optString(it).trim()}" }
                        if (normalized.isNotEmpty()) w.write("awg_parameters_fingerprint=${fingerprint(normalized)}\n")
                    }
                }
                w.write("split_enabled=${state.splitTunnelEnabled} split_by_apps=${state.splitTunnelByApps} split_only=${state.splitTunnelOnly}\n")
                val diag = context.getSharedPreferences("redshift_diag_state", Context.MODE_PRIVATE)
                w.write("last_import_result=${diag.getString("import_result", "none")} route=${diag.getString("import_route", "none")}\n")
                val provider = diag.getString("provider", "none") ?: "none"
                val olcrtc = if (provider == "none") "inactive" else if (RedShiftVpnService.tunReady) "tun-ready" else "starting-or-stopped"
                w.write("olcrtc=$olcrtc\n")
                w.write("provider=$provider transport=${diag.getString("transport", "none")}\n")
                w.write("last_stage=${diag.getString("stage", "none")}\n")
                w.write("last_failure=${diag.getString("failure", "none")}\n")
                val config = try { org.json.JSONObject(File(context.filesDir, "singbox/config.json").readText()) } catch (_: Exception) { null }
                val outbounds = config?.optJSONArray("outbounds")
                val actual = (0 until (outbounds?.length() ?: 0)).asSequence().mapNotNull { outbounds?.optJSONObject(it) }
                    .firstOrNull { it.optString("type") == "wireguard" }
                if (actual != null) {
                    val peer = actual.optJSONArray("peers")?.optJSONObject(0)
                    val address = peer?.optString("address").orEmpty()
                    val port = peer?.optInt("port") ?: 0
                    if (address.isNotBlank()) {
                        val endpoint = "${address.trim().lowercase(Locale.ROOT)}:$port"
                        w.write("saved_core_awg_endpoint_fingerprint=${fingerprint(endpoint)} port=$port\n")
                    }
                    w.write("saved_core_awg_peer_present=${peer != null} private_key_present=${actual.optString("private_key").isNotBlank()}\n")
                    val clientIp = actual.optJSONArray("address")?.optString(0).orEmpty().trim().substringBefore('/')
                    if (clientIp.isNotBlank()) w.write("saved_core_awg_client_address_fingerprint=${fingerprint(clientIp)}\n")
                    val pubkey = peer?.optString("public_key").orEmpty().trim()
                    if (pubkey.isNotBlank()) w.write("saved_core_awg_server_key_fingerprint=${fingerprint(pubkey)}\n")
                }
                val coreEvents = SafeCoreLog.read(File(context.filesDir, "singbox/run.log")) + SafeCoreLog.read(File(context.filesDir, "singbox/err.txt"))
                w.write("core_events=${coreEvents.size}\n")
                coreEvents.takeLast(40).forEach { w.write("core_event=$it\n") }
                w.write("note=core_events_are_allowlisted_categories_not_raw_logs; no_handshake_event_does_not_prove_no_handshake\n")
                w.write("Diagnostic details containing subscription credentials are omitted.\n")
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    private fun section(w: java.io.Writer, title: String, file: File) {
        w.write("\n================ $title ================\n")
        if (!file.exists()) {
            w.write("(missing)\n")
            return
        }
        try {
            val text = file.readText()
            w.write(if (text.length > MAX_LOG_CHARS) text.takeLast(MAX_LOG_CHARS) else text)
            if (text.length > MAX_LOG_CHARS) w.write("\n… truncated to last $MAX_LOG_CHARS chars\n")
        } catch (e: Exception) {
            w.write("(read failed: ${e.message})\n")
        }
    }

    /** Builds the bundle and opens the share sheet. Returns false when there was nothing to share. */
    fun share(context: Context): Boolean {
        val file = build(context) ?: return false
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "RedShift diagnostics")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "RedShift diagnostics").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }
}
