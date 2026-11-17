package me.aliahad.timemanager.data.repository

import kotlinx.coroutines.flow.Flow
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.core.datastore.UserPreferencesRepository
import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import me.aliahad.timemanager.data.ExpenseCategoryDao
import me.aliahad.timemanager.data.ExpenseDao
import me.aliahad.timemanager.data.Subscription
import me.aliahad.timemanager.data.SubscriptionDao

class FinanceRepository(
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val subscriptionDao: SubscriptionDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val activeExpenseCategories: Flow<List<ExpenseCategory>> = expenseCategoryDao.getAllActiveCategories()
    val allExpenseCategories: Flow<List<ExpenseCategory>> = expenseCategoryDao.getAllCategories()
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    val activeSubscriptions: Flow<List<Subscription>> = subscriptionDao.getActiveSubscriptions()
    val activeSubscriptionCount: Flow<Int> = subscriptionDao.getActiveSubscriptionCountFlow()

    fun getTodayTotalExpensesFlow(): Flow<Double> {
        val today = timeProvider.today().format(SafeDateTime.ISO_DATE)
        return expenseDao.getTotalForDateFlow(today)
    }

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun insertExpenseCategory(category: ExpenseCategory): Long =
        expenseCategoryDao.insertCategory(category)

    suspend fun updateExpenseCategory(category: ExpenseCategory) =
        expenseCategoryDao.updateCategory(category)

    suspend fun deleteExpenseCategory(category: ExpenseCategory) =
        expenseCategoryDao.deleteCategory(category)

    suspend fun insertSubscription(subscription: Subscription): Long =
        subscriptionDao.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: Subscription) =
        subscriptionDao.updateSubscription(subscription)

    suspend fun deleteSubscription(subscription: Subscription) =
        subscriptionDao.deleteSubscription(subscription)

    suspend fun toggleSubscriptionActive(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription.copy(isActive = !subscription.isActive))
    }

    suspend fun ensureDefaultExpenseCategoriesIfNeeded() {
        val prefs = preferencesRepository.getPreferences()
        if (prefs.expenseCategoriesSeeded) return
        if (expenseCategoryDao.getCategoryCount() == 0) {
            val defaults = listOf(
                ExpenseCategory(name = "Food & Dining", icon = "🍽️", color = 0xFFFF6B6BL),
                ExpenseCategory(name = "Transport", icon = "🚕", color = 0xFF4DABF7L),
                ExpenseCategory(name = "Groceries", icon = "🛒", color = 0xFF51CF66L),
                ExpenseCategory(name = "Subscriptions", icon = "💳", color = 0xFF9775FAL),
                ExpenseCategory(name = "Shopping", icon = "🛍️", color = 0xFFF06595L),
                ExpenseCategory(name = "Bills & Utilities", icon = "⚡", color = 0xFFFF922BL),
                ExpenseCategory(name = "Health", icon = "🏥", color = 0xFF20C997L),
                ExpenseCategory(name = "Other", icon = "📦", color = 0xFF868E96L)
            )
            defaults.forEach { expenseCategoryDao.insertCategory(it) }
        }
        preferencesRepository.setExpenseCategoriesSeeded(true)
    }
}
