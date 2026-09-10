package com.example.service

import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FocusStateHolder {

    // Common social media packages to lock during focus
    val DEFAULT_SOCIAL_PACKAGES = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",      // TikTok
        "com.facebook.katana",          // Facebook
        "com.twitter.android",          // X / Twitter
        "com.snapchat.android",         // Snapchat
        "com.reddit.frontpage",         // Reddit
        "com.google.android.youtube",   // YouTube
        "com.pinterest",
        "org.telegram.messenger",
        "com.discord"
    )

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _activeSessionTitle = MutableStateFlow("Study Hour")
    val activeSessionTitle: StateFlow<String> = _activeSessionTitle.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(25L * 60L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _totalSeconds = MutableStateFlow(25L * 60L)
    val totalSeconds: StateFlow<Long> = _totalSeconds.asStateFlow()

    private val _blockNotifications = MutableStateFlow(true)
    val blockNotifications: StateFlow<Boolean> = _blockNotifications.asStateFlow()

    private val _lockSocialApps = MutableStateFlow(true)
    val lockSocialApps: StateFlow<Boolean> = _lockSocialApps.asStateFlow()

    private val _lockedPackages = MutableStateFlow(DEFAULT_SOCIAL_PACKAGES)
    val lockedPackages: StateFlow<Set<String>> = _lockedPackages.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _emergencyBreakRemainingSeconds = MutableStateFlow(0L)
    val emergencyBreakRemainingSeconds: StateFlow<Long> = _emergencyBreakRemainingSeconds.asStateFlow()

    fun startSession(
        title: String,
        durationMinutes: Int,
        blockNotifs: Boolean = true,
        lockApps: Boolean = true
    ) {
        _activeSessionTitle.value = title
        val totalSecs = (durationMinutes * 60).toLong()
        _totalSeconds.value = totalSecs
        _remainingSeconds.value = totalSecs
        _blockNotifications.value = blockNotifs
        _lockSocialApps.value = lockApps
        _isPaused.value = false
        _emergencyBreakRemainingSeconds.value = 0L
        _isFocusActive.value = true
    }

    fun pauseSession() {
        if (_isFocusActive.value) {
            _isPaused.value = true
        }
    }

    fun resumeSession() {
        if (_isFocusActive.value) {
            _isPaused.value = false
        }
    }

    fun stopSession() {
        _isFocusActive.value = false
        _isPaused.value = false
        _emergencyBreakRemainingSeconds.value = 0L
    }

    fun tickOneSecond() {
        if (_isFocusActive.value && !_isPaused.value) {
            if (_emergencyBreakRemainingSeconds.value > 0) {
                _emergencyBreakRemainingSeconds.value -= 1
            }
            if (_remainingSeconds.value > 0) {
                _remainingSeconds.value -= 1
            } else {
                // Session naturally completed
                _isFocusActive.value = false
            }
        }
    }

    fun grantEmergencyBreak(seconds: Long = 60L) {
        _emergencyBreakRemainingSeconds.value = seconds
    }

    fun isEmergencyBreakActive(): Boolean {
        return _emergencyBreakRemainingSeconds.value > 0
    }

    fun isPackageLocked(packageName: String): Boolean {
        if (!isFocusActive.value || !lockSocialApps.value) return false
        if (isEmergencyBreakActive()) return false
        return _lockedPackages.value.contains(packageName)
    }

    fun setLockedPackages(packages: Set<String>) {
        _lockedPackages.value = packages
    }

    fun addLockedPackage(packageName: String) {
        _lockedPackages.value = _lockedPackages.value + packageName
    }

    fun removeLockedPackage(packageName: String) {
        _lockedPackages.value = _lockedPackages.value - packageName
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}
