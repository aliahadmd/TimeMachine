package me.aliahad.timemanager

import androidx.compose.ui.graphics.Color
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.DailyTask
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object DailyPlannerUtils {

    fun getTodayDateString(): String = LocalDate.now().format(SafeDateTime.ISO_DATE)

    fun formatDateForDisplay(dateString: String, today: LocalDate = LocalDate.now()): String {
        val date = SafeDateTime.parseLocalDateOrNull(dateString) ?: return dateString
        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(today.plusDays(1)) -> "Tomorrow"
            date.isEqual(today.minusDays(1)) -> "Yesterday"
            else -> date.format(SafeDateTime.DISPLAY_DATE_WEEKDAY)
        }
    }

    fun formatTimeForDisplay(timeString: String): String {
        val time = SafeDateTime.parseLocalTimeOrNull(timeString) ?: return timeString
        return time.format(SafeDateTime.DISPLAY_TIME)
    }

    fun calculateDuration(startTime: String, endTime: String): Int {
        val start = SafeDateTime.parseLocalTimeOrNull(startTime) ?: return 0
        val end = SafeDateTime.parseLocalTimeOrNull(endTime) ?: return 0
        val duration = ChronoUnit.MINUTES.between(start, end).toInt()
        return if (duration <= 0) duration + (24 * 60) else duration
    }

    fun getDurationText(minutes: Int): String = Formatters.formatMinutes(minutes)

    fun getTaskTypeIcon(type: String): String {
        return when (type) {
            "TASK" -> "📝"
            "EVENT" -> "📅"
            "BREAK" -> "☕"
            "FOCUS" -> "🎯"
            "ROUTINE" -> "🔄"
            else -> "📋"
        }
    }

    fun getTaskTypeColor(type: String): Color {
        return when (type) {
            "TASK" -> Color(0xFF4DABF7)
            "EVENT" -> Color(0xFF51CF66)
            "BREAK" -> Color(0xFFFFD93D)
            "FOCUS" -> Color(0xFFFF6B6B)
            "ROUTINE" -> Color(0xFF9775FA)
            else -> Color(0xFF868E96)
        }
    }

    fun getCategoryIcon(category: String): String {
        return when (category) {
            "Work" -> "💼"
            "Personal" -> "🏠"
            "Health" -> "💪"
            "Learning" -> "📚"
            "Social" -> "👥"
            "Finance" -> "💰"
            "Shopping" -> "🛒"
            "Travel" -> "✈️"
            else -> "📋"
        }
    }

    fun getPriorityColor(priority: String): Color {
        return when (priority.lowercase()) {
            "high" -> Color(0xFFFF6B6B)
            "medium" -> Color(0xFFFFB74D)
            "low" -> Color(0xFF51CF66)
            else -> Color(0xFF868E96)
        }
    }

    fun getTimeSlots(): List<String> = (0..23).map { Formatters.formatHourLabel(it) }

    fun isTimeInSlot(taskTime: String, slotTime: String): Boolean {
        val task = SafeDateTime.parseLocalTimeOrNull(taskTime) ?: return false
        val slot = SafeDateTime.parseLocalTimeOrNull(slotTime) ?: return false
        return task.hour == slot.hour
    }

    fun calculateProgress(tasks: List<DailyTask>): Float {
        if (tasks.isEmpty()) return 0f
        val completed = tasks.count { it.isCompleted }
        return completed.toFloat() / tasks.size.toFloat()
    }

    fun getCompletionText(completed: Int, total: Int): String = "$completed of $total completed"

    private fun toMinuteSegments(startTime: String, endTime: String): List<IntRange> {
        val start = SafeDateTime.parseLocalTimeOrNull(startTime) ?: return emptyList()
        val end = SafeDateTime.parseLocalTimeOrNull(endTime) ?: return emptyList()
        val startMin = start.hour * 60 + start.minute
        val endMin = end.hour * 60 + end.minute
        return when {
            endMin > startMin -> listOf(startMin until endMin)
            endMin < startMin -> listOf(startMin until 1440, 0 until endMin) // Overnight wrap
            else -> emptyList()
        }
    }

    private fun rangesOverlap(a: IntRange, b: IntRange): Boolean {
        return a.first <= b.last && b.first <= a.last
    }

    fun isTaskOverlapping(task1: DailyTask, task2: DailyTask): Boolean {
        val segs1 = toMinuteSegments(task1.startTime, task1.endTime)
        val segs2 = toMinuteSegments(task2.startTime, task2.endTime)
        return segs1.any { s1 -> segs2.any { s2 -> rangesOverlap(s1, s2) } }
    }

    fun getCurrentTimeSlot(now: LocalTime = LocalTime.now()): String = Formatters.formatHourLabel(now.hour)

    fun isTaskNow(
        task: DailyTask,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now()
    ): Boolean {
        val todayStr = today.format(SafeDateTime.ISO_DATE)
        val taskDate = SafeDateTime.parseLocalDateOrNull(task.date) ?: return false
        val start = SafeDateTime.parseLocalTimeOrNull(task.startTime) ?: return false
        val end = SafeDateTime.parseLocalTimeOrNull(task.endTime) ?: return false
        val nowMin = now.hour * 60 + now.minute
        val startMin = start.hour * 60 + start.minute
        val endMin = end.hour * 60 + end.minute

        return if (endMin >= startMin) {
            task.date == todayStr && nowMin in startMin until endMin
        } else {
            // Overnight: active from startTime on task.date until 23:59, OR 00:00..endTime on next day
            (task.date == todayStr && nowMin >= startMin) ||
                (taskDate.plusDays(1) == today && nowMin < endMin)
        }
    }

    fun isTaskUpcoming(
        task: DailyTask,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now()
    ): Boolean {
        val todayStr = today.format(SafeDateTime.ISO_DATE)
        if (task.date != todayStr) return false
        val start = SafeDateTime.parseLocalTimeOrNull(task.startTime) ?: return false
        val diff = ChronoUnit.MINUTES.between(now, start)
        return diff in 1..30
    }

    fun isValidTimeFormat(time: String): Boolean = SafeDateTime.parseLocalTimeOrNull(time) != null

    fun isEndTimeValid(startTime: String, endTime: String, allowOvernight: Boolean = true): Boolean {
        val start = SafeDateTime.parseLocalTimeOrNull(startTime) ?: return false
        val end = SafeDateTime.parseLocalTimeOrNull(endTime) ?: return false
        return if (allowOvernight) start != end else end.isAfter(start)
    }

    fun isEndTimeAfterStart(startTime: String, endTime: String): Boolean {
        return isEndTimeValid(startTime, endTime, allowOvernight = true)
    }

    fun matchesRecurringDay(recurringDays: String, date: LocalDate): Boolean {
        if (recurringDays.isBlank()) return true
        val tokens = recurringDays.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val dayNum = date.dayOfWeek.value.toString()
        val shortName = date.dayOfWeek.name.take(3).lowercase()
        return tokens.any { it == dayNum || it == shortName }
    }

    fun getOnboardingTasks(date: String): List<DailyTask> {
        return listOf(
            DailyTask(
                title = "Morning Calm",
                description = "Start your day with a brief moment of calm",
                date = date,
                startTime = "08:00",
                endTime = "08:15",
                taskType = "ROUTINE",
                category = "Personal",
                icon = "🌅",
                color = 0xFF9775FAL,
                priority = "Low"
            ),
            DailyTask(
                title = "Focus Block",
                description = "Tap Start Focus to run a timed work sprint",
                date = date,
                startTime = "10:00",
                endTime = "11:00",
                taskType = "FOCUS",
                category = "Work",
                icon = "🎯",
                color = 0xFFFF6B6BL,
                priority = "High"
            ),
            DailyTask(
                title = "Quick Break",
                description = "Step away from screens and stretch",
                date = date,
                startTime = "13:00",
                endTime = "13:30",
                taskType = "BREAK",
                category = "Health",
                icon = "☕",
                color = 0xFFFFD93DL,
                priority = "Medium"
            ),
            DailyTask(
                title = "Evening Review",
                description = "Wrap up tasks and check your habit streak",
                date = date,
                startTime = "20:00",
                endTime = "20:20",
                taskType = "ROUTINE",
                category = "Personal",
                icon = "🌙",
                color = 0xFF4DABF7L,
                priority = "Low"
            )
        )
    }
}
