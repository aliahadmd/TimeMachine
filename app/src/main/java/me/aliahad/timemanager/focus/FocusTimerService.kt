package me.aliahad.timemanager.focus

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.aliahad.timemanager.ImmersiveTimerActivity
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.R
import me.aliahad.timemanager.core.common.Formatters
import me.aliahad.timemanager.core.common.toStorageLong
import me.aliahad.timemanager.core.datastore.ActiveFocusTimerSnapshot
import me.aliahad.timemanager.core.di.appContainerOrNull
import me.aliahad.timemanager.domain.usecase.FocusSaveResult
import me.aliahad.timemanager.notifications.NotificationChannelRegistry
import me.aliahad.timemanager.scheduleReliableAlarm

data class FocusTimerUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val categoryId: Long = 0L,
    val categoryName: String = "Focus",
    val categoryIcon: String = "🎯",
    val categoryColor: Long = 0xFF4DABF7L,
    val linkedTaskId: Long = 0L,
    val elapsedSeconds: Int = 0,
    val targetDurationSeconds: Int = 0,
    val isPomodoro: Boolean = false,
    val breakDurationSeconds: Int = 300,
    val isBreakPhase: Boolean = false,
    val sessionStartWallMs: Long = 0L,
    val lastSavedResult: FocusSaveResult? = null
) {
    val isCountdown: Boolean get() = targetDurationSeconds > 0
    val remainingSeconds: Int
        get() = if (targetDurationSeconds > 0) (targetDurationSeconds - elapsedSeconds).coerceAtLeast(0) else 0
    val displaySeconds: Int
        get() = if (isCountdown) remainingSeconds else elapsedSeconds
    val progressFraction: Float
        get() = if (targetDurationSeconds > 0) {
            (elapsedSeconds.toFloat() / targetDurationSeconds).coerceIn(0f, 1f)
        } else 0f
}

