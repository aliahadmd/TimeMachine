package me.aliahad.timemanager.domain.gateway

import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.Habit

interface SystemNotificationGateway {
    suspend fun onFocusSessionSaved(
        categoryName: String,
        durationMinutes: Int,
        linkedHabitNames: List<String>
    )

    suspend fun notifyBudgetThreshold(
        categoryId: Long,
        categoryName: String,
        spent: Double,
        budget: Double,
        currencySymbol: String,
        isOverBudget: Boolean
    )

    suspend fun syncTaskReminder(task: DailyTask)
    suspend fun cancelTaskReminder(taskId: Long)
    suspend fun syncHabitReminder(habit: Habit)
    suspend fun cancelHabitReminder(habitId: Long)
    suspend fun syncAllReminders()
    suspend fun cancelAllReminders()
    fun requestWidgetRefresh()
}

object NoOpSystemNotificationGateway : SystemNotificationGateway {
    override suspend fun onFocusSessionSaved(
        categoryName: String,
        durationMinutes: Int,
        linkedHabitNames: List<String>
    ) = Unit

    override suspend fun notifyBudgetThreshold(
        categoryId: Long,
        categoryName: String,
        spent: Double,
        budget: Double,
        currencySymbol: String,
        isOverBudget: Boolean
    ) = Unit

    override suspend fun syncTaskReminder(task: DailyTask) = Unit
    override suspend fun cancelTaskReminder(taskId: Long) = Unit
    override suspend fun syncHabitReminder(habit: Habit) = Unit
    override suspend fun cancelHabitReminder(habitId: Long) = Unit
    override suspend fun syncAllReminders() = Unit
    override suspend fun cancelAllReminders() = Unit
    override fun requestWidgetRefresh() = Unit
}
