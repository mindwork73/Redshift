package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TrafficMonitor {

    data class Snapshot(val upBps: Long, val downBps: Long)

    interface Listener {
        fun onTrafficUpdate(upBps: Long, downBps: Long)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var client: OkHttpClient? = null

    private fun getClient(): OkHttpClient {
        val existing = client
        if (existing != null) return existing
        val created = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        client = created
        return created
    }

    fun start(listener: Listener) {
        stop()
        job = scope.launch {
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("http://127.0.0.1:${SingBoxConfigGenerator.CLASH_API_PORT}/traffic")
                        .build()
                    val response = getClient().newCall(request).execute()
                    response.use { resp ->
                        if (!resp.isSuccessful) return@launch
                        val source = resp.body?.source() ?: return@launch
                        while (isActive && !source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.isBlank()) continue
                            try {
                                val json = org.json.JSONObject(line)
                                listener.onTrafficUpdate(json.optLong("up", 0), json.optLong("down", 0))
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
