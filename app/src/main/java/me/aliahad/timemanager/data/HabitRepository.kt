package me.aliahad.timemanager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HabitRepository(val habitDao: HabitDao) {
    
    val allActiveHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    
    fun getHabitWithStats(habitId: Long): Flow<HabitWithStats?> {
        return habitDao.getHabitByIdFlow(habitId).map { habit ->
            if (habit == null) return@map null
            
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Get all completions for calculations
            val allCompletions = habitDao.getCompletionsInRange(
                habitId,
                LocalDate.ofEpochDay(habit.createdAt / (24 * 60 * 60 * 1000)).format(DateTimeFormatter.ISO_LOCAL_DATE),
                today
            )
            
            val totalAchieved = allCompletions.count { it.completionType == CompletionType.ACHIEVED }
            val totalGaveUp = allCompletions.count { it.completionType == CompletionType.GAVE_UP }
            val totalCompletions = allCompletions.size
            
            val isCompletedToday = habitDao.getCompletion(habitId, today) != null
            
            // Calculate streaks (only achieved)
            val streaks = calculateStreaks(habit, habitId)
            
            // Calculate completion rate (any completion / expected days)
            val daysSinceCreation = ChronoUnit.DAYS.between(
                LocalDate.ofEpochDay(habit.createdAt / (24 * 60 * 60 * 1000)),
                LocalDate.now()
            ).toInt() + 1
            
            val expectedCompletions = if (habit.isEveryday) daysSinceCreation else daysSinceCreation
            val completionRate = if (expectedCompletions > 0) {
                (totalCompletions.toFloat() / expectedCompletions).coerceAtMost(1f)
            } else 0f
            
            // Calculate success rate (achieved / total submissions)
            val successRate = if (totalCompletions > 0) {
                (totalAchieved.toFloat() / totalCompletions)
            } else 0f
            
            HabitWithStats(
                habit = habit,
                totalCompletions = totalCompletions,
                totalAchieved = totalAchieved,
                totalGaveUp = totalGaveUp,
                currentStreak = streaks.first,
                longestStreak = streaks.second,
                completionRate = completionRate,
                successRate = successRate,
                isCompletedToday = isCompletedToday
            )
        }
    }
    
    private suspend fun calculateStreaks(habit: Habit, habitId: Long): Pair<Int, Int> {
        val today = LocalDate.now()
        val completions = habitDao.getCompletionsInRange(
            habitId,
            today.minusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE),
            today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        
        if (completions.isEmpty()) return Pair(0, 0)
        
        // Only count ACHIEVED completions for streaks
        val achievedDates = completions
            .filter { it.completionType == CompletionType.ACHIEVED }
            .map { LocalDate.parse(it.date) }
            .toSet()
        
        if (achievedDates.isEmpty()) return Pair(0, 0)
        
        // Calculate current streak
        var currentStreak = 0
        var checkDate = today
        
        // Check if achieved today or yesterday (for current streak)
        val startDate = if (achievedDates.contains(today)) today else today.minusDays(1)
        checkDate = startDate
        
        while (achievedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }
        
        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: LocalDate? = null
        
        achievedDates.sortedDescending().forEach { date ->
            if (previousDate == null || ChronoUnit.DAYS.between(date, previousDate) == 1L) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 1
            }
            previousDate = date
        }
        
        return Pair(currentStreak, longestStreak)
    }
    
    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }
    
    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }
    
    suspend fun archiveHabit(habitId: Long) {
        habitDao.archiveHabit(habitId)
    }
    
    suspend fun addCompletion(
        habitId: Long, 
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        completionType: CompletionType = CompletionType.ACHIEVED
    ) {
        habitDao.insertCompletion(
            HabitCompletion(
                habitId = habitId,
                date = date,
                completionType = completionType
            )
        )
    }
    
    suspend fun removeCompletion(habitId: Long, date: String) {
        habitDao.deleteCompletionByDate(habitId, date)
    }
    
    suspend fun deleteHabit(habit: Habit) {
        habitDao.archiveHabit(habit.id)
    }
    
    fun getHabitCompletions(habitId: Long): Flow<List<HabitCompletion>> {
        return habitDao.getHabitCompletions(habitId)
    }
    
    suspend fun getCompletionsForDateRange(habitId: Long, startDate: String, endDate: String): List<HabitCompletion> {
        return habitDao.getCompletionsInRange(habitId, startDate, endDate)
    }
}

