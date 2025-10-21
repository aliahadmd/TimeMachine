package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_categories")
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String, // Icon identifier
    val color: Long, // Color as Long (from Color.value)
    val budget: Double = 0.0, // Monthly budget for this category
    val isActive: Boolean = true
)

