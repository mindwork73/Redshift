package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject

class SubscriptionRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val url = store.getBlockingSubscriptionUrl()
        if (url.isBlank()) return Result.success()

        val client = SubscriptionClient()
        val result = client.fetchSubscription(url)

        if (result.error != null) return Result.retry()

        val serversJson = JSONArray()
        for (server in result.servers) {
            serversJson.put(JSONObject().apply {
                put("id", server.id)
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
            })
        }

        store.setCachedServersJson(serversJson.toString())
        store.setCachedServersCount(result.servers.size)
        store.setLastRefreshTime(System.currentTimeMillis())

        return Result.success()
    }
}
