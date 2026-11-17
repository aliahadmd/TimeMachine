package me.aliahad.timemanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.UserProfile
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> ProfileViewModel(container) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    val stats = state.stats
    val sym = state.currencySymbol

    Scaffold(
        topBar = {
            TimeMachineTopBar(
                title = "Profile & Insights",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.refreshStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Insights")
                    }
                }
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
            item(key = "profile_card") {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.profile.avatarIcon, fontSize = 44.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.profile.name, style = MaterialTheme.typography.headlineSmall)
                            if (state.profile.bio.isNotBlank()) {
                                Text(
                                    state.profile.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
            }

            if (stats != null) {
                item(key = "focus_wellbeing_insights") {
                    SectionCard {
                        Text("Focus & Screen Time", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        MetricRow("Today's Focus", Formatters.formatMinutes(stats.todayFocusMinutes))
                        MetricRow("All-Time Focus", Formatters.formatMinutes(stats.totalFocusMinutes))
                        MetricRow("Total Sessions", stats.totalFocusSessions.toString())
                        MetricRow("Focus Streak", "${stats.focusStreak} day(s)")
                        MetricRow("Today's Screen Time", Formatters.formatMinutes(stats.todayScreenTimeSeconds / 60))
                        MetricRow("7-Day Avg Screen Time", Formatters.formatMinutes(stats.sevenDayAvgScreenTimeSeconds / 60))
                    }
                }

                item(key = "habits_planner_insights") {
                    SectionCard {
                        Text("Habits & Planner", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        MetricRow("Active Habits", "${stats.activeHabits} (${stats.totalHabits} total)")
                        MetricRow("Habit Check-Ins", stats.totalHabitCompletions.toString())
                        MetricRow("Habit Success Rate", "${(stats.habitSuccessRate * 100).toInt()}%")
                        MetricRow("Tasks Completed", "${stats.completedTasks} / ${stats.totalTasks}")
                        MetricRow("Task Completion Rate", "${(stats.taskCompletionRate * 100).toInt()}%")
                    }
                }

                item(key = "finance_tools_insights") {
                    SectionCard {
                        Text("Finance & Tools", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        MetricRow("Spent (Last 30 Days)", Formatters.formatCurrency(stats.totalSpentLast30Days, sym))
                        MetricRow("Avg / Active Spend Day", Formatters.formatCurrency(stats.averageDailySpending, sym))
                        MetricRow("Active Subscriptions", stats.activeSubscriptions.toString())
                        MetricRow("Monthly Subscriptions", Formatters.formatCurrency(stats.monthlySubscriptionCost, sym))
                        MetricRow("Saved Dates", stats.savedCalculations.toString())
                        stats.latestBMI?.let {
                            MetricRow("Latest BMI", Formatters.formatOneDecimal(it))
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = state.profile,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateProfile(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatarIcon by remember { mutableStateOf(profile.avatarIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("👤", "🧑‍💻", "🎯", "🌱", "🏃", "📚").forEach { emoji: String ->
                        TextButton(onClick = { avatarIcon = emoji }) {
                            Text(emoji, fontSize = if (avatarIcon == emoji) 24.sp else 18.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Focus Motto / Bio") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        profile.copy(
                            name = name.trim(),
                            bio = bio.trim(),
                            avatarIcon = avatarIcon,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
