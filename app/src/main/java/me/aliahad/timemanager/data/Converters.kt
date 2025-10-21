package me.aliahad.timemanager.data

import androidx.room.TypeConverter

/**
 * Room TypeConverters for enum types used in database entities.
 * These converters allow Room to store enums as strings in the database.
 */
class Converters {
    
    // HabitType converters
    @TypeConverter
    fun fromHabitType(value: HabitType): String {
        return value.name
    }
    
    @TypeConverter
    fun toHabitType(value: String): HabitType {
        return try {
            HabitType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            HabitType.BUILD // Default fallback
        }
    }
    
    // CompletionType converters
    @TypeConverter
    fun fromCompletionType(value: CompletionType): String {
        return value.name
    }
    
    @TypeConverter
    fun toCompletionType(value: String): CompletionType {
        return try {
            CompletionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            CompletionType.ACHIEVED // Default fallback
        }
    }
    
    // Gender converters
    @TypeConverter
    fun fromGender(value: Gender): String {
        return value.name
    }
    
    @TypeConverter
    fun toGender(value: String): Gender {
        return try {
            Gender.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Gender.MALE // Default fallback
        }
    }
    
    // BMIClassification converters
    @TypeConverter
    fun fromBMIClassification(value: BMIClassification): String {
        return value.name
    }
    
    @TypeConverter
    fun toBMIClassification(value: String): BMIClassification {
        return try {
            BMIClassification.valueOf(value)
        } catch (e: IllegalArgumentException) {
            BMIClassification.WHO // Default fallback
        }
    }
}

