package com.example.timekeeper.startup

/**
 * Adds or removes the app from Windows' "run at sign-in" list, so the tracker
 * keeps counting even when the window was never opened.
 *
 * Done through the `reg` command rather than a native call — it's the same thing
 * the Startup folder / Task Manager toggle does, and needs no extra dependency.
 */
object StartupManager {

    private const val VALUE_NAME = "TimeKeeper"
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"

    fun isEnabled(): Boolean = run("query", RUN_KEY, "/v", VALUE_NAME) == 0

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            val exe = ProcessHandle.current().info().command().orElse(null) ?: return
            // Quotes around a path with spaces are mandatory for a Run entry.
            // Two traps here: we must pass the value as `\"exe\"` (backslash-
            // escaped), not `"exe"` — reg.exe strips a single surrounding pair,
            // which stored the path unquoted; and a Run entry with an unquoted
            // spaced path is a launch hazard (CreateProcess may pick a wrong or
            // truncated binary).
            run("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\\\"$exe\\\"", "/f")
        } else {
            run("delete", RUN_KEY, "/v", VALUE_NAME, "/f")
        }
    }

    /**
     * Point an already-enabled entry at the executable running right now. The
     * entry is written once and then left alone, so an update that moves the app
     * (or installs it elsewhere) would otherwise leave sign-in launching the old
     * binary at the old path forever. No-op when startup is switched off, so a
     * deliberate "don't start with Windows" is respected.
     */
    fun refresh() {
        if (isEnabled()) setEnabled(true)
    }

    /** Runs `reg <args>` and returns its exit code (0 = success). */
    private fun run(vararg args: String): Int = try {
        val process = ProcessBuilder("reg", *args).redirectErrorStream(true).start()
        // Drain the output so the process can't block on a full pipe.
        process.inputStream.readBytes()
        process.waitFor()
    } catch (t: Throwable) {
        -1
    }
}
