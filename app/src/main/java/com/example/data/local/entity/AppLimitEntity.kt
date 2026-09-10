package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val limitMinutes: Long,
    val isEnabled: Boolean = true,
    val categoryName: String = "OTHER"
)
