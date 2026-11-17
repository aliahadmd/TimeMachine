package me.aliahad.timemanager.data.repository

import kotlinx.coroutines.flow.Flow
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.core.datastore.UserPreferencesRepository
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.DailyTaskDao
import me.aliahad.timemanager.domain.gateway.NoOpSystemNotificationGateway
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway

class PlannerRepository(
    private val dailyTaskDao: DailyTaskDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val notificationGateway: SystemNotificationGateway = NoOpSystemNotificationGateway
) {
    fun getTasksForDate(date: String): Flow<List<DailyTask>> =
        dailyTaskDao.getTasksForDate(date)

    fun getTodayTaskCountFlow(): Flow<Int> =
        dailyTaskDao.getTaskCountForDateFlow(timeProvider.today().format(SafeDateTime.ISO_DATE))

    fun getTodayCompletedCountFlow(): Flow<Int> =
        dailyTaskDao.getCompletedCountForDateFlow(timeProvider.today().format(SafeDateTime.ISO_DATE))

    suspend fun getTaskById(id: Long): DailyTask? =
        dailyTaskDao.getTaskById(id)

    suspend fun insertTask(task: DailyTask): Long {
        val newId = dailyTaskDao.insertTask(task)
        val saved = task.copy(id = newId)
        notificationGateway.syncTaskReminder(saved)
        notificationGateway.requestWidgetRefresh()
        return newId
    }

    suspend fun updateTask(task: DailyTask) {
        dailyTaskDao.updateTask(task)
        if (task.isCompleted || task.reminderMinutes <= 0) {
            notificationGateway.cancelTaskReminder(task.id)
        } else {
            notificationGateway.syncTaskReminder(task)
        }
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun toggleTaskCompletion(task: DailyTask) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        updateTask(updated)
    }

    suspend fun deleteTask(task: DailyTask) {
        dailyTaskDao.deleteTask(task)
        notificationGateway.cancelTaskReminder(task.id)
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun seedStarterPlanIfNeeded() {
        val prefs = preferencesRepository.getPreferences()
        if (prefs.plannerOnboardingSeeded) return
        val existing = dailyTaskDao.getTotalTaskCountSql()
        if (existing == 0) {
            val today = timeProvider.today().format(SafeDateTime.ISO_DATE)
            val starter = listOf(
                DailyTask(
                    title = "Morning Planning",
                    description = "Review priorities and top 3 outcomes for today",
                    date = today,
                    startTime = "09:00",
                    endTime = "09:30",
                    taskType = "ROUTINE",
                    category = "Planning",
                    priority = "High",
                    icon = "☀️",
                    color = 0xFFFF9800L,
                    reminderMinutes = 10
                ),
                DailyTask(
                    title = "Deep Work Block",
                    description = "Tap Start Focus to run a Pomodoro or stopwatch session",
                    date = today,
                    startTime = "10:00",
                    endTime = "11:30",
                    taskType = "FOCUS",
                    category = "Work",
                    priority = "High",
                    icon = "🎯",
                    color = 0xFF4CAF50L,
                    reminderMinutes = 5
                ),
                DailyTask(
                    title = "Evening Wind Down",
                    description = "Check off habits and reflect on your day",
                    date = today,
                    startTime = "21:00",
                    endTime = "21:30",
                    taskType = "BREAK",
                    category = "Wellness",
                    priority = "Low",
                    icon = "🌙",
                    color = 0xFF2196F3L
                )
            )
            starter.forEach { task ->
                val id = dailyTaskDao.insertTask(task)
                if (task.reminderMinutes > 0) {
                    notificationGateway.syncTaskReminder(task.copy(id = id))
                }
            }
            notificationGateway.requestWidgetRefresh()
        }
        preferencesRepository.setPlannerOnboardingSeeded(true)
    }
}
