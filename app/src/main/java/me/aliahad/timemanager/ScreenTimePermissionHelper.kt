package me.aliahad.timemanager

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.getSystemService

object ScreenTimePermissionHelper {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 36) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
                context.attributionTag
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isLikelyRestrictedSettingsBlocked(context: Context): Boolean {
        if (hasUsageAccess(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        return try {
            val installer = context.packageManager
                .getInstallSourceInfo(context.packageName)
                .installingPackageName
            installer == null ||
                installer == "com.android.shell" ||
                installer == "com.google.android.packageinstaller" ||
                installer == "com.miui.packageinstaller"
        } catch (_: Throwable) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
        }
    }
}
