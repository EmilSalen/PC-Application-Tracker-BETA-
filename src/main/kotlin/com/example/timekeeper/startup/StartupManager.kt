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
            run("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\"$exe\"", "/f")
        } else {
            run("delete", RUN_KEY, "/v", VALUE_NAME, "/f")
        }
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
