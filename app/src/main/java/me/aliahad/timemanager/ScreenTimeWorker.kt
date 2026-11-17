package me.aliahad.timemanager

import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import java.time.LocalDate
import java.time.ZoneId

class ScreenTimeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "ScreenTimeWorker"
        private const val PREFS_NAME = "screen_time_usage"
        private const val KEY_LAST_PROCESSED = "last_processed"
        private const val KEY_PENDING_START = "pending_start"
        private const val KEY_ACTIVE_APP_PKG = "active_app_pkg"
        private const val KEY_PENDING_UNLOCK = "pending_unlock"
        private const val MAX_LOOKBACK_MS = 12 * 60 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        // Without Usage Access granted, succeed quietly instead of triggering endless WorkManager retries
        if (!ScreenTimePermissionHelper.hasUsageAccess(applicationContext)) {
            return Result.success()
        }

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val defaultStart = (now - MAX_LOOKBACK_MS).coerceAtLeast(0L)
        var lastProcessed = prefs.getLong(KEY_LAST_PROCESSED, defaultStart)
        if (lastProcessed > now) {
            // Clock rolled back: clamp safely
            lastProcessed = defaultStart
        }

        var pendingStart = prefs.getLong(KEY_PENDING_START, 0L).takeIf { it > 0L && it <= now }
        if (pendingStart != null && now - pendingStart > ScreenTimeSegmentSplitter.MAX_SINGLE_SESSION_MS) {
            pendingStart = null
        }
        var activeAppPkg = prefs.getString(KEY_ACTIVE_APP_PKG, null)
        var pendingUnlockCount = prefs.getInt(KEY_PENDING_UNLOCK, 0)

        val usageStatsManager =
            applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return Result.success()

        val events = usageStatsManager.queryEvents(lastProcessed, now) ?: return Result.success()
        val rawSegments = mutableListOf<RawScreenSession>()
        val labelCache = mutableMapOf<String, String>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp < lastProcessed) continue

            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    if (pendingStart == null) {
                        pendingStart = event.timeStamp
                        pendingUnlockCount = 0
                    }
                }

                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    if (pendingStart == null) {
                        pendingStart = event.timeStamp
                    }
                    pendingUnlockCount += 1
                }

                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val pkg = event.packageName
                    val currentStart = pendingStart
                    if (currentStart != null && !activeAppPkg.isNullOrBlank() &&
                        activeAppPkg != pkg && event.timeStamp > currentStart
                    ) {
                        rawSegments += RawScreenSession(
                            startMs = currentStart,
                            endMs = event.timeStamp,
                            unlockCount = pendingUnlockCount.coerceAtLeast(if (rawSegments.isEmpty()) 1 else 0),
                            appUsed = resolveAppLabel(activeAppPkg!!, labelCache)
                        )
                        pendingStart = event.timeStamp
                        pendingUnlockCount = 0
                    }
                    if (!pkg.isNullOrBlank()) {
                        activeAppPkg = pkg
                    }
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    val start = pendingStart
                    if (start != null && event.timeStamp > start) {
                        rawSegments += RawScreenSession(
                            startMs = start,
                            endMs = event.timeStamp,
                            unlockCount = pendingUnlockCount.coerceAtLeast(1),
                            appUsed = activeAppPkg?.let { resolveAppLabel(it, labelCache) }
                        )
                    }
                    pendingStart = null
                    pendingUnlockCount = 0
                    if (event.eventType == UsageEvents.Event.DEVICE_SHUTDOWN) {
                        activeAppPkg = null
                    }
                }
            }
        }

        val container = applicationContext.appContainerOrNull()
        val zoneId = ZoneId.systemDefault()
        val affectedDates = mutableSetOf<String>()
        val today = LocalDate.now(zoneId)
        val todayIso = today.format(SafeDateTime.ISO_DATE)

        withContext(Dispatchers.IO) {
            val database = container?.database ?: me.aliahad.timemanager.data.TimerDatabase.getDatabase(
                applicationContext
            )
            val dao = database.screenTimeDao()

            rawSegments.forEach { raw ->
                ScreenTimeSegmentSplitter.splitSessionByDay(raw, zoneId).forEach { slice ->
                    dao.insertSession(slice)
                    affectedDates += slice.date
                }
            }

            affectedDates.forEach { date ->
                val sessions = dao.getSessionsForDateSync(date)
                ScreenTimeSegmentSplitter.buildDailySummary(date, sessions)?.let { summary ->
                    dao.insertDailySummary(summary)
                }
                dao.deleteHourlyForDate(date)
                ScreenTimeSegmentSplitter.buildHourlyBuckets(date, sessions, zoneId).forEach { hourly ->
                    dao.insertHourlyData(hourly)
                }
            }

            // Finalize yesterday's screen-time-under-limit habits if applicable
            if (container != null) {
                val userPrefs = container.preferencesRepository.getPreferences()
                val yesterdayIso = today.minusDays(1).format(SafeDateTime.ISO_DATE)
                val yesterdaySummary = dao.getDailySummary(yesterdayIso)
                if (yesterdaySummary != null) {
                    container.habitSyncUseCase.syncFinalizedScreenTimeDay(
                        date = yesterdayIso,
                        screenTimeMinutesForDay = yesterdaySummary.totalScreenTimeSeconds / 60,
                        defaultLimitMinutes = userPrefs.dailyScreenTimeLimitMinutes
                    )
                }

                // Check today's screen-time limit nudge (rate-limited once/day)
                val todaySummary = dao.getDailySummary(todayIso)
                val todayMinutes = (todaySummary?.totalScreenTimeSeconds ?: 0) / 60
                if (todayMinutes >= userPrefs.dailyScreenTimeLimitMinutes &&
                    userPrefs.dailyScreenTimeLimitMinutes > 0 &&
                    userPrefs.lastScreenTimeNudgeDate != todayIso &&
                    userPrefs.notificationsEnabled &&
                    !NotificationChannelRegistry.isQuietHoursNow(userPrefs)
                ) {
                    postScreenTimeNudge(todayMinutes, userPrefs.dailyScreenTimeLimitMinutes)
                    container.preferencesRepository.setLastScreenTimeNudgeDate(todayIso)
                }

                if (affectedDates.isNotEmpty()) {
                    container.notificationGateway.requestWidgetRefresh()
                }
            }
        }

        prefs.edit()
            .putLong(KEY_LAST_PROCESSED, now)
            .putLong(KEY_PENDING_START, pendingStart ?: 0L)
            .putString(KEY_ACTIVE_APP_PKG, activeAppPkg)
            .putInt(KEY_PENDING_UNLOCK, pendingUnlockCount)
            .apply()

        return Result.success()
    }

    private fun resolveAppLabel(packageName: String, cache: MutableMap<String, String>): String {
        cache[packageName]?.let { return it }
        val label = try {
            val info = applicationContext.packageManager.getApplicationInfo(packageName, 0)
            applicationContext.packageManager.getApplicationLabel(info).toString().ifBlank { packageName }
        } catch (_: Throwable) {
            packageName.substringAfterLast('.')
        }
        cache[packageName] = label
        return label
    }

    private fun postScreenTimeNudge(todayMinutes: Int, limitMinutes: Int) {
        if (!NotificationChannelRegistry.hasPostNotificationsPermission(applicationContext)) return
        NotificationChannelRegistry.ensureChannels(applicationContext)
        val notifId = NotificationChannelRegistry.WELLBEING_NOTIF_BASE + 10
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_WELLBEING
            )
            putExtra(NotificationChannelRegistry.EXTRA_INITIAL_TAB, 1)
        }
        val tapPending = PendingIntent.getActivity(
            applicationContext,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val focusIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FOCUS
            )
            putExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, true)
        }
        val focusPending = PendingIntent.getActivity(
            applicationContext,
            notifId + 1,
            focusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannelRegistry.CHANNEL_WELLBEING_NUDGES
        )
            .setSmallIcon(R.drawable.ic_stat_timemachine)
            .setContentTitle("Daily Screen Time Limit Reached")
            .setContentText(
                "${Formatters.formatMinutes(todayMinutes)} used today (goal ${Formatters.formatMinutes(limitMinutes)})"
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .addAction(0, "Start 25m Focus", focusPending)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(notifId, notification)
        } catch (sec: SecurityException) {
            Log.w(TAG, "Cannot post screen time nudge", sec)
        }
    }
}
