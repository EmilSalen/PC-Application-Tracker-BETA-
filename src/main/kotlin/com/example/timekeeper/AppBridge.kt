package com.example.timekeeper

/**
 * A tiny seam between the non-Compose world (tray menu, single-instance server)
 * and the Compose window's state. The composition fills these in once the window
 * exists; outside callers just invoke them.
 */
object AppBridge {
    /** Bring the window to the front and make it visible. */
    var showWindow: (() -> Unit)? = null

    /** Hide the window without quitting — the app keeps counting in the tray. */
    var hideWindow: (() -> Unit)? = null

    /** Close the app for good: stop tracking, drop the tray icon, exit. */
    var quit: (() -> Unit)? = null
}
