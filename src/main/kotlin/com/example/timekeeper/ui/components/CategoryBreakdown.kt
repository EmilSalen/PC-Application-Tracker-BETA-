package com.example.timekeeper.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timekeeper.domain.AppCategory
import com.example.timekeeper.domain.CategoryTotal
import com.example.timekeeper.domain.formatDuration

/**
 * Where the time went, by category: one stacked bar plus a legend. Shown above
 * the per-app list so the shape of the week is readable at a glance.
 */
@Composable
fun CategoryBreakdown(
    totals: List<CategoryTotal>,
    modifier: Modifier = Modifier,
) {
    if (totals.isEmpty()) return
    val grand = totals.sumOf { it.totalMillis }
    if (grand <= 0) return

    val colors = CategoryColors

    Column(modifier = modifier) {
        Text(
            text = "By category",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            totals.forEach { entry ->
                val weight = (entry.totalMillis.toFloat() / grand.toFloat()).coerceAtLeast(0.001f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(12.dp)
                        .background(colors.getValue(entry.category)),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            totals.forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.getValue(entry.category)),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = entry.category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatDuration(entry.totalMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.appCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Fixed palette so a given category always keeps the same color — everywhere:
 * the category bar, the donut arcs, and the per-app rows all read from here, so
 * "teal = Productivity" is a fact you can carry from one view to the next.
 * Hand-picked (rather than pulled from the theme scheme) so the five categories
 * read as five clearly distinct, saturated hues instead of five shades of one
 * accent.
 */
val CategoryColors: Map<AppCategory, Color> = mapOf(
    AppCategory.SOCIAL to Color(0xFF4C6FFF), // vivid blue
    AppCategory.VIDEO to Color(0xFFFF6F91), // warm pink-red
    AppCategory.GAMES to Color(0xFFFFA033), // amber
    AppCategory.PRODUCTIVITY to Color(0xFF1FC8A9), // teal-green
    AppCategory.OTHER to Color(0xFF9A8CFF), // soft violet
)

/** The stable color for one category, falling back to Other's hue. */
fun categoryColor(category: AppCategory): Color =
    CategoryColors[category] ?: CategoryColors.getValue(AppCategory.OTHER)
