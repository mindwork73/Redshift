package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.service.DiagnosticExporter
import com.example.service.SingBoxConfigGenerator
import com.example.service.SubServer
import com.example.service.SubscriptionClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditRegressionTest {
    private val server = SubServer(id = "test", name = "test", protocol = "Hysteria 2", address = "127.0.0.1", port = 443, flag = "")

    @Before fun resetSubscriptions() = runBlocking {
        val store = com.example.service.SettingsStore(ApplicationProvider.getApplicationContext())
        store.setCachedServersJson("[]")
        store.getBlockingSubscriptionUrls().toList().forEach { store.removeSubscription(it) }
        store.setSubscriptionUrl("")
        Unit
    }

    @Test fun legacyRedpillAwgUsesDirectVpnDns() {
        assertEquals("nl.redpillcloud.ru", SingBoxConfigGenerator.amneziaEndpointHost("redpillcloud.ru"))
        assertEquals("nl.redpillcloud.ru", SingBoxConfigGenerator.amneziaEndpointHost("REDPILLCLOUD.RU"))
        assertEquals("example.org", SingBoxConfigGenerator.amneziaEndpointHost("example.org"))
        assertEquals("redpillcloud.ru.example.org", SingBoxConfigGenerator.amneziaEndpointHost("redpillcloud.ru.example.org"))
    }

    @Test fun globalRoutingDoesNotSendRussianOrSplitDomainsDirect() {
        val config = JSONObject(SingBoxConfigGenerator().generateConfig(server, filesDir = "/data", splitDomains = listOf("example.com"), routingMode = "global", bypassRussia = true))
        val rules = config.getJSONObject("route").getJSONArray("rules")
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            assertFalse("Global must not apply domain bypass rules", rule.has("domain_suffix"))
            assertFalse("Global must not apply geo bypass rules", rule.has("rule_set"))
        }
        assertEquals("test", config.getJSONObject("route").getString("final"))
        assertEquals("mixed-in", rules.getJSONObject(0).getJSONArray("inbound").getString(0))
        assertEquals("test", rules.getJSONObject(0).getString("outbound"))
    }

    @Test fun oversizedVpnLinkIsRejectedWithoutAllocation() = runBlocking {
        val malicious = "vpn://" + android.util.Base64.encodeToString(byteArrayOf(0x7f, -1, -1, -1, 0), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        val result = SubscriptionClient().fetchSubscription(malicious)
        assertTrue(result.servers.isEmpty())
        assertTrue(result.error.isNotEmpty())
    }

    @Test fun directVlessProfileIsImportedWithoutHttpFetch() = runBlocking {
        val result = SubscriptionClient().fetchSubscription("vless://test-user@127.0.0.1:443#Direct")
        assertTrue(result.error.isEmpty())
        assertEquals(1, result.servers.size)
        assertEquals("Direct", result.servers.single().name)
    }

    @Test fun removingSubscriptionPreservesOtherSourcesAndRejectsStaleRefresh() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = com.example.service.SettingsStore(context)
        val url = "https://example.invalid/sub/one"
        val original = org.json.JSONArray().apply {
            put(JSONObject().put("id", "one").put("url", url))
            put(JSONObject().put("id", "two").put("url", "https://example.invalid/sub/two"))
        }.toString()
        store.setSubscriptionUrl(url)
        store.setCachedServersJson(original)
        assertTrue(store.removeSubscription(url))
        assertEquals("", store.getBlockingSubscriptionUrl())
        val remaining = org.json.JSONArray(store.getBlockingCachedServersJson())
        assertEquals(1, remaining.length())
        assertEquals("two", remaining.getJSONObject(0).getString("id"))
        assertFalse(store.saveRefreshIfUnchanged(url, original, "[]", 0))
        assertEquals(1, org.json.JSONArray(store.getBlockingCachedServersJson()).length())
    }

    @Test fun refreshDoesNotOverwriteAConcurrentImport() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = com.example.service.SettingsStore(context)
        val url = "https://example.invalid/sub/current"
        store.setSubscriptionUrl(url)
        store.setCachedServersJson("[{\"id\":\"new-import\"}]")
        assertFalse(store.saveRefreshIfUnchanged(url, "[]", "[]", 0))
        assertEquals("new-import", org.json.JSONArray(store.getBlockingCachedServersJson()).getJSONObject(0).getString("id"))
    }

    @Test fun importRegistersSubscriptionOnlyWhenCacheWriteSucceeds() = runBlocking {
        val store = com.example.service.SettingsStore(ApplicationProvider.getApplicationContext())
        val url = "https://example.invalid/new"
        store.setCachedServersJson("[{\"id\":\"existing\"}]")
        assertFalse(store.saveCacheIfUnchanged("[]", "[]", 0, url))
        assertFalse(url in store.getBlockingSubscriptionUrls())
        assertTrue(store.saveCacheIfUnchanged("[{\"id\":\"existing\"}]", "[]", 0, url))
        assertTrue(url in store.getBlockingSubscriptionUrls())
    }

    @Test fun clearingCacheRemovesSavedSubscriptionsAndPreventsStaleRefresh() = runBlocking {
        val store = com.example.service.SettingsStore(ApplicationProvider.getApplicationContext())
        val url = "https://example.invalid/sub"
        store.setSubscriptionUrl(url)
        store.setCachedServersJson("[{\"id\":\"server\",\"url\":\"$url\"}]")
        val before = store.getBlockingCachedServersJson()
        store.clearSavedServersAndSubscriptions()
        assertEquals(emptySet<String>(), store.getBlockingSubscriptionUrls())
        assertEquals("", store.getBlockingSubscriptionUrl())
        assertEquals(0, org.json.JSONArray(store.getBlockingCachedServersJson()).length())
        assertFalse(store.saveRefreshIfUnchanged(url, before, before, 1))
    }

    @Test fun removingSubscriptionWorksWithoutCachedServers() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = com.example.service.SettingsStore(context)
        val url = "https://example.invalid/sub/empty"
        store.setSubscriptionUrl(url)
        store.setCachedServersJson("")
        assertTrue(store.removeSubscription(url))
        assertEquals("", store.getBlockingSubscriptionUrl())
        assertEquals(0, org.json.JSONArray(store.getBlockingCachedServersJson()).length())
    }

    @Test fun migratesAllLegacySubscriptionsFromCache() {
        val store = com.example.service.SettingsStore(ApplicationProvider.getApplicationContext())
        val first = "https://example.invalid/one"
        val second = "https://example.invalid/two"
        val prefs = mutablePreferencesOf(
            com.example.service.SettingsStore.KEY_SUBSCRIPTION_URL to first,
            com.example.service.SettingsStore.KEY_CACHED_SERVERS_JSON to org.json.JSONArray().put(JSONObject().put("url", second)).put(JSONObject().put("url", "vpn://private" )).toString()
        )
        val method = store.javaClass.getDeclaredMethod("subscriptionUrlsFrom", Preferences::class.java).apply { isAccessible = true }
        assertEquals(setOf(first, second), method.invoke(store, prefs))
        prefs[com.example.service.SettingsStore.KEY_SUBSCRIPTION_URLS] = emptySet()
        assertEquals(emptySet<String>(), method.invoke(store, prefs))
    }

    @Test fun refreshesOtherSubscriptionWhenOneFails() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val socket = java.net.ServerSocket(0).apply { soTimeout = 10000 }
        val good = "http://127.0.0.1:${socket.localPort}/a"
        val bad = "http://127.0.0.1:${socket.localPort}/b"
        val responder = kotlin.concurrent.thread(isDaemon = true) {
            repeat(2) {
                socket.accept().use { peer ->
                    val reader = peer.getInputStream().bufferedReader()
                    val line = reader.readLine().orEmpty()
                    while (true) { val h = reader.readLine() ?: break; if (h.isEmpty()) break }
                    val success = line.contains(" /a ")
                    val body = if (success) "vless://test@127.0.0.1:443#New" else "unavailable"
                    peer.getOutputStream().apply {
                        write("HTTP/1.1 ${if (success) "200 OK" else "503 Unavailable"}\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body".toByteArray())
                        flush()
                    }
                }
            }
        }
        try {
            val store = com.example.service.SettingsStore(context)
            store.setSubscriptionUrl(good)
            store.setSubscriptionUrl(bad)
            store.setCachedServersJson(org.json.JSONArray().apply {
                put(JSONObject().put("id", "old-good").put("url", good).put("name", "Old").put("protocol", "VLESS").put("address", "198.51.100.1").put("port", 443))
                put(JSONObject().put("id", "old-bad").put("url", bad).put("name", "Other").put("protocol", "VLESS").put("address", "198.51.100.2").put("port", 443))
            }.toString())
            val worker = androidx.work.testing.TestListenableWorkerBuilder<com.example.service.SubscriptionRefreshWorker>(context).build()
            assertEquals(androidx.work.ListenableWorker.Result.retry(), worker.doWork())
            val result = org.json.JSONArray(store.getBlockingCachedServersJson())
            val rows = (0 until result.length()).map { result.getJSONObject(it) }
            assertTrue(rows.any { it.optString("name") == "New" })
            assertTrue(rows.any { it.optString("id") == "old-bad" })
            assertEquals(setOf(good, bad), store.getBlockingSubscriptionUrls())
        } finally { socket.close(); responder.join(1000) }
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
            assertTrue(rows.any { it.optString("id") == "old" && it.optString("uuid") == "new-credential" })
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
