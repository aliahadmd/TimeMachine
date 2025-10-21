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
fun SubscriptionTrackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
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
                title = { Text("Subscription Tracker") },
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
                    text = { Text("Subscriptions") },
                    icon = { Icon(Icons.Default.Subscriptions, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Calendar") },
                    icon = { Icon(Icons.Default.Event, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Stats") },
                    icon = { Icon(Icons.Default.Analytics, null) }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> SubscriptionsTab(
                    database = database,
                    refreshTrigger = refreshTrigger,
                    onRefresh = { refreshTrigger++ }
                )
                1 -> CalendarTab(database = database, refreshTrigger = refreshTrigger)
                2 -> SubscriptionStatsTab(database = database, refreshTrigger = refreshTrigger)
            }
        }
    }
}

@Composable
fun SubscriptionsTab(
    database: TimerDatabase,
    refreshTrigger: Int,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val subscriptions by database.subscriptionDao().getAllSubscriptions()
        .collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var subscriptionToDelete by remember { mutableStateOf<Subscription?>(null) }
    var filterType by remember { mutableStateOf("Active") } // Active, Inactive, All
    
    val filteredSubscriptions = remember(subscriptions, filterType) {
        when (filterType) {
            "Active" -> subscriptions.filter { it.isActive }
            "Inactive" -> subscriptions.filter { !it.isActive }
            else -> subscriptions
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Active", "Inactive", "All").forEach { filter ->
                    FilterChip(
                        selected = filterType == filter,
                        onClick = { filterType = filter },
                        label = { Text(filter) }
                    )
                }
            }
            
            if (filteredSubscriptions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Subscriptions,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No subscriptions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Add your first subscription to start tracking",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSubscriptions, key = { it.id }) { subscription ->
                        SubscriptionCard(
                            subscription = subscription,
                            onEdit = { editingSubscription = subscription },
                            onDelete = {
                                subscriptionToDelete = subscription
                                showDeleteDialog = true
                            },
                            onToggleActive = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.subscriptionDao().updateSubscription(
                                            subscription.copy(isActive = !subscription.isActive)
                                        )
                                    }
                                    onRefresh()
                                }
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
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
            Icon(Icons.Default.Add, "Add Subscription")
        }
    }
    
    // Add/Edit Subscription Dialog
    if (showAddDialog || editingSubscription != null) {
        SubscriptionDialog(
            subscription = editingSubscription,
            onDismiss = {
                showAddDialog = false
                editingSubscription = null
            },
            onSave = { subscription ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (editingSubscription != null) {
                            database.subscriptionDao().updateSubscription(subscription)
                        } else {
                            database.subscriptionDao().insertSubscription(subscription)
                        }
                    }
                    showAddDialog = false
                    editingSubscription = null
                    onRefresh()
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && subscriptionToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Cancel Subscription?") },
            text = {
                Text("Are you sure you want to delete '${subscriptionToDelete!!.name}'? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.subscriptionDao().deleteSubscription(subscriptionToDelete!!)
                            }
                            showDeleteDialog = false
                            subscriptionToDelete = null
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
fun SubscriptionCard(
    subscription: Subscription,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val color = Color(subscription.color)
    val daysUntil = SubscriptionAnalytics.daysUntilBilling(subscription.nextBillingDate)
    val isUrgent = daysUntil in 0..3 && subscription.isActive
    val isOverdue = daysUntil < 0 && subscription.isActive
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (subscription.isActive) 
                MaterialTheme.colorScheme.surface
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isUrgent || isOverdue) androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (isOverdue) MaterialTheme.colorScheme.error else Color(0xFFFFAB40)
        ) else null
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
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subscription.icon,
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subscription.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (subscription.isActive) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (subscription.isTrial) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF9775FA).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "TRIAL",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9775FA),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = subscription.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (subscription.isActive) {
                        Text(
                            text = "Next: ${SubscriptionAnalytics.formatDateForDisplay(subscription.nextBillingDate)} (${SubscriptionAnalytics.getRelativeTimeDescription(daysUntil)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isOverdue -> MaterialTheme.colorScheme.error
                                isUrgent -> Color(0xFFFFAB40)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            }
                        )
                    } else {
                        Text(
                            text = "Inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                
                // Cost
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${subscription.currency}${String.format("%.2f", subscription.cost)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = Color(SubscriptionAnalytics.getBillingCycleColor(subscription.billingCycle)).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = subscription.billingCycle,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(SubscriptionAnalytics.getBillingCycleColor(subscription.billingCycle))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (subscription.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (subscription.isActive) "Pause" else "Resume")
                }
                
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDialog(
    subscription: Subscription?,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit
) {
    val context = LocalContext.current
    
    var name by remember { mutableStateOf(subscription?.name ?: "") }
    var cost by remember { mutableStateOf(subscription?.cost?.toString() ?: "") }
    var billingCycle by remember { mutableStateOf(subscription?.billingCycle ?: "Monthly") }
    var startDate by remember { mutableStateOf(subscription?.startDate ?: SubscriptionAnalytics.getTodayDateString()) }
    var nextBillingDate by remember { mutableStateOf(subscription?.nextBillingDate ?: SubscriptionAnalytics.getTodayDateString()) }
    var category by remember { mutableStateOf(subscription?.category ?: "Entertainment") }
    var selectedIcon by remember { mutableStateOf(subscription?.icon ?: "🎬") }
    var selectedColor by remember { mutableStateOf(subscription?.let { Color(it.color) } ?: Color(0xFFFF6B6B)) }
    var paymentMethod by remember { mutableStateOf(subscription?.paymentMethod ?: "Card") }
    var website by remember { mutableStateOf(subscription?.website ?: "") }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }
    var isTrial by remember { mutableStateOf(subscription?.isTrial ?: false) }
    var trialEndDate by remember { mutableStateOf(subscription?.trialEndDate ?: "") }
    
    val billingCycles = listOf("Weekly", "Monthly", "Quarterly", "Yearly")
    val paymentMethods = listOf("Card", "PayPal", "Bank Transfer", "UPI", "Other")
    
    val categoryIcons = mapOf(
        "Entertainment" to "🎬",
        "Productivity" to "💼",
        "Cloud Storage" to "☁️",
        "Music & Audio" to "🎵",
        "Gaming" to "🎮",
        "Education" to "📚",
        "Health & Fitness" to "💪",
        "News & Magazines" to "📰",
        "Communication" to "💬",
        "Security" to "🔒",
        "Design & Creative" to "🎨",
        "Other" to "📦"
    )
    
    val colors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4DABF7), Color(0xFF51CF66), Color(0xFFFFD93D),
        Color(0xFF9775FA), Color(0xFFFF6BCB), Color(0xFF3BC9DB), Color(0xFFFFAB40)
    )
    
    // Update icon when category changes ONLY if it's a new subscription or the icon matches the old category
    // This prevents overwriting manual icon selection when editing
    LaunchedEffect(category) {
        if (subscription == null || categoryIcons[subscription.category] == subscription.icon) {
            selectedIcon = categoryIcons[category] ?: "📦"
        }
    }
    
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
                        text = if (subscription != null) "Edit Subscription" else "New Subscription",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Service Name *") },
                        placeholder = { Text("e.g., Netflix, Spotify") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { 
                                val filtered = it.filter { char -> char.isDigit() || char == '.' }
                                val decimalCount = filtered.count { char -> char == '.' }
                                if (decimalCount <= 1) {
                                    cost = filtered
                                }
                            },
                            label = { Text("Cost *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Text("৳") },
                            singleLine = true
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = billingCycle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cycle") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                billingCycles.forEach { cycle ->
                                    DropdownMenuItem(
                                        text = { Text(cycle) },
                                        onClick = {
                                            billingCycle = cycle
                                            expanded = false
                                            // Auto-calculate next billing date from start date
                                            nextBillingDate = SubscriptionAnalytics.calculateNextBillingDate(startDate, cycle)
                                        }
                                    )
                                }
                            }
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
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = { Text(selectedIcon, fontSize = 20.sp) }
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoryIcons.forEach { (cat, icon) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(icon, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(cat)
                                        }
                                    },
                                    onClick = {
                                        category = cat
                                        selectedIcon = icon
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Text("Select Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
                
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePickerDialog(
                                    context = context,
                                    currentDate = LocalDate.parse(startDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                    onDateSelected = { newDate ->
                                        startDate = newDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                        // Recalculate next billing date based on new start date and current billing cycle
                                        nextBillingDate = SubscriptionAnalytics.calculateNextBillingDate(startDate, billingCycle)
                                    }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Start Date", style = MaterialTheme.typography.labelMedium)
                                Text(SubscriptionAnalytics.formatDateForDisplay(startDate))
                            }
                            Icon(Icons.Default.CalendarToday, null)
                        }
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTrial,
                            onCheckedChange = { isTrial = it }
                        )
                        Text("Free Trial")
                    }
                    
                    if (isTrial) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDatePickerDialog(
                                        context = context,
                                        currentDate = LocalDate.now(),
                                        onDateSelected = { newDate ->
                                            trialEndDate = newDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                        },
                                        minDate = false
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Trial End Date", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        if (trialEndDate.isNotEmpty()) 
                                            SubscriptionAnalytics.formatDateForDisplay(trialEndDate)
                                        else 
                                            "Select date"
                                    )
                                }
                                Icon(Icons.Default.CalendarToday, null)
                            }
                        }
                    }
                }
                
                item {
                    var paymentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = paymentExpanded,
                        onExpandedChange = { paymentExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentExpanded,
                            onDismissRequest = { paymentExpanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website (Optional)") },
                        placeholder = { Text("e.g., netflix.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
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
                                if (name.isNotBlank() && cost.toDoubleOrNull() != null) {
                                    val newSubscription = Subscription(
                                        id = subscription?.id ?: 0,
                                        name = name.trim(),
                                        cost = cost.toDouble(),
                                        billingCycle = billingCycle,
                                        startDate = startDate,
                                        nextBillingDate = nextBillingDate,
                                        category = category,
                                        icon = selectedIcon,
                                        color = selectedColor.toArgb().toLong() and 0xFFFFFFFFL,
                                        paymentMethod = paymentMethod,
                                        website = website.trim(),
                                        notes = notes.trim(),
                                        isActive = subscription?.isActive ?: true,
                                        isTrial = isTrial,
                                        trialEndDate = if (isTrial && trialEndDate.isNotEmpty()) trialEndDate else null,
                                        reminderDaysBefore = 3
                                    )
                                    onSave(newSubscription)
                                }
                            },
                            enabled = name.isNotBlank() && cost.toDoubleOrNull() != null
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

fun showDatePickerDialog(
    context: Context,
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    minDate: Boolean = true
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
        if (minDate) {
            datePicker.maxDate = System.currentTimeMillis()
        }
        show()
    }
}

@Composable
fun CalendarTab(database: TimerDatabase, refreshTrigger: Int) {
    val subscriptions by database.subscriptionDao().getActiveSubscriptions()
        .collectAsState(initial = emptyList())
    
    val upcomingRenewals = remember(subscriptions) {
        SubscriptionAnalytics.getUpcomingRenewals(subscriptions, 30)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        text = "Upcoming Renewals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${upcomingRenewals.size}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "in next 30 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        if (upcomingRenewals.isEmpty()) {
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
                            Icons.Default.Event,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No upcoming renewals",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(upcomingRenewals) { renewal ->
                RenewalCard(renewal = renewal)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RenewalCard(renewal: SubscriptionAnalytics.UpcomingRenewal) {
    val subscription = renewal.subscription
    val color = Color(subscription.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (renewal.isUrgent) androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = Color(0xFFFFAB40)
        ) else null
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
                Text(text = subscription.icon, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = SubscriptionAnalytics.formatDateForDisplay(subscription.nextBillingDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = SubscriptionAnalytics.getRelativeTimeDescription(renewal.daysUntil),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (renewal.isUrgent) Color(0xFFFFAB40) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = if (renewal.isUrgent) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${subscription.currency}${String.format("%.2f", subscription.cost)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subscription.billingCycle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SubscriptionStatsTab(database: TimerDatabase, refreshTrigger: Int) {
    val subscriptions by database.subscriptionDao().getAllSubscriptions()
        .collectAsState(initial = emptyList())
    
    val stats = remember(subscriptions) {
        SubscriptionAnalytics.calculateStats(subscriptions)
    }
    
    val categorySpending = remember(subscriptions) {
        SubscriptionAnalytics.calculateCategorySpending(subscriptions)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Cost Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Monthly Total",
                    value = SubscriptionAnalytics.formatCurrency(stats.totalMonthly),
                    icon = Icons.Default.CalendarMonth,
                    color = Color(0xFF51CF66),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Yearly Total",
                    value = SubscriptionAnalytics.formatCurrency(stats.totalYearly),
                    icon = Icons.Default.CalendarViewMonth,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    title = "Active",
                    value = "${stats.totalActive}",
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Avg/Sub",
                    value = SubscriptionAnalytics.formatCurrency(stats.avgCostPerSub),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    title = "Due This Week",
                    value = "${stats.upcomingRenewalsCount}",
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Inactive",
                    value = "${stats.totalInactive}",
                    modifier = Modifier.weight(1f)
                )
            }
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
                CategorySpendingCard(spending = spending)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CategorySpendingCard(spending: SubscriptionAnalytics.CategorySpending) {
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
                Text(
                    text = spending.icon,
                    fontSize = 32.sp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spending.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${spending.count} subscription${if (spending.count > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = SubscriptionAnalytics.formatCurrency(spending.monthlyTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${spending.percentage.toInt()}% of total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { spending.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

