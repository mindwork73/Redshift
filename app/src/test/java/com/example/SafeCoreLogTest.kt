package com.example

import com.example.service.SafeCoreLog
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SafeCoreLogTest {
    @Test fun exportsOnlyCategoriesAndTimestampsWithoutSensitiveText() {
        val file = File.createTempFile("core", ".log")
        try {
            file.writeText("+0400 2026-11-02 13:00:00 ERROR https://private.example/sub/secret key=secret context deadline exceeded\n" +
                "2026-11-02 13:00:01 WARN user=secret connection refused\n" +
                "2026-11-02 13:00:02 INFO secret without known event\n")
            val events = SafeCoreLog.read(file)
            assertEquals(listOf("2026-11-02 13:00:00 timeout", "2026-11-02 13:00:01 connection_refused"), events)
            assertFalse(events.joinToString().contains("secret"))
            assertFalse(events.joinToString().contains("private.example"))
        } finally { file.delete() }
    }

    @Test fun ignoresUnrecognizedAndDoesNotInventHandshake() {
        assertNull(SafeCoreLog.classify("wireguard connected to 192.0.2.10"))
        assertEquals("handshake_related_error", SafeCoreLog.classify("handshake failed: secret"))
        assertEquals("connection_refused", SafeCoreLog.classify("report handshake success: connection refused"))
    }
}
