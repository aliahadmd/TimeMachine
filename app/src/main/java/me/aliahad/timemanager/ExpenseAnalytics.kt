package me.aliahad.timemanager

import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ExpenseAnalytics {

    fun getTodayDateString(today: LocalDate = LocalDate.now()): String = today.format(SafeDateTime.ISO_DATE)

    fun getMonthDateRange(today: LocalDate = LocalDate.now()): Pair<String, String> {
        val startOfMonth = today.withDayOfMonth(1)
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        return startOfMonth.format(SafeDateTime.ISO_DATE) to endOfMonth.format(SafeDateTime.ISO_DATE)
    }

    fun getWeekDateRange(today: LocalDate = LocalDate.now()): Pair<String, String> {
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val endOfWeek = startOfWeek.plusDays(6)
        return startOfWeek.format(SafeDateTime.ISO_DATE) to endOfWeek.format(SafeDateTime.ISO_DATE)
    }

    fun getYearDateRange(today: LocalDate = LocalDate.now()): Pair<String, String> {
        val startOfYear = today.withDayOfYear(1)
        val endOfYear = today.withDayOfYear(today.lengthOfYear())
        return startOfYear.format(SafeDateTime.ISO_DATE) to endOfYear.format(SafeDateTime.ISO_DATE)
    }

    fun formatCurrency(amount: Double, currency: String = CurrencyCatalog.DEFAULT_SYMBOL): String {
        return Formatters.formatCurrency(amount, currency)
    }

    data class CategorySpending(
        val category: ExpenseCategory,
        val total: Double,
        val percentage: Float,
        val expenseCount: Int
    )

    fun calculateCategorySpending(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>
    ): List<CategorySpending> {
        val totalAmount = expenses.sumOf { it.amount }
        if (totalAmount <= 0) return emptyList()

        val categoryMap = categories.associateBy { it.id }
        val spendingByCategory = expenses.groupBy { it.categoryId }

        return spendingByCategory.mapNotNull { (categoryId, categoryExpenses) ->
            val category = categoryMap[categoryId] ?: return@mapNotNull null
            val total = categoryExpenses.sumOf { it.amount }
            val percentage = ((total / totalAmount) * 100.0).toFloat().coerceIn(0f, 100f)

            CategorySpending(
                category = category,
                total = total,
                percentage = percentage,
                expenseCount = categoryExpenses.size
            )
        }.sortedByDescending { it.total }
    }

    data class DailySpending(
        val date: String,
        val total: Double,
        val expenseCount: Int
    )

    fun calculateDailyTrend(expenses: List<Expense>, days: Int = 7, today: LocalDate = LocalDate.now()): List<DailySpending> {
        val dateRange = (0 until days).map {
            today.minusDays(it.toLong()).format(SafeDateTime.ISO_DATE)
        }.reversed()

        val expensesByDate = expenses.groupBy { it.date }

        return dateRange.map { date ->
            val dayExpenses = expensesByDate[date].orEmpty()
            DailySpending(
                date = date,
                total = dayExpenses.sumOf { it.amount },
                expenseCount = dayExpenses.size
            )
        }
    }

    data class BudgetStatus(
        val category: ExpenseCategory,
        val spent: Double,
        val budget: Double,
        val remaining: Double,
        val percentageUsed: Float,
        val isOverBudget: Boolean
    )

    fun calculateBudgetStatus(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>
    ): List<BudgetStatus> {
        val spendingByCategory = expenses.groupBy { it.categoryId }

        return categories
            .filter { it.isActive && it.budget > 0 }
            .map { category ->
                val spent = spendingByCategory[category.id]?.sumOf { it.amount } ?: 0.0
                val remaining = category.budget - spent
                val percentageUsed = ((spent / category.budget) * 100.0).toFloat().coerceIn(0f, 200f)

                BudgetStatus(
                    category = category,
                    spent = spent,
                    budget = category.budget,
                    remaining = remaining,
                    percentageUsed = percentageUsed,
                    isOverBudget = spent > category.budget
                )
            }
            .sortedByDescending { it.percentageUsed }
    }

    data class ExpenseStats(
        val totalExpenses: Double,
        val averagePerDay: Double,
        val averagePerTransaction: Double,
        val transactionCount: Int,
        val highestExpense: Double,
        val mostExpensiveCategory: String?
    )

    fun calculateStats(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>,
        days: Int
    ): ExpenseStats {
        if (expenses.isEmpty()) {
            return ExpenseStats(0.0, 0.0, 0.0, 0, 0.0, null)
        }

        val total = expenses.sumOf { it.amount }
        val avgPerDay = if (days > 0) total / days else total
        val avgPerTransaction = total / expenses.size
        val highest = expenses.maxOfOrNull { it.amount } ?: 0.0

        val categoryMap = categories.associateBy { it.id }
        val mostExpensiveCategoryId = expenses
            .groupBy { it.categoryId }
            .maxByOrNull { it.value.sumOf { expense -> expense.amount } }
            ?.key
        val mostExpensiveCategory = mostExpensiveCategoryId?.let { categoryMap[it]?.name }

        return ExpenseStats(
            totalExpenses = total,
            averagePerDay = avgPerDay,
            averagePerTransaction = avgPerTransaction,
            transactionCount = expenses.size,
            highestExpense = highest,
            mostExpensiveCategory = mostExpensiveCategory
        )
    }

    fun calculateAllTimeDaysSpan(expenses: List<Expense>, today: LocalDate = LocalDate.now()): Int {
        val earliest = expenses.mapNotNull { SafeDateTime.parseLocalDateOrNull(it.date) }.minOrNull() ?: return 1
        return (ChronoUnit.DAYS.between(earliest, today).toInt() + 1).coerceAtLeast(1)
    }

    fun formatDateForDisplay(dateString: String): String = SafeDateTime.formatRelativeDate(dateString)

    fun getMonthName(dateString: String): String {
        val date = SafeDateTime.parseLocalDateOrNull(dateString) ?: return dateString
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    fun filterExpensesByDateRange(
        expenses: List<Expense>,
        startDate: String,
        endDate: String
    ): List<Expense> {
        return expenses.filter { expense ->
            expense.date >= startDate && expense.date <= endDate
        }
    }

    fun getLastMonths(count: Int): List<Pair<String, String>> {
        val currentMonth = YearMonth.now()
        return (0 until count).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            val startDate = month.atDay(1).format(SafeDateTime.ISO_DATE)
            month.format(DateTimeFormatter.ofPattern("MMM yyyy")) to startDate
        }.reversed()
    }
}
