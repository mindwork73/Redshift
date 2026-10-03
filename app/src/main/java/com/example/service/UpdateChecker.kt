package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases based auto-update. Checks https://api.github.com/repos/
 * mindwork73/Redshift/releases/latest, compares the tag to the installed
 * version and, on demand, downloads the release APK into the app cache and
 * hands it to the Android package installer via a FileProvider URI.
 */
object UpdateChecker {

    const val REPO = "mindwork73/Redshift"
    val API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val versionName: String,
        val tagName: String,
        val changelog: String,
        val apkUrl: String,
        val sizeBytes: Long
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Semantic version compare: "0.1.4" > "0.1.2", handles prerelease suffixes. */
    fun isNewerVersion(latest: String, installed: String): Boolean {
        fun parts(v: String): List<Int> {
            val clean = v.trim().lowercase().removePrefix("v")
            val body = clean.substringBefore('-').substringBefore('+')
            return body.split('.').mapNotNull { it.toIntOrNull() }
        }
        val a = parts(latest)
        val b = parts(installed)
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av > bv
        }
        return false
    }

    /** Fetches the latest release metadata; null on network/parse errors. */
    suspend fun checkLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(API_URL)
                .header("User-Agent", "RedShift/${BuildConfig.VERSION_NAME}")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body!!.string())
            val tag = json.optString("tag_name", "").removePrefix("v")
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var sizeBytes = 0L
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url", "")
                        sizeBytes = a.optLong("size", 0L)
                        break
                    }
                }
            }
            if (apkUrl.isBlank()) return@withContext null
            UpdateInfo(
                versionName = tag,
                tagName = json.optString("tag_name", tag),
                changelog = json.optString("body", ""),
                apkUrl = apkUrl,
                sizeBytes = sizeBytes
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Downloads the release APK into cacheDir/apk and returns the file, or null.
     * Call on a background dispatcher.
     */
    suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "apk").apply { mkdirs() }
            val fileName = "redshift-update.apk"
            val out = File(dir, fileName)
            val req = Request.Builder().url(url)
                .header("User-Agent", "RedShift/${BuildConfig.VERSION_NAME}")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            resp.body?.byteStream()?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            if (out.length() == 0L) return@withContext null
            out
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Launches the Android package installer with a FileProvider URI. Returns
     * false when the app is not allowed to install unknown apps yet.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        val pm = context.packageManager
        if (!pm.canRequestPackageInstalls()) return false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            return false
        }
        return true
    }

    /** Deep-links to the "install unknown apps" settings for our package. */
    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}