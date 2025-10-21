package me.aliahad.timemanager

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.data.DailyTask
import me.aliahad.timemanager.data.TimerDatabase
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPlannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var selectedDate by remember { mutableStateOf(DailyPlannerUtils.getTodayDateString()) }
    var tasks by remember { mutableStateOf<List<DailyTask>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DailyTask?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<DailyTask?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
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
    
    // Load tasks
    LaunchedEffect(selectedDate, refreshTrigger) {
        database.dailyTaskDao().getTasksForDate(selectedDate).collect {
            tasks = it
        }
    }
    
    // Separate effect for onboarding (runs once per date)
    LaunchedEffect(selectedDate) {
        if (selectedDate == DailyPlannerUtils.getTodayDateString()) {
            withContext(Dispatchers.IO) {
                val count = database.dailyTaskDao().getTaskCountForDate(selectedDate)
                if (count == 0) {
                    DailyPlannerUtils.getOnboardingTasks(selectedDate).forEach { task ->
                        database.dailyTaskDao().insertTask(task)
                    }
                }
            }
        }
    }
    
    val onRefresh = {
        refreshTrigger++
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Planner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showDailyPlannerDatePickerDialog(
                            context = context,
                            currentDate = LocalDate.parse(selectedDate),
                            onDateSelected = { newDate: LocalDate ->
                                selectedDate = newDate.toString()
                            }
                        )
                    }) {
                        Icon(Icons.Default.CalendarToday, "Select Date")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, "Add Task")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Date Header & Progress Card
                DateHeaderCard(selectedDate, tasks)
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Timeline View
                TimelineView(
                    tasks = tasks,
                    onTaskClick = { task ->
                        editingTask = task
                    },
                    onCompleteToggle = { task ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.dailyTaskDao().updateTask(
                                    task.copy(isCompleted = !task.isCompleted)
                                )
                            }
                            onRefresh()
                        }
                    },
                    onDeleteClick = { task ->
                        taskToDelete = task
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
    
    // Add/Edit Task Dialog
    if (showAddDialog || editingTask != null) {
        TaskDialog(
            task = editingTask,
            date = selectedDate,
            existingTasks = tasks,
            onDismiss = {
                showAddDialog = false
                editingTask = null
            },
            onSave = { task ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (editingTask != null) {
                            database.dailyTaskDao().updateTask(task)
                        } else {
                            database.dailyTaskDao().insertTask(task)
                        }
                    }
                    showAddDialog = false
                    editingTask = null
                    onRefresh()
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?") },
            text = {
                Text("Are you sure you want to delete '${taskToDelete!!.title}'?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.dailyTaskDao().deleteTask(taskToDelete!!)
                            }
                            showDeleteDialog = false
                            taskToDelete = null
                            onRefresh()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateHeaderCard(date: String, tasks: List<DailyTask>) {
    val progress = DailyPlannerUtils.calculateProgress(tasks)
    val completed = tasks.count { it.isCompleted }
    val total = tasks.size
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = DailyPlannerUtils.formatDateForDisplay(date),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = DailyPlannerUtils.getCompletionText(completed, total),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (total > 0) {
                        Text(
                            text = "${(progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Circular Progress
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                    )
                    Text(
                        text = "$completed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineView(
    tasks: List<DailyTask>,
    onTaskClick: (DailyTask) -> Unit,
    onCompleteToggle: (DailyTask) -> Unit,
    onDeleteClick: (DailyTask) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onCompleteToggle = { onCompleteToggle(task) },
                    onDeleteClick = { onDeleteClick(task) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TaskCard(
    task: DailyTask,
    onClick: () -> Unit,
    onCompleteToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isNow = DailyPlannerUtils.isTaskNow(task)
    val isUpcoming = DailyPlannerUtils.isTaskUpcoming(task)
    val duration = DailyPlannerUtils.calculateDuration(task.startTime, task.endTime)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNow) Modifier.border(2.dp, Color(task.color), RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Complete Checkbox
                    IconButton(
                        onClick = onCompleteToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (task.isCompleted) "Completed" else "Mark Complete",
                            tint = if (task.isCompleted) Color(0xFF51CF66) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onClick)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.icon,
                                fontSize = 20.sp
                            )
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                color = if (task.isCompleted) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${DailyPlannerUtils.formatTimeForDisplay(task.startTime)} - ${DailyPlannerUtils.formatTimeForDisplay(task.endTime)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DailyPlannerUtils.getDurationText(duration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(task.color)
                            )
                        }
                        
                        if (task.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Status Badges
                        if (isNow || isUpcoming || task.priority == "High") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isNow && !task.isCompleted) {
                                    StatusBadge("• NOW", Color(0xFFFF6B6B))
                                }
                                if (isUpcoming && !task.isCompleted && !isNow) {
                                    StatusBadge("SOON", Color(0xFFFFD93D))
                                }
                                if (task.priority == "High") {
                                    StatusBadge("HIGH", Color(0xFFFF6B6B))
                                }
                            }
                        }
                    }
                }
                
                // Delete Button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📅",
                fontSize = 64.sp
            )
            Text(
                text = "No tasks yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap + to add your first task\nand start planning your day!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    task: DailyTask?,
    date: String,
    existingTasks: List<DailyTask>,
    onDismiss: () -> Unit,
    onSave: (DailyTask) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var startTime by remember { mutableStateOf(task?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(task?.endTime ?: "10:00") }
    var taskType by remember { mutableStateOf(task?.taskType ?: "TASK") }
    var category by remember { mutableStateOf(task?.category ?: "Work") }
    var priority by remember { mutableStateOf(task?.priority ?: "Medium") }
    var selectedIcon by remember { mutableStateOf(task?.icon ?: "📝") }
    var selectedColor by remember { mutableStateOf(task?.let { Color(it.color) } ?: Color(0xFF4DABF7)) }
    
    val taskTypes = listOf("TASK", "EVENT", "BREAK", "FOCUS", "ROUTINE")
    val categories = listOf("Work", "Personal", "Health", "Learning", "Social", "Finance", "Shopping", "Travel")
    val priorities = listOf("Low", "Medium", "High")
    
    val icons = listOf(
        "📝", "📅", "☕", "🎯", "🔄", "💼", "🏠", "💪", "📚", "👥",
        "💰", "🛒", "✈️", "🍽️", "🧘", "🌅", "🌙", "🚀", "✨", "📋"
    )
    
    val colors = listOf(
        Color(0xFF4DABF7), Color(0xFF51CF66), Color(0xFFFFD93D), Color(0xFFFF6B6B),
        Color(0xFF9775FA), Color(0xFFFF6BCB), Color(0xFF3BC9DB), Color(0xFFFFAB40)
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (task != null) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { newValue ->
                                // Allow only digits and colon
                                if (newValue.length <= 5 && newValue.all { it.isDigit() || it == ':' }) {
                                    startTime = newValue
                                }
                            },
                            label = { Text("Start Time *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("09:00") },
                            supportingText = { Text("Format: HH:mm (24h)") },
                            isError = startTime.isNotBlank() && !DailyPlannerUtils.isValidTimeFormat(startTime)
                        )
                        
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { newValue ->
                                // Allow only digits and colon
                                if (newValue.length <= 5 && newValue.all { it.isDigit() || it == ':' }) {
                                    endTime = newValue
                                }
                            },
                            label = { Text("End Time *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("10:00") },
                            supportingText = { Text("Must be after start") },
                            isError = endTime.isNotBlank() && (!DailyPlannerUtils.isValidTimeFormat(endTime) || 
                                (startTime.isNotBlank() && DailyPlannerUtils.isValidTimeFormat(startTime) && 
                                 !DailyPlannerUtils.isEndTimeAfterStart(startTime, endTime)))
                        )
                    }
                }
                
                item {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        taskTypes.take(3).forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        taskTypes.drop(3).forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
                
                item {
                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${DailyPlannerUtils.getCategoryIcon(cat)} $cat") },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorities.forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio) }
                            )
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.take(10).forEach { icon ->
                            IconButton(
                                onClick = { selectedIcon = icon },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (selectedIcon == icon) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(text = icon, fontSize = 20.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.drop(10).forEach { icon ->
                            IconButton(
                                onClick = { selectedIcon = icon },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (selectedIcon == icon) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(text = icon, fontSize = 20.sp)
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .then(
                                        if (selectedColor == color) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    )
                            )
                        }
                    }
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
                                if (title.isNotBlank() && 
                                    DailyPlannerUtils.isValidTimeFormat(startTime) && 
                                    DailyPlannerUtils.isValidTimeFormat(endTime) &&
                                    DailyPlannerUtils.isEndTimeAfterStart(startTime, endTime)) {
                                    
                                    // Check for overlaps with existing tasks (excluding current task being edited)
                                    val newTask = DailyTask(
                                        id = task?.id ?: 0,
                                        title = title,
                                        description = description,
                                        date = date,
                                        startTime = startTime,
                                        endTime = endTime,
                                        taskType = taskType,
                                        category = category,
                                        priority = priority,
                                        icon = selectedIcon,
                                        color = selectedColor.value.toLong(),
                                        isCompleted = task?.isCompleted ?: false
                                    )
                                    
                                    // Check overlaps (warn but allow)
                                    val hasOverlap = existingTasks
                                        .filter { it.id != (task?.id ?: 0) }
                                        .any { DailyPlannerUtils.isTaskOverlapping(it, newTask) }
                                    
                                    if (hasOverlap) {
                                        android.util.Log.w("DailyPlanner", "Warning: Task overlaps with existing task(s)")
                                    }
                                    
                                    onSave(newTask)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = title.isNotBlank() && 
                                DailyPlannerUtils.isValidTimeFormat(startTime) && 
                                DailyPlannerUtils.isValidTimeFormat(endTime) &&
                                DailyPlannerUtils.isEndTimeAfterStart(startTime, endTime)
                        ) {
                            Text(if (task != null) "Update" else "Add")
                        }
                    }
                }
            }
        }
    }
}

fun showDailyPlannerDatePickerDialog(context: Context, currentDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        currentDate.year,
        currentDate.monthValue - 1,
        currentDate.dayOfMonth
    ).show()
}

