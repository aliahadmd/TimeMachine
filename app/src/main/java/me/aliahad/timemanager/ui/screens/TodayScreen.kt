package me.aliahad.timemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.HabitLinkType
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.TodayViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    onNavigateToPlanner: (String?) -> Unit,
    onNavigateToFocus: (categoryId: Long?, taskId: Long?, autoStart: Boolean) -> Unit,
    onNavigateToFinance: (Int) -> Unit,
    onNavigateToWellbeing: (Int) -> Unit,
    onNavigateToHabitDetail: (Long) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToYearCalculator: () -> Unit,
    onNavigateToBMICalculator: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val viewModel = appViewModel { container -> TodayViewModel(container) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDeviceHealth()
    }

    val statusColors = TimeMachineTheme.statusColors
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = "TimeMachine",
                subtitle = todayLabel,
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!state.bannerDismissed && state.healthIssues.isNotEmpty()) {
                item(key = "health_banner") {
                    val primaryIssue = state.healthIssues.first()
                    SectionCard(
                        containerColor = statusColors.warningContainer.copy(alpha = 0.45f),
                        borderColor = statusColors.warning.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = statusColors.warning
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${state.healthIssues.size} system setup check${if (state.healthIssues.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = primaryIssue.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = onNavigateToSettings) {
                                Text("Review")
                            }
                            IconButton(
                                onClick = { viewModel.dismissHealthBanner() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Focus vs Screen Time Pulse Card
            item(key = "daily_pulse") {
                val focusGoal = state.preferences.dailyFocusGoalMinutes.coerceAtLeast(1)
                val focusProgress = (state.todayFocusMinutes.toFloat() / focusGoal).coerceIn(0f, 1f)
                val screenLimit = state.preferences.dailyScreenTimeLimitMinutes.coerceAtLeast(1)
                val screenProgress = (state.todayScreenTimeMinutes.toFloat() / screenLimit).coerceIn(0f, 1f)

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DAILY BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${Formatters.formatMinutes(state.todayFocusMinutes)} Focus",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Goal ${Formatters.formatMinutes(focusGoal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { onNavigateToFocus(null, null, false) }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Focus")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { focusProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigateToWellbeing(1) }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Screen Time: ${Formatters.formatMinutes(state.todayScreenTimeMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Limit ${Formatters.formatMinutes(screenLimit)} • ${state.todayPickups} unlocks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { screenProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (screenProgress >= 1f) statusColors.danger else statusColors.wellbeingAccent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Now & Next Planner Block
            item(key = "now_and_next") {
                val spotlightTask = state.currentTask ?: state.nextTask
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PLANNER",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.plannerAccent
                            )
                            Text(
                                text = "${state.todayTasksCompleted}/${state.todayTasksTotal} completed today",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(onClick = { onNavigateToPlanner(null) }) {
                            Text("Schedule")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (spotlightTask == null) {
                        Text(
                            text = if (state.todayTasksTotal == 0) {
                                "No tasks scheduled yet for today."
                            } else {
                                "All scheduled tasks for today are done!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val isNow = state.currentTask?.id == spotlightTask.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, statusColors.cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(spotlightTask.icon, fontSize = 20.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isNow) "HAPPENING NOW" else "UP NEXT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isNow) statusColors.success else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = spotlightTask.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${spotlightTask.startTime} – ${spotlightTask.endTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleTaskCompleted(spotlightTask) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Done")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onNavigateToFocus(
                                                spotlightTask.linkedCategoryId,
                                                spotlightTask.id,
                                                true
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Start Focus")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Today's Habits Card
            item(key = "today_habits") {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TODAY'S HABITS",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.wellbeingAccent
                            )
                            Text(
                                text = "${state.habitsDoneToday} of ${state.scheduledHabitsToday.size} done",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(onClick = { onNavigateToWellbeing(0) }) {
                            Text("All Habits")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.scheduledHabitsToday.isEmpty()) {
                        Text(
                            text = "No habits scheduled for today. Create one in Wellbeing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.scheduledHabitsToday.take(5).forEach { habitStats ->
                                val habit = habitStats.habit
                                val done = habitStats.isCompletedToday
                                val accent = habit.color.toComposeColor()
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (done) statusColors.successContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToHabitDetail(habit.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleHabitToday(habitStats) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Toggle habit",
                                                tint = if (done) statusColors.success else accent
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = habit.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val meta = buildString {
                                                append("🔥 ${habitStats.currentStreak}d")
                                                if (habit.linkType == HabitLinkType.FOCUS_MINUTES) {
                                                    append(" • Auto-Focus")
                                                } else if (habit.linkType == HabitLinkType.SCREEN_TIME_UNDER_LIMIT) {
                                                    append(" • Screen Limit")
                                                }
                                            }
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Money & Renewals Card
            item(key = "money_pulse") {
                val sym = state.preferences.currencySymbol
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MONEY & RENEWALS",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.financeAccent
                            )
                            Text(
                                text = "${Formatters.formatCurrency(state.monthSpent, sym)} this month",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(onClick = { onNavigateToFinance(0) }) {
                            Text("Finance")
                        }
                    }

                    Text(
                        text = "Today: ${Formatters.formatCurrency(state.todaySpent, sym)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.topBudgetStatus?.let { budget ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${budget.category.icon} ${budget.category.name} budget",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${Formatters.formatCurrency(budget.spent, sym)} / ${Formatters.formatCurrency(budget.budget, sym)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (budget.isOverBudget) statusColors.danger else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (budget.percentageUsed / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                budget.isOverBudget -> statusColors.danger
                                budget.percentageUsed >= 80f -> statusColors.warning
                                else -> statusColors.financeAccent
                            },
                            strokeCap = StrokeCap.Round
                        )
                    }

                    state.nextRenewal?.let { sub ->
                        Spacer(modifier = Modifier.height(10.dp))
                        val label = when {
                            state.nextRenewalDays < 0 -> "${-state.nextRenewalDays}d overdue"
                            state.nextRenewalDays == 0L -> "Due today"
                            state.nextRenewalDays == 1L -> "Tomorrow"
                            else -> SafeDateTime.formatRelativeDate(sub.nextBillingDate)
                        }
                        Text(
                            text = "Next renewal: ${sub.icon} ${sub.name} ($label • ${Formatters.formatCurrency(sub.cost, sub.currency.ifBlank { sym })})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.nextRenewalDays <= 1L) statusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.upcomingMilestone?.let { milestone ->
                item(key = "upcoming_milestone") {
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UPCOMING MILESTONE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${milestone.title} (${milestone.category})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val whenText = when (milestone.daysUntil) {
                                    0L -> "Today!"
                                    1L -> "Tomorrow"
                                    else -> "In ${milestone.daysUntil} days"
                                }
                                Text(
                                    text = whenText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onNavigateToYearCalculator) {
                                Text("Dates")
                            }
                        }
                    }
                }
            }

            // Quick Tools Row
            item(key = "tools_strip") {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UTILITIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onNavigateToTools) {
                            Text("All Tools")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToYearCalculator,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dates & Age")
                        }
                        OutlinedButton(
                            onClick = onNavigateToBMICalculator,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.MonitorWeight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BMI Check")
                        }
                    }
                }
            }
        }
    }
}
