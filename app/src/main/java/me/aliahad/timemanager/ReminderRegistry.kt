package me.aliahad.timemanager

import android.content.Context
import me.aliahad.timemanager.data.Habit

data class HabitReminderMetadata(
    val habitId: Long,
    val hour: Int,
    val minute: Int
)

/**
 * Lightweight persistence for active habit reminders so we can rebuild alarms after
 * reboots, app updates, or when the app process is killed.
 */
object ReminderRegistry {

    private const val PREFS_NAME = "habit_reminder_registry"
    private const val KEY_ENTRIES = "entries"

    fun saveReminder(context: Context, habit: Habit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = prefs.getStringSet(KEY_ENTRIES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val entry = buildString {
            append(habit.id)
            append("|")
            append(habit.reminderTimeHour)
            append("|")
            append(habit.reminderTimeMinute)
        }
        entries.removeIf { it.startsWith("${habit.id}|") }
        entries.add(entry)
        prefs.edit().putStringSet(KEY_ENTRIES, entries).apply()
    }

    fun removeReminder(context: Context, habitId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = prefs.getStringSet(KEY_ENTRIES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (entries.removeIf { it.startsWith("$habitId|") }) {
            prefs.edit().putStringSet(KEY_ENTRIES, entries).apply()
        }
    }

    fun getReminders(context: Context): List<HabitReminderMetadata> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = prefs.getStringSet(KEY_ENTRIES, emptySet()) ?: emptySet()
        return entries.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                val id = parts[0].toLongOrNull()
                val hour = parts[1].toIntOrNull()
                val minute = parts[2].toIntOrNull()
                if (id != null && hour != null && minute != null) {
                    HabitReminderMetadata(id, hour, minute)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
}