class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private var currentCategoryId = 0L
    private var currentCategoryName = "Focus"
    private var currentCategoryIcon = "🎯"
    private var currentCategoryColor = 0xFF4DABF7L
    private var currentLinkedTaskId = 0L
    private var currentStartWallMs = 0L
    private var anchorElapsedRealtimeMs = 0L
    private var accumulatedActiveSeconds = 0
    private var targetDurationSeconds = 0
    private var isPomodoro = false
    private var breakDurationSeconds = 300
    private var isBreakPhase = false
    private var isPaused = false
    private var isRunning = false
    private var finishingSession = false
    private var lastNotificationUpdateSecond = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannelRegistry.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationChannelRegistry.ensureChannels(this)
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_ADD_FIVE_MIN -> handleAddFiveMinutes()
            ACTION_STOP_AND_SAVE -> handleStopAndSave()
            ACTION_SKIP_BREAK -> handleSkipBreak()
            ACTION_DISCARD -> handleDiscard()
            ACTION_COMPLETE_ALARM -> {
                if (!isRunning) promoteForeground()
                handleCompleteAlarm()
            }
            ACTION_RESTORE_IF_ACTIVE, null -> {
                if (!isRunning) promoteForeground()
                handleRestoreIfNeeded()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        currentCategoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, 0L)
        currentCategoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Focus"
        currentCategoryIcon = intent.getStringExtra(EXTRA_CATEGORY_ICON) ?: "🎯"
        currentCategoryColor = intent.getLongExtra(EXTRA_CATEGORY_COLOR, 0xFF4DABF7L)
        currentLinkedTaskId = intent.getLongExtra(EXTRA_LINKED_TASK_ID, 0L)
        targetDurationSeconds = intent.getIntExtra(EXTRA_TARGET_SECONDS, 0).coerceAtLeast(0)
        isPomodoro = intent.getBooleanExtra(EXTRA_IS_POMODORO, false)
        breakDurationSeconds = intent.getIntExtra(EXTRA_BREAK_SECONDS, 300).coerceAtLeast(60)
        isBreakPhase = false
        isPaused = false
        isRunning = true
        finishingSession = false
        accumulatedActiveSeconds = 0
        currentStartWallMs = System.currentTimeMillis()
        anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
        lastNotificationUpdateSecond = -1

        publishState(clearLastSaved = true)
        promoteForeground()
        persistSnapshotAsync()
        syncZenRuleAsync(active = true)
        scheduleCompletionAnchor()
        startTicker()
    }

    private fun handlePause() {
        if (!isRunning || isPaused) return
        accumulatedActiveSeconds = computeElapsedSeconds()
        isPaused = true
        cancelCompletionAnchor()
        publishState()
        updateForegroundNotification()
        persistSnapshotAsync()
        syncZenRuleAsync(active = false)
    }

    private fun handleResume() {
        if (!isRunning || !isPaused) return
        anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
        isPaused = false
        lastNotificationUpdateSecond = -1
        publishState()
        promoteForeground()
        persistSnapshotAsync()
        syncZenRuleAsync(active = !isBreakPhase)
        scheduleCompletionAnchor()
        startTicker()
    }

    private fun handleAddFiveMinutes() {
        if (!isRunning) return
        if (targetDurationSeconds <= 0) {
            targetDurationSeconds = computeElapsedSeconds() + 300
        } else {
            targetDurationSeconds += 300
        }
        publishState()
        updateForegroundNotification()
        persistSnapshotAsync()
        scheduleCompletionAnchor()
    }

    private fun handleStopAndSave() {
        if (!isRunning || finishingSession) {
            serviceScope.launch { stopServiceCleanly() }
            return
        }
        finishingSession = true
        tickerJob?.cancel()
        val elapsedSec = computeElapsedSeconds()
        if (isBreakPhase) {
            serviceScope.launch {
                stopServiceCleanly()
            }
            return
        }
        val durationMinutes = (elapsedSec / 60)
        if (durationMinutes < 1) {
            serviceScope.launch {
                stopServiceCleanly()
            }
            return
        }

        serviceScope.launch {
            val result = saveFocusToRepository(durationMinutes)
            _timerState.value = _timerState.value.copy(
                isRunning = false,
                isPaused = false,
                elapsedSeconds = 0,
                lastSavedResult = result
            )
            stopServiceCleanly(preserveLastSaved = true)
        }
    }

    private fun handleSkipBreak() {
        if (!isRunning) return
        serviceScope.launch {
            stopServiceCleanly()
        }
    }

    private fun handleDiscard() {
        finishingSession = true
        tickerJob?.cancel()
        serviceScope.launch {
            stopServiceCleanly()
        }
    }

    private fun handleCompleteAlarm() {
        if (isRunning && !isPaused && targetDurationSeconds > 0 &&
            computeElapsedSeconds() >= targetDurationSeconds
        ) {
            serviceScope.launch { completeTimedPhase() }
        } else if (!isRunning) {
            handleRestoreIfNeeded(stopImmediatelyIfIdle = true)
        }
    }

    private fun handleRestoreIfNeeded(stopImmediatelyIfIdle: Boolean = false) {
        if (isRunning) {
            promoteForeground()
            return
        }
        serviceScope.launch {
            val container = appContainerOrNull()
            val snapshot = container?.preferencesRepository?.getActiveTimerSnapshot()
            if (snapshot == null || !snapshot.isRunning) {
                ServiceCompat.stopForeground(
                    this@FocusTimerService,
                    ServiceCompat.STOP_FOREGROUND_REMOVE
                )
                stopSelf()
                return@launch
            }
            currentCategoryId = snapshot.categoryId
            currentCategoryName = snapshot.categoryName
            currentCategoryIcon = snapshot.categoryIcon
            currentCategoryColor = snapshot.categoryColor
            currentLinkedTaskId = snapshot.linkedTaskId
            targetDurationSeconds = snapshot.targetDurationSeconds
            isPomodoro = snapshot.isPomodoro
            breakDurationSeconds = snapshot.breakDurationSeconds
            isBreakPhase = snapshot.isBreakPhase
            isPaused = snapshot.isPaused
            isRunning = true
            finishingSession = false
            currentStartWallMs = snapshot.sessionStartWallMs

            val nowElapsed = SystemClock.elapsedRealtime()
            if (snapshot.isPaused) {
                accumulatedActiveSeconds = snapshot.accumulatedActiveSeconds
                anchorElapsedRealtimeMs = nowElapsed
            } else if (snapshot.anchorElapsedRealtimeMs > nowElapsed) {
                // Device rebooted mid-session (`elapsedRealtime` reset to 0)
                val wallDelta = ((System.currentTimeMillis() - snapshot.sessionStartWallMs) / 1000L)
                    .toInt()
                    .coerceAtLeast(snapshot.accumulatedActiveSeconds)
                accumulatedActiveSeconds = wallDelta
                anchorElapsedRealtimeMs = nowElapsed
            } else {
                accumulatedActiveSeconds = snapshot.accumulatedActiveSeconds
                anchorElapsedRealtimeMs = snapshot.anchorElapsedRealtimeMs
            }

            if (targetDurationSeconds > 0 && computeElapsedSeconds() >= targetDurationSeconds) {
                completeTimedPhase()
                return@launch
            }

            publishState()
            promoteForeground()
            if (!isPaused) {
                syncZenRuleAsync(active = !isBreakPhase)
                startTicker()
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && isRunning) {
                if (!isPaused) {
                    val elapsed = computeElapsedSeconds()
                    if (targetDurationSeconds > 0 && elapsed >= targetDurationSeconds) {
                        completeTimedPhase()
                        break
                    }
                    publishState()
                    if (lastNotificationUpdateSecond < 0 ||
                        kotlin.math.abs(elapsed - lastNotificationUpdateSecond) >= 5
                    ) {
                        lastNotificationUpdateSecond = elapsed
                        updateForegroundNotification()
                    }
                }
                delay(500L)
            }
        }
    }

    private suspend fun completeTimedPhase() {
        if (finishingSession) return
        finishingSession = true

        if (isBreakPhase) {
            postCompletionNotification(
                title = "Break complete",
                body = "Ready for your next $currentCategoryName session?"
            )
            stopServiceCleanly()
            return
        }

        val minutes = (targetDurationSeconds / 60).coerceAtLeast(1)
        val result = saveFocusToRepository(minutes)

        if (isPomodoro && breakDurationSeconds > 0) {
            postCompletionNotification(
                title = "Focus complete • ${Formatters.formatMinutes(minutes)} saved",
                body = "Starting ${breakDurationSeconds / 60}m Pomodoro break"
            )
            isBreakPhase = true
            isPaused = false
            finishingSession = false
            targetDurationSeconds = breakDurationSeconds
            accumulatedActiveSeconds = 0
            anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
            lastNotificationUpdateSecond = -1
            syncZenRuleAsync(active = false)
            publishState(overrideLastSaved = result)
            updateForegroundNotification()
            persistSnapshotAsync()
            startTicker()
        } else {
            postCompletionNotification(
                title = "$currentCategoryName complete",
                body = "${Formatters.formatMinutes(minutes)} logged" +
                    (result?.autoCompletedHabits?.takeIf { it.isNotEmpty() }
                        ?.let { " • Auto-completed ${it.joinToString(", ")}" } ?: "")
            )
            _timerState.value = _timerState.value.copy(
                isRunning = false,
                isPaused = false,
                elapsedSeconds = 0,
                lastSavedResult = result
            )
            stopServiceCleanly(preserveLastSaved = true)
        }
    }

    private suspend fun saveFocusToRepository(durationMinutes: Int): FocusSaveResult? {
        val container = appContainerOrNull() ?: return null
        if (currentCategoryId <= 0L) return null
        val endWall = System.currentTimeMillis()
        val startWall = currentStartWallMs.takeIf { it in 1..endWall }
            ?: (endWall - durationMinutes * 60_000L)
        return try {
            container.focusCompletionUseCase.saveSessionAndPropagate(
                categoryId = currentCategoryId,
                startTimeWallMs = startWall,
                endTimeWallMs = endWall,
                durationMinutes = durationMinutes,
                notes = "",
                linkedTaskId = currentLinkedTaskId.takeIf { it > 0L }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed saving completed focus session", t)
            null
        }
    }

    private fun computeElapsedSeconds(): Int {
        if (!isRunning) return 0
        if (isPaused) return accumulatedActiveSeconds.coerceAtLeast(0)
        val liveDelta = ((SystemClock.elapsedRealtime() - anchorElapsedRealtimeMs) / 1000L).toInt()
            .coerceAtLeast(0)
        return (accumulatedActiveSeconds + liveDelta).coerceAtLeast(0)
    }

    private fun publishState(
        clearLastSaved: Boolean = false,
        overrideLastSaved: FocusSaveResult? = null
    ) {
        val lastSaved = when {
            overrideLastSaved != null -> overrideLastSaved
            clearLastSaved -> null
            else -> _timerState.value.lastSavedResult
        }
        _timerState.value = FocusTimerUiState(
            isRunning = isRunning,
            isPaused = isPaused,
            categoryId = currentCategoryId,
            categoryName = currentCategoryName,
            categoryIcon = currentCategoryIcon,
            categoryColor = currentCategoryColor,
            linkedTaskId = currentLinkedTaskId,
            elapsedSeconds = computeElapsedSeconds(),
            targetDurationSeconds = targetDurationSeconds,
            isPomodoro = isPomodoro,
            breakDurationSeconds = breakDurationSeconds,
            isBreakPhase = isBreakPhase,
            sessionStartWallMs = currentStartWallMs,
            lastSavedResult = lastSaved
        )
    }

    private fun persistSnapshotAsync() {
        val snapshot = ActiveFocusTimerSnapshot(
            isRunning = isRunning,
            isPaused = isPaused,
            categoryId = currentCategoryId,
            categoryName = currentCategoryName,
            categoryIcon = currentCategoryIcon,
            categoryColor = currentCategoryColor,
            linkedTaskId = currentLinkedTaskId,
            sessionStartWallMs = currentStartWallMs,
            anchorElapsedRealtimeMs = anchorElapsedRealtimeMs,
            accumulatedActiveSeconds = accumulatedActiveSeconds,
            targetDurationSeconds = targetDurationSeconds,
            isPomodoro = isPomodoro,
            breakDurationSeconds = breakDurationSeconds,
            isBreakPhase = isBreakPhase
        )
        serviceScope.launch(Dispatchers.IO) {
            appContainerOrNull()?.preferencesRepository?.saveActiveTimerSnapshot(snapshot)
        }
    }

    private fun syncZenRuleAsync(active: Boolean) {
        serviceScope.launch(Dispatchers.IO) {
            val container = appContainerOrNull() ?: return@launch
            val prefs = container.preferencesRepository.getPreferences()
            if (prefs.focusDndEnabled && active) {
                FocusZenManager.setFocusZenActive(this@FocusTimerService, true, currentCategoryName)
            } else {
                FocusZenManager.setFocusZenActive(this@FocusTimerService, false)
            }
        }
    }

    private fun promoteForeground() {
        val notification = buildLiveNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(
            this,
            NotificationChannelRegistry.FOCUS_LIVE_NOTIF_ID,
            notification,
            fgsType
        )
    }

    private fun updateForegroundNotification() {
        if (!NotificationChannelRegistry.hasPostNotificationsPermission(this)) return
        try {
            NotificationManagerCompat.from(this).notify(
                NotificationChannelRegistry.FOCUS_LIVE_NOTIF_ID,
                buildLiveNotification()
            )
        } catch (sec: SecurityException) {
            Log.w(TAG, "Unable to update live notification", sec)
        }
    }

    @SuppressLint("WrongConstant")
    private fun buildLiveNotification(): Notification {
        val state = _timerState.value
        val displaySecs = state.displaySeconds
        val timeText = Formatters.formatTimerClock(displaySecs)
        val shortChip = when {
            state.isPaused -> "PAUSED"
            state.isBreakPhase -> "BRK $timeText"
            else -> timeText
        }

        val title = when {
            state.isBreakPhase -> "Break Time"
            state.isPaused -> "${state.categoryName} (Paused)"
            else -> "${state.categoryIcon} ${state.categoryName}"
        }
        val contentText = when {
            state.isPaused -> "Paused at $timeText"
            state.isCountdown -> "$timeText remaining"
            else -> "$timeText elapsed"
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FOCUS
            )
        }
        val contentPending = PendingIntent.getActivity(
            this,
            NotificationChannelRegistry.FOCUS_ACTION_BASE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (state.isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePending = PendingIntent.getService(
            this,
            NotificationChannelRegistry.FOCUS_ACTION_BASE + 1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addFiveIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_ADD_FIVE_MIN
        }
        val addFivePending = PendingIntent.getService(
            this,
            NotificationChannelRegistry.FOCUS_ACTION_BASE + 2,
            addFiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (state.isBreakPhase) ACTION_SKIP_BREAK else ACTION_STOP_AND_SAVE
        }
        val stopPending = PendingIntent.getService(
            this,
            NotificationChannelRegistry.FOCUS_ACTION_BASE + 3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val whenMs = if (state.isCountdown) {
            System.currentTimeMillis() + (state.remainingSeconds * 1000L)
        } else {
            System.currentTimeMillis() - (state.elapsedSeconds * 1000L)
        }

        if (Build.VERSION.SDK_INT >= 36) {
            val icon = Icon.createWithResource(this, R.drawable.ic_stat_timemachine)
            val builder = Notification.Builder(
                this,
                NotificationChannelRegistry.CHANNEL_FOCUS_LIVE
            )
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STOPWATCH)
                .setFlag(Notification.FLAG_PROMOTED_ONGOING, true)
                .setShortCriticalText(shortChip)
                .setContentIntent(contentPending)

            if (!state.isPaused) {
                builder.setUsesChronometer(true)
                    .setChronometerCountDown(state.isCountdown)
                    .setWhen(whenMs)
            }

            if (state.targetDurationSeconds > 0) {
                val accentInt = state.categoryColor.toInt()
                val style = Notification.ProgressStyle()
                    .setProgress(state.elapsedSeconds.coerceIn(0, state.targetDurationSeconds))
                    .addProgressSegment(
                        Notification.ProgressStyle.Segment(state.targetDurationSeconds)
                            .setColor(accentInt)
                    )
                builder.setStyle(style)
            }

            builder.addAction(
                Notification.Action.Builder(
                    icon,
                    if (state.isPaused) "Resume" else "Pause",
                    togglePending
                ).build()
            )
            if (state.isCountdown && !state.isBreakPhase) {
                builder.addAction(
                    Notification.Action.Builder(icon, "+5m", addFivePending).build()
                )
            }
            builder.addAction(
                Notification.Action.Builder(
                    icon,
                    if (state.isBreakPhase) "Skip Break" else "Finish",
                    stopPending
                ).build()
            )
            return builder.build()
        }

        val compat = NotificationCompat.Builder(
            this,
            NotificationChannelRegistry.CHANNEL_FOCUS_LIVE
        )
            .setSmallIcon(R.drawable.ic_stat_timemachine)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPending)
            .addAction(0, if (state.isPaused) "Resume" else "Pause", togglePending)

        if (!state.isPaused) {
            compat.setUsesChronometer(true)
                .setChronometerCountDown(state.isCountdown)
                .setWhen(whenMs)
        }
        if (state.targetDurationSeconds > 0) {
            compat.setProgress(
                state.targetDurationSeconds,
                state.elapsedSeconds.coerceIn(0, state.targetDurationSeconds),
                false
            )
        }
        if (state.isCountdown && !state.isBreakPhase) {
            compat.addAction(0, "+5m", addFivePending)
        }
        compat.addAction(0, if (state.isBreakPhase) "Skip Break" else "Finish", stopPending)
        return compat.build()
    }

    private suspend fun postCompletionNotification(title: String, body: String) {
        if (!NotificationChannelRegistry.hasPostNotificationsPermission(this)) return
        val prefs = appContainerOrNull()?.preferencesRepository?.getPreferences()
        if (prefs != null && !prefs.notificationsEnabled) return

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FOCUS
            )
        }
        val tapPending = PendingIntent.getActivity(
            this,
            NotificationChannelRegistry.FOCUS_COMPLETE_NOTIF_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sprintIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                NotificationChannelRegistry.ROUTE_FOCUS
            )
            putExtra(NotificationChannelRegistry.EXTRA_CATEGORY_ID, currentCategoryId)
            putExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, true)
        }
        val sprintPending = PendingIntent.getActivity(
            this,
            NotificationChannelRegistry.FOCUS_COMPLETE_NOTIF_ID + 2,
            sprintIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            this,
            NotificationChannelRegistry.CHANNEL_FOCUS_COMPLETE
        )
            .setSmallIcon(R.drawable.ic_stat_timemachine)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .addAction(0, "New Sprint", sprintPending)

        if (prefs?.focusStrictAlarmEnabled == true) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val canFsi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                nm.canUseFullScreenIntent()
            } else true
            if (canFsi) {
                val fsiIntent = Intent(this, ImmersiveTimerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val fsiPending = PendingIntent.getActivity(
                    this,
                    NotificationChannelRegistry.FOCUS_COMPLETE_NOTIF_ID + 1,
                    fsiIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setFullScreenIntent(fsiPending, true)
            }
        }

        try {
            NotificationManagerCompat.from(this).notify(
                NotificationChannelRegistry.FOCUS_COMPLETE_NOTIF_ID,
                builder.build()
            )
        } catch (sec: SecurityException) {
            Log.w(TAG, "Unable to post completion alert", sec)
        }
    }

    private suspend fun stopServiceCleanly(preserveLastSaved: Boolean = false) {
        cancelCompletionAnchor()
        isRunning = false
        isPaused = false
        isBreakPhase = false
        accumulatedActiveSeconds = 0
        targetDurationSeconds = 0
        FocusZenManager.setFocusZenActive(this, false)
        appContainerOrNull()?.preferencesRepository?.clearActiveTimerSnapshot()
        _timerState.value = FocusTimerUiState(
            lastSavedResult = if (preserveLastSaved) _timerState.value.lastSavedResult else null
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleCompletionAnchor() {
        cancelCompletionAnchor()
        if (!isRunning || isPaused || targetDurationSeconds <= 0) return
        val remainingSec = (targetDurationSeconds - computeElapsedSeconds()).coerceAtLeast(1)
        val triggerAtWall = System.currentTimeMillis() + (remainingSec * 1000L)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_COMPLETE_ALARM
        }
        val pi = PendingIntent.getForegroundService(
            this,
            NotificationChannelRegistry.FOCUS_ACTION_BASE + 99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.scheduleReliableAlarm(triggerAtWall, pi, TAG)
    }

    private fun cancelCompletionAnchor() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_COMPLETE_ALARM
        }
        val pi = PendingIntent.getForegroundService(
            this,
            NotificationChannelRegistry.FOCUS_ACTION_BASE + 99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    companion object {
        private const val TAG = "FocusTimerService"

        const val ACTION_START = "me.aliahad.timemanager.focus.START"
        const val ACTION_PAUSE = "me.aliahad.timemanager.focus.PAUSE"
        const val ACTION_RESUME = "me.aliahad.timemanager.focus.RESUME"
        const val ACTION_ADD_FIVE_MIN = "me.aliahad.timemanager.focus.ADD_FIVE"
        const val ACTION_STOP_AND_SAVE = "me.aliahad.timemanager.focus.STOP_AND_SAVE"
        const val ACTION_SKIP_BREAK = "me.aliahad.timemanager.focus.SKIP_BREAK"
        const val ACTION_DISCARD = "me.aliahad.timemanager.focus.DISCARD"
        const val ACTION_RESTORE_IF_ACTIVE = "me.aliahad.timemanager.focus.RESTORE"
        const val ACTION_COMPLETE_ALARM = "me.aliahad.timemanager.focus.COMPLETE_ALARM"

        private const val EXTRA_CATEGORY_ID = "category_id"
        private const val EXTRA_CATEGORY_NAME = "category_name"
        private const val EXTRA_CATEGORY_ICON = "category_icon"
        private const val EXTRA_CATEGORY_COLOR = "category_color"
        private const val EXTRA_LINKED_TASK_ID = "linked_task_id"
        private const val EXTRA_TARGET_SECONDS = "target_seconds"
        private const val EXTRA_IS_POMODORO = "is_pomodoro"
        private const val EXTRA_BREAK_SECONDS = "break_seconds"

        private val _timerState = MutableStateFlow(FocusTimerUiState())
        val timerState: StateFlow<FocusTimerUiState> = _timerState.asStateFlow()

        fun consumeLastSavedResult() {
            _timerState.value = _timerState.value.copy(lastSavedResult = null)
        }

        fun startSession(
            context: Context,
            categoryId: Long,
            categoryName: String,
            categoryIcon: String,
            categoryColor: androidx.compose.ui.graphics.Color,
            targetDurationSeconds: Int,
            isPomodoro: Boolean,
            breakDurationSeconds: Int = 300,
            linkedTaskId: Long = 0L
        ) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CATEGORY_ID, categoryId)
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_CATEGORY_ICON, categoryIcon)
                putExtra(EXTRA_CATEGORY_COLOR, categoryColor.toStorageLong())
                putExtra(EXTRA_TARGET_SECONDS, targetDurationSeconds)
                putExtra(EXTRA_IS_POMODORO, isPomodoro)
                putExtra(EXTRA_BREAK_SECONDS, breakDurationSeconds)
                putExtra(EXTRA_LINKED_TASK_ID, linkedTaskId)
            }
            context.startForegroundService(intent)
        }

        suspend fun restoreIfRunning(
            context: Context,
            prefsRepo: me.aliahad.timemanager.core.datastore.UserPreferencesRepository
        ) {
            if (timerState.value.isRunning) return
            val snapshot = prefsRepo.getActiveTimerSnapshot()
            if (snapshot.isRunning) {
                val intent = Intent(context, FocusTimerService::class.java).apply {
                    action = ACTION_RESTORE_IF_ACTIVE
                }
                context.startForegroundService(intent)
            }
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                this.action = action
            }
            if (action == ACTION_PAUSE || action == ACTION_RESUME ||
                action == ACTION_STOP_AND_SAVE || action == ACTION_ADD_FIVE_MIN ||
                action == ACTION_SKIP_BREAK || action == ACTION_DISCARD
            ) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        }
    }
}
