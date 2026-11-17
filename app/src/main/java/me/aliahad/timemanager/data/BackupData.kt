package me.aliahad.timemanager.data

import kotlinx.serialization.Serializable

/**
 * Complete backup schema for TimeMachine.
 * Version 1: Original TimeMachine v3.0.0 format (backward-compatible reader).
 * Version 2: Adds Presets, ScreenTime tables, Session notes/createdAt/linkedTaskId,
 *            Habit schedules/links, Subscription currencyCode/autoLogExpense, and DailyTask links.
 */
@Serializable
data class BackupData(
    val version: Int = 2,
    val timestamp: Long = System.currentTimeMillis(),
    val profile: UserProfileBackup? = null,
    val focusTracker: FocusTrackerBackup = FocusTrackerBackup(),
    val habitTracker: HabitTrackerBackup = HabitTrackerBackup(),
    val expenseTracker: ExpenseTrackerBackup = ExpenseTrackerBackup(),
    val subscriptionTracker: SubscriptionTrackerBackup = SubscriptionTrackerBackup(),
    val dailyPlanner: DailyPlannerBackup = DailyPlannerBackup(),
    val yearCalculator: YearCalculatorBackup = YearCalculatorBackup(),
    val bmiCalculator: BMICalculatorBackup = BMICalculatorBackup(),
    val screenTime: ScreenTimeBackup = ScreenTimeBackup()
)

@Serializable
data class UserProfileBackup(
    val id: Long = 0,
    val name: String = "User",
    val email: String = "",
    val bio: String = "",
    val avatarIcon: String = "👤",
    val joinDate: Long = 0,
    val dailyGoalMinutes: Int = 120,
    val theme: String = "Auto",
    val currency: String = "৳",
    val currencyCode: String = "",
    val notificationsEnabled: Boolean = true,
    val weekStartDay: String = "Monday",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class FocusTrackerBackup(
    val categories: List<CategoryBackup> = emptyList(),
    val sessions: List<SessionBackup> = emptyList(),
    val presets: List<PresetBackup> = emptyList()
)

@Serializable
data class PresetBackup(
    val id: Long = 0,
    val name: String,
    val hours: Int,
    val minutes: Int,
    val breakMinutes: Int = 5,
    val isPomodoro: Boolean = false,
    val createdAt: Long = 0
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
    val date: String,
    val notes: String = "",
    val linkedTaskId: Long? = null,
    val createdAt: Long = 0
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
    val scheduledDays: String = "1,2,3,4,5,6,7",
    val reminderTimeHour: Int?,
    val reminderTimeMinute: Int?,
    val linkedFocusCategoryId: Long? = null,
    val linkType: String = "MANUAL",
    val linkedTargetValue: Int = 0,
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
    val paymentMethod: String,
    val subscriptionId: Long? = null
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
    val currency: String = "৳",
    val currencyCode: String = "",
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
    val reminderDaysBefore: Int,
    val expenseCategoryId: Long? = null,
    val autoLogExpense: Boolean = false
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
    val linkedCategoryId: Long? = null,
    val linkedSessionId: Long? = null,
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

@Serializable
data class ScreenTimeBackup(
    val sessions: List<ScreenTimeSessionBackup> = emptyList(),
    val dailySummaries: List<ScreenTimeDailySummaryBackup> = emptyList(),
    val hourlyData: List<ScreenTimeHourlyBackup> = emptyList()
)

@Serializable
data class ScreenTimeSessionBackup(
    val date: String,
    val timestamp: Long,
    val sessionStart: Long,
    val sessionEnd: Long,
    val durationSeconds: Int,
    val unlockCount: Int = 1,
    val wasWalking: Boolean = false,
    val appUsed: String? = null
)

@Serializable
data class ScreenTimeDailySummaryBackup(
    val date: String,
    val totalScreenTimeSeconds: Int,
    val pickupsCount: Int,
    val firstPickupTime: Long? = null,
    val lastPickupTime: Long? = null,
    val walkingScreenTimeSeconds: Int = 0,
    val avgSessionDurationSeconds: Int,
    val longestSessionSeconds: Int,
    val shortestSessionSeconds: Int,
    val notificationsCount: Int = 0,
    val updatedAt: Long
)

@Serializable
data class ScreenTimeHourlyBackup(
    val date: String,
    val hour: Int,
    val screenTimeSeconds: Int,
    val pickupsCount: Int
)
