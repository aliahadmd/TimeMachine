package me.aliahad.timemanager

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.aliahad.timemanager.data.CompletionType
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitCompletion
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HabitSubmissionDialog(
    habit: Habit,
    existingCompletion: HabitCompletion?,
    onDismiss: () -> Unit,
    onSubmit: (CompletionType) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Icon and title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(habit.color))
                    ) {
                        Icon(
                            imageVector = getIconByName(habit.iconName),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = today,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Show current status if exists
                if (existingCompletion != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (existingCompletion.completionType) {
                            CompletionType.ACHIEVED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            CompletionType.GAVE_UP -> Color(0xFFE57373).copy(alpha = 0.1f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (existingCompletion.completionType) {
                                    CompletionType.ACHIEVED -> Icons.Default.CheckCircle
                                    CompletionType.GAVE_UP -> Icons.Default.Cancel
                                },
                                contentDescription = null,
                                tint = when (existingCompletion.completionType) {
                                    CompletionType.ACHIEVED -> Color(0xFF4CAF50)
                                    CompletionType.GAVE_UP -> Color(0xFFE57373)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Text(
                                text = when (existingCompletion.completionType) {
                                    CompletionType.ACHIEVED -> "You achieved your goal!"
                                    CompletionType.GAVE_UP -> "You gave up today"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = when (existingCompletion.completionType) {
                                    CompletionType.ACHIEVED -> Color(0xFF4CAF50)
                                    CompletionType.GAVE_UP -> Color(0xFFE57373)
                                }
                            )
                        }
                    }
                }
                
                Text(
                    text = if (existingCompletion != null) "Change today's entry?" else "How did it go today?",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Achieve button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        onClick = {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            onSubmit(CompletionType.ACHIEVED)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Achieved",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF4CAF50),
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Goal\n✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Give up button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        onClick = {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            onSubmit(CompletionType.GAVE_UP)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE57373).copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, Color(0xFFE57373).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFE57373)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Gave Up",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFE57373),
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Today\n✗",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE57373).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Remove button if exists
                if (existingCompletion != null) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Entry")
                    }
                }
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

