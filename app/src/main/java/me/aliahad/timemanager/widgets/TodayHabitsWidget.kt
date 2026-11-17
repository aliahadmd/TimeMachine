package me.aliahad.timemanager.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import java.time.LocalDate

class TodayHabitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            val container = context.appContainerOrNull()
            val today = LocalDate.now()
            val todayIso = today.format(SafeDateTime.ISO_DATE)
            val scheduled = container?.database?.habitDao()?.getAllActiveHabitsSync()
                ?.filter { HabitRepository.isScheduledOnDate(it, today) }
                .orEmpty()
            val items = scheduled.take(3).map { habit ->
                val done = container?.database?.habitDao()?.getCompletion(habit.id, todayIso)
                    ?.completionType == CompletionType.ACHIEVED
                WidgetHabitRow(habit.id, habit.name, done)
            }
            val doneCount = scheduled.count { habit ->
                container?.database?.habitDao()?.getCompletion(habit.id, todayIso)
                    ?.completionType == CompletionType.ACHIEVED
            }
            val nextTask = container?.database?.dailyTaskDao()?.getTasksForDateSync(todayIso)
                ?.firstOrNull { !it.isCompleted }
                ?.let { "${it.startTime} ${it.title}" }
            WidgetHabitsSnapshot(doneCount, scheduled.size, items, nextTask)
        }

        val openWellbeingIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_WELLBEING
            )
            putExtra(NotificationChannelRegistry.EXTRA_INITIAL_TAB, 0)
        }

        provideContent {
            GlanceTheme {
                HabitsWidgetContent(snapshot, openWellbeingIntent)
            }
        }
    }

    @Composable
    private fun HabitsWidgetContent(
        data: WidgetHabitsSnapshot,
        openIntent: Intent
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(14.dp)
                .clickable(actionStartActivity(openIntent))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S HABITS",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "${data.completedCount}/${data.totalCount}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            if (data.rows.isEmpty()) {
                Text(
                    text = "No habits scheduled today",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            } else {
                data.rows.forEach { row ->
                    val prefix = if (row.isDone) "✓ " else "○ "
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable(
                                actionRunCallback<ToggleHabitWidgetAction>(
                                    actionParametersOf(HabitIdParamKey to row.habitId)
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prefix + row.name,
                            style = TextStyle(
                                color = if (row.isDone) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (row.isDone) FontWeight.Bold else FontWeight.Normal
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
            data.nextTaskLabel?.let { nextLabel ->
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Next: $nextLabel",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

class ToggleHabitWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdParamKey] ?: return
        val container = context.appContainerOrNull() ?: return
        val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)
        withContext(Dispatchers.IO) {
            val existing = container.database.habitDao().getCompletion(habitId, todayIso)
            if (existing == null) {
                container.habitSyncUseCase.recordCompletion(
                    habitId = habitId,
                    date = todayIso,
                    completionType = CompletionType.ACHIEVED,
                    notes = "Completed from widget"
                )
            } else {
                container.habitSyncUseCase.removeCompletion(habitId, todayIso)
            }
        }
        TodayHabitsWidget().update(context, glanceId)
    }
}

private val HabitIdParamKey = ActionParameters.Key<Long>("habit_id")

private data class WidgetHabitRow(
    val habitId: Long,
    val name: String,
    val isDone: Boolean
)

private data class WidgetHabitsSnapshot(
    val completedCount: Int,
    val totalCount: Int,
    val rows: List<WidgetHabitRow>,
    val nextTaskLabel: String? = null
)

class TodayHabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayHabitsWidget()
}
