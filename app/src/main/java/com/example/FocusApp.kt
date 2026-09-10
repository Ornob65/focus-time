package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.example.data.local.AppDatabase

class FocusApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "focus_screentime.db"
        ).fallbackToDestructiveMigration().build()

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_ALERTS,
                "Focus & Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to stay focused and alerts for study hours"
                enableVibration(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Focus Session Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a study hour or focus session is actively running"
            }

            val limitsChannel = NotificationChannel(
                CHANNEL_LIMITS,
                "App Limit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings when daily app screen time limits are reached"
            }

            notificationManager.createNotificationChannel(focusChannel)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(limitsChannel)
        }
    }

    companion object {
        const val CHANNEL_FOCUS_ALERTS = "focus_alerts_channel"
        const val CHANNEL_SERVICE = "focus_service_channel"
        const val CHANNEL_LIMITS = "app_limits_channel"

        lateinit var instance: FocusApp
            private set
    }
}
