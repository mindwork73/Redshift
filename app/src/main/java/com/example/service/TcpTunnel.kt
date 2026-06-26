package com.example.service

import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

class TcpTunnel(
    private val connectionId: Int,
    private val srcIp: Int,
    private val srcPort: Int,
    private val dstIp: Int,
    private val dstPort: Int,
    private val seqNum: Int,
    private val ackNum: Int,
    private val tunOutput: java.io.FileOutputStream,
    private val protectSocket: (Socket) -> Unit,
    private val onClose: () -> Unit,
    private val socksLogin: String = "",
    private val socksPassword: String = ""
) {
    private var remoteSocket: Socket? = null
    private var remoteOut: OutputStream? = null
    private var remoteIn: InputStream? = null

    private var remoteSeqNum = 0
    private var remoteAckNum = seqNum + 1

    @Volatile
    private var closed = false
    private var connected = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connectToRemoteProxy(proxyHost: String, proxyPort: Int, writer: (ByteArray) -> Unit) {
        if (closed) return
        val dstStr = "${dstIp shr 24 and 0xFF}.${dstIp shr 16 and 0xFF}.${dstIp shr 8 and 0xFF}.${dstIp and 0xFF}"

        try {
            Log.e("RedShiftVPN", "Tunnel[$connectionId] connecting to proxy $proxyHost:$proxyPort for $dstStr:$dstPort")
            val socket = Socket()
            try {
                Log.e("RedShiftVPN", "Tunnel[$connectionId] calling protect(socket)...")
                protectSocket(socket)
                Log.e("RedShiftVPN", "Tunnel[$connectionId] protect() called OK")
            } catch (e: Exception) {
                Log.e("RedShiftVPN", "Tunnel[$connectionId] protect() threw: ${e.message}")
            }
            socket.connect(InetSocketAddress(proxyHost, proxyPort), 5000)
            Log.e("RedShiftVPN", "Tunnel[$connectionId] connected to proxy, localAddr=${socket.localAddress}")
            socket.soTimeout = 30000
            socket.tcpNoDelay = true

            remoteSocket = socket
            remoteOut = socket.getOutputStream()
            remoteIn = socket.getInputStream()

            Log.e("RedShiftVPN", "Tunnel[$connectionId] connected to proxy, performing SOCKS5 handshake for $dstStr:$dstPort")
            performSocks5Handshake(dstIp, dstPort)
            connected = true
            Log.e("RedShiftVPN", "Tunnel[$connectionId] SOCKS5 handshake OK for $dstStr:$dstPort")

            sendSynAckToTun(writer)

            scope.launch {
                readFromRemote(writer)
            }
        } catch (e: Exception) {
            Log.e("RedShiftVPN", "Tunnel[$connectionId] proxy error: ${e.message}")
            close()
        }
    }

    private fun performSocks5Handshake(targetIp: Int, targetPort: Int) {
        val out = remoteOut ?: return
        val inp = remoteIn ?: return

        val useAuth = socksLogin.isNotEmpty() && socksPassword.isNotEmpty()
        val nMethods = if (useAuth) 2 else 1
        out.write(byteArrayOf(0x05, nMethods.toByte(), 0x00, 0x02))
        out.flush()

        val resp = ByteArray(2)
        readFully(inp, resp)
        when (resp[1]) {
            0x00.toByte() -> {}
            0x02.toByte() -> {
                val loginBytes = socksLogin.toByteArray()
                val passBytes = socksPassword.toByteArray()
                val authReq = ByteArray(3 + loginBytes.size + passBytes.size).apply {
                    this[0] = 0x01
                    this[1] = loginBytes.size.toByte()
                    System.arraycopy(loginBytes, 0, this, 2, loginBytes.size)
                    this[2 + loginBytes.size] = passBytes.size.toByte()
                    System.arraycopy(passBytes, 0, this, 3 + loginBytes.size, passBytes.size)
                }
                out.write(authReq)
                out.flush()
                val authResp = ByteArray(2)
                readFully(inp, authResp)
                if (authResp[1] != 0x00.toByte()) throw Exception("SOCKS5 userpass auth failed")
            }
            else -> throw Exception("SOCKS5 no acceptable auth method: ${resp[1]}")
        }

        val dstBytes = ByteArray(4)
        dstBytes[0] = (targetIp shr 24 and 0xFF).toByte()
        dstBytes[1] = (targetIp shr 16 and 0xFF).toByte()
        dstBytes[2] = (targetIp shr 8 and 0xFF).toByte()
        dstBytes[3] = (targetIp and 0xFF).toByte()

        val connectRequest = ByteArray(10).apply {
            this[0] = 0x05
            this[1] = 0x01
            this[2] = 0x00
            this[3] = 0x01
            System.arraycopy(dstBytes, 0, this, 4, 4)
            this[8] = (targetPort shr 8 and 0xFF).toByte()
            this[9] = (targetPort and 0xFF).toByte()
        }
        out.write(connectRequest)
        out.flush()

        val connectResp = ByteArray(10)
        readFully(inp, connectResp)
        if (connectResp[1] != 0x00.toByte()) throw Exception("SOCKS5 connect failed: ${connectResp[1]}")
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw Exception("Connection closed")
            offset += read
        }
    }

    private fun sendSynAckToTun(writer: (ByteArray) -> Unit) {
        val ipId = (connectionId % 65535).toShort()
        val packet = buildTcpPacket(
            dstIp = srcIp,
            dstPort = srcPort,
            srcIp = dstIp,
            srcPort = dstPort,
            seqNum = remoteSeqNum,
            ackNum = remoteAckNum,
            flags = 0x12,
            ipId = ipId,
            payload = ByteArray(0)
        )
        writer(packet)
    }

    fun forwardToRemote(data: ByteArray) {
        if (!connected) return
        try {
            remoteOut?.write(data)
            remoteOut?.flush()
            remoteAckNum = (remoteAckNum + data.size) and 0x7FFFFFFF
        } catch (_: Exception) {}
    }

    fun handleFin(seq: Int, ack: Int) {
        val finAck = buildTcpPacket(
            dstIp = srcIp, dstPort = srcPort,
            srcIp = dstIp, srcPort = dstPort,
            seqNum = remoteSeqNum, ackNum = seq + 1,
            flags = 0x11,
            ipId = (connectionId % 65535 + 1).toShort(),
            payload = ByteArray(0)
        )
        try {
            tunOutput.write(finAck)
            tunOutput.flush()
        } catch (_: Exception) {}
    }

    fun close() {
        closed = true
        scope.cancel()
        try { remoteSocket?.close() } catch (_: Exception) {}
        onClose()
    }

    private suspend fun readFromRemote(writer: (ByteArray) -> Unit) {
        try {
            val buf = ByteArray(8192)
            while (!closed) {
                val read = remoteIn?.read(buf) ?: -1
                if (read == -1) break

                val chunk = if (read == buf.size) buf else buf.copyOfRange(0, read)
                val packet = buildTcpPacket(
                    dstIp = srcIp, dstPort = srcPort,
                    srcIp = dstIp, srcPort = dstPort,
                    seqNum = remoteSeqNum, ackNum = remoteAckNum,
                    flags = 0x10,
                    ipId = (connectionId % 65535).toShort(),
                    payload = chunk
                )
                writer(packet)
                remoteSeqNum = (remoteSeqNum + read) and 0x7FFFFFFF
            }
        } catch (_: Exception) {} finally {
            close()
        }
    }

    private fun buildTcpPacket(
        dstIp: Int, dstPort: Int,
        srcIp: Int, srcPort: Int,
        seqNum: Int, ackNum: Int,
        flags: Int, ipId: Short,
        payload: ByteArray
    ): ByteArray {
        val tcpHeaderLen = 20
        val totalLen = 20 + tcpHeaderLen + payload.size
        val buf = ByteBuffer.allocate(totalLen)

        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(ipId)
        buf.putShort(0x4000.toShort())
        buf.put(64.toByte())
        buf.put(6.toByte())
        val checksumPos = buf.position()
        buf.putShort(0)
        buf.putInt(srcIp)
        buf.putInt(dstIp)
        val ipChecksum = computeChecksum(buf.array(), 0, 20)
        buf.putShort(checksumPos, ipChecksum)

        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putInt(seqNum)
        buf.putInt(ackNum)
        buf.put(((flags and 0x3F) or 0x50).toByte())
        buf.put(0x00.toByte())
        buf.putShort(0)
        buf.putShort(0)
        buf.put(payload)

        val tcpChecksum = computeTcpChecksum(
            buf.array(), 20, tcpHeaderLen + payload.size,
            srcIp, dstIp
        )
        buf.putShort(36, tcpChecksum)

        return buf.array()
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Short {
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

    private fun computeTcpChecksum(tcpPacket: ByteArray, offset: Int, length: Int, srcIp: Int, dstIp: Int): Short {
        var sum = 0

        sum += (srcIp shr 16) and 0xFFFF
        sum += srcIp and 0xFFFF
        sum += (dstIp shr 16) and 0xFFFF
        sum += dstIp and 0xFFFF
        sum += 6
        sum += length

        var i = offset
        while (i < offset + length - 1) {
            sum += (tcpPacket[i].toInt() and 0xFF) shl 8 or (tcpPacket[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < offset + length) {
            sum += (tcpPacket[i].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }
}
