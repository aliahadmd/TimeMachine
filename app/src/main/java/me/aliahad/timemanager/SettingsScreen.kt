package me.aliahad.timemanager

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.data.TimerDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TimerDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Message, isSuccess
    
    // File picker for export (create document)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    // Export data
                    val exportResult = DataBackupManager.exportData(context, database)
                    if (exportResult.isSuccess) {
                        val backupData = exportResult.getOrNull()!!
                        val saveResult = DataBackupManager.saveBackupToFile(context, uri, backupData)
                        if (saveResult.isSuccess) {
                            operationMessage = "✅ Data exported successfully!" to true
                        } else {
                            operationMessage = "❌ Failed to save backup file" to false
                        }
                    } else {
                        operationMessage = "❌ Failed to export data" to false
                    }
                } catch (e: Exception) {
                    operationMessage = "❌ Error: ${e.message}" to false
                } finally {
                    isExporting = false
                }
            }
        }
    }
    
    // File picker for import (select document)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isImporting = true
                showImportDialog = false
                try {
                    val importResult = DataBackupManager.importData(context, uri, database)
                    if (importResult.isSuccess) {
                        val stats = importResult.getOrNull()!!
                        operationMessage = stats.getDetailedMessage() to true
                    } else {
                        val error = importResult.exceptionOrNull()
                        operationMessage = "❌ Import Failed\n${error?.message ?: "Unknown error"}" to false
                    }
                } catch (e: Exception) {
                    operationMessage = "❌ Import Error\n${e.message}" to false
                } finally {
                    isImporting = false
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Data Management
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Upload,
                    title = "Export Data",
                    subtitle = "Save backup to file",
                    iconColor = Color(0xFF4CAF50),
                    enabled = !isExporting && !isImporting && !isClearing,
                    onClick = { showExportDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Download,
                    title = "Import Data",
                    subtitle = "Restore from backup file",
                    iconColor = Color(0xFF2196F3),
                    enabled = !isExporting && !isImporting && !isClearing,
                    onClick = { showImportDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Data",
                    subtitle = "Delete all app data (⚠️ Cannot be undone)",
                    iconColor = Color(0xFFF44336),
                    enabled = !isExporting && !isImporting && !isClearing,
                    onClick = { showClearDataDialog = true }
                )
            }
            
            // Section: App Info
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Info,
                    title = "App Information",
                    subtitle = "Version 2.0.0",
                    iconColor = Color(0xFF9C27B0),
                    onClick = { showAboutDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Storage,
                    title = "Database Version",
                    subtitle = "Version 11",
                    iconColor = Color(0xFFFF9800),
                    onClick = { }
                )
            }
            
            // Loading/Operation Feedback
            if (isExporting || isImporting || isClearing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = when {
                                    isExporting -> "Exporting data..."
                                    isImporting -> "Importing data..."
                                    isClearing -> "Clearing all data..."
                                    else -> "Processing..."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Operation Result Message
            operationMessage?.let { (message, isSuccess) ->
                item {
                    // Auto-dismiss after 5 seconds for success messages
                    if (isSuccess) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(5000)
                            operationMessage = null
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) {
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            } else {
                                Color(0xFFF44336).copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { operationMessage = null }) {
                                Icon(Icons.Default.Close, "Dismiss")
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Export Confirmation Dialog
    if (showExportDialog) {
        ExportConfirmationDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = {
                showExportDialog = false
                exportLauncher.launch(DataBackupManager.generateBackupFileName())
            }
        )
    }
    
    // Import Warning Dialog
    if (showImportDialog) {
        ImportWarningDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = {
                showImportDialog = false
                importLauncher.launch(arrayOf("application/json"))
            }
        )
    }
    
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = {
                showClearDataDialog = false
                scope.launch {
                    isClearing = true
                    try {
                        withContext(Dispatchers.IO) {
                            database.clearAllTables()
                        }
                        operationMessage = "✅ All data cleared successfully!" to true
                    } catch (e: Exception) {
                        operationMessage = "❌ Failed to clear data: ${e.message}" to false
                    } finally {
                        isClearing = false
                    }
                }
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (enabled) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Action",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun ExportConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Upload,
                contentDescription = "Export",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Export Your Data?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This will create a backup file containing:")
                
                val items = listOf(
                    "• Your profile settings",
                    "• Focus tracker sessions & categories",
                    "• Habits & completions",
                    "• Expenses & categories",
                    "• Subscriptions",
                    "• Daily planner tasks",
                    "• Date calculations",
                    "• BMI records"
                )
                
                items.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The file will be saved in JSON format.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Import Data Warning",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⚠️ Important:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                
                Text("Importing data will:")
                
                val warnings = listOf(
                    "• Merge with existing data",
                    "• Potentially create duplicate entries",
                    "• Cannot be undone automatically"
                )
                
                warnings.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Tip: Export current data first as a safety backup!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("I Understand, Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = "Clear Data",
                tint = Color(0xFFF44336),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Clear All Data?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⚠️ CRITICAL WARNING:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                
                Text("This will permanently delete ALL app data:")
                
                val warnings = listOf(
                    "• User profile",
                    "• All focus tracker sessions",
                    "• All habits and completions",
                    "• All expenses and categories",
                    "• All subscriptions",
                    "• All daily planner tasks",
                    "• All date calculations",
                    "• All BMI records"
                )
                
                warnings.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⛔ THIS CANNOT BE UNDONE!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                
                Text(
                    text = "💡 Tip: Export your data first if you want to keep a backup!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Yes, Delete Everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⏰",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "TimeManager",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 2.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                item {
                    HorizontalDivider()
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val features = listOf(
                            "🎯 Focus Tracker",
                            "✅ Habit Tracker",
                            "💰 Expense Tracker",
                            "📱 Subscription Tracker",
                            "📋 Daily Planner",
                            "📅 Year Calculator",
                            "⚖️ BMI Calculator",
                            "👤 User Profile"
                        )
                        
                        features.forEach { feature ->
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Database",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Version: 11",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Storage: Local (Room Database)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Backup: JSON Export/Import",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                item {
                    HorizontalDivider()
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Developer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ali Ahad",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "🌐 aliahad.me",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "📧 ali@aliahad.me",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                item {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

