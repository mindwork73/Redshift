package com.example.service

import org.json.JSONArray
import org.json.JSONObject

class SingBoxConfigGenerator {

    fun generateConfig(server: SubServer, socksPort: Int = 10808): String {
        val outbound = buildOutbound(server)
        val config = JSONObject()

        config.put("log", JSONObject().apply {
            put("level", "warn")
            put("timestamp", true)
        })

        config.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "socks")
                put("tag", "socks-in")
                put("listen", "127.0.0.1")
                put("listen_port", socksPort)
                put("sniff", true)
                put("sniff_override_destination", false)
            })
            put(JSONObject().apply {
                put("type", "mixed")
                put("tag", "mixed-in")
                put("listen", "127.0.0.1")
                put("listen_port", socksPort + 1)
            })
        })

        config.put("outbounds", JSONArray().apply {
            put(outbound)
            put(JSONObject().apply { put("type", "direct"); put("tag", "direct") })
            put(JSONObject().apply { put("type", "block"); put("tag", "block") })
            put(JSONObject().apply { put("type", "dns"); put("tag", "dns-out") })
        })

        config.put("route", JSONObject().apply {
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("outbound", "dns-out")
                    put("protocol", "dns")
                })
                put(JSONObject().apply {
                    put("rule_set", JSONArray().put("geosite-cn"))
                    put("outbound", "block")
                })
            })
            put("final", server.id)
            put("auto_detect_interface", true)
        })

        config.put("experimental", JSONObject().apply {
            put("cache_file", JSONObject().apply { put("enabled", true); put("path", "") })
        })

        return config.toString(2)
    }

    private fun buildOutbound(server: SubServer): JSONObject {
        val proto = server.protocol.uppercase()
        return when {
            proto.contains("VLESS") -> buildVlessOutbound(server)
            proto.contains("TROJAN") -> buildTrojanOutbound(server)
            proto.contains("HYSTERIA") || proto.contains("HY2") || proto == "HYSTERIA 2" -> buildHysteria2Outbound(server)
            proto.contains("SHADOWSOCKS") || proto.contains("SS") -> buildShadowsocksOutbound(server)
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
            put("flow", s.flow.ifEmpty { "" })
            put("packet_encoding", "xudp")
        }

        if (s.tls || s.protocol.contains("REALITY", true) || s.protocol.contains("TLS", true)) {
            val tls = JSONObject().apply {
                put("enabled", true)
                put("server_name", s.sni.ifEmpty { s.address })
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
        if (network == "xhttp" || network == "ws" || network == "grpc") {
            val transport = JSONObject().apply { put("type", network) }
            when (network) {
                "xhttp" -> {
                    transport.put("path", "/xh")
                    transport.put("mode", "stream-up")
                }
                "ws" -> {
                    transport.put("path", "/")
                    transport.put("headers", JSONObject().apply {
                        if (s.sni.isNotEmpty()) put("Host", s.sni)
                    })
                }
            }
            out.put("transport", transport)
        }

        return out
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
            put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", s.sni.ifEmpty { s.address })
                put("alpn", JSONArray().put("h3"))
            })
        }
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
