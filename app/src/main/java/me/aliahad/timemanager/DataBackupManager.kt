package me.aliahad.timemanager

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.aliahad.timemanager.data.*
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object DataBackupManager {
    
    private const val TAG = "DataBackupManager"
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Export all data to JSON
     */
    suspend fun exportData(context: Context, database: TimerDatabase): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting data export...")
            
            val backupData = BackupData(
                version = 1,
                timestamp = System.currentTimeMillis(),
                profile = exportProfile(database),
                focusTracker = exportFocusTracker(database),
                habitTracker = exportHabitTracker(database),
                expenseTracker = exportExpenseTracker(database),
                subscriptionTracker = exportSubscriptionTracker(database),
                dailyPlanner = exportDailyPlanner(database),
                yearCalculator = exportYearCalculator(database),
                bmiCalculator = exportBMICalculator(database)
            )
            
            Log.d(TAG, "Data export completed successfully")
            Result.success(backupData)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save backup data to file
     */
    suspend fun saveBackupToFile(context: Context, uri: Uri, backupData: BackupData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(backupData)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
                outputStream.flush()
            }
            Log.d(TAG, "Backup saved to file successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving backup to file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import data from JSON
     */
    suspend fun importData(context: Context, uri: Uri, database: TimerDatabase): Result<ImportStats> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting data import...")
            
            // Read and validate file
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Log.d(TAG, "File size: ${bytes.size} bytes")
                
                // Warn if file is too large (> 10MB)
                if (bytes.size > 10 * 1024 * 1024) {
                    Log.w(TAG, "Large file detected: ${bytes.size / 1024 / 1024}MB")
                }
                
                bytes.decodeToString()
            } ?: return@withContext Result.failure(Exception("Failed to read file"))
            
            // Parse JSON
            val backupData = try {
                json.decodeFromString<BackupData>(jsonString)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Invalid backup file format: ${e.message}"))
            }
            
            // Validate backup version
            if (backupData.version != 1) {
                return@withContext Result.failure(Exception("Unsupported backup version: ${backupData.version}"))
            }
            
            Log.d(TAG, "Backup file validated successfully")
            
            val stats = ImportStats()
            
            // Import in order to respect foreign key constraints
            backupData.profile?.let { 
                importProfile(database, it)
                stats.profileImported = true
            }
            
            stats.categoriesImported = importFocusTracker(database, backupData.focusTracker)
            stats.habitsImported = importHabitTracker(database, backupData.habitTracker)
            stats.expensesImported = importExpenseTracker(database, backupData.expenseTracker)
            stats.subscriptionsImported = importSubscriptionTracker(database, backupData.subscriptionTracker)
            stats.tasksImported = importDailyPlanner(database, backupData.dailyPlanner)
            stats.dateCalculationsImported = importYearCalculator(database, backupData.yearCalculator)
            stats.bmiCalculationsImported = importBMICalculator(database, backupData.bmiCalculator)
            
            Log.d(TAG, "Data import completed: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing data", e)
            Result.failure(Exception("Import failed: ${e.message}"))
        }
    }
    
    /**
     * Generate default backup filename
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "TimeManager_Backup_$timestamp.json"
    }
    
    // Export functions for each module
    
    private suspend fun exportProfile(database: TimerDatabase): UserProfileBackup? {
        return try {
            database.userProfileDao().getProfileSync()?.let { profile ->
                UserProfileBackup(
                    id = profile.id,
                    name = profile.name,
                    email = profile.email,
                    bio = profile.bio,
                    avatarIcon = profile.avatarIcon,
                    joinDate = profile.joinDate,
                    dailyGoalMinutes = profile.dailyGoalMinutes,
                    theme = profile.theme,
                    currency = profile.currency,
                    notificationsEnabled = profile.notificationsEnabled,
                    weekStartDay = profile.weekStartDay,
                    createdAt = profile.createdAt,
                    updatedAt = profile.updatedAt
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting profile", e)
            null
        }
    }
    
    private suspend fun exportFocusTracker(database: TimerDatabase): FocusTrackerBackup {
        return try {
            val categories = database.activityCategoryDao().getAllCategoriesSync().map { category ->
                CategoryBackup(
                    id = category.id,
                    name = category.name,
                    icon = category.icon,
                    colorLong = category.color,
                    dailyGoalMinutes = category.dailyGoalMinutes,
                    isActive = category.isActive,
                    sortOrder = category.sortOrder,
                    createdAt = category.createdAt,
                    updatedAt = category.updatedAt
                )
            }
            
            val sessions = database.timeSessionDao().getAllSessionsSync().map { session ->
                SessionBackup(
                    id = session.id,
                    categoryId = session.categoryId,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    durationMinutes = session.durationMinutes,
                    date = session.date
                )
            }
            
            FocusTrackerBackup(categories, sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting focus tracker", e)
            FocusTrackerBackup()
        }
    }
    
    private suspend fun exportHabitTracker(database: TimerDatabase): HabitTrackerBackup {
        return try {
            val habits = database.habitDao().getAllHabitsSync().map { habit ->
                HabitBackup(
                    id = habit.id,
                    name = habit.name,
                    description = habit.description,
                    colorLong = habit.color,
                    iconName = habit.iconName,
                    habitType = habit.type.name,
                    goalPeriodDays = habit.goalPeriodDays,
                    isEveryday = habit.isEveryday,
                    reminderTimeHour = habit.reminderTimeHour,
                    reminderTimeMinute = habit.reminderTimeMinute,
                    isActive = habit.isActive,
                    createdAt = habit.createdAt
                )
            }
            
            val completions = database.habitDao().getAllCompletionsSync().map { completion ->
                HabitCompletionBackup(
                    habitId = completion.habitId,
                    date = completion.date,
                    completionType = completion.completionType.name,
                    completedAt = completion.completedAt,
                    notes = completion.notes
                )
            }
            
            HabitTrackerBackup(habits, completions)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting habit tracker", e)
            HabitTrackerBackup()
        }
    }
    
    private suspend fun exportExpenseTracker(database: TimerDatabase): ExpenseTrackerBackup {
        return try {
            val categories = database.expenseCategoryDao().getAllCategoriesSync().map { category ->
                ExpenseCategoryBackup(
                    id = category.id,
                    name = category.name,
                    icon = category.icon,
                    colorLong = category.color,
                    budget = category.budget,
                    isActive = category.isActive
                )
            }
            
            val expenses = database.expenseDao().getAllExpensesSync().map { expense ->
                ExpenseBackup(
                    id = expense.id,
                    amount = expense.amount,
                    categoryId = expense.categoryId,
                    description = expense.description,
                    date = expense.date,
                    timestamp = expense.timestamp,
                    paymentMethod = expense.paymentMethod
                )
            }
            
            ExpenseTrackerBackup(categories, expenses)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting expense tracker", e)
            ExpenseTrackerBackup()
        }
    }
    
    private suspend fun exportSubscriptionTracker(database: TimerDatabase): SubscriptionTrackerBackup {
        return try {
            val subscriptions = database.subscriptionDao().getAllSubscriptionsSync().map { sub ->
                SubscriptionBackup(
                    id = sub.id,
                    name = sub.name,
                    cost = sub.cost,
                    billingCycle = sub.billingCycle,
                    startDate = sub.startDate,
                    nextBillingDate = sub.nextBillingDate,
                    category = sub.category,
                    icon = sub.icon,
                    colorLong = sub.color,
                    paymentMethod = sub.paymentMethod,
                    website = sub.website,
                    notes = sub.notes,
                    isActive = sub.isActive,
                    isTrial = sub.isTrial,
                    trialEndDate = sub.trialEndDate,
                    reminderDaysBefore = sub.reminderDaysBefore
                )
            }
            
            SubscriptionTrackerBackup(subscriptions)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting subscription tracker", e)
            SubscriptionTrackerBackup()
        }
    }
    
    private suspend fun exportDailyPlanner(database: TimerDatabase): DailyPlannerBackup {
        return try {
            val tasks = database.dailyTaskDao().getAllTasks().map { task ->
                DailyTaskBackup(
                    id = task.id,
                    title = task.title,
                    description = task.description,
                    date = task.date,
                    startTime = task.startTime,
                    endTime = task.endTime,
                    taskType = task.taskType,
                    category = task.category,
                    priority = task.priority,
                    icon = task.icon,
                    colorLong = task.color,
                    isCompleted = task.isCompleted,
                    isRecurring = task.isRecurring,
                    recurringDays = task.recurringDays,
                    reminderMinutes = task.reminderMinutes,
                    notes = task.notes,
                    createdAt = task.createdAt
                )
            }
            
            DailyPlannerBackup(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting daily planner", e)
            DailyPlannerBackup()
        }
    }
    
    private suspend fun exportYearCalculator(database: TimerDatabase): YearCalculatorBackup {
        return try {
            val calculations = database.dateCalculationDao().getAllCalculationsSync().map { calc ->
                DateCalculationBackup(
                    id = calc.id,
                    title = calc.title,
                    startDate = calc.startDate,
                    endDate = calc.endDate,
                    category = calc.category,
                    createdAt = calc.createdAt,
                    updatedAt = calc.updatedAt
                )
            }
            
            YearCalculatorBackup(calculations)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting year calculator", e)
            YearCalculatorBackup()
        }
    }
    
    private suspend fun exportBMICalculator(database: TimerDatabase): BMICalculatorBackup {
        return try {
            val calculations = database.bmiCalculationDao().getAllRecordsSync().map { calc ->
                BMICalculationBackupData(
                    id = calc.id,
                    name = calc.name,
                    gender = calc.gender.name,
                    age = calc.age,
                    heightCm = calc.heightCm,
                    weightKg = calc.weightKg,
                    bmiValue = calc.bmiValue,
                    classification = calc.classification.name,
                    category = calc.category,
                    createdAt = calc.createdAt,
                    updatedAt = calc.updatedAt
                )
            }
            
            BMICalculatorBackup(calculations)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting BMI calculator", e)
            BMICalculatorBackup()
        }
    }
    
    // Import functions for each module
    
    private suspend fun importProfile(database: TimerDatabase, backup: UserProfileBackup) {
        try {
            val profile = UserProfile(
                id = 0, // Let database assign new ID
                name = backup.name,
                email = backup.email,
                bio = backup.bio,
                avatarIcon = backup.avatarIcon,
                joinDate = backup.joinDate,
                dailyGoalMinutes = backup.dailyGoalMinutes,
                theme = backup.theme,
                currency = backup.currency,
                notificationsEnabled = backup.notificationsEnabled,
                weekStartDay = backup.weekStartDay,
                createdAt = backup.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            
            // Delete existing profile and insert new one
            database.userProfileDao().deleteAllProfiles()
            database.userProfileDao().insertProfile(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing profile", e)
        }
    }
    
    private suspend fun importFocusTracker(database: TimerDatabase, backup: FocusTrackerBackup): Int {
        var count = 0
        try {
            // Map old category IDs to new IDs
            val categoryIdMap = mutableMapOf<Long, Long>()
            
            backup.categories.forEach { catBackup ->
                val category = ActivityCategory(
                    id = 0,
                    name = catBackup.name,
                    icon = catBackup.icon,
                    color = catBackup.colorLong,
                    dailyGoalMinutes = catBackup.dailyGoalMinutes,
                    isActive = catBackup.isActive,
                    sortOrder = catBackup.sortOrder,
                    createdAt = catBackup.createdAt,
                    updatedAt = catBackup.updatedAt
                )
                val newId = database.activityCategoryDao().insertCategory(category)
                categoryIdMap[catBackup.id] = newId
                count++
            }
            
            backup.sessions.forEach { sessionBackup ->
                // Use mapped category ID
                val newCategoryId = categoryIdMap[sessionBackup.categoryId]
                if (newCategoryId != null) {
                    val session = TimeSession(
                        id = 0,
                        categoryId = newCategoryId,
                        startTime = sessionBackup.startTime,
                        endTime = sessionBackup.endTime,
                        durationMinutes = sessionBackup.durationMinutes,
                        date = sessionBackup.date
                    )
                    database.timeSessionDao().insertSession(session)
                    count++
                } else {
                    Log.w(TAG, "Skipping session with unknown category ID: ${sessionBackup.categoryId}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing focus tracker", e)
        }
        return count
    }
    
    private suspend fun importHabitTracker(database: TimerDatabase, backup: HabitTrackerBackup): Int {
        var count = 0
        try {
            // Map old habit IDs to new IDs
            val habitIdMap = mutableMapOf<Long, Long>()
            
            backup.habits.forEach { habitBackup ->
                val habit = Habit(
                    id = 0,
                    name = habitBackup.name,
                    description = habitBackup.description,
                    color = habitBackup.colorLong,
                    iconName = habitBackup.iconName,
                    type = HabitType.valueOf(habitBackup.habitType),
                    goalPeriodDays = habitBackup.goalPeriodDays,
                    isEveryday = habitBackup.isEveryday,
                    reminderTimeHour = habitBackup.reminderTimeHour,
                    reminderTimeMinute = habitBackup.reminderTimeMinute,
                    isActive = habitBackup.isActive,
                    createdAt = habitBackup.createdAt
                )
                val newId = database.habitDao().insertHabit(habit)
                habitIdMap[habitBackup.id] = newId
                count++
            }
            
            backup.completions.forEach { completionBackup ->
                // Use mapped habit ID
                val newHabitId = habitIdMap[completionBackup.habitId]
                if (newHabitId != null) {
                    val completion = HabitCompletion(
                        habitId = newHabitId,
                        date = completionBackup.date,
                        completionType = CompletionType.valueOf(completionBackup.completionType),
                        completedAt = completionBackup.completedAt,
                        notes = completionBackup.notes
                    )
                    database.habitDao().insertCompletion(completion)
                    count++
                } else {
                    Log.w(TAG, "Skipping completion for unknown habit ID: ${completionBackup.habitId}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing habit tracker", e)
        }
        return count
    }
    
    private suspend fun importExpenseTracker(database: TimerDatabase, backup: ExpenseTrackerBackup): Int {
        var count = 0
        try {
            // Map old category IDs to new IDs
            val categoryIdMap = mutableMapOf<Long, Long>()
            
            backup.categories.forEach { catBackup ->
                val category = ExpenseCategory(
                    id = 0,
                    name = catBackup.name,
                    icon = catBackup.icon,
                    color = catBackup.colorLong,
                    budget = catBackup.budget,
                    isActive = catBackup.isActive
                )
                val newId = database.expenseCategoryDao().insertCategory(category)
                categoryIdMap[catBackup.id] = newId
                count++
            }
            
            backup.expenses.forEach { expenseBackup ->
                // Use mapped category ID
                val newCategoryId = categoryIdMap[expenseBackup.categoryId]
                if (newCategoryId != null) {
                    val expense = Expense(
                        id = 0,
                        amount = expenseBackup.amount,
                        categoryId = newCategoryId,
                        description = expenseBackup.description,
                        date = expenseBackup.date,
                        timestamp = expenseBackup.timestamp,
                        paymentMethod = expenseBackup.paymentMethod
                    )
                    database.expenseDao().insertExpense(expense)
                    count++
                } else {
                    Log.w(TAG, "Skipping expense with unknown category ID: ${expenseBackup.categoryId}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing expense tracker", e)
        }
        return count
    }
    
    private suspend fun importSubscriptionTracker(database: TimerDatabase, backup: SubscriptionTrackerBackup): Int {
        var count = 0
        try {
            backup.subscriptions.forEach { subBackup ->
                val subscription = Subscription(
                    id = 0,
                    name = subBackup.name,
                    cost = subBackup.cost,
                    billingCycle = subBackup.billingCycle,
                    startDate = subBackup.startDate,
                    nextBillingDate = subBackup.nextBillingDate,
                    category = subBackup.category,
                    icon = subBackup.icon,
                    color = subBackup.colorLong,
                    paymentMethod = subBackup.paymentMethod,
                    website = subBackup.website,
                    notes = subBackup.notes,
                    isActive = subBackup.isActive,
                    isTrial = subBackup.isTrial,
                    trialEndDate = subBackup.trialEndDate,
                    reminderDaysBefore = subBackup.reminderDaysBefore
                )
                database.subscriptionDao().insertSubscription(subscription)
                count++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing subscription tracker", e)
        }
        return count
    }
    
    private suspend fun importDailyPlanner(database: TimerDatabase, backup: DailyPlannerBackup): Int {
        var count = 0
        try {
            backup.tasks.forEach { taskBackup ->
                val task = DailyTask(
                    id = 0,
                    title = taskBackup.title,
                    description = taskBackup.description,
                    date = taskBackup.date,
                    startTime = taskBackup.startTime,
                    endTime = taskBackup.endTime,
                    taskType = taskBackup.taskType,
                    category = taskBackup.category,
                    priority = taskBackup.priority,
                    icon = taskBackup.icon,
                    color = taskBackup.colorLong,
                    isCompleted = taskBackup.isCompleted,
                    isRecurring = taskBackup.isRecurring,
                    recurringDays = taskBackup.recurringDays,
                    reminderMinutes = taskBackup.reminderMinutes,
                    notes = taskBackup.notes,
                    createdAt = taskBackup.createdAt
                )
                database.dailyTaskDao().insertTask(task)
                count++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing daily planner", e)
        }
        return count
    }
    
    private suspend fun importYearCalculator(database: TimerDatabase, backup: YearCalculatorBackup): Int {
        var count = 0
        try {
            backup.calculations.forEach { calcBackup ->
                val calculation = DateCalculation(
                    id = 0,
                    title = calcBackup.title,
                    startDate = calcBackup.startDate,
                    endDate = calcBackup.endDate,
                    category = calcBackup.category,
                    createdAt = calcBackup.createdAt,
                    updatedAt = calcBackup.updatedAt
                )
                database.dateCalculationDao().insertCalculation(calculation)
                count++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing year calculator", e)
        }
        return count
    }
    
    private suspend fun importBMICalculator(database: TimerDatabase, backup: BMICalculatorBackup): Int {
        var count = 0
        try {
            backup.calculations.forEach { calcBackup ->
                val calculation = BMICalculation(
                    id = 0,
                    name = calcBackup.name,
                    gender = Gender.valueOf(calcBackup.gender),
                    age = calcBackup.age,
                    heightCm = calcBackup.heightCm,
                    weightKg = calcBackup.weightKg,
                    bmiValue = calcBackup.bmiValue,
                    classification = BMIClassification.valueOf(calcBackup.classification),
                    category = calcBackup.category,
                    createdAt = calcBackup.createdAt,
                    updatedAt = calcBackup.updatedAt
                )
                database.bmiCalculationDao().insertCalculation(calculation)
                count++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing BMI calculator", e)
        }
        return count
    }
}

/**
 * Import statistics
 */
