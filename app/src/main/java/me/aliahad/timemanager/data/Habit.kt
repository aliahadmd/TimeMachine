package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Long, // Canonical 32-bit ARGB stored in Long
    val iconName: String = "fitness_center",
    val type: HabitType,
    val goalPeriodDays: Int,
    val isEveryday: Boolean,
    val scheduledDays: String = "1,2,3,4,5,6,7", // ISO DayOfWeek (1=Mon .. 7=Sun)
    val reminderTimeHour: Int? = null, // 0-23, null if no reminder
    val reminderTimeMinute: Int? = null, // 0-59
    val linkedFocusCategoryId: Long? = null,
    val linkType: HabitLinkType = HabitLinkType.MANUAL,
    val linkedTargetValue: Int = 0, // Target minutes for focus link or max screen time minutes
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class HabitType {
    BUILD,
    QUIT
}

enum class HabitLinkType {
    MANUAL,
    FOCUS_MINUTES,
    SCREEN_TIME_UNDER_LIMIT
}

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["date"])
    ]
)
data class HabitCompletion(
    val habitId: Long,
    val date: String, // Format: YYYY-MM-DD
    val completionType: CompletionType = CompletionType.ACHIEVED,
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class CompletionType {
    ACHIEVED,
    GAVE_UP
}

data class HabitWithStats(
    val habit: Habit,
    val totalCompletions: Int,
    val totalAchieved: Int,
    val totalGaveUp: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float,
    val successRate: Float,
    val goalProgress: Float,
    val isCompletedToday: Boolean,
    val todayCompletion: HabitCompletion? = null
)
