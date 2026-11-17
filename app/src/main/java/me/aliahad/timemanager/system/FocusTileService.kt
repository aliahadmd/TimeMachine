package me.aliahad.timemanager.system

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.focus.FocusTimerService
import me.aliahad.timemanager.notifications.NotificationChannelRegistry

class FocusTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val actionBlock = {
            val state = FocusTimerService.timerState.value
            if (state.isRunning) {
                FocusTimerService.sendAction(
                    this,
                    if (state.isPaused) FocusTimerService.ACTION_RESUME else FocusTimerService.ACTION_PAUSE
                )
                refreshTile()
            } else {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(
                        NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                        NotificationChannelRegistry.ROUTE_FOCUS
                    )
                    putExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, true)
                }
                launchSafely(intent)
            }
        }

        if (isLocked) {
            unlockAndRun(actionBlock)
        } else {
            actionBlock()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val state = FocusTimerService.timerState.value
        if (state.isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = state.categoryName.take(16)
            tile.subtitle = buildString {
                append(Formatters.formatTimerClock(state.displaySeconds))
                if (state.isPaused) append(" (Paused)")
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Focus Timer"
            tile.subtitle = "Tap to focus"
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchSafely(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                NotificationChannelRegistry.FOCUS_ACTION_BASE + 50,
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
