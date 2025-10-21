package me.aliahad.timemanager

import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object ExpenseAnalytics {
    
    // Date formatting
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }
    
    fun getMonthDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        return startOfMonth.format(dateFormatter) to endOfMonth.format(dateFormatter)
    }
    
    fun getWeekDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val endOfWeek = startOfWeek.plusDays(6)
        return startOfWeek.format(dateFormatter) to endOfWeek.format(dateFormatter)
    }
    
    fun getYearDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val startOfYear = today.withDayOfYear(1)
        val endOfYear = today.withDayOfYear(today.lengthOfYear())
        return startOfYear.format(dateFormatter) to endOfYear.format(dateFormatter)
    }
    
    // Format currency
    fun formatCurrency(amount: Double): String {
        return "৳%.2f".format(abs(amount))
    }
    
    // Calculate category spending
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
            val percentage = ((total / totalAmount) * 100).toFloat()
            
            CategorySpending(
                category = category,
                total = total,
                percentage = percentage,
                expenseCount = categoryExpenses.size
            )
        }.sortedByDescending { it.total }
    }
    
    // Daily spending trend
    data class DailySpending(
        val date: String,
        val total: Double,
        val expenseCount: Int
    )
    
    fun calculateDailyTrend(expenses: List<Expense>, days: Int = 7): List<DailySpending> {
        val today = LocalDate.now()
        val dateRange = (0 until days).map { 
            today.minusDays(it.toLong()).format(dateFormatter)
        }.reversed()
        
        val expensesByDate = expenses.groupBy { it.date }
        
        return dateRange.map { date ->
            val dayExpenses = expensesByDate[date] ?: emptyList()
            DailySpending(
                date = date,
                total = dayExpenses.sumOf { it.amount },
                expenseCount = dayExpenses.size
            )
        }
    }
    
    // Budget tracking
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
            .filter { it.budget > 0 }
            .map { category ->
                val spent = spendingByCategory[category.id]?.sumOf { it.amount } ?: 0.0
                val remaining = category.budget - spent
                val percentageUsed = ((spent / category.budget) * 100).toFloat().coerceIn(0f, 200f)
                
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
    
    // Statistics
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
        val avgPerDay = if (days > 0) total / days else 0.0
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
    
    // Format date for display
    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, dateFormatter)
            val today = LocalDate.now()
            
            when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM dd"))
                else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
    // Get month name
    fun getMonthName(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, dateFormatter)
            date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        } catch (e: Exception) {
            dateString
        }
    }
    
    // Filter expenses by date range
    fun filterExpensesByDateRange(
        expenses: List<Expense>,
        startDate: String,
        endDate: String
    ): List<Expense> {
        return expenses.filter { expense ->
            expense.date >= startDate && expense.date <= endDate
        }
    }
    
    // Get last N months
    fun getLastMonths(count: Int): List<Pair<String, String>> {
        val currentMonth = YearMonth.now()
        return (0 until count).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            val startDate = month.atDay(1).format(dateFormatter)
            val endDate = month.atEndOfMonth().format(dateFormatter)
            month.format(DateTimeFormatter.ofPattern("MMM yyyy")) to startDate
        }.reversed()
    }
}

