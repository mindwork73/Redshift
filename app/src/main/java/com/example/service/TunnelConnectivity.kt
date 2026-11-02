package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/** An unsuccessful probe is inconclusive: the target itself might be blocked. */
object TunnelConnectivity {
    data class Result(val reachable: Boolean, val outcome: String, val elapsedMs: Long)

    suspend fun check(proxyPort: Int = SingBoxConfigGenerator.MIXED_PORT, url: String = "https://www.cloudflare.com/cdn-cgi/trace"): Boolean =
        checkDetailed(proxyPort, url).reachable

    suspend fun checkDetailed(proxyPort: Int = SingBoxConfigGenerator.MIXED_PORT, url: String = "https://www.cloudflare.com/cdn-cgi/trace"): Result = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        fun elapsed() = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        try {
            val client = OkHttpClient.Builder()
                .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", proxyPort)))
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .callTimeout(12, TimeUnit.SECONDS)
                .build()
            client.newCall(Request.Builder().url(url).get().build()).execute().use {
                Result(it.isSuccessful, "http_${it.code}", elapsed())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val outcome = when (e) {
                is java.net.SocketTimeoutException -> "timeout"
                is java.net.UnknownHostException -> "dns_failure"
                is java.net.ConnectException -> "connect_failure"
                is javax.net.ssl.SSLException -> "tls_failure"
                is java.net.SocketException -> "socket_failure"
                is java.io.InterruptedIOException -> "interrupted_or_timeout"
                is java.io.IOException -> "io_failure"
                else -> "unexpected_failure"
            }
            Result(false, outcome, elapsed())
        }
    }
}
