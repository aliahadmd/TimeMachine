package me.aliahad.timemanager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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

@Composable
fun HomeScreen(
    onBlockClick: (TimerBlockType) -> Unit
) {
    val context = LocalContext.current
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    var serviceBound by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var areNotificationsConfigured by remember { mutableStateOf(false) }
    var needsExactAlarmPermission by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var dismissedExactAlarmBanner by remember { mutableStateOf(false) }
    
    // Check notification settings whenever app comes to foreground
    DisposableEffect(Unit) {
        val checkSettings = {
            val configured = NotificationSettingsHelper.areNotificationChannelsConfigured(context)
            areNotificationsConfigured = configured
            if (!configured && !showNotificationDialog) {
                // Only show dialog on first launch or if user hasn't seen it this session
                val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
                val hasSeenDialog = prefs.getBoolean("has_seen_dialog", false)
                if (!hasSeenDialog) {
                    showNotificationDialog = true
                }
            }

            val needsExact = ExactAlarmPermissionManager.needsExactAlarm(context)
            needsExactAlarmPermission = needsExact
            if (needsExact) {
                val prefs = context.getSharedPreferences("notification_setup", Context.MODE_PRIVATE)
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
    
    // Service connection
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as TimerService.TimerBinder
                timerService = binder.getService()
                serviceBound = true
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
                serviceBound = false
            }
        }
    }
    
    // Bind to service
    DisposableEffect(Unit) {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        onDispose {
            if (serviceBound) {
                context.unbindService(serviceConnection)
            }
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
            if (!areNotificationsConfigured) {
                NotificationSetupBanner(
                    onOpenSettings = {
                        NotificationSettingsHelper.openNotificationSettings(context)
                    },
                    onDismiss = {
                        // User can dismiss banner, will re-check on next app open
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
                            onClick = { onBlockClick(block.type) },
                            timerService = timerService
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
    onClick: () -> Unit,
    timerService: TimerService?
) {
    val context = LocalContext.current
    val isRunning by timerService?.isRunning?.collectAsState() ?: remember { mutableStateOf(false) }
    val remainingSeconds by timerService?.remainingSeconds?.collectAsState() ?: remember { mutableIntStateOf(0) }
    val isAlarmRinging by timerService?.isAlarmRinging?.collectAsState() ?: remember { mutableStateOf(false) }
    
    // For Habit Tracker, get habit count
    var habitCount by remember { mutableIntStateOf(0) }
    
    if (block.type == TimerBlockType.HABIT_TRACKER) {
        val database = remember { me.aliahad.timemanager.data.TimerDatabase.getDatabase(context) }
        val habits by database.habitDao().getAllActiveHabits().collectAsState(initial = emptyList())
        habitCount = habits.size
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAlarmRinging && block.type == TimerBlockType.FOCUS_TIMER -> 
                    MaterialTheme.colorScheme.errorContainer
                isRunning && block.type == TimerBlockType.FOCUS_TIMER -> 
                    MaterialTheme.colorScheme.primaryContainer
                else -> block.baseColor.copy(alpha = 0.15f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
            
                // Icon
                Surface(
                    modifier = Modifier
                        .size(64.dp),
                    shape = CircleShape,
                    color = when {
                        isAlarmRinging && block.type == TimerBlockType.FOCUS_TIMER -> 
                            MaterialTheme.colorScheme.error
                        isRunning && block.type == TimerBlockType.FOCUS_TIMER -> 
                            MaterialTheme.colorScheme.primary
                        else -> block.baseColor.copy(alpha = 0.3f)
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = block.icon,
                            contentDescription = block.title,
                            modifier = Modifier.size(32.dp),
                            tint = when {
                                isAlarmRinging && block.type == TimerBlockType.FOCUS_TIMER -> 
                                    MaterialTheme.colorScheme.onError
                                isRunning && block.type == TimerBlockType.FOCUS_TIMER -> 
                                    MaterialTheme.colorScheme.onPrimary
                                else -> block.baseColor
                            }
                        )
                    }
                }
            
            // Timer display or habit count
            if (block.type == TimerBlockType.FOCUS_TIMER && (isRunning || isAlarmRinging)) {
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val seconds = remainingSeconds % 60
                
                val timeText = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }
                
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = when {
                        isAlarmRinging -> MaterialTheme.colorScheme.error
                        isRunning -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
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
    HABIT_TRACKER
}

// Get available timer blocks with varied colors
fun getTimerBlocks(): List<TimerBlock> {
    return listOf(
        TimerBlock(
            type = TimerBlockType.FOCUS_TIMER,
            title = "Focus Timer",
            icon = Icons.Default.Timer,
            baseColor = Color(0xFFE57373) // Soft Red
        ),
        TimerBlock(
            type = TimerBlockType.HABIT_TRACKER,
            title = "Habit Tracker",
            icon = Icons.Default.CheckCircle,
            baseColor = Color(0xFF81C784) // Soft Green
        )
    )
}
