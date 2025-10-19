package me.aliahad.timemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.data.TimeSession
import me.aliahad.timemanager.ui.theme.TimeManagerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler

/**
 * True Immersive Fullscreen Timer Activity
 * - Forces landscape orientation
 * - Hides all system UI (status bar, nav bar)
 * - Prevents accidental exits
 * - Persists timer state
 */
class ImmersiveTimerActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_CATEGORY_ID = "category_id"
        private const val EXTRA_CATEGORY_NAME = "category_name"
        private const val EXTRA_CATEGORY_ICON = "category_icon"
        private const val EXTRA_CATEGORY_COLOR = "category_color"
        private const val EXTRA_ELAPSED_SECONDS = "elapsed_seconds"
        private const val EXTRA_SESSION_START = "session_start"
        
        private const val PREF_NAME = "immersive_timer_prefs"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_IS_PAUSED = "is_paused"
        private const val KEY_ELAPSED_SECONDS = "elapsed_seconds"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_CATEGORY_ID = "category_id"
        private const val KEY_CATEGORY_NAME = "category_name"
        private const val KEY_CATEGORY_ICON = "category_icon"
        private const val KEY_CATEGORY_COLOR = "category_color"
        
        fun start(
            context: Context,
            categoryId: Long,
            categoryName: String,
            categoryIcon: String,
            categoryColor: Long,
            elapsedSeconds: Int = 0,
            sessionStart: Long = System.currentTimeMillis()
        ) {
            val intent = Intent(context, ImmersiveTimerActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY_ID, categoryId)
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_CATEGORY_ICON, categoryIcon)
                putExtra(EXTRA_CATEGORY_COLOR, categoryColor)
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
                putExtra(EXTRA_SESSION_START, sessionStart)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        // Enable immersive mode
        setupImmersiveMode()
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Get intent extras or restore from preferences
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, 
            prefs.getLong(KEY_CATEGORY_ID, 0L))
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) 
            ?: prefs.getString(KEY_CATEGORY_NAME, "Focus")!!
        val categoryIcon = intent.getStringExtra(EXTRA_CATEGORY_ICON) 
            ?: prefs.getString(KEY_CATEGORY_ICON, "⏱️")!!
        val categoryColor = intent.getLongExtra(EXTRA_CATEGORY_COLOR, 
            prefs.getLong(KEY_CATEGORY_COLOR, 0xFF4DABF7))
        val elapsedSeconds = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, 
            prefs.getInt(KEY_ELAPSED_SECONDS, 0))
        val sessionStart = intent.getLongExtra(EXTRA_SESSION_START, 
            prefs.getLong(KEY_SESSION_START, System.currentTimeMillis()))
        
        // Save to preferences
        prefs.edit().apply {
            putLong(KEY_CATEGORY_ID, categoryId)
            putString(KEY_CATEGORY_NAME, categoryName)
            putString(KEY_CATEGORY_ICON, categoryIcon)
            putLong(KEY_CATEGORY_COLOR, categoryColor)
            putInt(KEY_ELAPSED_SECONDS, elapsedSeconds)
            putLong(KEY_SESSION_START, sessionStart)
            putBoolean(KEY_IS_RUNNING, true)
            putBoolean(KEY_IS_PAUSED, false)
            apply()
        }
        
        setContent {
            TimeManagerTheme {
                ImmersiveTimerScreen(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    categoryColor = categoryColor,
                    initialElapsedSeconds = elapsedSeconds,
                    sessionStart = sessionStart,
                    onExit = { elapsedSeconds, isRunning ->
                        if (isRunning) {
                            // Update preferences for restoration
                            prefs.edit().apply {
                                putInt(KEY_ELAPSED_SECONDS, elapsedSeconds)
                                putBoolean(KEY_IS_RUNNING, true)
                                apply()
                            }
                        } else {
                            // Clear preferences
                            prefs.edit().clear().apply()
                        }
                        finish()
                    }
                )
            }
        }
    }
    
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    override fun onResume() {
        super.onResume()
        setupImmersiveMode()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveMode()
        }
    }
    
    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}

