package me.aliahad.timemanager.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.aliahad.timemanager.core.common.CurrencyCatalog
import me.aliahad.timemanager.data.UserProfile
import java.io.IOException

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timemachine_preferences"
)

enum class ThemeMode(val displayName: String) {
    AUTO("Auto (System)"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(raw: String?): ThemeMode {
            return when (raw?.trim()?.uppercase()) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> AUTO
            }
        }
    }
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val dynamicColor: Boolean = true,
    val currencyCode: String = CurrencyCatalog.DEFAULT_CODE,
    val dailyFocusGoalMinutes: Int = 120,
    val dailyScreenTimeLimitMinutes: Int = 240,
    val notificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,
    val quietEndMinute: Int = 0,
    val focusDndEnabled: Boolean = false,
    val focusStrictAlarmEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val plannerOnboardingSeeded: Boolean = false,
    val expenseCategoriesSeeded: Boolean = false,
    val focusDefaultsSeeded: Boolean = false,
    val lastScreenTimeNudgeDate: String = ""
) {
    val currencySymbol: String
        get() = CurrencyCatalog.symbolFor(currencyCode)
}

data class ActiveFocusTimerSnapshot(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val categoryId: Long = 0L,
    val categoryName: String = "Focus",
    val categoryIcon: String = "⏱️",
    val categoryColor: Long = 0xFF4DABF7L,
    val linkedTaskId: Long = 0L,
    val sessionStartWallMs: Long = 0L,
    val anchorElapsedRealtimeMs: Long = 0L,
    val accumulatedActiveSeconds: Int = 0,
    val targetDurationSeconds: Int = 0,
    val isPomodoro: Boolean = false,
    val breakDurationSeconds: Int = 300,
    val isBreakPhase: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DAILY_FOCUS_GOAL_MINUTES = intPreferencesKey("daily_focus_goal_minutes")
        val DAILY_SCREEN_TIME_LIMIT_MINUTES = intPreferencesKey("daily_screen_time_limit_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_START_MINUTE = intPreferencesKey("quiet_start_minute")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        val QUIET_END_MINUTE = intPreferencesKey("quiet_end_minute")
        val FOCUS_DND_ENABLED = booleanPreferencesKey("focus_dnd_enabled")
        val FOCUS_STRICT_ALARM_ENABLED = booleanPreferencesKey("focus_strict_alarm_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PLANNER_ONBOARDING_SEEDED = booleanPreferencesKey("planner_onboarding_seeded")
        val EXPENSE_CATEGORIES_SEEDED = booleanPreferencesKey("expense_categories_seeded")
        val FOCUS_DEFAULTS_SEEDED = booleanPreferencesKey("focus_defaults_seeded")
        val LAST_SCREEN_TIME_NUDGE_DATE = stringPreferencesKey("last_screen_time_nudge_date")
        val LEGACY_MIGRATED = booleanPreferencesKey("legacy_migrated")

        // Active Focus timer snapshot keys
        val TIMER_IS_RUNNING = booleanPreferencesKey("timer_is_running")
        val TIMER_IS_PAUSED = booleanPreferencesKey("timer_is_paused")
        val TIMER_CATEGORY_ID = longPreferencesKey("timer_category_id")
        val TIMER_CATEGORY_NAME = stringPreferencesKey("timer_category_name")
        val TIMER_CATEGORY_ICON = stringPreferencesKey("timer_category_icon")
        val TIMER_CATEGORY_COLOR = longPreferencesKey("timer_category_color")
        val TIMER_LINKED_TASK_ID = longPreferencesKey("timer_linked_task_id")
        val TIMER_SESSION_START_WALL_MS = longPreferencesKey("timer_session_start_wall_ms")
        val TIMER_ANCHOR_ELAPSED_MS = longPreferencesKey("timer_anchor_elapsed_ms")
        val TIMER_ACCUMULATED_SECONDS = intPreferencesKey("timer_accumulated_seconds")
        val TIMER_TARGET_SECONDS = intPreferencesKey("timer_target_seconds")
        val TIMER_IS_POMODORO = booleanPreferencesKey("timer_is_pomodoro")
        val TIMER_BREAK_SECONDS = intPreferencesKey("timer_break_seconds")
        val TIMER_IS_BREAK_PHASE = booleanPreferencesKey("timer_is_break_phase")
    }

    private val dataStore = context.applicationContext.userPreferencesDataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserPreferences(
                themeMode = ThemeMode.fromString(prefs[Keys.THEME_MODE]),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                currencyCode = prefs[Keys.CURRENCY_CODE] ?: CurrencyCatalog.DEFAULT_CODE,
                dailyFocusGoalMinutes = prefs[Keys.DAILY_FOCUS_GOAL_MINUTES] ?: 120,
                dailyScreenTimeLimitMinutes = prefs[Keys.DAILY_SCREEN_TIME_LIMIT_MINUTES] ?: 240,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
                quietStartHour = prefs[Keys.QUIET_START_HOUR] ?: 22,
                quietStartMinute = prefs[Keys.QUIET_START_MINUTE] ?: 0,
                quietEndHour = prefs[Keys.QUIET_END_HOUR] ?: 7,
                quietEndMinute = prefs[Keys.QUIET_END_MINUTE] ?: 0,
                focusDndEnabled = prefs[Keys.FOCUS_DND_ENABLED] ?: false,
                focusStrictAlarmEnabled = prefs[Keys.FOCUS_STRICT_ALARM_ENABLED] ?: false,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                plannerOnboardingSeeded = prefs[Keys.PLANNER_ONBOARDING_SEEDED] ?: false,
                expenseCategoriesSeeded = prefs[Keys.EXPENSE_CATEGORIES_SEEDED] ?: false,
                focusDefaultsSeeded = prefs[Keys.FOCUS_DEFAULTS_SEEDED] ?: false,
                lastScreenTimeNudgeDate = prefs[Keys.LAST_SCREEN_TIME_NUDGE_DATE] ?: ""
            )
        }

    val activeTimerSnapshotFlow: Flow<ActiveFocusTimerSnapshot> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            ActiveFocusTimerSnapshot(
                isRunning = prefs[Keys.TIMER_IS_RUNNING] ?: false,
                isPaused = prefs[Keys.TIMER_IS_PAUSED] ?: false,
                categoryId = prefs[Keys.TIMER_CATEGORY_ID] ?: 0L,
                categoryName = prefs[Keys.TIMER_CATEGORY_NAME] ?: "Focus",
                categoryIcon = prefs[Keys.TIMER_CATEGORY_ICON] ?: "⏱️",
                categoryColor = prefs[Keys.TIMER_CATEGORY_COLOR] ?: 0xFF4DABF7L,
                linkedTaskId = prefs[Keys.TIMER_LINKED_TASK_ID] ?: 0L,
                sessionStartWallMs = prefs[Keys.TIMER_SESSION_START_WALL_MS] ?: 0L,
                anchorElapsedRealtimeMs = prefs[Keys.TIMER_ANCHOR_ELAPSED_MS] ?: 0L,
                accumulatedActiveSeconds = prefs[Keys.TIMER_ACCUMULATED_SECONDS] ?: 0,
                targetDurationSeconds = prefs[Keys.TIMER_TARGET_SECONDS] ?: 0,
                isPomodoro = prefs[Keys.TIMER_IS_POMODORO] ?: false,
                breakDurationSeconds = prefs[Keys.TIMER_BREAK_SECONDS] ?: 300,
                isBreakPhase = prefs[Keys.TIMER_IS_BREAK_PHASE] ?: false
            )
        }

    suspend fun getPreferences(): UserPreferences = preferencesFlow.first()

    suspend fun getActiveTimerSnapshot(): ActiveFocusTimerSnapshot = activeTimerSnapshotFlow.first()

    suspend fun migrateLegacyIfNeeded(profile: UserProfile?) {
        val current = dataStore.data.first()
        if (current[Keys.LEGACY_MIGRATED] == true) return
        val legacyOnboarding = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val seenTutorial = legacyOnboarding.getBoolean("tutorial_seen", false)
        dataStore.edit { prefs ->
            prefs[Keys.LEGACY_MIGRATED] = true
            if (seenTutorial) prefs[Keys.ONBOARDING_COMPLETED] = true
            profile?.let { p ->
                if (prefs[Keys.THEME_MODE] == null) {
                    prefs[Keys.THEME_MODE] = ThemeMode.fromString(p.theme).name
                }
                if (prefs[Keys.CURRENCY_CODE] == null) {
                    val code = p.currencyCode.ifBlank { CurrencyCatalog.inferCodeFromSymbol(p.currency) }
                    prefs[Keys.CURRENCY_CODE] = code
                }
                if (prefs[Keys.DAILY_FOCUS_GOAL_MINUTES] == null && p.dailyGoalMinutes > 0) {
                    prefs[Keys.DAILY_FOCUS_GOAL_MINUTES] = p.dailyGoalMinutes
                }
                if (prefs[Keys.NOTIFICATIONS_ENABLED] == null) {
                    prefs[Keys.NOTIFICATIONS_ENABLED] = p.notificationsEnabled
                }
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { it[Keys.CURRENCY_CODE] = CurrencyCatalog.byCode(code).code }
    }

    suspend fun setDailyFocusGoalMinutes(minutes: Int) {
        dataStore.edit { it[Keys.DAILY_FOCUS_GOAL_MINUTES] = minutes.coerceIn(10, 1440) }
    }

    suspend fun setDailyScreenTimeLimitMinutes(minutes: Int) {
        dataStore.edit { it[Keys.DAILY_SCREEN_TIME_LIMIT_MINUTES] = minutes.coerceIn(15, 1440) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setQuietHours(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        dataStore.edit {
            it[Keys.QUIET_HOURS_ENABLED] = enabled
            it[Keys.QUIET_START_HOUR] = startHour.coerceIn(0, 23)
            it[Keys.QUIET_START_MINUTE] = startMinute.coerceIn(0, 59)
            it[Keys.QUIET_END_HOUR] = endHour.coerceIn(0, 23)
            it[Keys.QUIET_END_MINUTE] = endMinute.coerceIn(0, 59)
        }
    }

    suspend fun setFocusDndEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.FOCUS_DND_ENABLED] = enabled }
    }

    suspend fun setFocusStrictAlarmEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.FOCUS_STRICT_ALARM_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPlannerOnboardingSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.PLANNER_ONBOARDING_SEEDED] = seeded }
    }

    suspend fun setExpenseCategoriesSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.EXPENSE_CATEGORIES_SEEDED] = seeded }
    }

    suspend fun setFocusDefaultsSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.FOCUS_DEFAULTS_SEEDED] = seeded }
    }

    suspend fun setLastScreenTimeNudgeDate(date: String) {
        dataStore.edit { it[Keys.LAST_SCREEN_TIME_NUDGE_DATE] = date }
    }

    suspend fun saveActiveTimerSnapshot(snapshot: ActiveFocusTimerSnapshot) {
        dataStore.edit {
            it[Keys.TIMER_IS_RUNNING] = snapshot.isRunning
            it[Keys.TIMER_IS_PAUSED] = snapshot.isPaused
            it[Keys.TIMER_CATEGORY_ID] = snapshot.categoryId
            it[Keys.TIMER_CATEGORY_NAME] = snapshot.categoryName
            it[Keys.TIMER_CATEGORY_ICON] = snapshot.categoryIcon
            it[Keys.TIMER_CATEGORY_COLOR] = snapshot.categoryColor
            it[Keys.TIMER_LINKED_TASK_ID] = snapshot.linkedTaskId
            it[Keys.TIMER_SESSION_START_WALL_MS] = snapshot.sessionStartWallMs
            it[Keys.TIMER_ANCHOR_ELAPSED_MS] = snapshot.anchorElapsedRealtimeMs
            it[Keys.TIMER_ACCUMULATED_SECONDS] = snapshot.accumulatedActiveSeconds
            it[Keys.TIMER_TARGET_SECONDS] = snapshot.targetDurationSeconds
            it[Keys.TIMER_IS_POMODORO] = snapshot.isPomodoro
            it[Keys.TIMER_BREAK_SECONDS] = snapshot.breakDurationSeconds
            it[Keys.TIMER_IS_BREAK_PHASE] = snapshot.isBreakPhase
        }
    }

    suspend fun clearActiveTimerSnapshot() {
        saveActiveTimerSnapshot(ActiveFocusTimerSnapshot())
    }
}
