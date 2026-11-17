package me.aliahad.timemanager.data.repository

import kotlinx.coroutines.flow.Flow
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.data.AppUsageSummary
import me.aliahad.timemanager.data.ScreenTimeDailySummary
import me.aliahad.timemanager.data.ScreenTimeDao
import me.aliahad.timemanager.data.ScreenTimeHourly
import me.aliahad.timemanager.data.ScreenTimeSession

class ScreenTimeRepository(
    private val screenTimeDao: ScreenTimeDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    fun getTodaySummaryFlow(): Flow<ScreenTimeDailySummary?> {
        val today = timeProvider.today().format(SafeDateTime.ISO_DATE)
        return screenTimeDao.getDailySummaryFlow(today)
    }

    fun getSummaryForDateFlow(date: String): Flow<ScreenTimeDailySummary?> =
        screenTimeDao.getDailySummaryFlow(date)

    fun getHourlyForDateFlow(date: String): Flow<List<ScreenTimeHourly>> =
        screenTimeDao.getHourlyDataForDate(date)

    fun getRecentDailySummaries(days: Int = 7): Flow<List<ScreenTimeDailySummary>> =
        screenTimeDao.getRecentDailySummaries(days)

    fun getSessionsForDateFlow(date: String): Flow<List<ScreenTimeSession>> =
        screenTimeDao.getSessionsForDate(date)

    fun getAppUsageForDateFlow(date: String): Flow<List<AppUsageSummary>> =
        screenTimeDao.getAppUsageForDateFlow(date)

    suspend fun getSevenDayAverageSeconds(): Int {
        val today = timeProvider.today()
        val start = today.minusDays(6).format(SafeDateTime.ISO_DATE)
        val end = today.format(SafeDateTime.ISO_DATE)
        return (screenTimeDao.getAverageScreenTime(start, end) ?: 0.0).toInt()
    }
}
