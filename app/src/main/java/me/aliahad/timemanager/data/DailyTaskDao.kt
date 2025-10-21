package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Insert
    suspend fun insertTask(task: DailyTask): Long
    
    @Update
    suspend fun updateTask(task: DailyTask)
    
    @Delete
    suspend fun deleteTask(task: DailyTask)
    
    @Query("SELECT * FROM daily_tasks WHERE date = :date ORDER BY startTime ASC")
    fun getTasksForDate(date: String): Flow<List<DailyTask>>
    
    @Query("SELECT * FROM daily_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): DailyTask?
    
    @Query("SELECT * FROM daily_tasks WHERE date = :date AND isCompleted = 0 ORDER BY startTime ASC")
    fun getPendingTasksForDate(date: String): Flow<List<DailyTask>>
    
    @Query("SELECT * FROM daily_tasks WHERE date = :date AND isCompleted = 1 ORDER BY startTime ASC")
    fun getCompletedTasksForDate(date: String): Flow<List<DailyTask>>
    
    @Query("SELECT COUNT(*) FROM daily_tasks WHERE date = :date")
    suspend fun getTaskCountForDate(date: String): Int
    
    @Query("SELECT COUNT(*) FROM daily_tasks WHERE date = :date AND isCompleted = 1")
    suspend fun getCompletedCountForDate(date: String): Int
    
    @Query("SELECT * FROM daily_tasks WHERE date = :date AND taskType = :type ORDER BY startTime ASC")
    fun getTasksByType(date: String, type: String): Flow<List<DailyTask>>
    
    @Query("SELECT * FROM daily_tasks WHERE isRecurring = 1")
    suspend fun getAllRecurringTasks(): List<DailyTask>
    
    @Query("DELETE FROM daily_tasks WHERE date < :beforeDate")
    suspend fun deleteOldTasks(beforeDate: String)
    
    @Query("SELECT * FROM daily_tasks ORDER BY date DESC, startTime ASC")
    suspend fun getAllTasks(): List<DailyTask>
}

