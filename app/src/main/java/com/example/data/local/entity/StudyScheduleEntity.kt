package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_schedules")
data class StudyScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekCsv: String, // e.g. "2,3,4,5,6"
    val isEnabled: Boolean = true,
    val blockNotifications: Boolean = true,
    val lockSocialApps: Boolean = true
)
