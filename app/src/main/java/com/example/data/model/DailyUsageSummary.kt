package com.example.data.model

data class DailyUsageSummary(
    val dayName: String,         // e.g. "Mon", "Tue"
    val dateLabel: String,       // e.g. "Sep 07"
    val dayIndex: Int,           // 0..6
    val totalMinutes: Long,
    val socialMinutes: Long,
    val productivityMinutes: Long,
    val entertainmentMinutes: Long,
    val otherMinutes: Long
) {
    val totalHoursFormatted: String
        get() {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes}m"
        }
}
