package com.example.timekeeper.notify

import com.example.timekeeper.startup.StartupManager
import com.example.timekeeper.ui.icons.AppIcon
import java.awt.CheckboxMenuItem
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionListener
import javax.swing.SwingUtilities

/**
 * The app lives in the system tray so it can keep counting while its window is
 * closed. This installs the tray icon and its menu (Open / Start with Windows /
 * Quit) and is the channel for the "goal reached" balloon.
 */
object TrayController {

    @Volatile
    private var trayIcon: TrayIcon? = null

    fun install(onOpen: () -> Unit, onQuit: () -> Unit) {
        if (trayIcon != null) return
        if (!SystemTray.isSupported()) return

        val popup = PopupMenu()

        val openItem = MenuItem("Open TimeKeeper")
        openItem.addActionListener { onOpen() }
        popup.add(openItem)

        val startupItem = CheckboxMenuItem("Start with Windows", StartupManager.isEnabled())
        startupItem.addActionListener {
            StartupManager.setEnabled(startupItem.state)
            // Reflect the real result — the registry write can fail.
            startupItem.state = StartupManager.isEnabled()
        }
        popup.add(startupItem)

        popup.addSeparator()

        val quitItem = MenuItem("Quit")
        quitItem.addActionListener { onQuit() }
        popup.add(quitItem)

        val icon = TrayIcon(AppIcon.image(16), "TimeKeeper", popup).apply {
            isImageAutoSize = true
            addActionListener(ActionListener { onOpen() })  // double-click / single-click
        }

        try {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        } catch (t: Throwable) {
            // Tray unavailable — the window still works, it just won't persist
            // in the background.
            trayIcon = null
        }
    }

    fun remove() {
        val icon = trayIcon ?: return
        runCatching { SystemTray.getSystemTray().remove(icon) }
        trayIcon = null
    }

    /** Show a Windows notification balloon from the tray icon. */
    fun showBalloon(title: String, body: String) {
        val icon = trayIcon ?: return
        SwingUtilities.invokeLater {
            runCatching { icon.displayMessage(title, body, TrayIcon.MessageType.NONE) }
        }
    }
}
