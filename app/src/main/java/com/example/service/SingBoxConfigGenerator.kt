package com.example.service

import com.example.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

class SingBoxConfigGenerator {

    companion object {
        const val CLASH_API_PORT = 9090
        const val MIXED_PORT = 10809
        /** Local SOCKS5 port the olcRTC runtime binds before sing-box routes through it. */
        const val OLCRTC_SOCKS_PORT = 8788
        private const val REMOTE_DNS = "https://8.8.8.8/dns-query"
        private const val LOCAL_DNS = "77.88.8.8"

        val RU_DOMAIN_SUFFIXES = arrayOf(
            ".ru", ".su", ".xn--p1ai", ".moscow", ".москва",
            ".yandex", ".ya.ru"
        )
        /** A compact ad/tracker blocklist used by the "Block ads" routing toggle. */
        val AD_DOMAIN_SUFFIXES = arrayOf(
            ".doubleclick.net", ".googlesyndication.com", ".googleadservices.com",
            ".adservice.google.com", ".googletagmanager.com", ".adform.net",
            ".adnxs.com", ".adsrvr.org", ".adroll.com", ".amazon-adsystem.com",
            ".moatads.com", ".outbrain.com", ".taboola.com", ".pubmatic.com",
            ".rubiconproject.com", ".criteo.com", ".casalemedia.com",
            ".scorecardresearch.com", ".quantserve.com", ".openx.net",
            ".crwdcntrl.net", ".bluekai.com", ".demdex.net", ".adsystem.com",
            ".provenance.ink", ".serverbid.com", ".spotx.tv", ".teads.tv"
        )
        private const val DEFAULT_HY2_UP_MBPS = 20
        private const val DEFAULT_HY2_DOWN_MBPS = 100
    }

    fun generateConfig(server: SubServer, socksPort: Int = 10808, filesDir: String = "", splitDomains: List<String> = emptyList(), splitOnly: Boolean = false, routingMode: String = "rule", bypassRussia: Boolean = true, bypassChina: Boolean = false, blockAds: Boolean = false): String {
        // Endpoints must be IPv4 literals in the generated config: sing-box would
        // otherwise resolve the peer/server domain through the tunnel's own DNS
        // (remote-dns detours to this outbound), which is a chicken-and-egg deadlock
        // while the tunnel is still being established (see: AWG handshake timeouts /
        // "failed to resolve endpoints"). Resolve locally, before the TUN is up.
        // olcRTC endpoints are WebRTC room ids, not hosts — never resolve those.
        val isOlcrtc = isOlcrtcProtocol(server)
        val cfgServer = if (isOlcrtc) server else server.copy(address = resolveHostToIp(server.address))
        val isAwg = isAmneziaProtocol(cfgServer)
        val outTag = cfgServer.id

        val config = baseTunConfig(cfgServer)
        config.put("dns", buildDnsConfig(cfgServer, filesDir))
        config.put("outbounds", JSONArray().apply {
            put(
                when {
                    isOlcrtc -> buildOlcrtcOutbound(cfgServer.id)
                    isAwg -> buildAmneziaWgOutbound(cfgServer)
                    else -> buildOutbound(cfgServer)
                }
            )
            put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
            put(JSONObject().apply { put("type", "block"); put("tag", "block") })
        })
        config.put("route", buildRouteConfig(outTag, filesDir, splitDomains, splitOnly, routingMode, bypassRussia, bypassChina, blockAds, dnsHijack = isOlcrtc))
        config.put("experimental", JSONObject().apply {
            put("clash_api", JSONObject().apply {
                put("external_controller", "127.0.0.1:$CLASH_API_PORT")
                put("default_mode", "Rule")
            })
        })
        return config.toString(2)
    }

