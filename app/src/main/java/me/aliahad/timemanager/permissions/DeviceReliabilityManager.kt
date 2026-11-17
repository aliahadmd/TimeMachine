package me.aliahad.timemanager.permissions

import android.app.Activity
import android.app.NotificationManager
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import me.aliahad.timemanager.NotificationSettingsHelper
import me.aliahad.timemanager.R
import me.aliahad.timemanager.ScreenTimePermissionHelper
import me.aliahad.timemanager.focus.FocusZenManager
import me.aliahad.timemanager.notifications.NotificationChannelRegistry

enum class HealthSeverity {
    CRITICAL,
    RECOMMENDED,
    HEALTHY
}

data class ReliabilityCheckItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isReady: Boolean,
    val severity: HealthSeverity,
    val actionLabel: String
)

object DeviceReliabilityManager {

    fun isXiaomiOrHyperOs(): Boolean {
        val mfg = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return mfg.contains("xiaomi") ||
            brand.contains("xiaomi") ||
            brand.contains("redmi") ||
            brand.contains("poco")
    }

    fun inspectDeviceHealth(context: Context): List<ReliabilityCheckItem> {
        val items = mutableListOf<ReliabilityCheckItem>()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationsReady = NotificationSettingsHelper.areNotificationChannelsConfigured(context)
        items += ReliabilityCheckItem(
            id = "notifications",
            title = "Notifications & Reminder Channels",
            subtitle = if (notificationsReady) {
                "Alerts enabled for Habits, Planner & Finance"
            } else {
                "Allow notifications and keep Habit/Planner channels unmuted"
            },
            isReady = notificationsReady,
            severity = if (notificationsReady) HealthSeverity.HEALTHY else HealthSeverity.CRITICAL,
            actionLabel = if (notificationsReady) "Manage" else "Allow"
        )

        val exactAlarmReady = !ExactAlarmPermissionManager.needsExactAlarm(context)
        items += ReliabilityCheckItem(
            id = "exact_alarm",
            title = "Exact Alarms & Reminders",
            subtitle = if (exactAlarmReady) {
                "On-time wakeup scheduling allowed"
            } else {
                "Needed so Habit and Planner reminders fire at the exact minute"
            },
            isReady = exactAlarmReady,
            severity = if (exactAlarmReady) HealthSeverity.HEALTHY else HealthSeverity.CRITICAL,
            actionLabel = if (exactAlarmReady) "Open" else "Enable"
        )

        val batteryReady = ExactAlarmPermissionManager.hasBatteryException(context)
        items += ReliabilityCheckItem(
            id = "battery",
            title = "Unrestricted Battery",
            subtitle = if (batteryReady) {
                "Excluded from aggressive background throttling"
            } else {
                "Prevent the system from pausing timers or freezing background alarms"
            },
            isReady = batteryReady,
            severity = if (batteryReady) HealthSeverity.HEALTHY else HealthSeverity.CRITICAL,
            actionLabel = if (batteryReady) "Settings" else "Set Unrestricted"
        )

        val usageReady = ScreenTimePermissionHelper.hasUsageAccess(context)
        val restrictedSideload = ScreenTimePermissionHelper.isLikelyRestrictedSettingsBlocked(context)
        items += ReliabilityCheckItem(
            id = "usage_stats",
            title = "Usage Access (Screen Time)",
            subtitle = when {
                usageReady -> "Active for daily Screen Time & app breakdown"
                restrictedSideload -> "Android Restricted Settings active: open App Info -> ⋮ -> Allow restricted settings first"
                else -> "Grant Usage Access to unlock automatic Screen Time tracking"
            },
            isReady = usageReady,
            severity = if (usageReady) HealthSeverity.HEALTHY else HealthSeverity.RECOMMENDED,
            actionLabel = when {
                usageReady -> "Open"
                restrictedSideload -> "Open App Info"
                else -> "Grant"
            }
        )

        if (Build.VERSION.SDK_INT >= 36) {
            val promotedReady = runCatching { nm.canPostPromotedNotifications() }.getOrDefault(false)
            items += ReliabilityCheckItem(
                id = "promoted_live",
                title = if (isXiaomiOrHyperOs()) "Live Updates / Super Island" else "Android Live Updates",
                subtitle = if (promotedReady) {
                    "Active Focus timer promoted on Lock Screen & status chip"
                } else {
                    "Allow Promoted Notifications so Focus timers surface on Lock Screen & Super Island"
                },
                isReady = promotedReady,
                severity = if (promotedReady) HealthSeverity.HEALTHY else HealthSeverity.RECOMMENDED,
                actionLabel = if (promotedReady) "Settings" else "Enable"
            )
        }

        val dndReady = FocusZenManager.hasDndPolicyAccess(context)
        items += ReliabilityCheckItem(
            id = "focus_dnd",
            title = "Do Not Disturb Access",
            subtitle = if (dndReady) {
                "Optional automatic Priority Mode during Focus sessions is unlocked"
            } else {
                "Optional: silence distracting alerts while Focus timer runs"
            },
            isReady = dndReady,
            severity = if (dndReady) HealthSeverity.HEALTHY else HealthSeverity.RECOMMENDED,
            actionLabel = if (dndReady) "Manage" else "Grant"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val fsiReady = nm.canUseFullScreenIntent()
            items += ReliabilityCheckItem(
                id = "full_screen",
                title = "Full-Screen Timer Alarm",
                subtitle = if (fsiReady) {
                    "Optional strict completion alarm allowed over Lock Screen"
                } else {
                    "Allow Full-Screen Intent for strict Focus end-of-timer alarms"
                },
                isReady = fsiReady,
                severity = if (fsiReady) HealthSeverity.HEALTHY else HealthSeverity.RECOMMENDED,
                actionLabel = if (fsiReady) "Open" else "Allow"
            )
        }

        if (isXiaomiOrHyperOs()) {
            val autostartIntent = Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
            if (isExportedActivityAvailable(context, autostartIntent)) {
                items += ReliabilityCheckItem(
                    id = "hyperos_autostart",
                    title = "HyperOS Autostart",
                    subtitle = "Enable Autostart in HyperOS Security so alarms recover immediately after reboot",
                    isReady = false,
                    severity = HealthSeverity.RECOMMENDED,
                    actionLabel = "Open Autostart"
                )
            }
            val focusNotifIntent = Intent("miui.settings.FOCUS_NOTIFICATION_SETTINGS").apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            if (isExportedActivityAvailable(context, focusNotifIntent)) {
                items += ReliabilityCheckItem(
                    id = "hyperos_island",
                    title = "HyperOS Super Island & Focus Alerts",
                    subtitle = "Allow Live Focus alerts on Lock Screen, Floating & Super Island surfaces",
                    isReady = false,
                    severity = HealthSeverity.RECOMMENDED,
                    actionLabel = "Configure"
                )
            }
        }

        return items
    }

