package com.example

import com.example.service.SubscriptionClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.DataInputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubscriptionVpnProxyTest {
    @Test fun subscriptionIsFetchedThroughSocksWithoutDirectDns() = runBlocking {
        val proxy = ServerSocket(0).apply { soTimeout = 10_000 }
        var requestedHost = ""
        val worker = thread(isDaemon = true) {
            proxy.accept().use { peer ->
                peer.soTimeout = 10_000
                val input = DataInputStream(peer.getInputStream())
                val output = peer.getOutputStream()
                check(input.readUnsignedByte() == 5)
                repeat(input.readUnsignedByte()) { input.readUnsignedByte() }
                output.write(byteArrayOf(5, 0)); output.flush()
                check(input.readUnsignedByte() == 5)
                input.readUnsignedByte(); input.readUnsignedByte()
                check(input.readUnsignedByte() == 3)
                val hostBytes = ByteArray(input.readUnsignedByte())
                input.readFully(hostBytes)
                requestedHost = String(hostBytes, Charsets.US_ASCII)
                input.readUnsignedShort()
                output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 80)); output.flush()
                val reader = input.bufferedReader()
                while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break }
                val body = "vless://test-id@127.0.0.1:443#Test".toByteArray()
                output.write("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                output.write(body); output.flush()
            }
        }
        try {
            val result = SubscriptionClient(useVpnProxy = true, proxyPort = proxy.localPort)
                .fetchSubscription("http://example.invalid/sub/test")
            worker.join(1000)
            assertEquals("example.invalid", requestedHost)
            assertEquals("", result.error)
            assertEquals(1, result.servers.size)
        } finally { proxy.close(); worker.join(1000) }
    }

    @Test fun unavailableVpnProxyReportsConnectionFailure() = runBlocking {
        val proxy = ServerSocket(0)
        val port = proxy.localPort
        proxy.close()
        val result = SubscriptionClient(useVpnProxy = true, proxyPort = port)
            .fetchSubscription("http://example.invalid/sub/test")
        assertEquals("connection", result.failureCode)
    }
}
