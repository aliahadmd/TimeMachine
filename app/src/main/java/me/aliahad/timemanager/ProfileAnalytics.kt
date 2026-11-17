package me.aliahad.timemanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.*
import java.time.LocalDate

object ProfileAnalytics {

    suspend fun getUserStatistics(database: TimerDatabase): UserStatistics = withContext(Dispatchers.IO) {
        val todayDate = LocalDate.now()
        val today = todayDate.format(SafeDateTime.ISO_DATE)
        val thirtyDaysAgo = todayDate.minusDays(29).format(SafeDateTime.ISO_DATE)
        val sevenDaysAgo = todayDate.minusDays(6).format(SafeDateTime.ISO_DATE)

        // Focus Tracker (SQL aggregates + single active-dates query)
        val totalFocusMinutes = database.timeSessionDao().getTotalFocusMinutesSql()
        val totalFocusSessions = database.timeSessionDao().getTotalSessionCountSql()
        val todayFocusMinutes = database.timeSessionDao().getTotalMinutesForDate(today) ?: 0
        val activeDates = database.timeSessionDao().getAllActiveDates()
        val focusStreak = calculateStreaks(activeDates, totalDays = 30, today = todayDate).currentStreak
        val activeCategories = database.activityCategoryDao().getActiveCategoryCount()

        // Habit Tracker (active habits + ACHIEVED vs total active submissions)
        val allHabits = database.habitDao().getAllHabitsSync()
        val totalHabits = allHabits.size
        val activeHabits = allHabits.count { it.isActive }
        val totalHabitCompletions = database.habitDao().getTotalCompletionsCountSql()
        val activeAchieved = database.habitDao().getTotalActiveAchievedCountSql()
        val activeSubmissions = database.habitDao().getTotalActiveSubmissionCountSql()
        val habitSuccessRate = if (activeSubmissions > 0) {
            (activeAchieved.toFloat() / activeSubmissions).coerceIn(0f, 1f)
        } else 0f

        // Expense Tracker (30-day spend aligned with 30-day distinct spending days)
        val totalExpenses = database.expenseDao().getTotalExpenseCountSql()
        val totalSpentLast30 = database.expenseDao().getTotalForDateRange(thirtyDaysAgo, today) ?: 0.0
        val expensesLast30Count = database.expenseDao().getExpenseCountInRangeSql(thirtyDaysAgo, today)
        val distinctDays30 = database.expenseDao().getDistinctExpenseDaysInRangeSql(thirtyDaysAgo, today)
        val averageDailySpending = if (distinctDays30 > 0) totalSpentLast30 / distinctDays30 else 0.0
        val expenseCategories = database.expenseCategoryDao().getAllCategoriesSync().count { it.isActive }

        // Subscription Tracker
        val totalSubscriptions = database.subscriptionDao().getTotalSubscriptionCountSql()
        val activeSubs = database.subscriptionDao().getActiveSubscriptionsSync()
        val activeSubscriptions = activeSubs.size
        val monthlySubscriptionCost = SubscriptionAnalytics.calculateTotalMonthlyCost(activeSubs)
        val yearlySubscriptionCost = SubscriptionAnalytics.calculateTotalYearlyCost(activeSubs)

        // Daily Planner (SQL aggregates)
        val allTasks = database.dailyTaskDao().getTotalTaskCountSql()
        val completedTasks = database.dailyTaskDao().getTotalCompletedTaskCountSql()
        val taskCompletionRate = if (allTasks > 0) completedTasks.toFloat() / allTasks else 0f
        val todayTasks = database.dailyTaskDao().getTaskCountForDate(today)
        val todayCompleted = database.dailyTaskDao().getCompletedCountForDate(today)
        val upcomingTasks = (todayTasks - todayCompleted).coerceAtLeast(0)

        // Screen Timer
        val todayScreenTimeSeconds = database.screenTimeDao().getDailySummary(today)?.totalScreenTimeSeconds ?: 0
        val sevenDayAvgScreenTimeSeconds = (database.screenTimeDao().getAverageScreenTime(sevenDaysAgo, today) ?: 0.0).toInt()

        // Year & BMI Calculators
        val savedCalculations = database.dateCalculationDao().getCalculationCount()
        val bmiRecords = database.bmiCalculationDao().getCalculationCount()
        val latestBMI = database.bmiCalculationDao().getLatestCalculation()?.bmiValue

        UserStatistics(
            totalFocusMinutes = totalFocusMinutes,
            totalFocusSessions = totalFocusSessions,
            focusStreak = focusStreak,
            activeCategories = activeCategories,
            todayFocusMinutes = todayFocusMinutes,
            totalHabits = totalHabits,
            activeHabits = activeHabits,
            totalHabitCompletions = totalHabitCompletions,
            habitSuccessRate = habitSuccessRate,
            totalExpenses = totalExpenses,
            totalSpentLast30Days = totalSpentLast30,
            totalExpensesLast30DaysCount = expensesLast30Count,
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
            todayScreenTimeSeconds = todayScreenTimeSeconds,
            sevenDayAvgScreenTimeSeconds = sevenDayAvgScreenTimeSeconds,
            savedCalculations = savedCalculations,
            bmiRecords = bmiRecords,
            latestBMI = latestBMI
        )
    }

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

    fun formatDuration(minutes: Int): String = Formatters.formatMinutes(minutes)

    fun formatCurrency(
        amount: Double,
        currency: String = CurrencyCatalog.DEFAULT_SYMBOL
    ): String = Formatters.formatCurrency(amount, currency)

    fun getAvatarEmojis(): List<String> {
        return listOf(
            "👤", "👨", "👩", "🧑", "👦", "👧",
            "😀", "😎", "🤓", "🥳", "😇", "🤩",
            "🦸", "🦹", "🧙", "🧚", "🦄", "🐱",
            "🐶", "🐼", "🐨", "🦁", "🐯", "🦊"
        )
    }
}