    fun handleCheckAction(context: Context, checkId: String) {
        when (checkId) {
            "notifications" -> NotificationSettingsHelper.openNotificationSettings(context)
            "promoted_live", "hyperos_island" -> openHyperOsFocusOrNotifications(context)
            "exact_alarm" -> {
                if (context is Activity) {
                    ExactAlarmPermissionManager.openExactAlarmSettings(context)
                } else {
                    openAppDetails(context)
                }
            }

            "battery" -> {
                if (context is Activity) {
                    ExactAlarmPermissionManager.requestBatteryException(context)
                } else {
                    openAppDetails(context)
                }
            }

            "usage_stats" -> {
                if (ScreenTimePermissionHelper.isLikelyRestrictedSettingsBlocked(context)) {
                    openAppDetails(context)
                } else {
                    ScreenTimePermissionHelper.openUsageAccessSettings(context)
                }
            }

            "focus_dnd" -> FocusZenManager.openDndPolicySettings(context)
            "full_screen" -> openFullScreenIntentSettings(context)
            "hyperos_autostart" -> openHyperOsAutostart(context)
            else -> openAppDetails(context)
        }
    }

    fun openAppDetails(context: Context) {
        launchExportedSafely(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
    }

    private fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (launchExportedSafely(context, intent)) return
        }
        NotificationSettingsHelper.openNotificationSettings(context)
    }

    private fun openHyperOsAutostart(context: Context) {
        val miuiIntent = Intent("miui.intent.action.OP_AUTO_START").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra("packageName", context.packageName)
        }
        if (launchExportedSafely(context, miuiIntent)) return
        openAppDetails(context)
    }

    private fun openHyperOsFocusOrNotifications(context: Context) {
        if (isXiaomiOrHyperOs()) {
            val candidates = listOf(
                "miui.settings.FOCUS_NOTIFICATION_SETTINGS",
                "miui.settings.LOCK_SCREEN_NOTIFICATION_SETTINGS",
                "miui.settings.FLOAT_NOTIFICATION_SETTINGS"
            )
            for (actionName in candidates) {
                val intent = Intent(actionName).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                if (launchExportedSafely(context, intent)) return
            }
        }
        NotificationSettingsHelper.openNotificationSettings(context)
    }

    fun requestAddQuickSettingsTile(
        context: Context,
        tileServiceClass: Class<*>,
        label: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
        statusBar.requestAddTileService(
            ComponentName(context, tileServiceClass),
            label,
            Icon.createWithResource(context, R.drawable.ic_stat_timemachine),
            context.mainExecutor
        ) {}
    }

    fun requestPinWidget(
        context: Context,
        receiverClass: Class<out AppWidgetProvider>
    ): Boolean {
        val awm = AppWidgetManager.getInstance(context)
        if (!awm.isRequestPinAppWidgetSupported) return false
        return awm.requestPinAppWidget(ComponentName(context, receiverClass), null, null)
    }

    private fun isExportedActivityAvailable(context: Context, intent: Intent): Boolean {
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.exported == true
    }

    private fun launchExportedSafely(context: Context, intent: Intent): Boolean {
        if (!isExportedActivityAvailable(context, intent)) return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
