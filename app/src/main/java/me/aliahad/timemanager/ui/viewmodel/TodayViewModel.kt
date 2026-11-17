package me.aliahad.timemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.aliahad.timemanager.DailyPlannerUtils
import me.aliahad.timemanager.ExpenseAnalytics
import me.aliahad.timemanager.SubscriptionAnalytics
import me.aliahad.timemanager.calculateDateDifference
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.datastore.UserPreferences
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.HabitWithStats
import me.aliahad.timemanager.data.Subscription
import me.aliahad.timemanager.permissions.DeviceReliabilityManager
import me.aliahad.timemanager.permissions.HealthSeverity
import me.aliahad.timemanager.permissions.ReliabilityCheckItem
import java.time.LocalDate

data class UpcomingMilestoneSummary(
    val title: String,
    val category: String,
    val daysUntil: Long
)

data class TodayUiState(
    val preferences: UserPreferences = UserPreferences(),
    val todayFocusMinutes: Int = 0,
    val todayScreenTimeMinutes: Int = 0,
    val todayPickups: Int = 0,
    val currentTask: DailyTask? = null,
    val nextTask: DailyTask? = null,
    val todayTasksCompleted: Int = 0,
    val todayTasksTotal: Int = 0,
    val scheduledHabitsToday: List<HabitWithStats> = emptyList(),
    val habitsDoneToday: Int = 0,
    val todaySpent: Double = 0.0,
    val monthSpent: Double = 0.0,
    val topBudgetStatus: ExpenseAnalytics.BudgetStatus? = null,
    val nextRenewal: Subscription? = null,
    val nextRenewalDays: Long = Long.MAX_VALUE,
    val upcomingMilestone: UpcomingMilestoneSummary? = null,
    val healthIssues: List<ReliabilityCheckItem> = emptyList(),
    val bannerDismissed: Boolean = false
)

