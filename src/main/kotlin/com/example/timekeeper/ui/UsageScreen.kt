package com.example.timekeeper.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.timekeeper.data.ThemeMode
import com.example.timekeeper.domain.TimeRange
import com.example.timekeeper.ui.components.AppUsageRow
import com.example.timekeeper.ui.components.CategoryBreakdown
import com.example.timekeeper.ui.components.GoalDialog
import com.example.timekeeper.ui.components.RangeSelector
import com.example.timekeeper.ui.components.ThemeDialog
import com.example.timekeeper.ui.components.TotalHeader
import com.example.timekeeper.ui.components.UsageDonut
import com.example.timekeeper.ui.icons.AppIcons

@Composable
fun UsageScreen(
    state: UiState,
    onSelectRange: (TimeRange) -> Unit,
    onOpenDetail: (String) -> Unit,
    onSetGoal: (Long) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    iconLoader: (String) -> ImageBitmap?,
) {
    val dashboard = state.dashboard
    val items = dashboard.items
    val max = items.maxOfOrNull { it.totalMillis } ?: 1L

    var showGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    // The donut tucks itself away the moment the list is scrolled, so it never
    // sits in front of the apps you're reading.
    val expanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 40
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            currentMillis = state.goalMillis,
            todayMillis = dashboard.todayMillis,
            onDismiss = { showGoalDialog = false },
            onSetGoal = {
                onSetGoal(it)
                showGoalDialog = false
            },
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = state.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                onSetThemeMode(it)
                showThemeDialog = false
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Screen time",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showThemeDialog = true }) {
                Icon(imageVector = AppIcons.Contrast, contentDescription = "Appearance")
            }
            IconButton(onClick = { showGoalDialog = true }) {
                Icon(imageVector = AppIcons.Target, contentDescription = "Daily goal")
            }
        }

        RangeSelector(selected = state.range, onSelect = onSelectRange)

        Spacer(modifier = Modifier.height(24.dp))
        TotalHeader(
            totalMillis = dashboard.totalMillis,
            appCount = items.size,
            rangeLabel = state.range.rangeLabel(),
            goalMillis = state.goalMillis,
            todayMillis = dashboard.todayMillis,
        )

        if (items.isEmpty()) {
            Text(
                text = "No usage in this range yet. Use your PC for a bit, then come back here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item(key = "donut") {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = tween(240),
                        label = "chevron",
                    )
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = AppIcons.ChevronDown,
                                contentDescription = "Scroll to see the app list",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp).rotate(rotation),
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            UsageDonut(items = items, totalMillis = dashboard.totalMillis)
                        }
                    }
                }
                item(key = "categories") {
                    Column {
                        CategoryBreakdown(totals = dashboard.categoryTotals)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "By app",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                items(items, key = { it.packageName }) { item ->
                    AppUsageRow(
                        usage = item,
                        icon = iconLoader(item.packageName),
                        fraction = (item.totalMillis.toFloat() / max.toFloat()).coerceIn(0f, 1f),
                        onClick = { onOpenDetail(item.packageName) },
                    )
                }
            }
        }
    }
}
