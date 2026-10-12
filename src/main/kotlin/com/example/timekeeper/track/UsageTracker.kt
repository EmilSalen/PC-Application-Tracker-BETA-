package com.example.timekeeper.track

import com.example.timekeeper.data.UsageStore
import com.example.timekeeper.data.normalizePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * The always-on heart of the PC app.
 *
 * Android hands us a finished per-day usage table; Windows doesn't, so we build
 * our own: every [POLL_MILLIS] we note which program owns the foreground window
 * and credit that program with the time since the previous sample. While the
 * machine is idle (no input for [IDLE_THRESHOLD_MILLIS]) nothing is credited, so
 * a laptop left open on a page doesn't pile up fake hours.
 *
 * Each credit is written with two totals — an all-day one and an hour bucket —
 * which between them feed every range on the main screen and the "by hour" chart.
 */
class UsageTracker(private val store: UsageStore) {

    private var job: Job? = null
    private var lastSampleAt = 0L

    /** Start sampling. Safe to call more than once; a second call is a no-op. */
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        lastSampleAt = System.currentTimeMillis()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(POLL_MILLIS)
                try {
                    sample()
                } catch (t: Throwable) {
                    // A tracker that dies on one bad sample is useless; keep going.
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun sample() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastSampleAt
        lastSampleAt = now
        if (elapsed <= 0) return

        val id = foregroundId() ?: return

        // A sample window can straddle midnight; split it so the time lands on
        // the right day when it does.
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(now - elapsed).atZone(zone)
        val end = Instant.ofEpochMilli(now).atZone(zone)

        if (start.toLocalDate() == end.toLocalDate()) {
            store.addDay(id, start.toLocalDate().toEpochDay(), elapsed)
            store.addHour(id, start.toLocalDate().toEpochDay(), start.hour, elapsed)
        } else {
            val midnight = end.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
            val beforeMidnight = (midnight - start.toInstant().toEpochMilli()).coerceIn(0L, elapsed)
            val afterMidnight = elapsed - beforeMidnight
            store.addDay(id, start.toLocalDate().toEpochDay(), beforeMidnight)
            store.addHour(id, start.toLocalDate().toEpochDay(), start.hour, beforeMidnight)
            store.addDay(id, end.toLocalDate().toEpochDay(), afterMidnight)
            store.addHour(id, end.toLocalDate().toEpochDay(), end.hour, afterMidnight)
        }
    }

    /**
     * The executable to credit for the time just elapsed, or null to credit
     * nobody. Null while idle or while our own window is in front — looking at
     * the tracker is not something to track.
     */
    private fun foregroundId(): String? {
        if (Win32.idleMillis() >= IDLE_THRESHOLD_MILLIS) return null
        val raw = Win32.foregroundProcessPath() ?: return null
        val path = normalizePath(raw.trim())
        if (path.isEmpty()) return null
        val self = ProcessHandle.current().info().command().orElse(null)
        if (self != null && path.equals(normalizePath(self.trim()), ignoreCase = true)) return null
        return path
    }

    private companion object {
        /** How often we sample the foreground window. */
        const val POLL_MILLIS = 5_000L

        /** No keyboard/mouse input for this long → stop counting. */
        const val IDLE_THRESHOLD_MILLIS = 120_000L
    }
}
