package me.aliahad.timemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import me.aliahad.timemanager.notifications.NotificationChannelRegistry

object NotificationSettingsHelper {

    fun areNotificationChannelsConfigured(context: Context): Boolean {
        NotificationChannelRegistry.ensureChannels(context)
        return NotificationChannelRegistry.hasPostNotificationsPermission(context) &&
            NotificationChannelRegistry.isChannelEnabled(
                context,
                NotificationChannelRegistry.CHANNEL_HABIT_REMINDERS
            ) &&
            NotificationChannelRegistry.isChannelEnabled(
                context,
                NotificationChannelRegistry.CHANNEL_PLANNER_TASKS
            )
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        startSafely(context, intent)
    }

    fun openChannelSettings(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
            if (!startSafely(context, intent)) {
                openNotificationSettings(context)
            }
        } else {
            openNotificationSettings(context)
        }
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            } catch (_: Throwable) {
                false
            }
        }
    }
}
