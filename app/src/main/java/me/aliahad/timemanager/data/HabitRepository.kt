package me.aliahad.timemanager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HabitRepository(
    val habitDao: HabitDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {

    val allActiveHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()

    fun getAllActiveHabitsWithStats(): Flow<List<HabitWithStats>> {
        return combine(
            habitDao.getAllActiveHabits(),
            habitDao.getAllCompletionsFlow()
        ) { habits, allCompletions ->
            val completionsByHabit = allCompletions.groupBy { it.habitId }
            val today = timeProvider.today()
            habits.map { habit ->
                computeHabitStats(
                    habit = habit,
                    completions = completionsByHabit[habit.id].orEmpty(),
                    today = today,
                    zoneId = timeProvider.zoneId()
                )
            }
        }
    }

    fun getHabitWithStats(habitId: Long): Flow<HabitWithStats?> {
        return combine(
            habitDao.getHabitByIdFlow(habitId),
            habitDao.getHabitCompletions(habitId)
        ) { habit, completions ->
            if (habit == null) return@combine null
            computeHabitStats(
                habit = habit,
                completions = completions,
                today = timeProvider.today(),
                zoneId = timeProvider.zoneId()
            )
        }
    }

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun archiveHabit(habitId: Long) = habitDao.archiveHabit(habitId)

    suspend fun deleteHabitPermanently(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.archiveHabit(habit.id)

    suspend fun addCompletion(
        habitId: Long,
        date: String = timeProvider.today().format(SafeDateTime.ISO_DATE),
        completionType: CompletionType = CompletionType.ACHIEVED,
        notes: String = ""
    ) {
        habitDao.insertCompletion(
            HabitCompletion(
                habitId = habitId,
                date = date,
                completionType = completionType,
                completedAt = timeProvider.nowMillis(),
                notes = notes
            )
        )
    }

    suspend fun removeCompletion(habitId: Long, date: String) {
        habitDao.deleteCompletionByDate(habitId, date)
    }

    fun getHabitCompletions(habitId: Long): Flow<List<HabitCompletion>> {
        return habitDao.getHabitCompletions(habitId)
    }

    suspend fun getCompletionsForDateRange(
        habitId: Long,
        startDate: String,
        endDate: String
    ): List<HabitCompletion> {
        return habitDao.getCompletionsInRange(habitId, startDate, endDate)
    }

    companion object {
        fun parseScheduledDays(scheduledDays: String): Set<Int> {
            val parsed = scheduledDays.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
            return if (parsed.isEmpty()) (1..7).toSet() else parsed
        }

        fun isScheduledOnDate(habit: Habit, date: LocalDate): Boolean {
            if (habit.isEveryday) return true
            return parseScheduledDays(habit.scheduledDays).contains(date.dayOfWeek.value)
        }

        fun computeHabitStats(
            habit: Habit,
            completions: List<HabitCompletion>,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): HabitWithStats {
            val todayStr = today.format(SafeDateTime.ISO_DATE)
            val creationDate = Instant.ofEpochMilli(habit.createdAt)
                .atZone(zoneId)
                .toLocalDate()
            val creationStr = creationDate.format(SafeDateTime.ISO_DATE)

            val validCompletions = completions.filter { it.date <= todayStr }
            val completionsSinceCreation = validCompletions.filter { it.date >= creationStr }

            val totalAchieved = validCompletions.count { it.completionType == CompletionType.ACHIEVED }
            val totalGaveUp = validCompletions.count { it.completionType == CompletionType.GAVE_UP }
            val totalCompletions = validCompletions.size
            val todayCompletion = validCompletions.firstOrNull { it.date == todayStr }

            val achievedDates = validCompletions
                .filter { it.completionType == CompletionType.ACHIEVED }
                .mapNotNull { SafeDateTime.parseLocalDateOrNull(it.date) }
                .filter { !it.isAfter(today) }
                .toSet()

            val (currentStreak, longestStreak) = calculateHabitStreaks(
                habit = habit,
                achievedDates = achievedDates,
                today = today,
                creationDate = creationDate
            )

            // Expected scheduled days since creation
            val expectedDays = countScheduledDays(habit, creationDate, today).coerceAtLeast(1)
            val achievedSinceCreation = completionsSinceCreation.count {
                it.completionType == CompletionType.ACHIEVED
            }
            val completionRate = (achievedSinceCreation.toFloat() / expectedDays).coerceIn(0f, 1f)
            val successRate = if (totalCompletions > 0) {
                (totalAchieved.toFloat() / totalCompletions).coerceIn(0f, 1f)
            } else 0f
            val goalProgress = if (habit.goalPeriodDays > 0) {
                (totalAchieved.toFloat() / habit.goalPeriodDays).coerceIn(0f, 1f)
            } else 0f

            return HabitWithStats(
                habit = habit,
                totalCompletions = totalCompletions,
                totalAchieved = totalAchieved,
                totalGaveUp = totalGaveUp,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                completionRate = completionRate,
                successRate = successRate,
                goalProgress = goalProgress,
                isCompletedToday = todayCompletion != null,
                todayCompletion = todayCompletion
            )
        }

        private fun countScheduledDays(habit: Habit, start: LocalDate, end: LocalDate): Int {
            if (start.isAfter(end)) return 0
            if (habit.isEveryday) {
                return (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
            }
            val allowed = parseScheduledDays(habit.scheduledDays)
            var count = 0
            var cursor = start
            while (!cursor.isAfter(end)) {
                if (allowed.contains(cursor.dayOfWeek.value)) count++
                cursor = cursor.plusDays(1)
            }
            return count.coerceAtLeast(1)
        }

        internal fun calculateHabitStreaks(
            habit: Habit,
            achievedDates: Set<LocalDate>,
            today: LocalDate,
            creationDate: LocalDate
        ): Pair<Int, Int> {
            if (achievedDates.isEmpty()) return 0 to 0

            val earliestDate = minOf(creationDate, achievedDates.min())

            // Current streak: start from today if achieved today, or walk backward to the latest scheduled day
            var currentStreak = 0
            var cursor = today
            if (!achievedDates.contains(today)) {
                // Allow streak to remain alive if today hasn't been logged yet (skip back from yesterday)
                cursor = today.minusDays(1)
                while (!habit.isEveryday && !cursor.isBefore(earliestDate) &&
                    !isScheduledOnDate(habit, cursor) && !achievedDates.contains(cursor)
                ) {
                    cursor = cursor.minusDays(1)
                }
            }
            while (!cursor.isBefore(earliestDate)) {
                if (achievedDates.contains(cursor)) {
                    currentStreak++
                    cursor = cursor.minusDays(1)
                } else if (!habit.isEveryday && !isScheduledOnDate(habit, cursor)) {
                    // Unscheduled rest day doesn't break streak
                    cursor = cursor.minusDays(1)
                } else {
                    break
                }
            }

            // Longest streak across full achieved history
            var longestStreak = 0
            var running = 0
            val minDate = achievedDates.min()
            val maxDate = achievedDates.max()
            var scan = minDate
            while (!scan.isAfter(maxDate)) {
                if (achievedDates.contains(scan)) {
                    running++
                    if (running > longestStreak) longestStreak = running
                } else if (!habit.isEveryday && !isScheduledOnDate(habit, scan)) {
                    // Unscheduled day keeps streak intact
                } else {
                    running = 0
                }
                scan = scan.plusDays(1)
            }

            return currentStreak to longestStreak
        }
    }
}
