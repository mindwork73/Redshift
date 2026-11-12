package com.example.service

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single app-wide log sink. The UI state machine, the VPN service, the sing-box wrapper
 * and the subscription parser all write here, and [DiagnosticExporter] ships exactly this
 * file through the share sheet — so a failure that only happens on the user's device
 * stays readable without adb.
 *
 * The file is capped: past [MAX_BYTES] it is rewritten in place with its newest half, so a
 * long install cannot grow a log the share sheet then fails to upload. The cap is checked
 * against a cached length instead of the file itself, so the common path stays one append.
 */
object DiagLog {

    private const val FILE_NAME = "redshift_debug.log"
    private const val MAX_BYTES = 512L * 1024L

    private val LOCK = Any()

    @Volatile
    private var root: File? = null

    /** Cached file length in bytes; -1 until the first append after [init]/rotation. */
    @Volatile
    private var bytes: Long = -1L

    /** Called once from RedShiftApp.onCreate. A missing call only disables file output. */
    fun init(filesDir: File?) {
        root = filesDir
        bytes = -1L
    }

    /** Appends "HH:mm:ss.SSS [tag] msg" to the diag log and mirrors the line to logcat. */
    fun d(tag: String, msg: String) {
        Log.e(tag, msg.trimEnd())
        val dir = root ?: return
        val line = "${ts()} [$tag] $msg\n"
        val payload = line.toByteArray(Charsets.UTF_8).size.toLong()
        synchronized(LOCK) {
            try {
                val f = File(dir, FILE_NAME)
                var size = bytes
                if (size < 0) size = if (f.exists()) f.length() else 0L
                if (size + payload > MAX_BYTES) {
                    val text = f.readText(Charsets.UTF_8)
                    val trimmed = text.takeLast((MAX_BYTES / 2).toInt())
                    f.writeText(trimmed, Charsets.UTF_8)
                    size = trimmed.toByteArray(Charsets.UTF_8).size.toLong()
                }
                f.appendText(line, Charsets.UTF_8)
                bytes = size + payload
            } catch (_: Exception) {
                // Logging must never take the app down; logcat still has the line.
            }
        }
    }

    private fun ts(): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
}
