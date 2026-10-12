package com.example.timekeeper.data

import java.util.prefs.Preferences

/** How the app picks light vs dark: follow Windows, or pin one. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** A daily screen-time target in milliseconds. 0 means "no goal set". */
data class DailyGoal(val millis: Long) {
    val isSet: Boolean get() = millis > 0
}

/**
 * User settings — theme choice, daily goal, and the "already warned today" flag.
 * Kept in the Windows registry (via [Preferences]) rather than the SQLite store,
 * so settings and data stay separate.
 */
class SettingsStore {

    private val prefs: Preferences = Preferences.userRoot().node("com/example/timekeeper")

    fun goal(): DailyGoal = DailyGoal(prefs.getLong(KEY_GOAL_MILLIS, 0L))

    fun setGoal(millis: Long) = prefs.putLong(KEY_GOAL_MILLIS, millis.coerceAtLeast(0L))

    fun themeMode(): ThemeMode {
        val name = prefs.get(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: ThemeMode) = prefs.put(KEY_THEME_MODE, mode.name)

    /** The last epoch day we fired a "goal reached" notification for, or null. */
    fun lastNotifiedDay(): Long? =
        if (prefs.get(KEY_LAST_NOTIFIED, null) != null) prefs.getLong(KEY_LAST_NOTIFIED, 0L) else null

    fun markNotified(day: Long) = prefs.putLong(KEY_LAST_NOTIFIED, day)

    /** Generic access for one-off flags (e.g. "have we offered to auto-start yet"). */
    fun string(key: String, default: String?): String? = prefs.get(key, default)

    fun putString(key: String, value: String) = prefs.put(key, value)

    private companion object {
        const val KEY_GOAL_MILLIS = "daily_goal_millis"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LAST_NOTIFIED = "last_notified_day"
    }
}
