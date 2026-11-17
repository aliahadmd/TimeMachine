package me.aliahad.timemanager.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromHabitType(value: HabitType): String = value.name

    @TypeConverter
    fun toHabitType(value: String): HabitType {
        return try {
            HabitType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            HabitType.BUILD
        }
    }

    @TypeConverter
    fun fromHabitLinkType(value: HabitLinkType): String = value.name

    @TypeConverter
    fun toHabitLinkType(value: String): HabitLinkType {
        return try {
            HabitLinkType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            HabitLinkType.MANUAL
        }
    }

    @TypeConverter
    fun fromCompletionType(value: CompletionType): String = value.name

    @TypeConverter
    fun toCompletionType(value: String): CompletionType {
        return try {
            CompletionType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            CompletionType.ACHIEVED
        }
    }

    @TypeConverter
    fun fromGender(value: Gender): String = value.name

    @TypeConverter
    fun toGender(value: String): Gender {
        return try {
            Gender.valueOf(value)
        } catch (_: IllegalArgumentException) {
            Gender.MALE
        }
    }

    @TypeConverter
    fun fromBMIClassification(value: BMIClassification): String = value.name

    @TypeConverter
    fun toBMIClassification(value: String): BMIClassification {
        return try {
            BMIClassification.valueOf(value)
        } catch (_: IllegalArgumentException) {
            BMIClassification.WHO
        }
    }
}
