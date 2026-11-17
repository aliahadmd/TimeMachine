package me.aliahad.timemanager.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.aliahad.timemanager.calculateStreaks
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.Preset
import me.aliahad.timemanager.data.StreakInfo
import me.aliahad.timemanager.data.TimeSession
import me.aliahad.timemanager.domain.usecase.FocusSaveResult
import me.aliahad.timemanager.focus.FocusTimerService
import me.aliahad.timemanager.focus.FocusTimerUiState
import me.aliahad.timemanager.system.AppShortcutsManager
import java.time.YearMonth

data class FocusUiState(
    val timerState: FocusTimerUiState = FocusTimerUiState(),
    val categories: List<ActivityCategory> = emptyList(),
    val selectedCategoryId: Long = 0L,
    val presets: List<Preset> = emptyList(),
    val allSessions: List<TimeSession> = emptyList(),
    val todayFocusMinutes: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val streakInfo: StreakInfo = StreakInfo(0, 0, 0, 0f),
    val currentCalendarMonth: YearMonth = YearMonth.now(),
    val linkedTaskId: Long? = null,
    val manualSaveFeedback: FocusSaveResult? = null
)

class FocusViewModel(
    private val container: AppContainer,
    initialCategoryId: Long? = null,
    initialTaskId: Long? = null
) : ViewModel() {

    private val selectedCategoryIdFlow = MutableStateFlow(initialCategoryId ?: 0L)
    private val linkedTaskIdFlow = MutableStateFlow(initialTaskId?.takeIf { it > 0L })
    private val calendarMonthFlow = MutableStateFlow(YearMonth.now())
    private val streakInfoFlow = MutableStateFlow(StreakInfo(0, 0, 0, 0f))
    private val manualSaveFeedbackFlow = MutableStateFlow<FocusSaveResult?>(null)

    init {
        viewModelScope.launch {
            FocusTimerService.restoreIfRunning(container.appContext, container.preferencesRepository)
            container.focusRepository.ensureDefaultFocusDataIfNeeded(container.preferencesRepository)
        }
        refreshStreaks()
    }

    private val baseFlow = combine(
        FocusTimerService.timerState,
        container.focusRepository.activeCategories,
        container.focusRepository.allPresets,
        container.focusRepository.allSessions,
        container.focusRepository.getTodayFocusMinutesFlow()
    ) { timer, categories, presets, sessions, todayMin ->
        if (categories.isNotEmpty()) {
            AppShortcutsManager.syncDynamicShortcuts(container.appContext, categories)
        }
        FocusDataTuple(timer, categories, presets, sessions, todayMin)
    }

    val uiState: StateFlow<FocusUiState> = combine(
        baseFlow,
        selectedCategoryIdFlow,
        linkedTaskIdFlow,
        calendarMonthFlow,
        container.preferencesRepository.preferencesFlow
    ) { tuple, selectedCat, linkedTask, calMonth, prefs ->
        val effectiveCat = if (selectedCat > 0L && tuple.categories.any { it.id == selectedCat }) {
            selectedCat
        } else {
            tuple.categories.firstOrNull()?.id ?: 0L
        }
        val activeDates = tuple.sessions.map { it.date }.distinct()
        val computedStreak = calculateStreaks(
            activeDates,
            30,
            container.timeProvider.today()
        )
        FocusUiState(
            timerState = tuple.timer,
            categories = tuple.categories,
            selectedCategoryId = effectiveCat,
            presets = tuple.presets,
            allSessions = tuple.sessions,
            todayFocusMinutes = tuple.todayMinutes,
            dailyGoalMinutes = prefs.dailyFocusGoalMinutes,
            streakInfo = computedStreak,
            currentCalendarMonth = calMonth,
            linkedTaskId = linkedTask,
            manualSaveFeedback = manualSaveFeedbackFlow.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState()
    )

    fun refreshStreaks() {
        viewModelScope.launch {
            streakInfoFlow.value = container.focusRepository.getStreakInfo()
        }
    }

    fun selectCategory(categoryId: Long) {
        selectedCategoryIdFlow.value = categoryId
    }

    fun clearLinkedTask() {
        linkedTaskIdFlow.value = null
    }

    fun stepCalendarMonth(deltaMonths: Long) {
        calendarMonthFlow.value = calendarMonthFlow.value.plusMonths(deltaMonths)
    }

    fun startFocusTimer(
        category: ActivityCategory,
        targetDurationSeconds: Int,
        isPomodoro: Boolean,
        breakDurationSeconds: Int = 300
    ) {
        FocusTimerService.startSession(
            context = container.appContext,
            categoryId = category.id,
            categoryName = category.name,
            categoryIcon = category.icon,
            categoryColor = category.color.toComposeColor(),
            targetDurationSeconds = targetDurationSeconds,
            isPomodoro = isPomodoro,
            breakDurationSeconds = breakDurationSeconds,
            linkedTaskId = linkedTaskIdFlow.value ?: 0L
        )
    }

    fun pauseTimer() = FocusTimerService.sendAction(container.appContext, FocusTimerService.ACTION_PAUSE)
    fun resumeTimer() = FocusTimerService.sendAction(container.appContext, FocusTimerService.ACTION_RESUME)
    fun addFiveMinutes() = FocusTimerService.sendAction(container.appContext, FocusTimerService.ACTION_ADD_FIVE_MIN)
    fun finishAndSaveTimer() = FocusTimerService.sendAction(container.appContext, FocusTimerService.ACTION_STOP_AND_SAVE)
    fun skipBreak() = FocusTimerService.sendAction(container.appContext, FocusTimerService.ACTION_SKIP_BREAK)
    fun discardTimer() = FocusTimerService.sendAction(container.appContext, FocusTimerService.ACTION_DISCARD)
    fun consumeSaveFeedback() {
        FocusTimerService.consumeLastSavedResult()
        manualSaveFeedbackFlow.value = null
    }

    fun logManualSession(categoryId: Long, durationMinutes: Int, notes: String, dateIso: String) {
        viewModelScope.launch {
            val endWall = container.timeProvider.nowMillis()
            val startWall = endWall - durationMinutes.coerceAtLeast(1) * 60_000L
            val parsedDate = SafeDateTime.parseLocalDateOrNull(dateIso)
            val result = if (parsedDate == null || parsedDate == container.timeProvider.today()) {
                container.focusCompletionUseCase.saveSessionAndPropagate(
                    categoryId = categoryId,
                    startTimeWallMs = startWall,
                    endTimeWallMs = endWall,
                    durationMinutes = durationMinutes,
                    notes = notes,
                    linkedTaskId = linkedTaskIdFlow.value
                )
            } else {
                val id = container.focusRepository.insertSession(
                    TimeSession(
                        categoryId = categoryId,
                        startTime = startWall,
                        endTime = endWall,
                        durationMinutes = durationMinutes.coerceAtLeast(1),
                        date = dateIso,
                        notes = notes.trim(),
                        linkedTaskId = linkedTaskIdFlow.value
                    )
                )
                FocusSaveResult(id, null, emptyList())
            }
            manualSaveFeedbackFlow.value = result
        }
    }

    fun updateSession(session: TimeSession) {
        viewModelScope.launch {
            container.focusRepository.updateSession(session)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            container.focusRepository.deleteSessionById(sessionId)
        }
    }

    fun saveCategory(category: ActivityCategory, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                container.focusRepository.updateCategory(category)
            } else {
                val id = container.focusRepository.insertCategory(category)
                selectedCategoryIdFlow.value = id
            }
        }
    }

    fun archiveCategory(categoryId: Long) {
        viewModelScope.launch {
            container.focusRepository.archiveCategory(categoryId)
        }
    }

    fun savePreset(name: String, minutes: Int, breakMinutes: Int, isPomodoro: Boolean, existing: Preset?) {
        viewModelScope.launch {
            val safeTotalMinutes = minutes.coerceIn(1, 720)
            val clean = Preset(
                id = existing?.id ?: 0L,
                name = name.trim(),
                hours = safeTotalMinutes / 60,
                minutes = safeTotalMinutes % 60,
                breakMinutes = breakMinutes.coerceIn(1, 60),
                isPomodoro = isPomodoro
            )
            if (existing == null) {
                container.focusRepository.insertPreset(clean)
            } else {
                container.focusRepository.updatePreset(clean)
            }
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            container.focusRepository.deletePreset(preset)
        }
    }

    private data class FocusDataTuple(
        val timer: FocusTimerUiState,
        val categories: List<ActivityCategory>,
        val presets: List<Preset>,
        val sessions: List<TimeSession>,
        val todayMinutes: Int
    )
}
