package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeDao {
    // Session operations
    @Insert
    suspend fun insertSession(session: ScreenTimeSession): Long
    
    @Query("SELECT * FROM screen_time_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getSessionsForDate(date: String): Flow<List<ScreenTimeSession>>
    
    @Query("SELECT * FROM screen_time_sessions WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getSessionsForDateSync(date: String): List<ScreenTimeSession>
    
    @Query("SELECT * FROM screen_time_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ScreenTimeSession>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM screen_time_sessions WHERE sessionStart = :sessionStart)")
    suspend fun hasSession(sessionStart: Long): Boolean
    
    // Daily summary operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: ScreenTimeDailySummary)
    
    @Query("SELECT * FROM screen_time_daily WHERE date = :date")
    suspend fun getDailySummary(date: String): ScreenTimeDailySummary?
    
    @Query("SELECT * FROM screen_time_daily WHERE date = :date")
    fun getDailySummaryFlow(date: String): Flow<ScreenTimeDailySummary?>
    
    @Query("SELECT * FROM screen_time_daily ORDER BY date DESC LIMIT :days")
    fun getRecentDailySummaries(days: Int): Flow<List<ScreenTimeDailySummary>>
    
    @Query("SELECT * FROM screen_time_daily ORDER BY date DESC LIMIT :days")
    suspend fun getRecentDailySummariesSync(days: Int): List<ScreenTimeDailySummary>
    
    // Hourly breakdown operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyData(hourly: ScreenTimeHourly)
    
    @Query("SELECT * FROM screen_time_hourly WHERE date = :date ORDER BY hour ASC")
    fun getHourlyDataForDate(date: String): Flow<List<ScreenTimeHourly>>
    
    @Query("SELECT * FROM screen_time_hourly WHERE date = :date ORDER BY hour ASC")
    suspend fun getHourlyDataForDateSync(date: String): List<ScreenTimeHourly>
    
    // Analytics queries
    @Query("SELECT SUM(totalScreenTimeSeconds) FROM screen_time_daily WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalScreenTime(startDate: String, endDate: String): Int?
    
    @Query("SELECT AVG(totalScreenTimeSeconds) FROM screen_time_daily WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageScreenTime(startDate: String, endDate: String): Int?
    
    @Query("SELECT SUM(pickupsCount) FROM screen_time_daily WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalPickups(startDate: String, endDate: String): Int?
    
    @Query("SELECT date, totalScreenTimeSeconds FROM screen_time_daily WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getScreenTimeTrend(startDate: String, endDate: String): List<DailyTrend>
    
    // Delete operations
    @Query("DELETE FROM screen_time_sessions WHERE date < :date")
    suspend fun deleteOldSessions(date: String)
    
    @Query("DELETE FROM screen_time_daily WHERE date < :date")
    suspend fun deleteOldDailySummaries(date: String)
    
    @Query("DELETE FROM screen_time_hourly WHERE date < :date")
    suspend fun deleteOldHourlyData(date: String)
}

// Helper data class for trends
data class DailyTrend(
    val date: String,
    val totalScreenTimeSeconds: Int
)
