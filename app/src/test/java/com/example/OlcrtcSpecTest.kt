package com.example

import com.example.service.OlcrtcUri
import com.example.service.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the two pure pieces of the olcRTC path that decide whether a connection can
 * even be attempted: the `olcrtc://` URI parser (a wrong spec means the runtime never joins
 * the room) and the updater's version comparison (a wrong ordering means users never receive
 * the fix).
 *
 * The URI under test is the live one the server hands out, copied verbatim from
 * `/sub/<user>` on NL.
 */
class OlcrtcSpecTest {

    private val liveUri =
        "olcrtc://telemost?vp8channel<vp8-fps=30&vp8-batch=64>@85010346785956" +
            "#dec77c7e1c4c3d3635f6439e41b6bd30f7c9710262187859ecc2428ee294c130\$NL / Telemost"

    @Test
    fun parsesTheLiveTelemostUri() {
        val spec = OlcrtcUri.parse(liveUri)

        assertNotNull("the live server URI must parse", spec)
        assertEquals("telemost", spec!!.provider)
        assertEquals("vp8channel", spec.transport)
        assertEquals("85010346785956", spec.room)
        assertEquals(
            "dec77c7e1c4c3d3635f6439e41b6bd30f7c9710262187859ecc2428ee294c130",
            spec.key
        )
        assertEquals("NL / Telemost", spec.label)
        assertEquals(30, spec.vp8Fps)
        assertEquals(64, spec.vp8Batch)
    }

    @Test
    fun rejectsBrokenUrisSoTheServiceCanReportTheReason() {
        // The service turns a null spec into "не удалось разобрать olcrtc:// ссылку", which
        // is what keeps a malformed link from silently becoming a dead tunnel.
        assertNull(OlcrtcUri.parse("vless://user@host:443"))
        assertNull("no key separator", OlcrtcUri.parse("olcrtc://telemost?vp8channel@room"))
        assertNull("no room separator", OlcrtcUri.parse("olcrtc://telemostvp8channel#key"))
        assertNull("empty transport", OlcrtcUri.parse("olcrtc://telemost?@room#key"))
        assertNull("empty key", OlcrtcUri.parse("olcrtc://telemost?vp8channel@room#"))
        assertNull("empty room", OlcrtcUri.parse("olcrtc://telemost?vp8channel@#key"))
    }

    @Test
    fun detectsTheSchemeRegardlessOfLeadingWhitespace() {
        assertTrue(OlcrtcUri.isOlcrtcUri("  olcrtc://telemost?vp8channel@room#key"))
        assertFalse(OlcrtcUri.isOlcrtcUri("https://example.com/sub"))
    }

    @Test
    fun appliesVp8DefaultsWhenTheParamsAreMissingOrInsane() {
        val noParams = OlcrtcUri.parse("olcrtc://telemost?vp8channel@room#key")
        assertNotNull(noParams)
        assertEquals(30, noParams!!.vp8Fps)
        assertEquals(64, noParams.vp8Batch)

        // Out-of-range values are clamped instead of breaking the transport.
        val clamped = OlcrtcUri.parse("olcrtc://telemost?vp8channel<vp8-fps=999&vp8-batch=0>@room#key")
        assertNotNull(clamped)
        assertEquals(120, clamped!!.vp8Fps)
        assertEquals(64, clamped.vp8Batch)
    }

    @Test
    fun versionOrderingPutsTheFixAboveEveryPublishedTrack() {
        // The distribution tracks in the repo: 0.1.x (releases), 0.2.21 (old pre-release),
        // 0.3.0 (this fix). A wrong comparison would silently hide the update.
        assertTrue(UpdateChecker.isNewerVersion("0.3.0", "0.1.9"))
        assertTrue(UpdateChecker.isNewerVersion("0.3.0", "0.2.21"))
        assertTrue(UpdateChecker.isNewerVersion("0.3.0", "0.1.8"))
        assertFalse(UpdateChecker.isNewerVersion("0.1.9", "0.3.0"))
        assertFalse(UpdateChecker.isNewerVersion("0.3.0", "0.3.0"))
        // Suffixes/tags must not flip the result.
        assertTrue(UpdateChecker.isNewerVersion("v0.3.0", "v0.2.21"))
        assertFalse(UpdateChecker.isNewerVersion("v0.3.0", "0.3.0"))
    }
}
