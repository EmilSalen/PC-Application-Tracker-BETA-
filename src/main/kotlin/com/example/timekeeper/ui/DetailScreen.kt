package com.example.timekeeper.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timekeeper.domain.AppDetail
import com.example.timekeeper.domain.TimeRange
import com.example.timekeeper.domain.formatDuration
import com.example.timekeeper.ui.components.TrendChart
import com.example.timekeeper.ui.components.categoryColor
import com.example.timekeeper.ui.icons.AppIcons

/**
 * Tap-through for a single app: a trend chart bucketed for the selected range,
 * plus the average / busiest / active stats that go with it.
 */
@Composable
fun DetailScreen(
    detail: AppDetail?,
    range: TimeRange,
    rangeLabel: String,
    icon: ImageBitmap?,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Back")
            }
            TextButton(onClick = onClose) { Text("Back") }
        }

        if (detail == null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Loading…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Text(
                    text = detail.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // A small pill in the category's own color, so the app's group is
            // legible here too — the same hue the donut arc and list bar carry.
            Text(
                text = detail.category.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = categoryColor(detail.category),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(categoryColor(detail.category).copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = formatDuration(detail.rangeTotalMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = detail.trendTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            TrendChart(buckets = detail.trend, axisCaption = detail.trendUnit)

            Spacer(modifier = Modifier.height(24.dp))

            StatsRow(detail, range)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatsRow(detail: AppDetail, range: TimeRange) {
    val activeBuckets = detail.activeBuckets.coerceAtLeast(1)
    val average = detail.rangeTotalMillis / activeBuckets

    val averageLabel = when (range) {
        TimeRange.TODAY -> "Hourly average"
        TimeRange.WEEK -> "Daily average"
        TimeRange.MONTH -> "Weekly average"
        TimeRange.YEAR -> "Monthly average"
        TimeRange.ALL -> "Average per year"
    }
    val busiestLabel = when (range) {
        TimeRange.TODAY -> "Busiest hour"
        TimeRange.WEEK -> "Busiest day"
        TimeRange.MONTH -> "Busiest week"
        TimeRange.YEAR -> "Busiest month"
        TimeRange.ALL -> "Busiest year"
    }
    val activeLabel = when (range) {
        TimeRange.TODAY -> "Active hours"
        TimeRange.WEEK -> "Active days"
        TimeRange.MONTH -> "Active weeks"
        TimeRange.YEAR -> "Active months"
        TimeRange.ALL -> "Active years"
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Stat(averageLabel, formatDuration(average))
        Stat(
            busiestLabel,
            detail.busiest?.let { "${formatDuration(it.millis)} · ${it.label}" } ?: "—",
        )
        Stat(activeLabel, "${detail.activeBuckets}")
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
