package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.BuildConfig
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
    val subDns: String = "",
    val subPath: String = "",
    val subHost: String = "",
    val subExtra: String = "",
    val subAlpn: String = ""
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
    const val VPN_CONSENT_REQUEST = 7332
    var currentActivity: android.app.Activity? = null

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
    var bypassRussia by mutableStateOf(true)
    var blockAds by mutableStateOf(false)

    var splitTunnelEnabled by mutableStateOf(false)
    var splitTunnelByApps by mutableStateOf(false)
    var splitTunnelOnly by mutableStateOf(true)
    var splitDomains by mutableStateOf<List<String>>(emptyList())
    var splitApps by mutableStateOf<List<String>>(emptyList())

    var availableUpdate by mutableStateOf<com.example.service.UpdateChecker.UpdateInfo?>(null)
    var isCheckingUpdate by mutableStateOf(false)
    var updateCheckError by mutableStateOf<String?>(null)

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

    /**
     * Why the last connect attempt failed, as reported by the service (e.g. the olcRTC
     * room never came up). Without it a failed connect silently reverted to DISCONNECTED.
     */
    var vpnError by mutableStateOf<String?>(null)

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

    /**
     * Identity keys of servers the user deleted. Loaded from [com.example.service.SettingsStore]
     * and consulted wherever the list is rebuilt (cache loader and subscription importer) so a
     * deleted server stays gone even though its subscription keeps re-sending it.
     */
    var hiddenServers by mutableStateOf<Set<String>>(emptySet())

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var telemetryJob: Job? = null
    private var trafficMonitor: com.example.service.TrafficMonitor? = null
    private var lastStreamUpdate = 0L
    private var lastConfig = ""

    /**
     * Monotonic generation counter for VPN connect/disconnect sessions. Every
     * toggle bumps it; the in-flight connect coroutine checks it at each
     * suspension point and aborts if it no longer matches. This prevents the
     * race where disconnect's stop() runs while the connect coroutine is still
     * polling for a TUN fd and then calls startWithTunFd() on a dead session
     * (native start/stop on different threads -> SEGV, app dies without trace).
     */
    private var vpnGeneration = 0L
    private var connectJob: Job? = null

    /** Native teardown of the previous session; a server switch waits for it. */
    private var teardownJob: Job? = null

    /** True while [restartConnection] is between "stop the old" and "start the new". */
    private var isSwitchingServer = false
    private var switchToken = 0L
    var sortByPing by mutableStateOf(false)
    var autoSelectBestServer by mutableStateOf(false)

    /**
     * Balancer region ("nl", "eu1") → selection score, lower wins. Filled from
     * GET /api/v1/load by the pings and left empty when the API is unreachable, in
     * which case the auto-select ranks purely by ping.
     */
    var serverLoad by mutableStateOf<Map<String, Int>>(emptyMap())

    /** Score of a server the balancer has no metric for: behind every known region. */
    private const val OVERLOADED_SCORE = Int.MAX_VALUE

    /** Region token bounded by separators, so "Premium" does not read as "eu". */
    private val REGION_TOKEN = Regex("(?i)(?:^|[-_/.\\s])(nl|eu1|eu|de|us|uk|sg)(?:$|[-_/.\\s])")

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
        com.example.service.ExpiryReminderScheduler.schedule(context.applicationContext)
        scope.launch {
            kotlinx.coroutines.delay(2500)
            pingAllServers()
        }
        scope.launch {
            kotlinx.coroutines.delay(1500)
            val store = settingsStore ?: return@launch
            val savedUrl = runCatching { store.subscriptionUrl.first() }.getOrDefault("")
            if (savedUrl.startsWith("http") && (!isLoggedIn || subscriptionPlan.isBlank())) {
                debugLog("auto-resync subscription on launch")
                importSubscription(savedUrl)
            }
        }
        scope.launch {
            kotlinx.coroutines.delay(3500)
            checkForUpdates()
        }
    }

    /**
     * Checks GitHub Releases for a newer app version and exposes the result via
     * [availableUpdate]. The UI surfaces a dialog whenever it is non-null.
     */
    fun checkForUpdates() {
        val ctx = appContext ?: return
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        updateCheckError = null
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                com.example.service.UpdateChecker.checkLatest()
            }
            isCheckingUpdate = false
            if (info != null &&
                com.example.service.UpdateChecker.isNewerVersion(info.versionName, BuildConfig.VERSION_NAME)
            ) {
                availableUpdate = info
            } else {
                updateCheckError = "up_to_date"
                delay(4000)
                if (updateCheckError == "up_to_date") updateCheckError = null
            }
        }
    }

    fun clearUpdateDialog() {
        availableUpdate = null
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
        DiagLog.d("RedShift", msg)
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
            // Read blocking here: this runs during init, before the reactive collector in
            // loadFromSettings() has had a chance to populate hiddenServers.
            val hidden = store.getBlockingHiddenServers()
            debugLog("cache load: json=${json.length} hidden=${hidden.size} keys=$hidden")
            if (json.isNotBlank()) {
                var loaded = 0
                try {
                    val arr = org.json.JSONArray(json)
                    // Heal the persisted cache if old builds stored duplicates
                    // (two slices of the same subscription with different ids/IPs,
                    // plus stale url-less rows from past imports).
                    val deduped = dedupeServerArray(arr)
                    for (i in 0 until deduped.length()) {
                        val obj = deduped.getJSONObject(i)
                        val candidate = serverFromCacheObject(obj)
                        if (hideKeysFor(candidate).any { it in hidden }) {
                            debugLog("cache hidden-skip name='${candidate.name}' addr=${candidate.address}:${candidate.port}")
                            continue
                        }
                        if (servers.any { it.id == candidate.id || it.sameEndpointAs(candidate) || it.sameNamedAs(candidate) }) continue
                        servers.add(candidate)
                        loaded++
                    }
                    if (deduped.length() < arr.length()) {
                        store.setCachedServersJson(deduped.toString())
                        store.setCachedServersCount(deduped.length())
                        debugLog("cache heal: ${arr.length()} -> ${deduped.length()}")
                    }
                } catch (e: Exception) {
                    debugLog("cache load failed ${e::class.simpleName}: ${e.message}")
                }
                debugLog("cache load done: loaded=$loaded total=${servers.size}")
            }
        }
    }

    /** Dedupe by exact endpoint, then by logical node name (same name = rotated/regenerated node). */
    private fun dedupeServerArray(arr: org.json.JSONArray): org.json.JSONArray {
        val survivors = mutableListOf<org.json.JSONObject>()
        val seenEndpoint = HashSet<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val key = endpointKey(
                obj.optString("protocol", ""),
                obj.optString("address", ""),
                obj.optInt("port", 0)
            )
            if (key != null && !seenEndpoint.add(key)) continue
            survivors.add(obj)
        }
        val byName = LinkedHashMap<String, MutableList<org.json.JSONObject>>()
        for (obj in survivors) {
            byName.getOrPut(nameKey(obj)) { mutableListOf() }.add(obj)
        }
        val out = org.json.JSONArray()
        val seenName = HashSet<String>()
        for (obj in survivors) {
            val nk = nameKey(obj)
            if (!seenName.add(nk)) continue
            val party = byName[nk] ?: continue
            // Prefer the row tied to a real subscription url (current), drop stale url-less artifacts.
            val winner = party.maxWithOrNull(
                compareBy({ it.optString("url", "").isBlank() }, { it.optString("id", "").isBlank() })
            ) ?: obj
            out.put(winner)
        }
        return out
    }

    private fun nameKey(obj: org.json.JSONObject): String {
        val name = obj.optString("name", "").uppercase().trim().filter { it.isLetterOrDigit() || it == ' ' }.trim()
        val protoFamily = obj.optString("protocol", "").uppercase().substringBefore('+').trim()
        return "$protoFamily::$name"
    }

    private fun endpointKey(protocol: String, address: String, port: Int): String? {
        val addr = address.trim()
        val proto = protocol.uppercase().filter { it.isLetterOrDigit() }
        if (addr.isBlank() || port <= 0) return null
        return "$addr:$port:$proto"
    }

    /**
     * Identity keys of a server used to recognise it after it comes back from the cache or
     * the subscription. The importer already treats a rotated node (same logical name, new
     * id/IP) as the same server, so a deletion has to match on the same set of keys —
     * otherwise the deleted server simply reappears once the subscription rotates its IP.
     */
    private fun hideKeysForParts(id: String, name: String, protocol: String, address: String, port: Int): List<String> {
        val keys = mutableListOf<String>()
        if (id.isNotBlank()) keys.add("id:$id")
        endpointKey(protocol, address, port)?.let { keys.add("ep:$it") }
        val norm = name.uppercase().trim().filter { it.isLetterOrDigit() || it == ' ' }.trim()
        if (norm.isNotBlank()) {
            keys.add("nm:${protocol.uppercase().substringBefore('+').trim()}::$norm")
        }
        return keys
    }

    private fun hideKeysFor(s: Server): List<String> =
        hideKeysForParts(s.id, s.name, s.protocol, s.address, s.port)

    /** True when the server matches any identity key stored in [hiddenServers]. */
    private fun isHiddenParts(id: String, name: String, protocol: String, address: String, port: Int): Boolean {
        if (hiddenServers.isEmpty()) return false
        return hideKeysForParts(id, name, protocol, address, port).any { it in hiddenServers }
    }

    private fun isHiddenServer(candidate: Server): Boolean =
        isHiddenParts(candidate.id, candidate.name, candidate.protocol, candidate.address, candidate.port)

    private fun Server.sameEndpointAs(other: Server): Boolean {
        return endpointKey(protocol, address, port) != null &&
            endpointKey(protocol, address, port) == endpointKey(other.protocol, other.address, other.port)
    }

    private fun Server.sameNamedAs(other: Server): Boolean {
        val protoFamily = protocol.uppercase().substringBefore('+').trim()
        val otherFamily = other.protocol.uppercase().substringBefore('+').trim()
        if (protoFamily != otherFamily) return false
        if (name.isBlank() || other.name.isBlank()) return false
        return name.uppercase().trim().filter { it.isLetterOrDigit() || it == ' ' }.trim() ==
            other.name.uppercase().trim().filter { it.isLetterOrDigit() || it == ' ' }.trim()
    }

    private fun serverFromCacheObject(obj: org.json.JSONObject): Server {
        return Server(
            id = obj.getString("id"),
            name = obj.optString("name", ""),
            protocol = obj.optString("protocol", ""),
            address = obj.optString("address", ""),
            port = obj.optInt("port", 443),
            flag = obj.optString("flag", "🌍"),
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
            subscriptionUrl = obj.optString("url", ""),
            subPrivateKey = obj.optString("privateKey", ""),
            subPresharedKey = obj.optString("presharedKey", ""),
            subServerPublicKey = obj.optString("serverPublicKey", ""),
            subLocalAddress = obj.optString("localAddress", ""),
            subMtu = obj.optInt("mtu", 0),
            subAwgParams = obj.optString("awgParams", ""),
            subDns = obj.optString("dns", ""),
            subPath = obj.optString("path", ""),
            subHost = obj.optString("host", ""),
            subExtra = obj.optString("extra", ""),
            subAlpn = obj.optString("alpn", "")
        )
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
                if (uid.toIntOrNull() != null) {
                    telegramToken = uid
                    isLoggedIn = true
                    val id = uid.toIntOrNull()
                    if (id != null && userInfo == null) refreshUserData(id)
                } else if (uid.isNotBlank()) {
                    // Legacy garbage: an old build wrote a full vpn:// deeplink as the
                    // user id. Never treat it as an account identifier.
                    settingsStore?.setUserId("")
                }
            }
        }
        scope.launch {
            store.splitTunnelEnabled.collect { splitTunnelEnabled = it }
        }
        scope.launch {
            store.splitTunnelByApps.collect { splitTunnelByApps = it }
        }
        scope.launch {
            store.splitTunnelOnly.collect { splitTunnelOnly = it }
        }
        scope.launch {
            store.splitDomains.collect { raw ->
                splitDomains = raw.split('\n').map { it.trim() }.filter { it.isNotBlank() }
            }
        }
        scope.launch {
            store.splitApps.collect { raw ->
                splitApps = raw.split('\n').map { it.trim() }.filter { it.isNotBlank() }
            }
        }
        scope.launch {
            store.routingMode.collect { raw ->
                routingMode = when (raw.lowercase()) {
                    "global" -> RoutingMode.GLOBAL
                    "direct" -> RoutingMode.DIRECT
                    else -> RoutingMode.RULE
                }
            }
        }
        scope.launch {
            store.blockAds.collect { blockAds = it }
        }
        scope.launch {
            store.autoSelectBest.collect { autoSelectBestServer = it }
        }
        scope.launch {
            store.sortByPing.collect { sortByPing = it }
        }
        scope.launch {
            store.hiddenServers.collect { hiddenServers = it }
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
            // Fetched in parallel with the pings: by the time the auto-select below runs
            // the metric is usually ready, and if it is not (API down, first launch) the
            // map simply stays empty and the choice falls back to ping only.
            val loadDeferred = async { apiClient.getLoad() }
            for (server in snapshot) {
                val index = servers.indexOfFirst { it.id == server.id }
                if (index < 0) continue
                // UDP-only protocols (Hysteria, AmneziaWG) have no TCP listener, so a
                // SOCKS CONNECT through the tunnel would fail. While connected the app's
                // sockets bypass the TUN (own package is disallowed), so ICMP still
                // reaches the host directly and reports a real RTT.
                val udpProto = com.example.service.ServerPinger.isUdpBased(server.protocol)
                val viaTunnel = connectionState == ConnectionState.CONNECTED &&
                    com.example.service.RedShiftVpnService.tunReady && !udpProto
                val ms = if (viaTunnel) {
                    com.example.service.ServerPinger.pingViaProxy(
                        server.address, server.port, server.protocol,
                        com.example.service.SingBoxManager.MIXED_PORT
                    )
                } else {
                    com.example.service.ServerPinger.ping(server.address, server.port, server.protocol)
                }
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    val i = servers.indexOfFirst { it.id == server.id }
                    if (i >= 0) servers[i] = servers[i].copy(latency = ms)
                }
            }
            val score = scoreRegions(loadDeferred.await())
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                serverLoad = score
            }
            debugLog("load scores=$score")
            if (autoSelectBestServer && connectionState != ConnectionState.CONNECTED) {
                val best = pickAutoServer(servers.filter { it.latency > 0 })
                if (best != null) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        // Do not steal an in-progress user selection while connecting.
                        if (connectionState != ConnectionState.CONNECTING) {
                            debugLog("auto-select ${best.name} load=${serverLoad[loadKeyFor(best)]} ping=${best.latency}")
                            selectServer(best.id)
                        }
                    }
                }
            }
        }
    }

    /**
     * Turn the raw API metric into the score used by the auto-select.
     *
     * Issued devices are the primary signal: they say how much spare capacity a region
     * still has, and NL (6 devices) has far more of it than EU (250) even though their
     * live peer counts are comparable — which is exactly the imbalance this selection
     * exists to correct. A region is only pushed behind every other region when it is
     * worse on BOTH signals (at least as many devices and more than twice the live
     * peers), so a small region that happens to have a couple of busy peers is not
     * mistaken for a full one.
     */
    private fun scoreRegions(raw: Map<String, com.example.service.RegionLoad>): Map<String, Int> {
        if (raw.isEmpty()) return emptyMap()
        val overloaded = raw.filter { (region, me) ->
            me.active >= 0 && raw.any { (other, theirs) ->
                other != region &&
                    theirs.active >= 0 &&
                    theirs.devices <= me.devices &&
                    me.active > 2 * theirs.active
            }
        }.keys
        return raw.mapValues { (region, load) ->
            if (region in overloaded) OVERLOADED_SCORE else load.devices.coerceAtLeast(0)
        }
    }

    /** Lowest score wins; ping breaks ties. */
    private fun pickAutoServer(candidates: List<Server>): Server? =
        candidates.minWithOrNull(compareBy({ loadScore(it) }, { it.latency }))

    /**
     * Score of a single server. A server whose region has no metric gets
     * [OVERLOADED_SCORE] so it never beats a region we actually have data for — but
     * when no metric is available at all every score is 0 and the comparison collapses
     * to the old fastest-ping behaviour.
     */
    private fun loadScore(s: Server): Int {
        if (serverLoad.isEmpty()) return 0
        return serverLoad[loadKeyFor(s)] ?: OVERLOADED_SCORE
    }

    fun isOlcrtcServer(s: Server?): Boolean =
        s?.protocol?.uppercase(java.util.Locale.US)?.contains("OLCRTC") == true

    /**
     * olcRTC connect failover order: the picked server first, then every other olcRTC
     * server ranked by the same balancer order the auto-select uses. Non-olcRTC servers
     * get a single-element list so a manual pick of another protocol is never overridden.
     *
     * olcRTC rooms sit on third-party meet services, so one of them can be unreachable
     * or flaky for a given operator while another is fine — the client has to move on
     * by itself instead of leaving the user with a dead tunnel.
     */
    fun olcrtcFailoverCandidates(selected: Server): List<Server> {
        if (!isOlcrtcServer(selected)) return listOf(selected)
        val ranked = servers
            .filter { isOlcrtcServer(it) && it.id != selected.id }
            .sortedWith(compareBy({ loadScore(it) }, { it.latency }))
        return listOf(selected) + ranked
    }

    /**
     * Maps a server onto a balancer region key ("nl" / "eu1"), or "" when it belongs to
     * no region we track.
     *
     * Plain VPN rows carry an IP, which [deriveRegionFromAddress] already handles. The
     * olcRTC rows do not — their address is a room URL
     * (https://meet.egovm.ru/redpill-olcrtc-nl) — so for those the region token is
     * matched in the room URL and in the server name instead.
     */
    private fun loadKeyFor(s: Server): String {
        val region = deriveRegionFromAddress(s.address).ifBlank {
            regionTokenIn("${s.address} ${s.name}")
        }
        return when (region) {
            "NL" -> "nl"
            "EU" -> "eu1"
            else -> region.lowercase(java.util.Locale.US)
        }
    }

    /** "…-nl", "…/eu1…", "RedPill EU" → NL / EU / …, anything else → "". */
    private fun regionTokenIn(text: String): String {
        val m = REGION_TOKEN.find(text) ?: return ""
        return when (m.value.lowercase(java.util.Locale.US)) {
            "nl" -> "NL"
            "eu", "eu1", "de" -> "EU"
            "us" -> "US"
            "uk" -> "UK"
            "sg" -> "SG"
            else -> ""
        }
    }

    fun setSortByPingEnabled(enabled: Boolean) {
        sortByPing = enabled
        scope.launch { settingsStore?.setSortByPing(enabled) }
    }

    fun setAutoSelectBest(enabled: Boolean) {
        autoSelectBestServer = enabled
        scope.launch { settingsStore?.setAutoSelectBest(enabled) }
    }

    fun addSplitDomain(domain: String) {
        val d = domain.trim().lowercase().removePrefix("*.").removePrefix("http://").removePrefix("https://")
        if (d.isBlank() || splitDomains.contains(d)) return
        splitDomains = splitDomains + d
        scope.launch { settingsStore?.setSplitDomains(splitDomains.joinToString("\n")) }
    }

    fun removeSplitDomain(domain: String) {
        splitDomains = splitDomains - domain
        scope.launch { settingsStore?.setSplitDomains(splitDomains.joinToString("\n")) }
    }

    fun toggleSplitApp(packageName: String) {
        splitApps = if (splitApps.contains(packageName)) {
            splitApps - packageName
        } else {
            splitApps + packageName
        }
        scope.launch { settingsStore?.setSplitApps(splitApps.joinToString("\n")) }
    }

    fun setSplitEnabled(enabled: Boolean) {
        splitTunnelEnabled = enabled
        scope.launch { settingsStore?.setSplitTunnelEnabled(enabled) }
    }

    fun setSplitByApps(byApps: Boolean) {
        splitTunnelByApps = byApps
        scope.launch { settingsStore?.setSplitTunnelByApps(byApps) }
    }

    fun setSplitOnly(only: Boolean) {
        splitTunnelOnly = only
        scope.launch { settingsStore?.setSplitTunnelOnly(only) }
    }

    fun applyRoutingMode(mode: RoutingMode) {
        routingMode = mode
        scope.launch { settingsStore?.setRoutingMode(mode.name.lowercase()) }
    }

    fun applyBypassLocal(enabled: Boolean) {
        bypassLocal = enabled
        scope.launch { settingsStore?.setBypassLocal(enabled) }
    }

    fun applyBypassLan(enabled: Boolean) {
        bypassLan = enabled
        scope.launch { settingsStore?.setBypassLan(enabled) }
    }

    fun applyBlockAds(enabled: Boolean) {
        blockAds = enabled
        scope.launch { settingsStore?.setBlockAds(enabled) }
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
            val candidate = Server(
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
                subDns = sub.dns,
                subPath = sub.path,
                subHost = sub.host,
                subExtra = sub.extra,
                subAlpn = sub.alpn
            )
            // Skip servers the user deleted earlier, so a subscription refresh does not
            // resurrect them (they are keyed by id / endpoint / logical name).
            if (isHiddenServer(candidate)) {
                debugLog("hidden-skip name='${candidate.name}' addr=${candidate.address}:${candidate.port} keys=${hideKeysForParts(candidate.id, candidate.name, candidate.protocol, candidate.address, candidate.port)}")
                continue
            }
            // Dedupe by id, endpoint and logical name: the whitelist subscription
            // refreshes with new ids/IPs, which used to double entire servers.
            if (servers.none { it.id == id || it.sameEndpointAs(candidate) || it.sameNamedAs(candidate) }) {
                servers.add(candidate)
            } else {
                debugLog("dupe-skip name='${candidate.name}' addr=${candidate.address}:${candidate.port}")
            }
        }
        debugLog("addFromSub url=${subUrl.take(80)} parsed=${subServers.size} kept=${servers.size}")
    }

    private fun expiresAtToMillis(expiresAt: String?): Long {
        if (expiresAt.isNullOrBlank() || expiresAt == "N/A") return 0L
        val s = expiresAt.trim().take(10)
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(s)?.time ?: 0L
        } catch (_: Exception) {
            0L
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
            dns = subDns,
            path = subPath,
            host = subHost,
            extra = subExtra,
            alpn = subAlpn
        )
    }

    fun removeServer(serverId: String) {
        val wasActive = selectedServerId == serverId
        val victim = servers.find { it.id == serverId }
        debugLog("remove id=$serverId name='${victim?.name ?: "-"}' wasActive=$wasActive state=$connectionState")
        victim?.let { hideServer(it) }
        servers.removeAll { it.id == serverId }
        recentServers.removeAll { it.id == serverId }
        if (wasActive) {
            // The tunnel is (or is still starting) on the server the user just deleted, so
            // tear it down — otherwise the session keeps running on a server that is gone.
            if (connectionState == ConnectionState.CONNECTED ||
                connectionState == ConnectionState.CONNECTING
            ) {
                debugLog("remove: active server deleted, disconnecting")
                disconnectVpnInternal()
            }
            selectedServerId = servers.firstOrNull()?.id ?: ""
            debugLog("remove: selectedServerId -> '${selectedServerId}'")
            scope.launch { settingsStore?.setSelectedServerId(selectedServerId) }
        }
    }

    /**
     * Records a server in the deleted set so the cache loader and the subscription importer
     * skip it from now on. The server list is rebuilt from those two sources on every launch
     * and refresh, so a plain removal from memory would only last until the next resync.
     */
    private fun hideServer(s: Server) {
        val keys = hideKeysFor(s)
        val next = hiddenServers + keys
        debugLog("hide name='${s.name}' keys=$keys hiddenCount=${next.size}")
        hiddenServers = next
        scope.launch { settingsStore?.setHiddenServers(next) }
    }

    /**
     * Brings back every server the user deleted. The hidden set is cleared and persisted
     * first, then the list is rebuilt: from the cache (covers locally imported servers) and,
     * when a subscription link is set, by re-fetching it — a subscription refresh drops
     * hidden rows from the cache, so those only come back over the network.
     */
    fun restoreHiddenServers() {
        if (hiddenServers.isEmpty()) return
        debugLog("restore hidden=${hiddenServers.size} keys=$hiddenServers")
        hiddenServers = emptySet()
        scope.launch {
            settingsStore?.setHiddenServers(emptySet())
            loadCachedServers()
            val url = subscriptionUrl
            if (url.startsWith("http")) importSubscription(url)
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

    /** Selects a server and persists the choice so the Home card survives restarts. */
    fun selectServer(serverId: String) {
        debugLog("select id=$serverId state=$connectionState")
        selectedServerId = serverId
        scope.launch { settingsStore?.setSelectedServerId(serverId) }
    }

    fun toggleVpn() {
        debugLog("toggle state=$connectionState")
        val ctx = appContext
        if (ctx == null) {
            debugLog("toggle aborted: appContext is null")
            return
        }
        when (connectionState) {
            ConnectionState.DISCONNECTED -> startVpnInternal()
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> disconnectVpnInternal()
        }
    }

    /**
     * Switches the selected server (or retries the current one) from any state.
     *
     * A plain [toggleVpn] cannot express this: while CONNECTING it would just disconnect,
     * and while CONNECTED it would do nothing at all. Tapping a server row has to mean
     * "give me a tunnel on this server", so the old attempt is torn down first and the new
     * one starts only after that teardown finished — otherwise the service still holds the
     * olcRTC room / SOCKS port and the fresh attempt fails on top of the dying one.
     */
    fun restartConnection() {
        debugLog("restart state=$connectionState server='$selectedServerId'")
        when (connectionState) {
            ConnectionState.DISCONNECTED -> startVpnInternal()
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                isSwitchingServer = true
                val token = ++switchToken
                debugLog("restart: stop then start, token=$token")
                stopVpnInternal(restartAfterStop = true, restartToken = token)
            }
        }
    }

    /** Tears the current session down (generation bump + native stop off the UI thread). */
    private fun disconnectVpnInternal() {
        debugLog("disconnect state=$connectionState")
        isSwitchingServer = false
        switchToken++
        stopVpnInternal(restartAfterStop = false, restartToken = 0L)
    }

    private fun stopVpnInternal(restartAfterStop: Boolean, restartToken: Long) {
        debugLog("stop gen=${vpnGeneration + 1} restart=$restartAfterStop token=$restartToken")
        // Bump generation first: any in-flight connect coroutine sees the
        // new value at its next suspension point and aborts, so native
        // start()/stop() can never run concurrently.
        vpnGeneration++
        connectJob?.cancel()
        connectJob = null
        connectionState = ConnectionState.DISCONNECTED
        lastConfig = ""
        val ctx = appContext
        if (ctx != null) {
            val intent = Intent(ctx, RedShiftVpnService::class.java).apply {
                action = RedShiftVpnService.ACTION_DISCONNECT
            }
            try {
                ctx.startService(intent)
            } catch (e: Exception) {
                debugLog("disconnect service start failed: ${e.message}")
            }
        }
        // stop() joins the native run loop and can block for a while on some devices;
        // never run it on the main thread (would ANR the UI). The previous teardown is
        // cancelled first so two native stop() calls can never overlap.
        teardownJob?.cancel()
        teardownJob = scope.launch(Dispatchers.IO) {
            singBoxManager?.stop()
            if (restartAfterStop) {
                // The service handles ACTION_DISCONNECT on its own thread; a short grace
                // period lets it release the olcRTC runtime (and 127.0.0.1:8788) before
                // we ask for a brand new tunnel.
                delay(600)
                if (isSwitchingServer && switchToken == restartToken &&
                    connectionState == ConnectionState.DISCONNECTED
                ) {
                    debugLog("restart: grace over, starting (token=$restartToken)")
                    isSwitchingServer = false
                    startVpnInternal()
                } else {
                    debugLog("restart cancelled: switching=$isSwitchingServer token=$switchToken/$restartToken state=$connectionState")
                }
            }
        }
        RedShiftVpnService.resetTunState()
        stopTelemetrySimulation()
    }

    /**
     * Starts a tunnel on the currently selected server. VpnService consent is requested
     * first (without VpnService.prepare() Builder.establish() returns null on most OEMs),
     * then the service brings the TUN up and sing-box is attached to its fd.
     */
    private fun startVpnInternal() {
        val ctx = appContext
        if (ctx == null) {
            Log.e("RedShiftVPN", "appContext is null")
            return
        }
            // First time the user triggers a connection the system must be asked
            // to consent to this app using VpnService; without VpnService.prepare()
            // Builder.establish() instantly returns null on most OEM builds.
            val consent = VpnService.prepare(ctx)
            if (consent != null) {
                try {
                    val act = currentActivity
                    if (act != null) {
                        act.startActivityForResult(consent, VPN_CONSENT_REQUEST)
                    } else {
                        consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(consent)
                    }
                    Log.e("RedShiftVPN", "VPN consent requested")
                } catch (e: Exception) {
                    Log.e("RedShiftVPN", "VPN consent activity failed: ${e.message}")
                }
                return
            }
            connectionState = ConnectionState.CONNECTING
            Log.e("RedShiftVPN", "Starting VPN connect sequence")

            // Mark this connect attempt with the current generation. Any later
            // toggle (disconnect) bumps vpnGeneration, and every suspension
            // point below re-checks it so a stale connect coroutine bails out
            // instead of racing the native stop().
            val gen = vpnGeneration
            connectJob?.cancel()
            connectJob = scope.launch {
                try {
                    debugLog("coroutine started, state=$connectionState")
                    if (gen != vpnGeneration) return@launch
                    val first = getSelectedServer()
                    debugLog("server=${first?.name}, proto=${first?.protocol}")

                    if (first == null) {
                        debugLog("server is null, abort")
                        connectionState = ConnectionState.DISCONNECTED
                        return@launch
                    }

                    val ok = singBoxManager?.ensureBinary() == true
                    debugLog("binary ok=$ok")
                    if (!ok) {
                        connectionState = ConnectionState.DISCONNECTED
                        return@launch
                    }

                    val candidates = olcrtcFailoverCandidates(first)
                    for ((index, candidate) in candidates.withIndex()) {
                        if (gen != vpnGeneration) return@launch
                        if (index > 0) {
                            debugLog(
                                "olcrtc failover ${index}/${candidates.size - 1}: " +
                                    "${candidate.name} after ${candidates[index - 1].name}"
                            )
                            // The previous attempt's olcRTC runtime must release
                            // 127.0.0.1:8788 before the next one can bind it.
                            delay(800)
                            if (gen != vpnGeneration) return@launch
                        }
                        markServerUsed(candidate.id)
                        settingsStore?.setSelectedServerId(candidate.id)
                        val connected = connectAttempt(candidate, gen) { gen != vpnGeneration || !isActive }
                        if (connected) {
                            debugLog("connected via ${candidate.name} (attempt ${index + 1}/${candidates.size})")
                            return@launch
                        }
                        debugLog("attempt failed: ${candidate.name}")
                    }
                    debugLog("all ${candidates.size} attempt(s) failed")
                    connectionState = ConnectionState.DISCONNECTED
                } catch (e: Exception) {
                    debugLog("EXCEPTION: ${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}")
                    connectionState = ConnectionState.DISCONNECTED
                }
            }
    }

    /**
     * One full connect attempt on [server]: generate the config, hand the TUN fd to the
     * VPN service, attach sing-box. Returns true once CONNECTED.
     *
     * A failed attempt tears everything down but deliberately leaves connectionState
     * untouched, so the olcRTC failover loop can try the next room before giving up.
     */
    private suspend fun connectAttempt(
        server: Server,
        gen: Long,
        isCancelled: () -> Boolean,
    ): Boolean {
        val sub = server.toSubServer()
        val ctx = appContext
        val filesDir = ctx?.filesDir?.absolutePath ?: ""
        try {
            if (ctx != null && filesDir.isNotEmpty()) {
                for (assetName in arrayOf("geoip-ru.srs", "geoip-cn.srs")) {
                    val srsFile = java.io.File(filesDir, assetName)
                    if (!srsFile.exists()) {
                        ctx.assets.open(assetName).use { input ->
                            srsFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        debugLog("generating config for ${server.name}...")
        // Domain split only makes sense when the user selected the
        // "Domains" mode and the toggle is on. In "Apps" mode the TUN
        // allow/deny (EXTRA_SPLIT_APPS) handles it, and a stale domain
        // list from a previous session must not leak into routing.
        val domainSplitEnabled = splitTunnelEnabled && !splitTunnelByApps
        val config = withContext(Dispatchers.IO) {
            SingBoxConfigGenerator().generateConfig(
                sub,
                SingBoxManager.SOCKS_PORT,
                filesDir,
                if (domainSplitEnabled) splitDomains else emptyList(),
                domainSplitEnabled && splitTunnelOnly,
                routingMode.name.lowercase(),
                // RU-direct is an always-on behaviour (original app), CN is off.
                bypassRussia = true,
                bypassChina = false,
                blockAds
            )
        }
        debugLog("config length=${config.length}")
        lastConfig = config

        if (isCancelled()) {
            debugLog("connect aborted after config gen (generation changed)")
            return false
        }

        debugLog("TUN fd passing for all protocols")
        val intent1 = Intent(appContext, RedShiftVpnService::class.java).apply {
            action = RedShiftVpnService.ACTION_CONNECT_AWG
            // App-based split only when the user actually picked the
            // "Apps" mode: otherwise a stale app list from a previous
            // session would filter the TUN behind the user's back.
            if (splitTunnelEnabled && splitTunnelByApps && splitApps.isNotEmpty()) {
                putExtra(RedShiftVpnService.EXTRA_SPLIT_APPS, splitApps.toTypedArray())
                putExtra(RedShiftVpnService.EXTRA_SPLIT_APPS_ONLY, splitTunnelOnly)
            }
            if (isOlcrtcServer(server)) {
                putExtra(RedShiftVpnService.EXTRA_OLCRTC_URI, server.subExtra.ifBlank { server.address })
            }
            val srvName = server.name
            if (srvName.isNotBlank()) {
                putExtra(RedShiftVpnService.EXTRA_SERVER_LABEL, srvName)
            }
            putExtra(RedShiftVpnService.EXTRA_NOTIFICATIONS, vpnNotification)
        }
        appContext?.startService(intent1)
        // Race fix: on some OEMs (vivo) VpnService.Builder.establish() takes
        // ~10s, so poll up to 40s (was 10s) before giving up on the fd.
        // Must run off the main thread — Thread.sleep here would ANR the UI.
        val tunFd = withContext(Dispatchers.IO) {
            var fd = -1
            for (i in 1..160) {
                if (isCancelled() || gen != vpnGeneration) break
                Thread.sleep(250)
                fd = RedShiftVpnService.getLastTunFdRaw()
                if (fd > 0) break
            }
            fd
        }
        debugLog("TUN fd=$tunFd after wait")
        if (isCancelled() || gen != vpnGeneration) {
            debugLog("connect aborted after TUN wait (generation changed)")
            return false
        }
        if (tunFd <= 0) {
            // TUN fd never arrived — fail-close instead of leaking via mixed SOCKS.
            debugLog("TUN fd not available, fail-close (no mixed fallback)")
            teardownFailedAttempt()
            return false
        }
        val started = withContext(Dispatchers.IO) { singBoxManager?.startWithTunFd(config, tunFd) == true }
        debugLog("sing-box started with tun fd=$started")
        if (!started) {
            // Fail-close: never fall back to mixed SOCKS (would leak UDP/DNS).
            debugLog("sing-box TUN failed, fail-close (no mixed fallback)")
            teardownFailedAttempt()
            return false
        }

        connectionState = ConnectionState.CONNECTED
        debugLog("setting CONNECTED")
        totalDataUsedMb = 0.0
        startSessionTimer()
        return true
    }

    private fun teardownFailedAttempt() {
        singBoxManager?.stop()
        RedShiftVpnService.resetTunState()
        appContext?.startService(Intent(appContext, RedShiftVpnService::class.java).apply {
            action = RedShiftVpnService.ACTION_DISCONNECT
        })
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
                val isSingleCustomLink = url.startsWith("vpn://") || url.startsWith("tt://") || OlcrtcUri.isOlcrtcUri(url)
                val dbgFirst = result.servers.first()
                debugLog("import OK: n=${result.servers.size} first=${dbgFirst.protocol} ${dbgFirst.address}:${dbgFirst.port} psk=${dbgFirst.presharedKey.isNotEmpty()} priv=${dbgFirst.privateKey.isNotEmpty()} srvPub=${dbgFirst.serverPublicKey.isNotEmpty()} awgParams=${if (dbgFirst.awgParams.isBlank()) "-" else "yes"}")

                // A single custom node (Amnezia vpn:// deeplink et al.) is NOT a
                // subscription: adding it must never touch the profile/user id or the
                // cached server list of an already imported subscription.
if (isSingleCustomLink) {
                addServersFromSubResult(result.servers, url)
                cacheServersToStore(result.servers, url)
                val first = result.servers.first()
                if (servers.any { it.id == first.id }) {
                    selectedServerId = first.id
                    settingsStore?.let { it.setSelectedServerId(first.id) }
                }
                markServerUsed(first.id)
                isImporting = false
                return@launch
            }

                subscriptions.removeAll { it.url == url }
                addServersFromSubResult(result.servers, url)

                val first = result.servers.first()
                if (servers.any { it.id == first.id }) {
                    selectedServerId = first.id
                    settingsStore?.let { store ->
                        store.setSelectedServerId(first.id)
                    }
                }

                cacheServersToStore(result.servers, url)

                val profile = result.profileInfo
                if (profile != null) {
                    if (profile.expiry > 0) {
                        val locale = if (LocalizationState.currentLanguage == AppLanguage.RU) java.util.Locale.forLanguageTag("ru-RU") else java.util.Locale.US
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", locale)
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
                    if (subscriptionPlan.isBlank() && profile.plan.isNotBlank()) {
                        subscriptionPlan = profile.plan
                    }
                    if ((subscriptionExpiry.isBlank() || subscriptionExpiry == "N/A") && profile.expiresAt.isNotBlank()) {
                        val exp = profile.expiresAt.take(10)
                        if (exp != "N/A") subscriptionExpiry = exp
                    }
                }

                val userIdFromUrl = url.substringAfterLast("/").substringBefore("?").trim()
                val numericUserId = userIdFromUrl.toIntOrNull()
                val usernameId = profile?.username?.trim()?.toIntOrNull()
                if (profile == null || subscriptionPlan.isBlank() || subscriptionExpiry.isBlank() || numericUserId == null) {
                    val resolvedUserId = when {
                        numericUserId != null -> numericUserId
                        usernameId != null -> usernameId
                        else -> telegramToken.toIntOrNull()
                    }
                    if (resolvedUserId != null) {
                        val user = apiClient.getUser(resolvedUserId)
                        if (user != null) {
                            userInfo = user
                            telegramToken = resolvedUserId.toString()
                            isLoggedIn = true
                            user.subscription?.tariff?.let { subscriptionPlan = it }
                            user.subscription?.expiresAt?.let { subscriptionExpiry = it }
                        }
                        val sub2 = apiClient.getSubscription(resolvedUserId)
                        if (sub2 != null) {
                            if (subscriptionPlan.isBlank()) subscriptionPlan = sub2.tariff
                            if (subscriptionExpiry.isBlank()) {
                                val exp = sub2.expiresAt.take(10)
                                if (exp.isNotBlank() && exp != "N/A") subscriptionExpiry = exp
                            }
                        }
                    }
                    settingsStore?.setUserId(if (resolvedUserId != null) resolvedUserId.toString() else userIdFromUrl)
                } else {
                    // Profile info came back with the subscription: the user is authed.
                    val resolvedUserId = numericUserId ?: usernameId ?: telegramToken.toIntOrNull()
                    if (resolvedUserId != null) {
                        telegramToken = resolvedUserId.toString()
                        isLoggedIn = true
                    }
                    settingsStore?.setUserId(if (resolvedUserId != null) resolvedUserId.toString() else userIdFromUrl)
                }
                settingsStore?.setTariffName(subscriptionPlan)
                settingsStore?.setSubscriptionExpiry(subscriptionExpiry)
                val finalTs = profile?.expiry?.takeIf { it > 0 }?.times(1000L)
                    ?: expiresAtToMillis(subscriptionExpiry)
                if (finalTs > 0) {
                    settingsStore?.setSubscriptionExpiryTs(finalTs)
                    com.example.service.ExpiryReminderScheduler.schedule(appContext!!)
                }

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

    private suspend fun cacheServersToStore(serverList: List<SubServer>, subUrl: String) {
        val store = settingsStore ?: return
        // Merge: keep servers of other subscriptions, replace only this one's.
        // Dedupe across ALL urls by endpoint and logical name — same node in two
        // slices must not double, stale url-less rows must not shadow current ones.
        val serversJson = org.json.JSONArray()
        val seen = HashSet<String>()
        val hidden = store.getBlockingHiddenServers()
        try {
            val existing = org.json.JSONArray(store.getBlockingCachedServersJson())
            for (i in 0 until existing.length()) {
                val obj = existing.getJSONObject(i)
                if (obj.optString("url") == subUrl) continue
                if (hideKeysForParts(
                        obj.optString("id", ""), obj.optString("name", ""), obj.optString("protocol", ""),
                        obj.optString("address", ""), obj.optInt("port", 0)
                    ).any { it in hidden }
                ) continue
                val key = endpointKey(
                    obj.optString("protocol", ""),
                    obj.optString("address", ""),
                    obj.optInt("port", 0)
                )
                if (key != null && !seen.add(key)) continue
                serversJson.put(obj)
            }
        } catch (_: Exception) {}
        for (s in serverList) {
            if (hideKeysForParts(s.id, s.name, s.protocol, s.address, s.port).any { it in hidden }) continue
            val key = endpointKey(s.protocol, s.address, s.port)
            if (key != null && !seen.add(key)) continue
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
                put("path", s.path)
                put("host", s.host)
                put("extra", s.extra)
                put("alpn", s.alpn)
                put("url", subUrl)
            })
        }
        val finalJson = dedupeServerArray(serversJson)
        store.setCachedServersJson(finalJson.toString())
        store.setCachedServersCount(finalJson.length())
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
                val ts = expiresAtToMillis(user.subscription?.expiresAt)
                if (ts > 0) {
                    settingsStore?.setSubscriptionExpiryTs(ts)
                    com.example.service.ExpiryReminderScheduler.schedule(appContext!!)
                }
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
                val ts = expiresAtToMillis(user.subscription?.expiresAt)
                if (ts > 0) {
                    settingsStore?.setSubscriptionExpiryTs(ts)
                    com.example.service.ExpiryReminderScheduler.schedule(appContext!!)
                }
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
