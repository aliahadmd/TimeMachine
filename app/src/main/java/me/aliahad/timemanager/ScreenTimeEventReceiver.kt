package me.aliahad.timemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

object ScreenTimeEventReceiver : BroadcastReceiver() {
    
    private const val TAG = "ScreenTimeEventReceiver"
    private var registered = false
    private var appContext: Context? = null
    
    private val intentFilter: IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_USER_PRESENT)
    }
    
    fun register(context: Context) {
        if (registered) return
        val ctx = context.applicationContext
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(this, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                ctx.registerReceiver(this, intentFilter)
            }
            registered = true
            appContext = ctx
            Log.d(TAG, "Registered screen event receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }
    }
    
    fun unregister() {
        if (!registered) return
        try {
            appContext?.unregisterReceiver(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        } finally {
            registered = false
            appContext = null
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context?.applicationContext ?: return
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "Screen event: ${intent.action}")
                ScreenTimeScheduler.triggerImmediateSync(ctx)
            }
        }
    }
}
