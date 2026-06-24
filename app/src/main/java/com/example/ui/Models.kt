package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class RoutingMode {
    GLOBAL,
    RULE,
    DIRECT
}

data class Server(
    val id: String,
    val flag: String,
    val name: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val latency: Int,
    val usedTraffic: Double,
    val totalTraffic: Double,
    val isCustom: Boolean = false
)

data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val serverCount: Int,
    val status: String, // OK, ERROR, UPDATING
    val lastUpdated: String,
    val expiryDays: Int
)

data class RoutingRule(
    val id: String,
    val type: String, // Domain, IP CIDR, GeoIP, Keyword, Process
    val value: String,
    val action: String, // Proxy, Direct, Block
    val isEnabled: Boolean = true
)

object RedShiftState {
    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)
    var selectedServerId by mutableStateOf("nl_reality")
    var routingMode by mutableStateOf(RoutingMode.RULE)
    
    // Traffic telemetry
    var downloadSpeed by mutableStateOf(0.0)
    var uploadSpeed by mutableStateOf(0.0)
    var sessionDurationSeconds by mutableStateOf(0L)
    var totalDataUsedMb by mutableStateOf(847.0)

    // User settings
    var startOnBoot by mutableStateOf(false)
    var vpnNotification by mutableStateOf(true)
    var killSwitch by mutableStateOf(false)
    var autoReconnect by mutableStateOf(true)
    var localPort by mutableStateOf(1080)
    var dnsProvider by mutableStateOf("Cloudflare")
    var allowLan by mutableStateOf(false)
    var ipv6Support by mutableStateOf(true)
    var muxEnabled by mutableStateOf(false)
    var muxConcurrency by mutableStateOf(4)
    var latencyThreshold by mutableStateOf(200)

    // Predefined Rules
    var bypassLocal by mutableStateOf(true)
    var bypassLan by mutableStateOf(true)
    var bypassChina by mutableStateOf(false)
    var bypassRussia by mutableStateOf(false)
    var blockAds by mutableStateOf(true)

    // Onboarding
    var isOnboarded by mutableStateOf(false)

    // Account state
    var telegramToken by mutableStateOf("")
    var isLoggedIn by mutableStateOf(false)
    var subscriptionPlan by mutableStateOf("RedPill Ultra")
    var subscriptionExpiry by mutableStateOf("in 23 days")

    // Lists
    val servers = mutableStateListOf<Server>()
    val subscriptions = mutableStateListOf<Subscription>()
    val routingRules = mutableStateListOf<RoutingRule>()

    private var simulationScope = CoroutineScope(Dispatchers.Main)
    private var simulationJob: Job? = null
    private var timerJob: Job? = null

    init {
        resetDefaultData()
    }

    fun resetDefaultData() {
        servers.clear()
        servers.addAll(listOf(
            Server("nl_reality", "🇳🇱", "NL • Reality #1", "VLESS+Reality", "nl1.redpillcloud.ru", 6443, 12, 2.4, 10.0),
            Server("nl_xhttp", "🇳🇱", "NL • XHTTP CDN", "VLESS+XHTTP", "xhttp.redpillcloud.ru", 443, 34, 1.8, 10.0),
            Server("nl_ss", "🇳🇱", "NL • Shadowsocks", "Shadowsocks 2022", "nl-ss.redpillcloud.ru", 11444, 18, 0.0, 10.0),
            Server("nl_hysteria", "🇳🇱", "NL • Hysteria2", "Hysteria 2", "nl-hy.redpillcloud.ru", 2443, 156, 5.2, 10.0),
            Server("eu_tls", "🇪🇺", "EU • TLS", "VLESS+TLS", "eu1.redpillcloud.ru", 5443, 45, 3.1, 10.0),
            Server("eu_reality", "🇪🇺", "EU • Reality", "VLESS+Reality", "eu2.redpillcloud.ru", 9443, 38, 0.9, 10.0),
            Server("eu_trojan", "🇪🇺", "EU • Trojan", "Trojan+TLS", "eu-tr.redpillcloud.ru", 7443, 41, 0.0, 10.0),
            Server("ru_hysteria", "🇷🇺", "VDSina • Hysteria2", "Hysteria 2", "ru-hy.redpillcloud.ru", 2443, 8, 7.6, 10.0),
            Server("nl_trust", "🔒", "TrustTunnel • NL", "TrustTunnel", "tt.redpillcloud.ru", 10443, 22, 0.4, 10.0),
            Server("nl_amnezia", "🔰", "AmneziaWG • NL", "AmneziaWG", "awg.redpillcloud.ru", 15, 15, 0.0, 10.0)
        ))

        subscriptions.clear()
        subscriptions.addAll(listOf(
            Subscription("sub_main", "RedPill Default Config", "https://redpillcloud.ru/sub/rp_user_948", 10, "OK", "2 hours ago", 23)
        ))

        routingRules.clear()
        routingRules.addAll(listOf(
            RoutingRule("rule_1", "Domain Keyword", "google", "Proxy"),
            RoutingRule("rule_2", "Domain Keyword", "telegram", "Proxy"),
            RoutingRule("rule_3", "IP CIDR", "10.0.0.0/8", "Direct"),
            RoutingRule("rule_4", "GeoIP", "CN", "Direct")
        ))
    }

    fun getSelectedServer(): Server? {
        return servers.find { it.id == selectedServerId }
    }

    fun toggleVpn() {
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                connectionState = ConnectionState.CONNECTING
                simulationJob?.cancel()
                simulationJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(1500) // Simulate loading delay
                    connectionState = ConnectionState.CONNECTED
                    startTelemetrySimulation()
                }
            }
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                connectionState = ConnectionState.DISCONNECTED
                downloadSpeed = 0.0
                uploadSpeed = 0.0
                simulationJob?.cancel()
                timerJob?.cancel()
            }
        }
    }

    private fun startTelemetrySimulation() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (connectionState == ConnectionState.CONNECTED) {
                delay(1000)
                sessionDurationSeconds++
                
                // Add minor random traffic increment
                val tx = Random.nextDouble(0.1, 0.8)
                val rx = Random.nextDouble(0.5, 3.5)
                downloadSpeed = rx * 8.2
                uploadSpeed = tx * 4.1
                
                totalDataUsedMb += (tx + rx) / 1024.0
            }
        }
    }

    fun login(token: String) {
        telegramToken = token
        isLoggedIn = true
        subscriptionPlan = "RedPill Pro (Premium)"
        subscriptionExpiry = "in 45 days"
        // Insert a server from telegram API
        if (servers.none { it.id == "telegram_custom" }) {
            servers.add(0, Server("telegram_custom", "🇳🇱", "NL • Premium TG Node", "VLESS+Reality", "tg1.redpillcloud.ru", 9443, 14, 0.1, 25.0))
        }
    }

    fun logout() {
        telegramToken = ""
        isLoggedIn = false
        subscriptionPlan = "RedPill Free"
        subscriptionExpiry = "N/A"
        servers.removeAll { it.id == "telegram_custom" }
    }
}
