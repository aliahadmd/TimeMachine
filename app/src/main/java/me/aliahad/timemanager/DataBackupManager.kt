package me.aliahad.timemanager

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.common.normalizeStoredColor
import me.aliahad.timemanager.core.datastore.ThemeMode
import me.aliahad.timemanager.core.datastore.UserPreferencesRepository
import me.aliahad.timemanager.data.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportMode {
    REPLACE,
    MERGE
}

object DataBackupManager {

    private const val TAG = "DataBackupManager"
    private const val MAX_IMPORT_BYTES = 25 * 1024 * 1024 // 25MB safety cap

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportData(context: Context, database: TimerDatabase): Result<BackupData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backupData = BackupData(
                    version = 2,
                    timestamp = System.currentTimeMillis(),
                    profile = exportProfile(database),
                    focusTracker = exportFocusTracker(database),
                    habitTracker = exportHabitTracker(database),
                    expenseTracker = exportExpenseTracker(database),
                    subscriptionTracker = exportSubscriptionTracker(database),
                    dailyPlanner = exportDailyPlanner(database),
                    yearCalculator = exportYearCalculator(database),
                    bmiCalculator = exportBMICalculator(database),
                    screenTime = exportScreenTime(database)
                )
                backupData
            }.onFailure { e ->
                Log.e(TAG, "Error exporting data", e)
            }
        }

    suspend fun saveBackupToFile(
        context: Context,
        uri: Uri,
        backupData: BackupData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = json.encodeToString(backupData)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Unable to open destination file stream")
            stream.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
        }.onFailure { e ->
            Log.e(TAG, "Error saving backup to file", e)
        }
    }

    suspend fun importData(
        context: Context,
        uri: Uri,
        database: TimerDatabase,
        mode: ImportMode = ImportMode.REPLACE,
        preferencesRepository: UserPreferencesRepository? = null,
        onAfterImport: suspend () -> Unit = {}
    ): Result<ImportStats> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Failed to open backup file")

            val jsonString = inputStream.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size > MAX_IMPORT_BYTES) {
                    throw IllegalArgumentException("Backup file exceeds 25MB limit")
                }
                bytes.decodeToString()
            }

            importFromJsonString(
                jsonString = jsonString,
                database = database,
                mode = mode,
                preferencesRepository = preferencesRepository,
                onAfterImport = onAfterImport
            )
        }.onFailure { e ->
            Log.e(TAG, "Error importing data", e)
        }
    }

    internal suspend fun importFromJsonString(
        jsonString: String,
        database: TimerDatabase,
        mode: ImportMode = ImportMode.REPLACE,
        preferencesRepository: UserPreferencesRepository? = null,
        onAfterImport: suspend () -> Unit = {}
    ): ImportStats {
        val backupData = try {
            json.decodeFromString<BackupData>(jsonString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup file format: ${e.message}", e)
        }

        if (backupData.version !in 1..2) {
            throw IllegalArgumentException("Unsupported backup version: ${backupData.version}")
        }

        val stats = ImportStats()

        database.withTransaction {
            if (mode == ImportMode.REPLACE) {
                database.clearAllTables()
            }

            backupData.profile?.let { profileBackup ->
                importProfile(database, profileBackup)
                stats.profileImported = true
            }

            val (cats, sessions, presets) = importFocusTracker(database, backupData.focusTracker, mode)
            stats.focusCategoriesImported = cats
            stats.focusSessionsImported = sessions
            stats.presetsImported = presets

            val (habits, completions) = importHabitTracker(database, backupData.habitTracker, mode)
            stats.habitsImported = habits
            stats.habitCompletionsImported = completions

            val (expCats, expenses) = importExpenseTracker(database, backupData.expenseTracker, mode)
            stats.expenseCategoriesImported = expCats
            stats.expensesImported = expenses

            stats.subscriptionsImported = importSubscriptionTracker(database, backupData.subscriptionTracker, mode)
            stats.tasksImported = importDailyPlanner(database, backupData.dailyPlanner, mode)
            stats.dateCalculationsImported = importYearCalculator(database, backupData.yearCalculator, mode)
            stats.bmiCalculationsImported = importBMICalculator(database, backupData.bmiCalculator, mode)
            stats.screenTimeRecordsImported = importScreenTime(database, backupData.screenTime, mode)
        }

        // Sync imported profile preferences into DataStore
        backupData.profile?.let { profileBackup ->
            preferencesRepository?.let { repo ->
                val resolvedCode = profileBackup.currencyCode.ifBlank {
                    CurrencyCatalog.inferCodeFromSymbol(profileBackup.currency)
                }
                repo.setCurrencyCode(resolvedCode)
                if (profileBackup.theme.isNotBlank()) {
                    repo.setThemeMode(ThemeMode.fromString(profileBackup.theme))
                }
                if (profileBackup.dailyGoalMinutes > 0) {
                    repo.setDailyFocusGoalMinutes(profileBackup.dailyGoalMinutes)
                }
                repo.setNotificationsEnabled(profileBackup.notificationsEnabled)
            }
        }

        onAfterImport()
        return stats
    }

    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        return "TimeMachine_Backup_${dateFormat.format(Date())}.json"
    }

    // region Export helpers
    private suspend fun exportProfile(database: TimerDatabase): UserProfileBackup? {
        return database.userProfileDao().getProfileSync()?.let { profile ->
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
                currencyCode = profile.currencyCode,
                notificationsEnabled = profile.notificationsEnabled,
                weekStartDay = profile.weekStartDay,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }

    private suspend fun exportFocusTracker(database: TimerDatabase): FocusTrackerBackup {
        val categories = database.activityCategoryDao().getAllCategoriesSync().map { category ->
            CategoryBackup(
                id = category.id,
                name = category.name,
                icon = category.icon,
                colorLong = normalizeStoredColor(category.color),
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
                date = session.date,
                notes = session.notes,
                linkedTaskId = session.linkedTaskId,
                createdAt = session.createdAt
            )
        }
        val presets = database.presetDao().getAllPresetsSync().map { preset ->
            PresetBackup(
                id = preset.id,
                name = preset.name,
                hours = preset.hours,
                minutes = preset.minutes,
                breakMinutes = preset.breakMinutes,
                isPomodoro = preset.isPomodoro,
                createdAt = preset.createdAt
            )
        }
        return FocusTrackerBackup(categories, sessions, presets)
    }

    private suspend fun exportHabitTracker(database: TimerDatabase): HabitTrackerBackup {
        val habits = database.habitDao().getAllHabitsSync().map { habit ->
            HabitBackup(
                id = habit.id,
                name = habit.name,
                description = habit.description,
                colorLong = normalizeStoredColor(habit.color),
                iconName = habit.iconName,
                habitType = habit.type.name,
                goalPeriodDays = habit.goalPeriodDays,
                isEveryday = habit.isEveryday,
                scheduledDays = habit.scheduledDays,
                reminderTimeHour = habit.reminderTimeHour,
                reminderTimeMinute = habit.reminderTimeMinute,
                linkedFocusCategoryId = habit.linkedFocusCategoryId,
                linkType = habit.linkType.name,
                linkedTargetValue = habit.linkedTargetValue,
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
        return HabitTrackerBackup(habits, completions)
    }

    private suspend fun exportExpenseTracker(database: TimerDatabase): ExpenseTrackerBackup {
        val categories = database.expenseCategoryDao().getAllCategoriesSync().map { category ->
            ExpenseCategoryBackup(
                id = category.id,
                name = category.name,
                icon = category.icon,
                colorLong = normalizeStoredColor(category.color),
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
                paymentMethod = expense.paymentMethod,
                subscriptionId = expense.subscriptionId
            )
        }
        return ExpenseTrackerBackup(categories, expenses)
    }

    private suspend fun exportSubscriptionTracker(database: TimerDatabase): SubscriptionTrackerBackup {
        val subscriptions = database.subscriptionDao().getAllSubscriptionsSync().map { sub ->
            SubscriptionBackup(
                id = sub.id,
                name = sub.name,
                cost = sub.cost,
                currency = sub.currency,
                currencyCode = sub.currencyCode,
                billingCycle = sub.billingCycle,
                startDate = sub.startDate,
                nextBillingDate = sub.nextBillingDate,
                category = sub.category,
                icon = sub.icon,
                colorLong = normalizeStoredColor(sub.color),
                paymentMethod = sub.paymentMethod,
                website = sub.website,
                notes = sub.notes,
                isActive = sub.isActive,
                isTrial = sub.isTrial,
                trialEndDate = sub.trialEndDate,
                reminderDaysBefore = sub.reminderDaysBefore,
                expenseCategoryId = sub.expenseCategoryId,
                autoLogExpense = sub.autoLogExpense
            )
        }
        return SubscriptionTrackerBackup(subscriptions)
    }

    private suspend fun exportDailyPlanner(database: TimerDatabase): DailyPlannerBackup {
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
                colorLong = normalizeStoredColor(task.color),
                isCompleted = task.isCompleted,
                isRecurring = task.isRecurring,
                recurringDays = task.recurringDays,
                reminderMinutes = task.reminderMinutes,
                notes = task.notes,
                linkedCategoryId = task.linkedCategoryId,
                linkedSessionId = task.linkedSessionId,
                createdAt = task.createdAt
            )
        }
        return DailyPlannerBackup(tasks)
    }

    private suspend fun exportYearCalculator(database: TimerDatabase): YearCalculatorBackup {
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
        return YearCalculatorBackup(calculations)
    }

    private suspend fun exportBMICalculator(database: TimerDatabase): BMICalculatorBackup {
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
        return BMICalculatorBackup(calculations)
    }

    private suspend fun exportScreenTime(database: TimerDatabase): ScreenTimeBackup {
        val sessions = database.screenTimeDao().getAllSessionsSync().map { s ->
            ScreenTimeSessionBackup(
                date = s.date,
                timestamp = s.timestamp,
                sessionStart = s.sessionStart,
                sessionEnd = s.sessionEnd,
                durationSeconds = s.durationSeconds,
                unlockCount = s.unlockCount,
                wasWalking = s.wasWalking,
                appUsed = s.appUsed
            )
        }
        val dailies = database.screenTimeDao().getAllDailySummariesSync().map { d ->
            ScreenTimeDailySummaryBackup(
                date = d.date,
                totalScreenTimeSeconds = d.totalScreenTimeSeconds,
                pickupsCount = d.pickupsCount,
                firstPickupTime = d.firstPickupTime,
                lastPickupTime = d.lastPickupTime,
                walkingScreenTimeSeconds = d.walkingScreenTimeSeconds,
                avgSessionDurationSeconds = d.avgSessionDurationSeconds,
                longestSessionSeconds = d.longestSessionSeconds,
                shortestSessionSeconds = d.shortestSessionSeconds,
                notificationsCount = d.notificationsCount,
                updatedAt = d.updatedAt
            )
        }
        val hourlies = database.screenTimeDao().getAllHourlyDataSync().map { h ->
            ScreenTimeHourlyBackup(
                date = h.date,
                hour = h.hour,
                screenTimeSeconds = h.screenTimeSeconds,
                pickupsCount = h.pickupsCount
            )
        }
        return ScreenTimeBackup(sessions, dailies, hourlies)
    }
    // endregion

    // region Import helpers
    private suspend fun importProfile(database: TimerDatabase, backup: UserProfileBackup) {
        val code = backup.currencyCode.ifBlank {
            CurrencyCatalog.inferCodeFromSymbol(backup.currency)
        }
        val symbol = backup.currency.ifBlank { CurrencyCatalog.symbolFor(code) }
        val profile = UserProfile(
            id = 0,
            name = backup.name.ifBlank { "User" },
            email = backup.email,
            bio = backup.bio,
            avatarIcon = backup.avatarIcon.ifBlank { "👤" },
            joinDate = backup.joinDate.takeIf { it > 0 } ?: System.currentTimeMillis(),
            dailyGoalMinutes = backup.dailyGoalMinutes.takeIf { it > 0 } ?: 120,
            theme = backup.theme.ifBlank { "Auto" },
            currency = symbol,
            currencyCode = code,
            notificationsEnabled = backup.notificationsEnabled,
            weekStartDay = backup.weekStartDay.ifBlank { "Monday" },
            createdAt = backup.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.userProfileDao().deleteAllProfiles()
        database.userProfileDao().insertProfile(profile)
    }

    private suspend fun importFocusTracker(
        database: TimerDatabase,
        backup: FocusTrackerBackup,
        mode: ImportMode
    ): Triple<Int, Int, Int> {
        val categoryDao = database.activityCategoryDao()
        val sessionDao = database.timeSessionDao()
        val presetDao = database.presetDao()

        val existingCategories = if (mode == ImportMode.MERGE) categoryDao.getAllCategoriesSync() else emptyList()
        val existingSessions = if (mode == ImportMode.MERGE) sessionDao.getAllSessionsSync() else emptyList()
        val existingPresets = if (mode == ImportMode.MERGE) presetDao.getAllPresetsSync() else emptyList()

        val categoryIdMap = mutableMapOf<Long, Long>()
        var catCount = 0
        backup.categories.forEach { catBackup ->
            val match = existingCategories.firstOrNull {
                it.name.equals(catBackup.name, ignoreCase = true)
            }
            if (match != null) {
                categoryIdMap[catBackup.id] = match.id
            } else {
                val newId = categoryDao.insertCategory(
                    ActivityCategory(
                        id = 0,
                        name = catBackup.name,
                        icon = catBackup.icon,
                        color = normalizeStoredColor(catBackup.colorLong),
                        dailyGoalMinutes = catBackup.dailyGoalMinutes,
                        isActive = catBackup.isActive,
                        sortOrder = catBackup.sortOrder,
                        createdAt = catBackup.createdAt,
                        updatedAt = catBackup.updatedAt
                    )
                )
                categoryIdMap[catBackup.id] = newId
                catCount++
            }
        }

        var sessionCount = 0
        backup.sessions.forEach { sessionBackup ->
            val newCategoryId = categoryIdMap[sessionBackup.categoryId] ?: return@forEach
            val dup = existingSessions.any {
                it.categoryId == newCategoryId &&
                    it.startTime == sessionBackup.startTime &&
                    it.durationMinutes == sessionBackup.durationMinutes
            }
            if (!dup) {
                sessionDao.insertSession(
                    TimeSession(
                        id = 0,
                        categoryId = newCategoryId,
                        startTime = sessionBackup.startTime,
                        endTime = sessionBackup.endTime,
                        durationMinutes = sessionBackup.durationMinutes,
                        date = sessionBackup.date,
                        notes = sessionBackup.notes,
                        linkedTaskId = sessionBackup.linkedTaskId,
                        createdAt = sessionBackup.createdAt.takeIf { it > 0 } ?: sessionBackup.startTime
                    )
                )
                sessionCount++
            }
        }

        var presetCount = 0
        backup.presets.forEach { presetBackup ->
            val dup = existingPresets.any { it.name.equals(presetBackup.name, ignoreCase = true) }
            if (!dup) {
                presetDao.insertPreset(
                    Preset(
                        id = 0,
                        name = presetBackup.name,
                        hours = presetBackup.hours,
                        minutes = presetBackup.minutes,
                        breakMinutes = presetBackup.breakMinutes,
                        isPomodoro = presetBackup.isPomodoro,
                        createdAt = presetBackup.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
                presetCount++
            }
        }

        return Triple(catCount, sessionCount, presetCount)
    }

    private suspend fun importHabitTracker(
        database: TimerDatabase,
        backup: HabitTrackerBackup,
        mode: ImportMode
    ): Pair<Int, Int> {
        val habitDao = database.habitDao()
        val existingHabits = if (mode == ImportMode.MERGE) habitDao.getAllHabitsSync() else emptyList()
        val habitIdMap = mutableMapOf<Long, Long>()

        var habitCount = 0
        backup.habits.forEach { habitBackup ->
            val match = existingHabits.firstOrNull {
                it.name.equals(habitBackup.name, ignoreCase = true) && it.createdAt == habitBackup.createdAt
            }
            if (match != null) {
                habitIdMap[habitBackup.id] = match.id
            } else {
                val parsedType = runCatching { HabitType.valueOf(habitBackup.habitType) }
                    .getOrDefault(HabitType.BUILD)
                val parsedLinkType = runCatching { HabitLinkType.valueOf(habitBackup.linkType) }
                    .getOrDefault(HabitLinkType.MANUAL)
                val newId = habitDao.insertHabit(
                    Habit(
                        id = 0,
                        name = habitBackup.name,
                        description = habitBackup.description,
                        color = normalizeStoredColor(habitBackup.colorLong),
                        iconName = habitBackup.iconName,
                        type = parsedType,
                        goalPeriodDays = habitBackup.goalPeriodDays,
                        isEveryday = habitBackup.isEveryday,
                        scheduledDays = habitBackup.scheduledDays.ifBlank { "1,2,3,4,5,6,7" },
                        reminderTimeHour = habitBackup.reminderTimeHour,
                        reminderTimeMinute = habitBackup.reminderTimeMinute,
                        linkedFocusCategoryId = habitBackup.linkedFocusCategoryId,
                        linkType = parsedLinkType,
                        linkedTargetValue = habitBackup.linkedTargetValue,
                        isActive = habitBackup.isActive,
                        createdAt = habitBackup.createdAt
                    )
                )
                habitIdMap[habitBackup.id] = newId
                habitCount++
            }
        }

        var completionCount = 0
        backup.completions.forEach { completionBackup ->
            val newHabitId = habitIdMap[completionBackup.habitId] ?: return@forEach
            val parsedCompletion = runCatching { CompletionType.valueOf(completionBackup.completionType) }
                .getOrDefault(CompletionType.ACHIEVED)
            habitDao.insertCompletion(
                HabitCompletion(
                    habitId = newHabitId,
                    date = completionBackup.date,
                    completionType = parsedCompletion,
                    completedAt = completionBackup.completedAt,
                    notes = completionBackup.notes
                )
            )
            completionCount++
        }

        return habitCount to completionCount
    }

    private suspend fun importExpenseTracker(
        database: TimerDatabase,
        backup: ExpenseTrackerBackup,
        mode: ImportMode
    ): Pair<Int, Int> {
        val categoryDao = database.expenseCategoryDao()
        val expenseDao = database.expenseDao()
        val existingCats = if (mode == ImportMode.MERGE) categoryDao.getAllCategoriesSync() else emptyList()
        val existingExpenses = if (mode == ImportMode.MERGE) expenseDao.getAllExpensesSync() else emptyList()
        val categoryIdMap = mutableMapOf<Long, Long>()

        var catCount = 0
        backup.categories.forEach { catBackup ->
            val match = existingCats.firstOrNull { it.name.equals(catBackup.name, ignoreCase = true) }
            if (match != null) {
                categoryIdMap[catBackup.id] = match.id
            } else {
                val newId = categoryDao.insertCategory(
                    ExpenseCategory(
                        id = 0,
                        name = catBackup.name,
                        icon = catBackup.icon,
                        color = normalizeStoredColor(catBackup.colorLong),
                        budget = catBackup.budget,
                        isActive = catBackup.isActive
                    )
                )
                categoryIdMap[catBackup.id] = newId
                catCount++
            }
        }

        var expenseCount = 0
        backup.expenses.forEach { expenseBackup ->
            val newCatId = categoryIdMap[expenseBackup.categoryId] ?: return@forEach
            val dup = existingExpenses.any {
                it.categoryId == newCatId &&
                    it.timestamp == expenseBackup.timestamp &&
                    it.amount == expenseBackup.amount
            }
            if (!dup) {
                expenseDao.insertExpense(
                    Expense(
                        id = 0,
                        amount = expenseBackup.amount,
                        categoryId = newCatId,
                        description = expenseBackup.description,
                        date = expenseBackup.date,
                        timestamp = expenseBackup.timestamp,
                        paymentMethod = expenseBackup.paymentMethod,
                        subscriptionId = expenseBackup.subscriptionId
                    )
                )
                expenseCount++
            }
        }

        return catCount to expenseCount
    }

    private suspend fun importSubscriptionTracker(
        database: TimerDatabase,
        backup: SubscriptionTrackerBackup,
        mode: ImportMode
    ): Int {
        val dao = database.subscriptionDao()
        val existing = if (mode == ImportMode.MERGE) dao.getAllSubscriptionsSync() else emptyList()
        var count = 0
        backup.subscriptions.forEach { subBackup ->
            val dup = existing.any {
                it.name.equals(subBackup.name, ignoreCase = true) && it.startDate == subBackup.startDate
            }
            if (!dup) {
                val code = subBackup.currencyCode.ifBlank {
                    CurrencyCatalog.inferCodeFromSymbol(subBackup.currency)
                }
                val symbol = subBackup.currency.ifBlank { CurrencyCatalog.symbolFor(code) }
                dao.insertSubscription(
                    Subscription(
                        id = 0,
                        name = subBackup.name,
                        cost = subBackup.cost,
                        currency = symbol,
                        currencyCode = code,
                        billingCycle = subBackup.billingCycle,
                        startDate = subBackup.startDate,
                        nextBillingDate = subBackup.nextBillingDate,
                        category = subBackup.category,
                        icon = subBackup.icon,
                        color = normalizeStoredColor(subBackup.colorLong),
                        paymentMethod = subBackup.paymentMethod,
                        website = subBackup.website,
                        notes = subBackup.notes,
                        isActive = subBackup.isActive,
                        isTrial = subBackup.isTrial,
                        trialEndDate = subBackup.trialEndDate,
                        reminderDaysBefore = subBackup.reminderDaysBefore,
                        expenseCategoryId = subBackup.expenseCategoryId,
                        autoLogExpense = subBackup.autoLogExpense
                    )
                )
                count++
            }
        }
        return count
    }

    private suspend fun importDailyPlanner(
        database: TimerDatabase,
        backup: DailyPlannerBackup,
        mode: ImportMode
    ): Int {
        val dao = database.dailyTaskDao()
        val existing = if (mode == ImportMode.MERGE) dao.getAllTasks() else emptyList()
        var count = 0
        backup.tasks.forEach { taskBackup ->
            val normalizedPriority = when (taskBackup.priority.trim().lowercase()) {
                "high" -> "High"
                "low" -> "Low"
                else -> "Medium"
            }
            val dup = existing.any {
                it.date == taskBackup.date &&
                    it.startTime == taskBackup.startTime &&
                    it.title.equals(taskBackup.title, ignoreCase = true)
            }
            if (!dup) {
                dao.insertTask(
                    DailyTask(
                        id = 0,
                        title = taskBackup.title,
                        description = taskBackup.description,
                        date = taskBackup.date,
                        startTime = taskBackup.startTime,
                        endTime = taskBackup.endTime,
                        taskType = taskBackup.taskType,
                        category = taskBackup.category,
                        priority = normalizedPriority,
                        icon = taskBackup.icon,
                        color = normalizeStoredColor(taskBackup.colorLong),
                        isCompleted = taskBackup.isCompleted,
                        isRecurring = taskBackup.isRecurring,
                        recurringDays = taskBackup.recurringDays,
                        reminderMinutes = taskBackup.reminderMinutes,
                        notes = taskBackup.notes,
                        linkedCategoryId = taskBackup.linkedCategoryId,
                        linkedSessionId = taskBackup.linkedSessionId,
                        createdAt = taskBackup.createdAt
                    )
                )
                count++
            }
        }
        return count
    }

    private suspend fun importYearCalculator(
        database: TimerDatabase,
        backup: YearCalculatorBackup,
        mode: ImportMode
    ): Int {
        val dao = database.dateCalculationDao()
        val existing = if (mode == ImportMode.MERGE) dao.getAllCalculationsSync() else emptyList()
        var count = 0
        backup.calculations.forEach { calcBackup ->
            val dup = existing.any {
                it.title.equals(calcBackup.title, ignoreCase = true) && it.startDate == calcBackup.startDate
            }
            if (!dup) {
                dao.insertCalculation(
                    DateCalculation(
                        id = 0,
                        title = calcBackup.title,
                        startDate = calcBackup.startDate,
                        endDate = calcBackup.endDate,
                        category = calcBackup.category,
                        createdAt = calcBackup.createdAt,
                        updatedAt = calcBackup.updatedAt
                    )
                )
                count++
            }
        }
        return count
    }

    private suspend fun importBMICalculator(
        database: TimerDatabase,
        backup: BMICalculatorBackup,
        mode: ImportMode
    ): Int {
        val dao = database.bmiCalculationDao()
        val existing = if (mode == ImportMode.MERGE) dao.getAllRecordsSync() else emptyList()
        var count = 0
        backup.calculations.forEach { calcBackup ->
            val dup = existing.any {
                it.name.equals(calcBackup.name, ignoreCase = true) && it.createdAt == calcBackup.createdAt
            }
            if (!dup) {
                val gender = runCatching { Gender.valueOf(calcBackup.gender) }.getOrDefault(Gender.MALE)
                val classification = runCatching { BMIClassification.valueOf(calcBackup.classification) }
                    .getOrDefault(BMIClassification.WHO)
                dao.insertCalculation(
                    BMICalculation(
                        id = 0,
                        name = calcBackup.name,
                        gender = gender,
                        age = calcBackup.age,
                        heightCm = calcBackup.heightCm,
                        weightKg = calcBackup.weightKg,
                        bmiValue = calcBackup.bmiValue,
                        classification = classification,
                        category = calcBackup.category,
                        createdAt = calcBackup.createdAt,
                        updatedAt = calcBackup.updatedAt
                    )
                )
                count++
            }
        }
        return count
    }

    private suspend fun importScreenTime(
        database: TimerDatabase,
        backup: ScreenTimeBackup,
        mode: ImportMode
    ): Int {
        val dao = database.screenTimeDao()
        var count = 0
        backup.sessions.forEach { s ->
            val inserted = dao.insertSession(
                ScreenTimeSession(
                    id = 0,
                    date = s.date,
                    timestamp = s.timestamp,
                    sessionStart = s.sessionStart,
                    sessionEnd = s.sessionEnd,
                    durationSeconds = s.durationSeconds,
                    unlockCount = s.unlockCount,
                    wasWalking = s.wasWalking,
                    appUsed = s.appUsed
                )
            )
            if (inserted > 0) count++
        }
        backup.dailySummaries.forEach { d ->
            if (mode == ImportMode.REPLACE || dao.getDailySummary(d.date) == null) {
                dao.insertDailySummary(
                    ScreenTimeDailySummary(
                        date = d.date,
                        totalScreenTimeSeconds = d.totalScreenTimeSeconds,
                        pickupsCount = d.pickupsCount,
                        firstPickupTime = d.firstPickupTime,
                        lastPickupTime = d.lastPickupTime,
                        walkingScreenTimeSeconds = d.walkingScreenTimeSeconds,
                        avgSessionDurationSeconds = d.avgSessionDurationSeconds,
                        longestSessionSeconds = d.longestSessionSeconds,
                        shortestSessionSeconds = d.shortestSessionSeconds,
                        notificationsCount = d.notificationsCount,
                        updatedAt = d.updatedAt
                    )
                )
                count++
            }
        }
        backup.hourlyData.forEach { h ->
            dao.insertHourlyData(
                ScreenTimeHourly(
                    id = 0,
                    date = h.date,
                    hour = h.hour,
                    screenTimeSeconds = h.screenTimeSeconds,
                    pickupsCount = h.pickupsCount
                )
            )
        }
        return count
    }
    // endregion
}

data class ImportStats(
    var profileImported: Boolean = false,
    var focusCategoriesImported: Int = 0,
    var focusSessionsImported: Int = 0,
    var presetsImported: Int = 0,
    var habitsImported: Int = 0,
    var habitCompletionsImported: Int = 0,
    var expenseCategoriesImported: Int = 0,
    var expensesImported: Int = 0,
    var subscriptionsImported: Int = 0,
    var tasksImported: Int = 0,
    var dateCalculationsImported: Int = 0,
    var bmiCalculationsImported: Int = 0,
    var screenTimeRecordsImported: Int = 0
) {
    fun getTotalImported(): Int {
        return focusCategoriesImported + focusSessionsImported + presetsImported +
            habitsImported + habitCompletionsImported +
            expenseCategoriesImported + expensesImported +
            subscriptionsImported + tasksImported +
            dateCalculationsImported + bmiCalculationsImported +
            screenTimeRecordsImported
    }

    fun getDetailedMessage(): String {
        val details = mutableListOf<String>()
        if (profileImported) details.add("✓ Profile")
        if (focusCategoriesImported > 0 || focusSessionsImported > 0 || presetsImported > 0) {
            details.add("✓ Focus: $focusCategoriesImported categories, $focusSessionsImported sessions, $presetsImported presets")
        }
        if (habitsImported > 0 || habitCompletionsImported > 0) {
            details.add("✓ Habits: $habitsImported habits, $habitCompletionsImported check-ins")
        }
        if (expenseCategoriesImported > 0 || expensesImported > 0) {
            details.add("✓ Expenses: $expenseCategoriesImported categories, $expensesImported transactions")
        }
        if (subscriptionsImported > 0) details.add("✓ $subscriptionsImported subscriptions")
        if (tasksImported > 0) details.add("✓ $tasksImported daily tasks")
        if (dateCalculationsImported > 0) details.add("✓ $dateCalculationsImported date calculations")
        if (bmiCalculationsImported > 0) details.add("✓ $bmiCalculationsImported BMI records")
        if (screenTimeRecordsImported > 0) details.add("✓ $screenTimeRecordsImported screen time records")

        return if (details.isEmpty()) {
            "No new records found to import"
        } else {
            "Successfully imported:\n${details.joinToString("\n")}"
        }
    }
}
