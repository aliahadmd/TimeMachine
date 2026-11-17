package me.aliahad.timemanager.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aliahad.timemanager.DataBackupManager
import me.aliahad.timemanager.ImportMode
import me.aliahad.timemanager.ImportStats
import me.aliahad.timemanager.ProfileAnalytics
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.core.datastore.ThemeMode
import me.aliahad.timemanager.core.datastore.UserPreferences
import me.aliahad.timemanager.core.di.AppContainer
import me.aliahad.timemanager.data.BMICalculation
import me.aliahad.timemanager.data.DateCalculation
import me.aliahad.timemanager.data.UserProfile
import me.aliahad.timemanager.data.UserStatistics
import me.aliahad.timemanager.permissions.DeviceReliabilityManager
import me.aliahad.timemanager.permissions.ReliabilityCheckItem

class YearCalculatorViewModel(private val container: AppContainer) : ViewModel() {
    val savedCalculations: StateFlow<List<DateCalculation>> = container.toolsRepository
        .allDateCalculations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCalculation(item: DateCalculation, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) container.toolsRepository.updateDateCalculation(item)
            else container.toolsRepository.insertDateCalculation(item)
        }
    }

    fun deleteCalculation(item: DateCalculation) {
        viewModelScope.launch { container.toolsRepository.deleteDateCalculation(item) }
    }
}

