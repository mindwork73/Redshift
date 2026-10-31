package com.example

import com.example.service.SubscriptionClient
import com.example.ui.RedShiftState
import com.example.ui.Server
import com.example.ui.ConnectionState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubscriptionCompatibilityTest {
    @Test fun refreshKeepsActiveServerUntilDisconnect() {
        val oldState = RedShiftState.connectionState
        val oldServers = RedShiftState.servers.toList()
        val url = "https://example.invalid/sub"
        val active = Server(id = "old", flag = "", name = "NL", protocol = "VLESS", address = "198.51.100.1", port = 443, subscriptionUrl = url)
        val replacement = active.copy(id = "new", address = "198.51.100.2")
        try {
            RedShiftState.servers.clear()
            RedShiftState.servers.add(replacement)
            RedShiftState.connectionState = ConnectionState.CONNECTED
            val method = RedShiftState.javaClass.getDeclaredMethod("keepActiveServerDuringImport", Server::class.java, String::class.java).apply { isAccessible = true }
            method.invoke(RedShiftState, active, url)
            assertEquals(listOf(active), RedShiftState.servers.toList())
        } finally {
            RedShiftState.connectionState = oldState
            RedShiftState.servers.clear()
            RedShiftState.servers.addAll(oldServers)
        }
    }
    @Test fun parsesBase64SubscriptionsAndJsonArrays() {
        val method = SubscriptionClient::class.java.getDeclaredMethod("parseSubscriptionBody", String::class.java).apply { isAccessible = true }
        val client = SubscriptionClient()
        val body = "vless://one@nl.example.org:443#Netherlands\ntrojan://pass@de.example.org:443#Germany"
        val base64 = Base64.encodeToString(body.toByteArray(), Base64.NO_WRAP)
        val servers = method.invoke(client, base64) as List<*>
        assertEquals(2, servers.size)
        val json = "[{\"protocol\":\"vless\",\"remark\":\"test\",\"address\":\"example.org\",\"port\":443,\"id\":\"uuid\"}]"
        assertEquals(1, (method.invoke(client, json) as List<*>).size)
    }

    @Test fun differentSubscriptionsDoNotEraseEachOther() {
        val method = RedShiftState::class.java.getDeclaredMethod("dedupeServerArray", JSONArray::class.java).apply { isAccessible = true }
        fun row(url: String) = JSONObject().put("url", url).put("id", url).put("name", "NL")
            .put("protocol", "VLESS").put("address", "example.org").put("port", 443)
        val input = JSONArray().put(row("https://one.invalid/sub")).put(row("https://two.invalid/sub"))
            .put(row("https://one.invalid/sub"))
        val result = method.invoke(RedShiftState, input) as JSONArray
        assertEquals(2, result.length())
    }
    @Test fun restoresCountryFlagFromServerNameAndKeepsUnknownNeutral() {
        val nl = Server(id = "a", flag = "🌐", name = "Ghostlane NL", protocol = "OLCRTC", address = "room", port = 0)
        assertEquals("🇳🇱", RedShiftState.serverFlag(nl))
        assertEquals("", RedShiftState.serverFlag(nl.copy(name = "Private server")))
        assertEquals("🇩🇪", RedShiftState.serverFlag(nl.copy(name = "🇩🇪 Private server")))
    }
    @Test fun normalizesOnlyRedpillApexSubscriptionHost() {
        assertEquals("https://www.redpillcloud.ru/sub/example?x=1", SubscriptionClient.normalizeSubscriptionUrl("https://redpillcloud.ru/sub/example?x=1"))
        assertEquals("https://redpillcloud.ru.evil.invalid/sub/example", SubscriptionClient.normalizeSubscriptionUrl("https://redpillcloud.ru.evil.invalid/sub/example"))
        assertEquals("https://www.redpillcloud.ru/sub/example", SubscriptionClient.normalizeSubscriptionUrl("https://www.redpillcloud.ru/sub/example"))
    }
}
