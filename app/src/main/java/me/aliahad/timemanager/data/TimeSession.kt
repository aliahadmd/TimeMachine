package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ActivityCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("date")]
)
data class TimeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val startTime: Long, // Unix timestamp in milliseconds
    val endTime: Long, // Unix timestamp in milliseconds
    val durationMinutes: Int, // Calculated duration in minutes
    val date: String, // Format: "YYYY-MM-DD" for easy querying
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Data class for aggregated statistics
 */
data class DailyStats(
    val date: String,
    val totalMinutes: Int,
    val sessionCount: Int,
    val categoriesUsed: Int
)

data class CategoryStats(
    val category: ActivityCategory,
    val totalMinutes: Int,
    val sessionCount: Int,
    val averageSessionMinutes: Float,
    val lastSessionDate: String?
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalActiveDays: Int,
    val consistencyPercentage: Float // Active days / Total days * 100
)

data class WeeklyActivity(
    val weekStartDate: String,
    val totalMinutes: Int,
    val dailyBreakdown: List<DailyStats>
)

