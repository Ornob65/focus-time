package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.FocusLockService
import com.example.service.FocusStateHolder
import com.example.service.StudyHourNotificationHelper

class StudyHourAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scheduleTitle = intent.getStringExtra(EXTRA_SCHEDULE_TITLE) ?: "Study Hour"
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 60)
        val blockNotifs = intent.getBooleanExtra(EXTRA_BLOCK_NOTIFS, true)
        val lockSocial = intent.getBooleanExtra(EXTRA_LOCK_SOCIAL, true)

        when (action) {
            ACTION_START_STUDY_HOUR -> {
                FocusStateHolder.startSession(
                    title = scheduleTitle,
                    durationMinutes = durationMinutes,
                    blockNotifs = blockNotifs,
                    lockApps = lockSocial
                )
                FocusLockService.startService(context)
                StudyHourNotificationHelper.showStudyHourStartedNotification(context, scheduleTitle)
            }
            ACTION_FOCUS_NUDGE -> {
                val message = intent.getStringExtra(EXTRA_NUDGE_MESSAGE)
                    ?: "You're doing great! Keep your focus on your study goals."
                StudyHourNotificationHelper.showFocusNudgeNotification(context, message)
            }
        }
    }

    companion object {
        const val ACTION_START_STUDY_HOUR = "com.example.ACTION_START_STUDY_HOUR"
        const val ACTION_FOCUS_NUDGE = "com.example.ACTION_FOCUS_NUDGE"

        const val EXTRA_SCHEDULE_TITLE = "extra_schedule_title"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_BLOCK_NOTIFS = "extra_block_notifs"
        const val EXTRA_LOCK_SOCIAL = "extra_lock_social"
        const val EXTRA_NUDGE_MESSAGE = "extra_nudge_message"
    }
}
