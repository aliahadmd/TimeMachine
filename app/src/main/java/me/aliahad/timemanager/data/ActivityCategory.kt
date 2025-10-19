package me.aliahad.timemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_categories")
data class ActivityCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long, // ARGB color value
    val icon: String = "⏱️", // Emoji icon
    val dailyGoalMinutes: Int = 60, // Daily goal in minutes (0 = no goal)
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Predefined activity icons
object ActivityIcons {
    val ICONS = listOf(
        "📚" to "Study",
        "💻" to "Code",
        "🎨" to "Design",
        "✍️" to "Write",
        "🏃" to "Exercise",
        "🧘" to "Meditate",
        "📖" to "Read",
        "🎓" to "Learn",
        "💼" to "Work",
        "🎯" to "Focus",
        "🎵" to "Music",
        "🎬" to "Video",
        "🔬" to "Research",
        "📝" to "Notes",
        "🗣️" to "Meeting",
        "☕" to "Break",
        "🎮" to "Gaming",
        "🌐" to "Language",
        "⏱️" to "Timer",
        "🚀" to "Project"
    )
}

// Predefined color palette
object CategoryColors {
    val COLORS = listOf(
        0xFFE57373 to "Red",
        0xFFF06292 to "Pink",
        0xFFBA68C8 to "Purple",
        0xFF9575CD to "Deep Purple",
        0xFF7986CB to "Indigo",
        0xFF64B5F6 to "Blue",
        0xFF4FC3F7 to "Light Blue",
        0xFF4DD0E1 to "Cyan",
        0xFF4DB6AC to "Teal",
        0xFF81C784 to "Green",
        0xFFAED581 to "Light Green",
        0xFFDCE775 to "Lime",
        0xFFFFD54F to "Amber",
        0xFFFFB74D to "Orange",
        0xFFFF8A65 to "Deep Orange",
        0xFFA1887F to "Brown",
        0xFF90A4AE to "Blue Grey"
    )
}

