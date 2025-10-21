package me.aliahad.timemanager

import androidx.compose.ui.graphics.Color
import me.aliahad.timemanager.data.DailyTask
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DailyPlannerUtils {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    
    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }
    
    fun formatDateForDisplay(dateString: String): String {
        val date = LocalDate.parse(dateString, dateFormatter)
        val today = LocalDate.now()
        
        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(today.plusDays(1)) -> "Tomorrow"
            date.isEqual(today.minusDays(1)) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }
    }
    
    fun formatTimeForDisplay(timeString: String): String {
        val time = LocalTime.parse(timeString, timeFormatter)
        return time.format(displayTimeFormatter)
    }
    
    fun calculateDuration(startTime: String, endTime: String): Int {
        val start = LocalTime.parse(startTime, timeFormatter)
        val end = LocalTime.parse(endTime, timeFormatter)
        val duration = ChronoUnit.MINUTES.between(start, end).toInt()
        // Handle negative duration (end before start or overnight tasks)
        return if (duration < 0) duration + (24 * 60) else duration
    }
    
    fun getDurationText(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
    
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
        return when (priority) {
            "High" -> Color(0xFFFF6B6B)
            "Medium" -> Color(0xFFFFD93D)
            "Low" -> Color(0xFF51CF66)
            else -> Color(0xFF868E96)
        }
    }
    
    fun getTimeSlots(): List<String> {
        return (0..23).map { hour ->
            String.format("%02d:00", hour)
        }
    }
    
    fun isTimeInSlot(taskTime: String, slotTime: String): Boolean {
        val task = LocalTime.parse(taskTime, timeFormatter)
        val slot = LocalTime.parse(slotTime, timeFormatter)
        return task.hour == slot.hour
    }
    
    fun calculateProgress(tasks: List<DailyTask>): Float {
        if (tasks.isEmpty()) return 0f
        val completed = tasks.count { it.isCompleted }
        return completed.toFloat() / tasks.size.toFloat()
    }
    
    fun getCompletionText(completed: Int, total: Int): String {
        return "$completed of $total completed"
    }
    
    fun isTaskOverlapping(task1: DailyTask, task2: DailyTask): Boolean {
        val start1 = LocalTime.parse(task1.startTime, timeFormatter)
        val end1 = LocalTime.parse(task1.endTime, timeFormatter)
        val start2 = LocalTime.parse(task2.startTime, timeFormatter)
        val end2 = LocalTime.parse(task2.endTime, timeFormatter)
        
        return start1.isBefore(end2) && start2.isBefore(end1)
    }
    
    fun getCurrentTimeSlot(): String {
        val now = LocalTime.now()
        return String.format("%02d:00", now.hour)
    }
    
    fun isTaskNow(task: DailyTask): Boolean {
        // Only check if task is today
        val today = LocalDate.now().format(dateFormatter)
        if (task.date != today) return false
        
        val now = LocalTime.now()
        val start = LocalTime.parse(task.startTime, timeFormatter)
        val end = LocalTime.parse(task.endTime, timeFormatter)
        return !now.isBefore(start) && now.isBefore(end)
    }
    
    fun isTaskUpcoming(task: DailyTask): Boolean {
        // Only check if task is today
        val today = LocalDate.now().format(dateFormatter)
        if (task.date != today) return false
        
        val now = LocalTime.now()
        val start = LocalTime.parse(task.startTime, timeFormatter)
        val diff = ChronoUnit.MINUTES.between(now, start)
        return diff in 1..30 // Within next 30 minutes
    }
    
    // Validate time format
    fun isValidTimeFormat(time: String): Boolean {
        return try {
            LocalTime.parse(time, timeFormatter)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Check if end time is after start time
    fun isEndTimeAfterStart(startTime: String, endTime: String): Boolean {
        return try {
            val start = LocalTime.parse(startTime, timeFormatter)
            val end = LocalTime.parse(endTime, timeFormatter)
            end.isAfter(start)
        } catch (e: Exception) {
            false
        }
    }
    
    // Default onboarding tasks
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
                color = 0xFF9775FA,
                priority = "Low"
            ),
            DailyTask(
                title = "Get Started",
                description = "Welcome to Daily Planner! Let's set up your day",
                date = date,
                startTime = "09:00",
                endTime = "09:30",
                taskType = "TASK",
                category = "Personal",
                icon = "🚀",
                color = 0xFF4DABF7,
                priority = "High"
            ),
            DailyTask(
                title = "Add Your First Task",
                description = "Tap + to create your own tasks and events",
                date = date,
                startTime = "10:00",
                endTime = "10:30",
                taskType = "TASK",
                category = "Personal",
                icon = "✨",
                color = 0xFF51CF66,
                priority = "High"
            ),
            DailyTask(
                title = "Focus Block",
                description = "Deep work session - your most important task",
                date = date,
                startTime = "11:00",
                endTime = "12:00",
                taskType = "FOCUS",
                category = "Work",
                icon = "🎯",
                color = 0xFFFF6B6B,
                priority = "High"
            ),
            DailyTask(
                title = "Lunch Break",
                description = "Take a proper break. You deserve it!",
                date = date,
                startTime = "13:00",
                endTime = "14:00",
                taskType = "BREAK",
                category = "Personal",
                icon = "🍽️",
                color = 0xFFFFD93D,
                priority = "Medium"
            ),
            DailyTask(
                title = "Afternoon Tasks",
                description = "Tackle your to-do list",
                date = date,
                startTime = "15:00",
                endTime = "16:00",
                taskType = "TASK",
                category = "Work",
                icon = "📋",
                color = 0xFF4DABF7,
                priority = "Medium"
            ),
            DailyTask(
                title = "Quick Stretch",
                description = "Brief 15m break. Move your body!",
                date = date,
                startTime = "16:30",
                endTime = "16:45",
                taskType = "BREAK",
                category = "Health",
                icon = "🧘",
                color = 0xFFFFD93D,
                priority = "Low"
            ),
            DailyTask(
                title = "Wind Down",
                description = "Reflect on your day and prepare for tomorrow",
                date = date,
                startTime = "20:00",
                endTime = "20:30",
                taskType = "ROUTINE",
                category = "Personal",
                icon = "🌙",
                color = 0xFF9775FA,
                priority = "Low"
            )
        )
    }
}

