package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.ui.RedShiftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubscriptionImportTimingTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun unreachableSubscriptionIsSavedForLaterWithoutDeletingServers() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val socket = ServerSocket(0)
        val url = "http://127.0.0.1:${socket.localPort}/pending"
        socket.close()
        val store = com.example.service.SettingsStore(context)
        kotlinx.coroutines.runBlocking { store.clearSavedServersAndSubscriptions() }
        RedShiftState.init(context)
        val server = com.example.ui.Server(id = "keep", name = "Keep", flag = "", protocol = "VLESS", address = "127.0.0.1", port = 443)
        RedShiftState.servers.clear()
        RedShiftState.servers.add(server)
        RedShiftState.importSubscription(url)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        while (RedShiftState.isImporting && System.nanoTime() < deadline) {
            dispatcher.scheduler.runCurrent()
            Thread.sleep(20)
        }
        dispatcher.scheduler.runCurrent()
        assertFalse(RedShiftState.isImporting)
        assertTrue(url in store.getBlockingSubscriptionUrls())
        assertNotNull(RedShiftState.importNotice)
        assertNull(RedShiftState.importError)
        assertTrue(RedShiftState.servers.any { it.id == server.id })
    }

    @Test fun readdingSubscriptionRestoresDeletedServersOnlyFromItsResponse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val store = com.example.service.SettingsStore(context)
        val hidden = setOf("nm:VLESS::ONE", "nm:VLESS::OTHER")
        kotlinx.coroutines.runBlocking {
            store.clearSavedServersAndSubscriptions()
            store.setHiddenServers(hidden)
        }
        RedShiftState.init(context)
        RedShiftState.hiddenServers = hidden
        RedShiftState.servers.clear()
        val socket = ServerSocket(0).apply { soTimeout = 10_000 }
        val server = thread(isDaemon = true) {
            socket.accept().use { peer ->
                val reader = peer.getInputStream().bufferedReader()
                while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break }
                val body = "vless://id1@198.51.100.1:443#One\nvless://id2@198.51.100.2:443#Two".toByteArray()
                peer.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(body); flush()
                }
            }
        }
        try {
            RedShiftState.importSubscription("http://127.0.0.1:${socket.localPort}/subscription")
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
            while (RedShiftState.isImporting && System.nanoTime() < deadline) {
                dispatcher.scheduler.runCurrent()
                Thread.sleep(20)
            }
            dispatcher.scheduler.runCurrent()
            assertFalse(RedShiftState.isImporting)
            assertNull(RedShiftState.importError)
            assertTrue(RedShiftState.servers.any { it.name == "One" })
            assertTrue(RedShiftState.servers.any { it.name == "Two" })
            val storedHidden = kotlinx.coroutines.runBlocking { store.hiddenServers.first() }
            assertFalse("nm:VLESS::ONE" in storedHidden)
            assertTrue("nm:VLESS::OTHER" in storedHidden)
        } finally { socket.close(); server.join(1000) }
    }

    @Test fun importFinishesWhileAccountApiIsStillBlocked() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val socket = ServerSocket(0)
        socket.soTimeout = 10000
        val accountRequest = CountDownLatch(1)
        val releaseAccount = CountDownLatch(1)
        val server = thread(isDaemon = true) {
            try {
                while (!socket.isClosed) {
                    val peer = socket.accept()
                    thread(isDaemon = true) {
                        peer.use {
                            val input = it.getInputStream().bufferedReader()
                            val line = input.readLine().orEmpty()
                            while (true) { val h = input.readLine() ?: break; if (h.isEmpty()) break }
                            val isSub = line.contains("/sub/12345")
                            if (!isSub) { accountRequest.countDown(); releaseAccount.await(10, TimeUnit.SECONDS) }
                            val body = if (isSub) "vless://test-id@127.0.0.1:443#Test%20server" else "{}"
                            val bytes = body.toByteArray()
                            val output = it.getOutputStream()
                            output.write("HTTP/1.1 200 OK\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            output.write(bytes); output.flush()
                        }
                    }
                }
            } catch (_: java.net.SocketException) {}
        }
        try {
            RedShiftState.init(context)
            RedShiftState.apiBaseUrl = "http://127.0.0.1:${socket.localPort}"
            RedShiftState.importSubscription("http://127.0.0.1:${socket.localPort}/sub/12345")
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
            while ((RedShiftState.isImporting || accountRequest.count > 0) && System.nanoTime() < deadline) {
                dispatcher.scheduler.runCurrent()
                Thread.sleep(20)
            }
            dispatcher.scheduler.runCurrent()
            assertEquals("the account API is still blocked", 0L, accountRequest.count)
            assertFalse("import must finish without waiting for account API", RedShiftState.isImporting)
            assertNull(RedShiftState.importError)
            assertTrue(RedShiftState.servers.any { it.name.contains("Test") })
        } finally {
            releaseAccount.countDown(); socket.close(); server.join(1000)
        }
    }
}
