package me.aliahad.timemanager

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay

class TimerService : Service() {
    
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    
    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging
    
    private var timerJob: Job? = null
    private var notificationManager: NotificationManager? = null
    
    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val ALARM_CHANNEL_ID = "TimerAlarmChannel"
        const val NOTIFICATION_ID = 1
        const val ALARM_NOTIFICATION_ID = 2
        const val TASK_REMOVED_NOTIFICATION_ID = 3
        const val FALLBACK_ALARM_REQUEST_CODE = 9999
        
        const val ACTION_START_TIMER = "ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "ACTION_STOP_TIMER"
        const val ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM"
        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        createAlarmNotificationChannel()
        
        // Try to restore timer if service was killed and restarted
        restoreTimerIfNeeded()
    }
    
    private fun restoreTimerIfNeeded() {
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val endTimeMillis = prefs.getLong("timer_end_time", 0)
        
        if (endTimeMillis > 0) {
            val now = System.currentTimeMillis()
            val remainingSeconds = ((endTimeMillis - now) / 1000).toInt()
            
            android.util.Log.d("TimerService", "🔄 Restoring timer: $remainingSeconds seconds remaining")
            
            if (remainingSeconds > 0) {
                // Timer still running - restore it
                _remainingSeconds.value = remainingSeconds
                _isRunning.value = true
                
                // Restart foreground service and countdown
                startForeground(NOTIFICATION_ID, createNotification(_remainingSeconds.value))
                
                timerJob = serviceScope.launch {
                    while (_remainingSeconds.value > 0 && _isRunning.value) {
                        delay(1000)
                        _remainingSeconds.value--
                        updateNotification(_remainingSeconds.value)
                        
                        if (_remainingSeconds.value == 0) {
                            onTimerComplete()
                        }
                    }
                }
            } else {
                // Timer already finished - trigger alarm immediately
                android.util.Log.d("TimerService", "⏰ Timer already finished, triggering alarm now")
                onTimerComplete()
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 0)
                if (durationSeconds > 0) {
                    startTimer(durationSeconds)
                }
            }
            ACTION_STOP_TIMER -> {
                stopTimer()
            }
            ACTION_DISMISS_ALARM -> {
                dismissAlarm()
            }
        }
        return START_STICKY  // Changed to START_STICKY so service restarts if killed
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        android.util.Log.d("TimerService", "🔴 onTaskRemoved called! Timer running: ${_isRunning.value}")
        
        // Only show notification if timer is actually running
        if (_isRunning.value) {
            android.util.Log.d("TimerService", "📱 Showing task removed notification...")
            showTaskRemovedNotification()
        }
        
        // Keep service running - don't restart, just keep current foreground service
        // No need to restart intent, service is already running as foreground
    }
    
    private fun showTaskRemovedNotification() {
        try {
            android.util.Log.d("TimerService", "🔔 Creating task removed notification...")
            
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                999, // Different request code
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use ALARM channel for higher priority (user will definitely see it)
            val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                .setContentTitle("⏰ Timer still running!")
                .setContentText("Don't worry! Your timer is still counting. Tap to reopen.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(longArrayOf(0, 200, 100, 200)) // Short vibration pattern
                .build()
            
            notificationManager?.notify(TASK_REMOVED_NOTIFICATION_ID, notification)
            android.util.Log.d("TimerService", "✅ Task removed notification shown!")
            
            // Auto-dismiss after 8 seconds (give user time to see it)
            serviceScope.launch {
                delay(8000)
                notificationManager?.cancel(TASK_REMOVED_NOTIFICATION_ID)
                android.util.Log.d("TimerService", "🗑️ Task removed notification auto-dismissed")
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "❌ Error showing task removed notification: ${e.message}")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Running",
                NotificationManager.IMPORTANCE_DEFAULT // Changed to DEFAULT for better visibility
            ).apply {
                description = "Persistent notification while timer is running. Tap to reopen app."
                setShowBadge(true) // Show badge
                setSound(null, null) // No sound for countdown notification
                enableVibration(false) // No vibration for countdown
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use system default alarm sound
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Timer Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when timer completes - IMPORTANT: Enable sound and vibration"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Bypass Do Not Disturb
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    fun startTimer(durationSeconds: Int) {
        if (_isRunning.value) return
        
        _remainingSeconds.value = durationSeconds
        _isRunning.value = true
        
        // Persist timer data for recovery
        val endTimeMillis = System.currentTimeMillis() + (durationSeconds * 1000L)
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("timer_end_time", endTimeMillis)
            .putInt("timer_duration", durationSeconds)
            .putLong("timer_start_time", System.currentTimeMillis())
            .apply()
        
        android.util.Log.d("TimerService", "💾 Timer persisted: end time = $endTimeMillis")
        
        // Schedule fallback alarm using AlarmManager
        scheduleFallbackAlarm(endTimeMillis)
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(_remainingSeconds.value))
        
        // Start countdown
        timerJob = serviceScope.launch {
            while (_remainingSeconds.value > 0 && _isRunning.value) {
                delay(1000)
                _remainingSeconds.value--
                
                // Update notification every second
                updateNotification(_remainingSeconds.value)
                
                if (_remainingSeconds.value == 0) {
                    onTimerComplete()
                }
            }
        }
    }
    
    private fun scheduleFallbackAlarm(endTimeMillis: Long) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            val intent = Intent(this, TimerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                FALLBACK_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = PendingIntent.getActivity(
                this,
                FALLBACK_ALARM_REQUEST_CODE + 1,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.scheduleReliableAlarm(
                endTimeMillis,
                showIntent,
                pendingIntent,
                tag = "TimerService"
            )

            android.util.Log.d("TimerService", "⏰ Fallback alarm scheduled for $endTimeMillis")
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "❌ Error scheduling fallback alarm: ${e.message}")
        }
    }
    
    private fun cancelFallbackAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(this, TimerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                FALLBACK_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            android.util.Log.d("TimerService", "🗑️ Fallback alarm cancelled")
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "❌ Error cancelling fallback alarm: ${e.message}")
        }
    }
    
    fun stopTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        
        // Clear persisted timer data
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // Cancel fallback alarm
        cancelFallbackAlarm()
        
        // Cancel task removed notification if showing
        notificationManager?.cancel(TASK_REMOVED_NOTIFICATION_ID)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun onTimerComplete() {
        _isRunning.value = false
        _isAlarmRinging.value = true  // Set alarm ringing state
        timerJob?.cancel()
        
        // Clear persisted timer data
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // Cancel fallback alarm
        cancelFallbackAlarm()
        
        // Stop the timer notification
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        // Start the dedicated alarm ringing service
        AlarmRingingService.startAlarm(this)
        
        // Send broadcast to notify MainActivity (if it's open)
        val intent = Intent("me.aliahad.timemanager.TIMER_COMPLETE")
        sendBroadcast(intent)
        
        // Stop this service - alarm is now handled by AlarmRingingService
        stopSelf()
    }
    
    private fun dismissAlarm() {
        _isAlarmRinging.value = false  // Clear alarm ringing state
        
        // Stop the alarm ringing service
        AlarmRingingService.stopAlarm(this)
        
        // Clear persisted timer data (also done in AlarmRingingService, but being safe)
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // Cancel fallback alarm
        cancelFallbackAlarm()
        
        // Cancel task removed notification if showing
        notificationManager?.cancel(TASK_REMOVED_NOTIFICATION_ID)
        
        stopSelf()
    }
    
    private fun createNotification(remainingSeconds: Int): Notification {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60
        
        val timeText = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        
        // Intent to open app when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Stop action
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Timer: $timeText")
            .setContentText("Tap to open app • Timer continues in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your timer is running in the background.\n\n💡 Tap this notification to return to the app anytime!\n\n⏱️ Time remaining: $timeText"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop Timer",
                stopPendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun updateNotification(remainingSeconds: Int) {
        val notification = createNotification(remainingSeconds)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
}
