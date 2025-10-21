package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["date"]),  // Frequently queried by date
        Index(value = ["date", "isCompleted"]),  // Composite index for filtering
        Index(value = ["isRecurring"])  // For recurring task queries
    ]
)
data class DailyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: String, // yyyy-MM-dd
    val startTime: String, // HH:mm (24-hour format)
    val endTime: String, // HH:mm
    val taskType: String, // TASK, EVENT, BREAK, FOCUS, ROUTINE
    val category: String = "General", // Work, Personal, Health, Learning, etc.
    val priority: String = "Medium", // Low, Medium, High
    val icon: String = "📝",
    val color: Long = 0xFF4DABF7,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringDays: String = "", // Comma-separated: Mon,Tue,Wed
    val reminderMinutes: Int = 0, // Minutes before to remind (0 = no reminder)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

