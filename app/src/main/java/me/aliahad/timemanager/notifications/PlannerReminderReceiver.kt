package me.aliahad.timemanager.notifications

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
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.R
import me.aliahad.timemanager.core.di.appContainerOrNull

class PlannerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationChannelRegistry.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationChannelRegistry.ensureChannels(context)
                val container = context.appContainerOrNull() ?: return@launch
                val prefs = container.preferencesRepository.getPreferences()
                if (!prefs.notificationsEnabled) return@launch
                if (NotificationChannelRegistry.isQuietHoursNow(prefs)) return@launch

                val task = container.database.dailyTaskDao().getTaskById(taskId) ?: return@launch
                if (task.isCompleted) return@launch
                if (!NotificationChannelRegistry.hasPostNotificationsPermission(context)) return@launch

                val notifId = NotificationChannelRegistry.PLANNER_CONTENT_BASE +
                    ((taskId and 0x7FFF_FFFFL) % 45_000L).toInt()

                val contentIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(
                        NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                        NotificationChannelRegistry.ROUTE_PLANNER
                    )
                    putExtra(NotificationChannelRegistry.EXTRA_OPEN_DATE, task.date)
                    putExtra(NotificationChannelRegistry.EXTRA_TASK_ID, task.id)
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_TASK_DONE
                    putExtra(NotificationChannelRegistry.EXTRA_TASK_ID, task.id)
                }
                val donePendingIntent = PendingIntent.getBroadcast(
                    context,
                    NotificationChannelRegistry.PLANNER_ACTION_BASE +
                        ((task.id and 0x7FFF_FFFFL) % 20_000L).toInt(),
                    doneIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_TASK_SNOOZE
                    putExtra(NotificationChannelRegistry.EXTRA_TASK_ID, task.id)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    NotificationChannelRegistry.PLANNER_ACTION_BASE + 45_000 +
                        ((task.id and 0x7FFF_FFFFL) % 20_000L).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(
                    context,
                    NotificationChannelRegistry.CHANNEL_PLANNER_TASKS
                )
                    .setSmallIcon(R.drawable.ic_stat_timemachine)
                    .setContentTitle("${task.icon} ${task.title}")
                    .setContentText("Starts at ${task.startTime} • ${task.taskType}")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            buildString {
                                append("Scheduled ${task.startTime} – ${task.endTime}")
                                if (task.description.isNotBlank()) {
                                    append("\n${task.description}")
                                }
                            }
                        )
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setGroup(NotificationChannelRegistry.GROUP_KEY_PLANNER)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .addAction(0, "Mark Done", donePendingIntent)
                    .addAction(0, "Snooze 10m", snoozePendingIntent)

                if (task.taskType.equals("FOCUS", ignoreCase = true)) {
                    val startFocusIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(
                            NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                            NotificationChannelRegistry.ROUTE_FOCUS
                        )
                        putExtra(NotificationChannelRegistry.EXTRA_TASK_ID, task.id)
                        putExtra(
                            NotificationChannelRegistry.EXTRA_CATEGORY_ID,
                            task.linkedCategoryId ?: 0L
                        )
                        putExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, true)
                    }
                    val focusPendingIntent = PendingIntent.getActivity(
                        context,
                        NotificationChannelRegistry.PLANNER_ACTION_BASE + 22_000 +
                            ((task.id and 0x7FFF_FFFFL) % 20_000L).toInt(),
                        startFocusIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, "Start Focus", focusPendingIntent)
                }

                try {
                    NotificationManagerCompat.from(context).notify(notifId, builder.build())
                } catch (sec: SecurityException) {
                    Log.w("PlannerReminderReceiver", "Missing notification permission", sec)
                }
            } catch (t: Throwable) {
                Log.e("PlannerReminderReceiver", "Error posting planner reminder", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
