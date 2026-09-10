package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusApp
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.BlockedNotificationEntity
import com.example.data.model.AppCategory
import com.example.data.model.AppUsageInfo
import com.example.data.model.DailyUsageSummary
import com.example.data.model.StudySchedule
import com.example.data.model.ThemeMode
import com.example.data.model.WeeklyUsageSummary
import com.example.data.repository.ScreenTimeRepository
import com.example.service.FocusLockService
import com.example.service.FocusStateHolder
import com.example.service.StudyHourNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenTimeRepository(
        application.applicationContext,
        AppDatabase.getInstance(application.applicationContext)
    )

    val allLimits: StateFlow<List<AppLimitEntity>> = repository.allLimits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<StudySchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedNotifications: StateFlow<List<BlockedNotificationEntity>> =
        repository.allBlockedNotifications
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedCount: StateFlow<Int> = repository.blockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _weeklySummary = MutableStateFlow<WeeklyUsageSummary?>(null)
    val weeklySummary: StateFlow<WeeklyUsageSummary?> = _weeklySummary.asStateFlow()

    private val _selectedDayIndex = MutableStateFlow<Int?>(null)
    val selectedDayIndex: StateFlow<Int?> = _selectedDayIndex.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    // Focus state delegates
    val isFocusActive: StateFlow<Boolean> = FocusStateHolder.isFocusActive
    val isPaused: StateFlow<Boolean> = FocusStateHolder.isPaused
    val activeSessionTitle: StateFlow<String> = FocusStateHolder.activeSessionTitle
    val remainingSeconds: StateFlow<Long> = FocusStateHolder.remainingSeconds
    val totalSeconds: StateFlow<Long> = FocusStateHolder.totalSeconds
    val blockNotifications: StateFlow<Boolean> = FocusStateHolder.blockNotifications
    val lockSocialApps: StateFlow<Boolean> = FocusStateHolder.lockSocialApps
    val lockedPackages: StateFlow<Set<String>> = FocusStateHolder.lockedPackages
    val themeMode: StateFlow<ThemeMode> = FocusStateHolder.themeMode

    // Extra simulated usage map for interactive testing
    private val _simulatedUsageDelta = MutableStateFlow<Map<String, Long>>(emptyMap())

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
            checkPermissions()
            refreshWeeklySummary()
        }
    }

    fun checkPermissions() {
        _hasUsagePermission.value = repository.hasUsageStatsPermission()
    }

    fun refreshWeeklySummary() {
        viewModelScope.launch {
            checkPermissions()
            val summary = repository.getWeeklyUsageSummary()
            _weeklySummary.value = summary
        }
    }

    fun selectDayIndex(index: Int?) {
        _selectedDayIndex.value = if (_selectedDayIndex.value == index) null else index
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    // App Limits Actions
    fun saveAppLimit(packageName: String, appName: String, limitMinutes: Long, isEnabled: Boolean, category: String = "SOCIAL") {
        viewModelScope.launch {
            repository.saveAppLimit(packageName, appName, limitMinutes, isEnabled, category)
            refreshWeeklySummary()
        }
    }

    fun toggleAppLimit(limit: AppLimitEntity) {
        viewModelScope.launch {
            repository.toggleAppLimit(limit)
            refreshWeeklySummary()
        }
    }

    fun deleteAppLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteAppLimit(packageName)
            refreshWeeklySummary()
        }
    }

    // Study Schedule Actions
    fun saveSchedule(schedule: StudySchedule) {
        viewModelScope.launch {
            repository.saveSchedule(schedule)
        }
    }

    fun toggleSchedule(schedule: StudySchedule) {
        viewModelScope.launch {
            repository.toggleSchedule(schedule)
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
        }
    }

    // Focus Session Controls
    fun startFocusSession(
        title: String,
        durationMinutes: Int,
        blockNotifs: Boolean = true,
        lockApps: Boolean = true
    ) {
        val context = getApplication<Application>().applicationContext
        FocusStateHolder.startSession(title, durationMinutes, blockNotifs, lockApps)
        FocusLockService.startService(context)
        StudyHourNotificationHelper.showStudyHourStartedNotification(context, title)
    }

    fun pauseFocusSession() {
        FocusStateHolder.pauseSession()
    }

    fun resumeFocusSession() {
        FocusStateHolder.resumeSession()
    }

    fun stopFocusSession() {
        val context = getApplication<Application>().applicationContext
        val title = FocusStateHolder.activeSessionTitle.value
        val totalMins = (FocusStateHolder.totalSeconds.value / 60).toInt()
        val remainingMins = (FocusStateHolder.remainingSeconds.value / 60).toInt()
        val elapsedMins = (totalMins - remainingMins).coerceAtLeast(1)

        viewModelScope.launch {
            repository.recordFocusSession(title, elapsedMins, true)
        }
        FocusLockService.stopService(context)
        FocusStateHolder.stopSession()
    }

    fun toggleLockedPackage(packageName: String) {
        if (FocusStateHolder.lockedPackages.value.contains(packageName)) {
            FocusStateHolder.removeLockedPackage(packageName)
        } else {
            FocusStateHolder.addLockedPackage(packageName)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        FocusStateHolder.setThemeMode(mode)
    }

    fun clearBlockedNotifications() {
        viewModelScope.launch {
            repository.clearBlockedNotifications()
        }
    }

    // Simulation helpers for demonstration in emulator
    fun simulateDistractingNotificationIntercepted() {
        val distractions = listOf(
            Triple("com.instagram.android", "Instagram", "alex_smith sent you a direct message"),
            Triple("com.twitter.android", "X (Twitter)", "Breaking: Trending topic in Technology"),
            Triple("com.zhiliaoapp.musically", "TikTok", "New video from your favorite creator"),
            Triple("com.discord", "Discord", "#general: Join the voice channel tonight!"),
            Triple("com.snapchat.android", "Snapchat", "Emily posted to their story")
        )
        val sample = distractions.random()
        viewModelScope.launch {
            repository.recordBlockedNotification(
                packageName = sample.first,
                appName = sample.second,
                title = sample.second,
                text = sample.third,
                sessionTitle = FocusStateHolder.activeSessionTitle.value
            )
        }
    }

    fun simulateAddUsage(packageName: String, additionalMinutes: Long) {
        viewModelScope.launch {
            val currentSummary = _weeklySummary.value ?: return@launch
            val updatedApps = currentSummary.topApps.map { app ->
                if (app.packageName == packageName) {
                    val newDuration = app.durationMinutes + additionalMinutes
                    val limit = app.limitMinutes
                    if (limit != null && app.isLimitEnabled && newDuration >= limit) {
                        StudyHourNotificationHelper.showAppLimitExceededNotification(
                            getApplication(),
                            app.appName,
                            limit
                        )
                    }
                    app.copy(durationMinutes = newDuration)
                } else {
                    app
                }
            }
            _weeklySummary.value = currentSummary.copy(
                totalMinutes = currentSummary.totalMinutes + additionalMinutes,
                topApps = updatedApps
            )
        }
    }
}
