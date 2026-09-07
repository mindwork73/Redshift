package com.example.service

import android.util.Log

object SingBoxNative {
    private const val TAG = "SingBoxNative"

    init {
        try {
            System.loadLibrary("singbox")
        } catch (t: Throwable) {
            Log.e(TAG, "loadLibrary failed: ${t.message}")
        }
    }

    external fun start(tunFd: Int): Int
    external fun stop()
    external fun alive(): Int
}