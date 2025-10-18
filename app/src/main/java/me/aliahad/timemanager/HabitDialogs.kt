package me.aliahad.timemanager

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import me.aliahad.timemanager.data.Habit
import me.aliahad.timemanager.data.HabitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(habitColors[0]) }
    var selectedIcon by remember { mutableStateOf(habitIcons[0]) }
    var habitType by remember { mutableStateOf(HabitType.BUILD) }
    var goalPeriodDays by remember { mutableIntStateOf(30) }
    var isEveryday by remember { mutableStateOf(true) }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }
    
    HabitDialogContent(
        title = "Create New Habit",
        buttonText = "Create",
        name = name,
        description = description,
        selectedColor = selectedColor,
        selectedIcon = selectedIcon,
        habitType = habitType,
        goalPeriodDays = goalPeriodDays,
        isEveryday = isEveryday,
        hasReminder = hasReminder,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        onNameChange = { name = it },
        onDescriptionChange = { description = it },
        onColorChange = { selectedColor = it },
        onIconChange = { selectedIcon = it },
        onTypeChange = { habitType = it },
        onGoalPeriodChange = { goalPeriodDays = it },
        onEverydayChange = { isEveryday = it },
        onReminderChange = { hasReminder = it },
        onReminderHourChange = { reminderHour = it },
        onReminderMinuteChange = { reminderMinute = it },
        onDismiss = onDismiss,
        onSave = {
            val newHabit = Habit(
                name = name,
                description = description,
                color = selectedColor.value.toLong(),
                iconName = selectedIcon.name,
                type = habitType,
                goalPeriodDays = goalPeriodDays,
                isEveryday = isEveryday,
                reminderTimeHour = if (hasReminder) reminderHour else null,
                reminderTimeMinute = if (hasReminder) reminderMinute else null
            )
            onSave(newHabit)
        },
        vibrator = vibrator
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    
    var name by remember { mutableStateOf(habit.name) }
    var description by remember { mutableStateOf(habit.description) }
    var selectedColor by remember { mutableStateOf(Color(habit.color)) }
    var selectedIcon by remember { mutableStateOf(habitIcons.find { it.name == habit.iconName } ?: habitIcons[0]) }
    var habitType by remember { mutableStateOf(habit.type) }
    var goalPeriodDays by remember { mutableIntStateOf(habit.goalPeriodDays) }
    var isEveryday by remember { mutableStateOf(habit.isEveryday) }
    var hasReminder by remember { mutableStateOf(habit.reminderTimeHour != null) }
    var reminderHour by remember { mutableIntStateOf(habit.reminderTimeHour ?: 9) }
    var reminderMinute by remember { mutableIntStateOf(habit.reminderTimeMinute ?: 0) }
    
    HabitDialogContent(
        title = "Edit Habit",
        buttonText = "Save",
        name = name,
        description = description,
        selectedColor = selectedColor,
        selectedIcon = selectedIcon,
        habitType = habitType,
        goalPeriodDays = goalPeriodDays,
        isEveryday = isEveryday,
        hasReminder = hasReminder,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        onNameChange = { name = it },
        onDescriptionChange = { description = it },
        onColorChange = { selectedColor = it },
        onIconChange = { selectedIcon = it },
        onTypeChange = { habitType = it },
        onGoalPeriodChange = { goalPeriodDays = it },
        onEverydayChange = { isEveryday = it },
        onReminderChange = { hasReminder = it },
        onReminderHourChange = { reminderHour = it },
        onReminderMinuteChange = { reminderMinute = it },
        onDismiss = onDismiss,
        onSave = {
            val updatedHabit = habit.copy(
                name = name,
                description = description,
                color = selectedColor.value.toLong(),
                iconName = selectedIcon.name,
                type = habitType,
                goalPeriodDays = goalPeriodDays,
                isEveryday = isEveryday,
                reminderTimeHour = if (hasReminder) reminderHour else null,
                reminderTimeMinute = if (hasReminder) reminderMinute else null
            )
            onSave(updatedHabit)
        },
        vibrator = vibrator
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitDialogContent(
    title: String,
    buttonText: String,
    name: String,
    description: String,
    selectedColor: Color,
    selectedIcon: HabitIcon,
    habitType: HabitType,
    goalPeriodDays: Int,
    isEveryday: Boolean,
    hasReminder: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorChange: (Color) -> Unit,
    onIconChange: (HabitIcon) -> Unit,
    onTypeChange: (HabitType) -> Unit,
    onGoalPeriodChange: (Int) -> Unit,
    onEverydayChange: (Boolean) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onReminderHourChange: (Int) -> Unit,
    onReminderMinuteChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    vibrator: Vibrator
) {
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Text(
                    text = "Create Habit",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g., Morning Exercise") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                
                // Description input
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add notes or details") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )
                
                // Icon selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(habitIcons) { habitIcon ->
                                Surface(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable {
                                            vibrator.vibrate(
                                                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                            )
                                            onIconChange(habitIcon)
                                        },
                                shape = CircleShape,
                                color = if (selectedIcon == habitIcon)
                                    selectedColor.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selectedIcon == habitIcon)
                                    BorderStroke(2.dp, selectedColor)
                                else null
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = habitIcon.icon,
                                        contentDescription = habitIcon.displayName,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (selectedIcon == habitIcon)
                                            selectedColor
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Color selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(habitColors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (selectedColor == color) {
                                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            } else Modifier
                                        )
                                        .clickable {
                                            vibrator.vibrate(
                                                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                            )
                                            onColorChange(color)
                                        }
                                )
                            }
                        }
                }
                
                // Habit Type
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                            HabitTypeOption(
                                type = HabitType.BUILD,
                                selected = habitType == HabitType.BUILD,
                                onClick = {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                    )
                                    onTypeChange(HabitType.BUILD)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            HabitTypeOption(
                                type = HabitType.QUIT,
                                selected = habitType == HabitType.QUIT,
                                onClick = {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                    )
                                    onTypeChange(HabitType.QUIT)
                                },
                                modifier = Modifier.weight(1f)
                            )
                    }
                }
                
                // Goal Period
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Goal Period",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "$goalPeriodDays days",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                        Slider(
                            value = goalPeriodDays.toFloat(),
                            onValueChange = { onGoalPeriodChange(it.toInt()) },
                            valueRange = 7f..365f,
                            steps = 50
                        )
                }
                
                // Everyday toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                                onEverydayChange(!isEveryday)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Everyday Task",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Track this habit daily",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        Switch(
                            checked = isEveryday,
                            onCheckedChange = {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                                onEverydayChange(it)
                            }
                        )
                    }
                
                // Reminder toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                                onReminderChange(!hasReminder)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Reminder",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Get notified every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                                onReminderChange(it)
                            }
                        )
                    }
                
                // Time picker if reminder is enabled
                if (hasReminder) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Text(
                                text = "Reminder Time",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Apple-style time picker
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour picker
                                    TimePickerColumn(
                                        label = "Hour",
                                        value = reminderHour,
                                        range = 0..23,
                                        onValueChange = {
                                            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                            onReminderHourChange(it)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    
                                    // Minute picker
                                    TimePickerColumn(
                                        label = "Min",
                                        value = reminderMinute,
                                        range = 0..59,
                                        onValueChange = {
                                            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                            onReminderMinuteChange(it)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                            }
                        }
                    }
                }
                
                Divider()
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                                onSave()
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
fun HabitTypeOption(
    type: HabitType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (type == HabitType.BUILD) 
                    Icons.Default.TrendingUp 
                else 
                    Icons.Default.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (type == HabitType.BUILD) "Build" else "Quit",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (type == HabitType.BUILD) "Start good habits" else "Break bad habits",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Predefined colors for habits
@Composable
fun TimePickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Up arrow
        IconButton(
            onClick = {
                val newValue = if (value - 1 < range.first) range.last else value - 1
                onValueChange(newValue)
            }
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Center highlight with value
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = String.format("%02d", value),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Down arrow
        IconButton(
            onClick = {
                val newValue = if (value + 1 > range.last) range.first else value + 1
                onValueChange(newValue)
            }
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Label at bottom
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

val habitColors = listOf(
    Color(0xFFE57373), // Red
    Color(0xFFFF7043), // Deep Orange
    Color(0xFFFFB74D), // Orange
    Color(0xFFFFD54F), // Amber
    Color(0xFFAED581), // Light Green
    Color(0xFF81C784), // Green
    Color(0xFF4DD0E1), // Cyan
    Color(0xFF64B5F6), // Blue
    Color(0xFF7986CB), // Indigo
    Color(0xFF9575CD), // Deep Purple
    Color(0xFFBA68C8), // Purple
    Color(0xFFF06292), // Pink
)

