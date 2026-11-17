package me.aliahad.timemanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

@Composable
fun TutorialModal(
    visible: Boolean,
    onRequestNotifications: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                emoji = "🧭",
                title = "Your Day in Five Pillars",
                description = "TimeMachine brings Today's Command Center, Planner blocks, Focus sessions, Finance & Budgets, Habits & Screen Time together."
            ),
            OnboardingPage(
                emoji = "⏱️",
                title = "Live Focus & Connected Habits",
                description = "Run Focus or Pomodoro timers in background or Desk Landscape mode. Completing a Focus session can auto-complete linked Planner blocks and Habits."
            ),
            OnboardingPage(
                emoji = "🔒",
                title = "Private, Local-First & Reliable",
                description = "All your data stays on your device with JSON Backup & Restore anytime. Enable notifications to get timely Habit, Planner & Renewal reminders."
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Skip")
                        }
                    }

                    Text(text = pages[currentPage].emoji, fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = pages[currentPage].title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = pages[currentPage].description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pages.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == currentPage) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == currentPage) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    if (currentPage == pages.lastIndex) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onRequestNotifications()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Allow Reminder Notifications")
                            }
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Get Started")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (currentPage > 0) {
                                OutlinedButton(onClick = { currentPage-- }) {
                                    Text("Back")
                                }
                            } else {
                                Spacer(modifier = Modifier.size(1.dp))
                            }
                            Button(onClick = { currentPage++ }) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}
