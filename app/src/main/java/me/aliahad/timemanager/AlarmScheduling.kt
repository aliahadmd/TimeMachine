package me.aliahad.timemanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log

/**
 * Schedules a reliable alarm: uses exact wakeup when permitted, and degrades safely to
 * `setAndAllowWhileIdle` (without requiring exact-alarm privileges) when restricted.
 */
fun AlarmManager.scheduleReliableAlarm(
    triggerAtMillis: Long,
    operation: PendingIntent,
    tag: String
): Boolean {
    return try {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
            Log.d(tag, "Scheduled exact wakeup alarm for $triggerAtMillis")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
            Log.w(tag, "Exact alarm permission denied; scheduled inexact idle-safe alarm for $triggerAtMillis")
        }
        true
    } catch (security: SecurityException) {
        Log.e(tag, "SecurityException scheduling alarm: ${security.message}", security)
        false
    } catch (throwable: Throwable) {
        Log.e(tag, "Error scheduling alarm: ${throwable.message}", throwable)
        false
    }
}
