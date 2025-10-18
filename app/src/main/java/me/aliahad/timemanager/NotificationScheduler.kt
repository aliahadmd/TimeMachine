package me.aliahad.timemanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.aliahad.timemanager.data.Habit
import java.util.Calendar

object NotificationScheduler {
    
    fun scheduleHabitReminder(context: Context, habit: Habit) {
        if (habit.reminderTimeHour == null || habit.reminderTimeMinute == null) {
            Log.d("NotificationScheduler", "No reminder time set for habit ${habit.name}")
            return
        }
        
        if (habit.id == 0L) {
            Log.e("NotificationScheduler", "Cannot schedule reminder for habit with ID 0 - save habit to database first!")
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habit.id)
            putExtra("HABIT_NAME", habit.name)
            putExtra("REMINDER_HOUR", habit.reminderTimeHour)
            putExtra("REMINDER_MINUTE", habit.reminderTimeMinute)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_HABIT_ID", habit.id)
        }

        val showIntent = PendingIntent.getActivity(
            context,
            (habit.id + 5000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val triggerAtMillis = calendar.timeInMillis
        alarmManager.scheduleReliableAlarm(
            triggerAtMillis,
            showIntent,
            pendingIntent,
            tag = "NotificationScheduler"
        )

        ReminderRegistry.saveReminder(context, habit)
    }
    
    fun cancelHabitReminder(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        ReminderRegistry.removeReminder(context, habitId)
        Log.d("NotificationScheduler", "Cancelled reminder for habit $habitId")
    }
}

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra("HABIT_ID", -1)
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Habit"
        val reminderHour = intent.getIntExtra("REMINDER_HOUR", -1)
        val reminderMinute = intent.getIntExtra("REMINDER_MINUTE", -1)
        
        Log.d("HabitReminderReceiver", "⏰ Reminder triggered for: $habitName (ID: $habitId)")
        
        showReminderNotification(context, habitId, habitName)
        
        // Reschedule for next day (since we're using setExactAndAllowWhileIdle, not repeating)
        if (reminderHour != -1 && reminderMinute != -1) {
            rescheduleForNextDay(context, habitId, habitName, reminderHour, reminderMinute)
        }
    }
    
    private fun rescheduleForNextDay(context: Context, habitId: Long, habitName: String, hour: Int, minute: Int) {
        try {
            // Create a temporary Habit object just for rescheduling
            val habitForReschedule = me.aliahad.timemanager.data.Habit(
                id = habitId,
                name = habitName,
                color = 0L,
                iconName = "",
                type = me.aliahad.timemanager.data.HabitType.BUILD,
                goalPeriodDays = 0,
                isEveryday = false,
                reminderTimeHour = hour,
                reminderTimeMinute = minute
            )
            NotificationScheduler.scheduleHabitReminder(context, habitForReschedule)
            Log.d("HabitReminderReceiver", "✅ Rescheduled reminder for tomorrow: $habitName")
        } catch (e: Exception) {
            Log.e("HabitReminderReceiver", "❌ Error rescheduling: ${e.message}")
        }
    }
    
    private fun showReminderNotification(context: Context, habitId: Long, habitName: String) {
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_HABIT_ID", habitId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, "HabitReminderChannel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $habitName")
            .setContentText("Time to track your habit!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(habitId.toInt() + 10000, notification)
        } catch (e: SecurityException) {
            Log.e("HabitReminderReceiver", "No notification permission", e)
        }
    }
}
