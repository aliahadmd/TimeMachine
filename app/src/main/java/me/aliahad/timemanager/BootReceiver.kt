package me.aliahad.timemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.ReminderRegistry

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootReceiver", "🔄 Device rebooted, rescheduling...")
            
            // Use a CoroutineScope for database operations
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Reschedule habit reminders using persisted registry
                    val database = TimerDatabase.getDatabase(context)
                    val reminderEntries = ReminderRegistry.getReminders(context)
                    var rescheduled = 0

                    if (reminderEntries.isNotEmpty()) {
                        reminderEntries.forEach { entry ->
                            val habit = database.habitDao().getHabitById(entry.habitId)
                            if (habit != null && habit.isActive &&
                                habit.reminderTimeHour != null && habit.reminderTimeMinute != null
                            ) {
                                NotificationScheduler.scheduleHabitReminder(
                                    context,
                                    habit.copy(
                                        reminderTimeHour = entry.hour,
                                        reminderTimeMinute = entry.minute
                                    )
                                )
                                rescheduled++
                            } else {
                                ReminderRegistry.removeReminder(context, entry.habitId)
                            }
                        }
                        Log.d("BootReceiver", "✅ Rescheduled $rescheduled habit reminders from registry")
                    } else {
                        // Fallback: best effort using active habits from DB
                        val activeHabits = database.habitDao().getAllActiveHabitsSync()
                        if (activeHabits.isNotEmpty()) {
                            activeHabits.forEach { habit ->
                                if (habit.reminderTimeHour != null && habit.reminderTimeMinute != null) {
                                    NotificationScheduler.scheduleHabitReminder(context, habit)
                                    ReminderRegistry.saveReminder(context, habit)
                                    rescheduled++
                                }
                            }
                            Log.d("BootReceiver", "ℹ️ Registry empty, rescheduled $rescheduled reminders from active habits")
                        }
                    }
                    
                    // Note: Old timer restore logic removed - new Focus Tracker uses database sessions
                    Log.d("BootReceiver", "✅ Boot completed, habit reminders rescheduled")
                    
                } catch (e: Exception) {
                    Log.e("BootReceiver", "❌ Error on boot: ${e.message}")
                }
            }
        }
    }
}
