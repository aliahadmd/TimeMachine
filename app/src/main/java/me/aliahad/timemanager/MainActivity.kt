package me.aliahad.timemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.aliahad.timemanager.core.datastore.UserPreferences
import me.aliahad.timemanager.focus.FocusTimerService
import me.aliahad.timemanager.notifications.FinanceAndDigestWorker
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import me.aliahad.timemanager.ui.navigation.FinanceRoute
import me.aliahad.timemanager.ui.navigation.FocusRoute
import me.aliahad.timemanager.ui.navigation.HabitDetailRoute
import me.aliahad.timemanager.ui.navigation.PlannerRoute
import me.aliahad.timemanager.ui.navigation.SettingsRoute
import me.aliahad.timemanager.ui.navigation.TodayRoute
import me.aliahad.timemanager.ui.navigation.WellbeingRoute
import me.aliahad.timemanager.ui.theme.TimeManagerTheme

class MainActivity : ComponentActivity() {

    private var deepLinkState by mutableStateOf<DeepLinkRequest?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        NotificationChannelRegistry.ensureChannels(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationChannelRegistry.ensureChannels(this)
        ScreenTimeScheduler.ensurePeriodicWork(this)
        ScreenTimeScheduler.triggerImmediateSync(this)
        FinanceAndDigestWorker.ensureScheduled(this)

        parseDeepLink(intent)?.let { deepLinkState = it }

        enableEdgeToEdge()
        val app = application as TimeManagerApp
        lifecycleScope.launch {
            FocusTimerService.restoreIfRunning(this@MainActivity, app.container.preferencesRepository)
        }

        setContent {
            val prefs by app.container.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            val scope = rememberCoroutineScope()

            TimeManagerTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box {
                        TimeMachineNavigation(deepLinkRequest = deepLinkState)
                        TutorialModal(
                            visible = !prefs.onboardingCompleted,
                            onRequestNotifications = { requestNotificationsIfNeeded() },
                            onDismiss = {
                                scope.launch {
                                    app.container.preferencesRepository.setOnboardingCompleted(true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent)?.let { deepLinkState = it }
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        NotificationSettingsHelper.openNotificationSettings(this)
    }

    private fun parseDeepLink(intent: Intent?): DeepLinkRequest? {
        if (intent == null) return null
        val routeTarget = intent.getStringExtra(NotificationChannelRegistry.EXTRA_ROUTE_TARGET)
        val habitId = intent.getLongExtra(NotificationChannelRegistry.EXTRA_OPEN_HABIT_ID, -1L)
        val initialTab = intent.getIntExtra(NotificationChannelRegistry.EXTRA_INITIAL_TAB, 0)
        val openDate = intent.getStringExtra(NotificationChannelRegistry.EXTRA_OPEN_DATE)
        val catId = intent.getLongExtra(NotificationChannelRegistry.EXTRA_CATEGORY_ID, 0L)
            .takeIf { it > 0L }
        val taskId = intent.getLongExtra(NotificationChannelRegistry.EXTRA_TASK_ID, 0L)
            .takeIf { it > 0L }
        val autoStart = intent.getBooleanExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, false)

        val route: Any? = when (routeTarget) {
            NotificationChannelRegistry.ROUTE_TODAY -> TodayRoute
            NotificationChannelRegistry.ROUTE_PLANNER -> PlannerRoute(openDate = openDate)
            NotificationChannelRegistry.ROUTE_FOCUS -> FocusRoute(
                categoryId = catId,
                taskId = taskId,
                autoStart = autoStart
            )
            NotificationChannelRegistry.ROUTE_FINANCE -> FinanceRoute(initialTab = initialTab)
            NotificationChannelRegistry.ROUTE_WELLBEING -> WellbeingRoute(initialTab = initialTab)
            NotificationChannelRegistry.ROUTE_HABIT_DETAIL -> if (habitId > 0L) HabitDetailRoute(habitId) else WellbeingRoute(0)
            NotificationChannelRegistry.ROUTE_SETTINGS -> SettingsRoute
            else -> if (habitId > 0L) HabitDetailRoute(habitId) else null
        }
        return route?.let { DeepLinkRequest(System.nanoTime(), it) }
    }
}
