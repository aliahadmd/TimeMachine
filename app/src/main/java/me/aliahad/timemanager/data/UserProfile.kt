package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "User",
    val email: String = "",
    val bio: String = "",
    val avatarIcon: String = "👤", // Emoji avatar
    val joinDate: Long = System.currentTimeMillis(),
    val dailyGoalMinutes: Int = 120, // Default daily focus goal
    val theme: String = "Auto", // Auto, Light, Dark
    val currency: String = "৳",
    val notificationsEnabled: Boolean = true,
    val weekStartDay: String = "Monday", // Monday, Sunday
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Aggregated user statistics from all modules
data class UserStatistics(
    // Focus Tracker
    val totalFocusMinutes: Int,
    val totalFocusSessions: Int,
    val focusStreak: Int,
    val activeCategories: Int,
    
    // Habit Tracker
    val totalHabits: Int,
    val activeHabits: Int,
    val totalHabitCompletions: Int,
    val habitSuccessRate: Float,
    
    // Expense Tracker
    val totalExpenses: Int,
    val totalSpent: Double,
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
    
    // Year Calculator
    val savedCalculations: Int,
    
    // BMI Calculator
    val bmiRecords: Int,
    val latestBMI: Float?
)

// Achievement data
data class UserAchievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float, // 0.0 to 1.0
    val targetValue: Int,
    val currentValue: Int
)

// Activity timeline entry
data class RecentActivity(
    val timestamp: Long,
    val module: String, // Focus, Habit, Expense, etc.
    val action: String, // Completed, Added, Deleted, etc.
    val details: String,
    val icon: String
)

