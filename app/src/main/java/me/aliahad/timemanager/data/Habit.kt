package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Long, // Store as Long (color.value)
    val iconName: String = "fitness_center", // Material icon name
    val type: HabitType,
    val goalPeriodDays: Int, // Total days for the goal
    val isEveryday: Boolean,
    val reminderTimeHour: Int? = null, // 0-23, null if no reminder
    val reminderTimeMinute: Int? = null, // 0-59
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class HabitType {
    BUILD,  // Build a good habit
    QUIT    // Quit a bad habit
}

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"]
)
data class HabitCompletion(
    val habitId: Long,
    val date: String, // Format: YYYY-MM-DD
    val completionType: CompletionType = CompletionType.ACHIEVED, // New field
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class CompletionType {
    ACHIEVED,  // Successfully completed/achieved goal (Green)
    GAVE_UP    // Gave up / failed (Red)
}

// Data class for habit with completion stats
data class HabitWithStats(
    val habit: Habit,
    val totalCompletions: Int,
    val totalAchieved: Int,
    val totalGaveUp: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float, // 0.0 to 1.0
    val successRate: Float, // achieved / (achieved + gave up)
    val isCompletedToday: Boolean
)

