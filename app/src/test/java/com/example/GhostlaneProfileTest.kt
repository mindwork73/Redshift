package com.example

import com.example.service.OlcrtcUri
import com.example.service.SubscriptionClient
import com.example.service.SingBoxConfigGenerator
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GhostlaneProfileTest {
    private val key = "ab".repeat(32)
    private val jitsi = "olcrtc://jitsi?datachannel@https://meet.example.org/room#${key}\$Ghostlane NL"
    private val jazz = "olcrtc://salutejazz?datachannel@abc:def#${key}\$Jazz"
    private val oldJitsi = "olcrtc://jitsi?datachannel@https://meet.example.org/legacy#${key}\$Legacy NL"

    @Test fun routingIsExplicitAndLegacyIsUnchanged() {
        assertTrue(OlcrtcUri.parse(jitsi)!!.ghostlane)
        assertTrue(OlcrtcUri.parse(jazz)!!.ghostlane)
        assertTrue(OlcrtcUri.parse(oldJitsi.replace("olcrtc://", "ghostlane://"))!!.ghostlane)
        assertFalse(OlcrtcUri.parse(oldJitsi)!!.ghostlane)
        assertEquals(key, OlcrtcUri.parse(jitsi)!!.key)
    }

    @Test fun directImportKeepsProfileAndUsesSeparateSocksPort() = runBlocking {
        for (link in listOf(jitsi, jazz, oldJitsi)) {
            val result = SubscriptionClient().fetchSubscription(link)
            assertEquals("", result.error)
            assertEquals(1, result.servers.size)
            val server = result.servers.single()
            assertEquals(link, server.extra)
            val out = JSONObject(SingBoxConfigGenerator().generateConfig(server)).getJSONArray("outbounds").getJSONObject(0)
            assertEquals(8788, out.getInt("server_port"))
        }
    }

    @Test fun deviceSuffixIsKeptApartFromEncryptionKey() {
        val profile = OlcrtcUri.parse("ghostlane://jitsi?datachannel@https://example.org/r#${key}%device-42\$test")!!
        assertEquals(key, profile.key)
        assertEquals("device-42", profile.deviceId)
    }
}
