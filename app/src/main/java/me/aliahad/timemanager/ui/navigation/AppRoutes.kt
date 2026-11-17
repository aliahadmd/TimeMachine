package me.aliahad.timemanager.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
data class PlannerRoute(val openDate: String? = null)

@Serializable
data class FocusRoute(
    val categoryId: Long? = null,
    val taskId: Long? = null,
    val autoStart: Boolean = false
)

@Serializable
data class FinanceRoute(val initialTab: Int = 0)

@Serializable
data class WellbeingRoute(val initialTab: Int = 0)

@Serializable
data class HabitDetailRoute(val habitId: Long)

@Serializable
object ToolsRoute

@Serializable
object YearCalculatorRoute

@Serializable
object BMICalculatorRoute

@Serializable
object ProfileRoute

@Serializable
object SettingsRoute
