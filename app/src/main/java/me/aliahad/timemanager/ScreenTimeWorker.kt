package me.aliahad.timemanager

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.data.ScreenTimeDailySummary
import me.aliahad.timemanager.data.ScreenTimeHourly
import me.aliahad.timemanager.data.ScreenTimeSession
import me.aliahad.timemanager.data.TimerDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScreenTimeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    
    companion object {
        private const val PREFS_NAME = "screen_time_usage"
        private const val KEY_LAST_PROCESSED = "last_processed"
        private const val KEY_PENDING_START = "pending_start"
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val MAX_LOOKBACK_MS = 12 * 60 * 60 * 1000L // 12 hours safeguard
    }
    
    override suspend fun doWork(): Result {
        if (!ScreenTimePermissionHelper.hasUsageAccess(applicationContext)) {
            return Result.retry()
        }
        
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val defaultStart = (now - MAX_LOOKBACK_MS).coerceAtLeast(0L)
        val lastProcessed = prefs.getLong(KEY_LAST_PROCESSED, defaultStart)
        var pendingStart = prefs.getLong(KEY_PENDING_START, 0L).takeIf { it > 0 }
        
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return Result.failure()
        
        val events = usageStatsManager.queryEvents(lastProcessed, now)
        val database = TimerDatabase.getDatabase(applicationContext)
        val sessionPairs = mutableListOf<Pair<Long, Long>>()
        
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    if (pendingStart == null) {
                        pendingStart = event.timeStamp
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    val start = pendingStart
                    if (start != null && event.timeStamp > start) {
                        sessionPairs.add(start to event.timeStamp)
                    }
                    pendingStart = null
                }
            }
        }
        
        if (sessionPairs.isEmpty() && pendingStart == prefs.getLong(KEY_PENDING_START, 0L)) {
            // Nothing new; update timestamp and exit
            prefs.edit()
                .putLong(KEY_LAST_PROCESSED, now)
                .putLong(KEY_PENDING_START, pendingStart ?: 0L)
                .apply()
            return Result.success()
        }
        
        val dao = database.screenTimeDao()
        val affectedDates = mutableSetOf<String>()
        
        withContext(Dispatchers.IO) {
            sessionPairs.forEach { (start, end) ->
                val durationSeconds = ((end - start) / 1000).toInt()
                if (durationSeconds <= 0) return@forEach
                
                val startDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(start),
                    ZoneId.systemDefault()
                )
                val endDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(end),
                    ZoneId.systemDefault()
                )
                
                // Split sessions that span multiple days
                var currentStart = startDateTime
                var remainingEnd = endDateTime
                while (currentStart.toLocalDate().isBefore(remainingEnd.toLocalDate())) {
                    val splitEnd = currentStart.toLocalDate().atStartOfDay().plusDays(1)
                    val splitDuration = ((splitEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                        currentStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 1000).toInt()
                    if (splitDuration > 0) {
                        saveSession(dao, currentStart, splitEnd, splitDuration)
                        affectedDates.add(currentStart.toLocalDate().format(DATE_FORMAT))
                    }
                    currentStart = splitEnd
                }
                
                // Final segment
                val segmentDuration = ((remainingEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                    currentStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 1000).toInt()
                if (segmentDuration > 0) {
                    saveSession(dao, currentStart, remainingEnd, segmentDuration)
                    affectedDates.add(currentStart.toLocalDate().format(DATE_FORMAT))
                }
            }
            
            affectedDates.forEach { date ->
                updateDailySummary(date, dao)
                updateHourlyData(date, dao)
            }
        }
        
        prefs.edit()
            .putLong(KEY_LAST_PROCESSED, now)
            .putLong(KEY_PENDING_START, pendingStart ?: 0L)
            .apply()
        
        return Result.success()
    }
    
    private suspend fun saveSession(
        dao: me.aliahad.timemanager.data.ScreenTimeDao,
        start: LocalDateTime,
        end: LocalDateTime,
        durationSeconds: Int
    ) {
        val session = ScreenTimeSession(
            date = start.toLocalDate().format(DATE_FORMAT),
            timestamp = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            sessionStart = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            sessionEnd = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationSeconds = durationSeconds,
            unlockCount = 1,
            wasWalking = false,
            appUsed = null
        )
        dao.insertSession(session)
    }
    
    private suspend fun updateDailySummary(
        date: String,
        dao: me.aliahad.timemanager.data.ScreenTimeDao
    ) {
        val sessions = dao.getSessionsForDateSync(date)
        if (sessions.isEmpty()) return
        
        val totalScreenTime = sessions.sumOf { it.durationSeconds }
        val pickupCount = sessions.size
        val avgSession = totalScreenTime / pickupCount.coerceAtLeast(1)
        val longestSession = sessions.maxOfOrNull { it.durationSeconds } ?: 0
        val shortestSession = sessions.minOfOrNull { it.durationSeconds } ?: 0
        val firstPickup = sessions.minOfOrNull { it.sessionStart }
        val lastPickup = sessions.maxOfOrNull { it.sessionStart }
        
        val summary = ScreenTimeDailySummary(
            date = date,
            totalScreenTimeSeconds = totalScreenTime,
            pickupsCount = pickupCount,
            firstPickupTime = firstPickup,
            lastPickupTime = lastPickup,
            walkingScreenTimeSeconds = 0,
            avgSessionDurationSeconds = avgSession,
            longestSessionSeconds = longestSession,
            shortestSessionSeconds = shortestSession,
            notificationsCount = 0
        )
        dao.insertDailySummary(summary)
    }
    
    private suspend fun updateHourlyData(
        date: String,
        dao: me.aliahad.timemanager.data.ScreenTimeDao
    ) {
        val sessions = dao.getSessionsForDateSync(date)
        if (sessions.isEmpty()) return
        
        val hourlyMap = sessions.groupBy { session ->
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(session.sessionStart),
                ZoneId.systemDefault()
            ).hour
        }
        
        hourlyMap.forEach { (hour, hourSessions) ->
            val totalTime = hourSessions.sumOf { it.durationSeconds }
            val pickups = hourSessions.size
            val hourlyData = ScreenTimeHourly(
                date = date,
                hour = hour,
                screenTimeSeconds = totalTime,
                pickupsCount = pickups
            )
            dao.insertHourlyData(hourlyData)
        }
    }
}
