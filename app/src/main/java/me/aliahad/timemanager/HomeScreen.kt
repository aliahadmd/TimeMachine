package me.aliahad.timemanager

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.activity.ComponentActivity
import me.aliahad.timemanager.permissions.ExactAlarmPermissionManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun HomeScreen(
    onBlockClick: (TimerBlockType) -> Unit
) {
    val context = LocalContext.current
    var showNotificationDialog by remember { mutableStateOf(false) }
    var areNotificationsConfigured by remember { mutableStateOf(false) }
    var needsExactAlarmPermission by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var dismissedExactAlarmBanner by remember { mutableStateOf(false) }
    var dismissedNotificationBanner by remember {
        mutableStateOf(
            context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                .getBoolean("dismissed_notification_banner", false)
        )
    }
    
    // Check notification settings whenever app comes to foreground
    DisposableEffect(Unit) {
        val checkSettings = {
            val configured = NotificationSettingsHelper.areNotificationChannelsConfigured(context)
            areNotificationsConfigured = configured
            val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
            dismissedNotificationBanner = prefs.getBoolean("dismissed_notification_banner", false)
            if (!configured && !showNotificationDialog && !dismissedNotificationBanner) {
                // Only show dialog on first launch or if user hasn't seen it this session
                val hasSeenDialog = prefs.getBoolean("has_seen_dialog", false)
                if (!hasSeenDialog) {
                    showNotificationDialog = true
                }
            }

            val needsExact = ExactAlarmPermissionManager.needsExactAlarm(context)
            needsExactAlarmPermission = needsExact
            if (needsExact) {
                val hasSeenExactDialog = prefs.getBoolean("has_seen_exact_alarm_dialog", false)
                if (!hasSeenExactDialog) {
                    showExactAlarmDialog = true
                }
                dismissedExactAlarmBanner = false
            }
        }
        
        // Initial check
        checkSettings()
        
        // Re-check when activity resumes (e.g., returning from settings)
        val activity = context as? ComponentActivity
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                checkSettings()
            }
        }
        activity?.lifecycle?.addObserver(lifecycleObserver)
        
        onDispose {
            activity?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }
    
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Show notification setup banner if needed
            if (!areNotificationsConfigured && !dismissedNotificationBanner) {
                NotificationSetupBanner(
                    onOpenSettings = {
                        NotificationSettingsHelper.openNotificationSettings(context)
                    },
                    onDismiss = {
                        dismissedNotificationBanner = true
                        context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("dismissed_notification_banner", true)
                            .apply()
                    }
                )
            }

            if (needsExactAlarmPermission && !dismissedExactAlarmBanner) {
                ExactAlarmPermissionBanner(
                    onDismiss = { dismissedExactAlarmBanner = true },
                    onOpenSettings = {
                        val activity = context as? ComponentActivity
                        if (activity != null) {
                            ExactAlarmPermissionManager.openExactAlarmSettings(activity)
                        } else {
                            NotificationSettingsHelper.openNotificationSettings(context)
                        }
                    }
                )
            }
            
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Text(
                    text = "TimeMachine",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            
                // Grid of timer blocks
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(getTimerBlocks()) { block ->
                        TimerBlockCard(
                            block = block,
                            onClick = { onBlockClick(block.type) }
                        )
                    }
                }
            }
        }
        
        // Show notification setup dialog on first launch
        if (showNotificationDialog) {
            NotificationSetupDialog(
                onDismiss = {
                    showNotificationDialog = false
                    // Mark that user has seen the dialog
                    val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_dialog", true).apply()
                },
                onOpenSettings = {
                    NotificationSettingsHelper.openNotificationSettings(context)
                    showNotificationDialog = false
                    // Mark that user has seen the dialog
                    val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_dialog", true).apply()
                }
            )
        }

        if (showExactAlarmDialog && needsExactAlarmPermission) {
            val activity = context as? ComponentActivity
            ExactAlarmPermissionDialog(
                showBatteryHelp = activity?.let { !ExactAlarmPermissionManager.hasBatteryException(it) } ?: false,
                onDismiss = {
                    showExactAlarmDialog = false
                    val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_exact_alarm_dialog", true).apply()
                },
                onOpenExactAlarmSettings = {
                    val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_seen_exact_alarm_dialog", true).apply()
                    if (activity != null) {
                        ExactAlarmPermissionManager.openExactAlarmSettings(activity)
                    } else {
                        NotificationSettingsHelper.openNotificationSettings(context)
                    }
                    showExactAlarmDialog = false
                },
                onOpenBatterySettings = {
                    if (activity != null) {
                        ExactAlarmPermissionManager.requestBatteryException(activity)
                    } else {
                        NotificationSettingsHelper.openNotificationSettings(context)
                    }
                }
            )
        }
    }
}

