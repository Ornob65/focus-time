package com.example.data.model

data class StudySchedule(
    val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Set<Int>, // 1 = Sun, 2 = Mon ... 7 = Sat (Calendar.DAY_OF_WEEK)
    val isEnabled: Boolean = true,
    val blockNotifications: Boolean = true,
    val lockSocialApps: Boolean = true
) {
    val timeRangeFormatted: String
        get() {
            fun formatTime(hour: Int, min: Int): String {
                val ampm = if (hour >= 12) "PM" else "AM"
                val h = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                return String.format("%d:%02d %s", h, min, ampm)
            }
            return "${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}"
        }

    val daysFormatted: String
        get() {
            if (daysOfWeek.size == 7) return "Every day"
            if (daysOfWeek == setOf(2, 3, 4, 5, 6)) return "Weekdays (Mon - Fri)"
            if (daysOfWeek == setOf(1, 7)) return "Weekends"
            val dayNames = mapOf(
                1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
                5 to "Thu", 6 to "Fri", 7 to "Sat"
            )
            return daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
        }
}
