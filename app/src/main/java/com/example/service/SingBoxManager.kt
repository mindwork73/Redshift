package com.example.service

import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class SingBoxManager(private val context: Context) {

    private var process: Process? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var configPath: String = ""
    private var binaryPath: String = ""

    companion object {
        const val SOCKS_PORT = 10808
        const val MIXED_PORT = 10809
        private const val SINGBOX_VERSION = "1.13.13"
    }

    fun getBinaryDir(): String {
        return context.getDir("singbox", Context.MODE_PRIVATE).absolutePath
    }

    fun getBinaryPath(): String {
        val abi = getAbi()
        return File(getBinaryDir(), abi).absolutePath + "/sing-box"
    }

    fun getConfigPath(): String {
        return File(context.filesDir, "singbox/config.json").absolutePath
    }

    suspend fun ensureBinary(): Boolean = withContext(Dispatchers.IO) {
        val binaryFile = File(getBinaryPath())
        val versionFile = File(getBinaryDir(), "version.txt")

        val currentVersion = try { versionFile.readText().trim() } catch (_: Exception) { "" }
        if (binaryFile.exists() && binaryFile.length() > 1000000 && currentVersion == SINGBOX_VERSION) return@withContext true

        try {
            val abi = getAbi()
            val assetPath = "singbox/$abi/sing-box"

            binaryFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(binaryFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            binaryFile.setExecutable(true)
            versionFile.writeText(SINGBOX_VERSION)

            binaryFile.exists() && binaryFile.length() > 1000000
        } catch (e: Exception) {
            false
        }
    }

    private fun getAbi(): String {
        val abis = Build.SUPPORTED_ABIS
        for (abi in abis) {
            if (abi.startsWith("arm64")) return "arm64-v8a"
            if (abi.startsWith("x86_64")) return "x86_64"
            if (abi.startsWith("x86")) return "x86"
            if (abi.startsWith("armeabi")) return "armeabi-v7a"
        }
        return "arm64-v8a"
    }

    fun start(configJson: String): Boolean {
        stop()

        configPath = getConfigPath()
        binaryPath = getBinaryPath()

        val configFile = File(configPath)
        configFile.parentFile?.mkdirs()
        configFile.writeText(configJson)

        try {
            val dataDir = context.getDir("singbox", Context.MODE_PRIVATE).absolutePath + "/data"
            val pb = ProcessBuilder(
                binaryPath, "run",
                "-c", configPath,
                "-D", dataDir
            )
            pb.directory(File(dataDir).parentFile)
            pb.environment()["HOME"] = context.filesDir.absolutePath
            pb.redirectErrorStream(true)

            process = pb.start()
            startMonitor()

            return process?.isAlive == true
        } catch (e: Exception) {
            process = null
            return false
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        process?.let { p ->
            p.destroyForcibly()
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        }
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true

    fun getLog(): List<String> {
        return try {
            process?.inputStream?.bufferedReader()?.readLines()?.takeLast(20) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun startMonitor() {
        monitorJob = scope.launch {
            while (isActive) {
                if (process?.isAlive == false) {
                    break
                }
                delay(2000)
            }
        }
    }
}
