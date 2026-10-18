package com.example.timekeeper.track

import com.sun.jna.platform.win32.User32
import com.sun.jna.ptr.IntByReference

/**
 * The one Windows fact the tracker needs: which executable owns the window in
 * front. Windows has no "usage database" the way Android does, so instead of
 * asking the OS for history we sample the foreground window on a timer and add
 * up the time ourselves (see [UsageTracker]).
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
}
