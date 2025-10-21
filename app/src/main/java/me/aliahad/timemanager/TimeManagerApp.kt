package me.aliahad.timemanager

import android.app.Application

class TimeManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenTimeEventReceiver.register(this)
        ScreenTimeScheduler.ensurePeriodicWork(this)
    }
}
