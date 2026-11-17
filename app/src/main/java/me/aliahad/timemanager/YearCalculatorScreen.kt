package me.aliahad.timemanager

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.DateCalculation
import me.aliahad.timemanager.data.DateCategories
import me.aliahad.timemanager.ui.components.ConfirmDeleteDialog
import me.aliahad.timemanager.ui.components.EmptyStateView
import me.aliahad.timemanager.ui.components.M3DatePickerModal
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.viewmodel.YearCalculatorViewModel
import java.time.LocalDate

@Composable
fun YearCalculatorScreen(onNavigateBack: () -> Unit) {
    val viewModel = appViewModel { container -> YearCalculatorViewModel(container) }
    val savedList by viewModel.savedCalculations.collectAsStateWithLifecycle()

    var modeIndex by rememberSaveable { mutableIntStateOf(0) } // 0: Since / Age, 1: Between Two Dates
    var startDate by remember { mutableStateOf(LocalDate.of(2000, 1, 1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var editingCalculation by remember { mutableStateOf<DateCalculation?>(null) }
    var pendingDelete by remember { mutableStateOf<DateCalculation?>(null) }

    val effectiveEndDate = if (modeIndex == 0) LocalDate.now() else endDate
    val diff = remember(startDate, effectiveEndDate) {
        calculateDateDifference(startDate, effectiveEndDate)
    }
    val westernZodiac = remember(startDate) { WesternZodiac.fromDate(startDate) }
    val chineseZodiac = remember(startDate) { ChineseZodiac.fromYear(startDate.year) }
    val milestones = remember(diff.years) { getAgeMilestones(diff.years) }

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = "Year & Date Calculator",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "calc_inputs") {
                SectionCard {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = modeIndex == 0,
                            onClick = { modeIndex = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Age / Elapsed") }
                        SegmentedButton(
                            selected = modeIndex == 1,
                            onClick = { modeIndex = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Between Dates") }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickingStart = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (modeIndex == 0) "From: ${startDate.toShortString()}"
                                else "Start: ${startDate.toShortString()}",
                                maxLines = 1
                            )
                        }
                        if (modeIndex == 1) {
                            OutlinedButton(
                                onClick = { pickingEnd = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("End: ${endDate.toShortString()}", maxLines = 1)
                            }
                        }
                    }
                }
            }

            item(key = "calc_results") {
                SectionCard {
                    Text(
                        text = if (diff.isNegative) "UNTIL TARGET DATE" else "ELAPSED DIFFERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${diff.years} yrs • ${diff.months} mos • ${diff.days} days",
                        style = MaterialTheme.typography.headlineSmall.merge(TabularNumberStyle),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DateStatColumn("Days", diff.totalDays.toString())
                        DateStatColumn("Weeks", diff.totalWeeks.toString())
                        DateStatColumn("Months", diff.totalMonths.toString())
                        DateStatColumn("Hours", diff.totalHours.toString())
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${westernZodiac.symbol} ${westernZodiac.displayName} • ${chineseZodiac.symbol} Year of the ${chineseZodiac.displayName} (${chineseZodiac.element})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    diff.daysUntilNext?.let { daysNext ->
                        Text(
                            text = "Next anniversary in $daysNext day${if (daysNext == 1L) "" else "s"} (${diff.nextOccurrence?.toShortString()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (milestones.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = milestones.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            editingCalculation = null
                            showSaveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Save Milestone")
                    }
                }
            }

            item(key = "saved_header") {
                Text("Saved Milestones (${savedList.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (savedList.isEmpty()) {
                item(key = "empty_dates") {
                    EmptyStateView(
                        emoji = "🎂",
                        title = "No Saved Dates Yet",
                        subtitle = "Save birthdays, anniversaries, work milestones, or countdowns above."
                    )
                }
            } else {
                items(savedList, key = { it.id }) { item ->
                    val sDate = item.startDate.toLocalDate()
                    val eDate = if (modeIndex == 0 && item.endDate == item.startDate) LocalDate.now() else LocalDate.now()
                    val itemDiff = calculateDateDifference(sDate, eDate)
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.category} • ${sDate.toReadableString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${itemDiff.years}y ${itemDiff.months}m ${itemDiff.days}d • Next in ${itemDiff.daysUntilNext ?: 0}d",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                editingCalculation = item
                                startDate = sDate
                                endDate = item.endDate.toLocalDate()
                                showSaveDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { pendingDelete = item }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickingStart) {
        M3DatePickerModal(
            initialDate = startDate,
            onDismiss = { pickingStart = false },
            onDateSelected = { startDate = it }
        )
    }
    if (pickingEnd) {
        M3DatePickerModal(
            initialDate = endDate,
            onDismiss = { pickingEnd = false },
            onDateSelected = { endDate = it }
        )
    }

    if (showSaveDialog) {
        SaveDateDialog(
            existing = editingCalculation,
            startDate = startDate,
            endDate = effectiveEndDate,
            onDismiss = { showSaveDialog = false },
            onSave = { item, isEdit ->
                viewModel.saveCalculation(item, isEdit)
                showSaveDialog = false
            }
        )
    }

    pendingDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete Saved Date?",
            message = "Remove \"${item.title}\"?",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                viewModel.deleteCalculation(item)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun DateStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SaveDateDialog(
    existing: DateCalculation?,
    startDate: LocalDate,
    endDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (DateCalculation, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: DateCategories.BIRTHDAY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Save Milestone" else "Edit Milestone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Mom's Birthday)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateCategories.DEFAULT_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val nowMs = System.currentTimeMillis()
                    onSave(
                        DateCalculation(
                            id = existing?.id ?: 0L,
                            title = title.trim(),
                            category = category,
                            startDate = startDate.toMillis(),
                            endDate = endDate.toMillis(),
                            createdAt = existing?.createdAt ?: nowMs,
                            updatedAt = nowMs
                        ),
                        existing != null
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
