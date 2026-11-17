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
import me.aliahad.timemanager.DailyPlannerUtils
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.DailyTask
import java.time.LocalDate

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateIso: String = LocalDate.now().format(SafeDateTime.ISO_DATE),
    val tasks: List<DailyTask> = emptyList(),
    val overlappingTaskIds: Set<Long> = emptySet(),
    val activeFocusCategories: List<ActivityCategory> = emptyList(),
    val filterMode: PlannerFilterMode = PlannerFilterMode.ALL
) {
    val completedCount: Int get() = tasks.count { it.isCompleted }
    val totalScheduledMinutes: Int
        get() = tasks.sumOf { DailyPlannerUtils.calculateDuration(it.startTime, it.endTime) }
}

enum class PlannerFilterMode(val label: String) {
    ALL("All"),
    PENDING("Remaining"),
    FOCUS("Focus"),
    DONE("Done")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val container: AppContainer,
    initialDateIso: String? = null
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(
        SafeDateTime.parseLocalDateOrNull(initialDateIso) ?: container.timeProvider.today()
    )
    private val filterModeFlow = MutableStateFlow(PlannerFilterMode.ALL)

    init {
        viewModelScope.launch {
            container.plannerRepository.seedStarterPlanIfNeeded()
        }
    }

    private val tasksForSelectedDateFlow = selectedDateFlow.flatMapLatest { date ->
        container.plannerRepository.getTasksForDate(date.format(SafeDateTime.ISO_DATE))
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        selectedDateFlow,
        tasksForSelectedDateFlow,
        container.focusRepository.activeCategories,
        filterModeFlow
    ) { date, rawTasks, categories, filter ->
        val dateIso = date.format(SafeDateTime.ISO_DATE)
        val overlaps = mutableSetOf<Long>()
        for (i in rawTasks.indices) {
            for (j in i + 1 until rawTasks.size) {
                if (DailyPlannerUtils.isTaskOverlapping(rawTasks[i], rawTasks[j])) {
                    overlaps += rawTasks[i].id
                    overlaps += rawTasks[j].id
                }
            }
        }
        val filtered = when (filter) {
            PlannerFilterMode.ALL -> rawTasks
            PlannerFilterMode.PENDING -> rawTasks.filter { !it.isCompleted }
            PlannerFilterMode.FOCUS -> rawTasks.filter { it.taskType.equals("FOCUS", ignoreCase = true) }
            PlannerFilterMode.DONE -> rawTasks.filter { it.isCompleted }
        }
        PlannerUiState(
            selectedDate = date,
            selectedDateIso = dateIso,
            tasks = filtered,
            overlappingTaskIds = overlaps,
            activeFocusCategories = categories,
            filterMode = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlannerUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun stepDate(days: Long) {
        selectedDateFlow.value = selectedDateFlow.value.plusDays(days)
    }

    fun setFilter(mode: PlannerFilterMode) {
        filterModeFlow.value = mode
    }

    fun saveTask(task: DailyTask, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                container.plannerRepository.updateTask(task)
            } else {
                container.plannerRepository.insertTask(task)
            }
        }
    }

    fun toggleTaskCompletion(task: DailyTask) {
        viewModelScope.launch {
            container.plannerRepository.toggleTaskCompletion(task)
        }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch {
            container.plannerRepository.deleteTask(task)
        }
    }
}
