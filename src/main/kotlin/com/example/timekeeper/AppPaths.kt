package com.example.timekeeper

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Where the app keeps its database and settings. Everything lives under %LOCALAPPDATA%. */
object AppPaths {

    val dataDir: Path = Paths.get(
        System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
        "TimeKeeper",
    )

    val dbFile: Path get() = dataDir.resolve("usage.db")

    /** Path of the running executable, so the tracker can ignore its own window. */
    val selfExe: String? = ProcessHandle.current().info().command().orElse(null)

    fun ensureDataDir() {
        Files.createDirectories(dataDir)
    }
}