data class ImportStats(
    var profileImported: Boolean = false,
    var categoriesImported: Int = 0,
    var habitsImported: Int = 0,
    var expensesImported: Int = 0,
    var subscriptionsImported: Int = 0,
    var tasksImported: Int = 0,
    var dateCalculationsImported: Int = 0,
    var bmiCalculationsImported: Int = 0
) {
    fun getTotalImported(): Int {
        return categoriesImported + habitsImported + expensesImported + 
               subscriptionsImported + tasksImported + dateCalculationsImported + 
               bmiCalculationsImported
    }
    
    fun getDetailedMessage(): String {
        val details = mutableListOf<String>()
        
        if (profileImported) details.add("✓ Profile")
        if (categoriesImported > 0) details.add("✓ $categoriesImported focus sessions")
        if (habitsImported > 0) details.add("✓ $habitsImported habit items")
        if (expensesImported > 0) details.add("✓ $expensesImported expenses")
        if (subscriptionsImported > 0) details.add("✓ $subscriptionsImported subscriptions")
        if (tasksImported > 0) details.add("✓ $tasksImported daily tasks")
        if (dateCalculationsImported > 0) details.add("✓ $dateCalculationsImported date calculations")
        if (bmiCalculationsImported > 0) details.add("✓ $bmiCalculationsImported BMI records")
        
        return if (details.isEmpty()) {
            "No data found to import"
        } else {
            "Successfully imported:\n${details.joinToString("\n")}"
        }
    }
}

