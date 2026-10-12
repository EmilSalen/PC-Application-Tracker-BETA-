package com.example.timekeeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timekeeper.domain.formatDuration

/**
 * Pick a daily screen-time goal from a few sensible presets. Kept to presets on
 * purpose — a slider or number entry is more furniture than this needs.
 */
@Composable
fun GoalDialog(
    currentMillis: Long,
    todayMillis: Long,
    onDismiss: () -> Unit,
    onSetGoal: (Long) -> Unit,
) {
    val presets = listOf(1, 2, 3, 4, 6).map { it * 60L * 60L * 1000L }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (currentMillis > 0) {
                        "Currently ${formatDuration(currentMillis)} per day."
                    } else {
                        "No goal set yet."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (currentMillis > 0) {
                    Text(
                        text = "Today: ${formatDuration(todayMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Pick a target. You'll get a notification the first " +
                        "time each day you pass it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                presets.forEach { millis ->
                    OutlinedButton(
                        onClick = { onSetGoal(millis) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = formatDuration(millis) +
                                if (millis == currentMillis) "  ✓" else "",
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSetGoal(0L) }) { Text("Turn off") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
