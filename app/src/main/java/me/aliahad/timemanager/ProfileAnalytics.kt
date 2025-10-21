package me.aliahad.timemanager

import me.aliahad.timemanager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ProfileAnalytics {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Aggregate statistics from all modules
     */
    suspend fun getUserStatistics(database: TimerDatabase): UserStatistics = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val thirtyDaysAgo = LocalDate.now().minusDays(30).format(dateFormatter)
        
        // Focus Tracker Stats
        val totalFocusMinutes = database.timeSessionDao().getTotalMinutes() ?: 0
        val totalFocusSessions = database.timeSessionDao().getTotalSessionCount()
        val focusStreak = calculateFocusStreak(database)
        val activeCategories = database.activityCategoryDao().getActiveCategoriesCount()
        
        // Habit Tracker Stats
        val allHabits = database.habitDao().getAllHabitsSync()
        val totalHabits = allHabits.size
        val activeHabits = allHabits.count { it.isActive }
        val totalHabitCompletions = database.habitDao().getTotalCompletionsCount()
        // Calculate actual success rate
        val habitSuccessRate = if (activeHabits > 0 && totalHabitCompletions > 0) {
            val expectedCompletions = activeHabits * 30 // Assume 30 day period
            (totalHabitCompletions.toFloat() / expectedCompletions).coerceAtMost(1f)
        } else 0f
        
        // Expense Tracker Stats
        val allExpenses = database.expenseDao().getAllExpensesSync()
        val totalExpenses = allExpenses.size
        val totalSpent = database.expenseDao().getTotalExpensesForDateRange(thirtyDaysAgo, today)
        // Calculate average based on actual days with expenses, not fixed 30
        val daysWithExpenses = allExpenses.map { it.date }.distinct().size
        val averageDailySpending = if (daysWithExpenses > 0) totalSpent / daysWithExpenses else 0.0
        val expenseCategories = database.expenseCategoryDao().getActiveCategoryCount()
        
        // Subscription Tracker Stats
        val allSubscriptions = database.subscriptionDao().getAllSubscriptionsListSync()
        val totalSubscriptions = allSubscriptions.size
        val activeSubscriptions = database.subscriptionDao().getActiveSubscriptionCount()
        val monthlySubscriptionCost = SubscriptionAnalytics.calculateTotalMonthlyCost(allSubscriptions.filter { it.isActive })
        val yearlySubscriptionCost = SubscriptionAnalytics.calculateTotalYearlyCost(allSubscriptions.filter { it.isActive })
        
        // Daily Planner Stats
        val allTasks = database.dailyTaskDao().getTaskCountForAllDates()
        val completedTasks = database.dailyTaskDao().getCompletedCountForAllDates()
        val taskCompletionRate = if (allTasks > 0) completedTasks.toFloat() / allTasks else 0f
        val todayTasks = database.dailyTaskDao().getTaskCountForDate(today)
        val todayCompleted = database.dailyTaskDao().getCompletedCountForDate(today)
        val upcomingTasks = (todayTasks - todayCompleted).coerceAtLeast(0) // Prevent negative
        
        // Year Calculator Stats
        val savedCalculations = database.dateCalculationDao().getAllCalculationsCount()
        
        // BMI Calculator Stats
        val bmiRecords = database.bmiCalculationDao().getAllRecordsCount()
        val latestBMI = database.bmiCalculationDao().getLatestBMI()
        
        UserStatistics(
            totalFocusMinutes = totalFocusMinutes,
            totalFocusSessions = totalFocusSessions,
            focusStreak = focusStreak,
            activeCategories = activeCategories,
            totalHabits = totalHabits,
            activeHabits = activeHabits,
            totalHabitCompletions = totalHabitCompletions,
            habitSuccessRate = habitSuccessRate,
            totalExpenses = totalExpenses,
            totalSpent = totalSpent,
            averageDailySpending = averageDailySpending,
            expenseCategories = expenseCategories,
            totalSubscriptions = totalSubscriptions,
            activeSubscriptions = activeSubscriptions,
            monthlySubscriptionCost = monthlySubscriptionCost,
            yearlySubscriptionCost = yearlySubscriptionCost,
            totalTasks = allTasks,
            completedTasks = completedTasks,
            taskCompletionRate = taskCompletionRate,
            upcomingTasks = upcomingTasks,
            savedCalculations = savedCalculations,
            bmiRecords = bmiRecords,
            latestBMI = latestBMI
        )
    }
    
    private suspend fun calculateFocusStreak(database: TimerDatabase): Int {
        // Calculate consecutive days with focus sessions
        var streak = 0
        var currentDate = LocalDate.now()
        val maxStreakDays = 365 // Safety limit to prevent infinite loops
        
        try {
            while (streak < maxStreakDays) {
                val dateStr = currentDate.format(dateFormatter)
                val minutesForDate = database.timeSessionDao().getTotalMinutesForDate(dateStr) ?: 0
                
                if (minutesForDate > 0) {
                    streak++
                    currentDate = currentDate.minusDays(1)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileAnalytics", "Error calculating streak", e)
            return 0
        }
        
        return streak
    }
    
    /**
     * Get user achievements based on statistics
     */
    fun getUserAchievements(stats: UserStatistics): List<UserAchievement> {
        return listOf(
            UserAchievement(
                id = "focus_100h",
                title = "Focus Master",
                description = "Complete 100 hours of focused work",
                icon = "🎯",
                isUnlocked = stats.totalFocusMinutes >= 6000,
                progress = (stats.totalFocusMinutes / 6000f).coerceAtMost(1f),
                targetValue = 6000,
                currentValue = stats.totalFocusMinutes
            ),
            UserAchievement(
                id = "habit_30_days",
                title = "Habit Champion",
                description = "Complete 30 days of habit tracking",
                icon = "✅",
                isUnlocked = stats.totalHabitCompletions >= 30,
                progress = (stats.totalHabitCompletions / 30f).coerceAtMost(1f),
                targetValue = 30,
                currentValue = stats.totalHabitCompletions
            ),
            UserAchievement(
                id = "expense_100",
                title = "Budget Tracker",
                description = "Track 100 expenses",
                icon = "💰",
                isUnlocked = stats.totalExpenses >= 100,
                progress = (stats.totalExpenses / 100f).coerceAtMost(1f),
                targetValue = 100,
                currentValue = stats.totalExpenses
            ),
            UserAchievement(
                id = "tasks_100",
                title = "Task Master",
                description = "Complete 100 tasks",
                icon = "📋",
                isUnlocked = stats.completedTasks >= 100,
                progress = (stats.completedTasks / 100f).coerceAtMost(1f),
                targetValue = 100,
                currentValue = stats.completedTasks
            ),
            UserAchievement(
                id = "streak_7",
                title = "Consistent Achiever",
                description = "Maintain a 7-day focus streak",
                icon = "🔥",
                isUnlocked = stats.focusStreak >= 7,
                progress = (stats.focusStreak / 7f).coerceAtMost(1f),
                targetValue = 7,
                currentValue = stats.focusStreak
            )
        )
    }
    
    /**
     * Format duration in hours and minutes
     */
    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> "${mins}m"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }
    
    /**
     * Format currency
     */
    fun formatCurrency(amount: Double, currency: String = "৳"): String {
        return "$currency%.2f".format(amount)
    }
    
    /**
     * Get emoji for profile avatar selection
     */
    fun getAvatarEmojis(): List<String> {
        return listOf(
            "👤", "👨", "👩", "🧑", "👦", "👧",
            "😀", "😎", "🤓", "🥳", "😇", "🤩",
            "🦸", "🦹", "🧙", "🧚", "🦄", "🐱",
            "🐶", "🐼", "🐨", "🦁", "🐯", "🦊"
        )
    }
}

