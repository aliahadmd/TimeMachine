package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "date_calculations")
data class DateCalculation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val startDate: Long, // timestamp in milliseconds
    val endDate: Long, // timestamp in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Predefined categories
object DateCategories {
    const val BIRTHDAY = "Birthday"
    const val ANNIVERSARY = "Anniversary"
    const val RELATIONSHIP = "Relationship"
    const val WORK = "Work Anniversary"
    const val MEMORIAL = "Memorial"
    const val CUSTOM = "Custom"
    
    val DEFAULT_CATEGORIES = listOf(
        BIRTHDAY,
        ANNIVERSARY,
        RELATIONSHIP,
        WORK,
        MEMORIAL,
        CUSTOM
    )
}

