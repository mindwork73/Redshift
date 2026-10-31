package com.example.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "redshift_settings")

class SettingsStore(private val context: Context) {

    companion object {
        val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val KEY_KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KEY_LOCAL_PORT = intPreferencesKey("local_port")
        val KEY_ALLOW_LAN = booleanPreferencesKey("allow_lan")
        val KEY_SELECTED_SERVER_ID = stringPreferencesKey("selected_server_id")
        val KEY_REMOTE_PROXY_HOST = stringPreferencesKey("remote_proxy_host")
        val KEY_REMOTE_PROXY_PORT = intPreferencesKey("remote_proxy_port")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_SUBSCRIPTION_URL = stringPreferencesKey("subscription_url")
        val KEY_SUBSCRIPTION_URLS = stringSetPreferencesKey("subscription_urls")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val KEY_REFRESH_INTERVAL_HOURS = intPreferencesKey("refresh_interval_hours")
        val KEY_CACHED_SERVERS_JSON = stringPreferencesKey("cached_servers_json")
        val KEY_CACHED_SERVERS_COUNT = intPreferencesKey("cached_servers_count")
        val KEY_LAST_REFRESH_TIME = longPreferencesKey("last_refresh_time")
        val KEY_TARIFF_NAME = stringPreferencesKey("tariff_name")
        val KEY_SUBSCRIPTION_EXPIRY = stringPreferencesKey("subscription_expiry")
        val KEY_SUBSCRIPTION_EXPIRY_TS = longPreferencesKey("subscription_expiry_ts")
        val KEY_REMIND_3D_SENT = booleanPreferencesKey("remind_3d_sent")
        val KEY_REMIND_1D_SENT = booleanPreferencesKey("remind_1d_sent")
        val KEY_REMIND_EXPIRED_SENT = booleanPreferencesKey("remind_expired_sent")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_SPLIT_TUNNEL_ENABLED = booleanPreferencesKey("split_tunnel_enabled")
        val KEY_SPLIT_TUNNEL_BY_APPS = booleanPreferencesKey("split_tunnel_by_apps")
        val KEY_SPLIT_TUNNEL_ONLY = booleanPreferencesKey("split_tunnel_only")
        val KEY_SPLIT_DOMAINS = stringPreferencesKey("split_domains")
        val KEY_SPLIT_APPS = stringPreferencesKey("split_apps")
        val KEY_ROUTING_MODE = stringPreferencesKey("routing_mode")
        val KEY_BYPASS_RUSSIA = booleanPreferencesKey("bypass_russia")
        val KEY_BYPASS_CHINA = booleanPreferencesKey("bypass_china")
        val KEY_BYPASS_LOCAL = booleanPreferencesKey("bypass_local")
        val KEY_BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val KEY_BLOCK_ADS = booleanPreferencesKey("block_ads")
        val KEY_AUTO_SELECT_BEST = booleanPreferencesKey("auto_select_best")
        val KEY_SORT_BY_PING = booleanPreferencesKey("sort_by_ping")

        /**
         * Servers the user deleted from the list, stored as identity keys (see
         * `RedShiftState.hideKeysForParts`). The server list itself is rebuilt from the
         * cache and the subscription on every launch, so without this set a deleted
         * server would reappear on the next refresh.
         */
        val KEY_HIDDEN_SERVERS = stringSetPreferencesKey("hidden_servers")
    }

