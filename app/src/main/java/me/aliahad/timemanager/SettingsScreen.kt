package me.aliahad.timemanager

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.datastore.ThemeMode
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.permissions.DeviceReliabilityManager
import me.aliahad.timemanager.permissions.HealthSeverity
import me.aliahad.timemanager.system.FocusTileService
import me.aliahad.timemanager.system.ScreenTimeTileService
import me.aliahad.timemanager.ui.components.ConfirmDeleteDialog
import me.aliahad.timemanager.ui.components.M3TimePickerModal
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.BackupActionStatus
import me.aliahad.timemanager.ui.viewmodel.SettingsViewModel
import me.aliahad.timemanager.widgets.TodayFocusWidgetReceiver
import me.aliahad.timemanager.widgets.TodayHabitsWidgetReceiver
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> SettingsViewModel(container) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshReliabilityChecks()
    }

    var pickQuietStart by remember { mutableStateOf(false) }
    var pickQuietEnd by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    LaunchedEffect(state.backupStatus) {
        when (val status = state.backupStatus) {
            is BackupActionStatus.Success -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.clearBackupStatus()
            }
            is BackupActionStatus.Error -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.clearBackupStatus()
            }
            else -> Unit
        }
    }

    val prefs = state.preferences
    val statusColors = TimeMachineTheme.statusColors

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TimeMachineTopBar(
                title = "Settings",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "appearance") {
                SectionCard {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size)
                            ) {
                                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dynamic Color (Material You)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Match phone wallpaper palette",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) }
                            )
                        }
                    }
                }
            }

            item(key = "goals_currency") {
                SectionCard {
                    Text("Currency & Daily Goals", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Default Currency (${prefs.currencyCode})", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyCatalog.ALL.forEach { spec ->
                            FilterChip(
                                selected = spec.code == prefs.currencyCode,
                                onClick = { viewModel.setCurrencyCode(spec.code) },
                                label = { Text("${spec.code} (${spec.symbol})") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Daily Focus Goal: ${Formatters.formatMinutes(prefs.dailyFocusGoalMinutes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = prefs.dailyFocusGoalMinutes.toFloat(),
                        onValueChange = { viewModel.setDailyFocusGoalMinutes(((it / 15f).roundToInt() * 15).coerceIn(15, 480)) },
                        valueRange = 15f..480f
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Daily Screen Time Limit: ${Formatters.formatMinutes(prefs.dailyScreenTimeLimitMinutes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = prefs.dailyScreenTimeLimitMinutes.toFloat(),
                        onValueChange = { viewModel.setDailyScreenTimeLimitMinutes(((it / 15f).roundToInt() * 15).coerceIn(30, 600)) },
                        valueRange = 30f..600f
                    )
                }
            }

            item(key = "notifications_prefs") {
                SectionCard {
                    Text("Notifications & Focus Behavior", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Enable Notifications",
                        subtitle = "Habit, Planner, Subscription & Budget alerts",
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    SettingSwitchRow(
                        title = "Quiet Hours",
                        subtitle = String.format(
                            java.util.Locale.US,
                            "Silence reminders %02d:%02d – %02d:%02d",
                            prefs.quietStartHour, prefs.quietStartMinute,
                            prefs.quietEndHour, prefs.quietEndMinute
                        ),
                        checked = prefs.quietHoursEnabled,
                        onCheckedChange = {
                            viewModel.setQuietHours(
                                it, prefs.quietStartHour, prefs.quietStartMinute,
                                prefs.quietEndHour, prefs.quietEndMinute
                            )
                        }
                    )
                    if (prefs.quietHoursEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pickQuietStart = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(String.format(java.util.Locale.US, "Start %02d:%02d", prefs.quietStartHour, prefs.quietStartMinute))
                            }
                            OutlinedButton(
                                onClick = { pickQuietEnd = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(String.format(java.util.Locale.US, "End %02d:%02d", prefs.quietEndHour, prefs.quietEndMinute))
                            }
                        }
                    }
                    SettingSwitchRow(
                        title = "Focus Priority DND",
                        subtitle = "Automatically enable Priority Do Not Disturb while timer runs (requires DND Access)",
                        checked = prefs.focusDndEnabled,
                        onCheckedChange = { viewModel.setFocusDndEnabled(it) }
                    )
                    SettingSwitchRow(
                        title = "Strict Completion Alarm",
                        subtitle = "Wake Lock Screen when a Focus countdown finishes",
                        checked = prefs.focusStrictAlarmEnabled,
                        onCheckedChange = { viewModel.setFocusStrictAlarmEnabled(it) }
                    )
                }
            }

            item(key = "reliability_center") {
                SectionCard {
                    Text(
                        text = if (state.isXiaomiDevice) "Phone & HyperOS Reliability Center"
                        else "Phone System Reliability",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep timers alive in background and ensure reminders arrive on schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.reliabilityChecks.forEach { check ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (check.isReady) statusColors.successContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (check.isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (check.isReady) statusColors.success
                                        else if (check.severity == HealthSeverity.CRITICAL) statusColors.danger
                                        else statusColors.warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(check.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            check.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(onClick = {
                                        DeviceReliabilityManager.handleCheckAction(context, check.id)
                                    }) {
                                        Text(check.actionLabel)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Widgets & Quick Settings", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                DeviceReliabilityManager.requestAddQuickSettingsTile(
                                    context,
                                    FocusTileService::class.java,
                                    "Focus Timer"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Focus Tile") }
                        OutlinedButton(
                            onClick = {
                                DeviceReliabilityManager.requestAddQuickSettingsTile(
                                    context,
                                    ScreenTimeTileService::class.java,
                                    "Screen Time"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Screen Tile") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                DeviceReliabilityManager.requestPinWidget(
                                    context,
                                    TodayFocusWidgetReceiver::class.java
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Pin Focus Widget") }
                        OutlinedButton(
                            onClick = {
                                DeviceReliabilityManager.requestPinWidget(
                                    context,
                                    TodayHabitsWidgetReceiver::class.java
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Pin Habits Widget") }
                    }
                }
            }

            item(key = "backup_data") {
                SectionCard {
                    Text("Backup & Data", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("timemachine_backup_${java.time.LocalDate.now()}.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Local Records")
                    }
                }
            }

            item(key = "about") {
                SectionCard {
                    Text("About TimeMachine", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • DB Schema v${TimerDatabase.DATABASE_VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.replayTutorial() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replay App Guide")
                    }
                }
            }
        }
    }

    if (pickQuietStart) {
        M3TimePickerModal(
            title = "Quiet Hours Start",
            initialHour = prefs.quietStartHour,
            initialMinute = prefs.quietStartMinute,
            onDismiss = { pickQuietStart = false },
            onTimeSelected = { h, m ->
                viewModel.setQuietHours(true, h, m, prefs.quietEndHour, prefs.quietEndMinute)
            }
        )
    }

    if (pickQuietEnd) {
        M3TimePickerModal(
            title = "Quiet Hours End",
            initialHour = prefs.quietEndHour,
            initialMinute = prefs.quietEndMinute,
            onDismiss = { pickQuietEnd = false },
            onTimeSelected = { h, m ->
                viewModel.setQuietHours(true, prefs.quietStartHour, prefs.quietStartMinute, h, m)
            }
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Choose Restore Mode") },
            text = {
                Text("Replace removes existing local records before importing the backup. Merge inserts backup entries alongside existing data.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importBackup(uri, ImportMode.REPLACE)
                    pendingImportUri = null
                }) { Text("Replace All") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
                    TextButton(onClick = {
                        viewModel.importBackup(uri, ImportMode.MERGE)
                        pendingImportUri = null
                    }) { Text("Merge") }
                }
            }
        )
    }

    if (showClearConfirm) {
        ConfirmDeleteDialog(
            title = "Clear All Local Data?",
            message = "This permanently deletes all Focus sessions, Habits, Tasks, Expenses, Subscriptions, Calculators, and Screen Time logs. Theme and app settings are preserved.",
            confirmText = "Clear Everything",
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                viewModel.clearAllUserData()
                showClearConfirm = false
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
