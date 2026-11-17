package me.aliahad.timemanager.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.focus.FocusTimerService
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import java.time.LocalDate

class TodayFocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = withContext(Dispatchers.IO) {
            val container = context.appContainerOrNull()
            val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)
            val focusMin = container?.database?.timeSessionDao()?.getTotalMinutesForDate(todayIso) ?: 0
            val goalMin = container?.preferencesRepository?.getPreferences()?.dailyFocusGoalMinutes ?: 120
            val screenSec = container?.database?.screenTimeDao()?.getDailySummary(todayIso)?.totalScreenTimeSeconds ?: 0
            val activeTimer = FocusTimerService.timerState.value
            WidgetFocusSnapshot(focusMin, goalMin, screenSec / 60, activeTimer.isRunning, activeTimer.categoryName)
        }

        val openFocusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FOCUS
            )
        }
        val quickStartIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FOCUS
            )
            putExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, true)
        }

        provideContent {
            GlanceTheme {
                FocusWidgetContent(summary, openFocusIntent, quickStartIntent)
            }
        }
    }

    @Composable
    private fun FocusWidgetContent(
        data: WidgetFocusSnapshot,
        openIntent: Intent,
        startIntent: Intent
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(14.dp)
                .clickable(actionStartActivity(openIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if (data.isTimerRunning) "FOCUSING • ${data.activeCategory}" else "TODAY'S FOCUS",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = "${Formatters.formatMinutes(data.focusMinutes)} / ${Formatters.formatMinutes(data.goalMinutes)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = "Screen Time: ${Formatters.formatMinutes(data.screenMinutes)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                Button(
                    text = if (data.isTimerRunning) "Open" else "Focus",
                    onClick = actionStartActivity(if (data.isTimerRunning) openIntent else startIntent)
                )
            }
        }
    }
}

private data class WidgetFocusSnapshot(
    val focusMinutes: Int,
    val goalMinutes: Int,
    val screenMinutes: Int,
    val isTimerRunning: Boolean,
    val activeCategory: String
)

class TodayFocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayFocusWidget()
}
