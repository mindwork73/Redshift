package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class RedShiftVpnService : VpnService() {

    private var tunFd: ParcelFileDescriptor? = null
    private var tunOut: ParcelFileDescriptor.AutoCloseOutputStream? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vpnJob: Job? = null
    @Volatile
    private var vpnRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeTunnels = ConcurrentHashMap<ConnectionKey, TcpTunnel>()
    private var remoteHost = "37.220.84.106"
    private var remotePort = 995
    private var socksLogin = ""
    private var socksPassword = ""
    private var euProxyHost = "217.156.64.40"
    private var euProxyPort = 10810

    private var splitApps: List<String> = emptyList()
    private var splitAppsOnly = false

    private var olcrtcRuntime: mobile.Runtime? = null

    // gomobile requires Go calls to run off the Android main thread (a locked Go
    // OS thread + a main-thread call into the runtime deadlocks with
    // "startlockedm: m has p"). A dedicated executor keeps every bind call on
    // the same worker thread.
    private var olcrtcExecutor: java.util.concurrent.ExecutorService? = null

    // Every connect attempt (and every disconnect) bumps connectGen. The olcRTC
    // start runs for seconds, so it re-checks this before touching the TUN and
    // before publishing the runtime — otherwise a disconnect that lands mid-start
    // is silently undone and the next connect finds port 8788 still held.
    private val connectGen = java.util.concurrent.atomic.AtomicLong(0)

    // Same idea for the Go runtime itself: bumped on every start and every stop so
    // a late-finishing start cannot resurrect a runtime the user already switched away from.
    private val olcrtcGen = java.util.concurrent.atomic.AtomicLong(0)

    // Every runtime we ever created, including the ones a superseded start never
    // published. A runtime holds 127.0.0.1:8788 from the moment the room handshake
    // finishes, so anything that is not in here can leak the SOCKS port and make the
    // *next* connect look like it hangs.
    private val olcrtcRuntimes = java.util.concurrent.ConcurrentHashMap<mobile.Runtime, Boolean>()

    @Volatile
    private var lastOlcrtcError: String? = null

    private fun olcrtcExec(): java.util.concurrent.ExecutorService =
        olcrtcExecutor ?: java.util.concurrent.Executors.newSingleThreadExecutor().also {
            olcrtcExecutor = it
        }

    private var nextConnectionId = 0

    companion object {
        const val ACTION_CONNECT = "com.example.action.CONNECT"
        const val ACTION_CONNECT_AWG = "com.example.action.CONNECT_AWG"
        const val ACTION_DISCONNECT = "com.example.action.DISCONNECT"
        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_USE_LOCAL_PROXY = "extra_use_local_proxy"
        const val EXTRA_SOCKS_LOGIN = "extra_socks_login"
        const val EXTRA_SOCKS_PASSWORD = "extra_socks_password"
        const val EXTRA_SPLIT_APPS = "extra_split_apps"
        const val EXTRA_SPLIT_APPS_ONLY = "extra_split_apps_only"
        const val EXTRA_SERVER_LABEL = "extra_server_label"
        const val EXTRA_NOTIFICATIONS = "extra_notifications"
        const val EXTRA_OLCRTC_URI = "extra_olcrtc_uri"

        /** Local SOCKS5 port the olcRTC runtime binds; sing-box routes through it. */
        const val OLCRTC_SOCKS_PORT = 8788

        /**
         * How long we give the WebRTC room to come up.
         *
         * The runtime reports "ready" only after the whole chain completed: the media
         * link connected, the control stream handshake finished and a peer answered.
         * The library itself allows 60 s for that and on mobile networks the room join
         * alone regularly takes 10-20 s (the app is the WebRTC *client* here — the
         * server also has to raise its peer session). A 9 s cap made a healthy join look
         * like a dead room: the app tore the runtime down mid-handshake, which made the
         * server side see a peer that keeps vanishing, and every retry started from a
         * worse state.
         */
        private const val OLCRTC_WAIT_MS = 25000L
        private const val OLCRTC_STOP_MS = 5000L

        /**
         * Hard ceiling for one whole olcRTC start (hang guard): slightly above the
         * readiness window so a wedged Go call can never block the worker thread forever.
         */
        private const val OLCRTC_START_HARD_MS = OLCRTC_WAIT_MS + 5000L

        /**
         * Set when a connect attempt failed inside the service (e.g. the olcRTC room
         * never became ready) so the UI can fail fast instead of polling for a TUN fd
         * that will never arrive. Cleared at the start of every connect.
         */
        @Volatile
        var lastStartError: String? = null
            private set

        const val ROUTE_NL = 0
        const val ROUTE_DIRECT = 1
        const val ROUTE_EU = 2

        private const val VPN_MTU = 1280

        @Volatile
        var tunFdRaw: Int = -1
            private set

        @Volatile
        var tunReady: Boolean = false
            private set

        fun resetTunState() {
            tunFdRaw = -1
            tunReady = false
        }

        fun getLastTunFdRaw(): Int = tunFdRaw
    }

    private fun debugLogVPN(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val line = "$ts [VPN] $msg\n"
        try {
            val f = java.io.File(filesDir, "redshift_debug.log")
            f.appendText(line)
        } catch (_: Exception) {}
        Log.e("RedShiftVPN", msg)
    }

    /**
     * Foreground notification shown while the VPN is active — small and concise.
     * When the user disabled notifications in settings we still must post one for
     * the foreground service to survive, so it just switches to a silent channel.
     */
    private fun buildVpnNotification(label: String, show: Boolean): Notification {
        val silent = !show
        val channelId = "redshift_vpn"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "RedShift VPN",
                if (silent) NotificationManager.IMPORTANCE_MIN else NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            channel.description = "Active VPN connection indicator"
            nm.createNotificationChannel(channel)
        }
        val text = if (label.isBlank()) "VPN active" else "VPN · $label"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("RedShift VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RuCidrs.init(this)

        Log.e("RedShiftVPN", "onStartCommand: action=${intent?.action}, useLocal=${intent?.getBooleanExtra(EXTRA_USE_LOCAL_PROXY, false)}")
        when (intent?.action) {
            ACTION_CONNECT -> {
                val channelId = "redshift_vpn"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "RedShift VPN", NotificationManager.IMPORTANCE_LOW)
                    channel.setShowBadge(false)
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
                }
                val notification = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(this, channelId)
                } else {
                    @Suppress("DEPRECATION")
                    Notification.Builder(this)
                })
                    .setContentTitle("RedShift VPN")
                    .setContentText("VPN active")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(true)
                    .build()
                startForeground(1, notification)
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RedShift:VPN").apply {
                    acquire(4 * 60 * 60 * 1000L)
                }
                startWakeLockRenewal()
                registerNetworkCallback()
                val useLocal = intent.getBooleanExtra(EXTRA_USE_LOCAL_PROXY, false)
                if (useLocal) {
                    remoteHost = "127.0.0.1"
                    remotePort = com.example.service.SingBoxManager.SOCKS_PORT
                } else {
                    remoteHost = intent.getStringExtra(EXTRA_HOST) ?: remoteHost
                    remotePort = intent.getIntExtra(EXTRA_PORT, remotePort)
                    socksLogin = intent.getStringExtra(EXTRA_SOCKS_LOGIN) ?: ""
                    socksPassword = intent.getStringExtra(EXTRA_SOCKS_PASSWORD) ?: ""
                }
                Log.e("RedShiftVPN", "Connecting to $remoteHost:$remotePort socksLogin='$socksLogin'")
                connectVpn()
            }
            ACTION_CONNECT_AWG -> {
                val label = intent.getStringExtra(EXTRA_SERVER_LABEL) ?: ""
                val showNotifications = intent.getBooleanExtra(EXTRA_NOTIFICATIONS, true)
                val pendingSplitApps = intent.getStringArrayExtra(EXTRA_SPLIT_APPS)?.toList() ?: emptyList()
                val pendingSplitAppsOnly = intent.getBooleanExtra(EXTRA_SPLIT_APPS_ONLY, false)
                val olcrtcUri = intent.getStringExtra(EXTRA_OLCRTC_URI)
                    ?.takeIf { OlcrtcUri.isOlcrtcUri(it) }

                lastStartError = null
                lastOlcrtcError = null

                // Post the notification first and return from onStartCommand immediately.
                // Everything below (WebRTC room join + Builder.establish(), which alone can
                // take ~10s on some OEM builds) runs off the main thread — blocking here
                // froze the whole UI and got the process ANR-killed on protocol switches.
                startForeground(1, buildVpnNotification(label, showNotifications))
                registerNetworkCallback()

                val myGen = connectGen.incrementAndGet()
                scope.launch(Dispatchers.IO) {
                    try {
                        if (olcrtcUri != null) {
                            Log.e("RedShiftVPN", "olcRTC: starting runtime")
                            if (!startOlcrtcRuntime(olcrtcUri, myGen)) {
                                failConnect(myGen, lastOlcrtcError ?: "olcRTC не подключился")
                                return@launch
                            }
                            debugLogVPN("olcRTC runtime ready (SOCKS 127.0.0.1:$OLCRTC_SOCKS_PORT), state=${olcrtcState()}")
                        }
                        if (connectGen.get() != myGen) {
                            debugLogVPN("connect superseded before TUN, aborting")
                            return@launch
                        }
                        splitApps = pendingSplitApps
                        splitAppsOnly = pendingSplitAppsOnly
                        connectTunOnly()
                    } catch (e: Exception) {
                        failConnect(myGen, "${e::class.simpleName}: ${e.message}")
                    }
                }
            }
            ACTION_DISCONNECT -> {
                connectGen.incrementAndGet()
                stopOlcrtcRuntime()
                disconnectVpn()
                unregisterNetworkCallback()
                try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
                wakeLock = null
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private var wakeLockRenewalJob: Job? = null

    private fun startWakeLockRenewal() {
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = scope.launch {
            while (isActive) {
                delay(3 * 60 * 60 * 1000L)
                if (!vpnRunning && tunFdRaw < 0) break
                try {
                    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RedShift:VPN").apply {
                        acquire(4 * 60 * 60 * 1000L)
                    }
                    debugLogVPN("wake lock renewed")
                } catch (_: Exception) {}
            }
        }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    debugLogVPN("network available: $network — resetting stale tunnels")
                    resetAllTunnels()
                }
            }
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
            debugLogVPN("registerNetworkCallback failed: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                cm.unregisterNetworkCallback(it)
            }
        } catch (_: Exception) {}
        networkCallback = null
    }

    private fun resetAllTunnels() {
        val tunnels = activeTunnels.values.toList()
        activeTunnels.clear()
        for (t in tunnels) {
            try { t.rstAndClose() } catch (_: Exception) {}
        }
        val sockets = udpSockets.values.toList()
        udpSockets.clear()
        for (s in sockets) {
            try { s.close() } catch (_: Exception) {}
        }
        dnsCache.clear()
    }

    override fun onRevoke() {
        connectGen.incrementAndGet()
        stopOlcrtcRuntime()
        disconnectVpn()
        shutdownOlcrtcExec()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        wakeLockRenewalJob?.cancel()
        connectGen.incrementAndGet()
        unregisterNetworkCallback()
        stopOlcrtcRuntime()
        disconnectVpn()
        // shutdown() (not shutdownNow) so the queued stop above still reaches Go and
        // actually frees 127.0.0.1:8788 for the next session.
        shutdownOlcrtcExec()
        stopSelf()
        super.onDestroy()
    }

    private fun shutdownOlcrtcExec() {
        try {
            olcrtcExecutor?.shutdown()
        } catch (_: Exception) {}
        olcrtcExecutor = null
    }

    /**
     * Spins up the gomobile olcRTC (WebRTC-over-Telemost) client runtime and waits
     * until it is ready. Its local SOCKS5 proxy on 127.0.0.1:8788 is what the
     * sing-box config routes through via an outbound of type "socks".
     *
     * Blocking by design, so it MUST be called off the main thread. Every call into
     * Go goes through the single-thread executor, so start/stop are strictly ordered:
     * a stop submitted while a start is still waiting for the room runs right after it
     * and therefore always finds the port free again.
     *
     * @param gen the connect generation this start belongs to; if a disconnect or a new
     *            connect bumped it meanwhile, the fresh runtime is torn down instead of
     *            being published (that resurrection used to leak a runtime holding 8788).
     */
    private fun startOlcrtcRuntime(uri: String, gen: Long): Boolean {
        val spec = OlcrtcUri.parse(uri)
        if (spec == null) {
            lastOlcrtcError = "не удалось разобрать olcrtc:// ссылку"
            return false
        }
        val myOlcGen = olcrtcGen.incrementAndGet()
        var startedRt: mobile.Runtime? = null
        var published = false
        try {
            val ready = olcrtcExec().submit(java.util.concurrent.Callable<Boolean> {
                // gomobile bind requires every call into Go from the SAME single
                // thread (including object construction) — mixing main-thread
                // new_() with worker threads crashes with SIGSEGV.
                val rt = mobile.Mobile.new_()
                startedRt = rt
                olcrtcRuntimes[rt] = true
                rt.setDebug(BuildConfig.DEBUG)
                // Stable identity per install: the server keys its peer sessions on the
                // device id, so a fresh UUID per attempt reads as a brand-new device on
                // every retry (and the server then reaps the previous peer as liveness).
                rt.setDeviceIDPath(java.io.File(filesDir, "olcrtc_device_id").absolutePath)
                rt.setProvider(spec.provider)
                rt.setTransport(spec.transport)
                rt.setRoom(spec.room)
                rt.setKey(spec.key)
                rt.setSocksPort(OLCRTC_SOCKS_PORT.toLong())
                if (spec.transport == "vp8channel") {
                    rt.setVP8Options(spec.vp8Fps.toLong(), spec.vp8Batch.toLong())
                }
                rt.setProtector(object : mobile.SocketProtector {
                    override fun protect(fd: Long): Boolean =
                        // Our own app is already excluded from the TUN (addDisallowedApplication),
                        // so olcrtc sockets always use the real network directly.
                        true
                })
                rt.start()
                // Throws (ErrReadyTimeout) when the room never comes up.
                rt.waitReady(OLCRTC_WAIT_MS)
                if (olcrtcGen.get() != myOlcGen || connectGen.get() != gen) {
                    // Switched away while we were joining the room: never publish, the
                    // finally below tears this runtime down.
                    debugLogVPN("olcRTC start superseded, runtime discarded")
                    false
                } else {
                    olcrtcRuntime = rt
                    published = true
                    debugLogVPN("olcRTC runtime ready (${spec.provider}/${spec.transport}, room=${spec.room})")
                    true
                }
            }).get(OLCRTC_START_HARD_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            return ready
        } catch (e: Exception) {
            val cause = (e as? java.util.concurrent.ExecutionException)?.cause ?: e
            lastOlcrtcError = when (cause) {
                is java.util.concurrent.TimeoutException ->
                    "комната не ответила за ${OLCRTC_WAIT_MS / 1000} с (сеть)"
                is OutOfMemoryError -> "не хватило памяти"
                else -> cause.message ?: cause::class.java.simpleName
            }
            debugLogVPN("olcRTC runtime error: $lastOlcrtcError")
            return false
        } finally {
            // Every path that does not keep the runtime must tear it down *here*: a
            // runtime that finished the room handshake already holds 127.0.0.1:8788,
            // and a leaked one makes the next connect block behind the single-thread
            // executor until its own start times out (that was the "won't connect
            // again" symptom). This also covers the hang-guard timeout above: the Go
            // task stays queued and may publish its runtime afterwards, so re-check
            // the generation instead of trusting the boolean.
            val rt = startedRt
            if (rt != null) {
                val stillCurrent = published && olcrtcGen.get() == myOlcGen
                if (!stillCurrent) {
                    debugLogVPN("olcRTC abandoning unpublished runtime")
                    olcrtcRuntimes.remove(rt)
                    if (olcrtcRuntime === rt) olcrtcRuntime = null
                    if (published) {
                        // It managed to publish itself behind the timeout: stop it for real.
                        stopOlcrtcRuntimeObject(rt)
                    } else {
                        // May run after the timeout fired — queue the stop so it lands
                        // as soon as the still-running start returns.
                        queueOlcrtcStop(rt)
                    }
                }
            }
        }
    }

    private fun olcrtcState(): String? =
        try { olcrtcExec().submit(java.util.concurrent.Callable<String?> { olcrtcRuntime?.state() }).get(2, java.util.concurrent.TimeUnit.SECONDS) }
        catch (_: Exception) { null }

    /**
     * Aborts a connect attempt that failed inside the service. Publishes the reason so
     * the UI can stop waiting for a TUN fd, and only tears the service down if this
     * attempt is still the current one.
     */
    private fun failConnect(gen: Long, reason: String) {
        // Even when a newer attempt already superseded us, our own runtime must go:
        // otherwise it keeps the room and the SOCKS port while the new attempt starts.
        stopOlcrtcRuntime()
        if (connectGen.get() != gen) return
        lastStartError = reason
        debugLogVPN("connect failed: $reason")
        disconnectVpn()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try { stopForeground(true) } catch (_: Exception) {}
            stopSelf()
        }
    }

    /** Queues a stop for one specific runtime and forgets it. */
    private fun stopOlcrtcRuntimeObject(rt: mobile.Runtime) {
        olcrtcRuntimes.remove(rt)
        queueOlcrtcStop(rt)
    }

    /** Queues a stop on the Go worker thread; used even for not-yet-published runtimes. */
    private fun queueOlcrtcStop(rt: mobile.Runtime) {
        val exec = olcrtcExec()
        try {
            exec.submit(Runnable {
                try {
                    if (rt.isRunning()) rt.stop(OLCRTC_STOP_MS)
                    debugLogVPN("olcRTC runtime stopped")
                } catch (e: Exception) {
                    debugLogVPN("olcRTC runtime stop error: ${e.message}")
                }
            })
        } catch (e: Exception) {
            // The executor was already shut down (service destroyed mid-join). Dropping
            // the stop here leaks the SOCKS port and the room for the whole process
            // lifetime — the next connect then fails on "failed to listen on
            // 127.0.0.1:8788". A one-off worker thread is the lesser evil and this path
            // only runs during shutdown.
            debugLogVPN("olcRTC executor refused stop (${e.message}), stopping on a fresh thread")
            try {
                Thread({
                    try { if (rt.isRunning()) rt.stop(OLCRTC_STOP_MS) } catch (_: Exception) {}
                }, "olcrtc-stop").also { it.isDaemon = true }.start()
            } catch (_: Exception) {}
        }
    }

    private fun stopOlcrtcRuntime() {
        // Invalidate any start still in flight *before* looking at the field: a start
        // that is waiting for the room must not publish its runtime after we tore down.
        olcrtcGen.incrementAndGet()
        // A plain olcrtcRuntime is normally also in the registry, but keep both in sync
        // so a runtime published by an older code path is still stopped.
        olcrtcRuntime?.let { rt ->
            olcrtcRuntime = null
            if (!olcrtcRuntimes.containsKey(rt)) stopOlcrtcRuntimeObject(rt)
        }
        for (rt in olcrtcRuntimes.keys.toList()) {
            stopOlcrtcRuntimeObject(rt)
        }
        olcrtcRuntime = null
    }

    private fun connectVpn() {
        disconnectVpn()

        val builder = Builder()
        builder.setSession("RedShift VPN")
        builder.setMtu(VPN_MTU)

        builder.addAddress("10.8.0.2", 32)
        builder.addRoute("0.0.0.0", 0)

        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("1.1.1.1")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        try {
            builder.addDisallowedApplication(packageName)
            Log.e("RedShiftVPN", "Excluded self ($packageName) from VPN")
        } catch (e: Exception) {
            Log.e("RedShiftVPN", "addDisallowedApplication failed: ${e.message}")
        }

        applySplitTunnel(builder)

        Log.e("RedShiftVPN", "Calling builder.establish()...")
        tunFd = builder.establish()
        if (tunFd == null) {
            debugLogVPN("builder.establish() returned null")
            stopSelf()
            return
        }
        debugLogVPN("TUN established, remote=$remoteHost:$remotePort socksLogin='$socksLogin'")
        tunOut = ParcelFileDescriptor.AutoCloseOutputStream(tunFd)

        vpnRunning = true
        val fd = tunFd!!
        vpnJob = scope.launch {
            debugLogVPN("VPN loop starting, fd.valid=${fd.fileDescriptor.valid()}")
            runVpnLoop(fd)
        }
    }

    private fun applySplitTunnel(builder: Builder) {
        if (splitApps.isEmpty()) return
        try {
            if (splitAppsOnly) {
                // "Only these apps go through the VPN": the rest bypass the tunnel.
                splitApps.forEach { pkg ->
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.e("RedShiftVPN", "addAllowedApplication($pkg) failed: ${e.message}")
                    }
                }
                Log.e("RedShiftVPN", "Split tunnel (only): ${splitApps.size} apps allowed")
            } else {
                // "Everything except these apps": the rest bypass the tunnel.
                splitApps.forEach { pkg ->
                    try {
                        builder.addDisallowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.e("RedShiftVPN", "addDisallowedApplication($pkg) failed: ${e.message}")
                    }
                }
                Log.e("RedShiftVPN", "Split tunnel (exclude): ${splitApps.size} apps bypass VPN")
            }
        } catch (e: Exception) {
            Log.e("RedShiftVPN", "split tunnel failed: ${e.message}")
        }
        splitApps = emptyList()
    }

    private fun connectTunOnly() {
        disconnectVpn()

        val builder = Builder()
        builder.setSession("RedShift AWG")
        builder.setMtu(VPN_MTU)

        builder.addAddress("10.8.0.2", 32)
        builder.addRoute("0.0.0.0", 0)

        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("1.1.1.1")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        // "Only" mode uses addAllowedApplication — Android forbids mixing it with
        // addDisallowedApplication (establish() throws IllegalArgumentException).
        // When only a whitelist is set, our own package is naturally not on it and
        // its sockets stay on the real network, so no explicit exclusion is needed.
        if (!splitAppsOnly) {
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.e("RedShiftVPN", "addDisallowedApplication failed: ${e.message}")
            }
        }

        applySplitTunnel(builder)

        tunFd = builder.establish()
        if (tunFd == null) {
            debugLogVPN("TUN establish failed for AWG mode")
            stopSelf()
            return
        }

        // establish() returns an fd with FD_CLOEXEC, which would be closed when
        // the sing-box child process is exec'd from startWithTunFd(), leaving it
        // to read on a stale descriptor. dup() alone does NOT clear cloexec on
        // Android, so clear it via the native fcntl shim (not subject to the
        // hidden-API policy that hides fcntl from Java reflection).
        val inheritable = tunFd!!.dup()
        val rawFd = inheritable.detachFd()
        val cloexecCleared = CloexecNative.clearCloseOnExec(rawFd)
        tunFdRaw = rawFd
        tunReady = true
        debugLogVPN("AWG TUN established, raw fd=$rawFd (cloexec cleared=$cloexecCleared)")
    }

    private fun disconnectVpn() {
        vpnRunning = false
        vpnJob?.cancel()
        vpnJob = null

        activeTunnels.values.forEach { it.close() }
        activeTunnels.clear()

        tunOut?.close()
        tunOut = null
        tunFd?.close()
        tunFd = null

        if (tunFdRaw > 0) {
            try { android.os.ParcelFileDescriptor.adoptFd(tunFdRaw).close() } catch (_: Exception) {}
        }
        resetTunState()
    }

    private fun runVpnLoop(tunFdLocal: ParcelFileDescriptor) {
        val buffer = ByteArray(VPN_MTU)
        var lastLogTime = System.currentTimeMillis()

        while (vpnRunning) {
            try {
                val bytesRead = Os.read(tunFdLocal.fileDescriptor, buffer, 0, buffer.size)
                if (bytesRead <= 0) {
                    Thread.sleep(50)
                    continue
                }
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 30000) {
                    debugLogVPN("VPN loop alive")
                    lastLogTime = now
                }
                processPacket(buffer, bytesRead)
            } catch (e: ErrnoException) {
                if (e.errno != OsConstants.EAGAIN) {
                    debugLogVPN("VPN loop fatal errno: ${e.errno} ${e.message}")
                    break
                }
                Thread.sleep(50)
            } catch (e: Exception) {
                debugLogVPN("VPN loop exception: ${e::class.simpleName}: ${e.message}")
                break
            }
        }
        debugLogVPN("VPN loop exited, vpnRunning=$vpnRunning")
    }

    private fun processPacket(data: ByteArray, length: Int) {
        if (length < 20) return

        val version = data[0].toInt() shr 4 and 0x0F
        if (version != 4) {
            return
        }

        val ihl = (data[0].toInt() and 0x0F) * 4
        if (ihl < 20 || ihl > length) return

        val protocol = data[9].toInt() and 0xFF

        when (protocol) {
            6 -> handleTcpPacket(data, length, ihl)
            17 -> handleUdpPacket(data, length, ihl)
        }
    }

    private data class ConnectionKey(
        val srcIp: Int,
        val srcPort: Int,
        val dstIp: Int,
        val dstPort: Int
    )

    private fun handleTcpPacket(data: ByteArray, length: Int, ihl: Int) {
        if (length < ihl + 20) return

        val srcIp = ByteBuffer.wrap(data, 12, 4).int
        val dstIp = ByteBuffer.wrap(data, 16, 4).int
        val srcPort = (data[ihl].toInt() and 0xFF) shl 8 or (data[ihl + 1].toInt() and 0xFF)
        val dstPort = (data[ihl + 2].toInt() and 0xFF) shl 8 or (data[ihl + 3].toInt() and 0xFF)
        val seqNum = ByteBuffer.wrap(data, ihl + 4, 4).int
        val ackNum = ByteBuffer.wrap(data, ihl + 8, 4).int
        val flags = data[ihl + 13].toInt() and 0xFF
        val dataOffset = ((data[ihl + 12].toInt() and 0xF0) shr 4) * 4

        val payloadLen = length - ihl - dataOffset
        val payload = if (payloadLen > 0) data.copyOfRange(ihl + dataOffset, length) else ByteArray(0)

        val syn = flags and 0x02 != 0
        val fin = flags and 0x01 != 0
        val rst = flags and 0x04 != 0
        val ack = flags and 0x10 != 0

        val key = ConnectionKey(srcIp, srcPort, dstIp, dstPort)

        val isLocal = dstIp and 0xFF000000.toInt() == 0x0A000000.toInt()

        val proxyIpInt = ipStringToInt(remoteHost)
        val isProxyTraffic = dstIp == proxyIpInt && dstPort == remotePort

        val euProxyIpInt = ipStringToInt(euProxyHost)
        val isEuProxyTraffic = dstIp == euProxyIpInt && dstPort == euProxyPort

        if (syn && !ack && !isLocal && !isProxyTraffic && !isEuProxyTraffic) {
            val routeMode = getRouteMode(dstIp)
            val isDirect = routeMode == ROUTE_DIRECT
            val tunnel = TcpTunnel(
                connectionId = nextConnectionId++,
                srcIp = srcIp,
                srcPort = srcPort,
                dstIp = dstIp,
                dstPort = dstPort,
                seqNum = seqNum,
                ackNum = ackNum,
                tunOutput = tunOut!!,
                protectSocket = { socket ->
                    val ok = protect(socket)
                    if (!ok) {
                        try {
                            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                            val physicalNetwork = cm.activeNetwork?.let { active ->
                                val caps = cm.getNetworkCapabilities(active)
                                if (caps != null && !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) active
                                else null
                            } ?: cm.allNetworks.firstOrNull { net ->
                                val caps = cm.getNetworkCapabilities(net)
                                caps != null
                                    && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                    && !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
                            }
                            if (physicalNetwork != null) {
                                physicalNetwork.bindSocket(socket)
                            }
                        } catch (_: Exception) {}
                    }
                },
                onClose = { activeTunnels.remove(key) },
                socksLogin = socksLogin,
                socksPassword = socksPassword,
                routeMode = routeMode
            )

            scope.launch {
                tunnel.connectToRemoteProxy(remoteHost, remotePort, euProxyHost, euProxyPort) { writeTunPacket(it) }
            }

            activeTunnels[key] = tunnel
            return
        }

        if (isProxyTraffic || isEuProxyTraffic) {
            return
        }

        val tunnel = activeTunnels[key] ?: return

        if (rst || fin) {
            tunnel.handleFin(seqNum, ackNum)
            if (rst || (fin && payload.isEmpty())) {
                tunnel.close()
                activeTunnels.remove(key)
            }
            return
        }

        if (payload.isNotEmpty()) {
            tunnel.forwardToRemote(payload)
        }
    }

    private fun getRouteMode(ip: Int): Int {
        val a = ip shr 24 and 0xFF
        if (a == 10 || a == 172 || a == 192 || a == 127) return ROUTE_NL

        if (a in cnFirstOctets) return ROUTE_DIRECT

        if (RuCidrs.contains(ip)) return ROUTE_DIRECT

        return ROUTE_NL
    }

    private val cnFirstOctets = HashSet<Int>().apply {
        addAll(listOf(
            1, 14, 27, 36, 39, 42, 49,
            58, 59, 60, 61,
            101, 103, 106,
            110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126,
            171, 175, 180, 182, 183,
            202, 210, 211, 218, 219, 220, 221, 222, 223
        ))
    }

    private val udpSockets = java.util.concurrent.ConcurrentHashMap<String, DatagramSocket>()
    private val dnsCache = java.util.concurrent.ConcurrentHashMap<String, Pair<List<Int>, Long>>()

    private fun handleUdpPacket(data: ByteArray, length: Int, ihl: Int) {
        if (length < ihl + 8) return

        val srcIp = ByteBuffer.wrap(data, 12, 4).int
        val dstIp = ByteBuffer.wrap(data, 16, 4).int
        val srcPort = (data[ihl].toInt() and 0xFF) shl 8 or (data[ihl + 1].toInt() and 0xFF)
        val dstPort = (data[ihl + 2].toInt() and 0xFF) shl 8 or (data[ihl + 3].toInt() and 0xFF)
        val udpLen = (data[ihl + 4].toInt() and 0xFF) shl 8 or (data[ihl + 5].toInt() and 0xFF)
        val payloadOffset = ihl + 8
        val payloadLen = minOf(length - payloadOffset, udpLen - 8)

        if (payloadLen <= 0) return

        val payload = data.copyOfRange(payloadOffset, payloadOffset + payloadLen)

        val isLocal = dstIp and 0xFF000000.toInt() == 0x0A000000.toInt()
        if (isLocal) return

        if (dstPort == 53) {
            scope.launch(Dispatchers.IO) {
                val response = resolveDns(payload)
                if (response != null) {
                    val ipPacket = buildUdpResponse(
                        dstIp = srcIp, dstPort = srcPort,
                        srcIp = dstIp, srcPort = dstPort,
                        payload = response
                    )
                    writeTunPacket(ipPacket)
                }
            }
            return
        }

        if (dstPort == 443) {
            // QUIC/HTTP3: drop instantly so apps fall back to TCP instead of timing out
            return
        }

        scope.launch(Dispatchers.IO) {
            forwardUdp(srcIp, srcPort, dstIp, dstPort, payload)
        }
    }

    private fun resolveDns(query: ByteArray): ByteArray? {
        if (query.size < 17) return null

        val qdcount = ((query[4].toInt() and 0xFF) shl 8) or (query[5].toInt() and 0xFF)
        if (qdcount == 0) return null

        var pos = 12
        val labels = mutableListOf<String>()
        while (pos < query.size) {
            val len = query[pos].toInt() and 0xFF
            if (len == 0) { pos++; break }
            if (len > 63 || pos + len + 1 >= query.size) return null
            labels.add(String(query, pos + 1, len, Charsets.US_ASCII))
            pos += 1 + len
        }
        if (labels.isEmpty()) return null
        if (pos + 4 > query.size) return null
        val qtype = ((query[pos].toInt() and 0xFF) shl 8) or (query[pos + 1].toInt() and 0xFF)
        val questionEnd = pos + 4

        val host = labels.joinToString(".")
        val cacheKey = "$host:$qtype"
        val cached = dnsCache[cacheKey]
        val ips: List<Int>
        if (cached != null && System.currentTimeMillis() - cached.second < 60000) {
            ips = cached.first
        } else {
            ips = try {
                InetAddress.getAllByName(host).mapNotNull { addr ->
                    val a = addr.address
                    if (a.size == 4) {
                        ((a[0].toInt() and 0xFF) shl 24) or ((a[1].toInt() and 0xFF) shl 16) or
                                ((a[2].toInt() and 0xFF) shl 8) or (a[3].toInt() and 0xFF)
                    } else null
                }
            } catch (_: Exception) { emptyList() }
            dnsCache[cacheKey] = ips to System.currentTimeMillis()
            if (dnsCache.size > 512) {
                dnsCache.entries.removeIf { System.currentTimeMillis() - it.value.second > 120000 }
            }
        }

        val rcode = if (ips.isEmpty()) 3 else 0

        val header = ByteArray(12)
        query.copyInto(header, 0, 0, 12)
        header[2] = 0x81.toByte()
        header[3] = (0x80 or rcode).toByte()

        val out = java.io.ByteArrayOutputStream()
        out.write(header)
        out.write(query, 12, questionEnd - 12)

        var ancount = 0
        if (qtype == 1 && ips.isNotEmpty()) {
            for (ip in ips.take(4)) {
                val answer = ByteArray(16)
                answer[0] = 0xC0.toByte(); answer[1] = 0x0C.toByte()
                answer[2] = 0; answer[3] = 1
                answer[4] = 0; answer[5] = 1
                answer[6] = 0; answer[7] = 0; answer[8] = 0; answer[9] = 60
                answer[10] = 0; answer[11] = 4
                answer[12] = (ip shr 24).toByte(); answer[13] = (ip shr 16).toByte()
                answer[14] = (ip shr 8).toByte(); answer[15] = ip.toByte()
                out.write(answer)
                ancount++
            }
        }

        val bytes = out.toByteArray()
        bytes[6] = ((ancount shr 8) and 0xFF).toByte()
        bytes[7] = (ancount and 0xFF).toByte()
        return bytes
    }

    private fun forwardUdp(
        srcIp: Int, srcPort: Int,
        dstIp: Int, dstPort: Int,
        payload: ByteArray
    ) {
        try {
            val key = "$srcPort"
            if (udpSockets.size > 64) {
                val it2 = udpSockets.entries.iterator()
                while (it2.hasNext() && udpSockets.size > 32) {
                    val e = it2.next()
                    try { e.value.close() } catch (_: Exception) {}
                    it2.remove()
                }
            }
            val socket = udpSockets.getOrPut(key) {
                DatagramSocket().also {
                    protectDatagramSocket(it)
                    it.soTimeout = 3000
                }
            }

            val dstAddr = ipFromInt(dstIp)
            val packet = DatagramPacket(payload, payload.size, dstAddr, dstPort)
            socket.send(packet)

            val buf = ByteArray(1500)
            val resp = DatagramPacket(buf, buf.size)
            try {
                socket.receive(resp)
                val ipPacket = buildUdpResponse(
                    dstIp = srcIp, dstPort = srcPort,
                    srcIp = dstIp, srcPort = dstPort,
                    payload = resp.data.copyOfRange(0, resp.length)
                )
                writeTunPacket(ipPacket)
            } catch (_: java.net.SocketTimeoutException) {} finally {
                udpSockets.remove(key)
                try { socket.close() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun ipFromInt(ip: Int): InetAddress {
        return InetAddress.getByAddress(
            byteArrayOf(
                (ip shr 24).toByte(),
                (ip shr 16).toByte(),
                (ip shr 8).toByte(),
                ip.toByte()
            )
        )
    }

    private fun buildUdpResponse(
        dstIp: Int, dstPort: Int,
        srcIp: Int, srcPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val buf = ByteBuffer.allocate(totalLen)

        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(0)
        buf.putShort(0x4000.toShort())
        buf.put(64.toByte())
        buf.put(17.toByte())
        val cksumPos = buf.position()
        buf.putShort(0)
        buf.putInt(srcIp)
        buf.putInt(dstIp)
        val ipCksum = computeChecksumVpn(buf.array(), 0, 20)
        buf.putShort(cksumPos, ipCksum)

        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort())
        buf.putShort(0)
        buf.put(payload)

        return buf.array()
    }

    private fun computeChecksumVpn(data: ByteArray, offset: Int, length: Int): Short {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            sum += (data[i].toInt() and 0xFF) shl 8 or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }

    private fun protectDatagramSocket(socket: DatagramSocket) {
        val ok = protect(socket)
        if (!ok) {
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val physicalNetwork = cm.activeNetwork?.let { active ->
                    val caps = cm.getNetworkCapabilities(active)
                    if (caps != null && !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) active
                    else null
                } ?: cm.allNetworks.firstOrNull { net ->
                    val caps = cm.getNetworkCapabilities(net)
                    caps != null
                        && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
                }
                if (physicalNetwork != null) {
                    physicalNetwork.bindSocket(socket)
                }
            } catch (_: Exception) {}
        }
    }

    private fun writeTunPacket(packet: ByteArray) {
        try {
            tunOut?.write(packet)
            tunOut?.flush()
        } catch (_: Exception) {}
    }

    private fun ipStringToInt(ip: String): Int {
        val parts = ip.split(".")
        if (parts.size != 4) return 0
        return (parts[0].toInt() shl 24) or (parts[1].toInt() shl 16) or (parts[2].toInt() shl 8) or parts[3].toInt()
    }
}
