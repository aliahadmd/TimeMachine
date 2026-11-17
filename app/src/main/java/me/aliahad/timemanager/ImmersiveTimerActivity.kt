package me.aliahad.timemanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.focus.FocusTimerService
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.theme.TimeManagerTheme

class ImmersiveTimerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        val app = application as? TimeManagerApp
        if (app != null) {
            lifecycleScope.launch {
                FocusTimerService.restoreIfRunning(
                    this@ImmersiveTimerActivity,
                    app.container.preferencesRepository
                )
            }
        }

        setContent {
            TimeManagerTheme {
                val state by FocusTimerService.timerState.collectAsStateWithLifecycle()

                LaunchedEffect(state.isRunning) {
                    if (!state.isRunning && state.lastSavedResult != null) {
                        finish()
                    }
                }

                val accent = Color(state.categoryColor.toInt())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0B0E))
                        .padding(24.dp)
                ) {
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color.White.copy(alpha = 0.12f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                val sweep = if (state.isCountdown) state.progressFraction * 360f else 360f
                                drawArc(
                                    color = accent,
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = state.categoryIcon,
                                fontSize = 54.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(36.dp))

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = when {
                                    state.isBreakPhase -> "BREAK PHASE"
                                    state.isPaused -> "${state.categoryName.uppercase()} • PAUSED"
                                    else -> state.categoryName.uppercase()
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = accent,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = Formatters.formatTimerClock(state.displaySeconds),
                                fontSize = 74.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = TabularNumberStyle
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.isRunning) {
                                    Button(
                                        onClick = {
                                            FocusTimerService.sendAction(
                                                this@ImmersiveTimerActivity,
                                                if (state.isPaused) FocusTimerService.ACTION_RESUME
                                                else FocusTimerService.ACTION_PAUSE
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                                    ) {
                                        Icon(
                                            if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (state.isPaused) "Resume" else "Pause")
                                    }

                                    if (state.isCountdown && !state.isBreakPhase) {
                                        OutlinedButton(
                                            onClick = {
                                                FocusTimerService.sendAction(
                                                    this@ImmersiveTimerActivity,
                                                    FocusTimerService.ACTION_ADD_FIVE_MIN
                                                )
                                            }
                                        ) {
                                            Text("+5m", color = Color.White)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            FocusTimerService.sendAction(
                                                this@ImmersiveTimerActivity,
                                                if (state.isBreakPhase) FocusTimerService.ACTION_SKIP_BREAK
                                                else FocusTimerService.ACTION_STOP_AND_SAVE
                                            )
                                            finish()
                                        }
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (state.isBreakPhase) "Skip" else "Finish", color = Color.White)
                                    }
                                } else {
                                    Button(onClick = { finish() }) {
                                        Text("Return to App")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
