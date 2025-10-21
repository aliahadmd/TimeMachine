package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSessionDao {
    @Query("SELECT * FROM time_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TimeSession>>
    
    @Query("SELECT * FROM time_sessions WHERE categoryId = :categoryId ORDER BY startTime DESC")
    fun getSessionsByCategory(categoryId: Long): Flow<List<TimeSession>>
    
    @Query("SELECT * FROM time_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsByDate(date: String): Flow<List<TimeSession>>
    
    @Query("SELECT * FROM time_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<TimeSession>>
    
    @Query("SELECT * FROM time_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<TimeSession>
    
    @Query("SELECT * FROM time_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TimeSession?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimeSession): Long
    
    @Update
    suspend fun updateSession(session: TimeSession)
    
    @Delete
    suspend fun deleteSession(session: TimeSession)
    
    @Query("DELETE FROM time_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
    
    // Statistics queries
    @Query("""
        SELECT date, SUM(durationMinutes) as totalMinutes, COUNT(*) as sessionCount, 
        COUNT(DISTINCT categoryId) as categoriesUsed
        FROM time_sessions 
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyStats(startDate: String, endDate: String): List<DailyStatsRaw>
    
    @Query("""
        SELECT SUM(durationMinutes) FROM time_sessions WHERE date = :date
    """)
    suspend fun getTotalMinutesForDate(date: String): Int?
    
    @Query("""
        SELECT SUM(durationMinutes) FROM time_sessions 
        WHERE categoryId = :categoryId
    """)
    suspend fun getTotalMinutesForCategory(categoryId: Long): Int?
    
    @Query("""
        SELECT COUNT(*) FROM time_sessions WHERE categoryId = :categoryId
    """)
    suspend fun getSessionCountForCategory(categoryId: Long): Int
    
    @Query("""
        SELECT AVG(durationMinutes) FROM time_sessions WHERE categoryId = :categoryId
    """)
    suspend fun getAverageSessionMinutesForCategory(categoryId: Long): Float?
    
    @Query("""
        SELECT MAX(date) FROM time_sessions WHERE categoryId = :categoryId
    """)
    suspend fun getLastSessionDateForCategory(categoryId: Long): String?
    
    @Query("""
        SELECT COUNT(DISTINCT date) FROM time_sessions
    """)
    suspend fun getTotalActiveDays(): Int
    
    @Query("""
        SELECT DISTINCT date FROM time_sessions ORDER BY date DESC
    """)
    suspend fun getAllActiveDates(): List<String>
    
    @Query("""
        SELECT COUNT(*) FROM time_sessions WHERE date = :date
    """)
    suspend fun getSessionCountForDate(date: String): Int
    
    @Query("SELECT * FROM time_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<TimeSession>
}

// Raw data class for Room query result
data class DailyStatsRaw(
    val date: String,
    val totalMinutes: Int,
    val sessionCount: Int,
    val categoriesUsed: Int
)

