package com.example.service

import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/** Export fixed categories only: native logs may contain keys, URLs and browsing destinations. */
object SafeCoreLog {
    fun classify(line: String): String? {
        val s = line.lowercase(Locale.ROOT)
        return when {
            "no route to host" in s || "network is unreachable" in s -> "network_unreachable"
            "permission denied" in s || "operation not permitted" in s -> "permission_denied"
            "connection refused" in s -> "connection_refused"
            "connection reset" in s -> "connection_reset"
            "handshake" in s && ("timeout" in s || "failed" in s || "error" in s) -> "handshake_related_error"
            "dns" in s && ("error" in s || "fail" in s || "timeout" in s) -> "dns_failure"
            "context deadline exceeded" in s || "timed out" in s || "timeout" in s -> "timeout"
            "wireguard" in s && ("error" in s || "failed" in s) -> "wireguard_error"
            "fatal" in s || "panic" in s -> "core_fatal"
            else -> null
        }
    }

    fun read(file: File): List<String> = try {
        if (!file.isFile) emptyList() else RandomAccessFile(file, "r").use { input ->
            val size = minOf(input.length(), 96L * 1024L).toInt()
            val bytes = ByteArray(size)
            input.seek(input.length() - size)
            input.readFully(bytes)
            String(bytes, Charsets.UTF_8).lineSequence().mapNotNull { line ->
                val category = classify(line) ?: return@mapNotNull null
                val timestamp = Regex("\\b\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\b").find(line)?.value ?: "time_unknown"
                "$timestamp $category"
            }.toList().takeLast(40)
        }
    } catch (_: Exception) { emptyList() }
}
