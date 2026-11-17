package me.aliahad.timemanager

import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.ScreenTimeDailySummary
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ScreenTimeAnalytics {

    fun formatDuration(seconds: Int): String = Formatters.formatSeconds(seconds)

    fun formatShortDuration(seconds: Int): String = Formatters.formatShortSeconds(seconds)

    fun formatTime(timestamp: Long): String {
        return try {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            )
            dateTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        } catch (_: Exception) {
            "--"
        }
    }

    fun formatTimeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - timestamp).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> formatTime(timestamp)
        }
    }

    fun calculateAverage(summaries: List<ScreenTimeDailySummary>): Int {
        if (summaries.isEmpty()) return 0
        return summaries.map { it.totalScreenTimeSeconds }.average().toInt()
    }

    fun calculateTotal(summaries: List<ScreenTimeDailySummary>): Int {
        return summaries.sumOf { it.totalScreenTimeSeconds }
    }

    fun calculateTotalPickups(summaries: List<ScreenTimeDailySummary>): Int {
        return summaries.sumOf { it.pickupsCount }
    }

    fun getUsageStatus(seconds: Int, goalSeconds: Int = 14400): UsageStatus {
        val percentage = if (goalSeconds > 0) (seconds.toFloat() / goalSeconds) * 100f else 0f
        return when {
            percentage < 50f -> UsageStatus.EXCELLENT
            percentage < 75f -> UsageStatus.GOOD
            percentage < 100f -> UsageStatus.MODERATE
            else -> UsageStatus.HIGH
        }
    }

    enum class UsageStatus(val label: String, val color: Long) {
        EXCELLENT("Excellent", 0xFF4CAF50L),
        GOOD("Good", 0xFF8BC34AL),
        MODERATE("Moderate", 0xFFFF9800L),
        HIGH("High", 0xFFF44336L)
    }

    fun getPickupFrequency(pickups: Int): String {
        return when {
            pickups < 20 -> "Light user"
            pickups < 50 -> "Moderate user"
            pickups < 100 -> "Active user"
            else -> "Heavy user"
        }
    }

    fun getDayOfWeekLabel(date: String): String {
        val localDate = SafeDateTime.parseLocalDateOrNull(date) ?: return date
        return localDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    }
}
