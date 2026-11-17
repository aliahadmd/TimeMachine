package me.aliahad.timemanager.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.ImmersiveTimerActivity
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.Preset
import me.aliahad.timemanager.data.TimeSession
import me.aliahad.timemanager.ui.components.ConfirmDeleteDialog
import me.aliahad.timemanager.ui.components.EmojiAndColorPicker
import me.aliahad.timemanager.ui.components.EmptyStateView
import me.aliahad.timemanager.ui.components.M3DatePickerModal
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.FocusViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FocusTrackerScreen(
    initialCategoryId: Long? = null,
    initialTaskId: Long? = null,
    autoStart: Boolean = false
) {
    val viewModel = appViewModel(key = "focus_${initialCategoryId}_${initialTaskId}") { container ->
        FocusViewModel(container, initialCategoryId, initialTaskId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var didAutoStart by rememberSaveable { mutableStateOf(false) }
    var isCountdownMode by rememberSaveable { mutableStateOf(true) }
    var selectedTargetMinutes by rememberSaveable { mutableIntStateOf(25) }
    var selectedBreakMinutes by rememberSaveable { mutableIntStateOf(5) }
    var selectedIsPomodoro by rememberSaveable { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ActivityCategory?>(null) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<Preset?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<TimeSession?>(null) }
    var sessionPendingDelete by remember { mutableStateOf<TimeSession?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val selectedCategory = remember(state.categories, state.selectedCategoryId) {
        state.categories.firstOrNull { it.id == state.selectedCategoryId } ?: state.categories.firstOrNull()
    }

    LaunchedEffect(autoStart, selectedCategory) {
        if (autoStart && !didAutoStart && selectedCategory != null && !state.timerState.isRunning) {
            didAutoStart = true
            viewModel.startFocusTimer(
                category = selectedCategory,
                targetDurationSeconds = if (isCountdownMode) selectedTargetMinutes * 60 else 0,
                isPomodoro = isCountdownMode && selectedIsPomodoro,
                breakDurationSeconds = selectedBreakMinutes * 60
            )
        }
    }

    val saveFeedback = state.timerState.lastSavedResult ?: state.manualSaveFeedback
    LaunchedEffect(saveFeedback) {
        if (saveFeedback != null) {
            val extra = buildList {
                saveFeedback.markedTaskTitle?.let { add("Task \"$it\" completed") }
                if (saveFeedback.autoCompletedHabits.isNotEmpty()) {
                    add("Habit unlocked: ${saveFeedback.autoCompletedHabits.joinToString(", ")}")
                }
            }.joinToString(" • ")
            snackbarHostState.showSnackbar(
                if (extra.isNotBlank()) "Session saved • $extra" else "Focus session saved"
            )
            viewModel.consumeSaveFeedback()
        }
    }

    val todayIso = LocalDate.now().format(SafeDateTime.ISO_DATE)
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TimeMachineTopBar(
                title = "Focus",
                subtitle = "${Formatters.formatMinutes(state.todayFocusMinutes)} today • 🔥 ${state.streakInfo.currentStreak}d streak",
                actions = {
                    if (state.timerState.isRunning) {
                        IconButton(onClick = {
                            context.startActivity(Intent(context, ImmersiveTimerActivity::class.java))
                        }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Desk Mode")
                        }
                    }
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Log", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Timer Hero Card
            item(key = "focus_hero") {
                val running = state.timerState
                val accent = if (running.isRunning) Color(running.categoryColor.toInt())
                else (selectedCategory?.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary)

                SectionCard(borderColor = accent.copy(alpha = 0.45f)) {
                    if (!running.isRunning) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = isCountdownMode,
                                onClick = { isCountdownMode = true },
                                shape = SegmentedButtonDefaults.itemShape(0, 2)
                            ) { Text("Countdown / Pomodoro") }
                            SegmentedButton(
                                selected = !isCountdownMode,
                                onClick = { isCountdownMode = false },
                                shape = SegmentedButtonDefaults.itemShape(1, 2)
                            ) { Text("Stopwatch") }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Categories row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category", style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = {
                                editingCategory = null
                                showCategoryDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Category")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.categories.forEach { cat ->
                                FilterChip(
                                    selected = cat.id == selectedCategory?.id,
                                    onClick = { viewModel.selectCategory(cat.id) },
                                    label = { Text("${cat.icon} ${cat.name}") }
                                )
                            }
                        }

                        if (isCountdownMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Presets", style = MaterialTheme.typography.labelMedium)
                                TextButton(onClick = {
                                    editingPreset = null
                                    showPresetDialog = true
                                }) {
                                    Text("+ Preset")
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.presets.forEach { preset ->
                                    val mins = preset.totalFocusMinutes.coerceAtLeast(1)
                                    val selected = selectedTargetMinutes == mins && selectedIsPomodoro == preset.isPomodoro
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            selectedTargetMinutes = mins
                                            selectedIsPomodoro = preset.isPomodoro
                                            selectedBreakMinutes = preset.breakMinutes
                                        },
                                        label = {
                                            val badge = if (preset.isPomodoro) "🍅" else "⏱️"
                                            Text(
                                                "$badge ${preset.name} (${mins}m)" +
                                                    if (preset.isPomodoro) " +${preset.breakMinutes}m" else ""
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (state.linkedTaskId != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.clearLinkedTask() },
                                label = { Text("Linked to Planner block • Tap to unlink") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                selectedCategory?.let { cat ->
                                    viewModel.startFocusTimer(
                                        category = cat,
                                        targetDurationSeconds = if (isCountdownMode) selectedTargetMinutes * 60 else 0,
                                        isPomodoro = isCountdownMode && selectedIsPomodoro,
                                        breakDurationSeconds = selectedBreakMinutes * 60
                                    )
                                }
                            },
                            enabled = selectedCategory != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCountdownMode) "Start ${selectedTargetMinutes}m ${selectedCategory?.name ?: "Focus"}"
                                else "Start ${selectedCategory?.name ?: "Focus"} Stopwatch",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Active Live Timer Ring
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when {
                                    running.isBreakPhase -> "☕ BREAK TIME"
                                    running.isPaused -> "${running.categoryIcon} ${running.categoryName} • PAUSED"
                                    else -> "${running.categoryIcon} ${running.categoryName}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier.size(190.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = trackColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    val sweep = if (running.isCountdown) running.progressFraction * 360f else 360f
                                    drawArc(
                                        color = accent,
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = Formatters.formatTimerClock(running.displaySeconds),
                                        style = MaterialTheme.typography.displayMedium.merge(TabularNumberStyle),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (running.isCountdown) "Remaining" else "Elapsed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (running.isPaused) viewModel.resumeTimer() else viewModel.pauseTimer()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (running.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (running.isPaused) "Resume" else "Pause")
                                }
                                if (running.isCountdown && !running.isBreakPhase) {
                                    OutlinedButton(onClick = { viewModel.addFiveMinutes() }) {
                                        Text("+5m")
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (running.isBreakPhase) viewModel.skipBreak()
                                        else viewModel.finishAndSaveTimer()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (running.isBreakPhase) "Skip" else "Finish")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = {
                                    if (running.elapsedSeconds >= 60 && !running.isBreakPhase) {
                                        showDiscardConfirm = true
                                    } else {
                                        viewModel.discardTimer()
                                    }
                                }) {
                                    Text("Discard", color = MaterialTheme.colorScheme.error)
                                }
                                TextButton(onClick = {
                                    context.startActivity(Intent(context, ImmersiveTimerActivity::class.java))
                                }) {
                                    Text("Desk Landscape Mode")
                                }
                            }
                        }
                    }
                }
            }

            // Daily Goal Progress Bar
            item(key = "daily_goal_strip") {
                val goal = state.dailyGoalMinutes.coerceAtLeast(1)
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Focus Goal", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${Formatters.formatMinutes(state.todayFocusMinutes)} / ${Formatters.formatMinutes(goal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (state.todayFocusMinutes.toFloat() / goal).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Sub-tabs
            item(key = "focus_tabs") {
                val tabs = listOf("Today", "History", "Stats", "Calendar")
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    val todaySessions = state.allSessions.filter { it.date == todayIso }
                    if (todaySessions.isEmpty()) {
                        item(key = "empty_today") {
                            EmptyStateView(
                                emoji = "⏱️",
                                title = "No Focus Sessions Today",
                                subtitle = "Start a timer or tap Log to record offline deep work."
                            )
                        }
                    } else {
                        items(todaySessions, key = { it.id }) { session ->
                            SessionCard(
                                session = session,
                                category = categoryMap[session.categoryId],
                                onEdit = { editingSession = session },
                                onDelete = { sessionPendingDelete = session }
                            )
                        }
                    }
                }

                1 -> {
                    if (state.allSessions.isEmpty()) {
                        item(key = "empty_history") {
                            EmptyStateView(
                                emoji = "📚",
                                title = "No Session History",
                                subtitle = "Completed sessions will appear here for editing or review."
                            )
                        }
                    } else {
                        items(state.allSessions.take(60), key = { it.id }) { session ->
                            SessionCard(
                                session = session,
                                category = categoryMap[session.categoryId],
                                showDate = true,
                                onEdit = { editingSession = session },
                                onDelete = { sessionPendingDelete = session }
                            )
                        }
                    }
                }

                2 -> {
                    item(key = "stats_overview") {
                        val totalMinutes = state.allSessions.sumOf { it.durationMinutes }
                        SectionCard {
                            Text("All-Time Overview", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatPair("Total", Formatters.formatMinutes(totalMinutes))
                                StatPair("Sessions", state.allSessions.size.toString())
                                StatPair("Best Streak", "${state.streakInfo.longestStreak}d")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val byCat = state.allSessions.groupBy { it.categoryId }
                            state.categories.forEach { cat ->
                                val catMinutes = byCat[cat.id]?.sumOf { it.durationMinutes } ?: 0
                                if (catMinutes > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${cat.icon} ${cat.name}", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            Formatters.formatMinutes(catMinutes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    item(key = "calendar_view") {
                        val month = state.currentCalendarMonth
                        val minutesByDate = remember(state.allSessions) {
                            state.allSessions.groupBy { it.date }.mapValues { (_, list) ->
                                list.sumOf { it.durationMinutes }
                            }
                        }
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.stepCalendarMonth(-1) }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                                }
                                Text(
                                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { viewModel.stepCalendarMonth(1) }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            MonthCalendarGrid(
                                month = month,
                                goalMinutes = state.dailyGoalMinutes,
                                minutesByDate = minutesByDate
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryEditorDialog(
            existing = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = { cat, isEdit ->
                viewModel.saveCategory(cat, isEdit)
                showCategoryDialog = false
            }
        )
    }

    if (showPresetDialog) {
        PresetEditorDialog(
            existing = editingPreset,
            onDismiss = { showPresetDialog = false },
            onSave = { name, mins, breakMins, isPomo ->
                viewModel.savePreset(name, mins, breakMins, isPomo, editingPreset)
                showPresetDialog = false
            }
        )
    }

    if (showManualDialog && selectedCategory != null) {
        ManualSessionDialog(
            categories = state.categories,
            defaultCategoryId = selectedCategory.id,
            onDismiss = { showManualDialog = false },
            onSave = { catId, mins, notes, dateIso ->
                viewModel.logManualSession(catId, mins, notes, dateIso)
                showManualDialog = false
            }
        )
    }

    editingSession?.let { sess ->
        EditSessionDialog(
            session = sess,
            onDismiss = { editingSession = null },
            onSave = { updated ->
                viewModel.updateSession(updated)
                editingSession = null
            }
        )
    }

    sessionPendingDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = "Delete Focus Session?",
            message = "Remove ${Formatters.formatMinutes(pending.durationMinutes)} session on ${pending.date}?",
            onDismiss = { sessionPendingDelete = null },
            onConfirm = {
                viewModel.deleteSession(pending.id)
                sessionPendingDelete = null
            }
        )
    }

    if (showDiscardConfirm) {
        ConfirmDeleteDialog(
            title = "Discard Active Timer?",
            message = "You have elapsed time on this timer. Discard without saving?",
            confirmText = "Discard",
            onDismiss = { showDiscardConfirm = false },
            onConfirm = {
                viewModel.discardTimer()
                showDiscardConfirm = false
            }
        )
    }
}

@Composable
private fun StatPair(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SessionCard(
    session: TimeSession,
    category: ActivityCategory?,
    showDate: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category?.icon ?: "⏱️", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category?.name ?: "Focus",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = buildString {
                        append(Formatters.formatMinutes(session.durationMinutes))
                        if (showDate) append(" • ${SafeDateTime.formatRelativeDate(session.date)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.notes.isNotBlank()) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    month: java.time.YearMonth,
    goalMinutes: Int,
    minutesByDate: Map<String, Int>
) {
    val firstDay = month.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value - 1) // Mon=0 .. Sun=6
    val totalDays = month.lengthOfMonth()
    val cells = startOffset + totalDays
    val rows = (cells + 6) / 7
    val statusColors = TimeMachineTheme.statusColors

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { col ->
                    val cellIdx = row * 7 + col
                    val dayNum = cellIdx - startOffset + 1
                    if (dayNum in 1..totalDays) {
                        val date = month.atDay(dayNum)
                        val iso = date.format(SafeDateTime.ISO_DATE)
                        val mins = minutesByDate[iso] ?: 0
                        val ratio = if (goalMinutes > 0) (mins.toFloat() / goalMinutes).coerceIn(0f, 1f) else 0f
                        val bg = when {
                            mins == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ratio >= 1f -> statusColors.success.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = (0.25f + ratio * 0.65f))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ratio >= 0.7f) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    existing: ActivityCategory?,
    onDismiss: () -> Unit,
    onSave: (ActivityCategory, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "🎯") }
    var colorLong by remember { mutableStateOf(existing?.color ?: Color(0xFF0A66C2).toStorageLong()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                EmojiAndColorPicker(
                    emojis = listOf("🎯", "💻", "📚", "🎨", "🏋️", "🧘", "✍️", "🎧"),
                    selectedEmoji = icon,
                    onEmojiSelected = { icon = it },
                    selectedColorLong = colorLong,
                    onColorSelected = { colorLong = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val cat = ActivityCategory(
                        id = existing?.id ?: 0L,
                        name = name.trim(),
                        icon = icon,
                        color = colorLong,
                        isActive = true
                    )
                    onSave(cat, existing != null)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PresetEditorDialog(
    existing: Preset?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "Deep Focus") }
    var minsText by remember { mutableStateOf((existing?.totalFocusMinutes ?: 25).toString()) }
    var breakText by remember { mutableStateOf((existing?.breakMinutes ?: 5).toString()) }
    var isPomodoro by remember { mutableStateOf(existing?.isPomodoro ?: true) }

    val mins = minsText.toIntOrNull()
    val brk = breakText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Focus Preset" else "Edit Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minsText,
                    onValueChange = { minsText = it.filter(Char::isDigit) },
                    label = { Text("Focus Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pomodoro Auto-Break")
                    Switch(checked = isPomodoro, onCheckedChange = { isPomodoro = it })
                }
                if (isPomodoro) {
                    OutlinedTextField(
                        value = breakText,
                        onValueChange = { breakText = it.filter(Char::isDigit) },
                        label = { Text("Break Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && mins != null && mins > 0,
                onClick = { onSave(name.trim(), mins ?: 25, (brk ?: 5).coerceAtLeast(1), isPomodoro) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ManualSessionDialog(
    categories: List<ActivityCategory>,
    defaultCategoryId: Long,
    onDismiss: () -> Unit,
    onSave: (Long, Int, String, String) -> Unit
) {
    var catId by remember { mutableStateOf(defaultCategoryId) }
    var minutesText by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(LocalDate.now().format(SafeDateTime.ISO_DATE)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val mins = minutesText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Focus Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = cat.id == catId,
                            onClick = { catId = cat.id },
                            label = { Text("${cat.icon} ${cat.name}") }
                        )
                    }
                }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit) },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: $dateIso")
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = mins != null && mins > 0,
                onClick = { onSave(catId, mins ?: 1, notes, dateIso) }
            ) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        M3DatePickerModal(
            initialDate = SafeDateTime.parseLocalDateOrNull(dateIso) ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { dateIso = it.format(SafeDateTime.ISO_DATE) }
        )
    }
}

@Composable
private fun EditSessionDialog(
    session: TimeSession,
    onDismiss: () -> Unit,
    onSave: (TimeSession) -> Unit
) {
    var minutesText by remember { mutableStateOf(session.durationMinutes.toString()) }
    var notes by remember { mutableStateOf(session.notes) }
    val mins = minutesText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit) },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = mins != null && mins > 0,
                onClick = {
                    val safeMins = mins ?: session.durationMinutes
                    onSave(
                        session.copy(
                            durationMinutes = safeMins,
                            endTime = session.startTime + safeMins * 60_000L,
                            notes = notes.trim()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
