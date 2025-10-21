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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.data.UserProfile
import me.aliahad.timemanager.data.UserStatistics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var statistics by remember { mutableStateOf<UserStatistics?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Lifecycle-aware refresh
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Load profile and statistics
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        
        // Load data on IO dispatcher
        val (loadedProfile, loadedStats) = withContext(Dispatchers.IO) {
            // Get or create profile
            var userProfile = database.userProfileDao().getProfileSync()
            if (userProfile == null) {
                // Create default profile
                val defaultProfile = UserProfile()
                database.userProfileDao().insertProfile(defaultProfile)
                userProfile = database.userProfileDao().getProfileSync()
            }
            
            // Load statistics
            val stats = ProfileAnalytics.getUserStatistics(database)
            
            // Return the data to be assigned on main thread
            Pair(userProfile, stats)
        }
        
        // Update Compose state on main dispatcher (automatically happens after withContext)
        profile = loadedProfile
        statistics = loadedStats
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Loading your profile...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header
                item {
                    ProfileHeaderCard(profile ?: UserProfile())
                }
                
                // Quick Stats
                item {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    statistics?.let { stats ->
                        QuickStatsGrid(stats)
                    }
                }
                
                // Module Statistics
                item {
                    Text(
                        text = "Module Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    statistics?.let { stats ->
                        ModuleStatisticsCards(stats)
                    }
                }
                
                // Achievements
                item {
                    Text(
                        text = "Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    statistics?.let { stats ->
                        AchievementsSection(stats)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Edit Profile Dialog
    if (showEditDialog && profile != null) {
        EditProfileDialog(
            profile = profile!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProfile ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        database.userProfileDao().updateProfile(updatedProfile)
                    }
                    showEditDialog = false
                    refreshTrigger++
                }
            }
        )
    }
}

@Composable
fun ProfileHeaderCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.avatarIcon,
                    fontSize = 56.sp
                )
            }
            
            // Name
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Bio
            if (profile.bio.isNotEmpty()) {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // Join Date
            Text(
                text = try {
                    "Member since ${
                        java.time.Instant.ofEpochMilli(profile.joinDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
                    }"
                } catch (e: Exception) {
                    "Member"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun QuickStatsGrid(stats: UserStatistics) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.heightIn(min = 200.dp, max = 280.dp), // Responsive height
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                icon = "🎯",
                title = "Focus Time",
                value = ProfileAnalytics.formatDuration(stats.totalFocusMinutes),
                subtitle = "${stats.totalFocusSessions} sessions",
                color = Color(0xFFFF6B6B)
            )
        }
        item {
            StatCard(
                icon = "✅",
                title = "Habits",
                value = "${stats.activeHabits}",
                subtitle = "${stats.totalHabitCompletions} completions",
                color = Color(0xFF51CF66)
            )
        }
        item {
            StatCard(
                icon = "💰",
                title = "Expenses",
                value = "${stats.totalExpenses}",
                subtitle = ProfileAnalytics.formatCurrency(stats.totalSpent),
                color = Color(0xFFFFAB40)
            )
        }
        item {
            StatCard(
                icon = "📋",
                title = "Tasks",
                value = "${stats.completedTasks}/${stats.totalTasks}",
                subtitle = "${(stats.taskCompletionRate * 100).toInt()}% done",
                color = Color(0xFF4DABF7)
            )
        }
    }
}