    val startOnBoot: Flow<Boolean> = context.dataStore.data.map { it[KEY_START_ON_BOOT] ?: false }
    val killSwitch: Flow<Boolean> = context.dataStore.data.map { it[KEY_KILL_SWITCH] ?: false }
    val autoReconnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_RECONNECT] ?: true }
    val localPort: Flow<Int> = context.dataStore.data.map { it[KEY_LOCAL_PORT] ?: 1080 }
    val allowLan: Flow<Boolean> = context.dataStore.data.map { it[KEY_ALLOW_LAN] ?: false }
    val selectedServerId: Flow<String> = context.dataStore.data.map { it[KEY_SELECTED_SERVER_ID] ?: "nl_reality" }
    val remoteProxyHost: Flow<String> = context.dataStore.data.map { it[KEY_REMOTE_PROXY_HOST] ?: "37.220.84.106" }
    val remoteProxyPort: Flow<Int> = context.dataStore.data.map { it[KEY_REMOTE_PROXY_PORT] ?: 995 }
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "" }
    val subscriptionUrl: Flow<String> = context.dataStore.data.map { it[KEY_SUBSCRIPTION_URL] ?: "" }
    val subscriptionUrls: Flow<Set<String>> = context.dataStore.data.map { subscriptionUrlsFrom(it) }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }
    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_REFRESH] ?: false }
    val refreshIntervalHours: Flow<Int> = context.dataStore.data.map { it[KEY_REFRESH_INTERVAL_HOURS] ?: 6 }
    val cachedServersJson: Flow<String> = context.dataStore.data.map { it[KEY_CACHED_SERVERS_JSON] ?: "" }
    val cachedServersCount: Flow<Int> = context.dataStore.data.map { it[KEY_CACHED_SERVERS_COUNT] ?: 0 }
    val lastRefreshTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_REFRESH_TIME] ?: 0L }
    val tariffName: Flow<String> = context.dataStore.data.map { it[KEY_TARIFF_NAME] ?: "" }
    val subscriptionExpiry: Flow<String> = context.dataStore.data.map { it[KEY_SUBSCRIPTION_EXPIRY] ?: "" }
    val subscriptionExpiryTs: Flow<Long> = context.dataStore.data.map { it[KEY_SUBSCRIPTION_EXPIRY_TS] ?: 0L }
    val remind3dSent: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMIND_3D_SENT] ?: false }
    val remind1dSent: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMIND_1D_SENT] ?: false }
    val remindExpiredSent: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMIND_EXPIRED_SENT] ?: false }
    val userId: Flow<String> = context.dataStore.data.map { it[KEY_USER_ID] ?: "" }
    val splitTunnelEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SPLIT_TUNNEL_ENABLED] ?: false }
    val splitTunnelByApps: Flow<Boolean> = context.dataStore.data.map { it[KEY_SPLIT_TUNNEL_BY_APPS] ?: false }
    val splitTunnelOnly: Flow<Boolean> = context.dataStore.data.map { it[KEY_SPLIT_TUNNEL_ONLY] ?: true }
    val splitDomains: Flow<String> = context.dataStore.data.map { it[KEY_SPLIT_DOMAINS] ?: "" }
    val splitApps: Flow<String> = context.dataStore.data.map { it[KEY_SPLIT_APPS] ?: "" }
    val routingMode: Flow<String> = context.dataStore.data.map { it[KEY_ROUTING_MODE] ?: "rule" }
    val bypassRussia: Flow<Boolean> = context.dataStore.data.map { it[KEY_BYPASS_RUSSIA] ?: true }
    val bypassChina: Flow<Boolean> = context.dataStore.data.map { it[KEY_BYPASS_CHINA] ?: false }
    val bypassLocal: Flow<Boolean> = context.dataStore.data.map { it[KEY_BYPASS_LOCAL] ?: true }
    val bypassLan: Flow<Boolean> = context.dataStore.data.map { it[KEY_BYPASS_LAN] ?: true }
    val blockAds: Flow<Boolean> = context.dataStore.data.map { it[KEY_BLOCK_ADS] ?: false }
    val autoSelectBest: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SELECT_BEST] ?: false }
    val sortByPing: Flow<Boolean> = context.dataStore.data.map { it[KEY_SORT_BY_PING] ?: false }
    val hiddenServers: Flow<Set<String>> = context.dataStore.data.map { it[KEY_HIDDEN_SERVERS] ?: emptySet() }

    suspend fun setStartOnBoot(value: Boolean) = context.dataStore.edit { it[KEY_START_ON_BOOT] = value }
    suspend fun setKillSwitch(value: Boolean) = context.dataStore.edit { it[KEY_KILL_SWITCH] = value }
    suspend fun setAutoReconnect(value: Boolean) = context.dataStore.edit { it[KEY_AUTO_RECONNECT] = value }
    suspend fun setLocalPort(value: Int) = context.dataStore.edit { it[KEY_LOCAL_PORT] = value }
    suspend fun setAllowLan(value: Boolean) = context.dataStore.edit { it[KEY_ALLOW_LAN] = value }
    suspend fun setSelectedServerId(value: String) = context.dataStore.edit { it[KEY_SELECTED_SERVER_ID] = value }
    suspend fun setRemoteProxyHost(value: String) = context.dataStore.edit { it[KEY_REMOTE_PROXY_HOST] = value }
    suspend fun setRemoteProxyPort(value: Int) = context.dataStore.edit { it[KEY_REMOTE_PROXY_PORT] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[KEY_LANGUAGE] = value }
    private fun subscriptionUrlsFrom(prefs: Preferences): Set<String> {
        prefs[KEY_SUBSCRIPTION_URLS]?.let { return it }
        val urls = mutableSetOf<String>()
        prefs[KEY_SUBSCRIPTION_URL]?.takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let(urls::add)
        try {
            val items = org.json.JSONArray((prefs[KEY_CACHED_SERVERS_JSON] ?: "").ifBlank { "[]" })
            for (i in 0 until items.length()) {
                val url = items.optJSONObject(i)?.optString("url").orEmpty()
                if (url.startsWith("http://") || url.startsWith("https://")) urls.add(url)
            }
        } catch (_: org.json.JSONException) { }
        return urls
    }

    suspend fun setSubscriptionUrl(value: String) = context.dataStore.edit { prefs ->
        val urls = subscriptionUrlsFrom(prefs).toMutableSet()
        if (value.startsWith("http://") || value.startsWith("https://")) urls.add(value)
        prefs[KEY_SUBSCRIPTION_URLS] = urls
        prefs[KEY_SUBSCRIPTION_URL] = value
    }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[KEY_NOTIFICATIONS] = value }
    suspend fun setAutoRefresh(value: Boolean) = context.dataStore.edit { it[KEY_AUTO_REFRESH] = value }
    suspend fun setRefreshIntervalHours(value: Int) = context.dataStore.edit { it[KEY_REFRESH_INTERVAL_HOURS] = value }
    suspend fun setCachedServersJson(value: String) = context.dataStore.edit { it[KEY_CACHED_SERVERS_JSON] = value }
    suspend fun setCachedServersCount(value: Int) = context.dataStore.edit { it[KEY_CACHED_SERVERS_COUNT] = value }
    suspend fun setLastRefreshTime(value: Long) = context.dataStore.edit { it[KEY_LAST_REFRESH_TIME] = value }
    suspend fun setTariffName(value: String) = context.dataStore.edit { it[KEY_TARIFF_NAME] = value }
    suspend fun setSubscriptionExpiry(value: String) = context.dataStore.edit { it[KEY_SUBSCRIPTION_EXPIRY] = value }
    suspend fun setSubscriptionExpiryTs(value: Long) = context.dataStore.edit { it[KEY_SUBSCRIPTION_EXPIRY_TS] = value }
    suspend fun setRemind3dSent(value: Boolean) = context.dataStore.edit { it[KEY_REMIND_3D_SENT] = value }
    suspend fun setRemind1dSent(value: Boolean) = context.dataStore.edit { it[KEY_REMIND_1D_SENT] = value }
    suspend fun setRemindExpiredSent(value: Boolean) = context.dataStore.edit { it[KEY_REMIND_EXPIRED_SENT] = value }
    suspend fun setUserId(value: String) = context.dataStore.edit { it[KEY_USER_ID] = value }
    suspend fun setSplitTunnelEnabled(value: Boolean) = context.dataStore.edit { it[KEY_SPLIT_TUNNEL_ENABLED] = value }
    suspend fun setSplitTunnelByApps(value: Boolean) = context.dataStore.edit { it[KEY_SPLIT_TUNNEL_BY_APPS] = value }
    suspend fun setSplitTunnelOnly(value: Boolean) = context.dataStore.edit { it[KEY_SPLIT_TUNNEL_ONLY] = value }
    suspend fun setSplitDomains(value: String) = context.dataStore.edit { it[KEY_SPLIT_DOMAINS] = value }
    suspend fun setSplitApps(value: String) = context.dataStore.edit { it[KEY_SPLIT_APPS] = value }
    suspend fun setRoutingMode(value: String) = context.dataStore.edit { it[KEY_ROUTING_MODE] = value }
    suspend fun setBypassRussia(value: Boolean) = context.dataStore.edit { it[KEY_BYPASS_RUSSIA] = value }
    suspend fun setBypassChina(value: Boolean) = context.dataStore.edit { it[KEY_BYPASS_CHINA] = value }
    suspend fun setBypassLocal(value: Boolean) = context.dataStore.edit { it[KEY_BYPASS_LOCAL] = value }
    suspend fun setBypassLan(value: Boolean) = context.dataStore.edit { it[KEY_BYPASS_LAN] = value }
    suspend fun setBlockAds(value: Boolean) = context.dataStore.edit { it[KEY_BLOCK_ADS] = value }
    suspend fun setAutoSelectBest(value: Boolean) = context.dataStore.edit { it[KEY_AUTO_SELECT_BEST] = value }
    suspend fun setSortByPing(value: Boolean) = context.dataStore.edit { it[KEY_SORT_BY_PING] = value }
    suspend fun setHiddenServers(value: Set<String>) = context.dataStore.edit { it[KEY_HIDDEN_SERVERS] = value }

    suspend fun clearSavedServersAndSubscriptions() = context.dataStore.edit { prefs ->
        prefs[KEY_SUBSCRIPTION_URL] = ""
        prefs[KEY_SUBSCRIPTION_URLS] = emptySet()
        prefs[KEY_CACHED_SERVERS_JSON] = "[]"
        prefs[KEY_CACHED_SERVERS_COUNT] = 0
        prefs[KEY_SELECTED_SERVER_ID] = ""
        prefs[KEY_LAST_REFRESH_TIME] = 0L
        prefs[KEY_HIDDEN_SERVERS] = emptySet()
        prefs[KEY_TARIFF_NAME] = ""
        prefs[KEY_SUBSCRIPTION_EXPIRY] = ""
        prefs[KEY_SUBSCRIPTION_EXPIRY_TS] = 0L
    }

    /** Remove only the selected subscription and its cached nodes in one transaction. */
    suspend fun removeSubscription(url: String): Boolean {
        var removed = false
        context.dataStore.edit { prefs ->
            val urls = subscriptionUrlsFrom(prefs).toMutableSet()
            if (url !in urls) return@edit
            val old = org.json.JSONArray((prefs[KEY_CACHED_SERVERS_JSON] ?: "").ifBlank { "[]" })
            val remaining = org.json.JSONArray()
            for (i in 0 until old.length()) {
                val item = old.getJSONObject(i)
                if (item.optString("url") != url) remaining.put(item)
            }
            urls.remove(url)
            prefs[KEY_SUBSCRIPTION_URLS] = urls
            if (prefs[KEY_SUBSCRIPTION_URL] == url) prefs[KEY_SUBSCRIPTION_URL] = urls.sorted().firstOrNull().orEmpty()
            prefs[KEY_CACHED_SERVERS_JSON] = remaining.toString()
            prefs[KEY_CACHED_SERVERS_COUNT] = remaining.length()
            prefs[KEY_LAST_REFRESH_TIME] = 0L
            prefs[KEY_TARIFF_NAME] = ""
            prefs[KEY_SUBSCRIPTION_EXPIRY] = ""
            prefs[KEY_SUBSCRIPTION_EXPIRY_TS] = 0L
            removed = true
        }
        return removed
    }

    /** Do not let a slow refresh overwrite newer imports or a removed subscription. */
    suspend fun saveRefreshIfUnchanged(url: String, expectedCache: String, newCache: String, count: Int): Boolean {
        var saved = false
        context.dataStore.edit { prefs ->
            if (url !in subscriptionUrlsFrom(prefs) || (prefs[KEY_CACHED_SERVERS_JSON] ?: "") != expectedCache) return@edit
            prefs[KEY_CACHED_SERVERS_JSON] = newCache
            prefs[KEY_CACHED_SERVERS_COUNT] = count
            prefs[KEY_LAST_REFRESH_TIME] = System.currentTimeMillis()
            saved = true
        }
        return saved
    }

    suspend fun saveCacheIfUnchanged(expectedCache: String, newCache: String, count: Int, sourceUrl: String): Boolean {
        var saved = false
        context.dataStore.edit { prefs ->
            if ((prefs[KEY_CACHED_SERVERS_JSON] ?: "") != expectedCache) return@edit
            if (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://")) {
                prefs[KEY_SUBSCRIPTION_URLS] = subscriptionUrlsFrom(prefs) + sourceUrl
            }
            prefs[KEY_CACHED_SERVERS_JSON] = newCache
            prefs[KEY_CACHED_SERVERS_COUNT] = count
            prefs[KEY_LAST_REFRESH_TIME] = System.currentTimeMillis()
            saved = true
        }
        return saved
    }

    /** Permanently remove locally cached entries for one server. */
    suspend fun removeCachedServer(id: String, extra: String) {
        context.dataStore.edit { prefs ->
            val old = try { org.json.JSONArray(prefs[KEY_CACHED_SERVERS_JSON] ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            val remaining = org.json.JSONArray()
            for (i in 0 until old.length()) {
                val item = old.optJSONObject(i) ?: continue
                if (item.optString("id") == id || (extra.isNotEmpty() && item.optString("extra") == extra)) continue
                remaining.put(item)
            }
            prefs[KEY_CACHED_SERVERS_JSON] = remaining.toString()
            prefs[KEY_CACHED_SERVERS_COUNT] = remaining.length()
        }
    }

    fun getBlockingRoutingMode(): String {
        return runBlocking { routingMode.first() }
    }

    fun getBlockingBypassRussia(): Boolean {
        return runBlocking { bypassRussia.first() }
    }

    fun getBlockingBypassChina(): Boolean {
        return runBlocking { bypassChina.first() }
    }

    fun getBlockingBlockAds(): Boolean {
        return runBlocking { blockAds.first() }
    }

    fun getBlockingSelectedServerId(): String {
        return runBlocking { selectedServerId.first() }
    }

    fun getBlockingRemoteProxyHost(): String {
        return runBlocking { remoteProxyHost.first() }
    }

    fun getBlockingRemoteProxyPort(): Int {
        return runBlocking { remoteProxyPort.first() }
    }

    fun getBlockingSubscriptionUrl(): String {
        return runBlocking { subscriptionUrl.first() }
    }

    fun getBlockingSubscriptionUrls(): Set<String> = runBlocking { subscriptionUrls.first() }

    fun getBlockingAutoRefresh(): Boolean {
        return runBlocking { autoRefresh.first() }
    }

    fun getBlockingCachedServersJson(): String {
        return runBlocking { cachedServersJson.first() }
    }

    fun getBlockingHiddenServers(): Set<String> {
        return runBlocking { hiddenServers.first() }
    }

    fun getBlockingExpiryTs(): Long {
        return runBlocking { subscriptionExpiryTs.first() }
    }
}
