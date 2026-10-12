package com.example.timekeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timekeeper.domain.formatDuration

/**
 * The big "you spent this long" headline, with the comparison line under it and
 * (when a goal is set) a compact ring for today's usage off to the right.
 */
@Composable
fun TotalHeader(
    totalMillis: Long,
    appCount: Int,
    rangeLabel: String,
    goalMillis: Long,
    todayMillis: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (totalMillis > 0) formatDuration(totalMillis) else "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    appCount == 0 -> "No usage recorded yet"
                    appCount == 1 -> "across 1 app"
                    else -> "across $appCount apps"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RangeLine(label = rangeLabel)
        }

        if (goalMillis > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GoalRing(usedMillis = todayMillis, goalMillis = goalMillis)
                Text(
                    text = "today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
