package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
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
    val fingerprint: String = "chrome",
    val privateKey: String = "",
    val presharedKey: String = "",
    val serverPublicKey: String = "",
    val localAddress: String = "",
    val mtu: Int = 1420,
    val awgParams: String = "",
    val dns: String = ""
)

data class SubscriptionResult(
    val url: String,
    val servers: List<SubServer>,
    val error: String = "",
    val profileInfo: ProfileInfo? = null
)

data class ProfileInfo(
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expiry: Long = 0,
    val username: String = ""
)

class SubscriptionClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchSubscription(url: String): SubscriptionResult = withContext(Dispatchers.IO) {
        if (url.startsWith("vpn://")) {
            val server = parseProxyUri(url, 0)
            return@withContext if (server != null) {
                SubscriptionResult(url, listOf(server))
            } else {
                SubscriptionResult(url, emptyList(), "Invalid AmneziaVPN config")
            }
        }
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RedShift/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext SubscriptionResult(url, emptyList(), "Empty response")

            val servers = parseSubscriptionBody(body)
            var profileInfo = parseSubscriptionUserinfoHeader(response)
            if (profileInfo == null) {
                profileInfo = parseProfileInfoHeader(response)
            }
            SubscriptionResult(url, servers, profileInfo = profileInfo)
        } catch (e: Exception) {
            SubscriptionResult(url, emptyList(), e.message ?: "Unknown error")
        }
    }

    private fun parseSubscriptionUserinfoHeader(response: okhttp3.Response): ProfileInfo? {
        val header = response.header("subscription-userinfo") ?: return null
        return try {
            val fields = HashMap<String, Long>()
            for (part in header.split(";")) {
                val kv = part.trim().split("=", limit = 2)
                if (kv.size == 2) {
                    fields[kv[0].trim().lowercase()] = kv[1].trim().toLongOrNull() ?: 0L
                }
            }
            if (fields.isEmpty()) return null
            ProfileInfo(
                upload = fields["upload"] ?: 0L,
                download = fields["download"] ?: 0L,
                total = fields["total"] ?: 0L,
                expiry = fields["expire"] ?: 0L,
                username = ""
            )
        } catch (_: Exception) { null }
    }

    private fun parseProfileInfoHeader(response: okhttp3.Response): ProfileInfo? {
        val header = response.header("profile-info") ?: return null
        return try {
            val json = JSONObject(header)
            ProfileInfo(
                upload = json.optLong("upload", 0),
                download = json.optLong("download", 0),
                total = json.optLong("total", 0),
                expiry = json.optLong("expiry", 0),
                username = json.optString("username", "")
            )
        } catch (_: Exception) {
            val decoded = try { java.net.URLDecoder.decode(header, "UTF-8") } catch (_: Exception) { header }
            try {
                val json = JSONObject(decoded)
                ProfileInfo(
                    upload = json.optLong("upload", 0),
                    download = json.optLong("download", 0),
                    total = json.optLong("total", 0),
                    expiry = json.optLong("expiry", 0),
                    username = json.optString("username", "")
                )
            } catch (_: Exception) { null }
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

    private fun extractName(uri: String): String {
        val hashIdx = uri.lastIndexOf('#')
        if (hashIdx < 0) return ""
        val fragment = uri.substring(hashIdx + 1)
        return try {
            java.net.URLDecoder.decode(fragment, "UTF-8")
        } catch (_: Exception) { fragment }
    }

    companion object {
        fun deriveRegionFromAddress(address: String): String {
            val ip = address.trim()
            return when {
                ip.startsWith("37.220.") -> "NL"
                ip.startsWith("217.156.") -> "EU"
                ip.contains("amsterdam") || ip.contains(".nl.") || ip.endsWith(".nl") -> "NL"
                ip.contains("frankfurt") || ip.contains(".de.") || ip.endsWith(".de") -> "DE"
                ip.contains(".eu") -> "EU"
                ip.contains(".md") || ip.contains("moldova") -> "MD"
                ip.contains(".us.") || ip.contains("usa") -> "US"
                ip.contains(".uk.") || ip.contains(".gb.") -> "UK"
                ip.contains(".sg.") -> "SG"
                ip.contains(".tr.") -> "TR"
                else -> ""
            }
        }

        fun flagForRegion(region: String): String = when (region) {
            "NL" -> "\uD83C\uDDF3\uD83C\uDDF1"
            "EU" -> "\uD83C\uDDEA\uD83C\uDDFA"
            "US" -> "\uD83C\uDDFA\uD83C\uDDF8"
            "UK" -> "\uD83C\uDDEC\uD83C\uDDE7"
            "SG" -> "\uD83C\uDDF8\uD83C\uDDEC"
            "DE" -> "\uD83C\uDDE9\uD83C\uDDEA"
            "MD" -> "\uD83C\uDDF2\uD83C\uDDE9"
            "TR" -> "\uD83C\uDDF9\uD83C\uDDF7"
            else -> ""
        }
    }

    private fun stripFragment(uri: String): String {
        val hashIdx = uri.lastIndexOf('#')
        return if (hashIdx >= 0) uri.substring(0, hashIdx) else uri
    }

    private fun parseProxyUri(uri: String, index: Int): SubServer? {
        val name = extractName(uri)
        val cleanUri = stripFragment(uri)
        val parsed = when {
            cleanUri.startsWith("vless://") -> parseVlessUri(cleanUri, name, "", index)
            cleanUri.startsWith("vmess://") -> parseVmessUri(cleanUri, name, "", index)
            cleanUri.startsWith("trojan://") -> parseTrojanUri(cleanUri, name, "", index)
            cleanUri.startsWith("ss://") -> parseShadowsocksUri(cleanUri, name, "", index)
            cleanUri.startsWith("hy2://") || cleanUri.startsWith("hysteria2://") -> parseHysteriaUri(cleanUri, name, "", index)
            cleanUri.startsWith("tt://") -> parseTrustTunnelUri(cleanUri, name, "", index)
            cleanUri.startsWith("vpn://") -> parseAmneziaVpnUri(cleanUri, name, "", index)
            else -> null
        } ?: return null
        return enrichWithRegion(parsed, name)
    }

    private fun enrichWithRegion(server: SubServer, originalName: String): SubServer {
        if (server.flag.isNotEmpty() && server.flag != "🌐" && server.protocol.uppercase().contains("AMNEZIA")) {
            return server
        }
        val region = deriveRegionFromAddress(server.address)
        if (region.isEmpty()) {
            return if (server.flag.isEmpty()) server.copy(flag = "🌐") else server
        }
        val regionFlag = flagForRegion(region)
        val nameHasRegion = Regex("\\b(${region})\\b", RegexOption.IGNORE_CASE).containsMatchIn(originalName) ||
                originalName.contains(regionFlag)
        val newName = if (nameHasRegion) {
            server.name
        } else {
            "${server.name} • $region".trim()
        }
        return server.copy(flag = regionFlag, name = newName)
    }

    private fun parseVlessUri(uri: String, name: String, flag: String, index: Int): SubServer {
        val withoutScheme = uri.removePrefix("vless://")
        val uuid = withoutScheme.substringBefore("@")
        val rest = withoutScheme.substringAfter("@", "")
        val addressPort = rest.substringBefore("?")
        val params = rest.substringAfter("?", "").split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }
        val host = addressPort.substringBefore(":")
        val port = addressPort.substringAfter(":").toIntOrNull() ?: 443
        val security = params["security"] ?: ""
        val protoName = when {
            security == "reality" -> "VLESS+Reality"
            security == "tls" -> "VLESS+TLS"
            else -> "VLESS"
        }
        return SubServer(
            id = "vless_${index}_${System.currentTimeMillis()}",
            name = name.ifEmpty { "VLESS Node ${index + 1}" },
            protocol = protoName,
            address = host,
            port = port,
            flag = flag,
            uuid = uuid,
            flow = params["flow"] ?: "",
            encryption = params["encryption"] ?: "none",
            network = params["type"] ?: "tcp",
            tls = security == "reality" || security == "tls",
            sni = params["sni"] ?: "",
            publicKey = params["pbk"] ?: "",
            shortId = params["sid"] ?: "",
            fingerprint = params["fp"] ?: "chrome"
        )
    }

    private fun parseVmessUri(uri: String, name: String, flag: String, index: Int): SubServer? {
        return try {
            val b64 = uri.removePrefix("vmess://")
            val decoded = String(java.util.Base64.getUrlDecoder().decode(b64), Charsets.UTF_8)
            val json = org.json.JSONObject(decoded)
            SubServer(
                id = "vmess_${index}_${System.currentTimeMillis()}",
                name = name.ifEmpty { json.optString("ps", "VMess Node ${index + 1}") },
                protocol = "VMess",
                address = json.optString("add", ""),
                port = json.optInt("port", 443),
                flag = flag,
                uuid = json.optString("id", ""),
                encryption = json.optString("scy", "auto"),
                network = json.optString("net", "tcp"),
                tls = json.optString("tls", "") == "tls",
                sni = json.optString("sni", "")
            )
        } catch (_: Exception) { null }
    }

    private fun parseTrojanUri(uri: String, name: String, flag: String, index: Int): SubServer {
        val withoutScheme = uri.removePrefix("trojan://")
        val password = withoutScheme.substringBefore("@")
        val rest = withoutScheme.substringAfter("@", "")
        val addressPort = rest.substringBefore("?")
        val params = rest.substringAfter("?", "").split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }
        val host = addressPort.substringBefore(":")
        val port = addressPort.substringAfter(":").toIntOrNull() ?: 443
        return SubServer(
            id = "trojan_${index}_${System.currentTimeMillis()}",
            name = name.ifEmpty { "Trojan Node ${index + 1}" },
            protocol = "Trojan+TLS",
            address = host,
            port = port,
            flag = flag,
            password = password,
            tls = true,
            sni = params["sni"] ?: host
        )
    }

    private fun parseShadowsocksUri(uri: String, name: String, flag: String, index: Int): SubServer? {
        return try {
            val withoutScheme = uri.removePrefix("ss://")
            val atIndex = withoutScheme.indexOf("@")
            val methodPass: String
            val hostPort: String

            if (atIndex > 0) {
                methodPass = String(java.util.Base64.getUrlDecoder().decode(withoutScheme.substring(0, atIndex)), Charsets.UTF_8)
                hostPort = withoutScheme.substring(atIndex + 1)
            } else {
                val decoded = String(java.util.Base64.getUrlDecoder().decode(withoutScheme), Charsets.UTF_8)
                val lastAtIndex = decoded.lastIndexOf("@")
                if (lastAtIndex < 0) return null
                methodPass = decoded.substring(0, lastAtIndex)
                hostPort = decoded.substring(lastAtIndex + 1)
            }

            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":").toIntOrNull() ?: 443
            val method = methodPass.substringBefore(":")
            val password = methodPass.substringAfter(":", "")

            SubServer(
                id = "ss_${index}_${System.currentTimeMillis()}",
                name = name.ifEmpty { "SS Node ${index + 1}" },
                protocol = "Shadowsocks",
                address = host,
                port = port,
                flag = flag,
                password = "$method:$password",
                encryption = method
            )
        } catch (_: Exception) { null }
    }

    private fun parseHysteriaUri(uri: String, name: String, flag: String, index: Int): SubServer {
        val cleanUri = uri.replace("hysteria2://", "").replace("hy2://", "")
        val authPart = cleanUri.substringBefore("@")
        val rest = cleanUri.substringAfter("@", "")
        val hostPort = rest.substringBefore("?")
        val params = rest.substringAfter("?", "").split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }
        val host = hostPort.substringBefore(":")
        val port = hostPort.substringAfter(":").toIntOrNull() ?: 2443
        return SubServer(
            id = "hy2_${index}_${System.currentTimeMillis()}",
            name = name.ifEmpty { "HY2 Node ${index + 1}" },
            protocol = "Hysteria 2",
            address = host,
            port = port,
            flag = flag,
            password = authPart,
            sni = params["sni"] ?: host
        )
    }

    private fun parseAmneziaVpnUri(uri: String, name: String, flag: String, index: Int): SubServer? {
        return try {
            val b64 = uri.removePrefix("vpn://")
            val decoded = java.util.Base64.getUrlDecoder().decode(b64)
            val uncompLen = java.nio.ByteBuffer.wrap(decoded, 0, 4).int
            val compressed = decoded.copyOfRange(4, decoded.size)
            val buf = ByteArray(uncompLen)
            var inflated = false
            for (nowrap in booleanArrayOf(false, true)) {
                val inflater = java.util.zip.Inflater(nowrap)
                inflater.setInput(compressed)
                try {
                    inflater.inflate(buf)
                    inflated = true
                } catch (_: Exception) {
                } finally {
                    inflater.end()
                }
                if (inflated) break
            }
            if (!inflated) return null
            val jsonStr = String(buf, Charsets.UTF_8)
            val root = org.json.JSONObject(jsonStr)
            val containers = root.optJSONArray("containers")
            if (containers == null || containers.length() == 0) return null
            val awgObj = containers.getJSONObject(0).optJSONObject("awg") ?: return null

            val lastConfigStr = awgObj.optString("last_config", "")
            if (lastConfigStr.isEmpty()) return null
            val configJson = org.json.JSONObject(lastConfigStr)

            val configStr = configJson.optString("config", "")
            val hostName = configJson.optString("hostName", "")
            val port = configJson.optInt("port", 38668)
            val clientPrivKey = configJson.optString("client_priv_key", "")
            val clientPubKey = configJson.optString("client_pub_key", "")
            val serverPubKey = configJson.optString("server_pub_key", "")
            val psk = configJson.optString("psk_key", "")
            val clientIpRaw = configJson.optString("client_ip", "10.8.0.2")
            val clientIp = if (clientIpRaw.contains("/")) clientIpRaw.substringBefore("/") else clientIpRaw
            val mtu = configJson.optInt("mtu", 1420)
            val dns1 = root.optString("dns1", "1.1.1.1")
            val subnetAddr = awgObj.optString("subnet_address", "10.8.0.0")

            val awgParamsJson = org.json.JSONObject()
            awgParamsJson.put("jc", awgObj.optString("Jc", "4"))
            awgParamsJson.put("jmin", awgObj.optString("Jmin", "10"))
            awgParamsJson.put("jmax", awgObj.optString("Jmax", "50"))
            awgParamsJson.put("s1", awgObj.optString("S1", "43"))
            awgParamsJson.put("s2", awgObj.optString("S2", "122"))
            awgParamsJson.put("s3", awgObj.optString("S3", "48"))
            awgParamsJson.put("s4", awgObj.optString("S4", "12"))
            awgParamsJson.put("h1", awgObj.optString("H1", ""))
            awgParamsJson.put("h2", awgObj.optString("H2", ""))
            awgParamsJson.put("h3", awgObj.optString("H3", ""))
            awgParamsJson.put("h4", awgObj.optString("H4", ""))
            awgParamsJson.put("i1", awgObj.optString("I1", ""))
            awgParamsJson.put("i2", awgObj.optString("I2", ""))
            awgParamsJson.put("i3", awgObj.optString("I3", ""))
            awgParamsJson.put("i4", awgObj.optString("I4", ""))
            awgParamsJson.put("i5", awgObj.optString("I5", ""))

            val desc = root.optString("description", "AmneziaWG")

            SubServer(
                id = "awg_${index}_${System.currentTimeMillis()}",
                name = name.ifEmpty { desc },
                protocol = "AmneziaWG",
                address = hostName,
                port = port,
                flag = flag,
                privateKey = clientPrivKey,
                presharedKey = psk,
                serverPublicKey = serverPubKey,
                localAddress = "$clientIp/32",
                mtu = mtu,
                awgParams = awgParamsJson.toString(),
                dns = dns1
            )
        } catch (_: Exception) { null }
    }

    private fun parseTrustTunnelUri(uri: String, name: String, flag: String, index: Int): SubServer? {
        return try {
            val withoutScheme = uri.removePrefix("tt://")
            val params = withoutScheme.split("?").getOrElse(1) { "" }
            val pairs = params.split("&").associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrElse(1) { "" })
            }
            val b64Data = pairs["AQ"] ?: return null
            val decoded = String(java.util.Base64.getUrlDecoder().decode(b64Data), Charsets.UTF_8)
            val lines = decoded.split("\n").filter { it.isNotBlank() }
            if (lines.size < 2) return null
            val host = lines[1]
            val portStr = lines.getOrNull(2) ?: "10443"
            val port = portStr.toIntOrNull() ?: 10443
            SubServer(
                id = "tt_${index}_${System.currentTimeMillis()}",
                name = name.ifEmpty { "TrustTunnel Node ${index + 1}" },
                protocol = "TrustTunnel",
                address = host,
                port = port,
                flag = flag,
                password = b64Data
            )
        } catch (_: Exception) { null }
    }

    private fun getFlagForProtocol(protocol: String): String = when {
        protocol.contains("hysteria", true) -> "🌐"
        protocol.contains("vless", true) -> "🌐"
        protocol.contains("vmess", true) -> "🌐"
        protocol.contains("trojan", true) -> "🌐"
        protocol.contains("shadowsocks", true) -> "🌐"
        protocol.contains("amnezia", true) || protocol.contains("awg", true) -> "🌐"
        else -> "🌐"
    }
}
