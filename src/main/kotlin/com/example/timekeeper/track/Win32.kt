package com.example.timekeeper.track

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference

/**
 * The two Windows facts the tracker needs, and nothing else:
 *  - which executable owns the window you're looking at, and
 *  - how long since you last touched the keyboard or mouse.
 *
 * Windows has no "usage database" the way Android does, so instead of asking the
 * OS for history we sample these two values on a timer and add up the time
 * ourselves (see [UsageTracker]).
 */
object Win32 {

    /**
     * Full path of the process owning the current foreground window, or null if
     * there isn't one (e.g. the desktop is focused) or it can't be inspected.
     *
     * JNA gives us the window's process id; Java's own [ProcessHandle] turns that
     * into an executable path, so we never have to open a process handle by hand.
     */
    fun foregroundProcessPath(): String? {
        val hwnd = User32.INSTANCE.GetForegroundWindow() ?: return null
        val pidRef = IntByReference()
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef)
        val pid = pidRef.value
        if (pid <= 0) return null

        val handle = ProcessHandle.of(pid.toLong()).orElse(null) ?: return null
        return handle.info().command().orElse(null)?.takeIf { it.isNotBlank() }
    }

    /** Milliseconds since the last keyboard/mouse input. 0 if it can't be read. */
    fun idleMillis(): Long {
        val info = WinUser.LASTINPUTINFO()
        info.cbSize = info.size()
        if (!User32.INSTANCE.GetLastInputInfo(info)) return 0L
        val now = Kernel32.INSTANCE.GetTickCount()
        // Both are 32-bit ticks; the difference is a small positive number even
        // across a wrap, so plain int math is correct here.
        val diff = now - info.dwTime
        return if (diff < 0) 0L else diff.toLong()
    }
}
