package me.aliahad.timemanager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Available habit icons with their names
data class HabitIcon(
    val name: String,
    val icon: ImageVector,
    val displayName: String
)

val habitIcons = listOf(
    HabitIcon("fitness_center", Icons.Default.FitnessCenter, "Fitness"),
    HabitIcon("directions_run", Icons.Default.DirectionsRun, "Running"),
    HabitIcon("self_improvement", Icons.Default.SelfImprovement, "Meditation"),
    HabitIcon("local_drink", Icons.Default.LocalDrink, "Hydration"),
    HabitIcon("restaurant", Icons.Default.Restaurant, "Healthy Eating"),
    HabitIcon("menu_book", Icons.Default.MenuBook, "Reading"),
    HabitIcon("edit_note", Icons.Default.EditNote, "Writing"),
    HabitIcon("school", Icons.Default.School, "Learning"),
    HabitIcon("brush", Icons.Default.Brush, "Art"),
    HabitIcon("music_note", Icons.Default.MusicNote, "Music"),
    HabitIcon("bedtime", Icons.Default.Bedtime, "Sleep"),
    HabitIcon("wb_sunny", Icons.Default.WbSunny, "Morning"),
    HabitIcon("nightlight", Icons.Default.Nightlight, "Evening"),
    HabitIcon("favorite", Icons.Default.Favorite, "Self Care"),
    HabitIcon("spa", Icons.Default.Spa, "Wellness"),
    HabitIcon("eco", Icons.Default.Eco, "Environment"),
    HabitIcon("savings", Icons.Default.Savings, "Savings"),
    HabitIcon("work", Icons.Default.Work, "Work"),
    HabitIcon("home", Icons.Default.Home, "Home"),
    HabitIcon("cleaning_services", Icons.Default.CleaningServices, "Cleaning"),
    HabitIcon("smoking_rooms", Icons.Default.SmokingRooms, "Quit Smoking"),
    HabitIcon("no_food", Icons.Default.NoFood, "Diet Control"),
    HabitIcon("phone_disabled", Icons.Default.PhoneDisabled, "Less Screen Time"),
    HabitIcon("videogame_asset_off", Icons.Default.VideogameAssetOff, "Less Gaming"),
)

// Get icon by name with fallback
fun getIconByName(name: String): ImageVector {
    return habitIcons.find { it.name == name }?.icon ?: Icons.Default.CheckCircle
}

// Get display name by icon name
fun getIconDisplayName(name: String): String {
    return habitIcons.find { it.name == name }?.displayName ?: "Habit"
}

