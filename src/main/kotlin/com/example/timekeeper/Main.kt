package com.example.timekeeper

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.timekeeper.data.SettingsStore
import com.example.timekeeper.data.UsageRepository
import com.example.timekeeper.data.UsageStore
import com.example.timekeeper.domain.formatDuration
import com.example.timekeeper.notify.GoalMonitor
import com.example.timekeeper.notify.TrayController
import com.example.timekeeper.startup.StartupManager
import com.example.timekeeper.track.UsageTracker
import com.example.timekeeper.ui.DetailScreen
import com.example.timekeeper.ui.UsageScreen
import com.example.timekeeper.ui.UsageViewModel
import com.example.timekeeper.ui.icons.AppIcon
import com.example.timekeeper.ui.icons.toIconBitmap
import com.example.timekeeper.ui.theme.TimeKeeperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main() {
    // If another copy is already running, ask it to show and stop here. This
    // matters: two trackers would add the same time to the same table twice.
    if (!SingleInstance.claim(onShow = { AppBridge.showWindow?.invoke() })) {
        SingleInstance.notifyExisting()
        return
    }

    AppPaths.ensureDataDir()

    val store = UsageStore(AppPaths.dbFile.toString())
    val settings = SettingsStore()
    val repo = UsageRepository(store, settings)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val tracker = UsageTracker(store)
    tracker.start(scope)

    // Offer to keep running from now on, but only once — never overwrite a
    // choice the user already made in the tray menu. Skipped for a dev run
    // (`gradlew run`), where the "executable" is java.exe and registering that
    // would just add a broken entry.
    if (isPackagedLaunch() && settings.string(KEY_STARTUP_PROMPTED, null) == null) {
        StartupManager.setEnabled(true)
        settings.putString(KEY_STARTUP_PROMPTED, "1")
    }

    val goalMonitor = GoalMonitor(repo, settings) { used, goal ->
        TrayController.showBalloon(
            "Daily screen-time goal reached",
            "You've used ${formatDuration(used)} today — your goal was ${formatDuration(goal)}.",
        )
    }
    goalMonitor.start(scope)

    val quit: () -> Unit = { scope.launch { onQuit(scope, tracker) } }
    TrayController.install(
        onOpen = { AppBridge.showWindow?.invoke() },
        onQuit = quit,
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        tracker.stop()
        SingleInstance.release()
    })

    application {
        val vm = remember { UsageViewModel(repo, scope) }
        val state by vm.state.collectAsState()

        val windowState = rememberWindowState(
            size = DpSize(440.dp, 780.dp),
            position = WindowPosition(Alignment.Center),
        )

        Window(
            onCloseRequest = { AppBridge.hideWindow?.invoke() },
            state = windowState,
            title = "TimeKeeper",
            icon = remember { BitmapPainter(AppIcon.image(64).toIconBitmap()!!) },
        ) {
            // Outlets for the tray menu and the second-launch poke, plus a
            // refresh whenever the window is shown again from the tray. Closing
            // the window hides it rather than quitting, so tracking continues.
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(360, 520)
                AppBridge.hideWindow = { window.isVisible = false }
                AppBridge.showWindow = {
                    SwingUtilities.invokeLater {
                        window.isVisible = true
                        window.toFront()
                        window.requestFocus()
                        vm.refresh()
                    }
                }
                AppBridge.quit = quit
                window.addWindowListener(object : WindowAdapter() {
                    override fun windowActivated(e: WindowEvent?) = vm.refresh()
                })
            }

            // Keep the numbers live. The tracker writes a fresh sample every few
            // seconds, but the screen otherwise only re-read on a range change —
            // so a window left open showed stale totals until you poked a range
            // chip. Re-read on the tracker's own cadence, only while visible.
            LaunchedEffect(Unit) {
                while (true) {
                    delay(LIVE_REFRESH_MILLIS)
                    if (window.isVisible) vm.refresh()
                }
            }

            TimeKeeperTheme(mode = state.themeMode) {
                // Paint the window's root with the theme's background. A desktop
                // window defaults to white, so without this the scheme's colors
                // changed but the backdrop stayed light — dark mode looked broken.
                // Surface (not a bare Box) also supplies the matching content
                // color, so text with no explicit color follows the theme instead
                // of falling back to black.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val detailFor = state.detailFor
                    if (detailFor != null) {
                        DetailScreen(
                            detail = state.detail,
                            range = state.range,
                            rangeLabel = state.range.rangeLabel(),
                            icon = vm.iconFor(detailFor),
                            onClose = vm::closeDetail,
                        )
                    } else {
                        UsageScreen(
                            state = state,
                            onSelectRange = vm::selectRange,
                            onOpenDetail = vm::openDetail,
                            onSetGoal = vm::setGoal,
                            onSetThemeMode = vm::setThemeMode,
                            iconLoader = vm::iconFor,
                        )
                    }
                }
            }
        }
    }
}

/** Tidy shutdown shared by the tray "Quit" item and the single-instance hook. */
private fun onQuit(scope: CoroutineScope, tracker: UsageTracker) {
    tracker.stop()
    TrayController.remove()
    SingleInstance.release()
    scope.cancel()
    exitProcess(0)
}

private const val KEY_STARTUP_PROMPTED = "startup_prompted"

/** How often the open window re-reads usage so its numbers stay live. */
private const val LIVE_REFRESH_MILLIS = 5_000L

/** True when the running command is the packaged app rather than a bare JVM. */
private fun isPackagedLaunch(): Boolean {
    val cmd = ProcessHandle.current().info().command().orElse(null) ?: return false
    val name = cmd.substringAfterLast('\\').substringAfterLast('/').lowercase()
    return name != "java.exe" && name != "javaw.exe"
}