class BMICalculatorViewModel(private val container: AppContainer) : ViewModel() {
    val savedRecords: StateFlow<List<BMICalculation>> = container.toolsRepository
        .allBmiCalculations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveRecord(item: BMICalculation, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) container.toolsRepository.updateBmiCalculation(item)
            else container.toolsRepository.insertBmiCalculation(item)
        }
    }

    fun deleteRecord(item: BMICalculation) {
        viewModelScope.launch { container.toolsRepository.deleteBmiCalculation(item) }
    }
}

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val stats: UserStatistics? = null,
    val currencySymbol: String = "$"
)

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    private val statsFlow = MutableStateFlow<UserStatistics?>(null)

    init {
        viewModelScope.launch {
            container.toolsRepository.getOrCreateProfile()
            refreshStats()
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        container.toolsRepository.userProfile,
        container.preferencesRepository.preferencesFlow,
        statsFlow
    ) { profile, prefs, stats ->
        ProfileUiState(
            profile = profile ?: UserProfile(),
            stats = stats,
            currencySymbol = prefs.currencySymbol
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun refreshStats() {
        viewModelScope.launch {
            statsFlow.value = ProfileAnalytics.getUserStatistics(container.database)
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            container.toolsRepository.updateProfile(profile)
        }
    }
}

sealed interface BackupActionStatus {
    data object Idle : BackupActionStatus
    data object Working : BackupActionStatus
    data class Success(val message: String) : BackupActionStatus
    data class Error(val message: String) : BackupActionStatus
}

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val reliabilityChecks: List<ReliabilityCheckItem> = emptyList(),
    val backupStatus: BackupActionStatus = BackupActionStatus.Idle,
    val isXiaomiDevice: Boolean = DeviceReliabilityManager.isXiaomiOrHyperOs()
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val checksFlow = MutableStateFlow(
        DeviceReliabilityManager.inspectDeviceHealth(container.appContext)
    )
    private val backupStatusFlow = MutableStateFlow<BackupActionStatus>(BackupActionStatus.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
        container.preferencesRepository.preferencesFlow,
        checksFlow,
        backupStatusFlow
    ) { prefs, checks, backupStatus ->
        SettingsUiState(
            preferences = prefs,
            reliabilityChecks = checks,
            backupStatus = backupStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun refreshReliabilityChecks() {
        checksFlow.value = DeviceReliabilityManager.inspectDeviceHealth(container.appContext)
    }

    fun clearBackupStatus() {
        backupStatusFlow.value = BackupActionStatus.Idle
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            container.preferencesRepository.setThemeMode(mode)
            val profile = container.toolsRepository.getOrCreateProfile()
            container.toolsRepository.updateProfile(profile.copy(theme = mode.name.lowercase()))
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.preferencesRepository.setDynamicColor(enabled) }
    }

    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            val spec = CurrencyCatalog.byCode(code)
            container.preferencesRepository.setCurrencyCode(spec.code)
            val profile = container.toolsRepository.getOrCreateProfile()
            container.toolsRepository.updateProfile(
                profile.copy(currency = spec.symbol, currencyCode = spec.code)
            )
        }
    }

    fun setDailyFocusGoalMinutes(minutes: Int) {
        viewModelScope.launch {
            container.preferencesRepository.setDailyFocusGoalMinutes(minutes)
            val profile = container.toolsRepository.getOrCreateProfile()
            container.toolsRepository.updateProfile(profile.copy(dailyGoalMinutes = minutes))
            container.notificationGateway.requestWidgetRefresh()
        }
    }

    fun setDailyScreenTimeLimitMinutes(minutes: Int) {
        viewModelScope.launch {
            container.preferencesRepository.setDailyScreenTimeLimitMinutes(minutes)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.preferencesRepository.setNotificationsEnabled(enabled)
            val profile = container.toolsRepository.getOrCreateProfile()
            container.toolsRepository.updateProfile(profile.copy(notificationsEnabled = enabled))
        }
    }

    fun setQuietHours(enabled: Boolean, startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        viewModelScope.launch {
            container.preferencesRepository.setQuietHours(enabled, startHour, startMin, endHour, endMin)
        }
    }

    fun setFocusDndEnabled(enabled: Boolean) {
        viewModelScope.launch { container.preferencesRepository.setFocusDndEnabled(enabled) }
    }

    fun setFocusStrictAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch { container.preferencesRepository.setFocusStrictAlarmEnabled(enabled) }
    }

    fun replayTutorial() {
        viewModelScope.launch { container.preferencesRepository.setOnboardingCompleted(false) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupStatusFlow.value = BackupActionStatus.Working
            val exportRes = DataBackupManager.exportData(container.appContext, container.database)
            exportRes.fold(
                onSuccess = { backup ->
                    val saveRes = DataBackupManager.saveBackupToFile(container.appContext, uri, backup)
                    backupStatusFlow.value = saveRes.fold(
                        onSuccess = { BackupActionStatus.Success("Backup exported successfully") },
                        onFailure = { e -> BackupActionStatus.Error(e.message ?: "Failed writing backup file") }
                    )
                },
                onFailure = { e ->
                    backupStatusFlow.value = BackupActionStatus.Error(e.message ?: "Export failed")
                }
            )
        }
    }

    fun importBackup(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            backupStatusFlow.value = BackupActionStatus.Working
            val res = DataBackupManager.importData(
                context = container.appContext,
                uri = uri,
                database = container.database,
                mode = mode,
                preferencesRepository = container.preferencesRepository,
                onAfterImport = {
                    container.notificationGateway.syncAllReminders()
                    container.notificationGateway.requestWidgetRefresh()
                }
            )
            backupStatusFlow.value = res.fold(
                onSuccess = { stats: ImportStats ->
                    BackupActionStatus.Success(
                        "Restored ${stats.getTotalImported()} records (${mode.name.lowercase()}): " +
                            "${stats.habitsImported} habits, ${stats.focusSessionsImported} sessions, " +
                            "${stats.expensesImported} expenses, ${stats.tasksImported} tasks"
                    )
                },
                onFailure = { e ->
                    BackupActionStatus.Error(e.message ?: "Backup import failed")
                }
            )
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            backupStatusFlow.value = BackupActionStatus.Working
            withContext(Dispatchers.IO) {
                container.notificationGateway.cancelAllReminders()
                container.database.clearAllTables()
                container.preferencesRepository.clearActiveTimerSnapshot()
                container.preferencesRepository.setPlannerOnboardingSeeded(false)
                container.preferencesRepository.setExpenseCategoriesSeeded(false)
                container.preferencesRepository.setFocusDefaultsSeeded(false)
                container.financeRepository.ensureDefaultExpenseCategoriesIfNeeded()
                container.focusRepository.ensureDefaultFocusDataIfNeeded(container.preferencesRepository)
                container.toolsRepository.getOrCreateProfile()
            }
            container.notificationGateway.requestWidgetRefresh()
            backupStatusFlow.value = BackupActionStatus.Success("All local records cleared (app preferences preserved)")
        }
    }
}
