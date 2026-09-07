package com.example.service

import android.os.Build

object CloexecNative {
    init {
        try {
            System.loadLibrary("cloexec")
        } catch (_: Throwable) {
        }
    }

    private external fun nativeClearCloseOnExec(fd: Int): Int

    fun supports(): Boolean =
        Build.SUPPORTED_ABIS.any { it.startsWith("arm64") || it.startsWith("x86") || it.startsWith("armeabi") }

    // Clears FD_CLOEXEC on the given raw fd via a native fcntl() so it survives
    // the fork+exec that launches the sing-box child process. Returns true on
    // success (or if the lib is unavailable and we can't verify).
    fun clearCloseOnExec(fd: Int): Boolean {
        if (fd <= 0) return false
        return try {
            nativeClearCloseOnExec(fd) == 0
        } catch (_: Throwable) {
            false
        }
    }
}
