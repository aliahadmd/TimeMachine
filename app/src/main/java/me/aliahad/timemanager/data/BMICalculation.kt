package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bmi_calculations")
data class BMICalculation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "My BMI",
    val age: Int,
    val heightCm: Float,
    val weightKg: Float,
    val gender: Gender,
    val classification: BMIClassification,
    val bmiValue: Float,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class BMIClassification(val displayName: String) {
    WHO("WHO (World Health Organization)"),
    DGE("DGE (German Nutrition Society)")
}

// WHO BMI Categories (Standard International)
enum class WHOBMICategory(
    val displayName: String,
    val minBMI: Float,
    val maxBMI: Float,
    val color: Long
) {
    SEVERE_UNDERWEIGHT("Severe Underweight", 0f, 16f, 0xFF1565C0),
    MODERATE_UNDERWEIGHT("Moderate Underweight", 16f, 17f, 0xFF1976D2),
    MILD_UNDERWEIGHT("Mild Underweight", 17f, 18.5f, 0xFF42A5F5),
    NORMAL("Normal", 18.5f, 25f, 0xFF4CAF50),
    OVERWEIGHT("Overweight", 25f, 30f, 0xFFFFA726),
    OBESE_CLASS_I("Obese Class I", 30f, 35f, 0xFFFF7043),
    OBESE_CLASS_II("Obese Class II", 35f, 40f, 0xFFE53935),
    OBESE_CLASS_III("Obese Class III", 40f, Float.MAX_VALUE, 0xFFB71C1C);
    
    companion object {
        fun fromBMI(bmi: Float): WHOBMICategory {
            return values().find { bmi >= it.minBMI && bmi < it.maxBMI } ?: OBESE_CLASS_III
        }
    }
}

// DGE BMI Categories (Age and Gender adjusted)
data class DGEBMIRange(
    val minBMI: Float,
    val maxBMI: Float,
    val category: String
)

object DGEClassification {
    fun getCategory(bmi: Float, age: Int, gender: Gender): String {
        val range = getIdealRange(age, gender)
        return when {
            bmi < range.minBMI -> "Underweight"
            bmi <= range.maxBMI -> "Normal"
            bmi <= range.maxBMI + 5 -> "Overweight"
            else -> "Obese"
        }
    }
    
    fun getIdealRange(age: Int, gender: Gender): DGEBMIRange {
        return when {
            age < 25 -> DGEBMIRange(19f, 24f, "Young Adult")
            age < 35 -> DGEBMIRange(20f, 25f, "Adult")
            age < 45 -> DGEBMIRange(21f, 26f, "Middle Age")
            age < 55 -> DGEBMIRange(22f, 27f, "Mature")
            age < 65 -> DGEBMIRange(23f, 28f, "Senior")
            else -> DGEBMIRange(24f, 29f, "Elderly")
        }
    }
}

