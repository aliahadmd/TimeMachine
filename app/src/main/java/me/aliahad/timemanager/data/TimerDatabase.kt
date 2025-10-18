package me.aliahad.timemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Preset::class], version = 1, exportSchema = false)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    
    companion object {
        @Volatile
        private var INSTANCE: TimerDatabase? = null
        
        fun getDatabase(context: Context): TimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimerDatabase::class.java,
                    "timer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

