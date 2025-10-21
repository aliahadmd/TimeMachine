package me.aliahad.timemanager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

data class TutorialPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val tutorialPages = listOf(
    TutorialPage(
        title = "Focus Tracker",
        description = "Run distraction-free focus sessions and review your productivity streaks.",
        icon = Icons.Default.Timer
    ),
    TutorialPage(
        title = "Habit Tracker",
        description = "Create healthy routines, schedule reminders, and visualize habit streaks.",
        icon = Icons.Default.CheckCircle
    ),
    TutorialPage(
        title = "Year Calculator",
        description = "Measure important milestones and countdowns with smart date calculations.",
        icon = Icons.Default.CalendarMonth
    ),
    TutorialPage(
        title = "BMI Calculator",
        description = "Track BMI records and get coaching insights tailored to your progress.",
        icon = Icons.Default.FitnessCenter
    ),
    TutorialPage(
        title = "Expense Tracker",
        description = "Log expenses, monitor budgets, and understand where your money goes.",
        icon = Icons.Default.AccountBalanceWallet
    ),
    TutorialPage(
        title = "Subscription Tracker",
        description = "Stay ahead of renewals and tame recurring costs with timely reminders.",
        icon = Icons.Default.Analytics
    ),
    TutorialPage(
        title = "Daily Planner",
        description = "Plan your day with smart tasks, reminders, and a clear schedule view.",
        icon = Icons.Default.EditCalendar
    ),
    TutorialPage(
        title = "Screen Timer",
        description = "Understand digital wellbeing trends using accurate, system-sourced screen time analytics.",
        icon = Icons.Default.Devices
    ),
    TutorialPage(
        title = "Profile & Insights",
        description = "Review achievements, usage stats, and personalize TimeMachine to fit your workflow.",
        icon = Icons.Default.Person
    ),
    TutorialPage(
        title = "Settings",
        description = "Secure backups, manage permissions, and fine-tune notifications with ease.",
        icon = Icons.Default.Settings
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialModal(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
            val scope = rememberCoroutineScope()
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to TimeMachine",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalPager(state = pagerState) { page ->
                    val tutorial = tutorialPages[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            tutorial.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            tutorial.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            tutorial.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(tutorialPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                    ) {
                        Text("Restart guide")
                    }
                    Button(
                        onClick = {
                            if (pagerState.currentPage < tutorialPages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Text(if (pagerState.currentPage < tutorialPages.lastIndex) "Next" else "Let's Go")
                    }
                }
            }
        }
    }
}
