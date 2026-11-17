package me.aliahad.timemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.aliahad.timemanager.ExpenseAnalytics
import me.aliahad.timemanager.SubscriptionAnalytics
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import me.aliahad.timemanager.data.Subscription
import me.aliahad.timemanager.domain.usecase.BudgetCheckOutcome

enum class ExpenseRangeFilter(val label: String) {
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    ALL_TIME("All Time")
}

data class FinanceUiState(
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val activeSubscriptions: List<Subscription> = emptyList(),
    val monthSpent: Double = 0.0,
    val todaySpent: Double = 0.0,
    val filteredStats: ExpenseAnalytics.ExpenseStats = ExpenseAnalytics.ExpenseStats(0.0, 0.0, 0.0, 0, 0.0, null),
    val budgetStatuses: List<ExpenseAnalytics.BudgetStatus> = emptyList(),
    val categoryBreakdown: List<ExpenseAnalytics.CategorySpending> = emptyList(),
    val dailyTrend: List<ExpenseAnalytics.DailySpending> = emptyList(),
    val subscriptionStats: SubscriptionAnalytics.SubscriptionStats = SubscriptionAnalytics.SubscriptionStats(
        totalActive = 0,
        totalInactive = 0,
        totalMonthly = 0.0,
        totalYearly = 0.0,
        mostExpensive = null,
        cheapest = null,
        avgCostPerSub = 0.0,
        upcomingRenewalsCount = 0,
        hasMixedCurrencies = false,
        monthlyByCurrency = emptyMap()
    ),
    val renewalCalendar: List<SubscriptionAnalytics.UpcomingRenewal> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val rangeFilter: ExpenseRangeFilter = ExpenseRangeFilter.THIS_MONTH,
    val lastBudgetAlert: BudgetCheckOutcome? = null
)