// Extension functions for DAOs to support ProfileAnalytics
suspend fun TimeSessionDao.getTotalMinutes(): Int? {
    // Get all sessions and sum durations
    return try {
        val sessions = getAllSessionsSync()
        sessions.sumOf { it.durationMinutes }
    } catch (e: Exception) {
        0
    }
}

suspend fun TimeSessionDao.getTotalSessionCount(): Int {
    return try {
        getAllSessionsSync().size
    } catch (e: Exception) {
        0
    }
}

suspend fun ActivityCategoryDao.getActiveCategoriesCount(): Int {
    return getAllCategoriesSync().count { it.isActive }
}

suspend fun HabitDao.getTotalCompletionsCount(): Int {
    return try {
        getAllCompletionsSync().size
    } catch (e: Exception) {
        0
    }
}

suspend fun ExpenseDao.getTotalExpensesForDateRange(startDate: String, endDate: String): Double {
    return try {
        val expenses = getAllExpensesSync()
        expenses.filter { it.date >= startDate && it.date <= endDate }.sumOf { it.amount }
    } catch (e: Exception) {
        0.0
    }
}

suspend fun ExpenseCategoryDao.getActiveCategoryCount(): Int {
    return getAllCategoriesSync().count { it.isActive }
}

suspend fun SubscriptionDao.getAllSubscriptionsListSync(): List<Subscription> {
    return try {
        getAllSubscriptionsSync()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun DailyTaskDao.getTaskCountForAllDates(): Int {
    return try {
        getAllTasksSync().size
    } catch (e: Exception) {
        0
    }
}

suspend fun DailyTaskDao.getCompletedCountForAllDates(): Int {
    return try {
        getAllTasksSync().count { it.isCompleted }
    } catch (e: Exception) {
        0
    }
}

suspend fun DailyTaskDao.getAllTasksSync(): List<DailyTask> {
    return try {
        getAllTasks()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun DateCalculationDao.getAllCalculationsCount(): Int {
    return try {
        getAllCalculationsSync().size
    } catch (e: Exception) {
        0
    }
}

suspend fun BMICalculationDao.getAllRecordsCount(): Int {
    return try {
        getAllRecordsSync().size
    } catch (e: Exception) {
        0
    }
}

suspend fun BMICalculationDao.getLatestBMI(): Float? {
    return try {
        getAllRecordsSync().maxByOrNull { it.createdAt }?.bmiValue
    } catch (e: Exception) {
        null
    }
}

