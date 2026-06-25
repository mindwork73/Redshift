package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class SubServer(
    val id: String,
    val name: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val flag: String,
    val uuid: String = "",
    val password: String = "",
    val flow: String = "",
    val encryption: String = "none",
    val network: String = "tcp",
    val tls: Boolean = false,
    val sni: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val fingerprint: String = "chrome"
)

data class SubscriptionResult(
    val url: String,
    val servers: List<SubServer>,
    val error: String? = null
)

class SubscriptionClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchSubscription(url: String): SubscriptionResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RedShift/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext SubscriptionResult(url, emptyList(), "Empty response")

            val servers = parseSubscriptionBody(body)
            SubscriptionResult(url, servers)
        } catch (e: Exception) {
            SubscriptionResult(url, emptyList(), e.message ?: "Unknown error")
        }
    }

    private fun parseSubscriptionBody(body: String): List<SubServer> {
        val trimmed = body.trim()

        if (trimmed.startsWith("{")) {
            return parseJsonSubscription(trimmed)
        }

        return parsePlainTextSubscription(trimmed)
    }

    private fun parseJsonSubscription(json: String): List<SubServer> {
        val servers = mutableListOf<SubServer>()

        try {
            val root = JSONArray(json)
            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val protocol = obj.optString("protocol", "vless")
                val remark = obj.optString("remark", "Node ${i + 1}")
                val address = obj.optString("address", "")
                val port = obj.optInt("port", 443)
                val id = obj.optString("id", "")

                servers.add(
                    SubServer(
                        id = "sub_${i}_${System.currentTimeMillis()}",
                        name = remark,
                        protocol = protocol.uppercase(),
                        address = address,
                        port = port,
                        flag = getFlagForProtocol(protocol),
                        uuid = id,
                        flow = obj.optString("flow", ""),
                        encryption = obj.optString("encryption", "none"),
                        network = obj.optString("network", "tcp"),
                        tls = obj.optBoolean("tls", false),
                        sni = obj.optString("sni", ""),
                        publicKey = obj.optString("publicKey", ""),
                        shortId = obj.optString("shortId", ""),
                        fingerprint = obj.optString("fingerprint", "chrome")
                    )
                )
            }
        } catch (_: Exception) {}

        return servers
    }

    private fun parsePlainTextSubscription(text: String): List<SubServer> {
        val servers = mutableListOf<SubServer>()

        val lines = text.lines().filter { it.isNotBlank() }

        for ((index, line) in lines.withIndex()) {
            try {
                val server = parseProxyUri(line, index)
                if (server != null) servers.add(server)
            } catch (_: Exception) {}
        }

        return servers
    }

    private fun parseProxyUri(uri: String, index: Int): SubServer? {
        return when {
            uri.startsWith("vless://") -> parseVlessUri(uri, index)
            uri.startsWith("vmess://") -> parseVmessUri(uri, index)
            uri.startsWith("trojan://") -> parseTrojanUri(uri, index)
            uri.startsWith("ss://") -> parseShadowsocksUri(uri, index)
            uri.startsWith("hy2://") || uri.startsWith("hysteria2://") -> parseHysteriaUri(uri, index)
            else -> null
        }
    }

    private fun parseVlessUri(uri: String, index: Int): SubServer {
        val withoutScheme = uri.removePrefix("vless://")
        val uuid = withoutScheme.substringBefore("@")
        val rest = withoutScheme.substringAfter("@", "")
        val addressPort = rest.substringBefore("?")
        val params = rest.substringAfter("?", "").split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }

        val host = addressPort.substringBefore(":")
        val port = addressPort.substringAfter(":").substringBefore("?").toIntOrNull() ?: 443
        val remark = params["remark"]?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "VLESS Node ${index + 1}"

        return SubServer(
            id = "vless_${index}_${System.currentTimeMillis()}",
            name = remark,
            protocol = "VLESS+Reality",
            address = host,
            port = port,
            flag = "🇳🇱",
            uuid = uuid,
            flow = params["flow"] ?: "",
            encryption = params["encryption"] ?: "none",
            network = params["type"] ?: "tcp",
            tls = params["security"] == "reality" || params["security"] == "tls",
            sni = params["sni"] ?: "",
            publicKey = params["pbk"] ?: "",
            shortId = params["sid"] ?: "",
            fingerprint = params["fp"] ?: "chrome"
        )
    }

    private fun parseVmessUri(uri: String, index: Int): SubServer? {
        return try {
            val b64 = uri.removePrefix("vmess://")
            val decoded = String(java.util.Base64.getUrlDecoder().decode(b64), Charsets.UTF_8)
            val json = org.json.JSONObject(decoded)
            SubServer(
                id = "vmess_${index}_${System.currentTimeMillis()}",
                name = json.optString("ps", "VMess Node ${index + 1}"),
                protocol = "VMess",
                address = json.optString("add", ""),
                port = json.optInt("port", 443),
                flag = "🇳🇱",
                uuid = json.optString("id", ""),
                encryption = json.optString("scy", "auto"),
                network = json.optString("net", "tcp"),
                tls = json.optString("tls", "") == "tls",
                sni = json.optString("sni", "")
            )
        } catch (_: Exception) { null }
    }

    private fun parseTrojanUri(uri: String, index: Int): SubServer {
        val withoutScheme = uri.removePrefix("trojan://")
        val password = withoutScheme.substringBefore("@")
        val rest = withoutScheme.substringAfter("@", "")
        val addressPort = rest.substringBefore("?")
        val params = rest.substringAfter("?", "").split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }

        val host = addressPort.substringBefore(":")
        val port = addressPort.substringAfter(":").substringBefore("?").toIntOrNull() ?: 443
        val remark = params["remark"]?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Trojan Node ${index + 1}"

        return SubServer(
            id = "trojan_${index}_${System.currentTimeMillis()}",
            name = remark,
            protocol = "Trojan+TLS",
            address = host,
            port = port,
            flag = "🇪🇺",
            password = password,
            tls = true,
            sni = params["sni"] ?: host
        )
    }

    private fun parseShadowsocksUri(uri: String, index: Int): SubServer? {
        return try {
            val withoutScheme = uri.removePrefix("ss://")
            val afterScheme = withoutScheme
            val atIndex = afterScheme.indexOf("@")
            val methodPass: String
            val hostPort: String

            if (atIndex > 0) {
                methodPass = String(java.util.Base64.getUrlDecoder().decode(afterScheme.substring(0, atIndex)), Charsets.UTF_8)
                hostPort = afterScheme.substring(atIndex + 1).split("#").first()
            } else {
                val decoded = String(java.util.Base64.getUrlDecoder().decode(afterScheme), Charsets.UTF_8)
                val colonIndex = decoded.indexOf(":")
                val lastAtIndex = decoded.lastIndexOf("@")
                if (lastAtIndex < 0) return null
                methodPass = decoded.substring(0, lastAtIndex)
                hostPort = decoded.substring(lastAtIndex + 1).split("#").first()
            }

            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":").toIntOrNull() ?: 443
            val method = methodPass.substringBefore(":")
            val password = methodPass.substringAfter(":", "")

            val fragment = uri.split("#").getOrElse(1) { "" }
            val remark = java.net.URLDecoder.decode(fragment, "UTF-8").ifEmpty { "SS Node ${index + 1}" }

            SubServer(
                id = "ss_${index}_${System.currentTimeMillis()}",
                name = remark,
                protocol = "Shadowsocks",
                address = host,
                port = port,
                flag = "🇳🇱",
                password = "$method:$password",
                encryption = method
            )
        } catch (_: Exception) { null }
    }

    private fun parseHysteriaUri(uri: String, index: Int): SubServer {
        val cleanUri = uri.replace("hysteria2://", "").replace("hy2://", "")
        val authPart = cleanUri.substringBefore("@")
        val rest = cleanUri.substringAfter("@", "")
        val hostPort = rest.substringBefore("?")
        val params = rest.substringAfter("?", "").split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }

        val host = hostPort.substringBefore(":")
        val port = hostPort.substringAfter(":").substringBefore("?").toIntOrNull() ?: 2443
        val remark = params["remark"]?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "HY2 Node ${index + 1}"

        return SubServer(
            id = "hy2_${index}_${System.currentTimeMillis()}",
            name = remark,
            protocol = "Hysteria 2",
            address = host,
            port = port,
            flag = "🇷🇺",
            password = authPart,
            sni = params["sni"] ?: host
        )
    }

    private fun getFlagForProtocol(protocol: String): String = when {
        protocol.contains("hysteria", true) -> "🇷🇺"
        protocol.contains("vless", true) -> "🇳🇱"
        protocol.contains("vmess", true) -> "🇳🇱"
        protocol.contains("trojan", true) -> "🇪🇺"
        protocol.contains("shadowsocks", true) -> "🇳🇱"
        else -> "🌐"
    }
}
