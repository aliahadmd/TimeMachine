package me.aliahad.timemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Preset::class, Habit::class, HabitCompletion::class, DateCalculation::class],
    version = 4,
    exportSchema = false
)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun habitDao(): HabitDao
    abstract fun dateCalculationDao(): DateCalculationDao
    
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

