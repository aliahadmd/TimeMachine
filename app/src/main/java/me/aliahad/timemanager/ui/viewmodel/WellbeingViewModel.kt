package me.aliahad.timemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.aliahad.timemanager.ScreenTimePermissionHelper
import me.aliahad.timemanager.ScreenTimeScheduler
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.AppUsageSummary
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitCompletion
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.HabitWithStats
import me.aliahad.timemanager.data.ScreenTimeDailySummary
import me.aliahad.timemanager.data.ScreenTimeHourly
import java.time.LocalDate

data class WellbeingUiState(
    val habitsWithStats: List<HabitWithStats> = emptyList(),
    val scheduledTodayCount: Int = 0,
    val completedTodayCount: Int = 0,
    val activeFocusCategories: List<ActivityCategory> = emptyList(),
    val dailyScreenLimitMinutes: Int = 240,
    val hasUsageAccess: Boolean = false,
    val isRestrictedSideload: Boolean = false,
    val selectedScreenDate: LocalDate = LocalDate.now(),
    val screenSummary: ScreenTimeDailySummary? = null,
    val hourlyBreakdown: List<ScreenTimeHourly> = emptyList(),
    val recentSummaries: List<ScreenTimeDailySummary> = emptyList(),
    val appUsage: List<AppUsageSummary> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class WellbeingViewModel(private val container: AppContainer) : ViewModel() {

    private val selectedScreenDateFlow = MutableStateFlow(container.timeProvider.today())
    private val usageAccessFlow = MutableStateFlow(ScreenTimePermissionHelper.hasUsageAccess(container.appContext))
    private val restrictedSideloadFlow = MutableStateFlow(
        ScreenTimePermissionHelper.isLikelyRestrictedSettingsBlocked(container.appContext)
    )

    private val screenSummaryFlow = selectedScreenDateFlow.flatMapLatest { date ->
        container.screenTimeRepository.getSummaryForDateFlow(date.format(SafeDateTime.ISO_DATE))
    }

    private val screenHourlyFlow = selectedScreenDateFlow.flatMapLatest { date ->
        container.screenTimeRepository.getHourlyForDateFlow(date.format(SafeDateTime.ISO_DATE))
    }

    private val appUsageFlow = selectedScreenDateFlow.flatMapLatest { date ->
        container.screenTimeRepository.getAppUsageForDateFlow(date.format(SafeDateTime.ISO_DATE))
    }

    private val habitSliceFlow = combine(
        container.habitRepository.getAllActiveHabitsWithStats(),
        container.focusRepository.activeCategories,
        container.preferencesRepository.preferencesFlow,
        usageAccessFlow,
        restrictedSideloadFlow
    ) { habits, focusCats, prefs, hasAccess, sideloadBlocked ->
        HabitSlice(habits, focusCats, prefs.dailyScreenTimeLimitMinutes, hasAccess, sideloadBlocked)
    }

    private val screenSliceFlow = combine(
        selectedScreenDateFlow,
        screenSummaryFlow,
        screenHourlyFlow,
        container.screenTimeRepository.getRecentDailySummaries(7),
        appUsageFlow
    ) { date, summary, hourly, recent, apps ->
        ScreenSlice(date, summary, hourly, recent, apps)
    }

    val uiState: StateFlow<WellbeingUiState> = combine(
        habitSliceFlow,
        screenSliceFlow
    ) { habitSlice, screenSlice ->
        val today = container.timeProvider.today()
        val scheduledToday = habitSlice.habits.filter { HabitRepository.isScheduledOnDate(it.habit, today) }
        val doneToday = scheduledToday.count { it.isCompletedToday }

        WellbeingUiState(
            habitsWithStats = habitSlice.habits,
            scheduledTodayCount = scheduledToday.size,
            completedTodayCount = doneToday,
            activeFocusCategories = habitSlice.focusCats,
            dailyScreenLimitMinutes = habitSlice.dailyScreenLimitMinutes,
            hasUsageAccess = habitSlice.hasAccess,
            isRestrictedSideload = habitSlice.sideloadBlocked,
            selectedScreenDate = screenSlice.date,
            screenSummary = screenSlice.summary,
            hourlyBreakdown = screenSlice.hourly,
            recentSummaries = screenSlice.recent,
            appUsage = screenSlice.apps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WellbeingUiState()
    )

    fun refreshUsagePermissionAndSync() {
        val granted = ScreenTimePermissionHelper.hasUsageAccess(container.appContext)
        usageAccessFlow.value = granted
        restrictedSideloadFlow.value =
            ScreenTimePermissionHelper.isLikelyRestrictedSettingsBlocked(container.appContext)
        if (granted) {
            ScreenTimeScheduler.triggerImmediateSync(container.appContext)
        }
    }

    fun selectScreenDate(date: LocalDate) {
        selectedScreenDateFlow.value = date
    }

    fun saveHabit(habit: Habit, isEdit: Boolean) {
        viewModelScope.launch {
            container.habitSyncUseCase.saveHabitWithReminderSync(habit, isEdit)
        }
    }

    fun toggleHabitCompletionToday(habitWithStats: HabitWithStats) {
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

    fun submitHabitWithNotes(
        habitId: Long,
        dateIso: String,
        completionType: CompletionType,
        notes: String
    ) {
        viewModelScope.launch {
            container.habitSyncUseCase.recordCompletion(
                habitId = habitId,
                date = dateIso,
                completionType = completionType,
                notes = notes
            )
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            container.habitSyncUseCase.archiveHabitWithCleanup(habitId)
        }
    }

    private data class HabitSlice(
        val habits: List<HabitWithStats>,
        val focusCats: List<ActivityCategory>,
        val dailyScreenLimitMinutes: Int,
        val hasAccess: Boolean,
        val sideloadBlocked: Boolean
    )

    private data class ScreenSlice(
        val date: LocalDate,
        val summary: ScreenTimeDailySummary?,
        val hourly: List<ScreenTimeHourly>,
        val recent: List<ScreenTimeDailySummary>,
        val apps: List<AppUsageSummary>
    )
}

class HabitDetailViewModel(
    private val container: AppContainer,
    private val habitId: Long
) : ViewModel() {

    val habitWithStats: StateFlow<HabitWithStats?> = container.habitRepository
        .getHabitWithStats(habitId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val completions: StateFlow<List<HabitCompletion>> = container.habitRepository
        .getHabitCompletions(habitId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeFocusCategories: StateFlow<List<ActivityCategory>> = container.focusRepository
        .activeCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun saveCompletionForDate(dateIso: String, type: CompletionType, notes: String) {
        viewModelScope.launch {
            container.habitSyncUseCase.recordCompletion(habitId, dateIso, type, notes)
        }
    }

    fun clearCompletionForDate(dateIso: String) {
        viewModelScope.launch {
            container.habitSyncUseCase.removeCompletion(habitId, dateIso)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            container.habitSyncUseCase.saveHabitWithReminderSync(habit, isEdit = true)
        }
    }

    fun archiveHabit() {
        viewModelScope.launch {
            container.habitSyncUseCase.archiveHabitWithCleanup(habitId)
        }
    }

    fun deletePermanently(habit: Habit) {
        viewModelScope.launch {
            container.habitSyncUseCase.deleteHabitPermanentlyWithCleanup(habit)
        }
    }
}
