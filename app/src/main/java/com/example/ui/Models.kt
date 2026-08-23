package com.example.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import android.net.Uri
import android.os.PowerManager

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
    val subFingerprint: String = "chrome",
    val subscriptionUrl: String = "",
    val subPrivateKey: String = "",
    val subPresharedKey: String = "",
    val subServerPublicKey: String = "",
    val subLocalAddress: String = "",
    val subMtu: Int = 1420,
    val subAwgParams: String = "",
    val subDns: String = ""
)

data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val serverCount: Int,
    val status: String,
    val lastUpdated: String,
    val expiryDays: Int,
    val expiryTimestamp: Long = 0,
    val usedTraffic: Long = 0,
    val totalTraffic: Long = 0
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
    var subscriptionPlan by mutableStateOf("")
    var subscriptionExpiry by mutableStateOf("")
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
    val recentServers = mutableStateListOf<Server>()
    val subscriptions = mutableStateListOf<Subscription>()
    val routingRules = mutableStateListOf<RoutingRule>()

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var telemetryJob: Job? = null
    private var trafficMonitor: com.example.service.TrafficMonitor? = null
    private var lastStreamUpdate = 0L
    private var lastConfig = ""
    var sortByPing by mutableStateOf(false)

    private var settingsStore: SettingsStore? = null
    private var appContext: Context? = null
    private var singBoxManager: SingBoxManager? = null
    private var remoteHost = "37.220.84.106"
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
        requestBatteryOptimization()
        scope.launch {
            kotlinx.coroutines.delay(2500)
            pingAllServers()
        }
        scope.launch {
            kotlinx.coroutines.delay(1500)
            val store = settingsStore ?: return@launch
            val savedUrl = runCatching { store.subscriptionUrl.first() }.getOrDefault("")
            if (savedUrl.startsWith("http") && subscriptionPlan.isBlank()) {
                debugLog("auto-resync subscription on launch")
                importSubscription(savedUrl)
            }
        }
    }

    private fun requestBatteryOptimization() {
        val ctx = appContext ?: return
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) return
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun debugLog(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val line = "$ts $msg\n"
        try {
            val f = java.io.File(appContext?.filesDir, "redshift_debug.log")
            f.appendText(line)
        } catch (_: Exception) {}
        Log.e("RedShift", msg)
    }

    fun readDebugLog(): String {
        return try {
            val f = java.io.File(appContext?.filesDir, "redshift_debug.log")
            if (f.exists()) f.readText().takeLast(4000) else "empty"
        } catch (e: Exception) { "err: ${e.message}" }
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
                                flag = obj.optString("flag", "рџЊђ"),
                                subUuid = obj.optString("uuid", ""),
                                subPassword = obj.optString("password", ""),
                                subFlow = obj.optString("flow", ""),
                                subEncryption = obj.optString("encryption", "none"),
                                subNetwork = obj.optString("network", "tcp"),
                                subTls = obj.optBoolean("tls", false),
                                subSni = obj.optString("sni", ""),
                                subPublicKey = obj.optString("publicKey", ""),
                                subShortId = obj.optString("shortId", ""),
                                subFingerprint = obj.optString("fingerprint", "chrome"),
                                subPrivateKey = obj.optString("privateKey", ""),
                                subPresharedKey = obj.optString("presharedKey", ""),
                                subServerPublicKey = obj.optString("serverPublicKey", ""),
                                subLocalAddress = obj.optString("localAddress", ""),
                                subMtu = obj.optInt("mtu", 0),
                                subAwgParams = obj.optString("awgParams", ""),
                                subDns = obj.optString("dns", "")
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
        scope.launch {
            store.tariffName.collect { if (it.isNotBlank()) subscriptionPlan = it }
        }
        scope.launch {
            store.subscriptionExpiry.collect { if (it.isNotBlank()) subscriptionExpiry = it }
        }
        scope.launch {
            val savedLang = runCatching { store.language.first() }.getOrDefault("")
            val resolved = when {
                savedLang == "RU" -> com.example.ui.AppLanguage.RU
                savedLang == "EN" -> com.example.ui.AppLanguage.EN
                else -> if (java.util.Locale.getDefault().language == "ru") com.example.ui.AppLanguage.RU
                        else com.example.ui.AppLanguage.EN
            }
            LocalizationState.currentLanguage = resolved
            if (savedLang.isBlank()) {
                runCatching { store.setLanguage(resolved.code) }
            }
        }
        scope.launch {
            store.userId.collect { uid ->
                if (uid.isNotBlank()) {
                    telegramToken = uid
                    isLoggedIn = true
                }
            }
        }
    }

    fun resetDefaultData() {
        servers.clear()
        recentServers.clear()
        subscriptions.clear()
        routingRules.clear()
        importError = null
    }

    fun pingAllServers() {
        val snapshot = servers.toList()
        scope.launch(Dispatchers.IO) {
            for (server in snapshot) {
                val index = servers.indexOfFirst { it.id == server.id }
                if (index < 0) continue
                val ms = com.example.service.ServerPinger.ping(server.address, server.port, server.protocol)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    val i = servers.indexOfFirst { it.id == server.id }
                    if (i >= 0) servers[i] = servers[i].copy(latency = ms)
                }
            }
        }
    }

    fun addServersFromSubResult(subServers: List<SubServer>, subUrl: String = "") {
        servers.removeAll { it.subscriptionUrl == subUrl }
        val nameCount = HashMap<String, Int>()
        for (sub in subServers) {
            var name = sub.name
            val count = nameCount.getOrDefault(name, 0)
            nameCount[name] = count + 1
            if (count > 0) {
                val region = deriveRegionFromAddress(sub.address)
                name = "$name ($region)"
            }
            val flag = if (sub.flag == "рџЊђ" || sub.flag.isEmpty()) {
                val regionFlag = flagFromRegion(deriveRegionFromAddress(sub.address))
                regionFlag.ifEmpty { sub.flag }
            } else sub.flag
            val id = if (count > 0) "${sub.id}_$count" else sub.id
            if (servers.none { it.id == id }) {
                servers.add(
                    Server(
                        id = id,
                        flag = flag,
                        name = name,
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
                        subFingerprint = sub.fingerprint,
                        subscriptionUrl = subUrl,
                        subPrivateKey = sub.privateKey,
                        subPresharedKey = sub.presharedKey,
                        subServerPublicKey = sub.serverPublicKey,
                        subLocalAddress = sub.localAddress,
                        subMtu = sub.mtu,
                        subAwgParams = sub.awgParams,
                        subDns = sub.dns
                    )
                )
            }
        }
    }

    private fun deriveRegionFromAddress(address: String): String {
        val ip = address.trim()
        return when {
            ip.startsWith("37.220.") -> "NL"
            ip.startsWith("217.156.") -> "EU"
            ip.contains("amsterdam") || ip.contains("nl.") -> "NL"
            ip.contains("frankfurt") || ip.contains("de.") || ip.contains("eu.") -> "EU"
            ip.contains("us.") || ip.contains("usa") -> "US"
            ip.contains("uk.") || ip.contains("gb.") -> "UK"
            ip.contains("sg.") -> "SG"
            else -> ""
        }
    }

    private fun flagFromRegion(region: String): String = when (region) {
        "NL" -> "рџ‡ірџ‡±"
        "EU" -> "рџ‡Єрџ‡є"
        "US" -> "рџ‡єрџ‡ё"
        "UK" -> "рџ‡¬рџ‡§"
        "SG" -> "рџ‡ёрџ‡¬"
        "DE" -> "рџ‡©рџ‡Є"
        else -> ""
    }

    fun getSelectedServer(): Server? {
        return servers.find { it.id == selectedServerId }
    }

    private fun isAmneziaServer(server: Server?): Boolean {
        val p = server?.protocol?.uppercase() ?: return false
        return p.contains("AMNEZIA") || p.contains("AWG") || p.contains("WIREGUARD") || p == "WG"
    }

    private fun Server.toSubServer(): SubServer {
        return SubServer(
            id = id,
            name = name,
            protocol = protocol,
            address = address,
            port = port,
            flag = flag,
            uuid = subUuid,
            password = subPassword,
            flow = subFlow,
            encryption = subEncryption,
            network = subNetwork,
            tls = subTls,
            sni = subSni,
            publicKey = subPublicKey,
            shortId = subShortId,
            fingerprint = subFingerprint,
            privateKey = subPrivateKey,
            presharedKey = subPresharedKey,
            serverPublicKey = subServerPublicKey,
            localAddress = subLocalAddress,
            mtu = subMtu,
            awgParams = subAwgParams,
            dns = subDns
        )
    }

    fun removeServer(serverId: String) {
        servers.removeAll { it.id == serverId }
        recentServers.removeAll { it.id == serverId }
        if (selectedServerId == serverId) {
            selectedServerId = servers.firstOrNull()?.id ?: ""
        }
    }

    fun markServerUsed(serverId: String) {
        val server = servers.find { it.id == serverId } ?: return
        recentServers.removeAll { it.id == serverId }
        recentServers.add(0, server)
        if (recentServers.size > 5) {
            recentServers.removeAt(recentServers.lastIndex)
        }
    }

    fun toggleVpn() {
        Log.e("RedShiftVPN", "toggleVpn() called, state=$connectionState")
        val ctx = appContext
        if (ctx == null) {
            Log.e("RedShiftVPN", "appContext is null")
            return
        }
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                connectionState = ConnectionState.CONNECTING
                Log.e("RedShiftVPN", "Starting VPN connect sequence")

                scope.launch {
                    try {
                        debugLog("coroutine started, state=$connectionState")
                        val server = getSelectedServer()
                        debugLog("server=${server?.name}, proto=${server?.protocol}")

                        if (server == null) {
                            debugLog("server is null, abort")
                            connectionState = ConnectionState.DISCONNECTED
                            return@launch
                        }
                        markServerUsed(server.id)

                        val ok = singBoxManager?.ensureBinary() == true
                        debugLog("binary ok=$ok")
                        if (!ok) {
                            connectionState = ConnectionState.DISCONNECTED
                            return@launch
                        }

                        val sub = server.toSubServer()
                        val ctx = appContext
                        val filesDir = ctx?.filesDir?.absolutePath ?: ""
                        try {
                            if (ctx != null && filesDir.isNotEmpty()) {
                                val srsFile = java.io.File(filesDir, "geoip-ru.srs")
                                if (!srsFile.exists()) {
                                    ctx.assets.open("geoip-ru.srs").use { input ->
                                        srsFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                        debugLog("generating config...")
                        val config = withContext(Dispatchers.IO) { SingBoxConfigGenerator().generateConfig(sub, SingBoxManager.SOCKS_PORT, filesDir) }
                        debugLog("config length=${config.length}")
                        lastConfig = config

                        val isAwg = SingBoxConfigGenerator().isAmneziaProtocol(sub)

                        debugLog("TUN fd passing for all protocols")
                        val intent1 = Intent(appContext, RedShiftVpnService::class.java).apply {
                            action = RedShiftVpnService.ACTION_CONNECT_AWG
                        }
                        appContext?.startService(intent1)
                        var tunFd = -1
                        // Race fix: on some OEMs (vivo) VpnService.Builder.establish() takes
                        // ~10s, so poll up to 40s (was 10s) before giving up on the fd.
                        for (i in 1..160) {
                            Thread.sleep(250)
                            tunFd = RedShiftVpnService.getLastTunFdRaw()
                            if (tunFd > 0) break
                        }
                        debugLog("TUN fd=$tunFd after wait")
                        if (tunFd > 0) {
                            val started = withContext(Dispatchers.IO) { singBoxManager?.startWithTunFd(config, tunFd) == true }
                            debugLog("sing-box started with tun fd=$started")
                            if (!started) {
                                // Fail-close: never fall back to mixed SOCKS (would leak UDP/DNS).
                                debugLog("sing-box TUN failed, fail-close (no mixed fallback)")
                                singBoxManager?.stop()
                                RedShiftVpnService.resetTunState()
                                appContext?.startService(Intent(appContext, RedShiftVpnService::class.java).apply {
                                    action = RedShiftVpnService.ACTION_DISCONNECT
                                })
                                connectionState = ConnectionState.DISCONNECTED
                                return@launch
                            }
                        } else {
                            // TUN fd never arrived — fail-close instead of leaking via mixed SOCKS.
                            debugLog("TUN fd not available, fail-close (no mixed fallback)")
                            singBoxManager?.stop()
                            RedShiftVpnService.resetTunState()
                            appContext?.startService(Intent(appContext, RedShiftVpnService::class.java).apply {
                                action = RedShiftVpnService.ACTION_DISCONNECT
                            })
                            connectionState = ConnectionState.DISCONNECTED
                            return@launch
                        }

                        connectionState = ConnectionState.CONNECTED
                        debugLog("setting CONNECTED")
                        totalDataUsedMb = 0.0
                        startSessionTimer()
                    } catch (e: Exception) {
                        debugLog("EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}")
                        connectionState = ConnectionState.DISCONNECTED
                    }
                }
            }
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                connectionState = ConnectionState.DISCONNECTED
                lastConfig = ""
                if (singBoxManager?.isRunning() == true) {
                    debugLog("stopping sing-box")
                }
                val intent = Intent(ctx, RedShiftVpnService::class.java).apply {
                    action = RedShiftVpnService.ACTION_DISCONNECT
                }
                ctx.startService(intent)
                singBoxManager?.stop()
                RedShiftVpnService.resetTunState()
                stopTelemetrySimulation()
            }
        }
    }

    private fun startSessionTimer() {
        stopTelemetrySimulation()
        lastStreamUpdate = 0L
        val monitor = trafficMonitor ?: com.example.service.TrafficMonitor().also { trafficMonitor = it }
        var streamUp = 0.0
        var streamDown = 0.0
        monitor.start(object : com.example.service.TrafficMonitor.Listener {
            override fun onTrafficUpdate(upBps: Long, downBps: Long) {
                uploadSpeed = upBps / 1024.0
                downloadSpeed = downBps / 1024.0
                streamUp += upBps
                streamDown += downBps
                totalDataUsedMb = (streamUp + streamDown) / (1024.0 * 1024.0)
                lastStreamUpdate = System.currentTimeMillis()
            }
        })
        timerJob = scope.launch {
            while (connectionState == ConnectionState.CONNECTED) {
                delay(1000)
                sessionDurationSeconds++

                if (System.currentTimeMillis() - lastStreamUpdate > 3000) {
                    val up = com.example.service.TcpTunnel.totalBytesUp.toDouble()
                    val down = com.example.service.TcpTunnel.totalBytesDown.toDouble()
                    if (up + down > 0) {
                        totalDataUsedMb = (up + down) / (1024.0 * 1024.0)
                        downloadSpeed = 0.0
                        uploadSpeed = 0.0
                    }
                }

                val cfg = lastConfig
                if (cfg.isNotBlank() && singBoxManager?.isRunning() != true) {
                    debugLog("watchdog: sing-box died, restarting")
                    val fd = com.example.service.RedShiftVpnService.getLastTunFdRaw()
                    val ok = if (fd > 0) {
                        kotlinx.coroutines.withContext(Dispatchers.IO) { singBoxManager?.startWithTunFd(cfg, fd) == true }
                    } else false
                    debugLog("watchdog: restart ok=$ok")
                }
            }
            monitor.stop()
        }
    }

    private fun stopTelemetrySimulation() {
        timerJob?.cancel()
        telemetryJob?.cancel()
        trafficMonitor?.stop()
        sessionDurationSeconds = 0L
    }

    fun importSubscription(url: String) {
        if (url.isBlank()) return
        isImporting = true
        importError = null

        scope.launch {
            val client = SubscriptionClient()
            val result = client.fetchSubscription(url)

            if (result.error.isNotEmpty()) {
                importError = result.error
                debugLog("import FAILED: ${result.error}")
            } else if (result.servers.isNotEmpty()) {
                val dbgFirst = result.servers.first()
                debugLog("import OK: n=${result.servers.size} first=${dbgFirst.protocol} ${dbgFirst.address}:${dbgFirst.port} psk=${dbgFirst.presharedKey.isNotEmpty()} priv=${dbgFirst.privateKey.isNotEmpty()} srvPub=${dbgFirst.serverPublicKey.isNotEmpty()} awgParams=${if (dbgFirst.awgParams.isBlank()) "-" else "yes"}")
                subscriptions.removeAll { it.url == url }
                addServersFromSubResult(result.servers, url)

                val first = result.servers.first()
                if (servers.any { it.id == first.id }) {
                    selectedServerId = first.id
                    settingsStore?.let { store ->
                        store.setSelectedServerId(first.id)
                    }
                }

                cacheServersToStore(result.servers)

                val profile = result.profileInfo
                if (profile != null) {
                    if (profile.expiry > 0) {
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                        subscriptionExpiry = sdf.format(java.util.Date(profile.expiry * 1000))
                    }
                    if (profile.total > 0) {
                        val usedMb = (profile.download + profile.upload) / (1024.0 * 1024.0)
                        val totalMb = profile.total / (1024.0 * 1024.0)
                        totalDataUsedMb = usedMb
                        if (subscriptionPlan.isBlank()) {
                            subscriptionPlan = "Data: %.1f / %.1f GB".format(usedMb / 1024.0, totalMb / 1024.0)
                        }
                    }
                }

                val userIdFromUrl = url.substringAfterLast("/").substringBefore("?").trim()
                if (userIdFromUrl.isNotEmpty() && userIdFromUrl.all { it.isDigit() }) {
                    val user = apiClient.getUser(userIdFromUrl.toIntOrNull() ?: 0)
                    if (user != null) {
                        userInfo = user
                        telegramToken = userIdFromUrl
                        isLoggedIn = true
                        user.subscription?.tariff?.let { subscriptionPlan = it }
                        user.subscription?.expiresAt?.let { subscriptionExpiry = it }
                    }
                    settingsStore?.setUserId(userIdFromUrl)
                }
                settingsStore?.setTariffName(subscriptionPlan)
                settingsStore?.setSubscriptionExpiry(subscriptionExpiry)

                val now = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.US).format(java.util.Date())
                subscriptions.add(
                    Subscription(
                        id = "sub_${System.currentTimeMillis()}",
                        name = result.servers.first().name.take(20).ifEmpty { "Imported" },
                        url = url,
                        serverCount = result.servers.size,
                        status = "OK",
                        lastUpdated = now,
                        expiryDays = 0,
                        expiryTimestamp = profile?.expiry ?: 0,
                        usedTraffic = (profile?.download ?: 0) + (profile?.upload ?: 0),
                        totalTraffic = profile?.total ?: 0
                    )
                )

                settingsStore?.let { store ->
                    store.setSubscriptionUrl(url)
                    if (autoRefresh) {
                        AutoRefreshScheduler.schedule(appContext!!, autoRefreshInterval.toLong())
                    }
                }

                subscriptionUrl = url
                pingAllServers()
            } else {
                importError = "No servers found in subscription"
            }

            isImporting = false
        }
    }

    private suspend fun cacheServersToStore(serverList: List<SubServer>) {
        val store = settingsStore ?: return
        val serversJson = org.json.JSONArray()
        for (s in serverList) {
            serversJson.put(org.json.JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("protocol", s.protocol)
                put("address", s.address)
                put("port", s.port)
                put("flag", s.flag)
                put("uuid", s.uuid)
                put("password", s.password)
                put("flow", s.flow)
                put("encryption", s.encryption)
                put("network", s.network)
                put("tls", s.tls)
                put("sni", s.sni)
                put("publicKey", s.publicKey)
                put("shortId", s.shortId)
                put("fingerprint", s.fingerprint)
                put("privateKey", s.privateKey)
                put("presharedKey", s.presharedKey)
                put("serverPublicKey", s.serverPublicKey)
                put("localAddress", s.localAddress)
                put("mtu", s.mtu)
                put("awgParams", s.awgParams)
                put("dns", s.dns)
            })
        }
        store.setCachedServersJson(serversJson.toString())
        store.setCachedServersCount(serverList.size)
        store.setLastRefreshTime(System.currentTimeMillis())
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
                subscriptionExpiry = user.subscription?.expiresAt ?: ""
                settingsStore?.setUserId(tgId.toString())
                settingsStore?.setTariffName(subscriptionPlan)
                settingsStore?.setSubscriptionExpiry(subscriptionExpiry)
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
                subscriptionExpiry = user.subscription?.expiresAt ?: ""
                settingsStore?.setTariffName(subscriptionPlan)
                settingsStore?.setSubscriptionExpiry(subscriptionExpiry)
            }
        }
    }

    fun setLanguage(lang: com.example.ui.AppLanguage) {
        LocalizationState.currentLanguage = lang
        scope.launch {
            settingsStore?.setLanguage(lang.code)
        }
    }

    fun logout() {
        telegramToken = ""
        isLoggedIn = false
        userInfo = null
        subscriptionPlan = ""
        subscriptionExpiry = ""
        loginError = null
        scope.launch {
            settingsStore?.setUserId("")
            settingsStore?.setTariffName("")
            settingsStore?.setSubscriptionExpiry("")
        }
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
