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
                w.write("split_enabled=${state.splitTunnelEnabled} split_by_apps=${state.splitTunnelByApps} split_only=${state.splitTunnelOnly}\n")
                val diag = context.getSharedPreferences("redshift_diag_state", Context.MODE_PRIVATE)
                val provider = diag.getString("provider", "none") ?: "none"
                val olcrtc = if (provider == "none") "inactive" else if (RedShiftVpnService.tunReady) "tun-ready" else "starting-or-stopped"
                w.write("olcrtc=$olcrtc\n")
                w.write("provider=$provider transport=${diag.getString("transport", "none")}\n")
                w.write("last_stage=${diag.getString("stage", "none")}\n")
                w.write("last_failure=${diag.getString("failure", "none")}\n")
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
