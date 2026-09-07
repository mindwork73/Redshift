package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object ServerPinger {

    data class PingResult(val latencyMs: Int, val method: String)

    suspend fun ping(server: SubServer): Int = ping(server.address, server.port, server.protocol)

    suspend fun ping(host: String, port: Int, protocol: String = ""): Int = withContext(Dispatchers.IO) {
        val proto = protocol.uppercase()
        val isUdpProto = proto.contains("HYSTERIA") || proto.contains("HY2") ||
                proto.contains("AMNEZIA") || proto.contains("AWG") || proto.contains("WIREGUARD")

        if (isUdpProto) {
            icmpPing(host) ?: tcpPing(host, port) ?: -1
        } else {
            tcpPing(host, port) ?: icmpPing(host) ?: -1
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
