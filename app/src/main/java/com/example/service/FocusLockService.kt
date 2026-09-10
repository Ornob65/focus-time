package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.FocusApp
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.repository.ScreenTimeRepository
import com.example.ui.lock.LockShieldActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusLockService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var monitorJob: Job? = null
    private lateinit var repository: ScreenTimeRepository
    private var lastShieldLaunchTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = ScreenTimeRepository(applicationContext, AppDatabase.getInstance(applicationContext))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopFocusSession()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        startTimerAndMonitoring()

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildOngoingNotification("Focus Session Active", "Remaining: --:--")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildOngoingNotification(title: String, text: String) =
        NotificationCompat.Builder(this, FocusApp.CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Session",
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, FocusLockService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun startTimerAndMonitoring() {
        timerJob?.cancel()
        monitorJob?.cancel()

        // 1-second countdown ticker
        timerJob = serviceScope.launch {
            while (isActive && FocusStateHolder.isFocusActive.value) {
                delay(1000)
                FocusStateHolder.tickOneSecond()

                val remaining = FocusStateHolder.remainingSeconds.value
                val mins = remaining / 60
                val secs = remaining % 60
                val timeStr = String.format("%02d:%02d", mins, secs)

                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val updatedNotif = buildOngoingNotification(
                    "🎯 ${FocusStateHolder.activeSessionTitle.value} Active",
                    "Time remaining: $timeStr • Social apps locked"
                )
                notificationManager?.notify(NOTIFICATION_ID, updatedNotif)

                if (remaining <= 0) {
                    // Completed!
                    val title = FocusStateHolder.activeSessionTitle.value
                    val totalMins = (FocusStateHolder.totalSeconds.value / 60).toInt()
                    repository.recordFocusSession(title, totalMins, true)
                    StudyHourNotificationHelper.showStudyHourCompletedNotification(
                        applicationContext,
                        title,
                        0
                    )
                    FocusStateHolder.stopSession()
                    stopSelf()
                    break
                }
            }
        }

        // Foreground app monitor loop
        monitorJob = serviceScope.launch {
            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val pm = packageManager

            while (isActive && FocusStateHolder.isFocusActive.value) {
                delay(1500)

                if (!FocusStateHolder.lockSocialApps.value || FocusStateHolder.isEmergencyBreakActive()) {
                    continue
                }

                val foregroundPkg = getForegroundPackage(usageStatsManager)
                if (foregroundPkg != null && FocusStateHolder.isPackageLocked(foregroundPkg)) {
                    val now = System.currentTimeMillis()
                    // Avoid launching shield faster than every 2 seconds
                    if (now - lastShieldLaunchTime > 2000) {
                        lastShieldLaunchTime = now
                        val appName = try {
                            val appInfo = pm.getApplicationInfo(foregroundPkg, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            foregroundPkg.substringAfterLast(".")
                        }

                        val lockIntent = Intent(applicationContext, LockShieldActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(LockShieldActivity.EXTRA_BLOCKED_APP_NAME, appName)
                            putExtra(LockShieldActivity.EXTRA_BLOCKED_PACKAGE, foregroundPkg)
                        }
                        startActivity(lockIntent)
                    }
                }
            }
        }
    }

    private fun getForegroundPackage(usageStatsManager: UsageStatsManager?): String? {
        if (usageStatsManager == null) return null
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000 * 5, time)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPackage = event.packageName
            }
        }
        return lastForegroundPackage
    }

    private fun stopFocusSession() {
        FocusStateHolder.stopSession()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 5001
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
