package me.aliahad.timemanager

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun HabitTrackerScreen(
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val repository = remember { HabitRepository(database.habitDao()) }
    val habits by repository.allActiveHabits.collectAsState(initial = emptyList())
    
    // Collect stats for all habits at screen level
    val habitStatsMap = remember { mutableStateMapOf<Long, HabitWithStats>() }
    
    LaunchedEffect(habits) {
        habits.forEach { habit ->
            launch {
                repository.getHabitWithStats(habit.id).collect { stats ->
                    stats?.let { habitStatsMap[habit.id] = it }
                }
            }
        }
    }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedHabitId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPress) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                
                Text(
                    text = "Habit Tracker",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Habit List
            if (habits.isEmpty()) {
                EmptyHabitsView(onCreateClick = { showCreateDialog = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            habitStats = habitStatsMap[habit.id],
                            repository = repository,
                            onClick = { selectedHabitId = habit.id }
                        )
                    }
                }
            }
        }
    }
    
    // Create/Edit Dialog
    if (showCreateDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { habit ->
                scope.launch {
                    // Insert habit and get the generated ID
                    val habitId = repository.insertHabit(habit)
                    
                    // Schedule reminder with the real ID from database
                    if (habit.reminderTimeHour != null && habit.reminderTimeMinute != null) {
                        val habitWithId = habit.copy(id = habitId)
                        NotificationScheduler.scheduleHabitReminder(context, habitWithId)
                    }
                    
                    showCreateDialog = false
                }
            }
        )
    }
    
    // Navigate to detail view
    selectedHabitId?.let { habitId ->
        HabitDetailScreen(
            habitId = habitId,
            repository = repository,
            onBack = { selectedHabitId = null }
        )
    }
}

@Composable
fun EmptyHabitsView(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Habits Yet",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Start building good habits or\nquitting bad ones",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Habit")
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    habitStats: HabitWithStats?,
    repository: HabitRepository,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    
    var showSubmitDialog by remember { mutableStateOf(false) }
    var todayCompletion by remember { mutableStateOf<HabitCompletion?>(null) }
    
    // Only fetch today's completion, not full stats
    LaunchedEffect(habit.id) {
        todayCompletion = repository.habitDao.getCompletion(habit.id, today)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with color indicator
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(habit.color))
                ) {
                    Icon(
                        imageVector = getIconByName(habit.iconName),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center),
                        tint = Color.White
                    )
                }
                
                // Habit info with inline stats
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                        // Inline analytics
                        habitStats?.let { stats ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stats.currentStreak > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔥",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${stats.currentStreak}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "${stats.totalAchieved}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFE57373)
                                    )
                                    Text(
                                        text = "${stats.totalGaveUp}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFFE57373)
                                    )
                                }
                                
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                
                                Text(
                                    text = "${(stats.successRate * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (stats.successRate >= 0.7f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                }
            }
            
            // Submission button
            IconButton(
                onClick = {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    showSubmitDialog = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (todayCompletion?.completionType) {
                            CompletionType.ACHIEVED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            CompletionType.GAVE_UP -> Color(0xFFE57373).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Icon(
                    imageVector = when (todayCompletion?.completionType) {
                        CompletionType.ACHIEVED -> Icons.Default.CheckCircle
                        CompletionType.GAVE_UP -> Icons.Default.Cancel
                        else -> Icons.Default.Circle
                    },
                    contentDescription = "Submit",
                    tint = when (todayCompletion?.completionType) {
                        CompletionType.ACHIEVED -> Color(0xFF4CAF50)
                        CompletionType.GAVE_UP -> Color(0xFFE57373)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
    
    // Submission dialog
    if (showSubmitDialog) {
        HabitSubmissionDialog(
            habit = habit,
            existingCompletion = todayCompletion,
            onDismiss = { showSubmitDialog = false },
            onSubmit = { completionType ->
                scope.launch {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    if (todayCompletion != null) {
                        repository.removeCompletion(habit.id, today)
                    }
                    repository.addCompletion(habit.id, today, completionType)
                    todayCompletion = repository.habitDao.getCompletion(habit.id, today)
                    showSubmitDialog = false
                }
            },
            onRemove = {
                scope.launch {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    repository.removeCompletion(habit.id, today)
                    todayCompletion = null
                    showSubmitDialog = false
                }
            }
        )
    }
}

