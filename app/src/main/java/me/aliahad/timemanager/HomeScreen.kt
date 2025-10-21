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
import androidx.compose.runtime.collectAsState
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
    
    // Get user's currency preference
    val userProfile by database.userProfileDao().getProfile().collectAsState(initial = null)
    val currency = userProfile?.currency ?: "৳"
    
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
    
    // For Expense Tracker, get today's total expenses
    var todayExpenses by remember { mutableStateOf(0.0) }
    
    if (block.type == TimerBlockType.EXPENSE_TRACKER) {
        LaunchedEffect(refreshTrigger) {
            while (true) {
                val today = ExpenseAnalytics.getTodayDateString()
                val total = database.expenseDao().getTotalForDate(today) ?: 0.0
                todayExpenses = total
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    // For Subscription Tracker, get active subscriptions count and monthly cost
    var activeSubsCount by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.SUBSCRIPTION_TRACKER) {
        LaunchedEffect(refreshTrigger) {
            while (true) {
                val count = database.subscriptionDao().getActiveSubscriptionCount()
                activeSubsCount = count
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    // For Daily Planner, get today's task count
    var todayTaskCount by remember { mutableIntStateOf(0) }
    var todayCompletedCount by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.DAILY_PLANNER) {
        LaunchedEffect(refreshTrigger) {
            while (true) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val todayDate = DailyPlannerUtils.getTodayDateString()
                    todayTaskCount = database.dailyTaskDao().getTaskCountForDate(todayDate)
                    todayCompletedCount = database.dailyTaskDao().getCompletedCountForDate(todayDate)
                }
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    // For Profile, get username
    var userName by remember { mutableStateOf("User") }
    
    if (block.type == TimerBlockType.PROFILE) {
        val profile by database.userProfileDao().getProfile().collectAsState(initial = null)
        userName = profile?.name ?: "User"
    }
    
    // For Settings, check for warnings/status
    var settingsWarning by remember { mutableStateOf("") }
    
    if (block.type == TimerBlockType.SETTINGS) {
        LaunchedEffect(refreshTrigger) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Check if profile exists
                val profileCount = database.userProfileDao().getProfileCount()
                settingsWarning = if (profileCount == 0) {
                    "⚠️ Setup needed"
                } else {
                    "All configured"
                }
            }
        }
    }
    
    // For Screen Timer, get today's screen time
    var todayScreenTime by remember { mutableIntStateOf(0) }
    var todayPickups by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.SCREEN_TIMER) {
        val todayDate = remember { getTodayDateString() }
        val summaryFlow = remember(todayDate) { database.screenTimeDao().getDailySummaryFlow(todayDate) }
        val summary by summaryFlow.collectAsState(initial = null)
        todayScreenTime = summary?.totalScreenTimeSeconds ?: 0
        todayPickups = summary?.pickupsCount ?: 0
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
            } else if (block.type == TimerBlockType.EXPENSE_TRACKER) {
                Text(
                    text = ExpenseAnalytics.formatCurrency(todayExpenses, currency),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "spent today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.SUBSCRIPTION_TRACKER) {
                Text(
                    text = "$activeSubsCount",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (activeSubsCount == 1) "subscription" else "subscriptions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.DAILY_PLANNER) {
                Text(
                    text = "$todayCompletedCount/$todayTaskCount",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "tasks completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.PROFILE) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "Your Profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.SETTINGS) {
                Text(
                    text = settingsWarning,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (settingsWarning.contains("⚠️")) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = if (settingsWarning.contains("⚠️")) {
                        Color(0xFFFF9800)
                    } else {
                        Color(0xFF4CAF50)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else if (block.type == TimerBlockType.SCREEN_TIMER) {
                Text(
                    text = ScreenTimeAnalytics.formatDuration(todayScreenTime),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$todayPickups pickups",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else {
                // Fallback for any future block types
                Text(
                    text = "Tap to open",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
    BMI_CALCULATOR,
    EXPENSE_TRACKER,
    SUBSCRIPTION_TRACKER,
    DAILY_PLANNER,
    SCREEN_TIMER,
    PROFILE,
    SETTINGS
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
        ),
        TimerBlock(
            type = TimerBlockType.EXPENSE_TRACKER,
            title = "Expense Tracker",
            icon = Icons.Default.AccountBalanceWallet,
            baseColor = Color(0xFFFFAB40) // Vibrant Orange
        ),
        TimerBlock(
            type = TimerBlockType.SUBSCRIPTION_TRACKER,
            title = "Subscription Tracker",
            icon = Icons.Default.Subscriptions,
            baseColor = Color(0xFF20C997) // Vibrant Teal
        ),
        TimerBlock(
            type = TimerBlockType.DAILY_PLANNER,
            title = "Daily Planner",
            icon = Icons.Default.EditCalendar,
            baseColor = Color(0xFF6C63FF) // Vibrant Indigo
        ),
        TimerBlock(
            type = TimerBlockType.SCREEN_TIMER,
            title = "Screen Timer",
            icon = Icons.Default.PhoneAndroid,
            baseColor = Color(0xFF00BCD4) // Vibrant Cyan
        ),
        TimerBlock(
            type = TimerBlockType.PROFILE,
            title = "Profile",
            icon = Icons.Default.Person,
            baseColor = Color(0xFFE91E63) // Vibrant Pink
        ),
        TimerBlock(
            type = TimerBlockType.SETTINGS,
            title = "Settings",
            icon = Icons.Default.Settings,
            baseColor = Color(0xFF607D8B) // Vibrant Blue Grey
        )
    )
}
