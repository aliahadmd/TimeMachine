package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["date"]),
        Index(value = ["date", "isCompleted"]),
        Index(value = ["isRecurring"])
    ]
)
data class DailyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: String, // yyyy-MM-dd
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val taskType: String, // TASK, EVENT, BREAK, FOCUS, ROUTINE
    val category: String = "General",
    val priority: String = "Medium", // Low, Medium, High
    val icon: String = "📝",
    val color: Long = 0xFF4DABF7L, // Canonical 32-bit ARGB stored in Long
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringDays: String = "", // Comma-separated: Mon,Tue,Wed or 1..7
    val reminderMinutes: Int = 0, // Minutes before startTime to remind (0 = none)
    val notes: String = "",
    val linkedCategoryId: Long? = null,
    val linkedSessionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
