package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "screen_time_sessions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["timestamp"]),
        Index(value = ["sessionStart"], unique = true)
    ]
)
data class ScreenTimeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val timestamp: Long, // When screen turned on
    val sessionStart: Long, // Screen on time
    val sessionEnd: Long, // Screen off time
    val durationSeconds: Int, // Total screen on duration
    val unlockCount: Int = 1, // Number of unlocks in this session
    val wasWalking: Boolean = false, // Detected motion during session
    val appUsed: String? = null // Top app used in this session
)

@Entity(
    tableName = "screen_time_daily",
    indices = [Index(value = ["date"], unique = true)]
)
data class ScreenTimeDailySummary(
    @PrimaryKey
    val date: String, // yyyy-MM-dd
    val totalScreenTimeSeconds: Int,
    val pickupsCount: Int,
    val firstPickupTime: Long?, // First screen on of the day
    val lastPickupTime: Long?, // Last screen on of the day
    val walkingScreenTimeSeconds: Int, // Time used while walking
    val avgSessionDurationSeconds: Int,
    val longestSessionSeconds: Int,
    val shortestSessionSeconds: Int,
    val notificationsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

// Hourly breakdown for analytics
@Entity(
    tableName = "screen_time_hourly",
    indices = [Index(value = ["date", "hour"], unique = true)]
)
data class ScreenTimeHourly(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val hour: Int, // 0-23
    val screenTimeSeconds: Int,
    val pickupsCount: Int
)
