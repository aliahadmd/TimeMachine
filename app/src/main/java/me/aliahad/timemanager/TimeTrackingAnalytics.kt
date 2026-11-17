package me.aliahad.timemanager

import androidx.compose.ui.graphics.Color
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Calculate strict consecutive streaks from active date strings ("yyyy-MM-dd").
 * Ignores malformed/future dates and deduplicates dates before computing streaks.
 */
fun calculateStreaks(
    activeDates: List<String>,
    totalDays: Int,
    today: LocalDate = LocalDate.now()
): StreakInfo {
    val parsedDates = activeDates
        .mapNotNull { SafeDateTime.parseLocalDateOrNull(it) }
        .filter { !it.isAfter(today) }
        .distinct()
        .sortedDescending()

    if (parsedDates.isEmpty()) {
        return StreakInfo(0, 0, 0, 0f)
    }

    val dateSet = parsedDates.toSet()

    // Anchor streak at today if active today, otherwise yesterday so active streaks stay alive
    var currentStreak = 0
    var cursor = if (dateSet.contains(today)) today else today.minusDays(1)
    while (dateSet.contains(cursor)) {
        currentStreak++
        cursor = cursor.minusDays(1)
    }

    // Calculate longest consecutive run across distinct sorted dates
    var longestStreak = 1
    var tempStreak = 1
    for (i in 0 until parsedDates.size - 1) {
        val diff = ChronoUnit.DAYS.between(parsedDates[i + 1], parsedDates[i])
        if (diff == 1L) {
            tempStreak++
            if (tempStreak > longestStreak) longestStreak = tempStreak
        } else {
            tempStreak = 1
        }
    }

    val totalActiveDays = parsedDates.size
    val consistencyPercentage = if (totalDays > 0) {
        ((totalActiveDays.toFloat() / totalDays) * 100f).coerceIn(0f, 100f)
    } else 0f

    return StreakInfo(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalActiveDays = totalActiveDays,
        consistencyPercentage = consistencyPercentage
    )
}

fun getDateRange(days: Int, today: LocalDate = LocalDate.now()): Pair<String, String> {
    val startDate = today.minusDays((days - 1).coerceAtLeast(0).toLong())
    return startDate.format(SafeDateTime.ISO_DATE) to today.format(SafeDateTime.ISO_DATE)
}

fun formatDuration(minutes: Int): String = Formatters.formatMinutes(minutes)

fun minutesToHours(minutes: Int): Float = minutes / 60f

fun getTodayDateString(): String = LocalDate.now().format(SafeDateTime.ISO_DATE)

fun String.toLocalDate(): LocalDate = SafeDateTime.parseLocalDateOrNull(this) ?: LocalDate.now()

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

data class HeatmapDay(
    val date: LocalDate,
    val minutes: Int,
    val intensity: Float
)

fun generateHeatmapData(
    dailyStats: Map<String, Int>,
    startDate: LocalDate,
    endDate: LocalDate,
    maxMinutes: Int = 480
): List<HeatmapDay> {
    val data = mutableListOf<HeatmapDay>()
    var current = startDate
    val denom = maxMinutes.coerceAtLeast(1)

    while (!current.isAfter(endDate)) {
        val dateString = current.format(SafeDateTime.ISO_DATE)
        val minutes = dailyStats[dateString] ?: 0
        val intensity = if (minutes > 0) {
            (minutes.toFloat() / denom).coerceIn(0.1f, 1.0f)
        } else 0f

        data.add(HeatmapDay(current, minutes, intensity))
        current = current.plusDays(1)
    }

    return data
}

fun groupByWeek(dailyStats: List<DailyStatsRaw>): List<WeeklyActivity> {
    if (dailyStats.isEmpty()) return emptyList()

    val grouped = dailyStats.mapNotNull { raw ->
        val date = SafeDateTime.parseLocalDateOrNull(raw.date) ?: return@mapNotNull null
        date to raw
    }.groupBy { (date, _) ->
        date.minusDays(date.dayOfWeek.value.toLong() - 1)
    }

    return grouped.map { (weekStart, entries) ->
        val days = entries.map { it.second }
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
            weekStartDate = weekStart.format(SafeDateTime.ISO_DATE),
            totalMinutes = totalMinutes,
            dailyBreakdown = dailyBreakdown
        )
    }.sortedByDescending { it.weekStartDate }
}

fun calculateGoalProgress(actualMinutes: Int, goalMinutes: Int): Float {
    if (goalMinutes <= 0) return 0f
    return ((actualMinutes.toFloat() / goalMinutes) * 100f).coerceIn(0f, 100f)
}

fun getIntensityColor(intensity: Float, baseColor: Color): Color {
    return when {
        intensity <= 0f -> Color.LightGray.copy(alpha = 0.1f)
        intensity < 0.25f -> baseColor.copy(alpha = 0.3f)
        intensity < 0.50f -> baseColor.copy(alpha = 0.5f)
        intensity < 0.75f -> baseColor.copy(alpha = 0.7f)
        else -> baseColor.copy(alpha = 0.9f)
    }
}

fun LocalDate.toDisplayString(): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return when (this) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> this.format(SafeDateTime.DISPLAY_DATE_FULL)
    }
}

fun calculateProductivityScore(
    totalMinutes: Int,
    goalMinutes: Int,
    consistency: Float,
    streakDays: Int
): Int {
    val goalAchievement = if (goalMinutes > 0) {
        (totalMinutes.toFloat() / goalMinutes * 40f).coerceIn(0f, 40f)
    } else 20f

    val consistencyScore = (consistency * 0.3f).coerceIn(0f, 30f)
    val streakBonus = (streakDays.coerceIn(0, 30) * 1f).coerceIn(0f, 30f)

    return (goalAchievement + consistencyScore + streakBonus).roundToInt()
}
