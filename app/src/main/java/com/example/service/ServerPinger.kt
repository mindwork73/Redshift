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
        return withContext(Dispatchers.IO) {
            solidPing(probeHost, probePort, proxyPort)
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

            if (port !in 1..65535) return -1
            val address = socksAddress(host) ?: return -1
            val request = byteArrayOf(0x05, 0x01, 0x00) + address +
                byteArrayOf((port shr 8).toByte(), port.toByte())
            out.write(request)
            out.flush()
            if (input.readUnsignedByte() != 5 || input.readUnsignedByte() != 0) return -1
            input.readUnsignedByte()
            val length = when (input.readUnsignedByte()) {
                1 -> 4
                3 -> input.readUnsignedByte()
                4 -> 16
                else -> return -1
            }
            input.readFully(ByteArray(length + 2))

            socket.close()
            return (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            return -1
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    internal fun socksAddress(host: String): ByteArray? {
        val raw = host.trim().removePrefix("[").removeSuffix("]")
        if (raw.isEmpty()) return null
        if (':' in raw) {
            val address = runCatching { java.net.InetAddress.getByName(raw).address }.getOrNull() ?: return null
            return if (address.size == 16) byteArrayOf(4) + address else null
        }
        val parts = raw.split('.')
        if (parts.size == 4 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            if (numbers.any { it !in 0..255 }) return null
            return byteArrayOf(1) + numbers.map { it.toByte() }.toByteArray()
        }
        val encoded = runCatching { java.net.IDN.toASCII(raw).toByteArray(Charsets.US_ASCII) }.getOrNull() ?: return null
        if (encoded.isEmpty() || encoded.size > 255) return null
        return byteArrayOf(3, encoded.size.toByte()) + encoded
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
