package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.service.DiagnosticExporter
import com.example.service.SingBoxConfigGenerator
import com.example.service.SubServer
import com.example.service.SubscriptionClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditRegressionTest {
    private val server = SubServer(id = "test", name = "test", protocol = "Hysteria 2", address = "127.0.0.1", port = 443, flag = "")

    @Test fun globalRoutingDoesNotSendRussianOrSplitDomainsDirect() {
        val config = JSONObject(SingBoxConfigGenerator().generateConfig(server, filesDir = "/data", splitDomains = listOf("example.com"), routingMode = "global", bypassRussia = true))
        val rules = config.getJSONObject("route").getJSONArray("rules")
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            assertFalse("Global must not apply domain bypass rules", rule.has("domain_suffix"))
            assertFalse("Global must not apply geo bypass rules", rule.has("rule_set"))
        }
        assertEquals("test", config.getJSONObject("route").getString("final"))
    }

    @Test fun oversizedVpnLinkIsRejectedWithoutAllocation() = runBlocking {
        val malicious = "vpn://" + android.util.Base64.encodeToString(byteArrayOf(0x7f, -1, -1, -1, 0), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        val result = SubscriptionClient().fetchSubscription(malicious)
        assertTrue(result.servers.isEmpty())
        assertTrue(result.error.isNotEmpty())
    }

    @Test fun refreshReplacesCurrentSubscriptionAndPreservesOtherSources() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val socket = java.net.ServerSocket(0)
        socket.soTimeout = 10000
        val url = "http://127.0.0.1:${socket.localPort}/sub"
        val payload = "vless://new-credential@198.51.100.2:443#Current"
        val responder = kotlin.concurrent.thread(isDaemon = true) {
            socket.accept().use { client ->
                val reader = client.getInputStream().bufferedReader()
                while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break }
                val bytes = payload.toByteArray(Charsets.UTF_8)
                client.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(bytes)
                    flush()
                }
            }
        }
        try {
            val store = com.example.service.SettingsStore(context)
            store.setSubscriptionUrl(url)
            store.setCachedServersJson(org.json.JSONArray().apply {
                put(JSONObject().put("id", "old").put("url", url).put("name", "Current").put("protocol", "VLESS").put("address", "198.51.100.2").put("port", 443).put("uuid", "old-credential"))
                put(JSONObject().put("id", "other").put("url", "other-source").put("name", "Other").put("protocol", "VLESS").put("address", "198.51.100.3").put("port", 443))
            }.toString())
            val worker = androidx.work.testing.TestListenableWorkerBuilder<com.example.service.SubscriptionRefreshWorker>(context).build()
            assertEquals(androidx.work.ListenableWorker.Result.success(), worker.doWork())
            val items = org.json.JSONArray(store.getBlockingCachedServersJson())
            assertEquals(2, items.length())
            val rows = (0 until items.length()).map { items.getJSONObject(it) }
            assertTrue(rows.any { it.optString("id") == "other" })
            assertTrue(rows.any { it.optString("uuid") == "new-credential" })
            assertFalse(rows.any { it.optString("uuid") == "old-credential" })
        } finally {
            socket.close()
            responder.join(1000)
        }
    }

    @Test fun removingCachedServerKeepsOtherEntries() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = com.example.service.SettingsStore(context)
        store.setCachedServersJson(org.json.JSONArray().apply {
            put(JSONObject().put("id", "delete-me").put("extra", "private-connection-one"))
            put(JSONObject().put("id", "keep-me").put("extra", "private-connection-two"))
        }.toString())
        store.removeCachedServer("delete-me", "private-connection-one")
        val result = org.json.JSONArray(store.getBlockingCachedServersJson())
        assertEquals(1, result.length())
        assertEquals("keep-me", result.getJSONObject(0).getString("id"))
    }

    @Test fun diagnosticExportOmitsSecretsFromExistingFiles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val secret = "TEST-PRIVATE-KEY-DO-NOT-EXPORT"
        File(context.filesDir, "singbox").mkdirs()
        File(context.filesDir, "singbox/config.json").writeText("{\"private_key\":\"$secret\"}")
        File(context.filesDir, "redshift_debug.log").writeText(secret)
        val report = DiagnosticExporter.build(context)!!.readText()
        assertFalse(report.contains(secret))
        assertFalse(report.contains("config.json"))
    }
}
