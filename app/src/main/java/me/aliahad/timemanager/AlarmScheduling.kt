package me.aliahad.timemanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log

/**
 * Schedules an exact alarm when possible, falling back to AlarmClock (user visible) or
 * the best available alternative to ensure delivery while the app is closed.
 */
fun AlarmManager.scheduleReliableAlarm(
    triggerAtMillis: Long,
    showIntent: PendingIntent?,
    operation: PendingIntent,
    tag: String
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            if (showIntent != null) {
                val alarmInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                setAlarmClock(alarmInfo, operation)
                Log.w(tag, "AlarmClock fallback scheduled for $triggerAtMillis")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                Log.w(tag, "setAndAllowWhileIdle fallback scheduled for $triggerAtMillis")
            } else {
                set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                Log.w(tag, "set fallback scheduled for $triggerAtMillis")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            Log.d(tag, "Exact alarm scheduled for $triggerAtMillis")
        } else {
            setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            Log.d(tag, "Exact alarm scheduled (legacy) for $triggerAtMillis")
        }
    } catch (security: SecurityException) {
        Log.e(tag, "SecurityException scheduling alarm: ${security.message}", security)
    } catch (throwable: Throwable) {
        Log.e(tag, "Error scheduling alarm: ${throwable.message}", throwable)
    }
}
