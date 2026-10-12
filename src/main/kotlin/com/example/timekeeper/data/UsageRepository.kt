package com.example.timekeeper.data

import com.example.timekeeper.domain.AppDetail
import com.example.timekeeper.domain.AppUsage
import com.example.timekeeper.domain.CategoryTotal
import com.example.timekeeper.domain.Dashboard
import com.example.timekeeper.domain.TimeRange
import com.example.timekeeper.domain.TrendBucket
import com.example.timekeeper.domain.TrendUnit
import com.example.timekeeper.domain.todayEpochDay
import java.awt.Image
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Everything the UI asks about usage goes through here, so all the filtering and
 * bucketing rules live in one place. The desktop twin of the phone app's
 * `UsageRepository`, reading the same shape of table the tracker writes.
 */
class UsageRepository(
    private val store: UsageStore,
    private val settings: SettingsStore,
) {

    private val apps = AppNameResolver()

    /** Ranked per-app totals for the given range, biggest first. */
    fun dashboardFor(range: TimeRange): Dashboard {
        val today = todayEpochDay()
        val items = ranked(range, today)
        return Dashboard(
            items = items,
            totalMillis = items.sumOf { it.totalMillis },
            categoryTotals = categoryTotals(items),
            todayMillis = filteredTotals(today, today).values.sum(),
        )
    }

    /**
     * Per-app detail for the tap-through screen: a trend chart bucketed to suit
     * the selected range, plus the numbers that go with it.
     */
    fun detailFor(exePath: String, range: TimeRange): AppDetail {
        val today = todayEpochDay()
        val unit = range.trendUnit

        val byDay = dayTotals(exePath, range, today)
        val trend = buildTrend(exePath, unit, byDay)

        // Busiest bucket of the chart as actually drawn — busiest hour/week/
        // month/year — so the number under the stat always names its own period.
        val busiest = if (unit == TrendUnit.DAY) {
            // Day bars are labelled by weekday; prefer the concrete date instead.
            byDay.maxByOrNull { it.value }?.let { TrendBucket(formatShortDate(it.key), it.value) }
        } else {
            trend.maxByOrNull { it.millis }?.takeIf { it.millis > 0 }
        }

        return AppDetail(
            packageName = exePath,
            label = apps.label(exePath),
            category = apps.category(exePath),
            trend = trend,
            trendUnit = axisCaption(unit),
            trendTitle = trendTitle(unit),
            busiest = busiest,
            rangeTotalMillis = byDay.values.sum(),
            activeBuckets = trend.count { it.millis > 0 },
        )
    }

    fun goal(): DailyGoal = settings.goal()

    fun setGoal(millis: Long) = settings.setGoal(millis)

    fun themeMode(): ThemeMode = settings.themeMode()

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    fun labelFor(exePath: String): String = apps.label(exePath)

    fun iconFor(exePath: String): Image? = apps.icon(exePath)

    /** Total screen time so far today, across all non-ignored programs. */
    fun todayTotalMillis(): Long {
        val today = todayEpochDay()
        return filteredTotals(today, today).values.sum()
    }

    // --- internals ---------------------------------------------------------

    private fun ranked(range: TimeRange, today: Long): List<AppUsage> =
        filteredTotals(range.startEpochDay(), today)
            .map { (exe, millis) ->
                AppUsage(
                    packageName = exe,
                    label = apps.label(exe),
                    totalMillis = millis,
                    category = apps.category(exe),
                )
            }
            .sortedByDescending { it.totalMillis }

    /** Totals for a day window with the noise rules applied (same rules everywhere). */
    private fun filteredTotals(startDay: Long, endDay: Long): Map<String, Long> =
        store.totalsBetween(startDay, endDay)
            .filterKeys { !apps.isIgnored(it) }
            .filterValues { it >= MIN_TRACKED_MILLIS }

    /** Day -> millis for [exePath] over exactly the window the main screen sums. */
    private fun dayTotals(exePath: String, range: TimeRange, today: Long): Map<Long, Long> =
        if (range == TimeRange.ALL) {
            store.allDailyMillisFor(exePath)
        } else {
            store.dailyMillisFor(exePath, range.startEpochDay(LocalDate.ofEpochDay(today)), today)
        }

    /**
     * Turn the daily table into one bar per bucket for [unit]. Every bucket sums
     * slices of [byDay], so the bars always add up to the headline total.
     */
    private fun buildTrend(exePath: String, unit: TrendUnit, byDay: Map<Long, Long>): List<TrendBucket> {
        val today = todayEpochDay()
        return when (unit) {
            TrendUnit.HOUR -> hourlyFor(exePath)
            TrendUnit.DAY -> {
                val start = today - (DAYS_IN_WEEK - 1)
                (start..today).map { TrendBucket(formatWeekday(it), byDay[it] ?: 0L) }
            }
            TrendUnit.WEEK -> {
                (0 until WEEKS_IN_MONTH).map { w ->
                    val back = WEEKS_IN_MONTH - 1 - w
                    val from = today - (back * DAYS_IN_WEEK + (DAYS_IN_WEEK - 1))
                    val to = today - back * DAYS_IN_WEEK
                    val total = (from..to).sumOf { byDay[it] ?: 0L }
                    TrendBucket(formatShortDate(from), total)
                }
            }
            TrendUnit.MONTH -> {
                val thisMonth = LocalDate.ofEpochDay(today).withDayOfMonth(1)
                (0 until MONTHS_IN_YEAR).map { m ->
                    val month = thisMonth.minusMonths((MONTHS_IN_YEAR - 1 - m).toLong())
                    val from = month.toEpochDay()
                    val to = month.plusMonths(1).minusDays(1).toEpochDay()
                    val total = byDay.filterKeys { it in from..to }.values.sum()
                    TrendBucket(month.format(MONTH_ABBR), total)
                }
            }
            TrendUnit.YEAR -> {
                val years = byDay.keys.map { LocalDate.ofEpochDay(it).year }.distinct().sorted()
                val shown = if (years.size <= MAX_YEAR_BARS) years else years.takeLast(MAX_YEAR_BARS)
                shown.map { y ->
                    val total = byDay.filterKeys { LocalDate.ofEpochDay(it).year == y }.values.sum()
                    TrendBucket(y.toString(), total)
                }
            }
        }
    }

    /**
     * The Today chart: 24 hourly bins for one program, read straight from the
     * tracker's hour buckets. Hours after the current one are dropped so the axis
     * ends "now" instead of trailing empty bars.
     */
    private fun hourlyFor(exePath: String): List<TrendBucket> {
        val today = todayEpochDay()
        val byHour = store.hourlyFor(exePath, today)
        val nowHour = java.time.LocalTime.now().hour
        return (0..nowHour).map { h ->
            TrendBucket(formatHourLabel(h), byHour[h] ?: 0L)
        }
    }

    private fun trendTitle(unit: TrendUnit) = when (unit) {
        TrendUnit.HOUR -> "By hour"
        TrendUnit.DAY -> "By day"
        TrendUnit.WEEK -> "By week"
        TrendUnit.MONTH -> "By month"
        TrendUnit.YEAR -> "By year"
    }

    private fun formatWeekday(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(WEEKDAY)

    private fun formatShortDate(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(SHORT_DATE)

    /** "12a" / "1p" / "12p" — short enough to fit a 24-bar axis. */
    private fun formatHourLabel(hour: Int): String = when (hour) {
        0 -> "12a"
        12 -> "12p"
        in 1..11 -> "${hour}a"
        else -> "${hour - 12}p"
    }

    private fun axisCaption(unit: TrendUnit) = when (unit) {
        TrendUnit.HOUR -> "hour of day"
        TrendUnit.DAY -> "day of week"
        TrendUnit.WEEK -> "week starting"
        TrendUnit.MONTH -> "month"
        TrendUnit.YEAR -> "year"
    }

    private fun categoryTotals(items: List<AppUsage>): List<CategoryTotal> =
        items.groupBy { apps.category(it.packageName) }
            .map { (category, apps) ->
                CategoryTotal(
                    category = category,
                    totalMillis = apps.sumOf { it.totalMillis },
                    appCount = apps.size,
                )
            }
            .sortedByDescending { it.totalMillis }

    private companion object {
        /** Below one second a row would just render as "0s" — pure noise. */
        const val MIN_TRACKED_MILLIS = 1_000L

        const val DAYS_IN_WEEK = 7
        const val WEEKS_IN_MONTH = 5
        const val MONTHS_IN_YEAR = 12
        const val MAX_YEAR_BARS = 6

        private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
        private val MONTH_ABBR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
        private val WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
    }
}
