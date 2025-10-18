package me.aliahad.timemanager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(25) }
    var isRunning by remember { mutableStateOf(false) }
    var isAlarmRinging by remember { mutableStateOf(false) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var vibrator by remember { mutableStateOf<Vibrator?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRunning) {
        if (isRunning && remainingSeconds > 0) {
            while (remainingSeconds > 0 && isRunning) {
                delay(1000)
                remainingSeconds--
            }
            if (remainingSeconds == 0 && isRunning) {
                // Timer finished - play alarm and vibrate
                isAlarmRinging = true
                isRunning = false
                vibrator = playAlarmAndVibrate(context)
                mediaPlayer = createMediaPlayer(context)
                mediaPlayer?.start()
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Timer",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(60.dp))

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
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .widthIn(min = 160.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Dismiss",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isRunning -> {
                        // Stop button
                        Button(
                            onClick = {
                                isRunning = false
                                remainingSeconds = 0
                            },
                            modifier = Modifier
                                .size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Stop",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    else -> {
                        // Start button
                        Button(
                            onClick = {
                                val total = hours * 3600 + minutes * 60
                                if (total > 0) {
                                    totalSeconds = total
                                    remainingSeconds = total
                                    isRunning = true
                                }
                            },
                            modifier = Modifier
                                .size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = hours > 0 || minutes > 0
                        ) {
                            Text(
                                text = "Start",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            vibrator?.cancel()
        }
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
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = value
    )
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val index = listState.firstVisibleItemIndex +
                    if (listState.firstVisibleItemScrollOffset > 30) 1 else 0
            onValueChange(index.coerceIn(range))
        }
    }

    Box(
        modifier = modifier
            .height(200.dp)
            .width(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator
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
            modifier = Modifier.fillMaxSize()
        ) {
            items(range.count()) { index ->
                val itemValue = range.first + index
                val isSelected = itemValue == value
                
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.3f,
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
                                fontSize = if (isSelected) 42.sp else 36.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.alpha(alpha),
                            textAlign = TextAlign.Center
                        )
                        if (isSelected) {
                            Text(
                                text = " $label",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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

