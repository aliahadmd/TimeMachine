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
                    
                    // 2. Restore timer if it was running
                    restoreTimerIfNeeded(context)
                    
                } catch (e: Exception) {
                    Log.e("BootReceiver", "❌ Error on boot: ${e.message}")
                }
            }
        }
    }
    
    private fun restoreTimerIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val endTimeMillis = prefs.getLong("timer_end_time", 0)
        
        if (endTimeMillis > 0) {
            val now = System.currentTimeMillis()
            val remainingSeconds = ((endTimeMillis - now) / 1000).toInt()
            
            Log.d("BootReceiver", "⏰ Found active timer: $remainingSeconds seconds remaining")
            
            if (remainingSeconds > 0) {
                // Timer still running - reschedule fallback alarm
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                
                val alarmIntent = Intent(context, TimerAlarmReceiver::class.java)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    TimerService.FALLBACK_ALARM_REQUEST_CODE,
                    alarmIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        endTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        endTimeMillis,
                        pendingIntent
                    )
                }
                
                Log.d("BootReceiver", "✅ Timer fallback alarm rescheduled")
            } else {
                // Timer already finished - clear data
                prefs.edit().clear().apply()
                Log.d("BootReceiver", "🗑️ Expired timer data cleared")
            }
        }
    }
}
