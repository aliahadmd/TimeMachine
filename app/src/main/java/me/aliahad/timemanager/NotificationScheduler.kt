package me.aliahad.timemanager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import me.aliahad.timemanager.notifications.ReminderActionReceiver
import me.aliahad.timemanager.notifications.ReminderOrchestrator
import java.time.LocalDate

object NotificationScheduler {
    fun scheduleHabitReminder(context: Context, habit: Habit) {
        ReminderOrchestrator.scheduleHabitReminder(context, habit)
    }

    fun cancelHabitReminder(context: Context, habitId: Long) {
        ReminderOrchestrator.cancelHabitReminder(context, habitId)
    }
}

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("HABIT_ID", -1L)
        if (habitId <= 0L) return
        val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationChannelRegistry.ensureChannels(context)
                val container = context.appContainerOrNull() ?: return@launch
                val habit = container.database.habitDao().getHabitById(habitId)

                // Always advance the recurring schedule to a future day before deciding whether to ring now
                if (!isSnooze) {
                    if (habit != null && habit.isActive &&
                        habit.reminderTimeHour != null && habit.reminderTimeMinute != null
                    ) {
                        ReminderOrchestrator.scheduleHabitReminder(
                            context = context,
                            habit = habit,
                            forceTomorrow = true
                        )
                    } else {
                        ReminderOrchestrator.cancelHabitReminder(context, habitId)
                        return@launch
                    }
                }

                if (habit == null || !habit.isActive) return@launch

                val prefs = container.preferencesRepository.getPreferences()
                if (!prefs.notificationsEnabled) return@launch
                if (NotificationChannelRegistry.isQuietHoursNow(prefs)) return@launch

                val today = LocalDate.now()
                if (!isSnooze && !HabitRepository.isScheduledOnDate(habit, today)) return@launch

                val todayIso = today.format(SafeDateTime.ISO_DATE)
                val alreadyLogged = container.database.habitDao().getCompletion(habit.id, todayIso) != null
                if (alreadyLogged) return@launch

                if (!NotificationChannelRegistry.hasPostNotificationsPermission(context)) return@launch

                val notifId = NotificationChannelRegistry.HABIT_CONTENT_BASE +
                    ((habit.id and 0x7FFF_FFFFL) % 45_000L).toInt()
                val reqBase = NotificationChannelRegistry.HABIT_ACTION_BASE +
                    ((habit.id and 0x7FFF_FFFFL) % 10_000L).toInt()

                val contentIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(
                        NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                        NotificationChannelRegistry.ROUTE_HABIT_DETAIL
                    )
                    putExtra(NotificationChannelRegistry.EXTRA_OPEN_HABIT_ID, habit.id)
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val achievedIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_HABIT_ACHIEVED
                    putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, habit.id)
                }
                val achievedPendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqBase,
                    achievedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_HABIT_SNOOZE
                    putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, habit.id)
                    putExtra(ReminderActionReceiver.EXTRA_HABIT_NAME, habit.name)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqBase + 10_000,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_HABIT_DISMISS
                    putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, habit.id)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqBase + 20_000,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(
                    context,
                    NotificationChannelRegistry.CHANNEL_HABIT_REMINDERS
                )
                    .setSmallIcon(R.drawable.ic_stat_timemachine)
                    .setContentTitle(habit.name)
                    .setContentText(
                        if (habit.description.isNotBlank()) habit.description
                        else "Time for your scheduled check-in"
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setGroup(NotificationChannelRegistry.GROUP_KEY_HABITS)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .addAction(0, "Done", achievedPendingIntent)
                    .addAction(0, "Snooze 15m", snoozePendingIntent)
                    .addAction(0, "Dismiss", dismissPendingIntent)
                    .build()

                try {
                    NotificationManagerCompat.from(context).notify(notifId, notification)
                } catch (sec: SecurityException) {
                    Log.w("HabitReminderReceiver", "Notification permission missing", sec)
                }
            } catch (t: Throwable) {
                Log.e("HabitReminderReceiver", "Error executing habit reminder", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
