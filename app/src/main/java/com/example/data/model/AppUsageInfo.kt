package com.example.data.model

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val durationMinutes: Long,
    val category: AppCategory,
    val limitMinutes: Long? = null,
    val isLimitEnabled: Boolean = false,
    val isLockedDuringFocus: Boolean = true
) {
    val durationFormatted: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }

    val progressFraction: Float
        get() {
            val limit = limitMinutes ?: return 0f
            if (limit <= 0) return 0f
            return (durationMinutes.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }

    val isLimitExceeded: Boolean
        get() = isLimitEnabled && limitMinutes != null && durationMinutes >= limitMinutes
}
