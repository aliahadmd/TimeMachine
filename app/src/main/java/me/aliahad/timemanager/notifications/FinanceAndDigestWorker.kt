package me.aliahad.timemanager.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.R
import me.aliahad.timemanager.SubscriptionAnalytics
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.data.HabitRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class FinanceAndDigestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        NotificationChannelRegistry.ensureChannels(context)
        val container = context.appContainerOrNull() ?: return Result.success()
        val prefs = container.preferencesRepository.getPreferences()

        if (!prefs.notificationsEnabled) return Result.success()
        if (NotificationChannelRegistry.isQuietHoursNow(prefs)) return Result.success()
        if (!NotificationChannelRegistry.hasPostNotificationsPermission(context)) return Result.success()

        val rateLimitPrefs = context.getSharedPreferences(RATE_LIMIT_PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val todayIso = today.format(SafeDateTime.ISO_DATE)

        val subscriptions = container.database.subscriptionDao().getActiveSubscriptionsSync()
        subscriptions.forEach { sub ->
            val daysUntil = SubscriptionAnalytics.daysUntilBilling(sub.nextBillingDate, today)
            val trialSoon = SubscriptionAnalytics.isTrialEndingSoon(sub, today)
            val renewalSoon = SubscriptionAnalytics.isRenewalDueSoon(sub, today) || daysUntil < 0

            val keyPrefix = when {
                trialSoon -> "trial_${sub.id}_$todayIso"
                renewalSoon -> "renew_${sub.id}_$todayIso"
                else -> null
            } ?: return@forEach

            if (rateLimitPrefs.getBoolean(keyPrefix, false)) return@forEach

            val notifId = NotificationChannelRegistry.FINANCE_NOTIF_BASE +
                ((sub.id and 0x7FFF_FFFFL) % 40_000L).toInt()
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(
                    NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                    NotificationChannelRegistry.ROUTE_FINANCE
                )
                putExtra(NotificationChannelRegistry.EXTRA_INITIAL_TAB, 2)
            }
            val tapPending = PendingIntent.getActivity(
                context,
                notifId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val renewIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_RENEW_SUBSCRIPTION
                putExtra(ReminderActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
            }
            val renewPending = PendingIntent.getBroadcast(
                context,
                notifId + 45_000,
                renewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_SNOOZE_SUBSCRIPTION
                putExtra(ReminderActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
            }
            val snoozePending = PendingIntent.getBroadcast(
                context,
                notifId + 90_000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = when {
                trialSoon -> "Trial Ending Soon: ${sub.name}"
                daysUntil < 0 -> "Overdue Renewal: ${sub.name}"
                daysUntil == 0L -> "Renews Today: ${sub.name}"
                daysUntil == 1L -> "Renews Tomorrow: ${sub.name}"
                else -> "Upcoming Renewal: ${sub.name}"
            }
            val costLabel = Formatters.formatCurrency(
                sub.cost,
                sub.currency.ifBlank { prefs.currencySymbol }
            )
            val body = when {
                trialSoon -> "Free trial ends ${SafeDateTime.formatRelativeDate(sub.trialEndDate ?: sub.nextBillingDate)} ($costLabel)"
                daysUntil < 0 -> "${-daysUntil}d overdue • $costLabel (${sub.billingCycle})"
                else -> "Due ${SafeDateTime.formatRelativeDate(sub.nextBillingDate)} • $costLabel"
            }

            val notification = NotificationCompat.Builder(
                context,
                NotificationChannelRegistry.CHANNEL_FINANCE_ALERTS
            )
                .setSmallIcon(R.drawable.ic_stat_timemachine)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setGroup(NotificationChannelRegistry.GROUP_KEY_FINANCE)
                .setAutoCancel(true)
                .setContentIntent(tapPending)
                .addAction(0, "Renew & Log", renewPending)
                .addAction(0, "Snooze 1d", snoozePending)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notifId, notification)
                rateLimitPrefs.edit().putBoolean(keyPrefix, true).apply()
            } catch (sec: SecurityException) {
                Log.w(TAG, "Cannot post finance alert", sec)
            }
        }

        // Evening habit check-in digest (once per day after 19:00)
        if (LocalTime.now().hour >= 19) {
            val digestKey = "evening_digest_$todayIso"
            if (!rateLimitPrefs.getBoolean(digestKey, false)) {
                val activeHabits = container.database.habitDao().getAllActiveHabitsSync()
                val scheduledRemaining = activeHabits.filter { habit ->
                    HabitRepository.isScheduledOnDate(habit, today) &&
                        container.database.habitDao().getCompletion(habit.id, todayIso) == null
                }
                if (scheduledRemaining.isNotEmpty()) {
                    val digestId = NotificationChannelRegistry.WELLBEING_NOTIF_BASE + 1
                    val tapIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(
                            NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                            NotificationChannelRegistry.ROUTE_WELLBEING
                        )
                    }
                    val tapPending = PendingIntent.getActivity(
                        context,
                        digestId,
                        tapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val summaryNames = scheduledRemaining.take(3).joinToString(", ") { it.name }
                    val notification = NotificationCompat.Builder(
                        context,
                        NotificationChannelRegistry.CHANNEL_WELLBEING_NUDGES
                    )
                        .setSmallIcon(R.drawable.ic_stat_timemachine)
                        .setContentTitle("Evening Check-In: ${scheduledRemaining.size} habit${if (scheduledRemaining.size > 1) "s" else ""} left")
                        .setContentText(summaryNames)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setAutoCancel(true)
                        .setContentIntent(tapPending)
                        .build()
                    try {
                        NotificationManagerCompat.from(context).notify(digestId, notification)
                        rateLimitPrefs.edit().putBoolean(digestKey, true).apply()
                    } catch (sec: SecurityException) {
                        Log.w(TAG, "Cannot post evening digest", sec)
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "FinanceAndDigestWorker"
        private const val UNIQUE_PERIODIC = "tm_finance_digest_periodic"
        private const val UNIQUE_IMMEDIATE = "tm_finance_digest_immediate"
        const val RATE_LIMIT_PREFS = "tm_notif_rate_limit"

        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<FinanceAndDigestWorker>(
                24, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runImmediateCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<FinanceAndDigestWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun clearRateLimits(context: Context) {
            context.getSharedPreferences(RATE_LIMIT_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
