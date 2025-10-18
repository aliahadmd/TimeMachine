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
    }
}