@Composable
fun ImmersiveTimerScreen(
    categoryId: Long,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    initialElapsedSeconds: Int,
    sessionStart: Long,
    onExit: (elapsedSeconds: Int, isRunning: Boolean) -> Unit
) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var isRunning by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(initialElapsedSeconds) }
    var showControls by remember { mutableStateOf(false) }
    var showHintMessage by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    
    // Daily goal tracking
    var categoryInfo by remember { mutableStateOf<me.aliahad.timemanager.data.ActivityCategory?>(null) }
    var todayCompletedMinutes by remember { mutableIntStateOf(0) }
    
    // Shared function to save session and exit
    val saveSessionAndExit = remember {
        { exitNow: Boolean ->
            scope.launch {
                val finalElapsedSeconds = elapsedSeconds
                val finalCategoryId = categoryId
                val finalSessionStart = sessionStart
                
                android.util.Log.d("ImmersiveTimer", "💾 Saving session before exit (elapsedSeconds=$finalElapsedSeconds)")
                
                // Save session if >= 30 seconds
                if (finalElapsedSeconds >= 30) {
                    try {
                        val sessionId = withContext(Dispatchers.IO) {
                            val todayDate = getTodayDateString()
                            val durationMins = (finalElapsedSeconds / 60).coerceAtLeast(1)
                            val session = TimeSession(
                                categoryId = finalCategoryId,
                                startTime = finalSessionStart,
                                endTime = System.currentTimeMillis(),
                                durationMinutes = durationMins,
                                date = todayDate
                            )
                            android.util.Log.d("ImmersiveTimer", "Saving session: categoryId=$finalCategoryId, duration=${durationMins}min (${finalElapsedSeconds}s), date=$todayDate")
                            val id = database.timeSessionDao().insertSession(session)
                            android.util.Log.d("ImmersiveTimer", "Insert returned ID=$id")
                            
                            val totalToday = database.timeSessionDao().getTotalMinutesForDate(todayDate) ?: 0
                            val sessionCount = database.timeSessionDao().getSessionCountForDate(todayDate)
                            android.util.Log.d("ImmersiveTimer", "Verification: Total today=${totalToday}min, Count=$sessionCount sessions")
                            id
                        }
                        android.util.Log.d("ImmersiveTimer", "✅ Session committed successfully (ID: $sessionId)")
                    } catch (e: Exception) {
                        android.util.Log.e("ImmersiveTimer", "❌ Error saving session", e)
                        e.printStackTrace()
                    }
                } else {
                    android.util.Log.d("ImmersiveTimer", "⚠️ Session too short (${finalElapsedSeconds}s < 30s), not saving")
                }
                
                // Exit after ensuring save is complete
                if (exitNow) {
                    android.util.Log.d("ImmersiveTimer", "Exiting activity...")
                    delay(300)
                    onExit(finalElapsedSeconds, false)
                }
            }
        }
    }
    
    // Handle back press - save session and exit
    BackHandler(enabled = true) {
        android.util.Log.w("ImmersiveTimer", "⬅️ BACK GESTURE/BUTTON pressed - saving session!")
        saveSessionAndExit(true)
    }
    
    // Load category info and today's completed time
    LaunchedEffect(categoryId) {
        scope.launch(Dispatchers.IO) {
            val category = database.activityCategoryDao().getCategoryById(categoryId)
            val today = getTodayDateString()
            val sessions = database.timeSessionDao().getSessionsBetweenDatesSync(today, today)
            val completedMinutes = sessions
                .filter { it.categoryId == categoryId }
                .sumOf { it.durationMinutes }
            
            withContext(Dispatchers.Main) {
                categoryInfo = category
                todayCompletedMinutes = completedMinutes
            }
        }
    }
    
    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(showControls) {
        if (showControls && isRunning && !isPaused) {
            delay(5000)
            showControls = false
        }
    }
    
    // Auto-hide hint message after 3 seconds
    LaunchedEffect(showHintMessage) {
        if (showHintMessage) {
            delay(3000)
            showHintMessage = false
        }
    }
    
    // Timer ticker
    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused) {
            delay(1000)
            elapsedSeconds++
        }
    }
    
    // Tap anywhere to show/hide controls
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0D0D0D),
                        Color(0xFF000000)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        val timeSinceLastTap = currentTime - lastTapTime
                        
                        if (timeSinceLastTap < 500) {
                            // Double tap detected - toggle controls
                            showControls = !showControls
                            showHintMessage = false
                            lastTapTime = 0L // Reset to prevent triple tap issues
                        } else {
                            // Single tap - show hint message
                            if (!showControls) {
                                showHintMessage = true
                            }
                            lastTapTime = currentTime
                        }
                    }
                )
            }
    ) {
        // Main timer content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Category indicator
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    Text(
                        categoryIcon,
                        fontSize = 48.sp
                    )
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Giant timer display
            Text(
                formatImmersiveTimer(elapsedSeconds),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 140.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = if (isPaused) {
                    Color(0xFFFFA726)
                } else {
                    Color(categoryColor)
                },
                textAlign = TextAlign.Center
            )
            
            // Status text and session save indicator
            AnimatedVisibility(
                visible = showControls || isPaused || elapsedSeconds < 30,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        when {
                            isPaused -> "⏸ PAUSED"
                            !isRunning -> "Session Ended"
                            else -> formatImmersiveDuration(elapsedSeconds / 60)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = when {
                            isPaused -> Color(0xFFFFA726)
                            !isRunning -> Color(0xFF66BB6A)
                            else -> Color.White.copy(alpha = 0.6f)
                        },
                        fontWeight = if (isPaused) FontWeight.Bold else FontWeight.Normal
                    )
                    
                    // Minimum session length indicator
                    if (isRunning && !isPaused && elapsedSeconds < 30) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Keep going... ${30 - elapsedSeconds}s to save",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFA726),
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isRunning && !isPaused && elapsedSeconds >= 30) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "✓ Session will be saved",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF66BB6A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Daily Goal Progress Bar
            if (categoryInfo != null && categoryInfo!!.dailyGoalMinutes > 0) {
                val currentSessionMinutes = elapsedSeconds / 60
                val totalTodayMinutes = todayCompletedMinutes + currentSessionMinutes
                val goalMinutes = categoryInfo!!.dailyGoalMinutes
                val progress = (totalTodayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
                val isGoalReached = totalTodayMinutes >= goalMinutes
                
                Column(
                    modifier = Modifier
                        .width(500.dp)
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress bar
                    Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = progress > 0f,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animateFloatAsState(
                                            targetValue = progress,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            ),
                                            label = "progress"
                                        ).value)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = if (isGoalReached) {
                                                    listOf(
                                                        Color(0xFF4CAF50),
                                                        Color(0xFF66BB6A)
                                                    )
                                                } else {
                                                    listOf(
                                                        Color(categoryColor),
                                                        Color(categoryColor).copy(alpha = 0.7f)
                                                    )
                                                }
                                            )
                                        )
                                )
                            }
                        }
                        
                        // Progress text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isGoalReached) {
                                    "🎉 Goal reached!"
                                } else {
                                    "Daily Goal Progress"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isGoalReached) {
                                    Color(0xFF66BB6A)
                                } else {
                                    Color.White.copy(alpha = 0.7f)
                                },
                                fontWeight = if (isGoalReached) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$totalTodayMinutes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isGoalReached) {
                                        Color(0xFF66BB6A)
                                    } else {
                                        Color(categoryColor)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "/ $goalMinutes min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    "(${(progress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                        
                        // Time remaining (if not reached)
                        if (!isGoalReached) {
                            val remainingMinutes = goalMinutes - totalTodayMinutes
                            if (remainingMinutes > 0) {
                                Text(
                                    "${remainingMinutes} min remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        
        // Control buttons (bottom)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit button (saves session and exits)
                FloatingActionButton(
                    onClick = { 
                        android.util.Log.w("ImmersiveTimer", "❌ EXIT BUTTON pressed - saving session and exiting!")
                        saveSessionAndExit(true)
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Exit",
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Pause/Resume button (large)
                FloatingActionButton(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier.size(96.dp),
                    containerColor = if (isPaused) {
                        Color(0xFF66BB6A)
                    } else {
                        Color(0xFFFFA726)
                    },
                    contentColor = Color.White
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Stop button (RED - saves session)
                FloatingActionButton(
                    onClick = {
                        android.util.Log.w("ImmersiveTimer", "🔴 STOP BUTTON pressed - saving session and exiting!")
                        isRunning = false
                        isPaused = false
                        saveSessionAndExit(true)
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = Color(0xFFEF5350),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        
        // Tap hint (shows for 3 seconds after single tap)
        AnimatedVisibility(
            visible = showHintMessage && !showControls && isRunning,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Text(
                    "Double tap to show controls",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatImmersiveTimer(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

private fun formatImmersiveDuration(minutes: Int): String {
    if (minutes == 0) return "Just started"
    
    val hours = minutes / 60
    val mins = minutes % 60
    
    return when {
        hours == 0 -> "$mins min"
        mins == 0 -> "$hours hr"
        else -> "$hours hr $mins min"
    }
}

