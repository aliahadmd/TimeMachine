package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val hours: Int,
    val minutes: Int,
    val breakMinutes: Int = 5,
    val isPomodoro: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalFocusMinutes: Int
        get() = (hours * 60) + minutes
}
