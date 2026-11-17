package me.aliahad.timemanager.domain.usecase

import me.aliahad.timemanager.core.common.ImmediateTransactionRunner
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.core.common.TransactionRunner
import me.aliahad.timemanager.data.ActivityCategoryDao
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.DailyTaskDao
import me.aliahad.timemanager.data.HabitCompletion
import me.aliahad.timemanager.data.HabitDao
import me.aliahad.timemanager.data.HabitLinkType
import me.aliahad.timemanager.data.TimeSession
import me.aliahad.timemanager.data.TimeSessionDao
import me.aliahad.timemanager.domain.gateway.NoOpSystemNotificationGateway
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway
import java.time.Instant

data class FocusSaveResult(
    val sessionId: Long,
    val markedTaskTitle: String?,
    val autoCompletedHabits: List<String>
)

class FocusCompletionUseCase(
    private val transactionRunner: TransactionRunner = ImmediateTransactionRunner,
    private val activityCategoryDao: ActivityCategoryDao,
    private val timeSessionDao: TimeSessionDao,
    private val dailyTaskDao: DailyTaskDao,
    private val habitDao: HabitDao,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val notificationGateway: SystemNotificationGateway = NoOpSystemNotificationGateway
) {
    suspend fun saveSessionAndPropagate(
        categoryId: Long,
        startTimeWallMs: Long,
        endTimeWallMs: Long,
        durationMinutes: Int,
        notes: String = "",
        linkedTaskId: Long? = null
    ): FocusSaveResult {
        val safeDuration = durationMinutes.coerceAtLeast(1)
        val validEndMs = endTimeWallMs.coerceAtLeast(startTimeWallMs)
        val sessionDate = Instant.ofEpochMilli(validEndMs)
            .atZone(timeProvider.zoneId())
            .toLocalDate()
            .format(SafeDateTime.ISO_DATE)
        val cleanTaskId = linkedTaskId?.takeIf { it > 0L }

        var categoryName = "Focus"
        var markedTaskIdToCancel: Long? = null
        var markedTaskTitle: String? = null
        val completedHabits = mutableListOf<Pair<Long, String>>()

        val sessionId = transactionRunner.runInTransaction {
            val category = activityCategoryDao.getCategoryById(categoryId)
            if (category != null) {
                categoryName = category.name
            }

            val insertedId = timeSessionDao.insertSession(
                TimeSession(
                    categoryId = categoryId,
                    startTime = startTimeWallMs,
                    endTime = validEndMs,
                    durationMinutes = safeDuration,
                    date = sessionDate,
                    notes = notes.trim(),
                    linkedTaskId = cleanTaskId,
                    createdAt = timeProvider.nowMillis()
                )
            )

            if (cleanTaskId != null) {
                val linkedTask = dailyTaskDao.getTaskById(cleanTaskId)
                if (linkedTask != null && !linkedTask.isCompleted) {
                    dailyTaskDao.updateTask(
                        linkedTask.copy(
                            isCompleted = true,
                            linkedSessionId = insertedId
                        )
                    )
                    markedTaskIdToCancel = linkedTask.id
                    markedTaskTitle = linkedTask.title
                }
            }

            val activeHabits = habitDao.getAllActiveHabitsSync()
            for (habit in activeHabits) {
                if (habit.linkType != HabitLinkType.FOCUS_MINUTES) continue
                val matchesCategory = habit.linkedFocusCategoryId == null ||
                    habit.linkedFocusCategoryId == categoryId
                if (!matchesCategory) continue

                val existing = habitDao.getCompletion(habit.id, sessionDate)
                if (existing != null) continue

                val totalMinutes = if (habit.linkedFocusCategoryId != null) {
                    timeSessionDao.getTotalMinutesForDateAndCategory(sessionDate, categoryId)
                } else {
                    timeSessionDao.getTotalMinutesForDate(sessionDate) ?: 0
                }
                val requiredMinutes = if (habit.linkedTargetValue > 0) habit.linkedTargetValue else 1
                if (totalMinutes >= requiredMinutes) {
                    habitDao.insertCompletion(
                        HabitCompletion(
                            habitId = habit.id,
                            date = sessionDate,
                            completionType = CompletionType.ACHIEVED,
                            completedAt = timeProvider.nowMillis(),
                            notes = "Auto-completed via Focus ($categoryName)"
                        )
                    )
                    completedHabits += habit.id to habit.name
                }
            }

            insertedId
        }

        markedTaskIdToCancel?.let { notificationGateway.cancelTaskReminder(it) }
        completedHabits.forEach { (habitId, _) ->
            notificationGateway.cancelHabitReminder(habitId)
        }
        val habitNames = completedHabits.map { it.second }
        notificationGateway.onFocusSessionSaved(categoryName, safeDuration, habitNames)
        notificationGateway.requestWidgetRefresh()

        return FocusSaveResult(
            sessionId = sessionId,
            markedTaskTitle = markedTaskTitle,
            autoCompletedHabits = habitNames
        )
    }
}
