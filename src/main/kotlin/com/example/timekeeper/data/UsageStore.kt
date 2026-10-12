package com.example.timekeeper.data

import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The local SQLite record of where your time went, and the desktop twin of the
 * phone app's `UsageDbHelper`.
 *
 * Two tables, both keyed by the executable's full path:
 *  - `daily_usage`  — one all-day total per program.
 *  - `hourly_usage` — the same time split into hour buckets, for the "by hour" chart.
 *
 * The tracker increments these as it samples; the UI only ever reads. A lock
 * keeps the two from tripping over the single shared connection.
 */
class UsageStore(dbPath: String) {

    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
    private val lock = ReentrantLock()

    init {
        conn.createStatement().use { st ->
            st.execute("PRAGMA journal_mode=WAL")
            st.execute("PRAGMA busy_timeout=3000")
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS daily_usage (
                    day INTEGER NOT NULL,
                    exe_path TEXT NOT NULL,
                    millis INTEGER NOT NULL,
                    PRIMARY KEY (day, exe_path)
                )
                """.trimIndent()
            )
            st.execute("CREATE INDEX IF NOT EXISTS idx_daily_day ON daily_usage(day)")
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS hourly_usage (
                    day INTEGER NOT NULL,
                    hour INTEGER NOT NULL,
                    exe_path TEXT NOT NULL,
                    millis INTEGER NOT NULL,
                    PRIMARY KEY (day, hour, exe_path)
                )
                """.trimIndent()
            )
            st.execute("CREATE INDEX IF NOT EXISTS idx_hourly_day ON hourly_usage(day)")
        }
    }

    /** Add [millis] of foreground time to a program's total for [day]. */
    fun addDay(exePath: String, day: Long, millis: Long) {
        if (millis <= 0) return
        lock.withLock {
            conn.prepareStatement(
                """
                INSERT INTO daily_usage(day, exe_path, millis) VALUES(?, ?, ?)
                ON CONFLICT(day, exe_path) DO UPDATE SET millis = millis + excluded.millis
                """.trimIndent()
            ).use { st ->
                st.setLong(1, day)
                st.setString(2, exePath)
                st.setLong(3, millis)
                st.executeUpdate()
            }
        }
    }

    /** Add [millis] of foreground time to a program's bucket for [hour] on [day]. */
    fun addHour(exePath: String, day: Long, hour: Int, millis: Long) {
        if (millis <= 0) return
        lock.withLock {
            conn.prepareStatement(
                """
                INSERT INTO hourly_usage(day, hour, exe_path, millis) VALUES(?, ?, ?, ?)
                ON CONFLICT(day, hour, exe_path) DO UPDATE SET millis = millis + excluded.millis
                """.trimIndent()
            ).use { st ->
                st.setLong(1, day)
                st.setInt(2, hour)
                st.setString(3, exePath)
                st.setLong(4, millis)
                st.executeUpdate()
            }
        }
    }

    /** Sum of millis per program for days in [startDay, endDay] inclusive. */
    fun totalsBetween(startDay: Long, endDay: Long): Map<String, Long> {
        val result = LinkedHashMap<String, Long>()
        lock.withLock {
            conn.prepareStatement(
                """
                SELECT exe_path, SUM(millis) AS total
                FROM daily_usage
                WHERE day BETWEEN ? AND ?
                GROUP BY exe_path
                ORDER BY total DESC
                """.trimIndent()
            ).use { st ->
                st.setLong(1, startDay)
                st.setLong(2, endDay)
                st.executeQuery().use { c ->
                    while (c.next()) result[c.getString(1)] = c.getLong(2)
                }
            }
        }
        return result
    }

    /** One program's millis for each day in [startDay, endDay], as day -> millis. */
    fun dailyMillisFor(exePath: String, startDay: Long, endDay: Long): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        lock.withLock {
            conn.prepareStatement(
                "SELECT day, millis FROM daily_usage WHERE exe_path = ? AND day BETWEEN ? AND ?"
            ).use { st ->
                st.setString(1, exePath)
                st.setLong(2, startDay)
                st.setLong(3, endDay)
                st.executeQuery().use { c ->
                    while (c.next()) result[c.getLong(1)] = c.getLong(2)
                }
            }
        }
        return result
    }

    /** One program's millis for every day we have on record, as day -> millis. */
    fun allDailyMillisFor(exePath: String): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        lock.withLock {
            conn.prepareStatement(
                "SELECT day, millis FROM daily_usage WHERE exe_path = ?"
            ).use { st ->
                st.setString(1, exePath)
                st.executeQuery().use { c ->
                    while (c.next()) result[c.getLong(1)] = c.getLong(2)
                }
            }
        }
        return result
    }

    /** One program's millis per hour for a single [day], as hour -> millis. */
    fun hourlyFor(exePath: String, day: Long): Map<Int, Long> {
        val result = HashMap<Int, Long>()
        lock.withLock {
            conn.prepareStatement(
                "SELECT hour, millis FROM hourly_usage WHERE exe_path = ? AND day = ?"
            ).use { st ->
                st.setString(1, exePath)
                st.setLong(2, day)
                st.executeQuery().use { c ->
                    while (c.next()) result[c.getInt(1)] = c.getLong(2)
                }
            }
        }
        return result
    }

    fun close() = lock.withLock { conn.close() }
}
