package me.aliahad.timemanager.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.R
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.datastore.UserPreferencesRepository
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway
import java.time.LocalDate

class AndroidSystemNotificationGateway(
    private val context: Context,
    private val database: TimerDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val widgetRefreshAction: (Context) -> Unit = {}
) : SystemNotificationGateway {

    override suspend fun onFocusSessionSaved(
        categoryName: String,
        durationMinutes: Int,
        linkedHabitNames: List<String>
    ) {
        if (linkedHabitNames.isEmpty()) return
        val prefs = preferencesRepository.getPreferences()
        if (!prefs.notificationsEnabled || NotificationChannelRegistry.isQuietHoursNow(prefs)) return
        if (!NotificationChannelRegistry.hasPostNotificationsPermission(context)) return

        NotificationChannelRegistry.ensureChannels(context)
        val notifId = NotificationChannelRegistry.WELLBEING_NOTIF_BASE + 20
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_WELLBEING
            )
        }
        val tapPending = PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannelRegistry.CHANNEL_WELLBEING_NUDGES
        )
            .setSmallIcon(R.drawable.ic_stat_timemachine)
            .setContentTitle("Focus goal unlocked a habit")
            .setContentText("Auto-completed: ${linkedHabitNames.joinToString(", ")}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (sec: SecurityException) {
            Log.w("AndroidNotifGateway", "Notification permission missing", sec)
        }
    }

    override suspend fun notifyBudgetThreshold(
        categoryId: Long,
        categoryName: String,
        spent: Double,
        budget: Double,
        currencySymbol: String,
        isOverBudget: Boolean
    ) {
        val prefs = preferencesRepository.getPreferences()
        if (!prefs.notificationsEnabled || NotificationChannelRegistry.isQuietHoursNow(prefs)) return
        if (!NotificationChannelRegistry.hasPostNotificationsPermission(context)) return

        val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)
        val ratePrefs = context.getSharedPreferences(
            FinanceAndDigestWorker.RATE_LIMIT_PREFS,
            Context.MODE_PRIVATE
        )
        val key = "budget_${categoryId}_${if (isOverBudget) "100" else "80"}_$todayIso"
        if (ratePrefs.getBoolean(key, false)) return

        NotificationChannelRegistry.ensureChannels(context)
        val notifId = NotificationChannelRegistry.FINANCE_NOTIF_BASE + 40_000 +
            ((categoryId and 0x7FFF_FFFFL) % 40_000L).toInt()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FINANCE
            )
            putExtra(NotificationChannelRegistry.EXTRA_INITIAL_TAB, 0)
        }
        val tapPending = PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val spentFmt = Formatters.formatCurrency(spent, currencySymbol)
        val budgetFmt = Formatters.formatCurrency(budget, currencySymbol)
        val title = if (isOverBudget) {
            "Budget Exceeded: $categoryName"
        } else {
            "80% Budget Reached: $categoryName"
        }

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannelRegistry.CHANNEL_FINANCE_ALERTS
        )
            .setSmallIcon(R.drawable.ic_stat_timemachine)
            .setContentTitle(title)
            .setContentText("Spent $spentFmt of $budgetFmt monthly limit")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
            ratePrefs.edit().putBoolean(key, true).apply()
        } catch (sec: SecurityException) {
            Log.w("AndroidNotifGateway", "Missing notification permission for budget alert", sec)
        }
    }

    override suspend fun syncTaskReminder(task: DailyTask) {
        NotificationChannelRegistry.ensureChannels(context)
        ReminderOrchestrator.scheduleTaskReminder(context, task)
    }

    override suspend fun cancelTaskReminder(taskId: Long) {
        ReminderOrchestrator.cancelTaskReminder(context, taskId)
    }

    override suspend fun syncHabitReminder(habit: Habit) {
        NotificationChannelRegistry.ensureChannels(context)
        ReminderOrchestrator.scheduleHabitReminder(context, habit)
    }

    override suspend fun cancelHabitReminder(habitId: Long) {
        ReminderOrchestrator.cancelHabitReminder(context, habitId)
    }

    override suspend fun syncAllReminders() {
        ReminderOrchestrator.syncAllReminders(context, database)
    }

    override suspend fun cancelAllReminders() {
        ReminderOrchestrator.cancelAllReminders(context, database)
        FinanceAndDigestWorker.clearRateLimits(context)
    }

    override fun requestWidgetRefresh() {
        widgetRefreshAction(context)
    }
}
