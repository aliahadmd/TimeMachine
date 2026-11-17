package me.aliahad.timemanager.domain.usecase

import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitDao
import me.aliahad.timemanager.data.HabitLinkType
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.domain.gateway.NoOpSystemNotificationGateway
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway

class HabitSyncUseCase(
    private val habitDao: HabitDao,
    private val habitRepository: HabitRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val notificationGateway: SystemNotificationGateway = NoOpSystemNotificationGateway
) {
    suspend fun recordCompletion(
        habitId: Long,
        date: String = timeProvider.today().format(SafeDateTime.ISO_DATE),
        completionType: CompletionType = CompletionType.ACHIEVED,
        notes: String = ""
    ) {
        habitRepository.addCompletion(
            habitId = habitId,
            date = date,
            completionType = completionType,
            notes = notes
        )
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun removeCompletion(habitId: Long, date: String) {
        habitRepository.removeCompletion(habitId, date)
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun saveHabitWithReminderSync(habit: Habit, isEdit: Boolean): Long {
        val savedId = if (isEdit) {
            habitRepository.updateHabit(habit)
            habit.id
        } else {
            habitRepository.insertHabit(habit)
        }
        val persisted = habit.copy(id = savedId)
        notificationGateway.syncHabitReminder(persisted)
        notificationGateway.requestWidgetRefresh()
        return savedId
    }

    suspend fun archiveHabitWithCleanup(habitId: Long) {
        habitRepository.archiveHabit(habitId)
        notificationGateway.cancelHabitReminder(habitId)
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun deleteHabitPermanentlyWithCleanup(habit: Habit) {
        habitRepository.deleteHabitPermanently(habit)
        notificationGateway.cancelHabitReminder(habit.id)
        notificationGateway.requestWidgetRefresh()
    }

    /**
     * Finalizes closed past days (`date < today`) for habits linked to
     * [HabitLinkType.SCREEN_TIME_UNDER_LIMIT]. Never auto-marks today mid-day,
     * avoiding false positives before bedtime.
     */
    suspend fun syncFinalizedScreenTimeDay(
        date: String,
        screenTimeMinutesForDay: Int,
        defaultLimitMinutes: Int
    ): List<String> {
        val parsedDate = SafeDateTime.parseLocalDateOrNull(date) ?: return emptyList()
        if (!parsedDate.isBefore(timeProvider.today())) return emptyList()

        val activeHabits = habitDao.getAllActiveHabitsSync()
        val autoCompleted = mutableListOf<String>()

        for (habit in activeHabits) {
            if (habit.linkType != HabitLinkType.SCREEN_TIME_UNDER_LIMIT) continue
            if (!HabitRepository.isScheduledOnDate(habit, parsedDate)) continue
            if (habitDao.getCompletion(habit.id, date) != null) continue

            val targetLimit = if (habit.linkedTargetValue > 0) habit.linkedTargetValue else defaultLimitMinutes
            if (screenTimeMinutesForDay <= targetLimit) {
                habitRepository.addCompletion(
                    habitId = habit.id,
                    date = date,
                    completionType = CompletionType.ACHIEVED,
                    notes = "Under ${targetLimit}m screen time limit (${screenTimeMinutesForDay}m used)"
                )
                autoCompleted += habit.name
            }
        }

        if (autoCompleted.isNotEmpty()) {
            notificationGateway.requestWidgetRefresh()
        }
        return autoCompleted
    }
}
