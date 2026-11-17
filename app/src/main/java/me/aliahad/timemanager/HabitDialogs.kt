package me.aliahad.timemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitLinkType
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.HabitType
import me.aliahad.timemanager.ui.components.M3TimePickerModal
import me.aliahad.timemanager.ui.components.PresetPaletteColors

@Composable
fun HabitEditorDialog(
    existingHabit: Habit? = null,
    focusCategories: List<ActivityCategory> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Habit, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existingHabit?.name ?: "") }
    var description by remember { mutableStateOf(existingHabit?.description ?: "") }
    var habitType by remember { mutableStateOf(existingHabit?.type ?: HabitType.BUILD) }
    var goalDaysText by remember { mutableStateOf((existingHabit?.goalPeriodDays ?: 30).toString()) }
    var isEveryday by remember { mutableStateOf(existingHabit?.isEveryday ?: true) }
    var scheduledDaysSet by remember {
        mutableStateOf(
            if (existingHabit != null) HabitRepository.parseScheduledDays(existingHabit.scheduledDays)
            else setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }
    var linkType by remember { mutableStateOf(existingHabit?.linkType ?: HabitLinkType.MANUAL) }
    var linkedCategoryId by remember { mutableStateOf(existingHabit?.linkedFocusCategoryId) }
    var linkedTargetText by remember {
        mutableStateOf((existingHabit?.linkedTargetValue?.takeIf { it > 0 } ?: 30).toString())
    }
    var hasReminder by remember { mutableStateOf(existingHabit?.reminderTimeHour != null) }
    var remHour by remember { mutableIntStateOf(existingHabit?.reminderTimeHour ?: 9) }
    var remMinute by remember { mutableIntStateOf(existingHabit?.reminderTimeMinute ?: 0) }
    var iconName by remember { mutableStateOf(existingHabit?.iconName ?: "fitness_center") }
    var colorLong by remember {
        mutableStateOf(existingHabit?.color ?: Color(0xFF0A66C2).toStorageLong())
    }
    var showTimePicker by remember { mutableStateOf(false) }

    val goalDays = goalDaysText.toIntOrNull()?.coerceIn(1, 3650) ?: 30
    val dayChips = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingHabit == null) "New Habit" else "Edit Habit") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Why / cue (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Habit Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = habitType == HabitType.BUILD,
                        onClick = { habitType = HabitType.BUILD },
                        label = { Text("Build") }
                    )
                    FilterChip(
                        selected = habitType == HabitType.QUIT,
                        onClick = { habitType = HabitType.QUIT },
                        label = { Text("Quit") }
                    )
                }

                OutlinedTextField(
                    value = goalDaysText,
                    onValueChange = { goalDaysText = it.filter(Char::isDigit) },
                    label = { Text("Goal Streak / Period (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Every Day", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isEveryday,
                        onCheckedChange = {
                            isEveryday = it
                            if (it) scheduledDaysSet = setOf(1, 2, 3, 4, 5, 6, 7)
                        }
                    )
                }

                if (!isEveryday) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayChips.forEach { (isoDay, label) ->
                            val selected = scheduledDaysSet.contains(isoDay)
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val next = if (selected && scheduledDaysSet.size > 1) {
                                            scheduledDaysSet - isoDay
                                        } else {
                                            scheduledDaysSet + isoDay
                                        }
                                        scheduledDaysSet = next
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Automation Link", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = linkType == HabitLinkType.MANUAL,
                        onClick = { linkType = HabitLinkType.MANUAL },
                        label = { Text("Manual") }
                    )
                    FilterChip(
                        selected = linkType == HabitLinkType.FOCUS_MINUTES,
                        onClick = { linkType = HabitLinkType.FOCUS_MINUTES },
                        label = { Text("Auto: Focus Goal") }
                    )
                    FilterChip(
                        selected = linkType == HabitLinkType.SCREEN_TIME_UNDER_LIMIT,
                        onClick = { linkType = HabitLinkType.SCREEN_TIME_UNDER_LIMIT },
                        label = { Text("Auto: Screen Cap") }
                    )
                }

                if (linkType != HabitLinkType.MANUAL) {
                    OutlinedTextField(
                        value = linkedTargetText,
                        onValueChange = { linkedTargetText = it.filter(Char::isDigit) },
                        label = {
                            Text(
                                if (linkType == HabitLinkType.FOCUS_MINUTES) "Minimum Focus Minutes/Day"
                                else "Maximum Screen Time Minutes/Day"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (linkType == HabitLinkType.FOCUS_MINUTES && focusCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = linkedCategoryId == null,
                                onClick = { linkedCategoryId = null },
                                label = { Text("Any Focus") }
                            )
                            focusCategories.forEach { cat ->
                                FilterChip(
                                    selected = linkedCategoryId == cat.id,
                                    onClick = { linkedCategoryId = cat.id },
                                    label = { Text("${cat.icon} ${cat.name}") }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Reminder")
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it })
                }

                if (hasReminder) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(String.format(java.util.Locale.US, "Reminder Time: %02d:%02d", remHour, remMinute))
                    }
                }

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    habitIcons.forEach { item ->
                        val selected = item.name == iconName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.clickable { iconName = item.name }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.displayName,
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetPaletteColors.forEach { color ->
                        val storageLong = color.toStorageLong()
                        val selected = storageLong == colorLong
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorLong = storageLong },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val daysCsv = if (isEveryday) "1,2,3,4,5,6,7"
                    else scheduledDaysSet.sorted().joinToString(",")
                    val targetVal = linkedTargetText.toIntOrNull()?.coerceAtLeast(1) ?: 0
                    val saved = Habit(
                        id = existingHabit?.id ?: 0L,
                        name = name.trim(),
                        description = description.trim(),
                        color = colorLong,
                        iconName = iconName,
                        type = habitType,
                        goalPeriodDays = goalDays,
                        isEveryday = isEveryday,
                        scheduledDays = daysCsv,
                        reminderTimeHour = if (hasReminder) remHour else null,
                        reminderTimeMinute = if (hasReminder) remMinute else null,
                        linkedFocusCategoryId = if (linkType == HabitLinkType.FOCUS_MINUTES) linkedCategoryId else null,
                        linkType = linkType,
                        linkedTargetValue = if (linkType == HabitLinkType.MANUAL) 0 else targetVal,
                        createdAt = existingHabit?.createdAt ?: System.currentTimeMillis(),
                        isActive = existingHabit?.isActive ?: true
                    )
                    onSave(saved, existingHabit != null)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showTimePicker) {
        M3TimePickerModal(
            title = "Habit Reminder",
            initialHour = remHour,
            initialMinute = remMinute,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { h, m -> remHour = h; remMinute = m }
        )
    }
}

@Composable
fun HabitCheckInDialog(
    habitName: String,
    dateLabel: String,
    initialType: CompletionType = CompletionType.ACHIEVED,
    initialNotes: String = "",
    canClearExisting: Boolean = false,
    onDismiss: () -> Unit,
    onClear: () -> Unit = {},
    onSubmit: (CompletionType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var notes by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(habitName, style = MaterialTheme.typography.titleMedium)
                    Text(dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == CompletionType.ACHIEVED,
                        onClick = { selectedType = CompletionType.ACHIEVED },
                        label = { Text("Achieved ✓") }
                    )
                    FilterChip(
                        selected = selectedType == CompletionType.GAVE_UP,
                        onClick = { selectedType = CompletionType.GAVE_UP },
                        label = { Text("Gave Up / Missed") }
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reflection or note (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedType, notes.trim()) }) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            Row {
                if (canClearExisting) {
                    TextButton(onClick = {
                        onClear()
                        onDismiss()
                    }) {
                        Text("Clear Day", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
