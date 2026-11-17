package me.aliahad.timemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.DailyPlannerUtils
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.ui.components.ConfirmDeleteDialog
import me.aliahad.timemanager.ui.components.EmojiAndColorPicker
import me.aliahad.timemanager.ui.components.EmptyStateView
import me.aliahad.timemanager.ui.components.M3DatePickerModal
import me.aliahad.timemanager.ui.components.M3TimePickerModal
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.PlannerFilterMode
import me.aliahad.timemanager.ui.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DailyPlannerScreen(
    initialDateIso: String? = null,
    onStartFocusFromTask: (categoryId: Long?, taskId: Long) -> Unit
) {
    val viewModel = appViewModel(key = initialDateIso ?: "planner_default") { container ->
        PlannerViewModel(container, initialDateIso)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DailyTask?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<DailyTask?>(null) }

    val statusColors = TimeMachineTheme.statusColors

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = "Planner",
                subtitle = state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Time Block")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "date_strip") {
                val baseDate = state.selectedDate
                val weekDates = remember(baseDate) {
                    (-3L..3L).map { baseDate.plusDays(it) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { viewModel.stepDate(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                        }
                        TextButton(onClick = { viewModel.selectDate(LocalDate.now()) }) {
                            Text(
                                text = SafeDateTime.formatRelativeDate(state.selectedDateIso),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        IconButton(onClick = { viewModel.stepDate(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekDates.forEach { d ->
                            val isSelected = d == state.selectedDate
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectDate(d) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = d.dayOfWeek.name.take(3),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = d.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "summary_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${state.completedCount}/${state.tasks.size} completed",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Scheduled: ${Formatters.formatMinutes(state.totalScheduledMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlannerFilterMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.filterMode == mode,
                                onClick = { viewModel.setFilter(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }

            if (state.tasks.isEmpty()) {
                item(key = "empty_planner") {
                    EmptyStateView(
                        emoji = "🗓️",
                        title = "No Time Blocks Here",
                        subtitle = "Plan tasks, deep-work focus blocks, events, or recurring routines with exact reminders.",
                        actionLabel = "Add Time Block",
                        onAction = {
                            editingTask = null
                            showTaskDialog = true
                        }
                    )
                }
            } else {
                items(state.tasks, key = { it.id }) { task ->
                    val today = LocalDate.now()
                    val isNow = !task.isCompleted && DailyPlannerUtils.isTaskNow(task, today)
                    val isSoon = !task.isCompleted && !isNow && DailyPlannerUtils.isTaskUpcoming(task, today)
                    val hasOverlap = state.overlappingTaskIds.contains(task.id)
                    val accent = task.color.toComposeColor()
                    val durationMin = DailyPlannerUtils.calculateDuration(task.startTime, task.endTime)

                    SectionCard(
                        borderColor = if (isNow) statusColors.success else statusColors.cardBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleTaskCompletion(task) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle completion",
                                    tint = if (task.isCompleted) statusColors.success else accent
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(task.icon, fontSize = 18.sp)
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${task.startTime} – ${task.endTime} (${Formatters.formatMinutes(durationMin)}) • ${task.taskType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (task.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isNow) {
                                        StatusBadge("NOW", statusColors.successContainer, statusColors.success)
                                    } else if (isSoon) {
                                        StatusBadge("SOON", statusColors.warningContainer, statusColors.warning)
                                    }
                                    if (hasOverlap) {
                                        StatusBadge("OVERLAP", statusColors.dangerContainer, statusColors.danger, icon = Icons.Default.Warning)
                                    }
                                    if (task.priority == "High") {
                                        StatusBadge("HIGH", statusColors.dangerContainer, statusColors.danger)
                                    }
                                    if (task.reminderMinutes > 0) {
                                        StatusBadge("${task.reminderMinutes}m alert", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, icon = Icons.Default.NotificationsActive)
                                    }
                                    if (task.isRecurring) {
                                        StatusBadge(
                                            task.recurringDays.ifBlank { "Recurring" },
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                            icon = Icons.Default.Repeat
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!task.isCompleted && task.taskType.equals("FOCUS", ignoreCase = true)) {
                                        OutlinedButton(
                                            onClick = { onStartFocusFromTask(task.linkedCategoryId, task.id) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Start Focus", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingTask = task
                                            showTaskDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit task", modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { taskPendingDelete = task }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete task",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
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

    if (showDatePicker) {
        M3DatePickerModal(
            initialDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { viewModel.selectDate(it) }
        )
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            initialDateIso = state.selectedDateIso,
            existingTask = editingTask,
            focusCategories = state.activeFocusCategories,
            onDismiss = { showTaskDialog = false },
            onSave = { task, isEdit ->
                viewModel.saveTask(task, isEdit)
                showTaskDialog = false
            }
        )
    }

    taskPendingDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = "Delete Time Block?",
            message = "Delete \"${pending.title}\" (${pending.startTime} – ${pending.endTime})?",
            onDismiss = { taskPendingDelete = null },
            onConfirm = {
                viewModel.deleteTask(pending)
                taskPendingDelete = null
            }
        )
    }
}

@Composable
private fun StatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TaskEditorDialog(
    initialDateIso: String,
    existingTask: DailyTask?,
    focusCategories: List<ActivityCategory>,
    onDismiss: () -> Unit,
    onSave: (DailyTask, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var dateIso by remember { mutableStateOf(existingTask?.date ?: initialDateIso) }

    val initStart = SafeDateTime.parseLocalTimeOrNull(existingTask?.startTime) ?: java.time.LocalTime.of(9, 0)
    val initEnd = SafeDateTime.parseLocalTimeOrNull(existingTask?.endTime) ?: initStart.plusMinutes(60)
    var startHour by remember { mutableIntStateOf(initStart.hour) }
    var startMinute by remember { mutableIntStateOf(initStart.minute) }
    var endHour by remember { mutableIntStateOf(initEnd.hour) }
    var endMinute by remember { mutableIntStateOf(initEnd.minute) }

    var taskType by remember { mutableStateOf(existingTask?.taskType ?: "TASK") }
    var priority by remember { mutableStateOf(existingTask?.priority ?: "Medium") }
    var reminderMinutes by remember { mutableIntStateOf(existingTask?.reminderMinutes ?: 10) }
    var isRecurring by remember { mutableStateOf(existingTask?.isRecurring ?: false) }
    var recurringDaysSet by remember {
        mutableStateOf(
            existingTask?.recurringDays?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
                ?: emptySet()
        )
    }
    var linkedCategoryId by remember { mutableStateOf(existingTask?.linkedCategoryId) }
    var icon by remember { mutableStateOf(existingTask?.icon ?: "📝") }
    var colorLong by remember { mutableStateOf(existingTask?.color ?: Color(0xFF0A66C2).toStorageLong()) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val taskTypes = listOf("TASK", "FOCUS", "EVENT", "ROUTINE", "BREAK")
    val priorities = listOf("Low", "Medium", "High")
    val reminderOptions = listOf(0 to "Off", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "1h")
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingTask == null) "New Time Block" else "Edit Time Block") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    taskTypes.forEach { t ->
                        FilterChip(
                            selected = taskType == t,
                            onClick = { taskType = t },
                            label = { Text(t) }
                        )
                    }
                }

                if (taskType == "FOCUS" && focusCategories.isNotEmpty()) {
                    Text("Focus Category", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        focusCategories.forEach { cat ->
                            FilterChip(
                                selected = linkedCategoryId == cat.id,
                                onClick = { linkedCategoryId = if (linkedCategoryId == cat.id) null else cat.id },
                                label = { Text("${cat.icon} ${cat.name}") }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1.2f)) {
                        Text(dateIso, maxLines = 1)
                    }
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(String.format(java.util.Locale.US, "%02d:%02d", startHour, startMinute))
                    }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(String.format(java.util.Locale.US, "%02d:%02d", endHour, endMinute))
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                Text("Reminder Before Start", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reminderOptions.forEach { (mins, label) ->
                        FilterChip(
                            selected = reminderMinutes == mins,
                            onClick = { reminderMinutes = mins },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring Weekly", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }

                if (isRecurring) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekdayLabels.forEach { day ->
                            val selected = recurringDaysSet.contains(day)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    recurringDaysSet = if (selected) recurringDaysSet - day else recurringDaysSet + day
                                },
                                label = { Text(day) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon & Color", style = MaterialTheme.typography.labelMedium)
                EmojiAndColorPicker(
                    emojis = listOf("📝", "🎯", "📅", "🔄", "☕", "💼", "🏠", "📚"),
                    selectedEmoji = icon,
                    onEmojiSelected = { icon = it },
                    selectedColorLong = colorLong,
                    onColorSelected = { colorLong = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val startFmt = String.format(java.util.Locale.US, "%02d:%02d", startHour, startMinute)
                    val endFmt = String.format(java.util.Locale.US, "%02d:%02d", endHour, endMinute)
                    val model = DailyTask(
                        id = existingTask?.id ?: 0L,
                        title = title.trim(),
                        description = description.trim(),
                        date = dateIso,
                        startTime = startFmt,
                        endTime = endFmt,
                        taskType = taskType,
                        category = existingTask?.category ?: "General",
                        priority = priority,
                        icon = icon,
                        color = colorLong,
                        isCompleted = existingTask?.isCompleted ?: false,
                        isRecurring = isRecurring,
                        recurringDays = if (isRecurring) recurringDaysSet.joinToString(",") else "",
                        reminderMinutes = reminderMinutes,
                        notes = notes.trim(),
                        linkedCategoryId = if (taskType == "FOCUS") linkedCategoryId else null,
                        linkedSessionId = existingTask?.linkedSessionId,
                        createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(model, existingTask != null)
                }
            ) {
                Text("Save")
            }
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
    if (showStartTimePicker) {
        M3TimePickerModal(
            title = "Start Time",
            initialHour = startHour,
            initialMinute = startMinute,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { h, m -> startHour = h; startMinute = m }
        )
    }
    if (showEndTimePicker) {
        M3TimePickerModal(
            title = "End Time",
            initialHour = endHour,
            initialMinute = endMinute,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m -> endHour = h; endMinute = m }
        )
    }
}
