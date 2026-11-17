package me.aliahad.timemanager

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import me.aliahad.timemanager.data.TimerDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerDatabaseMigrationAndBackupTest {

    @Test
    fun migrateV13ToV14_validatesSchemaAndRepairsColorsAndCurrencyCodes() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration_v13_v14_test.db"
        context.deleteDatabase(dbName)

        // Construct an authentic v13 SQLite database on disk and seed legacy/corrupted rows
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `presets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `hours` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `color` INTEGER NOT NULL, `iconName` TEXT NOT NULL, `type` TEXT NOT NULL, `goalPeriodDays` INTEGER NOT NULL, `isEveryday` INTEGER NOT NULL, `reminderTimeHour` INTEGER, `reminderTimeMinute` INTEGER, `createdAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `habit_completions` (`habitId` INTEGER NOT NULL, `date` TEXT NOT NULL, `completionType` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habitId` ON `habit_completions` (`habitId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_date` ON `habit_completions` (`date`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habitId_date` ON `habit_completions` (`habitId`, `date`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `date_calculations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `bmi_calculations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `age` INTEGER NOT NULL, `heightCm` REAL NOT NULL, `weightKg` REAL NOT NULL, `gender` TEXT NOT NULL, `classification` TEXT NOT NULL, `bmiValue` REAL NOT NULL, `category` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `activity_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `dailyGoalMinutes` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `time_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `date` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `activity_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_sessions_categoryId` ON `time_sessions` (`categoryId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_sessions_date` ON `time_sessions` (`date`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `expense_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `budget` REAL NOT NULL, `isActive` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `date` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `expense_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `cost` REAL NOT NULL, `currency` TEXT NOT NULL, `billingCycle` TEXT NOT NULL, `startDate` TEXT NOT NULL, `nextBillingDate` TEXT NOT NULL, `category` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `website` TEXT NOT NULL, `notes` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `isTrial` INTEGER NOT NULL, `trialEndDate` TEXT, `reminderDaysBefore` INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_nextBillingDate` ON `subscriptions` (`nextBillingDate`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isActive` ON `subscriptions` (`isActive`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isActive_nextBillingDate` ON `subscriptions` (`isActive`, `nextBillingDate`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isTrial_isActive` ON `subscriptions` (`isTrial`, `isActive`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `daily_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `date` TEXT NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `taskType` TEXT NOT NULL, `category` TEXT NOT NULL, `priority` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `isRecurring` INTEGER NOT NULL, `recurringDays` TEXT NOT NULL, `reminderMinutes` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_date` ON `daily_tasks` (`date`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_date_isCompleted` ON `daily_tasks` (`date`, `isCompleted`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_isRecurring` ON `daily_tasks` (`isRecurring`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `bio` TEXT NOT NULL, `avatarIcon` TEXT NOT NULL, `joinDate` INTEGER NOT NULL, `dailyGoalMinutes` INTEGER NOT NULL, `theme` TEXT NOT NULL, `currency` TEXT NOT NULL, `notificationsEnabled` INTEGER NOT NULL, `weekStartDay` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `screen_time_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `sessionStart` INTEGER NOT NULL, `sessionEnd` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `unlockCount` INTEGER NOT NULL, `wasWalking` INTEGER NOT NULL, `appUsed` TEXT)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_screen_time_sessions_date` ON `screen_time_sessions` (`date`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_screen_time_sessions_timestamp` ON `screen_time_sessions` (`timestamp`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_screen_time_sessions_sessionStart` ON `screen_time_sessions` (`sessionStart`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `screen_time_daily` (`date` TEXT NOT NULL PRIMARY KEY, `totalScreenTimeSeconds` INTEGER NOT NULL, `pickupsCount` INTEGER NOT NULL, `firstPickupTime` INTEGER, `lastPickupTime` INTEGER, `walkingScreenTimeSeconds` INTEGER NOT NULL, `avgSessionDurationSeconds` INTEGER NOT NULL, `longestSessionSeconds` INTEGER NOT NULL, `shortestSessionSeconds` INTEGER NOT NULL, `notificationsCount` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_screen_time_daily_date` ON `screen_time_daily` (`date`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `screen_time_hourly` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `hour` INTEGER NOT NULL, `screenTimeSeconds` INTEGER NOT NULL, `pickupsCount` INTEGER NOT NULL)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_screen_time_hourly_date_hour` ON `screen_time_hourly` (`date`, `hour`)")

                    val shiftedColor = 0xFF4CAF50L shl 32
                    db.execSQL("INSERT INTO habits (id, name, description, color, iconName, type, goalPeriodDays, isEveryday, createdAt, isActive) VALUES (1, 'Read', '', $shiftedColor, 'menu_book', 'BUILD', 30, 1, 1700000000000, 1)")
                    db.execSQL("INSERT INTO daily_tasks (id, title, description, date, startTime, endTime, taskType, category, priority, icon, color, isCompleted, isRecurring, recurringDays, reminderMinutes, notes, createdAt) VALUES (1, 'Plan', '', '2026-11-17', '09:00', '09:30', 'TASK', 'Work', 'Medium', '📝', $shiftedColor, 0, 0, '', 0, '', 1700000000000)")
                    db.execSQL("INSERT INTO subscriptions (id, name, cost, currency, billingCycle, startDate, nextBillingDate, category, icon, color, paymentMethod, website, notes, isActive, isTrial, trialEndDate, reminderDaysBefore) VALUES (1, 'Cloud', 9.99, '$', 'Monthly', '2026-01-01', '2026-12-01', 'Cloud Storage', '☁️', 4283215696, 'Card', '', '', 1, 0, NULL, 3)")
                    db.execSQL("INSERT INTO user_profile (id, name, email, bio, avatarIcon, joinDate, dailyGoalMinutes, theme, currency, notificationsEnabled, weekStartDay, createdAt, updatedAt) VALUES (1, 'Ali', '', '', '👤', 1700000000000, 120, 'Auto', 'JP¥', 1, 'Monday', 1700000000000, 1700000000000)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()

        // Open with Room v14 + MIGRATION_13_14 -> triggers full Room schema validation
        val roomDb = Room.databaseBuilder(context, TimerDatabase::class.java, dbName)
            .addMigrations(*TimerDatabase.ALL_MIGRATIONS)
            .build()

        val migratedHabit = roomDb.habitDao().getHabitById(1L)
        assertNotNull(migratedHabit)
        assertEquals(0xFF4CAF50L, migratedHabit!!.color)

        val migratedTask = roomDb.dailyTaskDao().getTaskById(1L)
        assertNotNull(migratedTask)
        assertEquals(0xFF4CAF50L, migratedTask!!.color)

        val migratedSub = roomDb.subscriptionDao().getSubscriptionById(1L)
        assertEquals("USD", migratedSub?.currencyCode)

        val migratedProfile = roomDb.userProfileDao().getProfileSync()
        assertEquals("JPY", migratedProfile?.currencyCode)

        // Export v2 backup, wipe, and re-import round-trip
        val exported = DataBackupManager.exportData(context, roomDb).getOrThrow()
        assertEquals(2, exported.version)

        roomDb.close()
        context.deleteDatabase(dbName)
        }
    }

    @Test
    fun backupRoundTrip_v2PreservesAllExtendedFieldsAndReplaceMode() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, TimerDatabase::class.java).build()

        val seedJson = """
            {
              "version": 1,
              "timestamp": 1700000000000,
              "profile": {
                "name": "Tester",
                "dailyGoalMinutes": 90,
                "currency": "$"
              },
              "focusTracker": {
                "categories": [
                  { "id": 1, "name": "Coding", "icon": "💻", "colorLong": 4283215696, "dailyGoalMinutes": 60, "sortOrder": 0, "isActive": true, "createdAt": 1700000000000, "updatedAt": 1700000000000 }
                ],
                "sessions": [
                  { "id": 1, "categoryId": 1, "startTime": 1700000000000, "endTime": 1700001800000, "durationMinutes": 30, "date": "2026-11-17" }
                ]
              },
              "habitTracker": {
                "habits": [
                  { "id": 1, "name": "Meditate", "description": "", "colorLong": -4522481082368, "iconName": "self_improvement", "habitType": "BUILD", "goalPeriodDays": 30, "isEveryday": true, "reminderTimeHour": null, "reminderTimeMinute": null, "createdAt": 1700000000000, "isActive": true }
                ],
                "completions": [
                  { "habitId": 1, "date": "2026-11-17", "completionType": "ACHIEVED", "completedAt": 1700000000000, "notes": "" }
                ]
              }
            }
        """.trimIndent()

        val stats = DataBackupManager.importFromJsonString(seedJson, db, ImportMode.REPLACE)
        assertEquals(1, stats.habitsImported)
        assertEquals(1, stats.focusSessionsImported)

        val reExport = DataBackupManager.exportData(context, db).getOrThrow()
        assertEquals(2, reExport.version)
        assertEquals(1, reExport.habitTracker.habits.size)
        assertTrue(reExport.focusTracker.sessions.isNotEmpty())

        db.close()
        }
    }
}