    fun generateMixedConfig(server: SubServer, socksPort: Int = 10808): String {
        val config = JSONObject().apply {
            put("log", logConfig())
            put("inbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "mixed")
                    put("tag", "mixed-in")
                    put("listen", "127.0.0.1")
                    put("listen_port", socksPort)
                })
            })
            put("outbounds", JSONArray().apply {
                put(buildOutbound(server))
                put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
            })
            put("route", JSONObject().apply {
                put("rules", JSONArray())
                put("final", server.id)
            })
        }
        return config.toString(2)
    }

    fun isAmneziaProtocol(server: SubServer): Boolean {
        return server.protocol.uppercase().let {
            it.contains("AMNEZIA") || it.contains("AWG") || it.contains("WIREGUARD") || it.contains("WG")
        }
    }

    fun isOlcrtcProtocol(server: SubServer): Boolean =
        server.protocol.uppercase().contains("OLCRTC")

    /** Everything goes through the local olcRTC SOCKS5 tunnel (127.0.0.1:8788). */
    private fun buildOlcrtcOutbound(tag: String): JSONObject = JSONObject().apply {
        put("type", "socks")
        put("tag", tag)
        put("server", "127.0.0.1")
        put("server_port", OLCRTC_SOCKS_PORT)
    }

    private fun logConfig(): JSONObject = JSONObject().apply {
        put("level", if (BuildConfig.DEBUG) "info" else "warn")
        put("timestamp", true)
    }

    private fun baseTunConfig(server: SubServer, sniff: Boolean = false): JSONObject {
        // sniff was removed from inbound fields in sing-box 1.13; it is now a route
        // rule action ({"action":"sniff"}), emitted by buildRouteConfig when dnsHijack.
        return JSONObject().apply {
            put("log", logConfig())
            put("inbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "tun")
                    put("tag", "tun-in")
                    put("address", JSONArray().apply { put("10.8.0.2/32") })
                    put("auto_route", false)
                    put("strict_route", false)
                    put("stack", "gvisor")
                    put("mtu", 1280)
                })
                // Local mixed proxy inside the tunnel: our own app traffic is
                // disallowed from the TUN (addDisallowedApplication), so the only
                // way it can measure servers "via proxy" (as Amnezia does) is to
                // hop through this loopback inbound, which forwards over the tunnel.
                put(JSONObject().apply {
                    put("type", "mixed")
                    put("tag", "mixed-in")
                    put("listen", "127.0.0.1")
                    put("listen_port", MIXED_PORT)
                })
            })
        }
    }

    private fun isIpAddress(value: String): Boolean {
        return value.isNotEmpty() && value[0].isDigit() && (value.contains('.') || value.contains(':'))
    }

    private fun resolveHostToIp(host: String): String {
        if (host.isBlank()) return host
        if (host.first().isDigit() && (host.contains('.') || host.contains(':'))) return host
        return try {
            val addrs = java.net.InetAddress.getAllByName(host)
            val ipv4 = addrs.firstOrNull { it is java.net.Inet4Address }
            (ipv4 ?: addrs.firstOrNull())?.hostAddress ?: host
        } catch (_: Exception) {
            host
        }
    }

    private fun buildDnsConfig(server: SubServer, filesDir: String): JSONObject {
        return JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "remote-dns")
                    put("type", "https")
                    put("server", "8.8.8.8")
                    put("server_port", 443)
                    put("detour", server.id)
                })
            })
            put("rules", JSONArray())
            put("final", "remote-dns")
            put("strategy", "prefer_ipv4")
        }
    }

    private fun buildRouteConfig(outboundTag: String, filesDir: String, splitDomains: List<String> = emptyList(), splitOnly: Boolean = false, routingMode: String = "rule", bypassRussia: Boolean = true, bypassChina: Boolean = false, blockAds: Boolean = false, dnsHijack: Boolean = false): JSONObject {
        val geoipRuPath = if (filesDir.isNotEmpty()) "$filesDir/geoip-ru.srs" else ""
        val geoipCnPath = if (filesDir.isNotEmpty()) "$filesDir/geoip-cn.srs" else ""
        val normalizedDomains = splitDomains
            .map { it.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("*.") }
            .filter { it.isNotBlank() }
            .distinct()
        // Routing modes: "global" = everything through the VPN (rules ignored),
        // "direct" = everything direct, "rule" (default) = rules below apply.
        val useRules = routingMode != "direct"
        // Domain rules (split, RU/CN bypass, adblock) can only match a domain if the
        // TUN packet is sniffed for TLS SNI / HTTP Host. Inbound `sniff` field was
        // removed in sing-box 1.13; it is now a non-final route rule action emitted
        // here — otherwise domain-based split tunneling would silently not work.
        val needSniff = useRules && (normalizedDomains.isNotEmpty() || blockAds || bypassRussia || bypassChina || dnsHijack)
        return JSONObject().apply {
            put("rules", JSONArray().apply {
                // sniff is a non-final rule action (inbound sniff was removed in
                // sing-box 1.13): it must run first so the protocol/domain of every
                // connection is detected before later rules route on it.
                if (needSniff) {
                    put(JSONObject().apply {
                        put("action", "sniff")
                    })
                }
                // Private/local networks always stay direct so the device can reach
                // its router and LAN. Not a toggle — without it the VPN breaks LAN access.
                put(JSONObject().apply {
                    put("ip_is_private", true)
                    put("outbound", "direct")
                })
                // olcRTC carries no UDP, so app DNS (UDP:53) must be intercepted and
                // resolved by sing-box over TCP (remote-dns, https via the SOCKS outbound).
                if (dnsHijack) {
                    put(JSONObject().apply {
                        put("protocol", "dns")
                        put("action", "hijack-dns")
                    })
                }
                if (useRules) {
                    if (normalizedDomains.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("domain_suffix", JSONArray().apply { normalizedDomains.forEach { put(it) } })
                            if (splitOnly) put("invert", true)
                            put("outbound", "direct")
                        })
                    }
                    if (blockAds) {
                        put(JSONObject().apply {
                            put("domain_suffix", JSONArray().apply { AD_DOMAIN_SUFFIXES.forEach { put(it) } })
                            put("outbound", "block")
                        })
                    }
                    // In "only these sites through the VPN" mode the built-in direct
                    // rules would leak those sites outside the tunnel, so drop them.
                    val inBypassMode = !splitOnly || normalizedDomains.isEmpty()
                    if (inBypassMode) {
                        if (bypassRussia) {
                            put(JSONObject().apply {
                                put("domain_suffix", JSONArray().apply { RU_DOMAIN_SUFFIXES.forEach { put(it) } })
                                put("outbound", "direct")
                            })
                            if (geoipRuPath.isNotEmpty()) {
                                put(JSONObject().apply {
                                    put("rule_set", JSONArray().apply { put("geoip-ru") })
                                    put("outbound", "direct")
                                })
                            }
                        }
                        if (bypassChina && geoipCnPath.isNotEmpty()) {
                            put(JSONObject().apply {
                                put("rule_set", JSONArray().apply { put("geoip-cn") })
                                put("outbound", "direct")
                            })
                        }
                    }
                }
            })
            put("final", if (routingMode == "direct") "direct" else outboundTag)
            // VpnService fd mode: the launcher owns interface addressing and
            // routing, so we must not auto-detect/monitor netlink (which is banned
            // for app uids on Android and would make startup fatal).
            put("auto_detect_interface", false)
            put("default_domain_resolver", "remote-dns")
            val ruleSets = JSONArray()
            if (geoipRuPath.isNotEmpty()) {
                ruleSets.put(JSONObject().apply {
                    put("type", "local")
                    put("tag", "geoip-ru")
                    put("path", geoipRuPath)
                })
            }
            if (useRules && bypassChina && geoipCnPath.isNotEmpty()) {
                ruleSets.put(JSONObject().apply {
                    put("type", "local")
                    put("tag", "geoip-cn")
                    put("path", geoipCnPath)
                })
            }
            if (ruleSets.length() > 0) {
                put("rule_set", ruleSets)
            }
        }
    }

    private fun buildOutbound(server: SubServer): JSONObject {
        val proto = server.protocol.uppercase()
        return when {
            isAmneziaProtocol(server) -> buildAmneziaWgOutbound(server)
            proto.contains("VLESS") -> buildVlessOutbound(server)
            proto.contains("TROJAN") -> buildTrojanOutbound(server)
            proto.contains("HYSTERIA") || proto.contains("HY2") -> buildHysteria2Outbound(server)
            proto.contains("SHADOWSOCKS") || proto == "SS" -> buildShadowsocksOutbound(server)
            proto.contains("VMESS") -> buildVmessOutbound(server)
            else -> buildVlessOutbound(server)
        }
    }

    private fun buildVlessOutbound(s: SubServer): JSONObject {
        val out = JSONObject().apply {
            put("type", "vless")
            put("tag", s.id)
            put("server", s.address)
            put("server_port", s.port)
            put("uuid", s.uuid)
            if (s.flow.isNotEmpty()) put("flow", s.flow)
        }

        if (s.tls || s.protocol.contains("REALITY", true) || s.protocol.contains("TLS", true)) {
            val tls = JSONObject().apply {
                put("enabled", true)
                put("server_name", s.sni.ifEmpty { s.address })
                if (s.alpn.isNotBlank()) {
                    put("alpn", JSONArray().apply { s.alpn.split(",").forEach { alpn -> put(alpn.trim()) } })
                }
                if (s.protocol.contains("REALITY", true) || s.publicKey.isNotEmpty()) {
                    put("utls", JSONObject().apply {
                        put("enabled", true)
                        put("fingerprint", s.fingerprint.ifEmpty { "chrome" })
                    })
                    put("reality", JSONObject().apply {
                        put("enabled", true)
                        put("public_key", s.publicKey)
                        put("short_id", s.shortId.ifEmpty { "" })
                    })
                }
            }
            out.put("tls", tls)
        }

        val network = s.network.lowercase()
        if (network == "ws" || network == "grpc" || network == "xhttp") {
            val transport = JSONObject().apply { put("type", network) }
            when (network) {
                "xhttp", "ws" -> {
                    val path = if (s.path.isNotBlank()) s.path else "/"
                    transport.put("path", path)
                    val headers = JSONObject()
                    val hostHeader = s.host.ifEmpty { s.sni }
                    if (hostHeader.isNotEmpty()) headers.put("Host", hostHeader)
                    val extra = parseExtra(s.extra)
                    if (extra != null) {
                        val extraHeaders = extra.optJSONObject("headers")
                        if (extraHeaders != null) {
                            extraHeaders.keys().forEach { key ->
                                headers.put(key, extraHeaders.optString(key))
                            }
                        }
                        val extraPath = extra.optString("path", "")
                        if (extraPath.isNotEmpty()) transport.put("path", extraPath)
                    }
                    if (headers.length() > 0) transport.put("headers", headers)
                }
            }
            out.put("transport", transport)
        }

        return out
    }

    private fun parseExtra(extra: String): org.json.JSONObject? {
        if (extra.isBlank()) return null
        return try {
            org.json.JSONObject(java.net.URLDecoder.decode(extra, "UTF-8"))
        } catch (_: Exception) {
            try {
                org.json.JSONObject(extra)
            } catch (_: Exception) { null }
        }
    }

    private fun buildTrojanOutbound(s: SubServer): JSONObject {
        return JSONObject().apply {
            put("type", "trojan")
            put("tag", s.id)
            put("server", s.address)
            put("server_port", s.port)
            put("password", s.password)
            put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", s.sni.ifEmpty { s.address })
                if (s.alpn.isNotBlank()) {
                    put("alpn", JSONArray().apply { s.alpn.split(",").forEach { alpn -> put(alpn.trim()) } })
                }
            })
        }
    }

    private fun buildHysteria2Outbound(s: SubServer): JSONObject {
        return JSONObject().apply {
            put("type", "hysteria2")
            put("tag", s.id)
            put("server", s.address)
            put("server_port", s.port)
            put("password", s.password)
            put("up_mbps", optsUpMbps(s))
            put("down_mbps", optsDownMbps(s))
            put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", s.sni.ifEmpty { s.address })
                if (s.alpn.isNotBlank()) {
                    put("alpn", JSONArray().apply { s.alpn.split(",").forEach { alpn -> put(alpn.trim()) } })
                } else {
                    put("alpn", JSONArray().put("h3"))
                }
            })
        }
    }

    private fun optsUpMbps(s: SubServer): Int {
        val p = parseExtra(s.extra)
        return p?.takeIf { it.has("up_mbps") }?.optInt("up_mbps", 20) ?: DEFAULT_HY2_UP_MBPS
    }

    private fun optsDownMbps(s: SubServer): Int {
        val p = parseExtra(s.extra)
        return p?.takeIf { it.has("down_mbps") }?.optInt("down_mbps", 100) ?: DEFAULT_HY2_DOWN_MBPS
    }

    private fun buildShadowsocksOutbound(s: SubServer): JSONObject {
        val parts = s.password.split(":", limit = 2)
        val method = if (parts.size == 2) parts[0] else s.encryption.ifEmpty { "2022-blake3-aes-128-gcm" }
        val password = if (parts.size == 2) parts[1] else s.password

        return JSONObject().apply {
            put("type", "shadowsocks")
            put("tag", s.id)
            put("server", s.address)
            put("server_port", s.port)
            put("method", method)
            put("password", password)
        }
    }

    private fun buildAmneziaWgOutbound(s: SubServer): JSONObject {
        val out = JSONObject().apply {
            put("type", "wireguard")
            put("tag", s.id)
            put("address", JSONArray().apply {
                put(if (s.localAddress.isNotBlank()) s.localAddress else "10.0.0.2/32")
            })
            put("private_key", s.privateKey)

            val peer = JSONObject().apply {
                put("address", s.address)
                put("port", s.port)
                put("public_key", s.serverPublicKey)
                if (s.presharedKey.isNotBlank()) put("pre_shared_key", s.presharedKey)
                put("allowed_ips", JSONArray().apply { put("0.0.0.0/0"); put("::/0") })
                put("persistent_keepalive_interval", 25)
            }
            put("peers", JSONArray().apply { put(peer) })
        }

        if (s.awgParams.isNotBlank()) {
            try {
                val p = org.json.JSONObject(s.awgParams)
                putIntIfPresent(out, "jc", p.optString("jc", ""))
                putIntIfPresent(out, "jmin", p.optString("jmin", ""))
                putIntIfPresent(out, "jmax", p.optString("jmax", ""))
                putIntIfPresent(out, "s1", p.optString("s1", ""))
                putIntIfPresent(out, "s2", p.optString("s2", ""))
                putIntIfPresent(out, "s3", p.optString("s3", ""))
                putIntIfPresent(out, "s4", p.optString("s4", ""))
                putValueIfPresent(out, "h1", p.optString("h1", ""))
                putValueIfPresent(out, "h2", p.optString("h2", ""))
                putValueIfPresent(out, "h3", p.optString("h3", ""))
                putValueIfPresent(out, "h4", p.optString("h4", ""))
                putValueIfPresent(out, "i1", p.optString("i1", ""))
                putValueIfPresent(out, "i2", p.optString("i2", ""))
                putValueIfPresent(out, "i3", p.optString("i3", ""))
                putValueIfPresent(out, "i4", p.optString("i4", ""))
                putValueIfPresent(out, "i5", p.optString("i5", ""))
            } catch (_: Exception) {}
        }

        return out
    }

    private fun putIntIfPresent(json: JSONObject, key: String, value: String) {
        val v = value.trim()
        if (v.isNotEmpty()) {
            try { json.put(key, v.toInt()) } catch (_: Exception) {}
        }
    }

    private fun putValueIfPresent(json: JSONObject, key: String, value: String) {
        val v = value.trim()
        if (v.isNotEmpty()) {
            try {
                if (v.contains("-")) json.put(key, v) else json.put(key, v.toInt())
            } catch (_: Exception) { json.put(key, v) }
        }
    }

    private fun buildVmessOutbound(s: SubServer): JSONObject {
        val out = JSONObject().apply {
            put("type", "vmess")
            put("tag", s.id)
            put("server", s.address)
            put("server_port", s.port)
            put("uuid", s.uuid)
            put("security", s.encryption.ifEmpty { "auto" })
        }

        if (s.tls) {
            out.put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", s.sni.ifEmpty { s.address })
            })
        }

        val network = s.network.lowercase()
        if (network == "ws") {
            out.put("transport", JSONObject().apply {
                put("type", "ws")
                put("path", "/")
                put("headers", JSONObject().apply {
                    if (s.sni.isNotEmpty()) put("Host", s.sni)
                })
            })
        }

        return out
    }
}
