package com.example.timekeeper.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.example.timekeeper.data.ThemeMode
import com.example.timekeeper.data.UsageRepository
import com.example.timekeeper.domain.AppDetail
import com.example.timekeeper.domain.Dashboard
import com.example.timekeeper.domain.TimeRange
import com.example.timekeeper.ui.icons.toIconBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val loading: Boolean = true,
    val range: TimeRange = TimeRange.WEEK,
    val dashboard: Dashboard = Dashboard(
        items = emptyList(),
        totalMillis = 0,
        categoryTotals = emptyList(),
        todayMillis = 0,
    ),
    val goalMillis: Long = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Which screen we're on. Null = the main list. */
    val detailFor: String? = null,
    val detail: AppDetail? = null,
)

/**
 * Everything the window reads. Same shape as the phone app's view model, minus
 * the Android lifecycles: the desktop app is a long-lived process, so it just
 * holds a [CoroutineScope] and refreshes on demand.
 */
class UsageViewModel(
    private val repo: UsageRepository,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(UiState(themeMode = repo.themeMode()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Program icons, resolved on IO and cached — the shell is slow to ask. */
    private val icons = HashMap<String, ImageBitmap?>()

    init {
        refresh()
    }

    fun selectRange(range: TimeRange) {
        if (range == _state.value.range) return
        _state.update { it.copy(range = range) }
        refresh()
    }

    fun openDetail(exePath: String) {
        _state.update { it.copy(detailFor = exePath, detail = null) }
        scope.launch {
            val range = _state.value.range
            val detail = withContext(Dispatchers.IO) { repo.detailFor(exePath, range) }
            _state.update { it.copy(detail = detail) }
        }
    }

    fun closeDetail() {
        _state.update { it.copy(detailFor = null, detail = null) }
    }

    fun setThemeMode(mode: ThemeMode) {
        repo.setThemeMode(mode)
        _state.update { it.copy(themeMode = mode) }
    }

    fun setGoal(millis: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { repo.setGoal(millis) }
            _state.update { it.copy(goalMillis = millis) }
        }
    }

    /** Re-read the table for the selected range. Icons for the top rows are pre-resolved. */
    fun refresh() {
        scope.launch {
            val range = _state.value.range
            val (dashboard, goal) = withContext(Dispatchers.IO) {
                val d = repo.dashboardFor(range)
                d.items.take(ICON_PRELOAD).forEach { row -> resolveIcon(row.packageName) }
                d to repo.goal().millis
            }
            _state.update { it.copy(loading = false, dashboard = dashboard, goalMillis = goal) }
            val open = _state.value.detailFor
            if (open != null) {
                val detail = withContext(Dispatchers.IO) {
                    resolveIcon(open)
                    repo.detailFor(open, range)
                }
                _state.update { it.copy(detail = detail) }
            }
        }
    }

    fun iconFor(exePath: String): ImageBitmap? = synchronized(icons) { icons[exePath] }

    private fun resolveIcon(exePath: String) {
        synchronized(icons) {
            if (icons.containsKey(exePath)) return
            icons[exePath] = repo.iconFor(exePath)?.toIconBitmap()
        }
    }

    private companion object {
        /** Resolving an icon shells out to Windows; only do it for what's on screen. */
        const val ICON_PRELOAD = 40
    }
}
