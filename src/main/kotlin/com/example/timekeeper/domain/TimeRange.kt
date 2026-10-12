package com.example.timekeeper.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The filter options shown on the main screen.
 * Each range is turned into a start "epoch day" so it can be summed from the
 * local daily-usage table.
 */
enum class TimeRange(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All");

    /** Epoch day (days since 1970-01-01) at which this range starts. ALL starts at 0. */
    fun startEpochDay(today: LocalDate = LocalDate.now()): Long = when (this) {
        TODAY -> today.toEpochDay()
        WEEK -> today.minusDays(6).toEpochDay()
        MONTH -> today.minusDays(29).toEpochDay()
        YEAR -> today.minusDays(364).toEpochDay()
        ALL -> 0L
    }

    /** Number of days this range covers; 0 for ALL (open-ended). */
    val days: Int get() = when (this) {
        TODAY -> 1
        WEEK -> 7
        MONTH -> 30
        YEAR -> 365
        ALL -> 0
    }

    /** What a single bar on the detail trend chart represents for this range. */
    val trendUnit: TrendUnit get() = when (this) {
        TODAY -> TrendUnit.HOUR
        WEEK -> TrendUnit.DAY
        MONTH -> TrendUnit.WEEK
        YEAR -> TrendUnit.MONTH
        ALL -> TrendUnit.YEAR
    }

    /**
     * The line shown under the total: the actual dates this range covers, so the
     * number has a concrete span attached to it. "Today" also carries the clock
     * time so it reads as a live snapshot.
     */
    fun rangeLabel(
        now: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = when (this) {
        TODAY -> "${LocalTime.now(zone).format(TIME)} · ${now.format(FULL_DATE)}"
        WEEK -> "${now.minusDays(6).format(SHORT_DATE)} – ${now.format(FULL_DATE)}"
        MONTH -> now.format(MONTH_YEAR)
        YEAR -> now.format(YEAR_ONLY)
        ALL -> "All time"
    }

    companion object {
        private val TIME = DateTimeFormatter.ofPattern("h:mm a")
        private val SHORT_DATE = DateTimeFormatter.ofPattern("MMM d")
        private val FULL_DATE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy")
        private val YEAR_ONLY = DateTimeFormatter.ofPattern("yyyy")
    }
}

/** The size of one bar on the detail trend chart. */
enum class TrendUnit { HOUR, DAY, WEEK, MONTH, YEAR }

fun todayEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).toEpochDay()