@Composable
fun StatCard(icon: String, title: String, value: String, subtitle: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ModuleStatisticsCards(stats: UserStatistics) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Focus Tracker
        ModuleStatCard(
            icon = "🎯",
            title = "Focus Tracker",
            stats = listOf(
                "Total Time" to ProfileAnalytics.formatDuration(stats.totalFocusMinutes),
                "Sessions" to "${stats.totalFocusSessions}",
                "Streak" to "${stats.focusStreak} days",
                "Categories" to "${stats.activeCategories}"
            ),
            color = Color(0xFFFF6B6B)
        )
        
        // Habit Tracker
        ModuleStatCard(
            icon = "✅",
            title = "Habit Tracker",
            stats = listOf(
                "Active Habits" to "${stats.activeHabits}/${stats.totalHabits}",
                "Completions" to "${stats.totalHabitCompletions}",
                "Success Rate" to "${(stats.habitSuccessRate * 100).toInt()}%"
            ),
            color = Color(0xFF51CF66)
        )
        
        // Expense Tracker
        ModuleStatCard(
            icon = "💰",
            title = "Expense Tracker",
            stats = listOf(
                "Total Expenses" to "${stats.totalExpenses}",
                "Total Spent" to ProfileAnalytics.formatCurrency(stats.totalSpent),
                "Avg/Day" to ProfileAnalytics.formatCurrency(stats.averageDailySpending),
                "Categories" to "${stats.expenseCategories}"
            ),
            color = Color(0xFFFFAB40)
        )
        
        // Subscription Tracker
        ModuleStatCard(
            icon = "📱",
            title = "Subscription Tracker",
            stats = listOf(
                "Active" to "${stats.activeSubscriptions}/${stats.totalSubscriptions}",
                "Monthly Cost" to ProfileAnalytics.formatCurrency(stats.monthlySubscriptionCost),
                "Yearly Cost" to ProfileAnalytics.formatCurrency(stats.yearlySubscriptionCost)
            ),
            color = Color(0xFF20C997)
        )
        
        // Daily Planner
        ModuleStatCard(
            icon = "📋",
            title = "Daily Planner",
            stats = listOf(
                "Total Tasks" to "${stats.totalTasks}",
                "Completed" to "${stats.completedTasks}",
                "Completion Rate" to "${(stats.taskCompletionRate * 100).toInt()}%",
                "Upcoming" to "${stats.upcomingTasks}"
            ),
            color = Color(0xFF6C63FF)
        )
        
        // Other Modules
        if (stats.savedCalculations > 0 || stats.bmiRecords > 0) {
            ModuleStatCard(
                icon = "📊",
                title = "Other",
                stats = buildList {
                    if (stats.savedCalculations > 0) {
                        add("Date Calculations" to "${stats.savedCalculations}")
                    }
                    if (stats.bmiRecords > 0) {
                        add("BMI Records" to "${stats.bmiRecords}")
                        stats.latestBMI?.let {
                            add("Latest BMI" to "%.1f".format(it))
                        }
                    }
                },
                color = Color(0xFF9775FA)
            )
        }
    }
}

@Composable
fun ModuleStatCard(icon: String, title: String, stats: List<Pair<String, String>>, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = icon, fontSize = 28.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            HorizontalDivider()
            
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementsSection(stats: UserStatistics) {
    val achievements = ProfileAnalytics.getUserAchievements(stats)
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        achievements.forEach { achievement ->
            AchievementCard(achievement)
        }
    }
}

@Composable
fun AchievementCard(achievement: me.aliahad.timemanager.data.UserAchievement) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (achievement.isUnlocked) achievement.icon else "🔒",
                    fontSize = 28.sp
                )
            }
            
            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                if (!achievement.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${achievement.currentValue}/${achievement.targetValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Status
            if (achievement.isUnlocked) {
                Icon(
                    Icons.Default.CheckCircle,
                    "Unlocked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var bio by remember { mutableStateOf(profile.bio) }
    var selectedAvatar by remember { mutableStateOf(profile.avatarIcon) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showAvatarPicker = !showAvatarPicker },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedAvatar, fontSize = 40.sp)
                        }
                        Text(
                            text = "Tap to change avatar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                if (showAvatarPicker) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.height(200.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ProfileAnalytics.getAvatarEmojis()) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedAvatar == emoji) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            selectedAvatar = emoji
                                            showAvatarPicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { 
                            if (it.length <= 200) bio = it // Max 200 characters
                        },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        supportingText = {
                            Text(
                                "${bio.length}/200",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onSave(
                                    profile.copy(
                                        name = name.ifBlank { "User" },
                                        bio = bio,
                                        avatarIcon = selectedAvatar,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

