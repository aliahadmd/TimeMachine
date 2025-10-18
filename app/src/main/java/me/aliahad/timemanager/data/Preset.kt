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
    val createdAt: Long = System.currentTimeMillis()
)

