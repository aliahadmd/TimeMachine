package me.aliahad.timemanager

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Main Screen with Tabs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onBackPress: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("⏱️ Track", "📊 Stats", "📁 Categories")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> TrackingTab(database)
                1 -> StatsTab(database)
                2 -> CategoriesTab(database)
            }
        }
    }
}

// TRACKING TAB - With immersive fullscreen mode
@Composable
fun TrackingTab(database: TimerDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories by database.activityCategoryDao().getAllActiveCategories().collectAsState(initial = emptyList())
    
    var selectedCategory by remember { mutableStateOf<ActivityCategory?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // For forcing UI refresh
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CategorySelectionCard(
                categories = categories,
                selectedCategory = selectedCategory,
                isRunning = false,
                onSelectCategory = { selectedCategory = it }
            )
        }
        
        item {
            StartTimerCard(
                category = selectedCategory,
                onStart = {
                    if (selectedCategory != null) {
                        // Launch immersive timer activity
                        ImmersiveTimerActivity.start(
                            context = context,
                            categoryId = selectedCategory!!.id,
                            categoryName = selectedCategory!!.name,
                            categoryIcon = selectedCategory!!.icon,
                            categoryColor = selectedCategory!!.color
                        )
                    }
                }
            )
        }
        
        item {
            key(refreshTrigger) {
                TodaySummaryCard(database, selectedCategory)
            }
        }
        
        item {
            key(refreshTrigger) {
                RecentSessionsCard(database)
            }
        }
    }
}

