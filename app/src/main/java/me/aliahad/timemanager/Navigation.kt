package me.aliahad.timemanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.focus.FocusTimerService
import me.aliahad.timemanager.ui.navigation.BMICalculatorRoute
import me.aliahad.timemanager.ui.navigation.FinanceRoute
import me.aliahad.timemanager.ui.navigation.FocusRoute
import me.aliahad.timemanager.ui.navigation.HabitDetailRoute
import me.aliahad.timemanager.ui.navigation.PlannerRoute
import me.aliahad.timemanager.ui.navigation.ProfileRoute
import me.aliahad.timemanager.ui.navigation.SettingsRoute
import me.aliahad.timemanager.ui.navigation.TodayRoute
import me.aliahad.timemanager.ui.navigation.ToolsRoute
import me.aliahad.timemanager.ui.navigation.WellbeingRoute
import me.aliahad.timemanager.ui.navigation.YearCalculatorRoute
import me.aliahad.timemanager.ui.screens.DailyPlannerScreen
import me.aliahad.timemanager.ui.screens.FinanceScreen
import me.aliahad.timemanager.ui.screens.FocusTrackerScreen
import me.aliahad.timemanager.ui.screens.TodayScreen
import me.aliahad.timemanager.ui.screens.ToolsScreen
import me.aliahad.timemanager.ui.screens.WellbeingScreen
import me.aliahad.timemanager.ui.theme.TabularNumberStyle
import kotlin.reflect.KClass

data class DeepLinkRequest(
    val id: Long,
    val route: Any
)

private data class PillarTab(
    val label: String,
    val icon: ImageVector,
    val routeClass: KClass<*>,
    val createRoute: () -> Any
)

private val PILLAR_TABS = listOf(
    PillarTab("Today", Icons.Default.Home, TodayRoute::class) { TodayRoute },
    PillarTab("Planner", Icons.AutoMirrored.Filled.EventNote, PlannerRoute::class) { PlannerRoute() },
    PillarTab("Focus", Icons.Default.Timer, FocusRoute::class) { FocusRoute() },
    PillarTab("Finance", Icons.Default.AccountBalanceWallet, FinanceRoute::class) { FinanceRoute() },
    PillarTab("Wellbeing", Icons.Default.SelfImprovement, WellbeingRoute::class) { WellbeingRoute() }
)

@Composable
fun TimeMachineNavigation(deepLinkRequest: DeepLinkRequest? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val timerState by FocusTimerService.timerState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(deepLinkRequest?.id) {
        val req = deepLinkRequest ?: return@LaunchedEffect
        navController.navigate(req.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
        }
    }

    val isOnPrimaryTab = PILLAR_TABS.any { currentDestination?.hasRoute(it.routeClass) == true }
    val isOnFocusTab = currentDestination?.hasRoute(FocusRoute::class) == true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(visible = timerState.isRunning && !isOnFocusTab) {
                    val accent = Color(timerState.categoryColor.toInt())
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = accent.copy(alpha = 0.14f),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!isOnPrimaryTab) Modifier.navigationBarsPadding() else Modifier)
                            .clickable { navigateTopTab(FocusRoute()) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(timerState.categoryIcon)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (timerState.isBreakPhase) "Break" else timerState.categoryName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = Formatters.formatTimerClock(timerState.displaySeconds),
                                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    FocusTimerService.sendAction(
                                        context,
                                        if (timerState.isPaused) FocusTimerService.ACTION_RESUME
                                        else FocusTimerService.ACTION_PAUSE
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (timerState.isPaused) "Resume" else "Pause",
                                    tint = accent
                                )
                            }
                        }
                    }
                }

                if (isOnPrimaryTab) {
                    NavigationBar {
                        PILLAR_TABS.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.routeClass) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { if (!selected) navigateTopTab(tab.createRoute()) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val bottomPad = innerPadding.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPad)
                .consumeWindowInsets(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = TodayRoute
            ) {
                composable<TodayRoute> {
                    TodayScreen(
                        onNavigateToPlanner = { dateIso ->
                            navigateTopTab(PlannerRoute(openDate = dateIso))
                        },
                        onNavigateToFocus = { categoryId, taskId, autoStart ->
                            navController.navigate(
                                FocusRoute(
                                    categoryId = categoryId,
                                    taskId = taskId,
                                    autoStart = autoStart
                                )
                            ) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToFinance = { tab ->
                            navigateTopTab(FinanceRoute(initialTab = tab))
                        },
                        onNavigateToWellbeing = { tab ->
                            navigateTopTab(WellbeingRoute(initialTab = tab))
                        },
                        onNavigateToHabitDetail = { habitId ->
                            navController.navigate(HabitDetailRoute(habitId))
                        },
                        onNavigateToTools = { navController.navigate(ToolsRoute) },
                        onNavigateToYearCalculator = { navController.navigate(YearCalculatorRoute) },
                        onNavigateToBMICalculator = { navController.navigate(BMICalculatorRoute) },
                        onNavigateToProfile = { navController.navigate(ProfileRoute) },
                        onNavigateToSettings = { navController.navigate(SettingsRoute) }
                    )
                }

                composable<PlannerRoute> { backStackEntry ->
                    val args = backStackEntry.toRoute<PlannerRoute>()
                    DailyPlannerScreen(
                        initialDateIso = args.openDate,
                        onStartFocusFromTask = { categoryId, taskId ->
                            navController.navigate(
                                FocusRoute(
                                    categoryId = categoryId,
                                    taskId = taskId,
                                    autoStart = true
                                )
                            ) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable<FocusRoute> { backStackEntry ->
                    val args = backStackEntry.toRoute<FocusRoute>()
                    FocusTrackerScreen(
                        initialCategoryId = args.categoryId,
                        initialTaskId = args.taskId,
                        autoStart = args.autoStart
                    )
                }

                composable<FinanceRoute> { backStackEntry ->
                    val args = backStackEntry.toRoute<FinanceRoute>()
                    FinanceScreen(initialTab = args.initialTab)
                }

                composable<WellbeingRoute> { backStackEntry ->
                    val args = backStackEntry.toRoute<WellbeingRoute>()
                    WellbeingScreen(
                        initialTab = args.initialTab,
                        onOpenHabitDetail = { habitId ->
                            navController.navigate(HabitDetailRoute(habitId))
                        }
                    )
                }

                composable<HabitDetailRoute> { backStackEntry ->
                    val args = backStackEntry.toRoute<HabitDetailRoute>()
                    HabitDetailScreen(
                        habitId = args.habitId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<ToolsRoute> {
                    ToolsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenYearCalculator = { navController.navigate(YearCalculatorRoute) },
                        onOpenBMICalculator = { navController.navigate(BMICalculatorRoute) }
                    )
                }

                composable<YearCalculatorRoute> {
                    YearCalculatorScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable<BMICalculatorRoute> {
                    BMICalculatorScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable<ProfileRoute> {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }

                composable<SettingsRoute> {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
