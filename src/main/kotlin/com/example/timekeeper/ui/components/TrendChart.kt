package com.example.timekeeper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timekeeper.domain.TrendBucket
import com.example.timekeeper.domain.formatDuration

/** Above this many buckets the x-axis labels would overlap, so they're hidden. */
private const val AXIS_MAX_BARS = 12

/**
 * A Steam-style trend chart: one bar per bucket, click or drag anywhere to
 * select a bar and read its exact value. A guide line and a floating chip track
 * the selection.
 */
@Composable
fun TrendChart(
    buckets: List<TrendBucket>,
    axisCaption: String,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val guideColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val maxMillis = buckets.maxOf { it.millis }.coerceAtLeast(1L)
    val maxLabel = formatDuration(maxMillis)

    // -1 = nothing picked yet; we draw that as "no selection".
    var selected by remember(buckets) { mutableIntStateOf(-1) }

    Column(modifier = modifier.fillMaxWidth()) {
        // The readout lives in its own band above the plot, so bars can never
        // grow up into it no matter how tall the tallest bucket is.
        Box(modifier = Modifier.fillMaxWidth().height(30.dp)) {
            if (selected in buckets.indices) {
                SelectionChip(
                    bucket = buckets[selected],
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } else {
                Text(
                    text = "max $maxLabel · click or drag to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        selected = indexAt(offset.x, size.width, buckets.size)
                    }
                }
                .pointerInput(buckets) {
                    detectDragGestures { change, _ ->
                        selected = indexAt(change.position.x, size.width, buckets.size)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val n = buckets.size
            val slot = w / n
            val barWidth = (slot * 0.56f)
                .coerceAtLeast(6f)
                .coerceAtMost(44.dp.toPx())

            fun barHeight(millis: Long): Float =
                (millis.toFloat() / maxMillis) * h

            buckets.forEachIndexed { i, bucket ->
                val cx = i * slot + slot / 2f
                val height = barHeight(bucket.millis)
                val isSelected = i == selected

                if (bucket.millis > 0) {
                    val color = when {
                        selected < 0 -> barColor
                        isSelected -> barColor
                        else -> dimColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - barWidth / 2f, h - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 3f),
                    )
                } else {
                    drawRoundRect(
                        color = emptyColor.copy(alpha = 0.6f),
                        topLeft = Offset(cx - barWidth / 2f, h - 3f),
                        size = Size(barWidth, 3f),
                        cornerRadius = CornerRadius(1.5f),
                    )
                }
            }

            if (selected in buckets.indices) {
                val gx = (selected + 0.5f) * slot
                drawLine(
                    color = guideColor,
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 2f,
                )
            }
        }

        if (buckets.size <= AXIS_MAX_BARS) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = axisCaption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                buckets.forEachIndexed { i, bucket ->
                    val faded = bucket.millis == 0L
                    val barColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i == selected) barColor.copy(alpha = 0.18f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (i == selected) barColor
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (faded) 0.5f else 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionChip(bucket: TrendBucket, modifier: Modifier = Modifier) {
    Text(
        text = "${bucket.label} · ${formatDuration(bucket.millis)}",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

private fun indexAt(x: Float, width: Int, count: Int): Int {
    if (count <= 0 || width <= 0) return -1
    val slot = width.toFloat() / count
    return (x / slot).toInt().coerceIn(0, count - 1)
}
