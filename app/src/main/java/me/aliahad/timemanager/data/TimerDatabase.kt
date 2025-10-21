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
    version = 13,  // Added screen time indices
    exportSchema = false
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
        @Volatile
        private var INSTANCE: TimerDatabase? = null
        
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove duplicate screen sessions before enforcing unique index
                database.execSQL(
                    "DELETE FROM screen_time_sessions WHERE rowid NOT IN (" +
                        "SELECT MIN(rowid) FROM screen_time_sessions GROUP BY sessionStart" +
                        ")"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_screen_time_sessions_sessionStart ON screen_time_sessions(sessionStart)"
                )
            }
        }
        
            fun getDatabase(context: Context): TimerDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TimerDatabase::class.java,
                        "timer_database"
                    )
                    .addMigrations(MIGRATION_12_13)
                    .build()
                    INSTANCE = instance
                    instance
                }
            }
    }
}
