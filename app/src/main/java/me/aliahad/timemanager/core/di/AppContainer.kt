package me.aliahad.timemanager.core.di

import android.content.Context
import me.aliahad.timemanager.TimeManagerApp
import me.aliahad.timemanager.core.common.RoomTransactionRunner
import me.aliahad.timemanager.core.common.SystemTimeProvider
import me.aliahad.timemanager.core.common.TimeProvider
import me.aliahad.timemanager.core.common.TransactionRunner
import me.aliahad.timemanager.core.datastore.UserPreferencesRepository
import me.aliahad.timemanager.data.HabitRepository
import me.aliahad.timemanager.data.TimerDatabase
import me.aliahad.timemanager.data.repository.FinanceRepository
import me.aliahad.timemanager.data.repository.FocusRepository
import me.aliahad.timemanager.data.repository.PlannerRepository
import me.aliahad.timemanager.data.repository.ScreenTimeRepository
import me.aliahad.timemanager.data.repository.ToolsRepository
import me.aliahad.timemanager.domain.gateway.SystemNotificationGateway
import me.aliahad.timemanager.domain.usecase.FinanceUseCases
import me.aliahad.timemanager.domain.usecase.FocusCompletionUseCase
import me.aliahad.timemanager.domain.usecase.HabitSyncUseCase
import me.aliahad.timemanager.notifications.AndroidSystemNotificationGateway
import me.aliahad.timemanager.widgets.WidgetUpdater

interface AppContainer {
    val appContext: Context
    val database: TimerDatabase
    val preferencesRepository: UserPreferencesRepository
    val timeProvider: TimeProvider
    val transactionRunner: TransactionRunner
    val notificationGateway: SystemNotificationGateway

    val focusRepository: FocusRepository
    val habitRepository: HabitRepository
    val plannerRepository: PlannerRepository
    val financeRepository: FinanceRepository
    val screenTimeRepository: ScreenTimeRepository
    val toolsRepository: ToolsRepository

    val focusCompletionUseCase: FocusCompletionUseCase
    val habitSyncUseCase: HabitSyncUseCase
    val financeUseCases: FinanceUseCases
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val appContext: Context = context.applicationContext
    override val database: TimerDatabase by lazy { TimerDatabase.getDatabase(appContext) }
    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }
    override val timeProvider: TimeProvider by lazy { SystemTimeProvider() }
    override val transactionRunner: TransactionRunner by lazy { RoomTransactionRunner(database) }

    override val notificationGateway: SystemNotificationGateway by lazy {
        AndroidSystemNotificationGateway(
            context = appContext,
            database = database,
            preferencesRepository = preferencesRepository,
            widgetRefreshAction = { ctx -> WidgetUpdater.refreshAll(ctx) }
        )
    }

    override val focusRepository: FocusRepository by lazy {
        FocusRepository(
            activityCategoryDao = database.activityCategoryDao(),
            timeSessionDao = database.timeSessionDao(),
            presetDao = database.presetDao(),
            timeProvider = timeProvider,
            notificationGateway = notificationGateway
        )
    }

    override val habitRepository: HabitRepository by lazy {
        HabitRepository(
            habitDao = database.habitDao(),
            timeProvider = timeProvider
        )
    }

    override val plannerRepository: PlannerRepository by lazy {
        PlannerRepository(
            dailyTaskDao = database.dailyTaskDao(),
            preferencesRepository = preferencesRepository,
            timeProvider = timeProvider,
            notificationGateway = notificationGateway
        )
    }

    override val financeRepository: FinanceRepository by lazy {
        FinanceRepository(
            expenseDao = database.expenseDao(),
            expenseCategoryDao = database.expenseCategoryDao(),
            subscriptionDao = database.subscriptionDao(),
            preferencesRepository = preferencesRepository,
            timeProvider = timeProvider
        )
    }

    override val screenTimeRepository: ScreenTimeRepository by lazy {
        ScreenTimeRepository(
            screenTimeDao = database.screenTimeDao(),
            timeProvider = timeProvider
        )
    }

    override val toolsRepository: ToolsRepository by lazy {
        ToolsRepository(
            dateCalculationDao = database.dateCalculationDao(),
            bmiCalculationDao = database.bmiCalculationDao(),
            userProfileDao = database.userProfileDao()
        )
    }

    override val focusCompletionUseCase: FocusCompletionUseCase by lazy {
        FocusCompletionUseCase(
            transactionRunner = transactionRunner,
            activityCategoryDao = database.activityCategoryDao(),
            timeSessionDao = database.timeSessionDao(),
            dailyTaskDao = database.dailyTaskDao(),
            habitDao = database.habitDao(),
            timeProvider = timeProvider,
            notificationGateway = notificationGateway
        )
    }

    override val habitSyncUseCase: HabitSyncUseCase by lazy {
        HabitSyncUseCase(
            habitDao = database.habitDao(),
            habitRepository = habitRepository,
            timeProvider = timeProvider,
            notificationGateway = notificationGateway
        )
    }

    override val financeUseCases: FinanceUseCases by lazy {
        FinanceUseCases(
            transactionRunner = transactionRunner,
            expenseDao = database.expenseDao(),
            expenseCategoryDao = database.expenseCategoryDao(),
            subscriptionDao = database.subscriptionDao(),
            preferencesRepository = preferencesRepository,
            timeProvider = timeProvider,
            notificationGateway = notificationGateway
        )
    }
}

fun Context.appContainerOrNull(): AppContainer? =
    (applicationContext as? TimeManagerApp)?.container
