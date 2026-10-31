package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject

private fun endpointKey(protocol: String, address: String, port: Int): String? {
    val addr = address.trim()
    val proto = protocol.uppercase().filter { it.isLetterOrDigit() }
    if (addr.isBlank() || port <= 0) return null
    return "$addr:$port:$proto"
}

private fun nameKey(obj: JSONObject): String {
    val name = obj.optString("name", "").uppercase().trim().filter { it.isLetterOrDigit() || it == ' ' }.trim()
    val protoFamily = obj.optString("protocol", "").uppercase().substringBefore('+').trim()
    return obj.optString("url", "") + "|$protoFamily::$name"
}

/** Dedupe by exact endpoint, then by logical node name (same name = rotated/regenerated node). */
private fun dedupeServerArray(arr: JSONArray): JSONArray {
    val survivors = mutableListOf<JSONObject>()
    val seenEndpoint = HashSet<String>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val key = endpointKey(
            obj.optString("protocol", ""),
            obj.optString("address", ""),
            obj.optInt("port", 0)
        )
        if (key != null && !seenEndpoint.add(obj.optString("url", "") + "|" + key)) continue
        survivors.add(obj)
    }
    val byName = LinkedHashMap<String, MutableList<JSONObject>>()
    for (obj in survivors) {
        byName.getOrPut(nameKey(obj)) { mutableListOf() }.add(obj)
    }
    val out = JSONArray()
    val seenName = HashSet<String>()
    for (obj in survivors) {
        val nk = nameKey(obj)
        if (!seenName.add(nk)) continue
        val party = byName[nk] ?: continue
        // Prefer the row tied to a real subscription url (current), drop stale url-less artifacts.
        val winner = party.maxWithOrNull(
            compareBy({ it.optString("url", "").isNotBlank() }, { it.optString("id", "").isNotBlank() })
        ) ?: obj
        out.put(winner)
    }
    return out
}

class SubscriptionRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val urls = store.getBlockingSubscriptionUrls().sorted()
        var failed = false
        for (url in urls) {
            if (!refreshOne(store, url)) failed = true
        }
        return if (failed) Result.retry() else Result.success()
    }

    private suspend fun refreshOne(store: SettingsStore, url: String): Boolean {
        val client = SubscriptionClient()
        val result = client.fetchSubscription(url)

        if (result.error.isNotEmpty() || result.servers.isEmpty()) return false

        // Merge: keep servers of other subscriptions (e.g. Amnezia vpn:// nodes),
        // replace only this subscription's cached servers. Deduplicate within each
        // source so two independent subscriptions cannot erase one another.
        val serversJson = JSONArray()
        val seen = HashSet<String>()
        val previous = mutableListOf<JSONObject>()
        val originalCache = store.getBlockingCachedServersJson()
        val existing = try { JSONArray(originalCache.ifBlank { "[]" }) } catch (_: org.json.JSONException) { return false }
        run {
            for (i in 0 until existing.length()) {
                val obj = existing.getJSONObject(i)
                if (obj.optString("url") == url) {
                    previous.add(obj)
                    continue
                }
                val key = endpointKey(
                    obj.optString("protocol", ""),
                    obj.optString("address", ""),
                    obj.optInt("port", 0)
                )
                if (key != null && !seen.add(obj.optString("url", "") + "|" + key)) continue
                serversJson.put(obj)
            }
        }

        for (server in result.servers) {
            val key = endpointKey(server.protocol, server.address, server.port)
            if (key != null && !seen.add(url + "|" + key)) continue
            val candidate = JSONObject().put("url", url).put("name", server.name).put("protocol", server.protocol)
            val old = previous.firstOrNull { item ->
                (key != null && endpointKey(item.optString("protocol"), item.optString("address"), item.optInt("port")) == key) ||
                    nameKey(item) == nameKey(candidate)
            }
            serversJson.put(JSONObject().apply {
                put("id", old?.optString("id")?.takeIf { it.isNotBlank() } ?: server.id)
                put("name", server.name)
                put("protocol", server.protocol)
                put("address", server.address)
                put("port", server.port)
                put("flag", server.flag)
                put("uuid", server.uuid)
                put("password", server.password)
                put("flow", server.flow)
                put("encryption", server.encryption)
                put("network", server.network)
                put("tls", server.tls)
                put("sni", server.sni)
                put("publicKey", server.publicKey)
                put("shortId", server.shortId)
                put("fingerprint", server.fingerprint)
                put("privateKey", server.privateKey)
                put("presharedKey", server.presharedKey)
                put("serverPublicKey", server.serverPublicKey)
                put("localAddress", server.localAddress)
                put("mtu", server.mtu)
                put("awgParams", server.awgParams)
                put("dns", server.dns)
                put("path", server.path)
                put("host", server.host)
                put("extra", server.extra)
                put("alpn", server.alpn)
                put("url", url)
            })
        }

        val finalJson = dedupeServerArray(serversJson)
        return store.saveRefreshIfUnchanged(url, originalCache, finalJson.toString(), finalJson.length())
    }
}
