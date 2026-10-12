package com.example.timekeeper.notify

import com.example.timekeeper.data.SettingsStore
import com.example.timekeeper.data.UsageRepository
import com.example.timekeeper.domain.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The desktop stand-in for the phone app's background worker: every few minutes,
 * check whether today's total has passed the daily goal, and if so say so — once
 * per day, not on every check.
 */
class GoalMonitor(
    private val repo: UsageRepository,
    private val settings: SettingsStore,
    private val onReached: (usedMillis: Long, goalMillis: Long) -> Unit,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                delay(FIRST_DELAY)
                runCatching { check() }
            }
        }
    }

    private fun check() {
        val goal = repo.goal()
        if (!goal.isSet) return

        val today = todayEpochDay()
        val used = repo.todayTotalMillis()
        if (used < goal.millis) return
        if (settings.lastNotifiedDay() == today) return

        settings.markNotified(today)
        onReached(used, goal.millis)
    }

    private companion object {
        const val FIRST_DELAY = 5 * 60_000L
    }
}
