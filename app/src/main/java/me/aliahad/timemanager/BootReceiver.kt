package me.aliahad.timemanager

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.notifications.FinanceAndDigestWorker
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import me.aliahad.timemanager.notifications.ReminderOrchestrator

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supported = action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED

        if (!supported) return

        Log.d(TAG, "System event $action received; rebuilding channels and reminders")
        NotificationChannelRegistry.ensureChannels(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TimerDatabase.getDatabase(context.applicationContext)
                ReminderOrchestrator.syncAllReminders(context.applicationContext, database)
                ScreenTimeScheduler.ensurePeriodicWork(context.applicationContext)
                ScreenTimeScheduler.triggerImmediateSync(context.applicationContext)
                FinanceAndDigestWorker.ensureScheduled(context.applicationContext)
            } catch (t: Throwable) {
                Log.e(TAG, "Error handling system event $action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
