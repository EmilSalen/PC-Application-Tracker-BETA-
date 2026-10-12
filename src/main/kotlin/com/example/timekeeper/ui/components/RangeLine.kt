package com.example.timekeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The quiet line under the total telling you exactly which dates (and, for
 * Today, which time) the number covers.
 */
@Composable
fun RangeLine(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
