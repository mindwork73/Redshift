package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlinx.coroutines.flow.first

/**
 * Daily expiry reminder, mirroring the RedPill Telegram bot behaviour
 * (3 days / 1 day before, plus "expired" once). Threshold flags are persisted
 * so each stage fires exactly once per subscription lifetime.
 */
object ExpiryReminderScheduler {

    private const val WORK_NAME = "subscription_expiry_reminder"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SubscriptionExpiryReminderWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class SubscriptionExpiryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val store = SettingsStore(context)
        val expiryMs = store.getBlockingExpiryTs()
        if (expiryMs <= 0) return Result.success()

        val storeRef = store
        val daysLeft = ceil(((expiryMs - System.currentTimeMillis()) / 86400000.0)).toInt()
        val sent3 = storeRef.remind3dSent.first()
        val sent1 = storeRef.remind1dSent.first()
        val sentExpired = storeRef.remindExpiredSent.first()

        val message: String? = when {
            daysLeft <= 0 && !sentExpired -> {
                storeRef.setRemindExpiredSent(true)
                "subscription_expired"
            }
            daysLeft <= 1 && !sent1 -> {
                storeRef.setRemind1dSent(true)
                "subscription_expires_tomorrow"
            }
            daysLeft <= 3 && !sent3 -> {
                storeRef.setRemind3dSent(true)
                "subscription_expires_soon"
            }
            else -> null
        }

        if (message != null) {
            postNotification(context, message)
        }
        return Result.success()
    }

    private fun postNotification(context: Context, key: String) {
        val message = when (key) {
            "subscription_expires_soon" -> {
                if (isRu()) "Подписка RedShift истекает через 3 дня. Продлите её в @redpillcloudbot." else "Your RedShift subscription expires in 3 days. Renew it via @redpillcloudbot."
            }
            "subscription_expires_tomorrow" -> {
                if (isRu()) "Подписка RedShift истекает завтра. Продлите её в @redpillcloudbot." else "Your RedShift subscription expires tomorrow. Renew it via @redpillcloudbot."
            }
            else -> {
                if (isRu()) "Подписка RedShift истекла. Продлите её в @redpillcloudbot." else "Your RedShift subscription has expired. Renew it via @redpillcloudbot."
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Subscription reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(if (isRu()) "RedShift: подписка" else "RedShift: subscription")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            manager.notify(REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun isRu(): Boolean {
        val lang = runCatching {
            com.example.ui.LocalizationState.currentLanguage.code
        }.getOrDefault("en")
        return lang == "ru"
    }

    private companion object {
        const val CHANNEL_ID = "subscription_reminders"
        const val REMINDER_NOTIFICATION_ID = 7331
    }
}