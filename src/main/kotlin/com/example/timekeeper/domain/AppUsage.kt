package com.example.timekeeper.domain

/** One row on the main screen: an app and how long it was in the foreground. */
data class AppUsage(
    val packageName: String,
    val label: String,
    val totalMillis: Long,
    val category: AppCategory = AppCategory.OTHER,
)

/** Formats a duration as "2h 05m", "45m", or "30s". */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes > 0 -> "%dm".format(minutes)
        else -> "%ds".format(seconds)
    }
}
