package me.aliahad.timemanager

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.core.di.DefaultAppContainer
import me.aliahad.timemanager.notifications.FinanceAndDigestWorker
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import me.aliahad.timemanager.system.AppShortcutsManager

class TimeManagerApp : Application() {

    val container: AppContainer by lazy { DefaultAppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannelRegistry.ensureChannels(this)
        ScreenTimeEventReceiver.register(this)
        ScreenTimeScheduler.ensurePeriodicWork(this)
        FinanceAndDigestWorker.ensureScheduled(this)

        appScope.launch {
            val profile = container.database.userProfileDao().getProfileSync()
            container.preferencesRepository.migrateLegacyIfNeeded(profile)
            container.toolsRepository.getOrCreateProfile()
            container.financeRepository.ensureDefaultExpenseCategoriesIfNeeded()
            container.focusRepository.ensureDefaultFocusDataIfNeeded(container.preferencesRepository)
            val categories = container.focusRepository.activeCategories.first()
            AppShortcutsManager.syncDynamicShortcuts(this@TimeManagerApp, categories)
        }
    }
}
