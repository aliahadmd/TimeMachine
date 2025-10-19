package me.aliahad.timemanager

import me.aliahad.timemanager.data.*
import kotlin.math.pow

/**
 * Comprehensive BMI calculation utilities
 */

/**
 * Calculate BMI from height (cm) and weight (kg)
 */
fun calculateBMI(heightCm: Float, weightKg: Float): Float {
    if (heightCm <= 0 || weightKg <= 0) return 0f
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

/**
 * Get BMI category based on classification system
 */
fun getBMICategory(
    bmi: Float,
    age: Int,
    gender: Gender,
    classification: BMIClassification
): String {
    return when (classification) {
        BMIClassification.WHO -> WHOBMICategory.fromBMI(bmi).displayName
        BMIClassification.DGE -> DGEClassification.getCategory(bmi, age, gender)
    }
}

/**
 * Get BMI category color
 */
fun getBMICategoryColor(bmi: Float): Long {
    return WHOBMICategory.fromBMI(bmi).color
}

/**
 * Calculate ideal weight range based on height and gender
 */
fun getIdealWeightRange(heightCm: Float, gender: Gender): Pair<Float, Float> {
    val heightM = heightCm / 100f
    // Normal BMI range is 18.5 - 24.9
    val minWeight = 18.5f * (heightM * heightM)
    val maxWeight = 24.9f * (heightM * heightM)
    return Pair(minWeight, maxWeight)
}

/**
 * Get detailed weight category table based on height
 */
data class WeightCategory(
    val category: String,
    val minWeight: Float,
    val maxWeight: Float,
    val color: Long
)

fun getWeightCategoryTable(heightCm: Float): List<WeightCategory> {
    val heightM = heightCm / 100f
    return listOf(
        WeightCategory(
            "Underweight",
            0f,
            18.5f * (heightM * heightM),
            0xFF42A5F5
        ),
        WeightCategory(
            "Normal",
            18.5f * (heightM * heightM),
            24.9f * (heightM * heightM),
            0xFF4CAF50
        ),
        WeightCategory(
            "Overweight",
            25f * (heightM * heightM),
            29.9f * (heightM * heightM),
            0xFFFFA726
        ),
        WeightCategory(
            "Obese",
            30f * (heightM * heightM),
            Float.MAX_VALUE,
            0xFFE53935
        )
    )
}

/**
 * Unit conversions
 */

// Height conversions
fun cmToFeet(cm: Float): Float = cm / 30.48f
fun cmToInches(cm: Float): Float = cm / 2.54f
fun feetAndInchesToCm(feet: Int, inches: Int): Float = (feet * 30.48f) + (inches * 2.54f)

// Weight conversions
fun kgToLbs(kg: Float): Float = kg * 2.20462f
fun lbsToKg(lbs: Float): Float = lbs / 2.20462f

/**
 * Format BMI value
 */
fun formatBMI(bmi: Float): String = String.format("%.1f", bmi)

/**
 * Get BMI interpretation message
 */
fun getBMIInterpretation(bmi: Float, category: String): String {
    return when {
        bmi < 16 -> "⚠️ Severely underweight. Please consult a healthcare professional."
        bmi < 18.5 -> "📉 You're underweight. Consider increasing caloric intake with nutritious foods."
        bmi < 25 -> "✅ You're in the healthy weight range! Keep up the good work."
        bmi < 30 -> "📈 You're overweight. Consider a balanced diet and regular exercise."
        bmi < 35 -> "⚠️ Obesity Class I. Consult a healthcare professional for guidance."
        bmi < 40 -> "⚠️ Obesity Class II. Medical intervention may be beneficial."
        else -> "🚨 Obesity Class III. Please seek medical advice immediately."
    }
}

/**
 * Calculate calories needed to reach target weight
 */
fun getTargetWeightGuidance(currentWeight: Float, targetWeight: Float): String {
    val diff = currentWeight - targetWeight
    return when {
        diff > 0 -> {
            val weeks = (diff / 0.5f).toInt() // Safe weight loss: 0.5kg per week
            "To reach your target weight, you need to lose ${String.format("%.1f", diff)} kg. " +
                    "At a safe rate of 0.5kg/week, this will take approximately $weeks weeks."
        }
        diff < 0 -> {
            val gain = -diff
            val weeks = (gain / 0.5f).toInt() // Safe weight gain: 0.5kg per week
            "To reach your target weight, you need to gain ${String.format("%.1f", gain)} kg. " +
                    "At a safe rate of 0.5kg/week, this will take approximately $weeks weeks."
        }
        else -> "You're already at your target weight! 🎉"
    }
}

/**
 * Get health tips based on BMI category
 */
fun getHealthTips(bmi: Float): List<String> {
    return when {
        bmi < 18.5 -> listOf(
            "🥗 Increase calorie intake with nutrient-dense foods",
            "💪 Include strength training to build muscle mass",
            "🥛 Ensure adequate protein intake",
            "👨‍⚕️ Consult a nutritionist for personalized advice"
        )
        bmi < 25 -> listOf(
            "✅ Maintain your current healthy lifestyle",
            "🏃 Continue regular physical activity",
            "🥗 Keep eating balanced, nutritious meals",
            "💧 Stay hydrated throughout the day"
        )
        bmi < 30 -> listOf(
            "🚶 Aim for 150 minutes of moderate exercise per week",
            "🥗 Focus on portion control and balanced meals",
            "💧 Drink plenty of water before meals",
            "😴 Ensure 7-8 hours of quality sleep"
        )
        else -> listOf(
            "👨‍⚕️ Consult a healthcare professional",
            "🏃 Start with low-impact exercises like walking",
            "🥗 Consider working with a nutritionist",
            "📊 Track your food intake and progress"
        )
    }
}

/**
 * Validate input values
 */
data class BMIInputValidation(
    val isValid: Boolean,
    val errorMessage: String? = null
)

fun validateBMIInputs(
    age: Int?,
    heightCm: Float?,
    weightKg: Float?
): BMIInputValidation {
    return when {
        age == null || age < 2 || age > 120 -> 
            BMIInputValidation(false, "Age must be between 2 and 120 years")
        heightCm == null || heightCm < 50 || heightCm > 250 -> 
            BMIInputValidation(false, "Height must be between 50 and 250 cm")
        weightKg == null || weightKg < 10 || weightKg > 300 -> 
            BMIInputValidation(false, "Weight must be between 10 and 300 kg")
        else -> BMIInputValidation(true)
    }
}

