package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.UpdateChecker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateDownloadTest {
    @Test fun truncatedDownloadDoesNotReplaceLastCompleteApk() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val socket = ServerSocket(0).apply { soTimeout = 10000 }
        val worker = thread(isDaemon = true) {
            repeat(2) { index ->
                socket.accept().use { peer ->
                    val reader = peer.getInputStream().bufferedReader()
                    while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break }
                    val body = if (index == 0) "complete" else "short"
                    val declared = if (index == 0) body.length else 50
                    peer.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: $declared\r\nConnection: close\r\n\r\n$body".toByteArray())
                        flush()
                    }
                }
            }
        }
        try {
            val url = "http://127.0.0.1:${socket.localPort}/update.apk"
            val first = UpdateChecker.downloadApk(context, url)
            assertNotNull(first)
            assertEquals("complete", first!!.readText())
            assertNull(UpdateChecker.downloadApk(context, url))
            assertEquals("complete", first.readText())
            assertFalse(first.resolveSibling("redshift-update.apk.part").exists())
        } finally {
            socket.close()
            worker.join(1000)
        }
    }
}
