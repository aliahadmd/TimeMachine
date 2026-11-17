package me.aliahad.timemanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.aliahad.timemanager.core.common.ImmediateTransactionRunner
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.ActivityCategoryDao
import me.aliahad.timemanager.data.BackupData
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.DailyStatsRaw
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.DailyTaskDao
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitCompletion
import me.aliahad.timemanager.data.HabitDao
import me.aliahad.timemanager.data.HabitLinkType
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.HabitType
import me.aliahad.timemanager.data.TimeSession
import me.aliahad.timemanager.data.TimeSessionDao
import me.aliahad.timemanager.domain.usecase.FocusCompletionUseCase
import me.aliahad.timemanager.domain.usecase.HabitSyncUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class UseCasesAndBackupParseTest {

    private class FakeTimeProvider(private val fixedDate: LocalDate) : TimeProvider {
        override fun today(): LocalDate = fixedDate
        override fun nowTime(): java.time.LocalTime = java.time.LocalTime.of(12, 0)
        override fun nowDateTime(): java.time.LocalDateTime = fixedDate.atTime(12, 0)
        override fun nowMillis(): Long = nowDateTime().atZone(zoneId()).toInstant().toEpochMilli()
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun elapsedRealtimeMillis(): Long = 100_000L
    }

    @Test
    fun focusCompletionUseCase_marksLinkedTaskDoneAndAutoCompletesLinkedHabit() = runTest {
        val today = LocalDate.of(2026, 11, 17)
        val todayIso = today.format(SafeDateTime.ISO_DATE)
        val tp = FakeTimeProvider(today)

        val fakeCatDao = FakeCategoryDao(
            mutableListOf(ActivityCategory(id = 1L, name = "Coding", icon = "💻", color = 0xFF0A66C2L))
        )
        val fakeSessions = FakeTimeSessionDao()
        val fakeTasks = FakeDailyTaskDao(
            mutableListOf(
                DailyTask(
                    id = 10L, title = "Ship API", date = todayIso,
                    startTime = "10:00", endTime = "11:00", taskType = "FOCUS", linkedCategoryId = 1L
                )
            )
        )
        val fakeHabits = FakeHabitDao(
            mutableListOf(
                Habit(
                    id = 5L, name = "Deep Work 30m", color = 0xFF16A34AL,
                    type = HabitType.BUILD, goalPeriodDays = 21, isEveryday = true,
                    linkType = HabitLinkType.FOCUS_MINUTES, linkedFocusCategoryId = 1L, linkedTargetValue = 30
                )
            )
        )

        val useCase = FocusCompletionUseCase(
            transactionRunner = ImmediateTransactionRunner,
            activityCategoryDao = fakeCatDao,
            timeSessionDao = fakeSessions,
            dailyTaskDao = fakeTasks,
            habitDao = fakeHabits,
            timeProvider = tp
        )

        val startMs = today.atTime(10, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val endMs = startMs + 35 * 60_000L
        val result = useCase.saveSessionAndPropagate(
            categoryId = 1L,
            startTimeWallMs = startMs,
            endTimeWallMs = endMs,
            durationMinutes = 35,
            notes = "Done cleanly",
            linkedTaskId = 10L
        )

        assertEquals("Ship API", result.markedTaskTitle)
        assertTrue(fakeTasks.getTaskById(10L)?.isCompleted == true)
        assertEquals(listOf("Deep Work 30m"), result.autoCompletedHabits)
        assertEquals(CompletionType.ACHIEVED, fakeHabits.getCompletion(5L, todayIso)?.completionType)
    }

    @Test
    fun habitSyncUseCase_onlyFinalizesClosedPastDaysForScreenTimeHabit() = runTest {
        val today = LocalDate.of(2026, 11, 17)
        val yesterdayIso = today.minusDays(1).format(SafeDateTime.ISO_DATE)
        val todayIso = today.format(SafeDateTime.ISO_DATE)
        val tp = FakeTimeProvider(today)

        val fakeHabits = FakeHabitDao(
            mutableListOf(
                Habit(
                    id = 7L, name = "Screen Under 2h", color = 0xFF7C3AEDL,
                    type = HabitType.BUILD, goalPeriodDays = 14, isEveryday = true,
                    linkType = HabitLinkType.SCREEN_TIME_UNDER_LIMIT, linkedTargetValue = 120
                )
            )
        )
        val syncUseCase = HabitSyncUseCase(
            habitDao = fakeHabits,
            habitRepository = HabitRepository(fakeHabits, tp),
            timeProvider = tp
        )

        // Never auto-complete today mid-day
        val todayResult = syncUseCase.syncFinalizedScreenTimeDay(todayIso, 30, 120)
        assertTrue(todayResult.isEmpty())

        // Yesterday under limit auto-completes once finalized
        val yesterdayResult = syncUseCase.syncFinalizedScreenTimeDay(yesterdayIso, 95, 120)
        assertEquals(listOf("Screen Under 2h"), yesterdayResult)
        assertNotNull(fakeHabits.getCompletion(7L, yesterdayIso))
    }

    @Test
    fun v1DemoBackupJson_decodesCleanlyIntoBackupDataSchema() {
        val candidates = listOf(
            File("../TimeMachine_DemoData_v3.0.0.json"),
            File("TimeMachine_DemoData_v3.0.0.json")
        )
        val demoFile = candidates.firstOrNull { it.exists() } ?: return
        val jsonParser = Json { ignoreUnknownKeys = true }
        val decoded = jsonParser.decodeFromString<BackupData>(demoFile.readText())
        assertEquals(1, decoded.version)
        assertTrue(decoded.focusTracker.categories.isNotEmpty())
        assertTrue(decoded.habitTracker.habits.isNotEmpty())
        assertTrue(decoded.expenseTracker.expenses.isNotEmpty())
    }

    private class FakeCategoryDao(private val list: MutableList<ActivityCategory>) : ActivityCategoryDao {
        override fun getAllActiveCategories(): Flow<List<ActivityCategory>> = MutableStateFlow(list)
        override fun getAllCategories(): Flow<List<ActivityCategory>> = MutableStateFlow(list)
        override suspend fun getCategoryById(id: Long): ActivityCategory? = list.find { it.id == id }
        override fun getCategoryByIdFlow(id: Long): Flow<ActivityCategory?> = MutableStateFlow(list.find { it.id == id })
        override suspend fun insertCategory(category: ActivityCategory): Long = category.id
        override suspend fun updateCategory(category: ActivityCategory) = Unit
        override suspend fun deleteCategory(category: ActivityCategory) = Unit
        override suspend fun archiveCategory(id: Long) = Unit
        override suspend fun getActiveCategoryCount(): Int = list.count { it.isActive }
        override suspend fun updateSortOrder(id: Long, sortOrder: Int) = Unit
        override suspend fun getAllCategoriesSync(): List<ActivityCategory> = list
    }

    private class FakeTimeSessionDao : TimeSessionDao {
        private val items = mutableListOf<TimeSession>()
        private var nextId = 1L
        override fun getAllSessions(): Flow<List<TimeSession>> = MutableStateFlow(items)
        override fun getSessionsByCategory(categoryId: Long): Flow<List<TimeSession>> = MutableStateFlow(items)
        override fun getSessionsByDate(date: String): Flow<List<TimeSession>> = MutableStateFlow(items)
        override fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<TimeSession>> = MutableStateFlow(items)
        override suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<TimeSession> = items
        override suspend fun getSessionById(id: Long): TimeSession? = items.find { it.id == id }
        override suspend fun insertSession(session: TimeSession): Long {
            val saved = session.copy(id = nextId++)
            items += saved
            return saved.id
        }
        override suspend fun updateSession(session: TimeSession) = Unit
        override suspend fun deleteSession(session: TimeSession) = Unit
        override suspend fun deleteSessionById(id: Long) = Unit
        override fun getTotalMinutesForDateFlow(date: String): Flow<Int> = MutableStateFlow(0)
        override suspend fun getTotalFocusMinutesSql(): Int = items.sumOf { it.durationMinutes }
        override suspend fun getTotalSessionCountSql(): Int = items.size
        override suspend fun getDailyStats(startDate: String, endDate: String): List<DailyStatsRaw> = emptyList()
        override suspend fun getTotalMinutesForDate(date: String): Int =
            items.filter { it.date == date }.sumOf { it.durationMinutes }
        override suspend fun getTotalMinutesForDateAndCategory(date: String, categoryId: Long): Int =
            items.filter { it.date == date && it.categoryId == categoryId }.sumOf { it.durationMinutes }
        override suspend fun getTotalMinutesForCategory(categoryId: Long): Int? = null
        override suspend fun getSessionCountForCategory(categoryId: Long): Int = 0
        override suspend fun getAverageSessionMinutesForCategory(categoryId: Long): Float? = null
        override suspend fun getLastSessionDateForCategory(categoryId: Long): String? = null
        override suspend fun getTotalActiveDays(): Int = 0
        override suspend fun getAllActiveDates(): List<String> = emptyList()
        override suspend fun getActiveDatesForCategory(categoryId: Long): List<String> = emptyList()
        override suspend fun getSessionCountForDate(date: String): Int = 0
        override suspend fun getAllSessionsSync(): List<TimeSession> = items
    }

    private class FakeDailyTaskDao(private val items: MutableList<DailyTask>) : DailyTaskDao {
        override suspend fun insertTask(task: DailyTask): Long = task.id
        override suspend fun updateTask(task: DailyTask) {
            val idx = items.indexOfFirst { it.id == task.id }
            if (idx >= 0) items[idx] = task
        }
        override suspend fun deleteTask(task: DailyTask) = Unit
        override fun getTasksForDate(date: String): Flow<List<DailyTask>> = MutableStateFlow(items)
        override suspend fun getTasksForDateSync(date: String): List<DailyTask> = items
        override suspend fun getTaskById(id: Long): DailyTask? = items.find { it.id == id }
        override fun getPendingTasksForDate(date: String): Flow<List<DailyTask>> = MutableStateFlow(items)
        override fun getCompletedTasksForDate(date: String): Flow<List<DailyTask>> = MutableStateFlow(items)
        override fun getTaskCountForDateFlow(date: String): Flow<Int> = MutableStateFlow(items.size)
        override fun getCompletedCountForDateFlow(date: String): Flow<Int> = MutableStateFlow(0)
        override suspend fun getTaskCountForDate(date: String): Int = items.size
        override suspend fun getCompletedCountForDate(date: String): Int = 0
        override suspend fun getTotalTaskCountSql(): Int = items.size
        override suspend fun getTotalCompletedTaskCountSql(): Int = 0
        override suspend fun getUpcomingTasksWithRemindersSync(fromDate: String): List<DailyTask> = emptyList()
        override fun getTasksByType(date: String, type: String): Flow<List<DailyTask>> = MutableStateFlow(items)
        override suspend fun getAllRecurringTasks(): List<DailyTask> = emptyList()
        override suspend fun deleteOldTasks(beforeDate: String) = Unit
        override suspend fun getAllTasks(): List<DailyTask> = items
    }

    private class FakeHabitDao(private val habits: MutableList<Habit>) : HabitDao {
        private val completions = MutableStateFlow<List<HabitCompletion>>(emptyList())
        override fun getAllActiveHabits(): Flow<List<Habit>> = MutableStateFlow(habits.filter { it.isActive })
        override suspend fun getAllActiveHabitsSync(): List<Habit> = habits.filter { it.isActive }
        override suspend fun getHabitById(habitId: Long): Habit? = habits.find { it.id == habitId }
        override fun getHabitByIdFlow(habitId: Long): Flow<Habit?> = MutableStateFlow(habits.find { it.id == habitId })
        override suspend fun insertHabit(habit: Habit): Long = habit.id
        override suspend fun updateHabit(habit: Habit) = Unit
        override suspend fun deleteHabit(habit: Habit) = Unit
        override suspend fun archiveHabit(habitId: Long) = Unit
        override fun getHabitCompletions(habitId: Long): Flow<List<HabitCompletion>> =
            completions.map { list -> list.filter { it.habitId == habitId } }
        override fun getAllCompletionsFlow(): Flow<List<HabitCompletion>> = completions
        override fun getCompletionsForDateFlow(date: String): Flow<List<HabitCompletion>> =
            completions.map { list -> list.filter { it.date == date } }
        override suspend fun getCompletion(habitId: Long, date: String): HabitCompletion? =
            completions.value.find { it.habitId == habitId && it.date == date }
        override suspend fun getCompletionsInRange(habitId: Long, startDate: String, endDate: String): List<HabitCompletion> =
            emptyList()
        override suspend fun insertCompletion(completion: HabitCompletion) {
            completions.value = completions.value.filterNot {
                it.habitId == completion.habitId && it.date == completion.date
            } + completion
        }
        override suspend fun deleteCompletion(completion: HabitCompletion) = Unit
        override suspend fun deleteCompletionByDate(habitId: Long, date: String) {
            completions.value = completions.value.filterNot { it.habitId == habitId && it.date == date }
        }
        override suspend fun getTotalCompletions(habitId: Long): Int = 0
        override suspend fun getTotalCompletionsCountSql(): Int = completions.value.size
        override suspend fun getTotalActiveAchievedCountSql(): Int = 0
        override suspend fun getTotalActiveSubmissionCountSql(): Int = 0
        override suspend fun getCompletionsSince(habitId: Long, startDate: String): Int = 0
        override suspend fun getAllHabitsSync(): List<Habit> = habits
        override suspend fun getAllCompletionsSync(): List<HabitCompletion> = completions.value
    }
}
