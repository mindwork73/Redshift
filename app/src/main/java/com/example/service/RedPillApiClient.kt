package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SubInfo(
    val id: Int,
    val tariff: String,
    val status: String,
    val region: String,
    val devicesLimit: Int,
    val startsAt: String,
    val expiresAt: String,
    val devices: List<DeviceInfo> = emptyList()
)

data class DeviceInfo(
    val id: Int,
    val subscriptionId: Int,
    val userId: Int,
    val deviceIndex: Int,
    val deviceName: String,
    val region: String,
    val status: String
)

data class UserInfo(
    val userId: Int,
    val username: String,
    val createdAt: String,
    val subscription: SubInfo?,
    val deviceCount: Int
)

data class AdminStats(
    val totalUsers: Int,
    val activeSubscriptions: Int,
    val totalDevices: Int
)

data class ServerInfo(
    val code: String,
    val name: String,
    val host: String,
    val endpoint: String,
    val publicKey: String,
    val isEnabled: Boolean
)

class RedPillApiClient(
    private val baseUrl: String = "https://api.redpillcloud.ru",
    private val adminToken: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    data class ProxyConfig(
        val host: String,
        val port: Int,
        val protocol: String,
        val socksLogin: String = "",
        val socksPassword: String = ""
    )

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/v1/ping").build()
            val resp = client.newCall(req).execute()
            resp.isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun getProxy(userId: Int? = null): ProxyConfig? = withContext(Dispatchers.IO) {
        try {
            val url = if (userId != null) "$baseUrl/api/v1/proxy?user_id=$userId" else "$baseUrl/api/v1/proxy"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body!!.string())
            ProxyConfig(
                host = json.getString("host"),
                port = json.getInt("port"),
                protocol = json.getString("protocol"),
                socksLogin = json.optString("socks_login", ""),
                socksPassword = json.optString("socks_password", "")
            )
        } catch (_: Exception) { null }
    }

    suspend fun getUser(tgId: Int): UserInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/v1/user/$tgId").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body!!.string())
            val subJson = json.optJSONObject("subscription")
            UserInfo(
                userId = json.getInt("user_id"),
                username = json.optString("username", ""),
                createdAt = json.optString("created_at", ""),
                subscription = subJson?.let { parseSubInfo(it) },
                deviceCount = json.optInt("device_count", 0)
            )
        } catch (_: Exception) { null }
    }

    suspend fun getSubscription(tgId: Int): SubInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/v1/user/$tgId/subscription").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body!!.string())
            parseSubInfo(json)
        } catch (_: Exception) { null }
    }

    suspend fun getDevices(tgId: Int): List<DeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/v1/user/$tgId/devices").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val arr = JSONObject(resp.body!!.string()).getJSONArray("devices")
            (0 until arr.length()).map { i -> parseDeviceInfo(arr.getJSONObject(i)) }
        } catch (_: Exception) { emptyList() }
    }

    // Admin

    suspend fun adminGrant(userId: Int, tariff: String = "pro_1m", days: Int = 30, region: String = "nl"): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("user_id", userId)
                put("tariff", tariff)
                put("days", days)
                put("region", region)
            }
            val req = Request.Builder()
                .url("$baseUrl/api/v1/admin/user/grant")
                .header("token", adminToken)
                .post(body.toString().toRequestBody(jsonType))
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            JSONObject(resp.body!!.string())
        } catch (_: Exception) { null }
    }

    suspend fun adminExtend(userId: Int, days: Int = 30): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("user_id", userId)
                put("days", days)
            }
            val req = Request.Builder()
                .url("$baseUrl/api/v1/admin/user/extend")
                .header("token", adminToken)
                .post(body.toString().toRequestBody(jsonType))
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            JSONObject(resp.body!!.string())
        } catch (_: Exception) { null }
    }

    suspend fun adminRevoke(userId: Int): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("user_id", userId) }
            val req = Request.Builder()
                .url("$baseUrl/api/v1/admin/user/revoke")
                .header("token", adminToken)
                .post(body.toString().toRequestBody(jsonType))
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            JSONObject(resp.body!!.string())
        } catch (_: Exception) { null }
    }

    suspend fun adminReissue(userId: Int, deviceIndex: Int = 0, region: String = "nl"): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("user_id", userId)
                put("device_index", deviceIndex)
                put("region", region)
            }
            val req = Request.Builder()
                .url("$baseUrl/api/v1/admin/user/reissue")
                .header("token", adminToken)
                .post(body.toString().toRequestBody(jsonType))
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            JSONObject(resp.body!!.string())
        } catch (_: Exception) { null }
    }

    suspend fun adminListUsers(): JSONArray? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/admin/users")
                .header("token", adminToken)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            JSONObject(resp.body!!.string()).getJSONArray("users")
        } catch (_: Exception) { null }
    }

    suspend fun adminStats(): AdminStats? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/admin/stats")
                .header("token", adminToken)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body!!.string())
            AdminStats(
                totalUsers = json.getInt("total_users"),
                activeSubscriptions = json.getInt("active_subscriptions"),
                totalDevices = json.getInt("total_devices")
            )
        } catch (_: Exception) { null }
    }

    suspend fun listServers(): List<ServerInfo> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/v1/servers").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val arr = JSONObject(resp.body!!.string()).getJSONArray("servers")
            (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                ServerInfo(
                    code = s.getString("code"),
                    name = s.getString("name"),
                    host = s.getString("host"),
                    endpoint = s.optString("endpoint", ""),
                    publicKey = s.optString("public_key", ""),
                    isEnabled = s.optInt("is_enabled", 1) == 1
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseSubInfo(json: JSONObject): SubInfo {
        val devicesArr = json.optJSONArray("devices")
        val devices = if (devicesArr != null) {
            (0 until devicesArr.length()).map { parseDeviceInfo(devicesArr.getJSONObject(it)) }
        } else emptyList()
        return SubInfo(
            id = json.getInt("id"),
            tariff = json.optString("tariff", ""),
            status = json.optString("status", ""),
            region = json.optString("region", ""),
            devicesLimit = json.optInt("devices_limit", 1),
            startsAt = json.optString("starts_at", ""),
            expiresAt = json.optString("expires_at", ""),
            devices = devices
        )
    }

    private fun parseDeviceInfo(json: JSONObject) = DeviceInfo(
        id = json.getInt("id"),
        subscriptionId = json.optInt("subscription_id", 0),
        userId = json.getInt("user_id"),
        deviceIndex = json.getInt("device_index"),
        deviceName = json.optString("device_name", ""),
        region = json.optString("region", ""),
        status = json.optString("status", "")
    )
}
