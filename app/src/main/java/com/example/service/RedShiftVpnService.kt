package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class RedShiftVpnService : VpnService() {

    private var tunFd: ParcelFileDescriptor? = null
    private var tunOut: FileOutputStream? = null
    private var vpnJob: Job? = null
    @Volatile
    private var vpnRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeTunnels = ConcurrentHashMap<ConnectionKey, TcpTunnel>()
    private var remoteHost = "216.57.106.89"
    private var remotePort = 995

    private var nextConnectionId = 0

    companion object {
        const val ACTION_CONNECT = "com.example.action.CONNECT"
        const val ACTION_DISCONNECT = "com.example.action.DISCONNECT"
        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_USE_LOCAL_PROXY = "extra_use_local_proxy"

        private const val VPN_MTU = 1500
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "redshift_vpn"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val useLocal = intent.getBooleanExtra(EXTRA_USE_LOCAL_PROXY, false)
                if (useLocal) {
                    remoteHost = "127.0.0.1"
                    remotePort = com.example.service.SingBoxManager.SOCKS_PORT
                } else {
                    remoteHost = intent.getStringExtra(EXTRA_HOST) ?: remoteHost
                    remotePort = intent.getIntExtra(EXTRA_PORT, remotePort)
                }
                connectVpn()
            }
            ACTION_DISCONNECT -> disconnectVpn()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        disconnectVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        disconnectVpn()
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

        tunFd = builder.establish() ?: return
        tunOut = FileOutputStream(tunFd?.fileDescriptor)

        val startIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RedShift VPN")
            .setContentText("Connected — securing your traffic")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        vpnRunning = true
        vpnJob = scope.launch {
            runVpnLoop()
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

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        val disconnectIntent = Intent("com.example.VPN_DISCONNECTED")
        sendBroadcast(disconnectIntent)
    }

    private fun runVpnLoop() {
        val input = FileInputStream(tunFd?.fileDescriptor)
        val buffer = ByteArray(VPN_MTU)

        try {
            while (vpnRunning) {
                val bytesRead = input.read(buffer)
                if (bytesRead <= 0) break

                processPacket(buffer, bytesRead)
            }
        } catch (e: Exception) {
            if (vpnRunning) disconnectVpn()
        }
    }

    private fun processPacket(data: ByteArray, length: Int) {
        if (length < 20) return

        val version = data[0].toInt() shr 4 and 0x0F
        if (version != 4) return

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

        if (syn && !ack && !isLocal) {
            val tunnel = TcpTunnel(
                connectionId = nextConnectionId++,
                srcIp = srcIp,
                srcPort = srcPort,
                dstIp = dstIp,
                dstPort = dstPort,
                seqNum = seqNum,
                ackNum = ackNum,
                tunOutput = tunOut!!,
                protectSocket = { protect(it) },
                onClose = { activeTunnels.remove(key) }
            )

            scope.launch {
                tunnel.connectToRemoteProxy(remoteHost, remotePort) { writeTunPacket(it) }
            }

            activeTunnels[key] = tunnel
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
        if (Build.VERSION.SDK_INT >= 33) {
            protect(socket)
        }
    }

    private fun writeTunPacket(packet: ByteArray) {
        try {
            tunOut?.write(packet)
            tunOut?.flush()
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RedShift VPN Status",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
