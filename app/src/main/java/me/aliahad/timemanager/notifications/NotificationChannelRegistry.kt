package me.aliahad.timemanager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.aliahad.timemanager.core.datastore.UserPreferences
import java.time.LocalTime

object NotificationChannelRegistry {
    const val CHANNEL_FOCUS_LIVE = "tm_focus_live"
    const val CHANNEL_FOCUS_COMPLETE = "tm_focus_complete"
    const val CHANNEL_HABIT_REMINDERS = "tm_habit_reminders"
    const val CHANNEL_PLANNER_TASKS = "tm_planner_tasks"
    const val CHANNEL_FINANCE_ALERTS = "tm_finance_alerts"
    const val CHANNEL_WELLBEING_NUDGES = "tm_wellbeing_nudges"

    private const val LEGACY_HABIT_CHANNEL = "HabitReminderChannel"
    private const val LEGACY_SCREEN_TIME_CHANNEL = "screen_time_channel"

    // Partitioned ID spaces to eliminate PendingIntent / Notification ID collisions
    const val FOCUS_LIVE_NOTIF_ID = 10_001
    const val FOCUS_COMPLETE_NOTIF_ID = 10_002
    const val FOCUS_ACTION_BASE = 50_000
    const val HABIT_ALARM_BASE = 100_000
    const val HABIT_CONTENT_BASE = 150_000
    const val HABIT_ACTION_BASE = 200_000
    const val PLANNER_ALARM_BASE = 300_000
    const val PLANNER_CONTENT_BASE = 350_000
    const val PLANNER_ACTION_BASE = 400_000
    const val FINANCE_NOTIF_BASE = 500_000
    const val WELLBEING_NOTIF_BASE = 600_000

    const val GROUP_KEY_HABITS = "tm_group_habits"
    const val GROUP_KEY_PLANNER = "tm_group_planner"
    const val GROUP_KEY_FINANCE = "tm_group_finance"

    // Deep-link extras
    const val EXTRA_ROUTE_TARGET = "tm_route_target"
    const val EXTRA_OPEN_HABIT_ID = "OPEN_HABIT_ID"
    const val EXTRA_OPEN_DATE = "tm_open_date"
    const val EXTRA_TASK_ID = "tm_task_id"
    const val EXTRA_CATEGORY_ID = "tm_category_id"
    const val EXTRA_INITIAL_TAB = "tm_initial_tab"
    const val EXTRA_AUTO_START_FOCUS = "tm_auto_start_focus"

    const val ROUTE_TODAY = "today"
    const val ROUTE_PLANNER = "planner"
    const val ROUTE_FOCUS = "focus"
    const val ROUTE_FINANCE = "finance"
    const val ROUTE_WELLBEING = "wellbeing"
    const val ROUTE_HABIT_DETAIL = "habit_detail"
    const val ROUTE_SETTINGS = "settings"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Clean up superseded legacy channel IDs if present
        manager.deleteNotificationChannel(LEGACY_HABIT_CHANNEL)
        manager.deleteNotificationChannel(LEGACY_SCREEN_TIME_CHANNEL)

        val focusLive = NotificationChannel(
            CHANNEL_FOCUS_LIVE,
            "Active Focus Session",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Live ongoing Focus and Pomodoro timer status"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val focusComplete = NotificationChannel(
            CHANNEL_FOCUS_COMPLETE,
            "Focus Timer Completed",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Chime when a countdown or Pomodoro phase finishes"
            setShowBadge(true)
            setSound(alarmSound, alarmAttrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 150, 350)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notifAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val habits = NotificationChannel(
            CHANNEL_HABIT_REMINDERS,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders for daily and weekday habits"
            setShowBadge(true)
            setSound(notifSound, notifAttrs)
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val planner = NotificationChannel(
            CHANNEL_PLANNER_TASKS,
            "Planner Time Blocks",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders before scheduled tasks and focus blocks"
            setShowBadge(true)
            setSound(notifSound, notifAttrs)
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val finance = NotificationChannel(
            CHANNEL_FINANCE_ALERTS,
            "Renewals & Budgets",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Subscription renewal alerts, trial warnings, and category budget caps"
            setShowBadge(true)
            setSound(notifSound, notifAttrs)
        }

        val wellbeing = NotificationChannel(
            CHANNEL_WELLBEING_NUDGES,
            "Wellbeing & Screen Time",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Screen-time threshold alerts and evening habit digests"
            setShowBadge(false)
        }

        manager.createNotificationChannels(
            listOf(focusLive, focusComplete, habits, planner, finance, wellbeing)
        )
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        if (!hasPostNotificationsPermission(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun isQuietHoursNow(
        prefs: UserPreferences,
        now: LocalTime = LocalTime.now()
    ): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val start = LocalTime.of(
            prefs.quietStartHour.coerceIn(0, 23),
            prefs.quietStartMinute.coerceIn(0, 59)
        )
        val end = LocalTime.of(
            prefs.quietEndHour.coerceIn(0, 23),
            prefs.quietEndMinute.coerceIn(0, 59)
        )
        if (start == end) return false
        return if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            // Overnight span (e.g., 22:00 .. 07:00)
            !now.isBefore(start) || now.isBefore(end)
        }
    }
}
