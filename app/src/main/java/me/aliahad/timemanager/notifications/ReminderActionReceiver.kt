package me.aliahad.timemanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.data.CompletionType
import java.time.LocalDate

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = context.appContainerOrNull() ?: return@launch
                val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)

                when (action) {
                    ACTION_HABIT_ACHIEVED -> {
                        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                        if (habitId > 0L) {
                            container.habitSyncUseCase.recordCompletion(
                                habitId = habitId,
                                date = todayIso,
                                completionType = CompletionType.ACHIEVED,
                                notes = "Completed from notification"
                            )
                            dismissHabitNotification(context, habitId)
                        }
                    }

                    ACTION_HABIT_SNOOZE -> {
                        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Habit"
                        if (habitId > 0L) {
                            dismissHabitNotification(context, habitId)
                            ReminderOrchestrator.snoozeHabitReminder(
                                context = context,
                                habitId = habitId,
                                habitName = habitName,
                                delayMinutes = 15
                            )
                        }
                    }

                    ACTION_HABIT_DISMISS -> {
                        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                        if (habitId > 0L) {
                            dismissHabitNotification(context, habitId)
                        }
                    }

                    ACTION_TASK_DONE -> {
                        val taskId = intent.getLongExtra(
                            NotificationChannelRegistry.EXTRA_TASK_ID,
                            -1L
                        )
                        if (taskId > 0L) {
                            val task = container.plannerRepository.getTaskById(taskId)
                            if (task != null && !task.isCompleted) {
                                container.plannerRepository.updateTask(task.copy(isCompleted = true))
                            }
                            NotificationManagerCompat.from(context).cancel(
                                NotificationChannelRegistry.PLANNER_CONTENT_BASE +
                                    ((taskId and 0x7FFF_FFFFL) % 45_000L).toInt()
                            )
                        }
                    }

                    ACTION_TASK_SNOOZE -> {
                        val taskId = intent.getLongExtra(
                            NotificationChannelRegistry.EXTRA_TASK_ID,
                            -1L
                        )
                        if (taskId > 0L) {
                            NotificationManagerCompat.from(context).cancel(
                                NotificationChannelRegistry.PLANNER_CONTENT_BASE +
                                    ((taskId and 0x7FFF_FFFFL) % 45_000L).toInt()
                            )
                            ReminderOrchestrator.snoozeTaskReminder(context, taskId, 10)
                        }
                    }

                    ACTION_SNOOZE_SUBSCRIPTION -> {
                        val subId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1L)
                        if (subId > 0L) {
                            NotificationManagerCompat.from(context).cancel(
                                NotificationChannelRegistry.FINANCE_NOTIF_BASE +
                                    ((subId and 0x7FFF_FFFFL) % 40_000L).toInt()
                            )
                            val tomorrowIso = java.time.LocalDate.now().plusDays(1)
                                .format(me.aliahad.timemanager.core.common.SafeDateTime.ISO_DATE)
                            context.getSharedPreferences(
                                FinanceAndDigestWorker.RATE_LIMIT_PREFS,
                                Context.MODE_PRIVATE
                            ).edit()
                                .putBoolean("renew_${subId}_$tomorrowIso", true)
                                .putBoolean("trial_${subId}_$tomorrowIso", true)
                                .apply()
                        }
                    }

                    ACTION_RENEW_SUBSCRIPTION -> {
                        val subId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1L)
                        if (subId > 0L) {
                            val sub = container.database.subscriptionDao().getSubscriptionById(subId)
                            if (sub != null) {
                                container.financeUseCases.renewSubscriptionAndMaybeLogExpense(
                                    subscription = sub,
                                    forceLogExpense = true
                                )
                            }
                            NotificationManagerCompat.from(context).cancel(
                                NotificationChannelRegistry.FINANCE_NOTIF_BASE +
                                    ((subId and 0x7FFF_FFFFL) % 40_000L).toInt()
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error handling notification action $action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun dismissHabitNotification(context: Context, habitId: Long) {
        val notifId = NotificationChannelRegistry.HABIT_CONTENT_BASE +
            ((habitId and 0x7FFF_FFFFL) % 45_000L).toInt()
        NotificationManagerCompat.from(context).cancel(notifId)
    }

    companion object {
        private const val TAG = "ReminderActionReceiver"

        const val ACTION_HABIT_ACHIEVED = "me.aliahad.timemanager.action.HABIT_ACHIEVED"
        const val ACTION_HABIT_SNOOZE = "me.aliahad.timemanager.action.HABIT_SNOOZE"
        const val ACTION_HABIT_DISMISS = "me.aliahad.timemanager.action.HABIT_DISMISS"
        const val ACTION_TASK_DONE = "me.aliahad.timemanager.action.TASK_DONE"
        const val ACTION_TASK_SNOOZE = "me.aliahad.timemanager.action.TASK_SNOOZE"
        const val ACTION_RENEW_SUBSCRIPTION = "me.aliahad.timemanager.action.RENEW_SUBSCRIPTION"
        const val ACTION_SNOOZE_SUBSCRIPTION = "me.aliahad.timemanager.action.SNOOZE_SUBSCRIPTION"

        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_SUBSCRIPTION_ID = "extra_subscription_id"
    }
}
