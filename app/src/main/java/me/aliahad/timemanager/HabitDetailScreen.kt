package me.aliahad.timemanager

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.HabitRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun HabitDetailScreen(
    habitId: Long,
    repository: HabitRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val scope = rememberCoroutineScope()
    
    val habitStats by repository.getHabitWithStats(habitId).collectAsState(initial = null)
    val completions by repository.getHabitCompletions(habitId).collectAsState(initial = emptyList())
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        habitStats?.let { stats ->
            val habit = stats.habit
            
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with back, edit, and delete
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                        
                        Row {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // Habit Info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(habit.color))
                        ) {
                            Icon(
                                imageVector = getIconByName(habit.iconName),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        if (habit.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (habit.type == me.aliahad.timemanager.data.HabitType.BUILD) "Build Habit" else "Quit Habit",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Stats Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Statistics",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        // Row 1: Streaks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Current",
                                value = "${stats.currentStreak}",
                                subtitle = "day streak",
                                icon = Icons.Default.LocalFireDepartment,
                                color = Color(0xFFFF6B6B),
                                modifier = Modifier.weight(1f)
                            )
                            
                            StatCard(
                                title = "Best",
                                value = "${stats.longestStreak}",
                                subtitle = "days",
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFD93D),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Row 2: Completions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Achieved",
                                value = "${stats.totalAchieved}",
                                subtitle = "days",
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            
                            StatCard(
                                title = "Gave Up",
                                value = "${stats.totalGaveUp}",
                                subtitle = "days",
                                icon = Icons.Default.Cancel,
                                color = Color(0xFFE57373),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Row 3: Rates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Success",
                                value = "${(stats.successRate * 100).toInt()}%",
                                subtitle = "of submissions",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF6BCF7F),
                                modifier = Modifier.weight(1f)
                            )
                            
                            StatCard(
                                title = "Activity",
                                value = "${(stats.completionRate * 100).toInt()}%",
                                subtitle = "consistency",
                                icon = Icons.Default.Timeline,
                                color = Color(0xFF64B5F6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // GitHub Heatmap
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Activity - Last 90 Days",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        
                        GitHubStyleHeatmap(
                            completions = completions,
                            habitColor = Color(habit.color)
                        )
                    }
                }
                
                // Calendar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        
                        MonthCalendarView(
                            completions = completions,
                            habitColor = Color(habit.color)
                        )
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
    
    // Edit dialog
    if (showEditDialog) {
        habitStats?.let { stats ->
            EditHabitDialog(
                habit = stats.habit,
                onDismiss = { showEditDialog = false },
                onSave = { updatedHabit ->
                    scope.launch {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        
                        // Cancel old reminder
                        NotificationScheduler.cancelHabitReminder(context, updatedHabit.id)
                        
                        // Update habit in database
                        repository.updateHabit(updatedHabit)
                        
                        // Schedule new reminder if enabled
                        if (updatedHabit.reminderTimeHour != null && updatedHabit.reminderTimeMinute != null) {
                            NotificationScheduler.scheduleHabitReminder(context, updatedHabit)
                        }
                        
                        showEditDialog = false
                    }
                }
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Habit?")
            },
            text = {
                Text("This will permanently delete this habit and all its history. This action cannot be undone.")
            },
            confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                habitStats?.let {
                                    // Cancel reminder
                                    NotificationScheduler.cancelHabitReminder(context, it.habit.id)
                                    repository.deleteHabit(it.habit)
                                }
                                showDeleteDialog = false
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun GitHubStyleHeatmap(
    completions: List<me.aliahad.timemanager.data.HabitCompletion>,
    habitColor: Color
) {
    val completionMap = completions.associateBy { it.date }
    val today = LocalDate.now()
    val startDate = today.minusDays(89)
    
    val weeks = mutableListOf<List<LocalDate>>()
    var currentWeek = mutableListOf<LocalDate>()
    var currentDate = startDate
    
    while (currentDate <= today) {
        currentWeek.add(currentDate)
        if (currentDate.dayOfWeek == java.time.DayOfWeek.SUNDAY || currentDate == today) {
            weeks.add(currentWeek.toList())
            currentWeek = mutableListOf()
        }
        currentDate = currentDate.plusDays(1)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Weekday labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        week.forEach { date ->
                            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val completion = completionMap[dateString]
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when (completion?.completionType) {
                                            me.aliahad.timemanager.data.CompletionType.ACHIEVED -> Color(0xFF4CAF50)
                                            me.aliahad.timemanager.data.CompletionType.GAVE_UP -> Color(0xFFE57373)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        text = "Achieved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE57373))
                    )
                    Text(
                        text = "Gave Up",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthCalendarView(
    completions: List<me.aliahad.timemanager.data.HabitCompletion>,
    habitColor: Color
) {
    val completionMap = completions.associateBy { it.date }
    val currentMonth = remember { YearMonth.now() }
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    val today = LocalDate.now()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.year,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Calendar grid - using Column/Row instead of LazyVerticalGrid to avoid nested scrolling
        val allDays = mutableListOf<LocalDate?>()
        
        // Add empty slots for days before first day of month
        repeat(startDayOfWeek) {
            allDays.add(null)
        }
        
        // Add all days of the month
        for (day in 1..lastDayOfMonth.dayOfMonth) {
            allDays.add(currentMonth.atDay(day))
        }
        
        // Create rows of 7 days
        val rows = allDays.chunked(7)
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            if (date != null) {
                                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val completion = completionMap[dateString]
                                val isToday = date == today
                                
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = when (completion?.completionType) {
                                        me.aliahad.timemanager.data.CompletionType.ACHIEVED -> Color(0xFF4CAF50)
                                        me.aliahad.timemanager.data.CompletionType.GAVE_UP -> Color(0xFFE57373)
                                        else -> if (isToday)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    border = if (isToday)
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    else null
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when (completion?.completionType) {
                                                me.aliahad.timemanager.data.CompletionType.ACHIEVED,
                                                me.aliahad.timemanager.data.CompletionType.GAVE_UP -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Fill remaining slots in the row
                    repeat(7 - row.size) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

