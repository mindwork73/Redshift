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
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
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
    private var vpnJob: Job? = null
    @Volatile
    private var vpnRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeTunnels = ConcurrentHashMap<ConnectionKey, TcpTunnel>()
    private var remoteHost = "216.57.106.89"
    private var remotePort = 995
    private var socksLogin = ""
    private var socksPassword = ""

    private var nextConnectionId = 0

    companion object {
        const val ACTION_CONNECT = "com.example.action.CONNECT"
        const val ACTION_DISCONNECT = "com.example.action.DISCONNECT"
        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_USE_LOCAL_PROXY = "extra_use_local_proxy"
        const val EXTRA_SOCKS_LOGIN = "extra_socks_login"
        const val EXTRA_SOCKS_PASSWORD = "extra_socks_password"

        private const val VPN_MTU = 1500
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("RedShiftVPN", "onStartCommand: action=${intent?.action}, useLocal=${intent?.getBooleanExtra(EXTRA_USE_LOCAL_PROXY, false)}")
        when (intent?.action) {
            ACTION_CONNECT -> {
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
            ACTION_DISCONNECT -> {
                disconnectVpn()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        disconnectVpn()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        disconnectVpn()
        stopSelf()
        super.onDestroy()
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

        Log.e("RedShiftVPN", "Calling builder.establish()...")
        tunFd = builder.establish()
        if (tunFd == null) {
            Log.e("RedShiftVPN", "builder.establish() returned null — VPN not prepared?")
            stopSelf()
            return
        }
        Log.e("RedShiftVPN", "TUN interface established")
        tunOut = ParcelFileDescriptor.AutoCloseOutputStream(tunFd)

        vpnRunning = true
        val fd = tunFd!!
        vpnJob = scope.launch {
            Log.e("RedShiftVPN", "VPN loop started, fd.valid=${fd.fileDescriptor.valid()}")
            runVpnLoop(fd)
        }
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
    }

    private fun runVpnLoop(tunFdLocal: ParcelFileDescriptor) {
        Log.e("RedShiftVPN", "VPN loop: fd.valid=${tunFdLocal.fileDescriptor.valid()}")
        val buffer = ByteArray(VPN_MTU)
        var packetCount = 0
        var lastLogTime = System.currentTimeMillis()

        while (vpnRunning) {
            try {
                val bytesRead = Os.read(tunFdLocal.fileDescriptor, buffer, 0, buffer.size)
                if (bytesRead <= 0) {
                    Thread.sleep(50)
                    continue
                }
                packetCount++
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 2000) {
                    Log.e("RedShiftVPN", "VPN loop: read $packetCount packets so far, last $bytesRead bytes")
                    lastLogTime = now
                }
                processPacket(buffer, bytesRead)
            } catch (e: ErrnoException) {
                if (e.errno != OsConstants.EAGAIN) {
                    Log.e("RedShiftVPN", "VPN loop fatal errno: ${e.errno} ${e.message}")
                    break
                }
                Thread.sleep(50)
            } catch (e: Exception) {
                Log.e("RedShiftVPN", "VPN loop exception: ${e::class.simpleName}: ${e.message}")
                break
            }
        }
        Log.e("RedShiftVPN", "VPN loop exited, packetCount=$packetCount, vpnRunning=$vpnRunning")
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
        val srcIpInt = ByteBuffer.wrap(data, 12, 4).int
        val dstIpInt = ByteBuffer.wrap(data, 16, 4).int
        val dstIpStr = "${dstIpInt shr 24 and 0xFF}.${dstIpInt shr 16 and 0xFF}.${dstIpInt shr 8 and 0xFF}.${dstIpInt and 0xFF}"

        Log.e("RedShiftVPN", "Packet proto=$protocol dst=$dstIpStr len=$length")

        when (protocol) {
            6 -> handleTcpPacket(data, length, ihl)
            17 -> {
                val dstPort = (data[ihl + 2].toInt() and 0xFF) shl 8 or (data[ihl + 3].toInt() and 0xFF)
                Log.e("RedShiftVPN", "UDP dst=$dstIpStr:$dstPort len=$length")
                handleUdpPacket(data, length, ihl)
            }
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

        if (syn && !ack && !isLocal && !isProxyTraffic) {
            val dstStr = "${dstIp shr 24 and 0xFF}.${dstIp shr 16 and 0xFF}.${dstIp shr 8 and 0xFF}.${dstIp and 0xFF}"
            Log.e("RedShiftVPN", "TCP SYN from $srcPort to $dstStr:$dstPort, creating tunnel")
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
                    Log.e("RedShiftVPN", "protect(socket) returned $ok for tunnel")
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
                                Log.e("RedShiftVPN", "Physical network bindSocket() succeeded")
                            } else {
                                Log.e("RedShiftVPN", "No physical network found, cannot bind socket")
                            }
                        } catch (e: Exception) {
                            Log.e("RedShiftVPN", "Network.bindSocket() failed: ${e.message}")
                        }
                    }
                },
                onClose = { activeTunnels.remove(key) },
                socksLogin = socksLogin,
                socksPassword = socksPassword
            )

            scope.launch {
                tunnel.connectToRemoteProxy(remoteHost, remotePort) { writeTunPacket(it) }
            }

            activeTunnels[key] = tunnel
            return
        }

        if (isProxyTraffic) {
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

        val dnsIp = dstIp and 0xFF000000.toInt() == 0x0A000000.toInt()
        if (dstPort == 53 && !dnsIp) {
            scope.launch {
                forwardDns(srcIp, srcPort, dstIp, dstPort, payload)
            }
        }
    }

    private suspend fun forwardDns(
        srcIp: Int, srcPort: Int,
        dstIp: Int, dstPort: Int,
        query: ByteArray
    ) = withContext(Dispatchers.IO) {
        try {
            val dnsServer = InetAddress.getByAddress(
                byteArrayOf(
                    (dstIp shr 24).toByte(),
                    (dstIp shr 16).toByte(),
                    (dstIp shr 8).toByte(),
                    dstIp.toByte()
                )
            )
            val socket = DatagramSocket()
            protectDatagramSocket(socket)
            socket.soTimeout = 5000
            val packet = DatagramPacket(query, query.size, dnsServer, dstPort)
            socket.send(packet)

            val buf = ByteArray(512)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)

            val ipPacket = buildUdpResponse(
                dstIp = srcIp, dstPort = srcPort,
                srcIp = dstIp, srcPort = dstPort,
                payload = resp.data.copyOfRange(0, resp.length)
            )
            writeTunPacket(ipPacket)
            socket.close()
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
