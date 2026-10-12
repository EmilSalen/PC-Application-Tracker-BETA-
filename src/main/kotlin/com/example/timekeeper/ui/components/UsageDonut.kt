package com.example.timekeeper.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timekeeper.domain.AppCategory
import com.example.timekeeper.domain.AppUsage
import com.example.timekeeper.domain.formatDuration
import kotlin.math.max

/** How many apps get their own arc; the rest are folded into "Other". */
private const val MAX_ARCS = 6

/**
 * A ring chart of the top apps. Each app is a colored arc sized by how much of
 * the total time it accounts for, with the overall total in the middle.
 *
 * Arcs are colored by the app's *category*, so the ring answers "where did the
 * time go, and what kind of time was it" at a glance: two social apps are two
 * shades of the same blue, a game is amber, and so on. The legend underneath
 * names each arc so the colors are never a mystery. Drawn by hand — no library.
 */
@Composable
fun UsageDonut(
    items: List<AppUsage>,
    totalMillis: Long,
    modifier: Modifier = Modifier,
) {
    // Read theme colors here, outside the Canvas lambda (that scope is not @Composable).
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val otherColor = MaterialTheme.colorScheme.outline

    // Animate the sweep so switching ranges feels smooth.
    val progress by animateFloatAsState(
        targetValue = if (totalMillis > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "donut",
    )

    val top = items.take(MAX_ARCS)
    val topSum = top.sumOf { it.totalMillis }
    val other = (totalMillis - topSum).coerceAtLeast(0L)

    // One color per arc, category-derived and de-duplicated so same-category
    // siblings stay tellable apart. Shared by the drawing and the legend so a
    // swatch can never disagree with its arc.
    val arcColors = remember(top) { categoryArcColors(top) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(210.dp)) {
                val stroke = 30.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
                // Same shading direction for every arc keeps the ring reading as one object.
                val shadeEnd = Offset(size.width, size.height)

                // Track — a faint full ring so an empty state still looks intentional.
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )

                if (totalMillis <= 0) return@Canvas

                var start = -90f
                top.forEachIndexed { i, item ->
                    val base = arcColors[i]
                    val fraction = item.totalMillis.toFloat() / totalMillis.toFloat()
                    val sweep = fraction * 360f * progress
                    drawArc(
                        brush = arcBrush(base, shadeEnd),
                        startAngle = start,
                        sweepAngle = max(sweep - GAP_DEGREES, 0f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    start += sweep
                }
                if (other > 0) {
                    val fraction = other.toFloat() / totalMillis.toFloat()
                    drawArc(
                        color = otherColor,
                        startAngle = start,
                        sweepAngle = max(fraction * 360f * progress - GAP_DEGREES, 0f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(totalMillis),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (top.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            DonutLegend(
                items = top,
                colors = arcColors,
                otherColor = otherColor,
                showOther = other > 0,
            )
        }
    }
}

/**
 * The legend: which app each color is. Two columns of swatch + name, plus an
 * "Other" swatch when the ring collapsed the tail into one arc.
 */
@Composable
private fun DonutLegend(
    items: List<AppUsage>,
    colors: List<Color>,
    otherColor: Color,
    showOther: Boolean,
) {
    val entries = buildList {
        items.forEachIndexed { i, item -> add(item.label to colors[i]) }
        if (showOther) add("Other" to otherColor)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                row.forEach { (label, color) ->
                    LegendItem(label = label, color = color, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One arc color per app: the app's category hue, then nudged toward white or
 * black for the 2nd, 3rd… app of that same category so siblings don't blur into
 * one another. Counts are per-category, so a lone Productivity app keeps the
 * exact palette teal.
 */
private fun categoryArcColors(items: List<AppUsage>): List<Color> {
    val counts = items.groupingBy { it.category }.eachCount()
    val seen = HashMap<AppCategory, Int>()
    return items.map { item ->
        val index = seen.getOrDefault(item.category, 0)
        seen[item.category] = index + 1
        shade(categoryColor(item.category), index, counts[item.category] ?: 1)
    }
}

/** Base hue for the first of a category; later siblings step lighter/darker. */
private fun shade(base: Color, index: Int, count: Int): Color = when {
    count <= 1 || index == 0 -> base
    else -> {
        val step = (0.18f * ((index + 1) / 2)).coerceAtMost(0.5f)
        val toward = if (index % 2 == 1) Color.White else Color.Black
        lerp(base, toward, step)
    }
}

/** A soft top-left highlight fading into the base tone, for a little depth. */
private fun arcBrush(base: Color, end: Offset): Brush = Brush.linearGradient(
    colors = listOf(lerp(base, Color.White, 0.22f), base),
    start = Offset.Zero,
    end = end,
)

/** Gap between neighboring arcs, in degrees. */
private const val GAP_DEGREES = 3f
