package me.aliahad.timemanager

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fullscreen Focus Timer - Beautiful desk-friendly UI
 * Shows large timer with pause/resume controls
 * Optimized for landscape orientation
 */
@Composable
fun FullscreenTimerView(
    elapsedSeconds: Int,
    isPaused: Boolean,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Color,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onExitFullscreen: () -> Unit
) {
    val context = LocalContext.current
    
    // Keep screen on while in fullscreen mode
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    // Handle back button
    BackHandler {
        onExitFullscreen()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117)
                    )
                )
            )
    ) {
        // Exit fullscreen button (top-left)
        IconButton(
            onClick = onExitFullscreen,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Exit Fullscreen",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Category badge
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = categoryColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        categoryIcon,
                        fontSize = 32.sp
                    )
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Giant timer display
            Text(
                formatFullscreenTimer(elapsedSeconds),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = if (isPaused) {
                    Color(0xFFFFA726) // Orange when paused
                } else {
                    categoryColor
                }
            )
            
            // Duration text
            Text(
                formatFullscreenDuration(elapsedSeconds / 60),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // Pause indicator
            if (isPaused) {
                Text(
                    "⏸ PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFA726),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop button
                FilledTonalButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFEF5350).copy(alpha = 0.3f),
                        contentColor = Color(0xFFEF5350)
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Pause/Resume button (large)
                FloatingActionButton(
                    onClick = if (isPaused) onResume else onPause,
                    modifier = Modifier.size(100.dp),
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
                        modifier = Modifier.size(50.dp)
                    )
                }
                
                // Info button
                FilledTonalButton(
                    onClick = onExitFullscreen,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Details",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            // Button labels
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Stop",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (isPaused) "Resume" else "Pause",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Details",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Bottom info bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoItem(
                    icon = Icons.Default.Timer,
                    label = "Elapsed",
                    value = formatFullscreenTimer(elapsedSeconds)
                )
                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Status",
                    value = if (isPaused) "Paused" else "Running"
                )
                InfoItem(
                    icon = if (isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = "Mode",
                    value = if (isPaused) "Hold" else "Active"
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// Formatting functions for fullscreen timer
private fun formatFullscreenTimer(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

private fun formatFullscreenDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    
    return when {
        hours == 0 -> "$mins minutes"
        mins == 0 -> "$hours hours"
        else -> "$hours hours $mins minutes"
    }
}

