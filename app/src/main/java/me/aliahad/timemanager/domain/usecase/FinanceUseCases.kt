package me.aliahad.timemanager.domain.usecase

import me.aliahad.timemanager.ExpenseAnalytics
import me.aliahad.timemanager.SubscriptionAnalytics
import me.aliahad.timemanager.core.common.ImmediateTransactionRunner
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.core.common.TransactionRunner
import me.aliahad.timemanager.core.datastore.UserPreferencesRepository
import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import me.aliahad.timemanager.data.ExpenseCategoryDao
import me.aliahad.timemanager.data.ExpenseDao
import me.aliahad.timemanager.data.Subscription
import me.aliahad.timemanager.data.SubscriptionDao
import me.aliahad.timemanager.domain.gateway.NoOpSystemNotificationGateway
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway

enum class BudgetAlertLevel {
    NONE,
    WARNING_80,
    EXCEEDED
}

data class BudgetCheckOutcome(
    val alertLevel: BudgetAlertLevel,
    val categoryName: String = "",
    val spent: Double = 0.0,
    val budget: Double = 0.0
)

data class RenewSubscriptionResult(
    val updatedSubscription: Subscription,
    val loggedExpense: Expense?,
    val budgetOutcome: BudgetCheckOutcome
)

class FinanceUseCases(
    private val transactionRunner: TransactionRunner = ImmediateTransactionRunner,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val subscriptionDao: SubscriptionDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val notificationGateway: SystemNotificationGateway = NoOpSystemNotificationGateway
) {
    suspend fun addOrUpdateExpenseWithBudgetCheck(
        expense: Expense,
        isEdit: Boolean
    ): BudgetCheckOutcome {
        if (isEdit) {
            expenseDao.updateExpense(expense)
        } else {
            expenseDao.insertExpense(expense)
        }
        return evaluateBudgetAndNotify(expense.categoryId, expense.date)
    }

    suspend fun renewSubscriptionAndMaybeLogExpense(
        subscription: Subscription,
        forceLogExpense: Boolean? = null
    ): RenewSubscriptionResult {
        val today = timeProvider.today().format(SafeDateTime.ISO_DATE)
        val shouldLog = forceLogExpense ?: subscription.autoLogExpense

        val (updatedSub, createdExpense) = transactionRunner.runInTransaction {
            val nextBilling = SubscriptionAnalytics.calculateNextBillingFromDate(
                subscription.nextBillingDate,
                subscription.billingCycle
            )
            val renewed = subscription.copy(
                nextBillingDate = nextBilling,
                isTrial = false
            )
            subscriptionDao.updateSubscription(renewed)

            val insertedExpense = if (shouldLog && subscription.cost > 0.0) {
                val categoryId = resolveExpenseCategoryForSubscription(subscription)
                val newExpense = Expense(
                    categoryId = categoryId,
                    amount = subscription.cost,
                    description = "${subscription.name} (${subscription.billingCycle})",
                    date = today,
                    timestamp = timeProvider.nowMillis(),
                    paymentMethod = subscription.paymentMethod,
                    subscriptionId = subscription.id
                )
                val expenseId = expenseDao.insertExpense(newExpense)
                newExpense.copy(id = expenseId)
            } else {
                null
            }
            renewed to insertedExpense
        }

        val budgetOutcome = if (createdExpense != null) {
            evaluateBudgetAndNotify(createdExpense.categoryId, createdExpense.date)
        } else {
            BudgetCheckOutcome(BudgetAlertLevel.NONE)
        }

        return RenewSubscriptionResult(
            updatedSubscription = updatedSub,
            loggedExpense = createdExpense,
            budgetOutcome = budgetOutcome
        )
    }

    private suspend fun resolveExpenseCategoryForSubscription(subscription: Subscription): Long {
        subscription.expenseCategoryId?.let { explicitId ->
            if (expenseCategoryDao.getCategoryById(explicitId) != null) {
                return explicitId
            }
        }
        val existingCategories = expenseCategoryDao.getAllCategoriesSync().filter { it.isActive }
        val matching = existingCategories.firstOrNull { cat ->
            cat.name.contains("subscription", ignoreCase = true) ||
                cat.name.contains("bill", ignoreCase = true)
        } ?: existingCategories.firstOrNull()
        if (matching != null) return matching.id

        return expenseCategoryDao.insertCategory(
            ExpenseCategory(
                name = "Subscriptions",
                icon = "💳",
                color = 0xFF9775FAL
            )
        )
    }

    private suspend fun evaluateBudgetAndNotify(
        categoryId: Long,
        referenceDate: String
    ): BudgetCheckOutcome {
        val category = expenseCategoryDao.getCategoryById(categoryId)
            ?: return BudgetCheckOutcome(BudgetAlertLevel.NONE)
        if (category.budget <= 0.0) {
            return BudgetCheckOutcome(BudgetAlertLevel.NONE, category.name)
        }

        val parsedDate = SafeDateTime.parseLocalDateOrNull(referenceDate) ?: timeProvider.today()
        val (monthStart, monthEnd) = ExpenseAnalytics.getMonthDateRange(parsedDate)
        val spent = expenseDao.getTotalForCategoryInRange(category.id, monthStart, monthEnd) ?: 0.0
        val ratio = spent / category.budget

        val alertLevel = when {
            ratio >= 1.0 -> BudgetAlertLevel.EXCEEDED
            ratio >= 0.8 -> BudgetAlertLevel.WARNING_80
            else -> BudgetAlertLevel.NONE
        }

        if (alertLevel != BudgetAlertLevel.NONE) {
            val prefs = preferencesRepository.getPreferences()
            notificationGateway.notifyBudgetThreshold(
                categoryId = category.id,
                categoryName = category.name,
                spent = spent,
                budget = category.budget,
                currencySymbol = prefs.currencySymbol,
                isOverBudget = alertLevel == BudgetAlertLevel.EXCEEDED
            )
        }

        return BudgetCheckOutcome(
            alertLevel = alertLevel,
            categoryName = category.name,
            spent = spent,
            budget = category.budget
        )
    }
}
