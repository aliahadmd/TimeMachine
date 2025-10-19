package me.aliahad.timemanager

import me.aliahad.timemanager.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Utility functions for time tracking analytics and statistics
 */

/**
 * Calculate current and longest streaks
 */
fun calculateStreaks(activeDates: List<String>, totalDays: Int): StreakInfo {
    if (activeDates.isEmpty()) {
        return StreakInfo(0, 0, 0, 0f)
    }
    
    val dates = activeDates.map { LocalDate.parse(it) }.sortedDescending()
    val today = LocalDate.now()
    
    // Calculate current streak
    var currentStreak = 0
    var checkDate = today
    
    for (date in dates) {
        if (date.isEqual(checkDate) || date.isEqual(checkDate.minusDays(1))) {
            currentStreak++
            checkDate = date.minusDays(1)
        } else {
            break
        }
    }
    
    // Calculate longest streak
    var longestStreak = 0
    var tempStreak = 1
    
    for (i in 0 until dates.size - 1) {
        val daysDiff = ChronoUnit.DAYS.between(dates[i + 1], dates[i])
        if (daysDiff == 1L) {
            tempStreak++
        } else {
            longestStreak = maxOf(longestStreak, tempStreak)
            tempStreak = 1
        }
    }
    longestStreak = maxOf(longestStreak, tempStreak)
    
    // Calculate consistency
    val totalActiveDays = activeDates.size
    val consistencyPercentage = if (totalDays > 0) {
        (totalActiveDays.toFloat() / totalDays * 100)
    } else 0f
    
    return StreakInfo(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalActiveDays = totalActiveDays,
        consistencyPercentage = consistencyPercentage
    )
}

/**
 * Get date range for the last N days
 */
fun getDateRange(days: Int): Pair<String, String> {
    val endDate = LocalDate.now()
    val startDate = endDate.minusDays(days.toLong() - 1)
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    return Pair(startDate.format(formatter), endDate.format(formatter))
}

/**
 * Format minutes to human-readable time
 */
fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}

/**
 * Format minutes to decimal hours
 */
fun minutesToHours(minutes: Int): Float {
    return minutes / 60f
}

/**
 * Get today's date as string
 */
fun getTodayDateString(): String {
    return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
}

/**
 * Parse date string to LocalDate
 */
fun String.toLocalDate(): LocalDate {
    return LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
}

/**
 * Calculate category statistics
 */
suspend fun calculateCategoryStats(
    category: ActivityCategory,
    timeSessionDao: TimeSessionDao
): CategoryStats {
    val totalMinutes = timeSessionDao.getTotalMinutesForCategory(category.id) ?: 0
    val sessionCount = timeSessionDao.getSessionCountForCategory(category.id)
    val averageMinutes = timeSessionDao.getAverageSessionMinutesForCategory(category.id) ?: 0f
    val lastDate = timeSessionDao.getLastSessionDateForCategory(category.id)
    
    return CategoryStats(
        category = category,
        totalMinutes = totalMinutes,
        sessionCount = sessionCount,
        averageSessionMinutes = averageMinutes,
        lastSessionDate = lastDate
    )
}

/**
 * Get heatmap data for calendar visualization
 */
data class HeatmapDay(
    val date: LocalDate,
    val minutes: Int,
    val intensity: Float // 0.0 to 1.0
)

fun generateHeatmapData(
    dailyStats: Map<String, Int>,
    startDate: LocalDate,
    endDate: LocalDate,
    maxMinutes: Int = 480 // 8 hours as max intensity
): List<HeatmapDay> {
    val data = mutableListOf<HeatmapDay>()
    var current = startDate
    
    while (!current.isAfter(endDate)) {
        val dateString = current.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val minutes = dailyStats[dateString] ?: 0
        val intensity = if (minutes > 0) {
            (minutes.toFloat() / maxMinutes).coerceIn(0.1f, 1.0f)
        } else 0f
        
        data.add(HeatmapDay(current, minutes, intensity))
        current = current.plusDays(1)
    }
    
    return data
}

/**
 * Calculate weekly summary
 */
fun groupByWeek(dailyStats: List<DailyStatsRaw>): List<WeeklyActivity> {
    if (dailyStats.isEmpty()) return emptyList()
    
    val grouped = dailyStats.groupBy { dateString ->
        val date = LocalDate.parse(dateString.date)
        // Get Monday of the week
        date.minusDays(date.dayOfWeek.value.toLong() - 1)
    }
    
    return grouped.map { (weekStart, days) ->
        val totalMinutes = days.sumOf { it.totalMinutes }
        val dailyBreakdown = days.map { raw ->
            DailyStats(
                date = raw.date,
                totalMinutes = raw.totalMinutes,
                sessionCount = raw.sessionCount,
                categoriesUsed = raw.categoriesUsed
            )
        }
        
        WeeklyActivity(
            weekStartDate = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
            totalMinutes = totalMinutes,
            dailyBreakdown = dailyBreakdown
        )
    }.sortedByDescending { it.weekStartDate }
}

/**
 * Calculate goal progress percentage
 */
fun calculateGoalProgress(actualMinutes: Int, goalMinutes: Int): Float {
    if (goalMinutes <= 0) return 0f
    return ((actualMinutes.toFloat() / goalMinutes) * 100).coerceIn(0f, 100f)
}

/**
 * Get intensity color for heatmap
 */
fun getIntensityColor(intensity: Float, baseColor: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    return when {
        intensity == 0f -> androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.1f)
        intensity < 0.25f -> baseColor.copy(alpha = 0.3f)
        intensity < 0.50f -> baseColor.copy(alpha = 0.5f)
        intensity < 0.75f -> baseColor.copy(alpha = 0.7f)
        else -> baseColor.copy(alpha = 0.9f)
    }
}

/**
 * Format date for display
 */
fun LocalDate.toDisplayString(): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    
    return when (this) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> this.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

/**
 * Calculate productivity score (0-100)
 */
fun calculateProductivityScore(
    totalMinutes: Int,
    goalMinutes: Int,
    consistency: Float,
    streakDays: Int
): Int {
    val goalAchievement = if (goalMinutes > 0) {
        (totalMinutes.toFloat() / goalMinutes * 40).coerceIn(0f, 40f)
    } else 20f
    
    val consistencyScore = (consistency * 0.3f).coerceIn(0f, 30f)
    val streakBonus = (streakDays.coerceIn(0, 30) * 1f).coerceIn(0f, 30f)
    
    return (goalAchievement + consistencyScore + streakBonus).roundToInt()
}