class FinanceViewModel(private val container: AppContainer) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<Long?>(null)
    private val rangeFilterFlow = MutableStateFlow(ExpenseRangeFilter.THIS_MONTH)
    private val budgetAlertFlow = MutableStateFlow<BudgetCheckOutcome?>(null)

    init {
        viewModelScope.launch {
            container.financeRepository.ensureDefaultExpenseCategoriesIfNeeded()
        }
    }

    private val dataFlow = combine(
        container.preferencesRepository.preferencesFlow,
        container.financeRepository.allExpenses,
        container.financeRepository.activeExpenseCategories,
        container.financeRepository.allSubscriptions
    ) { prefs, expenses, categories, subscriptions ->
        FinanceRawData(prefs.currencyCode, prefs.currencySymbol, expenses, categories, subscriptions)
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        dataFlow,
        searchQueryFlow,
        selectedCategoryFlow,
        rangeFilterFlow,
        budgetAlertFlow
    ) { data, query, selectedCatId, range, alert ->
        val today = container.timeProvider.today()
        val todayIso = today.format(SafeDateTime.ISO_DATE)
        val (monthStart, monthEnd) = ExpenseAnalytics.getMonthDateRange(today)
        val (weekStart, weekEnd) = ExpenseAnalytics.getWeekDateRange(today)

        val monthExpenses = ExpenseAnalytics.filterExpensesByDateRange(data.expenses, monthStart, monthEnd)
        val rangeExpenses = when (range) {
            ExpenseRangeFilter.THIS_MONTH -> monthExpenses
            ExpenseRangeFilter.THIS_WEEK -> ExpenseAnalytics.filterExpensesByDateRange(data.expenses, weekStart, weekEnd)
            ExpenseRangeFilter.ALL_TIME -> data.expenses
        }
        val filtered = rangeExpenses.filter { expense ->
            val matchesCat = selectedCatId == null || expense.categoryId == selectedCatId
            val matchesQuery = query.isBlank() ||
                expense.description.contains(query, ignoreCase = true) ||
                expense.paymentMethod.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }

        val daysSpan = when (range) {
            ExpenseRangeFilter.THIS_MONTH -> today.dayOfMonth
            ExpenseRangeFilter.THIS_WEEK -> 7
            ExpenseRangeFilter.ALL_TIME -> ExpenseAnalytics.calculateAllTimeDaysSpan(filtered, today)
        }

        val activeSubs = data.subscriptions.filter { it.isActive }
        FinanceUiState(
            currencyCode = data.currencyCode,
            currencySymbol = data.currencySymbol,
            expenses = data.expenses,
            filteredExpenses = filtered,
            categories = data.categories,
            subscriptions = data.subscriptions,
            activeSubscriptions = activeSubs,
            monthSpent = monthExpenses.sumOf { it.amount },
            todaySpent = data.expenses.filter { it.date == todayIso }.sumOf { it.amount },
            filteredStats = ExpenseAnalytics.calculateStats(filtered, data.categories, daysSpan),
            budgetStatuses = ExpenseAnalytics.calculateBudgetStatus(monthExpenses, data.categories),
            categoryBreakdown = ExpenseAnalytics.calculateCategorySpending(filtered, data.categories),
            dailyTrend = ExpenseAnalytics.calculateDailyTrend(data.expenses, 7, today),
            subscriptionStats = SubscriptionAnalytics.calculateStats(data.subscriptions, today),
            renewalCalendar = SubscriptionAnalytics.getUpcomingRenewals(
                subscriptions = data.subscriptions,
                days = 30,
                includeOverdue = true,
                today = today
            ),
            searchQuery = query,
            selectedCategoryId = selectedCatId,
            rangeFilter = range,
            lastBudgetAlert = alert
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState()
    )

    fun setSearchQuery(q: String) {
        searchQueryFlow.value = q
    }

    fun selectCategoryFilter(catId: Long?) {
        selectedCategoryFlow.value = catId
    }

    fun setRangeFilter(filter: ExpenseRangeFilter) {
        rangeFilterFlow.value = filter
    }

    fun clearBudgetAlert() {
        budgetAlertFlow.value = null
    }

    fun saveExpense(expense: Expense, isEdit: Boolean) {
        viewModelScope.launch {
            val outcome = container.financeUseCases.addOrUpdateExpenseWithBudgetCheck(expense, isEdit)
            if (outcome.alertLevel != me.aliahad.timemanager.domain.usecase.BudgetAlertLevel.NONE) {
                budgetAlertFlow.value = outcome
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            container.financeRepository.deleteExpense(expense)
        }
    }

    fun saveExpenseCategory(category: ExpenseCategory, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                container.financeRepository.updateExpenseCategory(category)
            } else {
                container.financeRepository.insertExpenseCategory(category)
            }
        }
    }

    fun deleteExpenseCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            container.financeRepository.deleteExpenseCategory(category)
        }
    }

    fun saveSubscription(subscription: Subscription, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                container.financeRepository.updateSubscription(subscription)
            } else {
                container.financeRepository.insertSubscription(subscription)
            }
        }
    }

    fun renewSubscription(subscription: Subscription, logExpense: Boolean) {
        viewModelScope.launch {
            val result = container.financeUseCases.renewSubscriptionAndMaybeLogExpense(
                subscription = subscription,
                forceLogExpense = logExpense
            )
            if (result.budgetOutcome.alertLevel != me.aliahad.timemanager.domain.usecase.BudgetAlertLevel.NONE) {
                budgetAlertFlow.value = result.budgetOutcome
            }
        }
    }

    fun toggleSubscriptionActive(subscription: Subscription) {
        viewModelScope.launch {
            container.financeRepository.toggleSubscriptionActive(subscription)
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            container.financeRepository.deleteSubscription(subscription)
        }
    }

    private data class FinanceRawData(
        val currencyCode: String,
        val currencySymbol: String,
        val expenses: List<Expense>,
        val categories: List<ExpenseCategory>,
        val subscriptions: List<Subscription>
    )
}
