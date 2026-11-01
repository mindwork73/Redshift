package com.example

import com.example.service.TunnelRecoveryPolicy
import org.junit.Assert.*
import org.junit.Test

class TunnelRecoveryPolicyTest {
    @Test fun requiresThreeFailuresAndValidatedNetwork() {
        val p = TunnelRecoveryPolicy()
        assertFalse(p.observe(false, true, true, 0))
        assertFalse(p.observe(false, true, true, 60_000))
        assertFalse(p.observe(false, false, true, 120_000))
        assertFalse(p.observe(false, true, true, 180_000))
        assertFalse(p.observe(false, true, true, 240_000))
        assertTrue(p.observe(false, true, true, 300_000))
    }

    @Test fun successAndDisabledOptionClearFailures() {
        val p = TunnelRecoveryPolicy()
        assertFalse(p.observe(false, true, true, 0))
        assertFalse(p.observe(true, true, true, 1))
        assertFalse(p.observe(false, true, true, 2))
        assertFalse(p.observe(false, true, false, 3))
        assertFalse(p.observe(false, true, true, 4))
        assertFalse(p.observe(false, true, true, 5))
        assertTrue(p.observe(false, true, true, 6))
    }

    @Test fun limitsRecoveryAttemptsAcrossSuccessiveSessions() {
        val p = TunnelRecoveryPolicy()
        fun failuresAt(t: Long): Boolean {
            p.observe(false, true, true, t)
            p.observe(false, true, true, t + 1)
            return p.observe(false, true, true, t + 2)
        }
        assertTrue(failuresAt(0))
        assertTrue(failuresAt(10_000))
        assertTrue(failuresAt(20_000))
        assertFalse(failuresAt(30_000))
        assertTrue(failuresAt(15 * 60_000L + 2))
    }
}
