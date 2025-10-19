package me.aliahad.timemanager

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.aliahad.timemanager.data.DateCalculation
import me.aliahad.timemanager.data.DateCategories
import me.aliahad.timemanager.data.TimerDatabase
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val dateCalculationDao = database.dateCalculationDao()
    val scope = rememberCoroutineScope()
    
    val savedCalculations by dateCalculationDao.getAllCalculations().collectAsState(initial = emptyList())
    
    var showCalculator by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var editingCalculation by remember { mutableStateOf<DateCalculation?>(null) }
    
    var startDate by remember { mutableStateOf(LocalDate.now().minusYears(25)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    
    val dateDifference = remember(startDate, endDate) {
        calculateDateDifference(startDate, endDate)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📅 Year Calculator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCalculator = !showCalculator }) {
                        Icon(
                            if (showCalculator) Icons.AutoMirrored.Filled.List else Icons.Default.Add,
                            if (showCalculator) "View saved" else "New calculation"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (showCalculator) {
                FloatingActionButton(
                    onClick = { showSaveDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Save, "Save calculation")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = showCalculator,
                transitionSpec = {
                    slideInHorizontally { if (targetState) it else -it } + fadeIn() togetherWith
                            slideOutHorizontally { if (targetState) -it else it } + fadeOut()
                }
            ) { isCalculatorView ->
                if (isCalculatorView) {
                    CalculatorView(
                        startDate = startDate,
                        endDate = endDate,
                        dateDifference = dateDifference,
                        onStartDateChange = { startDate = it },
                        onEndDateChange = { endDate = it }
                    )
                } else {
                    SavedCalculationsView(
                        calculations = savedCalculations,
                        onCalculationClick = { calc ->
                            startDate = calc.startDate.toLocalDate()
                            endDate = calc.endDate.toLocalDate()
                            showCalculator = true
                        },
                        onCalculationDelete = { calc ->
                            scope.launch {
                                dateCalculationDao.deleteCalculation(calc)
                            }
                        }
                    )
                }
            }
        }
        
        if (showSaveDialog) {
            SaveCalculationDialog(
                startDate = startDate,
                endDate = endDate,
                onDismiss = { showSaveDialog = false },
                onSave = { title, category ->
                    scope.launch {
                        val calculation = DateCalculation(
                            title = title,
                            category = category,
                            startDate = startDate.toMillis(),
                            endDate = endDate.toMillis()
                        )
                        dateCalculationDao.insertCalculation(calculation)
                        showSaveDialog = false
                        showCalculator = false
                    }
                }
            )
        }
    }
}

@Composable
fun CalculatorView(
    startDate: LocalDate,
    endDate: LocalDate,
    dateDifference: DateDifference,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DatePickerCard(
                title = "Start Date",
                date = startDate,
                icon = Icons.Default.CalendarToday,
                color = MaterialTheme.colorScheme.primary,
                onDateChange = onStartDateChange
            )
        }
        
        item {
            DatePickerCard(
                title = "End Date",
                date = endDate,
                icon = Icons.Default.Event,
                color = MaterialTheme.colorScheme.secondary,
                onDateChange = onEndDateChange
            )
        }
        
        item {
            TimeDifferenceCard(dateDifference)
        }
        
        item {
            TotalsCard(dateDifference)
        }
        
        item {
            ZodiacCard(startDate)
        }
        
        if (dateDifference.nextOccurrence != null && dateDifference.daysUntilNext != null) {
            item {
                NextOccurrenceCard(
                    nextDate = dateDifference.nextOccurrence,
                    daysUntil = dateDifference.daysUntilNext
                )
            }
        }
        
        val milestones = getAgeMilestones(dateDifference.years)
        if (milestones.isNotEmpty()) {
            item {
                MilestonesCard(milestones)
            }
        }
    }
}

@Composable
fun DatePickerCard(
    title: String,
    date: LocalDate,
    icon: ImageVector,
    color: Color,
    onDateChange: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date.toReadableString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                Icons.Default.Edit,
                contentDescription = "Change date",
                tint = color
            )
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = date,
            onDateSelected = { onDateChange(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun TimeDifferenceCard(difference: DateDifference) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Time Difference",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeUnit(value = difference.years, label = "Years", emoji = "📅")
                TimeUnit(value = difference.months, label = "Months", emoji = "📆")
                TimeUnit(value = difference.days, label = "Days", emoji = "📍")
            }
        }
    }
}

