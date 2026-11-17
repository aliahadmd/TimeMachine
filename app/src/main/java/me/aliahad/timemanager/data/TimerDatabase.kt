package me.aliahad.timemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Preset::class,
        Habit::class,
        HabitCompletion::class,
        DateCalculation::class,
        BMICalculation::class,
        ActivityCategory::class,
        TimeSession::class,
        Expense::class,
        ExpenseCategory::class,
        Subscription::class,
        DailyTask::class,
        UserProfile::class,
        ScreenTimeSession::class,
        ScreenTimeDailySummary::class,
        ScreenTimeHourly::class
    ],
    version = TimerDatabase.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun habitDao(): HabitDao
    abstract fun dateCalculationDao(): DateCalculationDao
    abstract fun bmiCalculationDao(): BMICalculationDao
    abstract fun activityCategoryDao(): ActivityCategoryDao
    abstract fun timeSessionDao(): TimeSessionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun screenTimeDao(): ScreenTimeDao

    companion object {
        const val DATABASE_VERSION = 14

        @Volatile
        private var INSTANCE: TimerDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `budget` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `expense_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscriptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `cost` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `billingCycle` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `nextBillingDate` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `website` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `isTrial` INTEGER NOT NULL,
                        `trialEndDate` TEXT,
                        `reminderDaysBefore` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_nextBillingDate` ON `subscriptions` (`nextBillingDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isActive` ON `subscriptions` (`isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isActive_nextBillingDate` ON `subscriptions` (`isActive`, `nextBillingDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_isTrial_isActive` ON `subscriptions` (`isTrial`, `isActive`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `taskType` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isRecurring` INTEGER NOT NULL,
                        `recurringDays` TEXT NOT NULL,
                        `reminderMinutes` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_date` ON `daily_tasks` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_date_isCompleted` ON `daily_tasks` (`date`, `isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_isRecurring` ON `daily_tasks` (`isRecurring`)")
            }
        }

        val MIGRATION_9_11 = object : Migration(9, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `bio` TEXT NOT NULL,
                        `avatarIcon` TEXT NOT NULL,
                        `joinDate` INTEGER NOT NULL,
                        `dailyGoalMinutes` INTEGER NOT NULL,
                        `theme` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL,
                        `weekStartDay` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `screen_time_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sessionStart` INTEGER NOT NULL,
                        `sessionEnd` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `unlockCount` INTEGER NOT NULL,
                        `wasWalking` INTEGER NOT NULL,
                        `appUsed` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_screen_time_sessions_date` ON `screen_time_sessions` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_screen_time_sessions_timestamp` ON `screen_time_sessions` (`timestamp`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `screen_time_daily` (
                        `date` TEXT NOT NULL PRIMARY KEY,
                        `totalScreenTimeSeconds` INTEGER NOT NULL,
                        `pickupsCount` INTEGER NOT NULL,
                        `firstPickupTime` INTEGER,
                        `lastPickupTime` INTEGER,
                        `walkingScreenTimeSeconds` INTEGER NOT NULL,
                        `avgSessionDurationSeconds` INTEGER NOT NULL,
                        `longestSessionSeconds` INTEGER NOT NULL,
                        `shortestSessionSeconds` INTEGER NOT NULL,
                        `notificationsCount` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_screen_time_daily_date` ON `screen_time_daily` (`date`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `screen_time_hourly` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `hour` INTEGER NOT NULL,
                        `screenTimeSeconds` INTEGER NOT NULL,
                        `pickupsCount` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_screen_time_hourly_date_hour` ON `screen_time_hourly` (`date`, `hour`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM screen_time_sessions WHERE rowid NOT IN (" +
                        "SELECT MIN(rowid) FROM screen_time_sessions GROUP BY sessionStart" +
                        ")"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_screen_time_sessions_sessionStart` ON `screen_time_sessions` (`sessionStart`)"
                )
                // Recompute daily summary totals after deduplicating sessions
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `screen_time_daily` (
                        `date`, `totalScreenTimeSeconds`, `pickupsCount`,
                        `firstPickupTime`, `lastPickupTime`, `walkingScreenTimeSeconds`,
                        `avgSessionDurationSeconds`, `longestSessionSeconds`, `shortestSessionSeconds`,
                        `notificationsCount`, `updatedAt`
                    )
                    SELECT
                        date,
                        SUM(durationSeconds),
                        COUNT(*),
                        MIN(sessionStart),
                        MAX(sessionStart),
                        SUM(CASE WHEN wasWalking = 1 THEN durationSeconds ELSE 0 END),
                        SUM(durationSeconds) / MAX(COUNT(*), 1),
                        MAX(durationSeconds),
                        MIN(durationSeconds),
                        0,
                        strftime('%s','now') * 1000
                    FROM screen_time_sessions
                    GROUP BY date
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Repair 64-bit corrupted Color.value.toLong() values in habits & daily_tasks
                db.execSQL(
                    """
                    UPDATE habits
                    SET color = ((color >> 32) & 4294967295)
                    WHERE (color & 4294967295) = 0 AND (color >> 32) != 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE daily_tasks
                    SET color = ((color >> 32) & 4294967295)
                    WHERE (color & 4294967295) = 0 AND (color >> 32) != 0
                    """.trimIndent()
                )

                // 2. Extend habits with custom schedule & Focus/ScreenTime auto-links
                db.execSQL("ALTER TABLE habits ADD COLUMN `scheduledDays` TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
                db.execSQL("ALTER TABLE habits ADD COLUMN `linkedFocusCategoryId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE habits ADD COLUMN `linkType` TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE habits ADD COLUMN `linkedTargetValue` INTEGER NOT NULL DEFAULT 0")

                // 3. Remove redundant habit_completions indices duplicating primary key
                db.execSQL("DROP INDEX IF EXISTS `index_habit_completions_habitId`")
                db.execSQL("DROP INDEX IF EXISTS `index_habit_completions_habitId_date`")

                // 4. Extend presets for Focus Pomodoro/countdown templates
                db.execSQL("ALTER TABLE presets ADD COLUMN `breakMinutes` INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE presets ADD COLUMN `isPomodoro` INTEGER NOT NULL DEFAULT 0")

                // 5. Cross-module foreign links: time_sessions <-> daily_tasks, expenses <-> subscriptions
                db.execSQL("ALTER TABLE time_sessions ADD COLUMN `linkedTaskId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN `linkedCategoryId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN `linkedSessionId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `subscriptionId` INTEGER DEFAULT NULL")

                // 6. Currency ISO codes & auto-logging on subscriptions and user_profile
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'BDT'")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN `expenseCategoryId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN `autoLogExpense` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'BDT'")

                // Backfill currencyCode from symbol where present
                db.execSQL(
                    """
                    UPDATE subscriptions SET currencyCode = CASE currency
                        WHEN '$' THEN 'USD'
                        WHEN '€' THEN 'EUR'
                        WHEN '£' THEN 'GBP'
                        WHEN '¥' THEN 'CNY'
                        WHEN 'CN¥' THEN 'CNY'
                        WHEN 'JP¥' THEN 'JPY'
                        WHEN '₹' THEN 'INR'
                        WHEN '₩' THEN 'KRW'
                        WHEN '₽' THEN 'RUB'
                        WHEN 'R$' THEN 'BRL'
                        WHEN 'C$' THEN 'CAD'
                        WHEN 'A$' THEN 'AUD'
                        WHEN 'CHF' THEN 'CHF'
                        WHEN 'kr' THEN 'SEK'
                        WHEN 'zł' THEN 'PLN'
                        ELSE 'BDT'
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE user_profile SET currencyCode = CASE currency
                        WHEN '$' THEN 'USD'
                        WHEN '€' THEN 'EUR'
                        WHEN '£' THEN 'GBP'
                        WHEN '¥' THEN 'CNY'
                        WHEN 'CN¥' THEN 'CNY'
                        WHEN 'JP¥' THEN 'JPY'
                        WHEN '₹' THEN 'INR'
                        WHEN '₩' THEN 'KRW'
                        WHEN '₽' THEN 'RUB'
                        WHEN 'R$' THEN 'BRL'
                        WHEN 'C$' THEN 'CAD'
                        WHEN 'A$' THEN 'AUD'
                        WHEN 'CHF' THEN 'CHF'
                        WHEN 'kr' THEN 'SEK'
                        WHEN 'zł' THEN 'PLN'
                        ELSE 'BDT'
                    END
                    """.trimIndent()
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14
        )

        fun getDatabase(context: Context): TimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimerDatabase::class.java,
                    "timer_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
