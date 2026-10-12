package com.example.timekeeper.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timekeeper.domain.TimeRange

/** Today / Week / Month / Year / All filter, as a Material 3 segmented control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeSelector(
    selected: TimeRange,
    onSelect: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranges = TimeRange.entries
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
        space = 4.dp,
    ) {
        ranges.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index, ranges.size),
                modifier = Modifier.weight(1f),
                icon = {},
            ) {
                Text(
                    text = range.label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
