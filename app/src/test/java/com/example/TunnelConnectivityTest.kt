package com.example

import com.example.service.TunnelConnectivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.DataInputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

class TunnelConnectivityTest {
    @Test fun probeUsesSocksProxyAndChecksResponse() = runBlocking {
        val server = ServerSocket(0).apply { soTimeout = 10000 }
        val worker = thread(isDaemon = true) {
            server.accept().use { peer ->
                peer.soTimeout = 10000
                val input = DataInputStream(peer.getInputStream())
                val output = peer.getOutputStream()
                assertEquals(5, input.readUnsignedByte())
                val methods = input.readUnsignedByte()
                repeat(methods) { input.readUnsignedByte() }
                output.write(byteArrayOf(5, 0)); output.flush()
                assertEquals(5, input.readUnsignedByte())
                input.readUnsignedByte(); input.readUnsignedByte()
                when (input.readUnsignedByte()) {
                    1 -> repeat(4) { input.readUnsignedByte() }
                    3 -> repeat(input.readUnsignedByte()) { input.readUnsignedByte() }
                    4 -> repeat(16) { input.readUnsignedByte() }
                    else -> error("Unexpected SOCKS address")
                }
                input.readUnsignedShort()
                output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 80)); output.flush()
                val reader = input.bufferedReader()
                while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break }
                output.write("HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()); output.flush()
            }
        }
        try {
            assertTrue(TunnelConnectivity.check(server.localPort, "http://example.invalid/probe"))
        } finally { server.close(); worker.join(1000) }
    }

    @Test fun missingProxyIsNotReportedAsReachable() = runBlocking {
        val server = ServerSocket(0)
        val port = server.localPort
        server.close()
        assertFalse(TunnelConnectivity.check(port, "http://example.invalid/probe"))
    }
}
