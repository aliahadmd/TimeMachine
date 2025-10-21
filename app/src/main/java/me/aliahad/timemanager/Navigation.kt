package me.aliahad.timemanager

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable

@Composable
fun TimeMachineNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onBlockClick = { blockType ->
                    when (blockType) {
                        TimerBlockType.FOCUS_TIMER -> navController.navigate("focus_timer")
                        TimerBlockType.HABIT_TRACKER -> navController.navigate("habit_tracker")
                        TimerBlockType.YEAR_CALCULATOR -> navController.navigate("year_calculator")
                        TimerBlockType.BMI_CALCULATOR -> navController.navigate("bmi_calculator")
                        TimerBlockType.EXPENSE_TRACKER -> navController.navigate("expense_tracker")
                        TimerBlockType.SUBSCRIPTION_TRACKER -> navController.navigate("subscription_tracker")
                    }
                }
            )
        }
        
        composable("focus_timer") {
            TimerScreen(onBackPress = { navController.popBackStack() })
        }
        
        composable("habit_tracker") {
            HabitTrackerScreen(onBackPress = { navController.popBackStack() })
        }
        
        composable("year_calculator") {
            YearCalculatorScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable("bmi_calculator") {
            BMICalculatorScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable("expense_tracker") {
            ExpenseTrackerScreen(onBack = { navController.popBackStack() })
        }
        
        composable("subscription_tracker") {
            SubscriptionTrackerScreen(onBack = { navController.popBackStack() })
        }
    }
}