class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val healthChecksFlow = MutableStateFlow<List<ReliabilityCheckItem>>(emptyList())
    private val bannerDismissedFlow = MutableStateFlow(false)

    init {
        refreshDeviceHealth()
    }

    private val coreFlow = combine(
        container.preferencesRepository.preferencesFlow,
        container.focusRepository.getTodayFocusMinutesFlow(),
        container.screenTimeRepository.getTodaySummaryFlow(),
        container.plannerRepository.getTasksForDate(
            container.timeProvider.today().format(SafeDateTime.ISO_DATE)
        ),
        container.habitRepository.getAllActiveHabitsWithStats()
    ) { prefs, focusMin, screenSummary, tasks, habits ->
        CoreTodaySlice(
            prefs = prefs,
            focusMin = focusMin,
            screenMinutes = (screenSummary?.totalScreenTimeSeconds ?: 0) / 60,
            pickups = screenSummary?.pickupsCount ?: 0,
            tasks = tasks,
            habits = habits
        )
    }

    private val financeAndMilestonesFlow = combine(
        container.financeRepository.allExpenses,
        container.financeRepository.activeExpenseCategories,
        container.financeRepository.activeSubscriptions,
        container.toolsRepository.allDateCalculations
    ) { expenses, categories, subs, calcs ->
        FinanceMilestoneSlice(expenses, categories, subs, calcs)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        coreFlow,
        financeAndMilestonesFlow,
        healthChecksFlow,
        bannerDismissedFlow
    ) { core, finSlice, healthChecks, dismissed ->
        val expenses = finSlice.expenses
        val categories = finSlice.categories
        val subs = finSlice.subs
        val today = container.timeProvider.today()
        val todayIso = today.format(SafeDateTime.ISO_DATE)
        val (monthStart, monthEnd) = ExpenseAnalytics.getMonthDateRange(today)

        val currentTask = core.tasks.firstOrNull { !it.isCompleted && DailyPlannerUtils.isTaskNow(it, today) }
        val nextTask = core.tasks.firstOrNull { !it.isCompleted && it.id != currentTask?.id }

        val scheduledHabits = core.habits.filter { HabitRepository.isScheduledOnDate(it.habit, today) }
        val habitsDone = scheduledHabits.count { it.todayCompletion?.completionType == CompletionType.ACHIEVED }

        val todayExpenses = expenses.filter { it.date == todayIso }
        val monthExpenses = ExpenseAnalytics.filterExpensesByDateRange(expenses, monthStart, monthEnd)
        val budgets = ExpenseAnalytics.calculateBudgetStatus(monthExpenses, categories)

        val nearestSub = subs.minByOrNull { SubscriptionAnalytics.daysUntilBilling(it.nextBillingDate, today) }
        val nearestDays = nearestSub?.let { SubscriptionAnalytics.daysUntilBilling(it.nextBillingDate, today) }
            ?: Long.MAX_VALUE

        val nextMilestone = finSlice.calcs.mapNotNull { item ->
            val startLocal = SafeDateTime.epochMillisToLocalDate(item.startDate, container.timeProvider.zoneId())
            val diff = calculateDateDifference(startLocal, today)
            val daysUntil = diff.daysUntilNext ?: return@mapNotNull null
            if (daysUntil in 0..7) UpcomingMilestoneSummary(item.title, item.category, daysUntil) else null
        }.minByOrNull { it.daysUntil }

        val attentionChecks = healthChecks.filter { !it.isReady && it.severity != HealthSeverity.HEALTHY }

        TodayUiState(
            preferences = core.prefs,
            todayFocusMinutes = core.focusMin,
            todayScreenTimeMinutes = core.screenMinutes,
            todayPickups = core.pickups,
            currentTask = currentTask,
            nextTask = nextTask,
            todayTasksCompleted = core.tasks.count { it.isCompleted },
            todayTasksTotal = core.tasks.size,
            scheduledHabitsToday = scheduledHabits,
            habitsDoneToday = habitsDone,
            todaySpent = todayExpenses.sumOf { it.amount },
            monthSpent = monthExpenses.sumOf { it.amount },
            topBudgetStatus = budgets.firstOrNull(),
            nextRenewal = nearestSub,
            nextRenewalDays = nearestDays,
            upcomingMilestone = nextMilestone,
            healthIssues = attentionChecks,
            bannerDismissed = dismissed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    fun refreshDeviceHealth() {
        healthChecksFlow.value = DeviceReliabilityManager.inspectDeviceHealth(container.appContext)
    }

    fun dismissHealthBanner() {
        bannerDismissedFlow.value = true
    }

    fun toggleHabitToday(habitWithStats: HabitWithStats) {
        viewModelScope.launch {
            val todayIso = container.timeProvider.today().format(SafeDateTime.ISO_DATE)
            if (habitWithStats.isCompletedToday) {
                container.habitSyncUseCase.removeCompletion(habitWithStats.habit.id, todayIso)
            } else {
                container.habitSyncUseCase.recordCompletion(
                    habitId = habitWithStats.habit.id,
                    date = todayIso,
                    completionType = CompletionType.ACHIEVED
                )
            }
        }
    }

    fun toggleTaskCompleted(task: DailyTask) {
        viewModelScope.launch {
            container.plannerRepository.toggleTaskCompletion(task)
        }
    }

    private data class CoreTodaySlice(
        val prefs: UserPreferences,
        val focusMin: Int,
        val screenMinutes: Int,
        val pickups: Int,
        val tasks: List<DailyTask>,
        val habits: List<HabitWithStats>
    )

    private data class FinanceMilestoneSlice(
        val expenses: List<me.aliahad.timemanager.data.Expense>,
        val categories: List<me.aliahad.timemanager.data.ExpenseCategory>,
        val subs: List<Subscription>,
        val calcs: List<me.aliahad.timemanager.data.DateCalculation>
    )
}
