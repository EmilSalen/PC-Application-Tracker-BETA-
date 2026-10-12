package com.example.timekeeper.domain

/**
 * A coarse grouping so the user sees "1h 20m in Social" instead of only a flat
 * per-app list. Programs don't reliably declare a category themselves, so a
 * curated name map does most of the work ([com.example.timekeeper.data.AppNameResolver])
 * with everything unknown falling back to [OTHER].
 *
 * Order matters: it's the stable order used for the breakdown colors.
 */
enum class AppCategory(val label: String) {
    SOCIAL("Social"),
    VIDEO("Video"),
    GAMES("Games"),
    PRODUCTIVITY("Productivity"),
    OTHER("Other"),
}
