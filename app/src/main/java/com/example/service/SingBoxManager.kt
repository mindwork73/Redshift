package com.example.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

class SingBoxManager(private val context: Context) {

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var configPath: String = ""

    // All native start/stop transitions go through this monitor so that a racing
    // disconnect (which calls stop()) can never overlap with an in-progress
    // startWithTunFd(). On some devices concurrent native start/stop deadlocked
    // or crashed the process with no Java stack (probable native SEGV).
    private val nativeLock = Any()

    companion object {
        const val SOCKS_PORT = 10808
        const val MIXED_PORT = 10809
        private const val SINGBOX_VERSION = "1.14.0-lx.26-jni"
        private const val NATIVE_FAIL = -8
    }

    private fun debugLog(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val line = "$ts [SB] $msg\n"
        try {
            val f = java.io.File(context.filesDir, "redshift_debug.log")
            f.appendText(line)
        } catch (_: Exception) {}
        Log.e("SingBoxManager", msg)
    }

    fun getConfigPath(): String {
        return File(context.filesDir, "singbox/config.json").absolutePath
    }

    fun getRunLogPath(): String {
        return File(context.filesDir, "singbox/run.log").absolutePath
    }

    fun getErrPath(): String {
        return File(context.filesDir, "singbox/err.txt").absolutePath
    }

    private fun prepareFiles(configJson: String) {
        configPath = getConfigPath()
        val configFile = File(configPath)
        configFile.parentFile?.mkdirs()
        configFile.writeText(configJson)
        try { File(getRunLogPath()).writeText("") } catch (_: Exception) {}
        try { File(getErrPath()).writeText("") } catch (_: Exception) {}
    }

    fun start(configJson: String): Boolean = startWithTunFd(configJson, 0)

    suspend fun ensureBinary(): Boolean = withContext(Dispatchers.IO) {
        val loaded = try {
            SingBoxNative.alive() >= 0
        } catch (_: Throwable) {
            false
        }
        if (!loaded) debugLog("native library not loadable")
        loaded
    }

    fun startWithTunFd(configJson: String, tunFd: Int): Boolean = synchronized(nativeLock) {
        stop()

        prepareFiles(configJson)
        val cfg = File(getConfigPath())
        if (!cfg.exists() || cfg.length() == 0L) {
            debugLog("config not written")
            return@synchronized false
        }

        debugLog("calling native start(tunFd=$tunFd), config=${getConfigPath()}")
        val rc = SingBoxNative.start(tunFd)
        if (rc != 0) {
            val err = readErrFile()
            debugLog("native start failed rc=$rc: $err")
            return@synchronized false
        }

        // Give the core a moment to come up, then confirm.
        val deadline = System.currentTimeMillis() + 5000
        var alive = false
        while (System.currentTimeMillis() < deadline) {
            alive = isRunning()
            if (alive) break
            Thread.sleep(100)
        }
        debugLog("sing-box alive=$alive (tun fd=$tunFd) rc=$rc")
        startMonitor()
        alive
    }

    fun stop() {
        synchronized(nativeLock) {
            monitorJob?.cancel()
            monitorJob = null
            SingBoxNative.stop()
        }
    }

    fun isRunning(): Boolean = SingBoxNative.alive() == 1

    private fun readErrFile(): String {
        return try {
            val f = File(getErrPath())
            if (f.exists()) f.readText().takeLast(2000) else "err.txt missing"
        } catch (e: Exception) {
            "err read failed: ${e.message}"
        }
    }

    fun getLog(): List<String> {
        return try {
            val f = File(getRunLogPath())
            if (f.exists()) f.readLines().takeLast(20) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun startMonitor() {
        monitorJob = scope.launch {
            var lastLogSize = 0L
            while (isActive) {
                if (SingBoxNative.alive() != 1) {
                    val err = readErrFile()
                    debugLog("sing-box dead. err.txt: $err")
                    break
                }
                try {
                    val f = File(getRunLogPath())
                    if (f.exists()) {
                        val len = f.length()
                        if (len > lastLogSize && len - lastLogSize < 200_000) {
                            val tail = f.readLines().takeLast(5)
                            for (l in tail) debugLog("[SB-LOG] $l")
                        }
                        lastLogSize = len
                    }
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }
}