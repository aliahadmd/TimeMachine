package me.aliahad.timemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
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
            
            // Call goAsync() to ensure the receiver stays alive during async work
            val pendingResult = goAsync()
            
            // Recreate notification channel first (must be done before any notifications)
            createReminderNotificationChannel(context)
            
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
                    
                    // Schedule screen time background work
                    ScreenTimeScheduler.ensurePeriodicWork(context)
                    ScreenTimeScheduler.triggerImmediateSync(context)
                    Log.d("BootReceiver", "✅ Screen Time work scheduled on boot")
                    
                    Log.d("BootReceiver", "✅ Boot completed, all services initialized")
                    
                } catch (e: Exception) {
                    Log.e("BootReceiver", "❌ Error on boot: ${e.message}")
                } finally {
                    // Signal that async work is complete
                    pendingResult.finish()
                }
            }
        }
    }
    
    private fun createReminderNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            
            val channel = NotificationChannel(
                "HabitReminderChannel",
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for your habits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                setSound(reminderSound, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("BootReceiver", "✅ Notification channel recreated")
        }
    }
}
