package me.aliahad.timemanager

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import me.aliahad.timemanager.ui.theme.TimeManagerTheme

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Check exact alarm permission for Android 12+ (but don't launch settings immediately)
        // This permission is needed for habit reminders to work precisely
        // The app will show an in-app prompt when user tries to set a reminder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Log for debugging - actual permission request happens in NotificationScheduler
                android.util.Log.d("MainActivity", "Exact alarm permission not granted - will prompt when needed")
            }
        }
        
        // Create reminder notification channel
        createReminderNotificationChannel()
        
        // Schedule Screen Time tracking work
        ScreenTimeScheduler.ensurePeriodicWork(this)
        ScreenTimeScheduler.triggerImmediateSync(this)
        
        val tutorialPrefs = getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val hasSeenTutorial = tutorialPrefs.getBoolean("tutorial_seen", false)
        enableEdgeToEdge()
        setContent {
            TimeManagerTheme {
                var showTutorial by remember { mutableStateOf(!hasSeenTutorial) }
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box {
                        TimeMachineNavigation()
                        TutorialModal(visible = showTutorial) {
                            tutorialPrefs.edit().putBoolean("tutorial_seen", true).apply()
                            showTutorial = false
                        }
                    }
                }
            }
        }
    }
    
    private fun createReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use system default notification sound
            val reminderSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
            
            val channel = android.app.NotificationChannel(
                "HabitReminderChannel",
                "Habit Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for your habits - IMPORTANT: Enable sound and vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                setSound(reminderSound, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle the intent when activity is already running (SINGLE_TOP)
        setIntent(intent)
    }
}
