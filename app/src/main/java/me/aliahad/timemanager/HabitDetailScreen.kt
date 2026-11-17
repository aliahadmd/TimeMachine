package me.aliahad.timemanager

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.HabitCompletion
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.HabitDetailViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun HabitDetailScreen(
    habitId: Long,
    onBack: () -> Unit
) {
    val viewModel = appViewModel(key = "habit_detail_$habitId") { container ->
        HabitDetailViewModel(container, habitId)
    }
    val habitStats by viewModel.habitWithStats.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val focusCategories by viewModel.activeFocusCategories.collectAsStateWithLifecycle()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var editingDateIso by remember { mutableStateOf<String?>(null) }
    var showEditHabit by remember { mutableStateOf(false) }
    var showRemoveChoice by remember { mutableStateOf(false) }

    val stats = habitStats
    val statusColors = TimeMachineTheme.statusColors

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = stats?.habit?.name ?: "Habit Details",
                subtitle = stats?.let { "${it.habit.type.name} • Goal ${it.habit.goalPeriodDays} days" },
                onBack = onBack,
                actions = {
                    if (stats != null) {
                        IconButton(onClick = { showEditHabit = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Habit")
                        }
                        IconButton(onClick = { showRemoveChoice = true }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Archive or Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading habit details...")
            }
            return@Scaffold
        }

        val habit = stats.habit
        val accent = habit.color.toComposeColor()
        val completionsByDate = remember(completions) { completions.associateBy { it.date } }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "detail_stats") {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accent.copy(alpha = 0.16f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconByName(habit.iconName),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.titleLarge)
                            if (habit.description.isNotBlank()) {
                                Text(
                                    habit.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailMetric("Current Streak", "${stats.currentStreak}d")
                        DetailMetric("Best Streak", "${stats.longestStreak}d")
                        DetailMetric("Scheduled Rate", "${(stats.completionRate * 100).toInt()}%")
                        DetailMetric("Success Rate", "${(stats.successRate * 100).toInt()}%")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Goal Period Progress", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${stats.totalAchieved} / ${habit.goalPeriodDays} days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { stats.goalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accent,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            item(key = "interactive_calendar") {
                val today = LocalDate.now()
                val firstDay = currentMonth.atDay(1)
                val startOffset = firstDay.dayOfWeek.value - 1
                val totalDays = currentMonth.lengthOfMonth()
                val rows = (startOffset + totalDays + 6) / 7

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev month")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Tap any day to log or edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(rows) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(7) { col ->
                                    val dayNum = row * 7 + col - startOffset + 1
                                    if (dayNum in 1..totalDays) {
                                        val date = currentMonth.atDay(dayNum)
                                        val iso = date.format(SafeDateTime.ISO_DATE)
                                        val entry = completionsByDate[iso]
                                        val isFuture = date.isAfter(today)
                                        val bg = when (entry?.completionType) {
                                            CompletionType.ACHIEVED -> statusColors.success
                                            CompletionType.GAVE_UP -> statusColors.danger
                                            null -> if (date == today) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        }
                                        val fg = if (entry != null) Color.White else MaterialTheme.colorScheme.onSurface
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = bg,
                                            border = if (date == today) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(enabled = !isFuture) { editingDateIso = iso }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isFuture) fg.copy(alpha = 0.35f) else fg
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).height(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "heatmap_90") {
                SectionCard {
                    Text("90-Day Consistency Map", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    AlignedNinetyDayHeatmap(completionsByDate = completionsByDate, accent = accent)
                }
            }

            val notesEntries = completions.filter { it.notes.isNotBlank() }
            if (notesEntries.isNotEmpty()) {
                item(key = "notes_header") {
                    Text("Journal Notes", style = MaterialTheme.typography.titleMedium)
                }
                items(notesEntries.take(20), key = { "${it.habitId}_${it.date}" }) { entry ->
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                SafeDateTime.formatRelativeDate(entry.date),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (entry.completionType == CompletionType.ACHIEVED) "Achieved" else "Gave Up",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.completionType == CompletionType.ACHIEVED) statusColors.success else statusColors.danger
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(entry.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        editingDateIso?.let { dateIso ->
            val existing = completionsByDate[dateIso]
            HabitCheckInDialog(
                habitName = habit.name,
                dateLabel = SafeDateTime.formatRelativeDate(dateIso) + " ($dateIso)",
                initialType = existing?.completionType ?: CompletionType.ACHIEVED,
                initialNotes = existing?.notes ?: "",
                canClearExisting = existing != null,
                onDismiss = { editingDateIso = null },
                onClear = { viewModel.clearCompletionForDate(dateIso) },
                onSubmit = { type, notes ->
                    viewModel.saveCompletionForDate(dateIso, type, notes)
                    editingDateIso = null
                }
            )
        }

        if (showEditHabit) {
            HabitEditorDialog(
                existingHabit = habit,
                focusCategories = focusCategories,
                onDismiss = { showEditHabit = false },
                onSave = { updated, _ ->
                    viewModel.updateHabit(updated)
                    showEditHabit = false
                }
            )
        }

        if (showRemoveChoice) {
            AlertDialog(
                onDismissRequest = { showRemoveChoice = false },
                title = { Text("Remove \"${habit.name}\"?") },
                text = {
                    Text("Archiving keeps past streak history while stopping future reminders. Permanent deletion erases all check-ins forever.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.archiveHabit()
                            showRemoveChoice = false
                            onBack()
                        }
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Archive")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showRemoveChoice = false }) { Text("Cancel") }
                        OutlinedButton(
                            onClick = {
                                viewModel.deletePermanently(habit)
                                showRemoveChoice = false
                                onBack()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete Forever")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlignedNinetyDayHeatmap(
    completionsByDate: Map<String, HabitCompletion>,
    accent: Color
) {
    val today = LocalDate.now()
    val earliest = today.minusDays(89)
    val startMonday = earliest.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = mutableListOf<List<LocalDate>>()
    var cursor = startMonday
    while (!cursor.isAfter(today)) {
        val week = (0L..6L).map { cursor.plusDays(it) }
        weeks += week
        cursor = cursor.plusWeeks(1)
    }
    val statusColors = TimeMachineTheme.statusColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val iso = day.format(SafeDateTime.ISO_DATE)
                    val inRange = !day.isBefore(earliest) && !day.isAfter(today)
                    val entry = if (inRange) completionsByDate[iso] else null
                    val boxColor = when {
                        !inRange -> Color.Transparent
                        entry?.completionType == CompletionType.ACHIEVED -> accent
                        entry?.completionType == CompletionType.GAVE_UP -> statusColors.danger
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = boxColor,
                        modifier = Modifier.size(14.dp)
                    ) {}
                }
            }
        }
    }
}
