package com.example.service

import android.content.Context
import java.io.DataInputStream

object RuCidrs {
    private var buckets: Map<Int, Pair<IntArray, ByteArray>>? = null

    fun init(context: Context) {
        if (buckets != null) return
        val map = HashMap<Int, Pair<IntArray, ByteArray>>()
        try {
            context.assets.open("cidrs_ru.bin").use { stream ->
                DataInputStream(stream).use { ds ->
                    val nBuckets = ds.readUnsignedShort()
                    repeat(nBuckets) {
                        val first = ds.readUnsignedByte()
                        val nEntries = ds.readUnsignedShort()
                        val ips = IntArray(nEntries)
                        val pfx = ByteArray(nEntries)
                        repeat(nEntries) { i ->
                            ips[i] = ds.readInt()
                            pfx[i] = ds.readByte()
                        }
                        map[first] = Pair(ips, pfx)
                    }
                }
            }
        } catch (_: Exception) {}
        buckets = map
    }

    fun contains(ip: Int): Boolean {
        val b = buckets ?: return false
        val first = (ip shr 24) and 0xFF
        val bucket = b[first] ?: return false
        val ips = bucket.first
        val pfx = bucket.second
        for (i in ips.indices) {
            val mask = (-1 shl (32 - pfx[i].toInt()))
            if ((ip and mask) == (ips[i] and mask)) return true
        }
        return false
    }
}
