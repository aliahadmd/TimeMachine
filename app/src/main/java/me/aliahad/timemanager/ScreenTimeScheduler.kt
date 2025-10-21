package me.aliahad.timemanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ScreenTimeScheduler {
    
    private const val UNIQUE_WORK_NAME = "screen_time_tracking"
    
    fun ensurePeriodicWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        
        val request = PeriodicWorkRequestBuilder<ScreenTimeWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
    
    fun triggerImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<ScreenTimeWorker>()
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${UNIQUE_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
