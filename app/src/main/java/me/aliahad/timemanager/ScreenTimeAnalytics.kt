package me.aliahad.timemanager

import me.aliahad.timemanager.data.ScreenTimeDailySummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ScreenTimeAnalytics {
    
    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
    
    fun formatShortDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
    
    fun formatTime(timestamp: Long): String {
        return try {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            )
            dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e: Exception) {
            "--"
        }
    }
    
    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        
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
        val percentage = if (goalSeconds > 0) (seconds.toFloat() / goalSeconds) * 100 else 0f
        
        return when {
            percentage < 50 -> UsageStatus.EXCELLENT
            percentage < 75 -> UsageStatus.GOOD
            percentage < 100 -> UsageStatus.MODERATE
            else -> UsageStatus.HIGH
        }
    }
    
    enum class UsageStatus(val label: String, val color: Long) {
        EXCELLENT("Excellent", 0xFF4CAF50),
        GOOD("Good", 0xFF8BC34A),
        MODERATE("Moderate", 0xFFFF9800),
        HIGH("High", 0xFFF44336)
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
        return try {
            val localDate = java.time.LocalDate.parse(date)
            localDate.dayOfWeek.toString().substring(0, 3).lowercase()
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }
}

