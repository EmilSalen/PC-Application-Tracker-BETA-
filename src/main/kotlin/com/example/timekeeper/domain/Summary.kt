package com.example.timekeeper.domain

/** One category and how long it accounts for in the selected range. */
data class CategoryTotal(
    val category: AppCategory,
    val totalMillis: Long,
    val appCount: Int,
)

/** A single bucket on the trend chart: a label to show and how long it held. */
data class TrendBucket(
    val label: String,
    val millis: Long,
)

/** Everything the per-app detail screen needs about one app. */
data class AppDetail(
    val packageName: String,
    val label: String,
    val category: AppCategory,
    /** One bar per bucket, oldest first, for the selected range only. */
    val trend: List<TrendBucket>,
    /** Caption for the chart's x-axis, shown only when the bars are sparse. */
    val trendUnit: String,
    /** Title for the chart, e.g. "By hour" or "By day". */
    val trendTitle: String,
    /** Largest single bucket, e.g. the app's busiest day. */
    val busiest: TrendBucket?,
    /** Total for the currently selected range. */
    val rangeTotalMillis: Long,
    /** How many buckets had any usage. */
    val activeBuckets: Int,
)

/**
 * A bundle of everything the main screen shows for one range, computed in a
 * single pass over the local table.
 */
data class Dashboard(
    val items: List<AppUsage>,
    val totalMillis: Long,
    val categoryTotals: List<CategoryTotal>,
    val todayMillis: Long,
)
