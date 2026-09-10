package com.example.data.model

data class WeeklyUsageSummary(
    val totalMinutes: Long,
    val dailyAverageMinutes: Long,
    val dailyBreakdowns: List<DailyUsageSummary>,
    val topApps: List<AppUsageInfo>,
    val changePercentVsLastWeek: Int = -12, // e.g. -12% screen time reduction
    val mostUsedCategory: AppCategory = AppCategory.SOCIAL
) {
    val totalHoursFormatted: String
        get() {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes}m"
        }

    val dailyAverageFormatted: String
        get() {
            val hours = dailyAverageMinutes / 60
            val minutes = dailyAverageMinutes % 60
            return "${hours}h ${minutes}m"
        }
}