@Composable
fun TimerBlockCard(
    block: TimerBlock,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = remember { me.aliahad.timemanager.data.TimerDatabase.getDatabase(context) }
    
    // For Focus Timer, get today's tracked time
    var todayMinutes by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // Listen for lifecycle events to refresh immediately when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    if (block.type == TimerBlockType.FOCUS_TIMER) {
        LaunchedEffect(refreshTrigger) {
            while (true) {
                val today = getTodayDateString()
                val minutes = database.timeSessionDao().getTotalMinutesForDate(today) ?: 0
                android.util.Log.d("HomeScreen", "Fetching today's minutes for $today: $minutes min (refreshTrigger=$refreshTrigger)")
                todayMinutes = minutes
                kotlinx.coroutines.delay(5000) // Update every 5 seconds for more responsive UI
            }
        }
    }
    
    // For Habit Tracker, get habit count
    var habitCount by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.HABIT_TRACKER) {
        val habits by database.habitDao().getAllActiveHabits().collectAsState(initial = emptyList())
        habitCount = habits.size
    }
    
    // For Year Calculator, get saved calculations count
    var calculationCount by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.YEAR_CALCULATOR) {
        val calculations by database.dateCalculationDao().getAllCalculations().collectAsState(initial = emptyList())
        calculationCount = calculations.size
    }
    
    // For BMI Calculator, get saved BMI count
    var bmiCount by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.BMI_CALCULATOR) {
        val bmiCalculations by database.bmiCalculationDao().getAllCalculations().collectAsState(initial = emptyList())
        bmiCount = bmiCalculations.size
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = block.baseColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title
            Text(
                text = block.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    block.baseColor.copy(alpha = 0.15f),
                                    block.baseColor.copy(alpha = 0.25f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = block.icon,
                        contentDescription = block.title,
                        modifier = Modifier.size(40.dp),
                        tint = block.baseColor
                    )
                }
            
            // Display statistics for each block type
            if (block.type == TimerBlockType.FOCUS_TIMER) {
                Text(
                    text = formatDuration(todayMinutes),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.HABIT_TRACKER) {
                Text(
                    text = "$habitCount",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (habitCount == 1) "habit" else "habits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.YEAR_CALCULATOR) {
                Text(
                    text = "$calculationCount",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (calculationCount == 1) "calculation" else "calculations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.BMI_CALCULATOR) {
                Text(
                    text = "$bmiCount",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (bmiCount == 1) "record" else "records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "0:00",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Data classes
data class TimerBlock(
    val type: TimerBlockType,
    val title: String,
    val icon: ImageVector,
    val baseColor: Color
)

enum class TimerBlockType {
    FOCUS_TIMER,
    HABIT_TRACKER,
    YEAR_CALCULATOR,
    BMI_CALCULATOR
}

// Get available timer blocks with varied colors
fun getTimerBlocks(): List<TimerBlock> {
    return listOf(
        TimerBlock(
            type = TimerBlockType.FOCUS_TIMER,
            title = "Focus Tracker",
            icon = Icons.Default.Timer,
            baseColor = Color(0xFFFF6B6B) // Vibrant Red
        ),
        TimerBlock(
            type = TimerBlockType.HABIT_TRACKER,
            title = "Habit Tracker",
            icon = Icons.Default.CheckCircle,
            baseColor = Color(0xFF51CF66) // Vibrant Green
        ),
        TimerBlock(
            type = TimerBlockType.YEAR_CALCULATOR,
            title = "Year Calculator",
            icon = Icons.Default.CalendarMonth,
            baseColor = Color(0xFF4DABF7) // Vibrant Blue
        ),
        TimerBlock(
            type = TimerBlockType.BMI_CALCULATOR,
            title = "BMI Calculator",
            icon = Icons.Default.FitnessCenter,
            baseColor = Color(0xFFAB47BC) // Vibrant Purple
        )
    )
}