@Composable
fun CategorySelectionCard(
    categories: List<ActivityCategory>,
    selectedCategory: ActivityCategory?,
    isRunning: Boolean,
    onSelectCategory: (ActivityCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning && selectedCategory != null) {
                Color(selectedCategory.color).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isRunning) "Tracking..." else "Select Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedCategory != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(selectedCategory.color).copy(alpha = 0.2f)
                    ) {
                        Text(
                            "${selectedCategory.icon} ${selectedCategory.name}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(selectedCategory.color)
                        )
                    }
                }
            }
            
            if (isRunning) {
                Text(
                    "⏱️ Timer is running. Stop to change category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (categories.isEmpty()) {
                Text(
                    "No categories yet. Add one in the Categories tab!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(minOf(categories.size * 40 + 40, 240).dp)
                ) {
                    items(categories) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { onSelectCategory(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: ActivityCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Color(category.color).copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val borderColor = if (isSelected) {
        Color(category.color)
    } else {
        Color.Transparent
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(category.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StartTimerCard(
    category: ActivityCategory?,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category != null) {
                Color(category.color).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (category != null) {
                        Color(category.color)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    "Immersive Focus Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    "Enter true fullscreen mode with landscape orientation.\nPerfect for desk placement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Start Button
            Button(
                onClick = onStart,
                enabled = category != null,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (category != null) {
                        Color(category.color)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (category != null) "Start Focus Session" else "Select a Category First",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Features list
            if (category != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureRow("🔒 Landscape mode locked")
                    FeatureRow("📱 System UI hidden")
                    FeatureRow("⏸️ Pause anytime")
                    FeatureRow("💡 Auto-save on completion")
                }
            }
        }
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatTimerDisplay(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

@Composable
fun TodaySummaryCard(database: TimerDatabase, selectedCategory: ActivityCategory?) {
    val scope = rememberCoroutineScope()
    var todayTotal by remember { mutableIntStateOf(0) }
    var categoryToday by remember { mutableIntStateOf(0) }
    var sessionCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(selectedCategory) {
        scope.launch(Dispatchers.IO) {
            val today = getTodayDateString()
            val total = database.timeSessionDao().getTotalMinutesForDate(today) ?: 0
            val count = database.timeSessionDao().getSessionCountForDate(today)
            
            withContext(Dispatchers.Main) {
                todayTotal = total
                sessionCount = count
            }
            
            if (selectedCategory != null) {
                val sessions = database.timeSessionDao().getSessionsBetweenDatesSync(today, today)
                val catTotal = sessions.filter { it.categoryId == selectedCategory.id }
                    .sumOf { it.durationMinutes }
                
                withContext(Dispatchers.Main) {
                    categoryToday = catTotal
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📅 Today's Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = formatDuration(todayTotal),
                    label = "Total Time",
                    color = MaterialTheme.colorScheme.primary
                )
                StatColumn(
                    value = sessionCount.toString(),
                    label = "Sessions",
                    color = MaterialTheme.colorScheme.secondary
                )
                if (selectedCategory != null) {
                    StatColumn(
                        value = formatDuration(categoryToday),
                        label = selectedCategory.name,
                        color = Color(selectedCategory.color)
                    )
                }
            }
            
            // Goal Progress
            if (selectedCategory != null && selectedCategory.dailyGoalMinutes > 0) {
                val progress = (categoryToday.toFloat() / selectedCategory.dailyGoalMinutes).coerceIn(0f, 1f)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Daily Goal Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(selectedCategory.color)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(selectedCategory.color),
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentSessionsCard(database: TimerDatabase) {
    val scope = rememberCoroutineScope()
    var recentSessions by remember { mutableStateOf<List<Pair<TimeSession, ActivityCategory>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val today = getTodayDateString()
            val sessions = database.timeSessionDao().getSessionsBetweenDatesSync(today, today)
            val sessionsWithCategories = sessions.mapNotNull { session ->
                val category = database.activityCategoryDao().getCategoryById(session.categoryId)
                if (category != null) session to category else null
            }
            
            withContext(Dispatchers.Main) {
                recentSessions = sessionsWithCategories
            }
        }
    }
    
    if (recentSessions.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Today's Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                recentSessions.take(5).forEach { (session, category) ->
                    SessionRow(session, category)
                }
            }
        }
    }
}

@Composable
fun SessionRow(session: TimeSession, category: ActivityCategory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(category.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatTime(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            formatDuration(session.durationMinutes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(category.color)
        )
    }
}

fun formatTime(timestamp: Long): String {
    val time = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp),
        java.time.ZoneId.systemDefault()
    )
    return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}

// STATS TAB - Category-based with Calendar View
@Composable
fun StatsTab(database: TimerDatabase) {
    val scope = rememberCoroutineScope()
    val categories by database.activityCategoryDao().getAllActiveCategories().collectAsState(initial = emptyList())
    var selectedCategoryForStats by remember { mutableStateOf<ActivityCategory?>(null) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CategorySelectorCard(
                categories = categories,
                selectedCategory = selectedCategoryForStats,
                onSelectCategory = { selectedCategoryForStats = it }
            )
        }
        
        if (selectedCategoryForStats != null) {
            item {
                CategoryStatsCard(database, selectedCategoryForStats!!)
            }
            
            item {
                CalendarGoalView(database, selectedCategoryForStats!!)
            }
        } else if (categories.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a category to view statistics",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelectorCard(
    categories: List<ActivityCategory>,
    selectedCategory: ActivityCategory?,
    onSelectCategory: (ActivityCategory?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Select Category for Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (categories.isEmpty()) {
                Text(
                    "No categories yet. Create one in the Categories tab!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(minOf((categories.size / 3 + 1) * 80, 240).dp)
                ) {
                    items(categories.size) { index ->
                        val category = categories[index]
                        StatsCategoryChip(
                            category = category,
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { 
                                onSelectCategory(if (selectedCategory?.id == category.id) null else category)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCategoryChip(
    category: ActivityCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            Color(category.color).copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(category.color))
        } else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(category.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CategoryStatsCard(database: TimerDatabase, category: ActivityCategory) {
    val scope = rememberCoroutineScope()
    var totalMinutes by remember { mutableIntStateOf(0) }
    var sessionCount by remember { mutableIntStateOf(0) }
    var averageMinutes by remember { mutableFloatStateOf(0f) }
    var lastSessionDate by remember { mutableStateOf<String?>(null) }
    var streakInfo by remember { mutableStateOf<StreakInfo?>(null) }
    
    LaunchedEffect(category.id) {
        scope.launch(Dispatchers.IO) {
            val total = database.timeSessionDao().getTotalMinutesForCategory(category.id) ?: 0
            val count = database.timeSessionDao().getSessionCountForCategory(category.id)
            val avg = database.timeSessionDao().getAverageSessionMinutesForCategory(category.id) ?: 0f
            val last = database.timeSessionDao().getLastSessionDateForCategory(category.id)
            
            // Get category-specific dates
            val sessions = database.timeSessionDao().getSessionsBetweenDatesSync(
                LocalDate.now().minusDays(90).format(DateTimeFormatter.ISO_LOCAL_DATE),
                getTodayDateString()
            )
            val categoryDates = sessions.filter { it.categoryId == category.id }
                .map { it.date }
                .distinct()
            
            val streaks = if (categoryDates.isNotEmpty()) {
                calculateStreaks(categoryDates, 90)
            } else {
                StreakInfo(0, 0, 0, 0f)
            }
            
            withContext(Dispatchers.Main) {
                totalMinutes = total
                sessionCount = count
                averageMinutes = avg
                lastSessionDate = last
                streakInfo = streaks
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(category.color).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.icon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = formatDuration(totalMinutes),
                    label = "Total Time",
                    color = Color(category.color)
                )
                StatColumn(
                    value = sessionCount.toString(),
                    label = "Sessions",
                    color = Color(category.color)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = formatDuration(averageMinutes.toInt()),
                    label = "Avg Session",
                    color = Color(category.color)
                )
                StatColumn(
                    value = "${streakInfo?.currentStreak ?: 0}🔥",
                    label = "Current Streak",
                    color = Color(category.color)
                )
            }
            
            if (category.dailyGoalMinutes > 0) {
                HorizontalDivider()
                Text(
                    "Daily Goal: ${formatDuration(category.dailyGoalMinutes)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(category.color)
                )
            }
            
            if (lastSessionDate != null) {
                Text(
                    "Last session: ${formatDateDisplay(lastSessionDate!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatDateDisplay(dateString: String): String {
    val date = LocalDate.parse(dateString)
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    
    return when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

// Calendar Goal View - Green = Goal Met, Red = Goal Not Met
@Composable
fun CalendarGoalView(database: TimerDatabase, category: ActivityCategory) {
    val scope = rememberCoroutineScope()
    var calendarData by remember { mutableStateOf<List<CalendarDay>>(emptyList()) }
    val currentMonth = remember { LocalDate.now().withDayOfMonth(1) }
    
    LaunchedEffect(category.id) {
        scope.launch(Dispatchers.IO) {
            val startOfMonth = currentMonth
            val endOfMonth = currentMonth.plusMonths(1).minusDays(1)
            
            val sessions = database.timeSessionDao().getSessionsBetweenDatesSync(
                startOfMonth.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endOfMonth.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            
            val dailyMinutes = sessions
                .filter { it.categoryId == category.id }
                .groupBy { it.date }
                .mapValues { it.value.sumOf { session -> session.durationMinutes } }
            
            val days = mutableListOf<CalendarDay>()
            var currentDate = startOfMonth
            
            while (!currentDate.isAfter(endOfMonth)) {
                val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val minutes = dailyMinutes[dateStr] ?: 0
                val goalMet = if (category.dailyGoalMinutes > 0) {
                    minutes >= category.dailyGoalMinutes
                } else {
                    minutes > 0
                }
                
                days.add(CalendarDay(
                    date = currentDate,
                    minutes = minutes,
                    goalMet = goalMet,
                    hasActivity = minutes > 0
                ))
                
                currentDate = currentDate.plusDays(1)
            }
            
            withContext(Dispatchers.Main) {
                calendarData = days
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "📅 ${currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Day headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(((calendarData.size / 7 + 1) * 48).dp)
            ) {
                // Add empty cells for days before month starts
                val startDayOfWeek = calendarData.firstOrNull()?.date?.dayOfWeek?.value ?: 0
                val emptyDays = if (startDayOfWeek == 7) 0 else startDayOfWeek
                
                items(emptyDays) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
                
                items(calendarData.size) { index ->
                    CalendarDayCell(calendarData[index], Color(category.color))
                }
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(Color(0xFF4CAF50), "Goal Met")
                LegendItem(Color(0xFFF44336), "Not Met")
                LegendItem(MaterialTheme.colorScheme.surfaceVariant, "No Activity")
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: CalendarDay, categoryColor: Color) {
    val backgroundColor = when {
        day.goalMet -> Color(0xFF4CAF50).copy(alpha = 0.8f) // Green
        day.hasActivity -> Color(0xFFF44336).copy(alpha = 0.6f) // Red
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val isToday = day.date == LocalDate.now()
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.hasActivity) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (day.minutes > 0) {
                Text(
                    formatDuration(day.minutes),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class CalendarDay(
    val date: LocalDate,
    val minutes: Int,
    val goalMet: Boolean,
    val hasActivity: Boolean
)

// CATEGORIES TAB - Complete with edit functionality
@Composable
fun CategoriesTab(database: TimerDatabase) {
    val scope = rememberCoroutineScope()
    val categories by database.activityCategoryDao().getAllActiveCategories().collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ActivityCategory?>(null) }
    var deletingCategory by remember { mutableStateOf<ActivityCategory?>(null) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Category")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("📁", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Categories Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create your first activity category to start tracking time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    CategoryListItem(
                        category = category,
                        onEdit = { editingCategory = category },
                        onDelete = { deletingCategory = category }
                    )
                }
            }
        }
    }
    
    // Add Dialog
    if (showAddDialog) {
        CategoryDialog(
            category = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, icon, color, goalMinutes ->
                scope.launch(Dispatchers.IO) {
                    val category = ActivityCategory(
                        name = name,
                        icon = icon,
                        color = color,
                        dailyGoalMinutes = goalMinutes
                    )
                    database.activityCategoryDao().insertCategory(category)
                    
                    withContext(Dispatchers.Main) {
                        showAddDialog = false
                    }
                }
            }
        )
    }
    
    // Edit Dialog
    if (editingCategory != null) {
        CategoryDialog(
            category = editingCategory,
            onDismiss = { editingCategory = null },
            onSave = { name, icon, color, goalMinutes ->
                scope.launch(Dispatchers.IO) {
                    val updated = editingCategory!!.copy(
                        name = name,
                        icon = icon,
                        color = color,
                        dailyGoalMinutes = goalMinutes,
                        updatedAt = System.currentTimeMillis()
                    )
                    database.activityCategoryDao().updateCategory(updated)
                    
                    withContext(Dispatchers.Main) {
                        editingCategory = null
                    }
                }
            }
        )
    }
    
    // Delete Confirmation
    if (deletingCategory != null) {
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Category?") },
            text = {
                Text("This will delete \"${deletingCategory!!.name}\" and all its tracked sessions. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            database.activityCategoryDao().deleteCategory(deletingCategory!!)
                            
                            withContext(Dispatchers.Main) {
                                deletingCategory = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryListItem(
    category: ActivityCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(category.color).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(category.color).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (category.dailyGoalMinutes > 0) {
                    Text(
                        "Goal: ${formatDuration(category.dailyGoalMinutes)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(category.color)
                    )
                } else {
                    Text(
                        "No daily goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Category Dialog with Manual Goal Input
@Composable
fun CategoryDialog(
    category: ActivityCategory?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Int) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(category?.icon ?: ActivityIcons.ICONS[0].first) }
    var selectedColor by remember { mutableStateOf(category?.color ?: CategoryColors.COLORS[0].first) }
    var goalMinutes by remember { mutableStateOf(category?.dailyGoalMinutes ?: 60) }
    var goalInput by remember { mutableStateOf((category?.dailyGoalMinutes ?: 60).toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Create Category" else "Edit Category") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(500.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g., Learn Python") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    Column {
                        Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.height(140.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(ActivityIcons.ICONS.size) { index ->
                                val icon = ActivityIcons.ICONS[index].first
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedIcon == icon)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            else Color.Transparent
                                        )
                                        .clickable { selectedIcon = icon },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(icon, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
                
                item {
                    Column {
                        Text("Select Color", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CategoryColors.COLORS.size) { index ->
                                val color = CategoryColors.COLORS[index].first
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            3.dp,
                                            if (selectedColor == color)
                                                MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Column {
                        Text("Daily Goal", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { 
                                goalInput = it
                                goalMinutes = it.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                            },
                            label = { Text("Minutes per day") },
                            placeholder = { Text("e.g., 180 for 3 hours") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            supportingText = {
                                Text(
                                    if (goalMinutes > 0) formatDuration(goalMinutes) else "No goal",
                                    color = if (goalMinutes > 0) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, selectedIcon, selectedColor, goalMinutes) },
                enabled = name.isNotBlank()
            ) {
                Text(if (category == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
