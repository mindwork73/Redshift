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
    private val socksPassword: String = "",
    private val routeMode: Int = 0
) {
    private var remoteSocket: Socket? = null
    private var remoteOut: OutputStream? = null
    private var remoteIn: InputStream? = null

    private var remoteSeqNum = 0

    companion object {
        @Volatile var totalBytesUp: Long = 0L
        @Volatile var totalBytesDown: Long = 0L
        fun resetCounters() { totalBytesUp = 0L; totalBytesDown = 0L }
    }
    private var remoteAckNum = seqNum + 1

    @Volatile
    private var closed = false
    private var connected = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun tunnelLog(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val line = "$ts [TUN$connectionId] $msg\n"
        try {
            val f = java.io.File("/data/data/com.aistudio.redshift.rkzvpt/files/redshift_debug.log")
            f.appendText(line)
        } catch (_: Exception) {}
    }    suspend fun connectToRemoteProxy(proxyHost: String, proxyPort: Int, euProxyHost: String, euProxyPort: Int, writer: (ByteArray) -> Unit) {
        if (closed) return
        val dstStr = "${dstIp shr 24 and 0xFF}.${dstIp shr 16 and 0xFF}.${dstIp shr 8 and 0xFF}.${dstIp and 0xFF}"

        try {
            val socket = Socket()
            try {
                protectSocket(socket)
            } catch (e: Exception) {
                tunnelLog("protect() ERROR: ${e.message}")
            }

            val isDirect = routeMode == 1
            val useEu = routeMode == 2

            if (isDirect) {
                socket.connect(InetSocketAddress(dstStr, dstPort), 5000)
            } else if (useEu) {
                socket.connect(InetSocketAddress(euProxyHost, euProxyPort), 5000)
            } else {
                socket.connect(InetSocketAddress(proxyHost, proxyPort), 5000)
            }

            socket.soTimeout = 30000
            socket.tcpNoDelay = true
            remoteSocket = socket
            remoteOut = socket.getOutputStream()
            remoteIn = socket.getInputStream()

            if (!isDirect) {
                performSocks5Handshake(dstIp, dstPort)
            }

            connected = true
            sendSynAckToTun(writer)
            remoteSeqNum = (remoteSeqNum + 1) and 0x7FFFFFFF

            scope.launch {
                readFromRemote(writer)
            }
        } catch (e: Exception) {
            tunnelLog("ERROR: $dstStr:$dstPort -> ${e.message}")
            close()
        }
    }

    private fun performSocks5Handshake(targetIp: Int, targetPort: Int) {
        val out = remoteOut ?: return
        val inp = remoteIn ?: return

        val useAuth = socksLogin.isNotEmpty() && socksPassword.isNotEmpty()
        val greeting = if (useAuth) {
            byteArrayOf(0x05, 0x02, 0x00, 0x02)
        } else {
            byteArrayOf(0x05, 0x01, 0x00)
        }
        out.write(greeting)
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
            totalBytesUp += data.size
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

    fun rstAndClose() {
        if (closed) return
        val rst = buildTcpPacket(
            dstIp = srcIp, dstPort = srcPort,
            srcIp = dstIp, srcPort = dstPort,
            seqNum = remoteSeqNum, ackNum = remoteAckNum,
            flags = 0x04,
            ipId = (connectionId % 65535 + 2).toShort(),
            payload = ByteArray(0)
        )
        try {
            tunOutput.write(rst)
            tunOutput.flush()
        } catch (_: Exception) {}
        close()
    }

    private suspend fun readFromRemote(writer: (ByteArray) -> Unit) {
        try {
            val buf = ByteArray(16384)
            while (!closed) {
                val read = remoteIn?.read(buf) ?: -1
                if (read == -1) break
                totalBytesDown += read

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
        buf.put(0x50.toByte()) // data offset = 5 * 16
        buf.put(flags.toByte()) // TCP flags (SYN, ACK, etc.)
        buf.putShort(65535.toShort()) // window size
        buf.putShort(0) // TCP checksum placeholder
        buf.putShort(0) // urgent pointer
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
