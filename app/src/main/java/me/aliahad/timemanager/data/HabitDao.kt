package me.aliahad.timemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveHabits(): Flow<List<Habit>>
    
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAllActiveHabitsSync(): List<Habit>
    
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): Habit?
    
    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getHabitByIdFlow(habitId: Long): Flow<Habit?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long
    
    @Update
    suspend fun updateHabit(habit: Habit)
    
    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Query("UPDATE habits SET isActive = 0 WHERE id = :habitId")
    suspend fun archiveHabit(habitId: Long)
    
    // Habit Completions
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getHabitCompletions(habitId: Long): Flow<List<HabitCompletion>>
    
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun getCompletion(habitId: Long, date: String): HabitCompletion?
    
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getCompletionsInRange(habitId: Long, startDate: String, endDate: String): List<HabitCompletion>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)
    
    @Delete
    suspend fun deleteCompletion(completion: HabitCompletion)
    
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletionByDate(habitId: Long, date: String)
    
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId")
    suspend fun getTotalCompletions(habitId: Long): Int
    
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date >= :startDate")
    suspend fun getCompletionsSince(habitId: Long, startDate: String): Int
}

