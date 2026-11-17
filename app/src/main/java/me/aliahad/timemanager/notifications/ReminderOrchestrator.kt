package me.aliahad.timemanager.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import me.aliahad.timemanager.HabitReminderReceiver
import me.aliahad.timemanager.ReminderRegistry
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.scheduleReliableAlarm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderOrchestrator {

    private const val TAG = "ReminderOrchestrator"

    private fun habitAlarmRequestCode(habitId: Long): Int =
        NotificationChannelRegistry.HABIT_ALARM_BASE + ((habitId and 0x7FFF_FFFFL) % 45_000L).toInt()

    private fun habitSnoozeRequestCode(habitId: Long): Int =
        NotificationChannelRegistry.HABIT_ACTION_BASE + 10_000 + ((habitId and 0x7FFF_FFFFL) % 45_000L).toInt()

    private fun plannerAlarmRequestCode(taskId: Long): Int =
        NotificationChannelRegistry.PLANNER_ALARM_BASE + ((taskId and 0x7FFF_FFFFL) % 45_000L).toInt()

    fun scheduleHabitReminder(
        context: Context,
        habit: Habit,
        forceTomorrow: Boolean = false
    ) {
        if (!habit.isActive || habit.id <= 0L ||
            habit.reminderTimeHour == null || habit.reminderTimeMinute == null
        ) {
            if (habit.id > 0L) cancelHabitReminder(context, habit.id)
            return
        }

        val now = LocalDateTime.now()
        val hour = habit.reminderTimeHour.coerceIn(0, 23)
        val minute = habit.reminderTimeMinute.coerceIn(0, 59)
        val reminderTime = LocalTime.of(hour, minute)

        val startDay = when {
            forceTomorrow -> now.toLocalDate().plusDays(1)
            !LocalDateTime.of(now.toLocalDate(), reminderTime).isAfter(now) -> now.toLocalDate().plusDays(1)
            else -> now.toLocalDate()
        }

        val targetDate = (0L..7L).firstNotNullOfOrNull { offset ->
            val candidate = startDay.plusDays(offset)
            if (HabitRepository.isScheduledOnDate(habit, candidate)) candidate else null
        } ?: startDay

        val triggerAtMillis = LocalDateTime.of(targetDate, reminderTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildHabitAlarmPendingIntent(
            context = context,
            habitId = habit.id,
            habitName = habit.name,
            hour = hour,
            minute = minute,
            requestCode = habitAlarmRequestCode(habit.id),
            isSnooze = false
        )

        alarmManager.scheduleReliableAlarm(
            triggerAtMillis = triggerAtMillis,
            operation = pendingIntent,
            tag = TAG
        )
        ReminderRegistry.saveReminder(context, habit)
    }

    fun snoozeHabitReminder(
        context: Context,
        habitId: Long,
        habitName: String,
        delayMinutes: Int = 15
    ) {
        if (habitId <= 0L) return
        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1) * 60_000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildHabitAlarmPendingIntent(
            context = context,
            habitId = habitId,
            habitName = habitName,
            hour = -1,
            minute = -1,
            requestCode = habitSnoozeRequestCode(habitId),
            isSnooze = true
        )
        alarmManager.scheduleReliableAlarm(
            triggerAtMillis = triggerAtMillis,
            operation = pendingIntent,
            tag = TAG
        )
    }

    fun cancelHabitReminder(context: Context, habitId: Long) {
        if (habitId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(
            buildHabitAlarmPendingIntent(
                context = context,
                habitId = habitId,
                habitName = "",
                hour = -1,
                minute = -1,
                requestCode = habitAlarmRequestCode(habitId),
                isSnooze = false
            )
        )
        alarmManager.cancel(
            buildHabitAlarmPendingIntent(
                context = context,
                habitId = habitId,
                habitName = "",
                hour = -1,
                minute = -1,
                requestCode = habitSnoozeRequestCode(habitId),
                isSnooze = true
            )
        )
        ReminderRegistry.removeReminder(context, habitId)
        NotificationManagerCompat.from(context).cancel(
            NotificationChannelRegistry.HABIT_CONTENT_BASE + ((habitId and 0x7FFF_FFFFL) % 45_000L).toInt()
        )
    }

    fun scheduleTaskReminder(context: Context, task: DailyTask) {
        if (task.id <= 0L) return
        if (task.isCompleted || task.reminderMinutes <= 0) {
            cancelTaskReminder(context, task.id)
            return
        }

        val taskDate = SafeDateTime.parseLocalDateOrNull(task.date) ?: return
        val startTime = SafeDateTime.parseLocalTimeOrNull(task.startTime) ?: return
        val triggerDateTime = LocalDateTime.of(taskDate, startTime)
            .minusMinutes(task.reminderMinutes.toLong())
        val triggerMillis = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) {
            cancelTaskReminder(context, task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PlannerReminderReceiver::class.java).apply {
            putExtra(NotificationChannelRegistry.EXTRA_TASK_ID, task.id)
        }
        val operation = PendingIntent.getBroadcast(
            context,
            plannerAlarmRequestCode(task.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.scheduleReliableAlarm(
            triggerAtMillis = triggerMillis,
            operation = operation,
            tag = TAG
        )
    }

    fun snoozeTaskReminder(
        context: Context,
        taskId: Long,
        delayMinutes: Int = 10
    ) {
        if (taskId <= 0L) return
        val triggerMillis = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1) * 60_000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PlannerReminderReceiver::class.java).apply {
            putExtra(NotificationChannelRegistry.EXTRA_TASK_ID, taskId)
        }
        val operation = PendingIntent.getBroadcast(
            context,
            plannerAlarmRequestCode(taskId) + 45_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.scheduleReliableAlarm(
            triggerAtMillis = triggerMillis,
            operation = operation,
            tag = TAG
        )
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PlannerReminderReceiver::class.java)
        val operation = PendingIntent.getBroadcast(
            context,
            plannerAlarmRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(operation)
        NotificationManagerCompat.from(context).cancel(
            NotificationChannelRegistry.PLANNER_CONTENT_BASE + ((taskId and 0x7FFF_FFFFL) % 45_000L).toInt()
        )
    }

    suspend fun syncAllReminders(context: Context, database: TimerDatabase) {
        NotificationChannelRegistry.ensureChannels(context)
        val activeHabits = database.habitDao().getAllActiveHabitsSync()
        activeHabits.forEach { habit ->
            if (habit.reminderTimeHour != null && habit.reminderTimeMinute != null) {
                scheduleHabitReminder(context, habit)
            } else {
                cancelHabitReminder(context, habit.id)
            }
        }

        val today = LocalDate.now().format(SafeDateTime.ISO_DATE)
        val tasks = database.dailyTaskDao().getUpcomingTasksWithRemindersSync(today)
        tasks.forEach { task ->
            scheduleTaskReminder(context, task)
        }
    }

    suspend fun cancelAllReminders(context: Context, database: TimerDatabase) {
        database.habitDao().getAllHabitsSync().forEach { habit ->
            cancelHabitReminder(context, habit.id)
        }
        database.dailyTaskDao().getAllTasks().forEach { task ->
            cancelTaskReminder(context, task.id)
        }
        ReminderRegistry.clearAll(context)
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun buildHabitAlarmPendingIntent(
        context: Context,
        habitId: Long,
        habitName: String,
        hour: Int,
        minute: Int,
        requestCode: Int,
        isSnooze: Boolean
    ): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_NAME", habitName)
            putExtra("REMINDER_HOUR", hour)
            putExtra("REMINDER_MINUTE", minute)
            putExtra("IS_SNOOZE", isSnooze)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
