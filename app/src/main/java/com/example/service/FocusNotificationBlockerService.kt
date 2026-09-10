package com.example.service

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FocusApp
import com.example.data.local.AppDatabase
import com.example.data.repository.ScreenTimeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusNotificationBlockerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: ScreenTimeRepository

    override fun onCreate() {
        super.onCreate()
        repository = ScreenTimeRepository(applicationContext, AppDatabase.getInstance(applicationContext))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Do not block notifications from our own app or essential system calls
        if (packageName == packageName ||
            packageName == "com.android.server.telecom" ||
            packageName == "com.google.android.dialer" ||
            packageName == "com.android.phone"
        ) {
            return
        }

        val isFocusActive = FocusStateHolder.isFocusActive.value
        val blockEnabled = FocusStateHolder.blockNotifications.value

        if (isFocusActive && blockEnabled) {
            // Check if this package is distracting or social
            val isSocialOrDistracting = FocusStateHolder.isPackageLocked(packageName) ||
                    DISTRACTING_PACKAGES.contains(packageName) ||
                    sbn.notification.category == "social" ||
                    sbn.notification.category == "msg"

            if (isSocialOrDistracting) {
                // Cancel / suppress the notification to minimize interruptions!
                try {
                    cancelNotification(sbn.key)
                } catch (e: Exception) {
                    // Ignored if cancel permissions not fully ready
                }

                // Extract title and text to log for user review
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: "New Message"
                val text = extras.getCharSequence("android.text")?.toString() ?: "Distracting Notification"

                val pm = packageManager
                val appName = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName.substringAfterLast(".")
                }

                serviceScope.launch {
                    repository.recordBlockedNotification(
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        sessionTitle = FocusStateHolder.activeSessionTitle.value
                    )
                }
            }
        }
    }

    companion object {
        val DISTRACTING_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.twitter.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.google.android.youtube",
            "com.discord",
            "org.telegram.messenger",
            "com.whatsapp",
            "com.pinterest",
            "com.netflix.mediaclient",
            "tv.twitch.android.app"
        )
    }
}
