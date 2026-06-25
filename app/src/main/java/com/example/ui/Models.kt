package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.service.*
import kotlinx.coroutines.*

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
    val latency: Int = 0,
    val usedTraffic: Double = 0.0,
    val totalTraffic: Double = 0.0,
    val isCustom: Boolean = false,
    val subUuid: String = "",
    val subPassword: String = "",
    val subFlow: String = "",
    val subEncryption: String = "none",
    val subNetwork: String = "tcp",
    val subTls: Boolean = false,
    val subSni: String = "",
    val subPublicKey: String = "",
    val subShortId: String = "",
    val subFingerprint: String = "chrome"
)

data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val serverCount: Int,
    val status: String,
    val lastUpdated: String,
    val expiryDays: Int
)

data class RoutingRule(
    val id: String,
    val type: String,
    val value: String,
    val action: String,
    val isEnabled: Boolean = true
)

object RedShiftState {
    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)
    var selectedServerId by mutableStateOf("nl_reality")
    var routingMode by mutableStateOf(RoutingMode.RULE)

    var downloadSpeed by mutableStateOf(0.0)
    var uploadSpeed by mutableStateOf(0.0)
    var sessionDurationSeconds by mutableStateOf(0L)
    var totalDataUsedMb by mutableStateOf(0.0)

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

    var bypassLocal by mutableStateOf(true)
    var bypassLan by mutableStateOf(true)
    var bypassChina by mutableStateOf(false)
    var bypassRussia by mutableStateOf(false)
    var blockAds by mutableStateOf(true)

    var isOnboarded by mutableStateOf(false)

    var telegramToken by mutableStateOf("")
    var isLoggedIn by mutableStateOf(false)
    var subscriptionPlan by mutableStateOf("RedPill Free")
    var subscriptionExpiry by mutableStateOf("N/A")
    var subscriptionUrl by mutableStateOf("")

    var isImporting by mutableStateOf(false)
    var importError by mutableStateOf<String?>(null)
    var loginError by mutableStateOf<String?>(null)
    var isLoadingUser by mutableStateOf(false)

    var userInfo by mutableStateOf<UserInfo?>(null)

    var apiBaseUrl by mutableStateOf("https://api.redpillcloud.ru")
    var apiAdminToken by mutableStateOf("")

    var autoRefresh by mutableStateOf(false)
    var autoRefreshInterval by mutableStateOf(6)
    var cachedServerCount by mutableStateOf(0)
    var lastRefreshTime by mutableStateOf(0L)

    val servers = mutableStateListOf<Server>()
    val subscriptions = mutableStateListOf<Subscription>()
    val routingRules = mutableStateListOf<RoutingRule>()

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var telemetryJob: Job? = null

    private var settingsStore: SettingsStore? = null
    private var appContext: Context? = null
    private var singBoxManager: SingBoxManager? = null
    private var remoteHost = "216.57.106.89"
    private var remotePort = 995

    private val apiClient: RedPillApiClient
        get() = RedPillApiClient(baseUrl = apiBaseUrl, adminToken = apiAdminToken)

    fun init(context: Context) {
        appContext = context.applicationContext
        settingsStore = SettingsStore(context.applicationContext)
        singBoxManager = SingBoxManager(context.applicationContext)

        resetDefaultData()
        loadFromSettings()
        loadCachedServers()
    }

    private fun loadCachedServers() {
        val store = settingsStore ?: return
        scope.launch {
            val json = store.getBlockingCachedServersJson()
            if (json.isNotBlank()) {
                try {
                    val arr = org.json.JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getString("id")
                        if (servers.none { it.id == id }) {
                            servers.add(Server(
                                id = id,
                                name = obj.optString("name", ""),
                                protocol = obj.optString("protocol", ""),
                                address = obj.optString("address", ""),
                                port = obj.optInt("port", 443),
                                flag = obj.optString("flag", "🌐"),
                                subUuid = obj.optString("uuid", ""),
                                subPassword = obj.optString("password", ""),
                                subFlow = obj.optString("flow", ""),
                                subEncryption = obj.optString("encryption", "none"),
                                subNetwork = obj.optString("network", "tcp"),
                                subTls = obj.optBoolean("tls", false),
                                subSni = obj.optString("sni", ""),
                                subPublicKey = obj.optString("publicKey", ""),
                                subShortId = obj.optString("shortId", ""),
                                subFingerprint = obj.optString("fingerprint", "chrome")
                            ))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun loadFromSettings() {
        val store = settingsStore ?: return
        scope.launch {
            store.selectedServerId.collect { id ->
                if (id.isNotEmpty()) selectedServerId = id
            }
        }
        scope.launch {
            store.subscriptionUrl.collect { url ->
                subscriptionUrl = url
            }
        }
        scope.launch {
            store.startOnBoot.collect { startOnBoot = it }
        }
        scope.launch {
            store.killSwitch.collect { killSwitch = it }
        }
        scope.launch {
            store.autoReconnect.collect { autoReconnect = it }
        }
        scope.launch {
            store.allowLan.collect { allowLan = it }
        }
        scope.launch {
            store.notifications.collect { vpnNotification = it }
        }
        scope.launch {
            store.autoRefresh.collect { autoRefresh = it }
        }
        scope.launch {
            store.refreshIntervalHours.collect { autoRefreshInterval = it }
        }
        scope.launch {
            store.cachedServersCount.collect { cachedServerCount = it }
        }
        scope.launch {
            store.lastRefreshTime.collect { lastRefreshTime = it }
        }
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

        importError = null
    }

    fun addServersFromSubResult(subServers: List<SubServer>) {
        for (sub in subServers) {
            val id = sub.id
            if (servers.none { it.id == id }) {
                servers.add(
                    Server(
                        id = id,
                        flag = sub.flag,
                        name = sub.name,
                        protocol = sub.protocol,
                        address = sub.address,
                        port = sub.port,
                        subUuid = sub.uuid,
                        subPassword = sub.password,
                        subFlow = sub.flow,
                        subEncryption = sub.encryption,
                        subNetwork = sub.network,
                        subTls = sub.tls,
                        subSni = sub.sni,
                        subPublicKey = sub.publicKey,
                        subShortId = sub.shortId,
                        subFingerprint = sub.fingerprint
                    )
                )
            }
        }
    }

    fun getSelectedServer(): Server? {
        return servers.find { it.id == selectedServerId }
    }

    fun toggleVpn() {
        val ctx = appContext ?: return
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                connectionState = ConnectionState.CONNECTING

                scope.launch {
                    val server = getSelectedServer()
                    val useLocalProxy = server != null

                    if (useLocalProxy) {
                        val subServer = server!!.let {
                            SubServer(
                                id = it.id,
                                name = it.name,
                                protocol = it.protocol,
                                address = it.address,
                                port = it.port,
                                flag = it.flag,
                                uuid = it.subUuid,
                                password = it.subPassword,
                                flow = it.subFlow,
                                encryption = it.subEncryption,
                                network = it.subNetwork,
                                tls = it.subTls,
                                sni = it.subSni,
                                publicKey = it.subPublicKey,
                                shortId = it.subShortId,
                                fingerprint = it.subFingerprint
                            )
                        }
                        val config = SingBoxConfigGenerator().generateConfig(subServer)

                        val manager = singBoxManager ?: return@launch
                        val binaryOk = manager.ensureBinary()
                        if (!binaryOk) {
                            connectionState = ConnectionState.DISCONNECTED
                            importError = "Failed to extract sing-box binary"
                            return@launch
                        }

                        val started = manager.start(config)
                        if (!started) {
                            connectionState = ConnectionState.DISCONNECTED
                            importError = "Failed to start sing-box"
                            return@launch
                        }
                    }

                    if (!useLocalProxy) {
                        val proxy = apiClient.getProxy()
                        remoteHost = proxy?.host ?: "216.57.106.89"
                        remotePort = proxy?.port ?: 995
                    }

                    val intent = Intent(ctx, RedShiftVpnService::class.java).apply {
                        action = RedShiftVpnService.ACTION_CONNECT
                        putExtra(RedShiftVpnService.EXTRA_USE_LOCAL_PROXY, useLocalProxy)
                        if (!useLocalProxy) {
                            putExtra(RedShiftVpnService.EXTRA_HOST, remoteHost)
                            putExtra(RedShiftVpnService.EXTRA_PORT, remotePort)
                        }
                    }

                    try {
                        ctx.startForegroundService(intent)
                        startTelemetrySimulation()
                        delay(3000)
                        connectionState = ConnectionState.CONNECTED
                    } catch (e: Exception) {
                        connectionState = ConnectionState.DISCONNECTED
                        singBoxManager?.stop()
                    }
                }
            }
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                connectionState = ConnectionState.DISCONNECTED
                val intent = Intent(ctx, RedShiftVpnService::class.java).apply {
                    action = RedShiftVpnService.ACTION_DISCONNECT
                }
                ctx.startService(intent)
                singBoxManager?.stop()
                downloadSpeed = 0.0
                uploadSpeed = 0.0
                stopTelemetrySimulation()
            }
        }
    }

    private fun startTelemetrySimulation() {
        stopTelemetrySimulation()
        timerJob = scope.launch {
            while (connectionState == ConnectionState.CONNECTED) {
                delay(1000)
                sessionDurationSeconds++
            }
        }
        telemetryJob = scope.launch {
            while (connectionState == ConnectionState.CONNECTED) {
                downloadSpeed = kotlin.random.Random.nextDouble(2.0, 45.0)
                uploadSpeed = kotlin.random.Random.nextDouble(0.5, 12.0)
                totalDataUsedMb += kotlin.random.Random.nextDouble(0.001, 0.05)
                delay(2000)
            }
        }

        scope.launch {
            delay(2000)
            if (connectionState == ConnectionState.CONNECTING) {
                connectionState = ConnectionState.CONNECTED
            }
        }
    }

    private fun stopTelemetrySimulation() {
        timerJob?.cancel()
        telemetryJob?.cancel()
        sessionDurationSeconds = 0L
    }

    fun importSubscription(url: String) {
        if (url.isBlank()) return
        isImporting = true
        importError = null

        scope.launch {
            val client = SubscriptionClient()
            val result = client.fetchSubscription(url)

            if (result.error != null) {
                importError = result.error
            } else if (result.servers.isNotEmpty()) {
                addServersFromSubResult(result.servers)

                val subId = "sub_${System.currentTimeMillis()}"
                subscriptions.add(
                    Subscription(
                        id = subId,
                        name = result.servers.first().name.take(20).ifEmpty { "Imported" },
                        url = url,
                        serverCount = result.servers.size,
                        status = "OK",
                        lastUpdated = "Just now",
                        expiryDays = 30
                    )
                )

                settingsStore?.let { store ->
                    store.setSubscriptionUrl(url)
                    if (autoRefresh) {
                        AutoRefreshScheduler.schedule(appContext!!, autoRefreshInterval.toLong())
                    }
                }

                subscriptionUrl = url
            } else {
                importError = "No servers found in subscription"
            }

            isImporting = false
        }
    }

    fun login(tgId: Int) {
        loginError = null
        isLoadingUser = true

        scope.launch {
            val user = apiClient.getUser(tgId)
            if (user != null) {
                userInfo = user
                telegramToken = tgId.toString()
                isLoggedIn = true
                subscriptionPlan = user.subscription?.tariff ?: "No subscription"
                subscriptionExpiry = user.subscription?.expiresAt ?: "N/A"
            } else {
                loginError = "User not found or API error"
                isLoggedIn = false
            }
            isLoadingUser = false
        }
    }

    fun refreshUserData(tgId: Int) {
        if (!isLoggedIn) return
        scope.launch {
            val user = apiClient.getUser(tgId)
            if (user != null) {
                userInfo = user
                subscriptionPlan = user.subscription?.tariff ?: "No subscription"
                subscriptionExpiry = user.subscription?.expiresAt ?: "N/A"
            }
        }
    }

    fun logout() {
        telegramToken = ""
        isLoggedIn = false
        userInfo = null
        subscriptionPlan = "RedPill Free"
        subscriptionExpiry = "N/A"
        loginError = null
        servers.removeAll { it.id == "telegram_custom" }
    }

    fun setAutoRefreshEnabled(enabled: Boolean, intervalHours: Int = 6) {
        autoRefresh = enabled
        autoRefreshInterval = intervalHours
        val ctx = appContext ?: return
        scope.launch {
            settingsStore?.setAutoRefresh(enabled)
            settingsStore?.setRefreshIntervalHours(intervalHours)
            if (enabled && subscriptionUrl.isNotBlank()) {
                AutoRefreshScheduler.schedule(ctx, intervalHours.toLong())
            } else if (!enabled) {
                AutoRefreshScheduler.cancel(ctx)
            }
        }
    }
}
