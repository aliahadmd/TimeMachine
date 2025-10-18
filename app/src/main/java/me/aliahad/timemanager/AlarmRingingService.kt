package me.aliahad.timemanager

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Dedicated foreground service for alarm playback.
 * Ensures alarm survives process death and can be reliably dismissed.
 */
class AlarmRingingService : Service() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val CHANNEL_ID = "TimerAlarmChannel"
        const val NOTIFICATION_ID = 999
        const val ACTION_DISMISS = "ACTION_DISMISS_ALARM_SERVICE"
        
        private var isRinging = false
        
        fun startAlarm(context: Context) {
            if (isRinging) {
                Log.d("AlarmRingingService", "⚠️ Alarm already ringing, ignoring duplicate start")
                return
            }
            
            val intent = Intent(context, AlarmRingingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_DISMISS
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("AlarmRingingService", "🔔 Alarm service created")
        
        // Acquire wake lock to ensure alarm plays even if screen is off
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TimeMachine::AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            Log.d("AlarmRingingService", "🔕 Dismiss action received")
            stopAlarmAndService()
            return START_NOT_STICKY
        }
        
        // Start foreground immediately
        startForeground(NOTIFICATION_ID, createAlarmNotification())
        
        // Start alarm playback
        startAlarmPlayback()
        isRinging = true
        
        // Auto-dismiss after 10 minutes
        android.os.Handler(mainLooper).postDelayed({
            Log.d("AlarmRingingService", "⏰ Auto-dismissing alarm after 10 minutes")
            stopAlarmAndService()
        }, 10 * 60 * 1000L)
        
        return START_NOT_STICKY
    }
    
    private fun startAlarmPlayback() {
        try {
            // Stop any existing playback
            mediaPlayer?.release()
            
            // Create and configure MediaPlayer
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingingService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            
            // Start vibration
            vibrator = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).apply {
                val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(pattern, 0)
                }
            }
            
            Log.d("AlarmRingingService", "🔊 Alarm sound and vibration started")
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "❌ Error starting alarm: ${e.message}", e)
        }
    }
    
    private fun stopAlarmAndService() {
        Log.d("AlarmRingingService", "🛑 Stopping alarm playback")
        
        // Stop media player
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        
        // Stop vibration
        vibrator?.cancel()
        vibrator = null
        
        // Release wake lock
        wakeLock?.release()
        wakeLock = null
        
        // Clear timer prefs
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        isRinging = false
        
        // Stop foreground and service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createAlarmNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_DISMISS
        }
        
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Timer Complete!")
            .setContentText("Your timer has finished - Tap to dismiss")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Dismiss",
                dismissPendingIntent
            )
            .setFullScreenIntent(pendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmRingingService", "🗑️ Alarm service destroyed")
        
        // Ensure cleanup
        mediaPlayer?.release()
        vibrator?.cancel()
        wakeLock?.release()
        isRinging = false
    }
}