@Composable
fun TimeUnit(value: Long, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = emoji,
            fontSize = 32.sp
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TotalsCard(difference: DateDifference) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Total Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalRow("Years", String.format("%.2f", difference.totalYears), "🗓️")
                TotalRow("Months", difference.totalMonths.toString(), "📅")
                TotalRow("Weeks", difference.totalWeeks.toString(), "📊")
                TotalRow("Days", difference.totalDays.toString(), "☀️")
                TotalRow("Hours", difference.totalHours.toString(), "⏰")
                TotalRow("Minutes", difference.totalMinutes.toString(), "⏱️")
                TotalRow("Seconds", difference.totalSeconds.toString(), "⚡")
            }
        }
    }
}

@Composable
fun TotalRow(label: String, value: String, emoji: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun ZodiacCard(date: LocalDate) {
    val westernZodiac = WesternZodiac.fromDate(date)
    val chineseZodiac = ChineseZodiac.fromYear(date.year)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Zodiac Signs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Western Zodiac
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = westernZodiac.symbol,
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Western Zodiac",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = westernZodiac.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chinese Zodiac
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chineseZodiac.symbol,
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chinese Zodiac",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${chineseZodiac.displayName} (${chineseZodiac.element})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = chineseZodiac.traits,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NextOccurrenceCard(nextDate: LocalDate, daysUntil: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Cake,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Next Occurrence",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                nextDate.toReadableString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "In $daysUntil days",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun MilestonesCard(milestones: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Milestone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            milestones.forEach { milestone ->
                Text(
                    text = milestone,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SavedCalculationsView(
    calculations: List<DateCalculation>,
    onCalculationClick: (DateCalculation) -> Unit,
    onCalculationDelete: (DateCalculation) -> Unit
) {
    if (calculations.isEmpty()) {
        EmptyStateView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(calculations, key = { it.id }) { calculation ->
                SavedCalculationCard(
                    calculation = calculation,
                    onClick = { onCalculationClick(calculation) },
                    onDelete = { onCalculationDelete(calculation) }
                )
            }
        }
    }
}

@Composable
fun SavedCalculationCard(
    calculation: DateCalculation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val startDate = calculation.startDate.toLocalDate()
    val endDate = calculation.endDate.toLocalDate()
    val difference = calculateDateDifference(startDate, endDate)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(calculation.category),
                    fontSize = 28.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = calculation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = calculation.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${difference.years}y ${difference.months}m ${difference.days}d",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (difference.daysUntilNext != null) {
                    Text(
                        text = "Next in ${difference.daysUntilNext} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📅",
                fontSize = 80.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Saved Calculations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to create your first date calculation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveCalculationDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DateCategories.BIRTHDAY) }
    var customCategory by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Save, contentDescription = null) },
        title = { Text("Save Calculation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., My Birthday, Anniversary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory == DateCategories.CUSTOM) customCategory else selectedCategory,
                        onValueChange = {},
                        readOnly = selectedCategory != DateCategories.CUSTOM,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        DateCategories.DEFAULT_CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(getCategoryEmoji(category))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedCategory == DateCategories.CUSTOM) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Custom Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalCategory = if (selectedCategory == DateCategories.CUSTOM && customCategory.isNotBlank()) {
                            customCategory
                        } else {
                            selectedCategory
                        }
                        onSave(title, finalCategory)
                    }
                },
                enabled = title.isNotBlank() && (selectedCategory != DateCategories.CUSTOM || customCategory.isNotBlank())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toMillis()
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toLocalDate())
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category) {
        DateCategories.BIRTHDAY -> "🎂"
        DateCategories.ANNIVERSARY -> "💑"
        DateCategories.RELATIONSHIP -> "❤️"
        DateCategories.WORK -> "💼"
        DateCategories.MEMORIAL -> "🕯️"
        DateCategories.CUSTOM -> "⭐"
        else -> "📅"
    }
}

