package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.aliahad.timemanager.core.common.CurrencyCatalog

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "User",
    val email: String = "",
    val bio: String = "",
    val avatarIcon: String = "👤",
    val joinDate: Long = System.currentTimeMillis(),
    val dailyGoalMinutes: Int = 120,
    val theme: String = "Auto", // Auto, Light, Dark
    val currency: String = CurrencyCatalog.DEFAULT_SYMBOL,
    val currencyCode: String = CurrencyCatalog.DEFAULT_CODE,
    val notificationsEnabled: Boolean = true,
    val weekStartDay: String = "Monday",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class UserStatistics(
    // Focus Tracker
    val totalFocusMinutes: Int,
    val totalFocusSessions: Int,
    val focusStreak: Int,
    val activeCategories: Int,
    val todayFocusMinutes: Int = 0,

    // Habit Tracker
    val totalHabits: Int,
    val activeHabits: Int,
    val totalHabitCompletions: Int,
    val habitSuccessRate: Float,

    // Expense Tracker
    val totalExpenses: Int,
    val totalSpentLast30Days: Double,
    val totalExpensesLast30DaysCount: Int,
    val averageDailySpending: Double,
    val expenseCategories: Int,

    // Subscription Tracker
    val totalSubscriptions: Int,
    val activeSubscriptions: Int,
    val monthlySubscriptionCost: Double,
    val yearlySubscriptionCost: Double,

    // Daily Planner
    val totalTasks: Int,
    val completedTasks: Int,
    val taskCompletionRate: Float,
    val upcomingTasks: Int,

    // Screen Timer
    val todayScreenTimeSeconds: Int = 0,
    val sevenDayAvgScreenTimeSeconds: Int = 0,

    // Year & BMI Calculators
    val savedCalculations: Int,
    val bmiRecords: Int,
    val latestBMI: Float?
)

data class UserAchievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float,
    val targetValue: Int,
    val currentValue: Int
)

data class RecentActivity(
    val timestamp: Long,
    val module: String,
    val action: String,
    val details: String,
    val icon: String
)
