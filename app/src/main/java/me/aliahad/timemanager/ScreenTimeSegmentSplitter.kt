package me.aliahad.timemanager

import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.ScreenTimeDailySummary
import me.aliahad.timemanager.data.ScreenTimeHourly
import me.aliahad.timemanager.data.ScreenTimeSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class RawScreenSession(
    val startMs: Long,
    val endMs: Long,
    val unlockCount: Int = 1,
    val appUsed: String? = null
)

object ScreenTimeSegmentSplitter {
    const val MAX_SINGLE_SESSION_MS = 12 * 60 * 60 * 1000L

    fun splitSessionByDay(
        raw: RawScreenSession,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScreenTimeSession> {
        if (raw.endMs <= raw.startMs) return emptyList()
        if (raw.endMs - raw.startMs > MAX_SINGLE_SESSION_MS) return emptyList()

        val result = mutableListOf<ScreenTimeSession>()
        var cursor = LocalDateTime.ofInstant(Instant.ofEpochMilli(raw.startMs), zoneId)
        val finalEnd = LocalDateTime.ofInstant(Instant.ofEpochMilli(raw.endMs), zoneId)
        var firstSlice = true

        while (cursor.toLocalDate().isBefore(finalEnd.toLocalDate())) {
            val nextMidnight = cursor.toLocalDate().plusDays(1).atStartOfDay()
            val startMillis = cursor.atZone(zoneId).toInstant().toEpochMilli()
            val endMillis = nextMidnight.atZone(zoneId).toInstant().toEpochMilli()
            val secs = ((endMillis - startMillis) / 1000L).toInt()
            if (secs > 0) {
                result += ScreenTimeSession(
                    date = cursor.toLocalDate().format(SafeDateTime.ISO_DATE),
                    timestamp = startMillis,
                    sessionStart = startMillis,
                    sessionEnd = endMillis,
                    durationSeconds = secs,
                    unlockCount = if (firstSlice) raw.unlockCount else 0,
                    wasWalking = false,
                    appUsed = raw.appUsed
                )
                firstSlice = false
            }
            cursor = nextMidnight
        }

        val startMillis = cursor.atZone(zoneId).toInstant().toEpochMilli()
        val secs = ((raw.endMs - startMillis) / 1000L).toInt()
        if (secs > 0) {
            result += ScreenTimeSession(
                date = cursor.toLocalDate().format(SafeDateTime.ISO_DATE),
                timestamp = startMillis,
                sessionStart = startMillis,
                sessionEnd = raw.endMs,
                durationSeconds = secs,
                unlockCount = if (firstSlice) raw.unlockCount else 0,
                wasWalking = false,
                appUsed = raw.appUsed
            )
        }
        return result
    }

    fun buildHourlyBuckets(
        date: String,
        sessionsForDate: List<ScreenTimeSession>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScreenTimeHourly> {
        if (sessionsForDate.isEmpty()) return emptyList()
        val secondsPerHour = IntArray(24)
        val pickupsPerHour = IntArray(24)

        sessionsForDate.forEach { session ->
            if (session.sessionEnd <= session.sessionStart) return@forEach
            val startDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(session.sessionStart), zoneId)
            val endDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(session.sessionEnd), zoneId)
            val startHour = startDt.hour.coerceIn(0, 23)
            if (session.unlockCount > 0) {
                pickupsPerHour[startHour] += 1
            }

            var cursor = startDt
            while (cursor.isBefore(endDt)) {
                val nextHour = cursor.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                val segEnd = if (endDt.isBefore(nextHour)) endDt else nextHour
                val segSeconds = ChronoUnit.SECONDS.between(cursor, segEnd).toInt().coerceAtLeast(0)
                if (segSeconds > 0 && cursor.toLocalDate().format(SafeDateTime.ISO_DATE) == date) {
                    secondsPerHour[cursor.hour.coerceIn(0, 23)] += segSeconds
                }
                cursor = segEnd
            }
        }

        return (0..23).mapNotNull { hour ->
            val secs = secondsPerHour[hour]
            val pickups = pickupsPerHour[hour]
            if (secs > 0 || pickups > 0) {
                ScreenTimeHourly(
                    date = date,
                    hour = hour,
                    screenTimeSeconds = secs,
                    pickupsCount = pickups
                )
            } else null
        }
    }

    fun buildDailySummary(
        date: String,
        sessionsForDate: List<ScreenTimeSession>
    ): ScreenTimeDailySummary? {
        if (sessionsForDate.isEmpty()) return null
        val totalScreenTime = sessionsForDate.sumOf { it.durationSeconds }
        val explicitUnlocks = sessionsForDate.sumOf { it.unlockCount }
        val pickupCount = explicitUnlocks.coerceAtLeast(sessionsForDate.size)
        val avgSession = totalScreenTime / pickupCount.coerceAtLeast(1)
        val longest = sessionsForDate.maxOfOrNull { it.durationSeconds } ?: 0
        val shortest = sessionsForDate.minOfOrNull { it.durationSeconds } ?: 0
        val firstPickup = sessionsForDate.minOfOrNull { it.sessionStart }
        val lastPickup = sessionsForDate.maxOfOrNull { it.sessionEnd }

        return ScreenTimeDailySummary(
            date = date,
            totalScreenTimeSeconds = totalScreenTime,
            pickupsCount = pickupCount,
            firstPickupTime = firstPickup,
            lastPickupTime = lastPickup,
            walkingScreenTimeSeconds = 0,
            avgSessionDurationSeconds = avgSession,
            longestSessionSeconds = longest,
            shortestSessionSeconds = shortest,
            notificationsCount = 0
        )
    }
}
