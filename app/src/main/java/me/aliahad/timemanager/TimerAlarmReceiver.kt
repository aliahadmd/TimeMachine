package me.aliahad.timemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Fallback receiver that triggers timer alarm even if the service is dead.
 * This ensures the alarm fires even if Android kills the process.
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerAlarmReceiver", "⏰ Fallback timer alarm triggered!")
        
        // Start the dedicated alarm ringing service
        AlarmRingingService.startAlarm(context)
        
        Log.d("TimerAlarmReceiver", "✅ AlarmRingingService started")
    }
}

