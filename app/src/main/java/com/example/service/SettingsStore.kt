package com.example.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val KEY_REFRESH_INTERVAL_HOURS = intPreferencesKey("refresh_interval_hours")
        val KEY_CACHED_SERVERS_JSON = stringPreferencesKey("cached_servers_json")
        val KEY_CACHED_SERVERS_COUNT = intPreferencesKey("cached_servers_count")
        val KEY_LAST_REFRESH_TIME = longPreferencesKey("last_refresh_time")
    }

    val startOnBoot: Flow<Boolean> = context.dataStore.data.map { it[KEY_START_ON_BOOT] ?: false }
    val killSwitch: Flow<Boolean> = context.dataStore.data.map { it[KEY_KILL_SWITCH] ?: false }
    val autoReconnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_RECONNECT] ?: true }
    val localPort: Flow<Int> = context.dataStore.data.map { it[KEY_LOCAL_PORT] ?: 1080 }
    val allowLan: Flow<Boolean> = context.dataStore.data.map { it[KEY_ALLOW_LAN] ?: false }
    val selectedServerId: Flow<String> = context.dataStore.data.map { it[KEY_SELECTED_SERVER_ID] ?: "nl_reality" }
    val remoteProxyHost: Flow<String> = context.dataStore.data.map { it[KEY_REMOTE_PROXY_HOST] ?: "216.57.106.89" }
    val remoteProxyPort: Flow<Int> = context.dataStore.data.map { it[KEY_REMOTE_PROXY_PORT] ?: 995 }
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "EN" }
    val subscriptionUrl: Flow<String> = context.dataStore.data.map { it[KEY_SUBSCRIPTION_URL] ?: "" }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }
    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_REFRESH] ?: false }
    val refreshIntervalHours: Flow<Int> = context.dataStore.data.map { it[KEY_REFRESH_INTERVAL_HOURS] ?: 6 }
    val cachedServersJson: Flow<String> = context.dataStore.data.map { it[KEY_CACHED_SERVERS_JSON] ?: "" }
    val cachedServersCount: Flow<Int> = context.dataStore.data.map { it[KEY_CACHED_SERVERS_COUNT] ?: 0 }
    val lastRefreshTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_REFRESH_TIME] ?: 0L }

    suspend fun setStartOnBoot(value: Boolean) = context.dataStore.edit { it[KEY_START_ON_BOOT] = value }
    suspend fun setKillSwitch(value: Boolean) = context.dataStore.edit { it[KEY_KILL_SWITCH] = value }
    suspend fun setAutoReconnect(value: Boolean) = context.dataStore.edit { it[KEY_AUTO_RECONNECT] = value }
    suspend fun setLocalPort(value: Int) = context.dataStore.edit { it[KEY_LOCAL_PORT] = value }
    suspend fun setAllowLan(value: Boolean) = context.dataStore.edit { it[KEY_ALLOW_LAN] = value }
    suspend fun setSelectedServerId(value: String) = context.dataStore.edit { it[KEY_SELECTED_SERVER_ID] = value }
    suspend fun setRemoteProxyHost(value: String) = context.dataStore.edit { it[KEY_REMOTE_PROXY_HOST] = value }
    suspend fun setRemoteProxyPort(value: Int) = context.dataStore.edit { it[KEY_REMOTE_PROXY_PORT] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[KEY_LANGUAGE] = value }
    suspend fun setSubscriptionUrl(value: String) = context.dataStore.edit { it[KEY_SUBSCRIPTION_URL] = value }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[KEY_NOTIFICATIONS] = value }
    suspend fun setAutoRefresh(value: Boolean) = context.dataStore.edit { it[KEY_AUTO_REFRESH] = value }
    suspend fun setRefreshIntervalHours(value: Int) = context.dataStore.edit { it[KEY_REFRESH_INTERVAL_HOURS] = value }
    suspend fun setCachedServersJson(value: String) = context.dataStore.edit { it[KEY_CACHED_SERVERS_JSON] = value }
    suspend fun setCachedServersCount(value: Int) = context.dataStore.edit { it[KEY_CACHED_SERVERS_COUNT] = value }
    suspend fun setLastRefreshTime(value: Long) = context.dataStore.edit { it[KEY_LAST_REFRESH_TIME] = value }

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

    fun getBlockingAutoRefresh(): Boolean {
        return runBlocking { autoRefresh.first() }
    }

    fun getBlockingCachedServersJson(): String {
        return runBlocking { cachedServersJson.first() }
    }
}
