package me.aliahad.timemanager

import android.app.Activity
import android.content.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.Preset
import me.aliahad.timemanager.data.PresetRepository
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.permissions.ExactAlarmPermissionManager

@Composable
fun TimerScreen(
    onBackPress: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val repository = remember { PresetRepository(database.presetDao()) }
    val presets by repository.allPresets.collectAsState(initial = emptyList())
    
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(25) }
    var isAlarmRinging by remember { mutableStateOf(false) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var vibrator by remember { mutableStateOf<Vibrator?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Service binding
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    var serviceBound by remember { mutableStateOf(false) }
    
    // Service state - use mutableStateOf and update via LaunchedEffect
    var isRunning by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var serviceAlarmRinging by remember { mutableStateOf(false) }
    
    // Update states when service connects or changes
    LaunchedEffect(timerService) {
        timerService?.let { service ->
            launch {
                service.isRunning.collect { isRunning = it }
            }
            launch {
                service.remainingSeconds.collect { remainingSeconds = it }
            }
            launch {
                service.totalSeconds.collect { serviceTotalSeconds ->
                    if (serviceTotalSeconds > 0) {
                        totalSeconds = serviceTotalSeconds
                    }
                }
            }
            launch {
                service.isAlarmRinging.collect { serviceAlarmRinging = it }
            }
        }
    }
    
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
    
    // Sync local alarm state with service state
    LaunchedEffect(serviceAlarmRinging) {
        if (serviceAlarmRinging && !isAlarmRinging) {
            isAlarmRinging = true
        }
    }
    
    // Timer completion receiver
    val timerCompleteReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isAlarmRinging = true
                vibrator = playAlarmAndVibrate(context!!)
                mediaPlayer = createMediaPlayer(context)
                mediaPlayer?.start()
            }
        }
    }
    
    DisposableEffect(Unit) {
        // Bind to service
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        // Register broadcast receiver
        val filter = IntentFilter("me.aliahad.timemanager.TIMER_COMPLETE")
        context.registerReceiver(timerCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        onDispose {
            context.unbindService(serviceConnection)
            try {
                context.unregisterReceiver(timerCompleteReceiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
            mediaPlayer?.release()
            vibrator?.cancel()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button if navigation is available
                if (onBackPress != null) {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Text(
                    text = "Focus Timer",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                IconButton(
                    onClick = { showSaveDialog = true },
                    enabled = !isRunning && (hours > 0 || minutes > 0)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save Preset",
                        tint = if (!isRunning && (hours > 0 || minutes > 0)) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            // Preset list - horizontal scroll
            if (presets.isNotEmpty()) {
                PresetList(
                    presets = presets,
                    onPresetClick = { preset ->
                        if (!isRunning && !isAlarmRinging) {
                            // Set the time
                            hours = preset.hours
                            minutes = preset.minutes
                            
                            // Start the timer immediately via service
                            val total = preset.hours * 3600 + preset.minutes * 60
                            if (total > 0) {
                                if (ExactAlarmPermissionManager.needsExactAlarm(context)) {
                                    showExactAlarmDialog = true
                                } else {
                                    totalSeconds = total
                                    val intent = Intent(context, TimerService::class.java).apply {
                                        action = TimerService.ACTION_START_TIMER
                                        putExtra(TimerService.EXTRA_DURATION_SECONDS, total)
                                    }
                                    context.startForegroundService(intent)
                                }
                            }
                        }
                    },
                    onDeletePreset = { preset ->
                        scope.launch {
                            repository.deletePreset(preset)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = when {
                    isAlarmRinging -> 2
                    isRunning -> 1
                    else -> 0
                },
                label = "timer_state"
            ) { state ->
                when (state) {
                    1 -> {
                        // Show countdown timer
                        TimerDisplay(
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    2 -> {
                        // Show alarm notification
                        AlarmRingingDisplay(
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        // Show time picker
                        TimePickerWheel(
                            hours = hours,
                            minutes = minutes,
                            onHoursChange = { hours = it },
                            onMinutesChange = { minutes = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                when {
                    isAlarmRinging -> {
                        // Dismiss alarm button
                        Button(
                            onClick = {
                                isAlarmRinging = false
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                vibrator?.cancel()
                                vibrator = null
                                
                                // Dismiss alarm from service too
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_DISMISS_ALARM
                                }
                                context.startService(intent)
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .widthIn(min = 160.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Dismiss",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                    isRunning -> {
                        // Stop button
                        Button(
                            onClick = {
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_STOP_TIMER
                                }
                                context.startService(intent)
                            },
                            modifier = Modifier
                                .size(90.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Stop",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                    else -> {
                        // Start button
                        Button(
                            onClick = {
                                val total = hours * 3600 + minutes * 60
                                if (total > 0) {
                                    if (ExactAlarmPermissionManager.needsExactAlarm(context)) {
                                        showExactAlarmDialog = true
                                    } else {
                                        totalSeconds = total
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_START_TIMER
                                            putExtra(TimerService.EXTRA_DURATION_SECONDS, total)
                                        }
                                        context.startForegroundService(intent)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(90.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = hours > 0 || minutes > 0,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Start",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        SavePresetDialog(
            hours = hours,
            minutes = minutes,
            onDismiss = { showSaveDialog = false },
            onSave = { presetName ->
                scope.launch {
                    repository.insertPreset(
                        Preset(
                            name = presetName,
                            hours = hours,
                            minutes = minutes
                        )
                    )
                    showSaveDialog = false
                }
            }
        )
    }

    if (showExactAlarmDialog && ExactAlarmPermissionManager.needsExactAlarm(context)) {
        val activity = context as? Activity
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

@Composable
fun AlarmRingingDisplay(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⏰",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 100.sp
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "Time's Up!",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your timer has finished",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            // Progress circle
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Time display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hours > 0) {
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TimePickerWheel(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Set Duration",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours picker
            NumberPicker(
                value = hours,
                range = 0..23,
                onValueChange = onHoursChange,
                label = "hr",
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minutes picker
            NumberPicker(
                value = minutes,
                range = 0..59,
                onValueChange = onMinutesChange,
                label = "min",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    var lastVibratedValue by remember { mutableIntStateOf(value) }
    
    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Scroll to initial value on first composition
    LaunchedEffect(Unit) {
        listState.scrollToItem(value)
    }

    // Derive the current center value from scroll state
    val currentCenterValue by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            
            layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                ?.index?.coerceIn(range) ?: value
        }
    }
    
    // Handle value changes and haptic feedback
    LaunchedEffect(currentCenterValue, listState.isScrollInProgress) {
        if (currentCenterValue != lastVibratedValue) {
            if (listState.isScrollInProgress) {
                // Haptic feedback during scroll
                vibrator.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            lastVibratedValue = currentCenterValue
        }
        
        if (!listState.isScrollInProgress && currentCenterValue != value) {
            // Update value when scroll stops
            onValueChange(currentCenterValue)
        }
    }

    Box(
        modifier = modifier
            .height(200.dp)
            .width(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator - positioned at center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 70.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(range.count()) { index ->
                val itemValue = range.first + index
                val isSelected = itemValue == currentCenterValue
                
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.4f,
                    animationSpec = tween(200),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format("%02d", itemValue),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = if (isSelected) 42.sp else 34.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.alpha(alpha),
                            textAlign = TextAlign.Center
                        )
                        if (isSelected) {
                            Text(
                                text = " $label",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun playAlarmAndVibrate(context: Context): Vibrator {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
    return vibrator
}

private fun createMediaPlayer(context: Context): MediaPlayer? {
    return try {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        MediaPlayer().apply {
            setDataSource(context, alarmUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            isLooping = true
            prepare()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun PresetList(
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onDeletePreset: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetChip(
                preset = preset,
                onClick = { onPresetClick(preset) },
                onDelete = { onDeletePreset(preset) }
            )
        }
    }
}

@Composable
fun PresetChip(
    preset: Preset,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buildString {
                        if (preset.hours > 0) append("${preset.hours}h ")
                        append("${preset.minutes}m")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SavePresetDialog(
    hours: Int,
    minutes: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Preset",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Duration: ${if (hours > 0) "${hours}h " else ""}${minutes}min",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g., Pomodoro, Break") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (presetName.isNotBlank()) {
                        onSave(presetName.trim())
                    }
                },
                enabled = presetName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
