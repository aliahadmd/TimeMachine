package me.aliahad.timemanager.data.repository

import kotlinx.coroutines.flow.Flow
import me.aliahad.timemanager.calculateStreaks
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.ActivityCategoryDao
import me.aliahad.timemanager.data.CategoryStats
import me.aliahad.timemanager.data.DailyStats
import me.aliahad.timemanager.data.Preset
import me.aliahad.timemanager.data.PresetDao
import me.aliahad.timemanager.data.StreakInfo
import me.aliahad.timemanager.data.TimeSession
import me.aliahad.timemanager.data.TimeSessionDao
import me.aliahad.timemanager.domain.gateway.NoOpSystemNotificationGateway
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway

class FocusRepository(
    private val activityCategoryDao: ActivityCategoryDao,
    private val timeSessionDao: TimeSessionDao,
    private val presetDao: PresetDao,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val notificationGateway: SystemNotificationGateway = NoOpSystemNotificationGateway
) {
    val activeCategories: Flow<List<ActivityCategory>> = activityCategoryDao.getAllActiveCategories()
    val allCategories: Flow<List<ActivityCategory>> = activityCategoryDao.getAllCategories()
    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets()
    val allSessions: Flow<List<TimeSession>> = timeSessionDao.getAllSessions()

    fun getTodayFocusMinutesFlow(): Flow<Int> {
        val today = timeProvider.today().format(SafeDateTime.ISO_DATE)
        return timeSessionDao.getTotalMinutesForDateFlow(today)
    }

    fun getSessionsForDate(date: String): Flow<List<TimeSession>> =
        timeSessionDao.getSessionsByDate(date)

    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<TimeSession>> =
        timeSessionDao.getSessionsBetweenDates(startDate, endDate)

    suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<TimeSession> =
        timeSessionDao.getSessionsBetweenDatesSync(startDate, endDate)

    suspend fun getCategoryById(id: Long): ActivityCategory? =
        activityCategoryDao.getCategoryById(id)

    suspend fun insertCategory(category: ActivityCategory): Long =
        activityCategoryDao.insertCategory(category)

    suspend fun updateCategory(category: ActivityCategory) =
        activityCategoryDao.updateCategory(category)

    suspend fun archiveCategory(id: Long) =
        activityCategoryDao.archiveCategory(id)

    suspend fun insertPreset(preset: Preset): Long =
        presetDao.insertPreset(preset)

    suspend fun updatePreset(preset: Preset) =
        presetDao.updatePreset(preset)

    suspend fun deletePreset(preset: Preset) =
        presetDao.deletePreset(preset)

    suspend fun insertSession(session: TimeSession): Long {
        val id = timeSessionDao.insertSession(session)
        notificationGateway.requestWidgetRefresh()
        return id
    }

    suspend fun updateSession(session: TimeSession) {
        timeSessionDao.updateSession(session)
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun deleteSessionById(sessionId: Long) {
        timeSessionDao.deleteSessionById(sessionId)
        notificationGateway.requestWidgetRefresh()
    }

    suspend fun getCategoryStats(category: ActivityCategory): CategoryStats {
        val totalMinutes = timeSessionDao.getTotalMinutesForCategory(category.id) ?: 0
        val count = timeSessionDao.getSessionCountForCategory(category.id)
        val avg = timeSessionDao.getAverageSessionMinutesForCategory(category.id) ?: 0f
        val lastDate = timeSessionDao.getLastSessionDateForCategory(category.id)
        return CategoryStats(
            category = category,
            totalMinutes = totalMinutes,
            sessionCount = count,
            averageSessionMinutes = avg,
            lastSessionDate = lastDate
        )
    }

    suspend fun getStreakInfo(days: Int = 30): StreakInfo {
        val dates = timeSessionDao.getAllActiveDates()
        return calculateStreaks(dates, days, timeProvider.today())
    }

    suspend fun getDailyStats(startDate: String, endDate: String): List<DailyStats> {
        return timeSessionDao.getDailyStats(startDate, endDate).map {
            DailyStats(
                date = it.date,
                totalMinutes = it.totalMinutes,
                sessionCount = it.sessionCount,
                categoriesUsed = it.categoriesUsed
            )
        }
    }

    suspend fun ensureDefaultFocusDataIfNeeded(
        prefsRepo: me.aliahad.timemanager.core.datastore.UserPreferencesRepository
    ) {
        val prefs = prefsRepo.getPreferences()
        if (prefs.focusDefaultsSeeded) return
        if (activityCategoryDao.getActiveCategoryCount() == 0) {
            listOf(
                ActivityCategory(name = "Deep Work", icon = "💻", color = 0xFF0A66C2L, sortOrder = 0),
                ActivityCategory(name = "Study", icon = "📚", color = 0xFF0D9488L, sortOrder = 1),
                ActivityCategory(name = "Reading", icon = "📖", color = 0xFF7C3AEDL, sortOrder = 2),
                ActivityCategory(name = "Writing", icon = "✍️", color = 0xFFD97706L, sortOrder = 3),
                ActivityCategory(name = "Exercise", icon = "🏃", color = 0xFF16A34AL, sortOrder = 4)
            ).forEach { activityCategoryDao.insertCategory(it) }
        }
        if (presetDao.getPresetCount() == 0) {
            listOf(
                Preset(name = "Pomodoro", hours = 0, minutes = 25, breakMinutes = 5, isPomodoro = true),
                Preset(name = "Deep Block", hours = 0, minutes = 50, breakMinutes = 10, isPomodoro = true),
                Preset(name = "Quick Sprint", hours = 0, minutes = 15, breakMinutes = 5, isPomodoro = false)
            ).forEach { presetDao.insertPreset(it) }
        }
        prefsRepo.setFocusDefaultsSeeded(true)
    }
}
