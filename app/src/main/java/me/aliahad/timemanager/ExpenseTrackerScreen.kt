package me.aliahad.timemanager

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.data.*
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // Get user's currency preference
    val userProfile by database.userProfileDao().getProfile().collectAsState(initial = null)
    val currency = userProfile?.currency ?: "৳"
    
    // Initialize default categories on first launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val count = database.expenseCategoryDao().getCategoryCount()
            if (count == 0) {
                initializeDefaultCategories(database)
            }
        }
    }
    
    // Refresh data when screen resumes
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Add") },
                    icon = { Icon(Icons.Default.Add, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("List") },
                    icon = { Icon(Icons.Default.List, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Stats") },
                    icon = { Icon(Icons.Default.BarChart, null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Categories") },
                    icon = { Icon(Icons.Default.Category, null) }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> AddExpenseTab(
                    database = database,
                    refreshTrigger = refreshTrigger,
                    onRefresh = { refreshTrigger++ },
                    currency = currency
                )
                1 -> ExpenseListTab(database = database, refreshTrigger = refreshTrigger, currency = currency)
                2 -> StatsTab(database = database, refreshTrigger = refreshTrigger, currency = currency)
                3 -> CategoriesTab(
                    database = database,
                    refreshTrigger = refreshTrigger,
                    onRefresh = { refreshTrigger++ },
                    currency = currency
                )
            }
        }
    }
}

@Composable
fun AddExpenseTab(
    database: TimerDatabase,
    refreshTrigger: Int,
    onRefresh: () -> Unit,
    currency: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories by database.expenseCategoryDao().getAllActiveCategories()
        .collectAsState(initial = emptyList())
    
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Auto-select first category if none selected
    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
    }
    
    // Refresh categories when refreshTrigger changes
    LaunchedEffect(refreshTrigger) {
        // Categories will auto-refresh via Flow
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Amount Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = amount,
                            onValueChange = { 
                                // Allow only digits and single decimal point
                                val filtered = it.filter { char -> char.isDigit() || char == '.' }
                                val decimalCount = filtered.count { char -> char == '.' }
                                if (decimalCount <= 1) {
                                    amount = filtered
                                }
                            },
                            placeholder = { Text("0.00", style = MaterialTheme.typography.headlineLarge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }
        
        // Category Selection
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { /* Switch to categories tab */ }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage", fontSize = 12.sp)
                }
            }
        }
        
        item {
            if (categories.isEmpty()) {
                Text(
                    text = "No categories available. Please add categories first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.chunked(2).forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCategories.forEach { category ->
                                CategoryChip(
                                    category = category,
                                    isSelected = selectedCategoryId == category.id,
                                    onClick = { selectedCategoryId = category.id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Add empty space if odd number of categories
                            if (rowCategories.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        // Description
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
        }
        
        // Date Selection
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePickerDialog(
                            context = context,
                            currentDate = selectedDate,
                            onDateSelected = { newDate -> selectedDate = newDate }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ExpenseAnalytics.formatDateForDisplay(
                                selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        // Payment Method
        item {
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cash", "Card", "Online", "Other").forEach { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text(method) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Success Message
        item {
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Expense added successfully!",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Add Button
        item {
            Button(
                onClick = {
                    scope.launch {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null && amountValue > 0 && selectedCategoryId != null) {
                            withContext(Dispatchers.IO) {
                                database.expenseDao().insertExpense(
                                    Expense(
                                        categoryId = selectedCategoryId!!,
                                        amount = amountValue,
                                        description = description.trim(),
                                        date = selectedDate.format(
                                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                        ),
                                        paymentMethod = selectedPaymentMethod
                                    )
                                )
                            }
                            // Reset form
                            amount = ""
                            description = ""
                            // Keep category and payment method selected
                            selectedDate = LocalDate.now()
                            showSuccessMessage = true
                            onRefresh() // Refresh home screen data
                            kotlinx.coroutines.delay(2000)
                            showSuccessMessage = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && selectedCategoryId != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Expense", fontSize = 16.sp)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Helper function to show date picker dialog
fun showDatePickerDialog(
    context: Context,
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth)
    
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // Set max date to today
        datePicker.maxDate = System.currentTimeMillis()
        show()
    }
}

@Composable
fun CategoryChip(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(category.color)
    
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoriesTab(
    database: TimerDatabase,
    refreshTrigger: Int,
    onRefresh: () -> Unit,
    currency: String
) {
    val scope = rememberCoroutineScope()
    val categories by database.expenseCategoryDao().getAllCategories()
        .collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<ExpenseCategory?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (categories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Category,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No categories yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Create your first category to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onEdit = { editingCategory = category },
                        onDelete = {
                            categoryToDelete = category
                            showDeleteDialog = true
                        },
                        currency = currency
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Category")
        }
    }
    
    // Add/Edit Category Dialog
    if (showAddDialog || editingCategory != null) {
        ExpenseCategoryDialog(
            category = editingCategory,
            onDismiss = {
                showAddDialog = false
                editingCategory = null
            },
            onSave = { name, icon, color, budget ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (editingCategory != null) {
                            database.expenseCategoryDao().updateCategory(
                                editingCategory!!.copy(
                                    name = name,
                                    icon = icon,
                                    color = color.toArgb().toLong() and 0xFFFFFFFFL,
                                    budget = budget
                                )
                            )
                        } else {
                            database.expenseCategoryDao().insertCategory(
                                ExpenseCategory(
                                    name = name,
                                    icon = icon,
                                    color = color.toArgb().toLong() and 0xFFFFFFFFL,
                                    budget = budget,
                                    isActive = true
                                )
                            )
                        }
                    }
                    showAddDialog = false
                    editingCategory = null
                    onRefresh()
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Category?") },
            text = {
                Text("Are you sure you want to delete '${categoryToDelete!!.name}'? All expenses in this category will also be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.expenseCategoryDao().deleteCategory(categoryToDelete!!)
                            }
                            showDeleteDialog = false
                            categoryToDelete = null
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
fun CategoryItem(
    category: ExpenseCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    currency: String
) {
    val color = Color(category.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (category.budget > 0) {
                    Text(
                        text = "Budget: ${ExpenseAnalytics.formatCurrency(category.budget, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ExpenseCategoryDialog(
    category: ExpenseCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: Color, budget: Double) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "💰") }
    var selectedColor by remember { mutableStateOf(category?.let { Color(it.color) } ?: Color(0xFFFF6B6B)) }
    var budget by remember { mutableStateOf(category?.budget?.toString() ?: "") }
    
    val availableIcons = listOf(
        "💰", "🍔", "🍕", "☕", "🚗", "🚌", "🚕", "✈️",
        "🏠", "💡", "💊", "🏥", "📚", "🎓", "🛍️", "👕",
        "🎮", "🎬", "🎵", "⚽", "🎨", "📱", "💻", "🔧"
    )
    
    val availableColors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4DABF7), Color(0xFF51CF66), Color(0xFFFFD93D),
        Color(0xFF9775FA), Color(0xFFFF6BCB), Color(0xFF3BC9DB), Color(0xFFFFAB40),
        Color(0xFFADB5BD), Color(0xFFFA5252), Color(0xFF748FFC), Color(0xFF20C997)
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (category != null) "Edit Category" else "New Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.labelLarge
                )
                
                // Icon Grid
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableIcons.chunked(8)) { rowIcons ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowIcons.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (icon == emoji) selectedColor.copy(alpha = 0.3f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (icon == emoji) 2.dp else 1.dp,
                                            color = if (icon == emoji) selectedColor
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { icon = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
                
                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.labelLarge
                )
                
                // Color Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    availableColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monthly Budget (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("৳", style = MaterialTheme.typography.titleMedium) },
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name.trim(),
                                    icon,
                                    selectedColor,
                                    budget.toDoubleOrNull() ?: 0.0
                                )
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseListTab(database: TimerDatabase, refreshTrigger: Int, currency: String) {
    val expenses by database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
    val categories by database.expenseCategoryDao().getAllCategories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    var selectedFilter by remember { mutableStateOf("All") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    
    val filteredExpenses = remember(expenses, selectedFilter) {
        when (selectedFilter) {
            "Today" -> {
                val today = ExpenseAnalytics.getTodayDateString()
                expenses.filter { it.date == today }
            }
            "Week" -> {
                val (start, end) = ExpenseAnalytics.getWeekDateRange()
                expenses.filter { it.date >= start && it.date <= end }
            }
            "Month" -> {
                val (start, end) = ExpenseAnalytics.getMonthDateRange()
                expenses.filter { it.date >= start && it.date <= end }
            }
            else -> expenses
        }
    }
    
    val totalAmount = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Today", "Week", "Month").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }
        
        // Total Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total $selectedFilter Expenses",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ExpenseAnalytics.formatCurrency(totalAmount, currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${filteredExpenses.size} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expense List
        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Receipt,
                        null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No expenses yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Add your first expense to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group by date
                val groupedExpenses = filteredExpenses.groupBy { it.date }.toSortedMap(reverseOrder())
                
                groupedExpenses.forEach { (date, dayExpenses) ->
                    item {
                        Text(
                            text = ExpenseAnalytics.formatDateForDisplay(date),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    
                    items(dayExpenses, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            category = categoryMap[expense.categoryId],
                            onDelete = {
                                expenseToDelete = expense
                                showDeleteDialog = true
                            },
                            currency = currency
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            expenseToDelete?.let {
                                withContext(Dispatchers.IO) {
                                    database.expenseDao().deleteExpense(it)
                                }
                            }
                            showDeleteDialog = false
                            expenseToDelete = null
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
fun ExpenseItem(
    expense: Expense,
    category: ExpenseCategory?,
    onDelete: () -> Unit,
    currency: String
) {
    val color = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.secondary
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category?.icon ?: "💰",
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (expense.description.isNotBlank()) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = expense.paymentMethod,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // Amount and Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ExpenseAnalytics.formatCurrency(expense.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsTab(database: TimerDatabase, refreshTrigger: Int, currency: String) {
    val expenses by database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
    val categories by database.expenseCategoryDao().getAllCategories().collectAsState(initial = emptyList())
    
    var selectedPeriod by remember { mutableStateOf("Month") }
    
    val filteredExpenses = remember(expenses, selectedPeriod) {
        when (selectedPeriod) {
            "Week" -> {
                val (start, end) = ExpenseAnalytics.getWeekDateRange()
                ExpenseAnalytics.filterExpensesByDateRange(expenses, start, end)
            }
            "Month" -> {
                val (start, end) = ExpenseAnalytics.getMonthDateRange()
                ExpenseAnalytics.filterExpensesByDateRange(expenses, start, end)
            }
            "Year" -> {
                val (start, end) = ExpenseAnalytics.getYearDateRange()
                ExpenseAnalytics.filterExpensesByDateRange(expenses, start, end)
            }
            else -> expenses
        }
    }
    
    val stats = remember(filteredExpenses, categories, selectedPeriod) {
        val days = when (selectedPeriod) {
            "Week" -> 7
            "Month" -> LocalDate.now().lengthOfMonth()
            "Year" -> if (LocalDate.now().isLeapYear) 366 else 365
            else -> 30
        }
        ExpenseAnalytics.calculateStats(filteredExpenses, categories, days)
    }
    
    val categorySpending = remember(filteredExpenses, categories) {
        ExpenseAnalytics.calculateCategorySpending(filteredExpenses, categories)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Week", "Month", "Year", "All Time").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Total Spent Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ExpenseAnalytics.formatCurrency(stats.totalExpenses, currency),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${stats.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Statistics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Daily Avg",
                    value = ExpenseAnalytics.formatCurrency(stats.averagePerDay, currency),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Per Transaction",
                    value = ExpenseAnalytics.formatCurrency(stats.averagePerTransaction, currency),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            StatCard(
                title = "Highest Expense",
                value = ExpenseAnalytics.formatCurrency(stats.highestExpense, currency),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Category Breakdown
        if (categorySpending.isNotEmpty()) {
            item {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            items(categorySpending) { spending ->
                CategorySpendingItem(spending = spending, currency = currency)
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No data for this period",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CategorySpendingItem(spending: ExpenseAnalytics.CategorySpending, currency: String) {
    val color = Color(spending.category.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = spending.category.icon, fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spending.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${spending.expenseCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ExpenseAnalytics.formatCurrency(spending.total, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${spending.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { spending.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

// Initialize default categories
suspend fun initializeDefaultCategories(database: TimerDatabase) {
    val defaultCategories = listOf(
        ExpenseCategory(name = "Food", icon = "🍔", color = 0xFFFF6B6B),
        ExpenseCategory(name = "Transport", icon = "🚗", color = 0xFF4DABF7),
        ExpenseCategory(name = "Shopping", icon = "🛍️", color = 0xFFFF6BCB),
        ExpenseCategory(name = "Bills", icon = "💡", color = 0xFFFFD93D),
        ExpenseCategory(name = "Entertainment", icon = "🎮", color = 0xFF9775FA),
        ExpenseCategory(name = "Health", icon = "💊", color = 0xFF51CF66),
        ExpenseCategory(name = "Education", icon = "📚", color = 0xFF3BC9DB),
        ExpenseCategory(name = "Other", icon = "💰", color = 0xFFADB5BD)
    )
    
    defaultCategories.forEach { category ->
        database.expenseCategoryDao().insertCategory(category)
    }
}
