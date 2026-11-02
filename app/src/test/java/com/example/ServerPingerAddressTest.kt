package com.example

import com.example.service.ServerPinger
import org.junit.Assert.*
import org.junit.Test

class ServerPingerAddressTest {
    @Test fun encodesNamesAndIpAddressesForSocks() {
        val domain = ServerPinger.socksAddress("example.org")!!
        assertEquals(3, domain[0].toInt())
        assertEquals("example.org", String(domain.copyOfRange(2, domain.size)))
        assertArrayEquals(byteArrayOf(1, 127, 0, 0, 1), ServerPinger.socksAddress("127.0.0.1"))
        assertEquals(17, ServerPinger.socksAddress("2001:db8::1")!!.size)
        assertNull(ServerPinger.socksAddress("999.1.1.1"))
    }
}
