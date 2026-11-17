package me.aliahad.timemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.HabitCheckInDialog
import me.aliahad.timemanager.HabitEditorDialog
import me.aliahad.timemanager.ScreenTimePermissionHelper
import me.aliahad.timemanager.ScreenTimeScheduler
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitLinkType
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.HabitWithStats
import me.aliahad.timemanager.getIconByName
import me.aliahad.timemanager.permissions.DeviceReliabilityManager
import me.aliahad.timemanager.ui.components.EmptyStateView
import me.aliahad.timemanager.ui.components.M3DatePickerModal
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.WellbeingViewModel
import java.time.LocalDate

@Composable
fun WellbeingScreen(
    initialTab: Int = 0,
    onOpenHabitDetail: (Long) -> Unit
) {
    val viewModel = appViewModel { container -> WellbeingViewModel(container) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshUsagePermissionAndSync()
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var checkInHabit by remember { mutableStateOf<HabitWithStats?>(null) }
    var showScreenDatePicker by remember { mutableStateOf(false) }

    val statusColors = TimeMachineTheme.statusColors

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = "Wellbeing",
                subtitle = if (selectedTab == 0) {
                    "${state.completedTodayCount}/${state.scheduledTodayCount} scheduled today"
                } else {
                    val mins = (state.screenSummary?.totalScreenTimeSeconds ?: 0) / 60
                    "${Formatters.formatMinutes(mins)} • ${SafeDateTime.formatRelativeDate(state.selectedScreenDate.format(SafeDateTime.ISO_DATE))}"
                },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { showScreenDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                        IconButton(onClick = { ScreenTimeScheduler.triggerImmediateSync(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Screen Time")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingHabit = null
                        showHabitDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Habit")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Habits") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Screen Time") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    if (state.habitsWithStats.isEmpty()) {
                        item(key = "empty_habits") {
                            EmptyStateView(
                                emoji = "🌱",
                                title = "Build Lasting Habits",
                                subtitle = "Create Build or Quit habits with custom weekday schedules, reminders, and Focus/Screen Time links.",
                                actionLabel = "Create First Habit",
                                onAction = {
                                    editingHabit = null
                                    showHabitDialog = true
                                }
                            )
                        }
                    } else {
                        items(state.habitsWithStats, key = { it.habit.id }) { item ->
                            val habit = item.habit
                            val accent = habit.color.toComposeColor()
                            val today = LocalDate.now()
                            val scheduledToday = HabitRepository.isScheduledOnDate(habit, today)
                            val todayStatus = item.todayCompletion

                            SectionCard(
                                borderColor = when (todayStatus?.completionType) {
                                    CompletionType.ACHIEVED -> statusColors.success.copy(alpha = 0.55f)
                                    CompletionType.GAVE_UP -> statusColors.danger.copy(alpha = 0.45f)
                                    null -> statusColors.cardBorder
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenHabitDetail(habit.id) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = accent.copy(alpha = 0.16f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = getIconByName(habit.iconName),
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = habit.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val subtitleText = buildString {
                                                append("🔥 ${item.currentStreak}d")
                                                append(" • Goal ${item.totalAchieved}/${habit.goalPeriodDays}d")
                                                if (!scheduledToday) append(" • Rest day")
                                            }
                                            Text(
                                                text = subtitleText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.toggleHabitCompletionToday(item) }) {
                                            Icon(
                                                imageVector = if (todayStatus?.completionType == CompletionType.ACHIEVED) {
                                                    Icons.Default.CheckCircle
                                                } else {
                                                    Icons.Default.RadioButtonUnchecked
                                                },
                                                contentDescription = "Quick check-in",
                                                tint = if (todayStatus?.completionType == CompletionType.ACHIEVED) {
                                                    statusColors.success
                                                } else accent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { item.goalProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = accent,
                                        strokeCap = StrokeCap.Round
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val linkTag = when (habit.linkType) {
                                            HabitLinkType.FOCUS_MINUTES -> "⏱ Auto: ${habit.linkedTargetValue}m Focus"
                                            HabitLinkType.SCREEN_TIME_UNDER_LIMIT -> "📵 Auto: ≤${habit.linkedTargetValue}m Screen"
                                            HabitLinkType.MANUAL -> if (habit.reminderTimeHour != null) {
                                                String.format(java.util.Locale.US, "🔔 %02d:%02d", habit.reminderTimeHour, habit.reminderTimeMinute)
                                            } else "Tap for calendar & stats"
                                        }
                                        Text(
                                            text = linkTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row {
                                            IconButton(
                                                onClick = { checkInHabit = item },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.EditNote,
                                                    contentDescription = "Note / Gave Up",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    editingHabit = habit
                                                    showHabitDialog = true
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Habit",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Screen Time Tab
                    if (!state.hasUsageAccess) {
                        item(key = "usage_access_card") {
                            SectionCard(
                                containerColor = statusColors.warningContainer.copy(alpha = 0.4f),
                                borderColor = statusColors.warning.copy(alpha = 0.5f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = statusColors.warning)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Enable Usage Access",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.isRestrictedSideload) {
                                        "Because TimeMachine was installed directly via USB/APK, Android restricts Usage Access initially. Tap 'Open App Info' -> ⋮ menu -> 'Allow restricted settings', then grant Usage Access."
                                    } else {
                                        "Grant Usage Access so TimeMachine can measure daily screen time, pickups, and top apps completely locally."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (state.isRestrictedSideload) {
                                        OutlinedButton(
                                            onClick = { DeviceReliabilityManager.openAppDetails(context) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Open App Info")
                                        }
                                    }
                                    Button(
                                        onClick = { ScreenTimePermissionHelper.openUsageAccessSettings(context) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Usage Access")
                                    }
                                }
                            }
                        }
                    }

                    item(key = "screen_date_nav") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val today = LocalDate.now()
                            listOf(
                                today to "Today",
                                today.minusDays(1) to "Yesterday"
                            ).forEach { (date, label) ->
                                val selected = state.selectedScreenDate == date
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.selectScreenDate(date) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { showScreenDatePicker = true }) {
                                Text("History")
                            }
                        }
                    }

                    item(key = "screen_summary") {
                        val summary = state.screenSummary
                        val totalMinutes = (summary?.totalScreenTimeSeconds ?: 0) / 60
                        val limit = state.dailyScreenLimitMinutes.coerceAtLeast(1)
                        val progress = (totalMinutes.toFloat() / limit).coerceIn(0f, 1f)

                        SectionCard {
                            Text(
                                text = "DAILY SCREEN TIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.wellbeingAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatMinutes(totalMinutes),
                                style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily limit: ${Formatters.formatMinutes(limit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progress >= 1f) statusColors.danger else statusColors.wellbeingAccent,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MiniScreenMetric("Unlocks", (summary?.pickupsCount ?: 0).toString())
                                MiniScreenMetric(
                                    "Avg Session",
                                    Formatters.formatSeconds(summary?.avgSessionDurationSeconds ?: 0)
                                )
                                MiniScreenMetric(
                                    "Longest",
                                    Formatters.formatSeconds(summary?.longestSessionSeconds ?: 0)
                                )
                            }
                        }
                    }

                    if (state.appUsage.isNotEmpty()) {
                        item(key = "app_usage_breakdown") {
                            val totalAppSecs = state.appUsage.sumOf { it.totalSeconds }.coerceAtLeast(1)
                            SectionCard {
                                Text("Top Apps", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    state.appUsage.take(10).forEach { app ->
                                        val frac = (app.totalSeconds.toFloat() / totalAppSecs).coerceIn(0f, 1f)
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = app.appName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = Formatters.formatSeconds(app.totalSeconds),
                                                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { frac },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                strokeCap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "hourly_chart") {
                        SectionCard {
                            Text("24-Hour Rhythm", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            val byHour = remember(state.hourlyBreakdown) {
                                state.hourlyBreakdown.associateBy { it.hour }
                            }
                            val maxHourSec = (state.hourlyBreakdown.maxOfOrNull { it.screenTimeSeconds } ?: 0)
                                .coerceAtLeast(60)
                            val barColor = statusColors.wellbeingAccent
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                            ) {
                                val slotW = size.width / 24f
                                val barW = (slotW * 0.65f).coerceAtLeast(2.dp.toPx())
                                (0..23).forEach { hr ->
                                    val secs = byHour[hr]?.screenTimeSeconds ?: 0
                                    val h = if (secs > 0) ((secs.toFloat() / maxHourSec) * size.height).coerceAtLeast(4.dp.toPx()) else 2.dp.toPx()
                                    drawRoundRect(
                                        color = if (secs > 0) barColor else barColor.copy(alpha = 0.2f),
                                        topLeft = Offset(hr * slotW + (slotW - barW) / 2f, size.height - h),
                                        size = Size(barW, h),
                                        cornerRadius = CornerRadius(3.dp.toPx())
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("06:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("18:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("23:59", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (state.recentSummaries.isNotEmpty()) {
                        item(key = "seven_day_screen") {
                            SectionCard {
                                Text("Recent Days", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.recentSummaries.forEach { day ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(SafeDateTime.formatRelativeDate(day.date), style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${Formatters.formatMinutes(day.totalScreenTimeSeconds / 60)} • ${day.pickupsCount} unlocks",
                                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
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
        }
    }

    if (showHabitDialog) {
        HabitEditorDialog(
            existingHabit = editingHabit,
            focusCategories = state.activeFocusCategories,
            onDismiss = { showHabitDialog = false },
            onSave = { habit, isEdit ->
                viewModel.saveHabit(habit, isEdit)
                showHabitDialog = false
            }
        )
    }

    checkInHabit?.let { hw ->
        val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)
        HabitCheckInDialog(
            habitName = hw.habit.name,
            dateLabel = "Today ($todayIso)",
            initialType = hw.todayCompletion?.completionType ?: CompletionType.ACHIEVED,
            initialNotes = hw.todayCompletion?.notes ?: "",
            canClearExisting = hw.todayCompletion != null,
            onDismiss = { checkInHabit = null },
            onClear = {
                viewModel.toggleHabitCompletionToday(hw)
            },
            onSubmit = { type, notes ->
                viewModel.submitHabitWithNotes(hw.habit.id, todayIso, type, notes)
                checkInHabit = null
            }
        )
    }

    if (showScreenDatePicker) {
        M3DatePickerModal(
            initialDate = state.selectedScreenDate,
            onDismiss = { showScreenDatePicker = false },
            onDateSelected = { viewModel.selectScreenDate(it) }
        )
    }
}

@Composable
private fun MiniScreenMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle), fontWeight = FontWeight.Bold)
    }
}
