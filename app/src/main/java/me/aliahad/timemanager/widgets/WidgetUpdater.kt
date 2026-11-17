package me.aliahad.timemanager.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdater {
    private const val TAG = "WidgetUpdater"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                TodayFocusWidget().updateAll(appContext)
                TodayHabitsWidget().updateAll(appContext)
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to refresh Glance widgets: ${t.message}")
            }
        }
    }
}
