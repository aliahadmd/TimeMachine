package me.aliahad.timemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
        UserProfile::class
    ],
    version = 11,  // Added UserProfile entity
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
    
    companion object {
        @Volatile
        private var INSTANCE: TimerDatabase? = null
        
            fun getDatabase(context: Context): TimerDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TimerDatabase::class.java,
                        "timer_database"
                    )
                    // Production: Proper migrations would be added here for schema changes
                    // For now, app is in initial release so destructive migration is acceptable
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                }
            }
    }
}

