package me.aliahad.timemanager.data

import kotlinx.serialization.Serializable

/**
 * Complete backup data structure for the entire app ecosystem
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val profile: UserProfileBackup? = null,
    val focusTracker: FocusTrackerBackup = FocusTrackerBackup(),
    val habitTracker: HabitTrackerBackup = HabitTrackerBackup(),
    val expenseTracker: ExpenseTrackerBackup = ExpenseTrackerBackup(),
    val subscriptionTracker: SubscriptionTrackerBackup = SubscriptionTrackerBackup(),
    val dailyPlanner: DailyPlannerBackup = DailyPlannerBackup(),
    val yearCalculator: YearCalculatorBackup = YearCalculatorBackup(),
    val bmiCalculator: BMICalculatorBackup = BMICalculatorBackup()
)

@Serializable
data class UserProfileBackup(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarIcon: String = "",
    val joinDate: Long = 0,
    val dailyGoalMinutes: Int = 0,
    val theme: String = "",
    val currency: String = "",
    val notificationsEnabled: Boolean = true,
    val weekStartDay: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class FocusTrackerBackup(
    val categories: List<CategoryBackup> = emptyList(),
    val sessions: List<SessionBackup> = emptyList()
)

@Serializable
data class CategoryBackup(
    val id: Long,
    val name: String,
    val icon: String,
    val colorLong: Long,
    val dailyGoalMinutes: Int,
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SessionBackup(
    val id: Long,
    val categoryId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val date: String
)

@Serializable
data class HabitTrackerBackup(
    val habits: List<HabitBackup> = emptyList(),
    val completions: List<HabitCompletionBackup> = emptyList()
)

@Serializable
data class HabitBackup(
    val id: Long,
    val name: String,
    val description: String,
    val colorLong: Long,
    val iconName: String,
    val habitType: String,
    val goalPeriodDays: Int,
    val isEveryday: Boolean,
    val reminderTimeHour: Int?,
    val reminderTimeMinute: Int?,
    val isActive: Boolean,
    val createdAt: Long
)

@Serializable
data class HabitCompletionBackup(
    val habitId: Long,
    val date: String,
    val completionType: String,
    val completedAt: Long,
    val notes: String
)

@Serializable
data class ExpenseTrackerBackup(
    val categories: List<ExpenseCategoryBackup> = emptyList(),
    val expenses: List<ExpenseBackup> = emptyList()
)

@Serializable
data class ExpenseCategoryBackup(
    val id: Long,
    val name: String,
    val icon: String,
    val colorLong: Long,
    val budget: Double,
    val isActive: Boolean
)

@Serializable
data class ExpenseBackup(
    val id: Long,
    val amount: Double,
    val categoryId: Long,
    val description: String,
    val date: String,
    val timestamp: Long,
    val paymentMethod: String
)

@Serializable
data class SubscriptionTrackerBackup(
    val subscriptions: List<SubscriptionBackup> = emptyList()
)

@Serializable
data class SubscriptionBackup(
    val id: Long,
    val name: String,
    val cost: Double,
    val billingCycle: String,
    val startDate: String,
    val nextBillingDate: String,
    val category: String,
    val icon: String,
    val colorLong: Long,
    val paymentMethod: String,
    val website: String,
    val notes: String,
    val isActive: Boolean,
    val isTrial: Boolean,
    val trialEndDate: String? = null,
    val reminderDaysBefore: Int
)

@Serializable
data class DailyPlannerBackup(
    val tasks: List<DailyTaskBackup> = emptyList()
)

@Serializable
data class DailyTaskBackup(
    val id: Long,
    val title: String,
    val description: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val taskType: String,
    val category: String,
    val priority: String,
    val icon: String,
    val colorLong: Long,
    val isCompleted: Boolean,
    val isRecurring: Boolean,
    val recurringDays: String,
    val reminderMinutes: Int,
    val notes: String,
    val createdAt: Long
)

@Serializable
data class YearCalculatorBackup(
    val calculations: List<DateCalculationBackup> = emptyList()
)

@Serializable
data class DateCalculationBackup(
    val id: Long,
    val title: String,
    val startDate: Long,
    val endDate: Long,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BMICalculatorBackup(
    val calculations: List<BMICalculationBackupData> = emptyList()
)

@Serializable
data class BMICalculationBackupData(
    val id: Long,
    val name: String,
    val gender: String,
    val age: Int,
    val heightCm: Float,
    val weightKg: Float,
    val bmiValue: Float,
    val classification: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

