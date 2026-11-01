package com.example.service

/** Limits automatic recovery when the probe target or network is temporarily unavailable. */
class TunnelRecoveryPolicy {
    private var failures = 0
    private val attempts = ArrayDeque<Long>()

    fun observe(reachable: Boolean, networkValidated: Boolean, enabled: Boolean, nowMs: Long): Boolean {
        if (reachable) {
            failures = 0
            return false
        }
        if (!networkValidated || !enabled) {
            failures = 0
            return false
        }
        failures++
        if (failures < 3) return false
        failures = 0
        while (attempts.isNotEmpty() && nowMs - attempts.first() >= 15 * 60_000L) attempts.removeFirst()
        if (attempts.size >= 3) return false
        attempts.addLast(nowMs)
        return true
    }
}
