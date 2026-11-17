package me.aliahad.timemanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.timemanager.SubscriptionAnalytics
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.SafeDateTime
import me.aliahad.timemanager.core.common.toComposeColor
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.core.di.appViewModel
import me.aliahad.timemanager.data.Expense
import me.aliahad.timemanager.data.ExpenseCategory
import me.aliahad.timemanager.data.Subscription
import me.aliahad.timemanager.domain.usecase.BudgetAlertLevel
import me.aliahad.timemanager.ui.components.ConfirmDeleteDialog
import me.aliahad.timemanager.ui.components.EmojiAndColorPicker
import me.aliahad.timemanager.ui.components.EmptyStateView
import me.aliahad.timemanager.ui.components.M3DatePickerModal
import me.aliahad.timemanager.ui.components.SectionCard
import me.aliahad.timemanager.ui.components.TimeMachineTopBar
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import me.aliahad.timemanager.ui.theme.TimeMachineTheme
import me.aliahad.timemanager.ui.viewmodel.ExpenseRangeFilter
import me.aliahad.timemanager.ui.viewmodel.FinanceViewModel
import java.time.LocalDate

@Composable
fun FinanceScreen(initialTab: Int = 0) {
    val viewModel = appViewModel { container -> FinanceViewModel(container) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    var showCategoryManager by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showCategoryEditor by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<ExpenseCategory?>(null) }

    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    var subscriptionToDelete by remember { mutableStateOf<Subscription?>(null) }
    var subscriptionToRenew by remember { mutableStateOf<Subscription?>(null) }

    LaunchedEffect(state.lastBudgetAlert) {
        val alert = state.lastBudgetAlert ?: return@LaunchedEffect
        val label = if (alert.alertLevel == BudgetAlertLevel.EXCEEDED) {
            "Budget exceeded for ${alert.categoryName}: ${Formatters.formatCurrency(alert.spent, state.currencySymbol)} / ${Formatters.formatCurrency(alert.budget, state.currencySymbol)}"
        } else {
            "80% budget reached for ${alert.categoryName}"
        }
        snackbarHostState.showSnackbar(label)
        viewModel.clearBudgetAlert()
    }

    val statusColors = TimeMachineTheme.statusColors
    val sym = state.currencySymbol
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TimeMachineTopBar(
                title = "Finance",
                subtitle = "This month: ${Formatters.formatCurrency(state.monthSpent, sym)} • Subs: ${Formatters.formatCurrency(state.subscriptionStats.totalMonthly, sym)}/mo",
                actions = {
                    IconButton(onClick = { showCategoryManager = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Categories & Budgets")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 2) {
                        editingSubscription = null
                        showSubscriptionDialog = true
                    } else {
                        editingExpense = null
                        showExpenseDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 2) "Add Subscription" else "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("Overview", "Expenses", "Subs", "Analytics").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, maxLines = 1) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item(key = "overview_hero") {
                            val combinedMonthly = state.monthSpent + state.subscriptionStats.totalMonthly
                            SectionCard {
                                Text(
                                    text = "MONTHLY COMMITMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColors.financeAccent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(combinedMonthly, sym),
                                    style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Expenses ${Formatters.formatCurrency(state.monthSpent, sym)} + Active Subs ${Formatters.formatCurrency(state.subscriptionStats.totalMonthly, sym)}/mo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Today spent: ${Formatters.formatCurrency(state.todaySpent, sym)} • ${state.activeSubscriptions.size} active subscriptions",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                if (state.subscriptionStats.hasMixedCurrencies) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val breakdownText = state.subscriptionStats.monthlyByCurrency.entries
                                        .joinToString(" • ") { (curr, total) ->
                                            "${Formatters.formatCurrency(total, curr)}/mo"
                                        }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = statusColors.warningContainer.copy(alpha = 0.45f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = statusColors.warning,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Multi-currency subs ($breakdownText)",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "budgets_card") {
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Monthly Budgets", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { showCategoryManager = true }) {
                                        Text("Configure")
                                    }
                                }
                                if (state.budgetStatuses.isEmpty()) {
                                    Text(
                                        text = "Set a monthly budget on any expense category to receive 80% & 100% spend alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.budgetStatuses.forEach { status ->
                                            val barColor = when {
                                                status.isOverBudget -> statusColors.danger
                                                status.percentageUsed >= 80f -> statusColors.warning
                                                else -> statusColors.financeAccent
                                            }
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "${status.category.icon} ${status.category.name}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        "${Formatters.formatCurrency(status.spent, sym)} / ${Formatters.formatCurrency(status.budget, sym)}",
                                                        style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                                        color = barColor
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { (status.percentageUsed / 100f).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(7.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    color = barColor,
                                                    strokeCap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "upcoming_renewals_overview") {
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Due & Overdue Renewals", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { selectedTab = 2 }) {
                                        Text("All Subs")
                                    }
                                }
                                val urgent = state.renewalCalendar.filter { it.daysUntil <= 7 }
                                if (urgent.isEmpty()) {
                                    Text(
                                        text = "No renewals or trials due within 7 days.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        urgent.forEach { renewal ->
                                            val sub = renewal.subscription
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${sub.icon} ${sub.name}", style = MaterialTheme.typography.titleSmall)
                                                    val dueLabel = when {
                                                        renewal.isOverdue -> "${-renewal.daysUntil}d overdue"
                                                        renewal.daysUntil == 0L -> "Due today"
                                                        renewal.daysUntil == 1L -> "Tomorrow"
                                                        else -> "In ${renewal.daysUntil}d"
                                                    }
                                                    Text(
                                                        text = "$dueLabel • ${Formatters.formatCurrency(sub.cost, sub.currency.ifBlank { sym })}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (renewal.isOverdue || renewal.daysUntil <= 1) statusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = { subscriptionToRenew = sub },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Renew")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item(key = "expense_filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    placeholder = { Text("Search expenses or payment method...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExpenseRangeFilter.entries.forEach { range ->
                                        FilterChip(
                                            selected = state.rangeFilter == range,
                                            onClick = { viewModel.setRangeFilter(range) },
                                            label = { Text(range.label) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = state.selectedCategoryId == null,
                                        onClick = { viewModel.selectCategoryFilter(null) },
                                        label = { Text("All Categories") }
                                    )
                                    state.categories.forEach { cat ->
                                        FilterChip(
                                            selected = state.selectedCategoryId == cat.id,
                                            onClick = {
                                                viewModel.selectCategoryFilter(
                                                    if (state.selectedCategoryId == cat.id) null else cat.id
                                                )
                                            },
                                            label = { Text("${cat.icon} ${cat.name}") }
                                        )
                                    }
                                }
                            }
                        }

                        if (state.filteredExpenses.isEmpty()) {
                            item(key = "empty_expenses") {
                                EmptyStateView(
                                    emoji = "💸",
                                    title = "No Expenses Found",
                                    subtitle = "Tap + to log an expense or adjust your filters.",
                                    actionLabel = "Add Expense",
                                    onAction = {
                                        editingExpense = null
                                        showExpenseDialog = true
                                    }
                                )
                            }
                        } else {
                            items(state.filteredExpenses, key = { it.id }) { expense ->
                                val cat = catMap[expense.categoryId]
                                SectionCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat?.icon ?: "💳", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = expense.description.ifBlank { cat?.name ?: "Expense" },
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = buildString {
                                                    append(cat?.name ?: "Other")
                                                    append(" • ${SafeDateTime.formatRelativeDate(expense.date)}")
                                                    append(" • ${expense.paymentMethod}")
                                                    if (expense.subscriptionId != null) append(" • Auto-Renew")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatCurrency(expense.amount, sym),
                                            style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = {
                                            editingExpense = expense
                                            showExpenseDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { expenseToDelete = expense }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (state.subscriptions.isEmpty()) {
                            item(key = "empty_subs") {
                                EmptyStateView(
                                    emoji = "🔄",
                                    title = "No Subscriptions Yet",
                                    subtitle = "Track streaming, SaaS tools, memberships, and free trials so renewals never surprise you.",
                                    actionLabel = "Add Subscription",
                                    onAction = {
                                        editingSubscription = null
                                        showSubscriptionDialog = true
                                    }
                                )
                            }
                        } else {
                            items(state.subscriptions, key = { it.id }) { sub ->
                                val subCurrency = sub.currency.ifBlank { CurrencyCatalog.symbolFor(sub.currencyCode) }
                                val daysLeft = SubscriptionAnalytics.daysUntilBilling(sub.nextBillingDate)
                                val statusColor = when {
                                    !sub.isActive -> MaterialTheme.colorScheme.onSurfaceVariant
                                    daysLeft < 0 -> statusColors.danger
                                    daysLeft <= sub.reminderDaysBefore -> statusColors.warning
                                    else -> statusColors.financeAccent
                                }

                                SectionCard(borderColor = if (sub.isActive && daysLeft <= 1) statusColor.copy(alpha = 0.5f) else statusColors.cardBorder) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sub.icon, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (!sub.isActive) {
                                                    Text("PAUSED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                } else if (sub.isTrial) {
                                                    Text("TRIAL", style = MaterialTheme.typography.labelSmall, color = statusColors.warning)
                                                }
                                            }
                                            Text(
                                                text = "${Formatters.formatCurrency(sub.cost, subCurrency)} / ${sub.billingCycle} (${sub.currencyCode})",
                                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = when {
                                                    !sub.isActive -> "Paused"
                                                    daysLeft < 0 -> "${-daysLeft}d overdue (${sub.nextBillingDate})"
                                                    daysLeft == 0L -> "Due today (${sub.nextBillingDate})"
                                                    else -> "Next billing: ${SafeDateTime.formatRelativeDate(sub.nextBillingDate)} (${daysLeft}d)"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = statusColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (sub.isActive) {
                                                OutlinedButton(
                                                    onClick = { subscriptionToRenew = sub },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Renew")
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.toggleSubscriptionActive(sub) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    if (sub.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (sub.isActive) "Pause" else "Resume")
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = {
                                                editingSubscription = sub
                                                showSubscriptionDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { subscriptionToDelete = sub }) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item(key = "analytics_summary") {
                            SectionCard {
                                Text("Spending Summary (${state.rangeFilter.label})", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.formatCurrency(state.filteredStats.totalExpenses, sym), style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle))
                                    }
                                    Column {
                                        Text("Avg/Day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.formatCurrency(state.filteredStats.averagePerDay, sym), style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle))
                                    }
                                    Column {
                                        Text("Txns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(state.filteredStats.transactionCount.toString(), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }

                        item(key = "seven_day_trend") {
                            SectionCard {
                                Text("Last 7 Days", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                val trend = state.dailyTrend
                                val maxVal = (trend.maxOfOrNull { it.total } ?: 0.0).coerceAtLeast(1.0)
                                val barColor = MaterialTheme.colorScheme.primary
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    trend.forEach { day ->
                                        val frac = (day.total / maxVal).toFloat().coerceIn(0f, 1f)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val h = (size.height * frac).coerceAtLeast(if (day.total > 0) 6.dp.toPx() else 2.dp.toPx())
                                                    drawRoundRect(
                                                        color = if (day.total > 0) barColor else barColor.copy(alpha = 0.2f),
                                                        topLeft = Offset(size.width * 0.2f, size.height - h),
                                                        size = Size(size.width * 0.6f, h),
                                                        cornerRadius = CornerRadius(6.dp.toPx())
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = day.date.takeLast(2),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.categoryBreakdown.isNotEmpty()) {
                            item(key = "cat_breakdown") {
                                SectionCard {
                                    Text("Category Share", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.categoryBreakdown.forEach { item ->
                                            val catColor = item.category.color.toComposeColor()
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("${item.category.icon} ${item.category.name}")
                                                    Text(
                                                        "${Formatters.formatCurrency(item.total, sym)} (${item.percentage.toInt()}%)",
                                                        style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = catColor,
                                                    strokeCap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        ExpenseEditorDialog(
            categories = state.categories,
            currencySymbol = sym,
            existing = editingExpense,
            onDismiss = { showExpenseDialog = false },
            onSave = { exp, isEdit ->
                viewModel.saveExpense(exp, isEdit)
                showExpenseDialog = false
            }
        )
    }

    if (showCategoryManager) {
        AlertDialog(
            onDismissRequest = { showCategoryManager = false },
            title = { Text("Expense Categories & Budgets") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingCategory = null
                            showCategoryEditor = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Category")
                    }
                    state.categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${cat.icon} ${cat.name}", modifier = Modifier.weight(1f))
                            if (cat.budget > 0) {
                                Text(
                                    Formatters.formatCurrency(cat.budget, sym),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                editingCategory = cat
                                showCategoryEditor = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            if (state.categories.size > 1) {
                                IconButton(onClick = { categoryToDelete = cat }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryManager = false }) { Text("Close") }
            }
        )
    }

    if (showCategoryEditor) {
        ExpenseCategoryEditorDialog(
            existing = editingCategory,
            currencySymbol = sym,
            onDismiss = { showCategoryEditor = false },
            onSave = { cat, isEdit ->
                viewModel.saveExpenseCategory(cat, isEdit)
                showCategoryEditor = false
            }
        )
    }

    if (showSubscriptionDialog) {
        SubscriptionEditorDialog(
            existing = editingSubscription,
            defaultCurrencyCode = state.currencyCode,
            expenseCategories = state.categories,
            onDismiss = { showSubscriptionDialog = false },
            onSave = { sub, isEdit ->
                viewModel.saveSubscription(sub, isEdit)
                showSubscriptionDialog = false
            }
        )
    }

    subscriptionToRenew?.let { sub ->
        val subSym = sub.currency.ifBlank { CurrencyCatalog.symbolFor(sub.currencyCode) }
        AlertDialog(
            onDismissRequest = { subscriptionToRenew = null },
            title = { Text("Renew ${sub.name}?") },
            text = {
                Text(
                    "Advance next billing date from ${sub.nextBillingDate} (${sub.billingCycle}). Also log ${Formatters.formatCurrency(sub.cost, subSym)} as an Expense today?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renewSubscription(sub, logExpense = true)
                    subscriptionToRenew = null
                }) { Text("Renew & Log Expense") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { subscriptionToRenew = null }) { Text("Cancel") }
                    TextButton(onClick = {
                        viewModel.renewSubscription(sub, logExpense = false)
                        subscriptionToRenew = null
                    }) { Text("Renew Only") }
                }
            }
        )
    }

    expenseToDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = "Delete Expense?",
            message = "Delete ${Formatters.formatCurrency(pending.amount, sym)} (${pending.description.ifBlank { "Expense" }})?",
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                viewModel.deleteExpense(pending)
                expenseToDelete = null
            }
        )
    }

    subscriptionToDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = "Delete Subscription?",
            message = "Remove ${pending.name} permanently?",
            onDismiss = { subscriptionToDelete = null },
            onConfirm = {
                viewModel.deleteSubscription(pending)
                subscriptionToDelete = null
            }
        )
    }

    categoryToDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = "Delete Expense Category?",
            message = "Deleting \"${pending.name}\" also deletes expenses assigned to it.",
            onDismiss = { categoryToDelete = null },
            onConfirm = {
                viewModel.deleteExpenseCategory(pending)
                categoryToDelete = null
            }
        )
    }
}

@Composable
private fun ExpenseEditorDialog(
    categories: List<ExpenseCategory>,
    currencySymbol: String,
    existing: Expense?,
    onDismiss: () -> Unit,
    onSave: (Expense, Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: categories.firstOrNull()?.id ?: 0L) }
    var dateIso by remember { mutableStateOf(existing?.date ?: LocalDate.now().format(SafeDateTime.ISO_DATE)) }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: "Card") }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Expense" else "Edit Expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = cat.id == categoryId,
                            onClick = { categoryId = cat.id },
                            label = { Text("${cat.icon} ${cat.name}") }
                        )
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: $dateIso")
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Card", "Cash", "Online", "Other").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = amount != null && amount > 0 && categoryId > 0L,
                onClick = {
                    onSave(
                        Expense(
                            id = existing?.id ?: 0L,
                            categoryId = categoryId,
                            amount = amount ?: 0.0,
                            description = description.trim(),
                            date = dateIso,
                            timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                            paymentMethod = paymentMethod,
                            subscriptionId = existing?.subscriptionId
                        ),
                        existing != null
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        M3DatePickerModal(
            initialDate = SafeDateTime.parseLocalDateOrNull(dateIso) ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { dateIso = it.format(SafeDateTime.ISO_DATE) }
        )
    }
}

@Composable
private fun ExpenseCategoryEditorDialog(
    existing: ExpenseCategory?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var budgetText by remember {
        mutableStateOf(if (existing != null && existing.budget > 0) existing.budget.toString() else "")
    }
    var icon by remember { mutableStateOf(existing?.icon ?: "🛍️") }
    var colorLong by remember { mutableStateOf(existing?.color ?: Color(0xFF059669).toStorageLong()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Expense Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly Budget Limit ($currencySymbol, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                EmojiAndColorPicker(
                    emojis = listOf("🍽️", "🚕", "🛒", "💳", "🛍️", "⚡", "🏥", "🎮", "✈️", "📦"),
                    selectedEmoji = icon,
                    onEmojiSelected = { icon = it },
                    selectedColorLong = colorLong,
                    onColorSelected = { colorLong = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val budget = budgetText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    onSave(
                        ExpenseCategory(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            icon = icon,
                            color = colorLong,
                            budget = budget,
                            isActive = true
                        ),
                        existing != null
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SubscriptionEditorDialog(
    existing: Subscription?,
    defaultCurrencyCode: String,
    expenseCategories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onSave: (Subscription, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var costText by remember { mutableStateOf(existing?.cost?.toString() ?: "") }
    var currencyCode by remember { mutableStateOf(existing?.currencyCode ?: defaultCurrencyCode) }
    var billingCycle by remember { mutableStateOf(existing?.billingCycle ?: "Monthly") }
    var startDateIso by remember { mutableStateOf(existing?.startDate ?: LocalDate.now().format(SafeDateTime.ISO_DATE)) }
    var nextBillingIso by remember { mutableStateOf(existing?.nextBillingDate ?: LocalDate.now().plusMonths(1).format(SafeDateTime.ISO_DATE)) }
    var category by remember { mutableStateOf(existing?.category ?: "Entertainment") }
    var icon by remember { mutableStateOf(existing?.icon ?: "🎬") }
    var colorLong by remember { mutableStateOf(existing?.color ?: Color(0xFF7C3AED).toStorageLong()) }
    var isTrial by remember { mutableStateOf(existing?.isTrial ?: false) }
    var trialEndIso by remember { mutableStateOf(existing?.trialEndDate ?: LocalDate.now().plusDays(7).format(SafeDateTime.ISO_DATE)) }
    var reminderDays by remember { mutableIntStateOf(existing?.reminderDaysBefore ?: 3) }
    var autoLogExpense by remember { mutableStateOf(existing?.autoLogExpense ?: true) }
    var expenseCatId by remember { mutableStateOf(existing?.expenseCategoryId ?: expenseCategories.firstOrNull()?.id) }

    var pickNextDate by remember { mutableStateOf(false) }
    var pickTrialDate by remember { mutableStateOf(false) }

    val cost = costText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Subscription" else "Edit Subscription") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Cost (${CurrencyCatalog.symbolFor(currencyCode)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Currency", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyCatalog.ALL.forEach { spec ->
                        FilterChip(
                            selected = spec.code == currencyCode,
                            onClick = { currencyCode = spec.code },
                            label = { Text("${spec.code} (${spec.symbol})") }
                        )
                    }
                }
                Text("Billing Cycle", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Weekly", "Monthly", "Quarterly", "Yearly").forEach { cycle ->
                        FilterChip(
                            selected = billingCycle == cycle,
                            onClick = { billingCycle = cycle },
                            label = { Text(cycle) }
                        )
                    }
                }
                OutlinedButton(onClick = { pickNextDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Next Billing: $nextBillingIso")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Free Trial")
                    Switch(checked = isTrial, onCheckedChange = { isTrial = it })
                }
                if (isTrial) {
                    OutlinedButton(onClick = { pickTrialDate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Trial Ends: $trialEndIso")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-log Expense on Renew")
                    Switch(checked = autoLogExpense, onCheckedChange = { autoLogExpense = it })
                }
                Text("Remind Before Billing", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 7).forEach { days ->
                        FilterChip(
                            selected = reminderDays == days,
                            onClick = { reminderDays = days },
                            label = { Text("${days}d") }
                        )
                    }
                }
                EmojiAndColorPicker(
                    emojis = SubscriptionAnalytics.DEFAULT_CATEGORIES.map { it.second },
                    selectedEmoji = icon,
                    onEmojiSelected = { icon = it },
                    selectedColorLong = colorLong,
                    onColorSelected = { colorLong = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && cost != null && cost >= 0.0,
                onClick = {
                    val spec = CurrencyCatalog.byCode(currencyCode)
                    onSave(
                        Subscription(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            cost = cost ?: 0.0,
                            currency = spec.symbol,
                            currencyCode = spec.code,
                            billingCycle = billingCycle,
                            startDate = startDateIso,
                            nextBillingDate = nextBillingIso,
                            category = category,
                            icon = icon,
                            color = colorLong,
                            paymentMethod = existing?.paymentMethod ?: "Card",
                            website = existing?.website ?: "",
                            notes = existing?.notes ?: "",
                            isActive = existing?.isActive ?: true,
                            isTrial = isTrial,
                            trialEndDate = if (isTrial) trialEndIso else null,
                            reminderDaysBefore = reminderDays,
                            expenseCategoryId = expenseCatId,
                            autoLogExpense = autoLogExpense
                        ),
                        existing != null
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickNextDate) {
        M3DatePickerModal(
            initialDate = SafeDateTime.parseLocalDateOrNull(nextBillingIso) ?: LocalDate.now(),
            onDismiss = { pickNextDate = false },
            onDateSelected = { nextBillingIso = it.format(SafeDateTime.ISO_DATE) }
        )
    }
    if (pickTrialDate) {
        M3DatePickerModal(
            initialDate = SafeDateTime.parseLocalDateOrNull(trialEndIso) ?: LocalDate.now(),
            onDismiss = { pickTrialDate = false },
            onDateSelected = { trialEndIso = it.format(SafeDateTime.ISO_DATE) }
        )
    }
}
