package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

object ServerPinger {

    data class PingResult(val latencyMs: Int, val method: String)

    /** True for protocols that only talk UDP (no TCP listener to probe). */
    fun isUdpBased(protocol: String): Boolean {
        val p = protocol.uppercase()
        return p.contains("HYSTERIA") || p.contains("HY2") ||
            p.contains("AMNEZIA") || p.contains("AWG") ||
            p.contains("WIREGUARD") || p == "WG"
    }

    suspend fun ping(server: SubServer): Int = ping(server.address, server.port, server.protocol)

    suspend fun ping(host: String, port: Int, protocol: String = ""): Int = withContext(Dispatchers.IO) {
        // olcRTC rows give us a room URL instead of a host:port, so probing the raw
        // string never resolved and every olcRTC server ended up with latency -1, which
        // also kept it out of the auto-select.
        val (probeHost, probePort) = normalizeTarget(host, port)
        val isUdpProto = isUdpBased(protocol)

        if (isUdpProto) {
            icmpPing(probeHost) ?: tcpPing(probeHost, probePort) ?: -1
        } else {
            tcpPing(probeHost, probePort) ?: icmpPing(probeHost) ?: -1
        }
    }

    /** "https://meet.example.org/room", 0 → "meet.example.org", 443. Anything else is left as is. */
    private fun normalizeTarget(host: String, port: Int): Pair<String, Int> {
        val trimmed = host.trim()
        val https = trimmed.startsWith("https://")
        if (!https && !trimmed.startsWith("http://")) return host to port
        val noScheme = trimmed.substringAfter("://")
        val name = noScheme.substringBefore('/').substringBefore(':')
        val resolvedPort = if (port > 0) port else if (https) 443 else 80
        return name to resolvedPort
    }

    /**
     * Latency measured through the local mixed proxy (127.0.0.1:proxyPort) that runs
     * inside the tunnel — the app's own sockets are excluded from the TUN, so this is
     * the only way for the UI to ping while connected (same approach as Amnezia "ping
     * via proxy"). Uses a SOCKS5 CONNECT handshake and times the full round-trip.
     */
    suspend fun pingViaProxy(host: String, port: Int, protocol: String = "", proxyPort: Int = SingBoxManager.MIXED_PORT): Int {
        val (probeHost, probePort) = normalizeTarget(host, port)
        val ipHost = try {
            java.net.InetAddress.getByName(probeHost).hostAddress ?: probeHost
        } catch (_: Exception) { probeHost }
        return withContext(Dispatchers.IO) {
            solidPing(ipHost, probePort, proxyPort)
        }
    }

    private fun solidPing(host: String, port: Int, proxyPort: Int): Int {
        val start = System.currentTimeMillis()
        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress("127.0.0.1", proxyPort), 3000)
            socket.soTimeout = 3000
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            out.write(byteArrayOf(0x05, 0x01, 0x00)) // SOCKS5, one method: no-auth
            out.flush()
            val greeting = ByteArray(2)
            input.readFully(greeting)
            if (greeting[0] != 0x05.toByte() || greeting[1] != 0x00.toByte()) return -1

            val addrBytes = host.split(".").mapNotNull { it.toIntOrNull() }.take(4)
            val b4 = ByteArray(4)
            for (i in 0 until 4) b4[i] = ((addrBytes.getOrElse(i) { 0 }) and 0xFF).toByte()
            val request = byteArrayOf(
                0x05, 0x01, 0x00, 0x01,
                b4[0], b4[1], b4[2], b4[3],
                ((port shr 8) and 0xFF).toByte(), (port and 0xFF).toByte()
            )
            out.write(request)
            out.flush()
            val reply = ByteArray(10)
            input.readFully(reply)
            if (reply[1] != 0x00.toByte()) return -1

            socket.close()
            return (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            return -1
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun icmpPing(host: String): Int? {
        return try {
            val proc = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "2", host)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            val regex = Regex("time[=<]([0-9.]+)\\s*ms")
            val match = regex.find(output) ?: return null
            (match.groupValues[1].toDoubleOrNull() ?: return null).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun tcpPing(host: String, port: Int): Int? {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 3000)
            socket.close()
            val elapsed = System.currentTimeMillis() - start
            elapsed.toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            null
        }
    }
}
