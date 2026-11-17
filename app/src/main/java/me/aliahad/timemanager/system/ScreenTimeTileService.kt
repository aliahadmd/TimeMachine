package me.aliahad.timemanager.system

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.ScreenTimePermissionHelper
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import java.time.LocalDate

class ScreenTimeTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val hasAccess = ScreenTimePermissionHelper.hasUsageAccess(this)
        if (!hasAccess) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Screen Time"
            tile.subtitle = "Grant access"
            tile.updateTile()
            return
        }

        scope.launch {
            val container = appContainerOrNull() ?: return@launch
            val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)
            val screenMinutes = (container.database.screenTimeDao().getDailySummary(todayIso)
                ?.totalScreenTimeSeconds ?: 0) / 60
            val focusMinutes = container.database.timeSessionDao().getTotalMinutesForDate(todayIso) ?: 0
            val limitMinutes = container.preferencesRepository.getPreferences().dailyScreenTimeLimitMinutes

            tile.state = if (screenMinutes <= limitMinutes) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Screen ${Formatters.formatMinutes(screenMinutes)}"
            tile.subtitle = "Focus ${Formatters.formatMinutes(focusMinutes)}"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_WELLBEING
            )
            putExtra(NotificationChannelRegistry.EXTRA_INITIAL_TAB, 1)
        }
        if (isLocked) {
            unlockAndRun { launchSafely(intent) }
        } else {
            launchSafely(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchSafely(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                NotificationChannelRegistry.WELLBEING_NOTIF_BASE + 51,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
