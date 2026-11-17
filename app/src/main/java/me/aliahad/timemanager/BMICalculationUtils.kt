package me.aliahad.timemanager

import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.data.*
import kotlin.math.ceil
import kotlin.math.roundToInt

fun calculateBMI(heightCm: Float, weightKg: Float): Float {
    if (heightCm <= 0f || weightKg <= 0f) return 0f
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

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

fun getBMICategoryColor(
    bmi: Float,
    age: Int = 30,
    gender: Gender = Gender.MALE,
    classification: BMIClassification = BMIClassification.WHO
): Long {
    return when (classification) {
        BMIClassification.WHO -> WHOBMICategory.fromBMI(bmi).color
        BMIClassification.DGE -> {
            when (DGEClassification.getCategory(bmi, age, gender)) {
                "Underweight" -> 0xFF42A5F5
                "Normal" -> 0xFF4CAF50
                "Overweight" -> 0xFFFFA726
                else -> 0xFFE53935
            }
        }
    }
}

fun getIdealWeightRange(
    heightCm: Float,
    gender: Gender,
    age: Int = 30,
    classification: BMIClassification = BMIClassification.WHO
): Pair<Float, Float> {
    val heightM = heightCm / 100f
    val (minBmi, maxBmi) = when (classification) {
        BMIClassification.WHO -> 18.5f to 24.9f
        BMIClassification.DGE -> {
            val range = DGEClassification.getIdealRange(age, gender)
            range.minBMI to range.maxBMI
        }
    }
    return Pair(minBmi * (heightM * heightM), maxBmi * (heightM * heightM))
}

data class WeightCategory(
    val category: String,
    val minWeight: Float,
    val maxWeight: Float,
    val color: Long
)

fun getWeightCategoryTable(heightCm: Float): List<WeightCategory> {
    val heightM = heightCm / 100f
    val sq = heightM * heightM
    return listOf(
        WeightCategory("Underweight", 0f, 18.5f * sq, 0xFF42A5F5),
        WeightCategory("Normal", 18.5f * sq, 24.9f * sq, 0xFF4CAF50),
        WeightCategory("Overweight", 25f * sq, 29.9f * sq, 0xFFFFA726),
        WeightCategory("Obese", 30f * sq, Float.MAX_VALUE, 0xFFE53935)
    )
}

fun cmToFeet(cm: Float): Float = cm / 30.48f
fun cmToInches(cm: Float): Float = cm / 2.54f

fun cmToFeetAndInches(cm: Float): Pair<Int, Int> {
    val totalInches = (cm / 2.54f).roundToInt().coerceAtLeast(0)
    val feet = totalInches / 12
    val inches = totalInches % 12
    return feet to inches
}

fun feetAndInchesToCm(feet: Int, inches: Int): Float {
    val raw = (feet * 30.48f) + (inches * 2.54f)
    return raw.coerceIn(50f, 250f)
}

fun kgToLbs(kg: Float): Float = kg * 2.20462f
fun lbsToKg(lbs: Float): Float = lbs / 2.20462f

fun formatBMI(bmi: Float): String = Formatters.formatOneDecimal(bmi)

fun getBMIInterpretation(
    bmi: Float,
    category: String,
    classification: BMIClassification = BMIClassification.WHO
): String {
    if (classification == BMIClassification.DGE) {
        return when (category) {
            "Underweight" -> "📉 Below your age/gender DGE reference range. Consider nutrient-dense foods."
            "Normal" -> "✅ Within your age/gender DGE healthy weight range! Keep up the good work."
            "Overweight" -> "📈 Above your age/gender DGE range. Consider balanced meals and regular activity."
            else -> "⚠️ Significantly above your DGE range. Consult a healthcare professional for guidance."
        }
    }
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

fun getTargetWeightGuidance(currentWeight: Float, targetWeight: Float): String {
    val diff = currentWeight - targetWeight
    return when {
        diff > 0.05f -> {
            val weeks = ceil(diff / 0.5f).toInt().coerceAtLeast(1)
            "To reach your target weight, you need to lose ${Formatters.formatOneDecimal(diff)} kg. " +
                "At a safe rate of 0.5kg/week, this will take approximately $weeks week(s)."
        }
        diff < -0.05f -> {
            val gain = -diff
            val weeks = ceil(gain / 0.5f).toInt().coerceAtLeast(1)
            "To reach your target weight, you need to gain ${Formatters.formatOneDecimal(gain)} kg. " +
                "At a safe rate of 0.5kg/week, this will take approximately $weeks week(s)."
        }
        else -> "You're already at your target weight! 🎉"
    }
}

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
        heightCm == null || heightCm < 50f || heightCm > 250f ->
            BMIInputValidation(false, "Height must be between 50 and 250 cm")
        weightKg == null || weightKg < 10f || weightKg > 300f ->
            BMIInputValidation(false, "Weight must be between 10 and 300 kg")
        else -> BMIInputValidation(true)
    }
}
